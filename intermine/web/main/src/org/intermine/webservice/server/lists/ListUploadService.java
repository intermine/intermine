package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.intermine.InterMineException;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * A class to create a new list with via a set of identifiers uploaded by a user.
 * @author Alexis Kalderimis.
 *
 */
public class ListUploadService extends ListMakerService
{

    /**
     * A usage string to return for bad requests.
     */
    public static final String USAGE =
        "\nList Upload Service\n"
        + "===================\n"
        + "Upload a new list, or overwrite an existing one\n"
        + "Parameters:\n"
        + "* name: the name of the list\n"
        + "* type: type of the list\n"
        + "* description: A description of the list (optional)\n"
        + "* extraValue: An extra field value to allow disambiguation(optional)\n"
        + "* tags: a semi-colon delimited list of tags to tag the new list with\n"
        + "\n"
        + "Content: text/plain - list of ids\n";

    /**
     * The maximum number of ids to query for each batch.
     */
    public static final int BAG_QUERY_MAX_BATCH_SIZE = 10000;

    private final BagQueryRunner runner;
    protected static final String PLAIN_TEXT = "text/plain";

    /**
     * Constructor
     * @param im A reference to the main settings bundle
     */
    public ListUploadService(final InterMineAPI im) {
        super(im);
        runner = im.getBagQueryRunner();
    }

    /**
     * Gets the header attributes on the output object.
     * @return A map of header attributes for JSON output.
     */
    @Override
    protected Map<String, Object> getHeaderAttributes() {
        final Map<String, Object> attributes = super.getHeaderAttributes();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"unmatchedIdentifiers\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
            attributes.put(JSONFormatter.KEY_QUOTE, Boolean.TRUE);
        }
        return attributes;
    }

    /**
     * Sets the size of the list on the header attributes.
     * @param size The size of the newly created list.
     */
    protected void setListSize(final Integer size) {
        addOutputInfo(LIST_SIZE_KEY, size + "");
    }

    /**
     * Get the String Matcher for parsing the list of identifiers.
     * @return The matcher to use.
     */
    protected StrMatcher getMatcher() {
        final String bagUploadDelims = getProperty("list.upload.delimiters") + " ";
        final StrMatcher matcher = StrMatcher.charSetMatcher(bagUploadDelims);
        return matcher;
    }

    @Override
    protected String getNewListType(final ListInput input) {
        return input.getType();
    }

    /**
     * Parse the parameters for this request.
     * @return A parsed representation of the parameters.
     */
    @Override
    protected ListCreationInput getInput() {
        return new ListCreationInput(request, bagManager, getPermission().getProfile());
    }

    @Override
    protected void makeList(
            final ListInput listInput,
            final String type,
            final Profile profile,
            final Set<String> temporaryBagNamesAccumulator) throws Exception {

        if (StringUtils.isBlank(type)) {
            throw new BadRequestException("No list type provided");
        }

        ListCreationInput input = (ListCreationInput) listInput;

        if (input.doReplace()) {
            ListServiceUtils.ensureBagIsDeleted(profile, input.getListName());
        }
        if (profile.getAllBags().containsKey(input.getListName())) {
            throw new BadRequestException(
                "Attempt to overwrite an existing bag - name: '"
                + input.getListName() + "'");
        }

        final Set<String> ids = new LinkedHashSet<String>();
        final Set<String> unmatchedIds = new HashSet<String>();
        final InterMineBag tempBag = profile.createBag(input.getTemporaryListName(),
                type, input.getDescription(), im.getClassKeys());

        processIdentifiers(type, input, ids, unmatchedIds, tempBag);

        setListSize(tempBag.size());

        for (final Iterator<String> i = unmatchedIds.iterator(); i.hasNext();) {
            final List<String> row = new ArrayList<String>(Arrays.asList(i.next()));
            if (i.hasNext()) {
                row.add("");
            }
            output.addResultItem(row);
        }

        if (!input.getTags().isEmpty()) {
            im.getBagManager().addTagsToBag(input.getTags(), tempBag, profile);
        }
        profile.renameBag(input.getTemporaryListName(), input.getListName());
    }

    /**
     * Process the identifiers.
     * @param type The type of thing these identifiers are.
     * @param input The creation input.
     * @param ids The identifiers.
     * @param unmatchedIds A collector for unmatched identifiers.
     * @param tempBag The temporary bag to add results to.
     * @throws IOException If we can't from the request.
     * @throws ClassNotFoundException if the type is not valid.
     * @throws InterMineException If something goes wrong building the bag.
     * @throws ObjectStoreException If there is a problem on the database level.
     */
    protected void processIdentifiers(
            final String type,
            ListCreationInput input,
            final Set<String> ids,
            final Set<String> unmatchedIds,
            final InterMineBag tempBag)
        throws IOException, ClassNotFoundException, InterMineException, ObjectStoreException {
        final Collection<String> addIssues = input.getAddIssues();
        String line;
        final StrMatcher matcher = getMatcher();
        final BufferedReader r = getReader(request);
        try {
            while ((line = r.readLine()) != null) {
                final StrTokenizer st =
                    new StrTokenizer(line, matcher, StrMatcher.doubleQuoteMatcher());
                while (st.hasNext()) {
                    final String token = st.nextToken();
                    ids.add(token);
                }
                if (ids.size() >= BAG_QUERY_MAX_BATCH_SIZE) {
                    addIdsToList(ids, tempBag, type, input.getExtraValue(),
                            unmatchedIds, addIssues);
                    ids.clear();
                }
            }
        } finally {
            if (r != null) {
                r.close();
            }
        }
        if (ids.size() > 0) {
            addIdsToList(ids, tempBag, type, input.getExtraValue(), unmatchedIds, addIssues);
        }
    }

    /**
     * Get the reader for the identifiers uploaded with this request.
     * @param request The request object.
     * @return A buffered reader for reading the identifiers.
     */
    protected BufferedReader getReader(final HttpServletRequest request) {
        BufferedReader r = null;

        if (ServletFileUpload.isMultipartContent(request)) {
            final ServletFileUpload upload = new ServletFileUpload();
            try {
                final FileItemIterator iter = upload.getItemIterator(request);
                while (iter.hasNext()) {
                    final FileItemStream item = iter.next();
                    final String fieldName = item.getFieldName();
                    if (!item.isFormField() && "identifiers".equalsIgnoreCase(fieldName)) {
                        final InputStream stream = item.openStream();
                        final InputStreamReader in = new InputStreamReader(stream);
                        r = new BufferedReader(in);
                        break;
                    }
                }
            } catch (FileUploadException e) {
                throw new ServiceException("Could not read request body", e);
            } catch (IOException e) {
                throw new ServiceException(e);
            }
        } else {
            if (!requestIsOfSuitableType()) {
                throw new BadRequestException("Bad content type - "
                        + request.getContentType() + USAGE);
            }
            try {
                r = request.getReader();
            } catch (IOException e) {
                throw new ServiceException(e);
            }
        }
        if (r == null) {
            throw new BadRequestException("No identifiers found in request." + USAGE);
        }
        return r;
    }

    /**
     * Determine if we should service this request.
     * @return whether or not this request's content is of the right type for us to read it.
     */
    protected boolean requestIsOfSuitableType() {
        final String mimetype = request.getContentType();
        return "application/octet-stream".equals(mimetype) || mimetype.startsWith("text");
    }

    /**
     * Adds objects to the a bag for the matches against a set of identifiers.
     * @param ids A collection of identifiers
     * @param bag The bag to add the objects to
     * @param type The type of this bag
     * @param extraFieldValue An extra value for disambiguation.
     * @param unmatchedIds An accumulator to store the failed matches.
     * @param acceptableIssues the list of issues that are OK to ignore.
     * @throws ClassNotFoundException if the type is not a valid class.
     * @throws InterMineException If something goes wrong building the bag.
     * @throws ObjectStoreException If there is a problem on the database level.
     */
    protected void addIdsToList(
            final Collection<? extends String> ids,
            final InterMineBag bag,
            final String type,
            final String extraFieldValue,
            final Set<String> unmatchedIds,
            final Collection<String> acceptableIssues)
        throws ClassNotFoundException, InterMineException, ObjectStoreException {
        final BagQueryResult result = runner.searchForBag(
                type, new ArrayList<String>(ids), extraFieldValue,
                acceptableIssues.contains(BagQueryResult.WILDCARD));
        bag.addIdsToBag(result.getMatches().keySet(), type);

        for (final String issueType: result.getIssues().keySet()) {
            if (acceptableIssues.contains(issueType)) {
                bag.addIdsToBag(result.getIssueIds(issueType), type);
            } else {
                unmatchedIds.addAll(result.getInputIdentifiersForIssue(issueType));
            }
        }
        unmatchedIds.addAll(result.getUnresolvedIdentifiers());
    }

}
