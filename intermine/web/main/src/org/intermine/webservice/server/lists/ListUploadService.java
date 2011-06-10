package org.intermine.webservice.server.lists;

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
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.intermine.InterMineException;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.Constants;
import org.intermine.webservice.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;

public class ListUploadService extends ListMakerService {

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
    public ListUploadService(InterMineAPI im) {
        super(im);
        this.runner = im.getBagQueryRunner();
    }

    /**
     * Sets the header attributes on the output object.
     * @param requiredParams The list of parameters which are required by this service.
     */
    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = super.getHeaderAttributes();
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
    protected void setListSize(Integer size) {
        addOutputInfo(LIST_SIZE_KEY, size + "");
    }

    /**
     * Get the String Matcher for parsing the list of identifiers.
     * @return
     */
    protected StrMatcher getMatcher() {
        HttpSession session = request.getSession();
        Properties webProperties
            = (Properties) session.getServletContext().getAttribute(Constants.WEB_PROPERTIES);

        String bagUploadDelims =
            (String) webProperties.get("list.upload.delimiters") + " ";
        StrMatcher matcher = StrMatcher.charSetMatcher(bagUploadDelims);
        return matcher;
    }

    @Override
    protected String getNewListType(ListInput input) {
        return input.getType();
    }

    protected ListInput getInput(HttpServletRequest request, BagManager bagManager) {
        return new ListCreationInput(request, bagManager);
    }

    @Override
    protected void makeList(ListInput input, String type, Profile profile,
        Set<String> temporaryBagNamesAccumulator) throws Exception {

        StrMatcher matcher = getMatcher();

        BufferedReader r = getReader(request);
        Set<String> ids = new LinkedHashSet<String>();
        Set<String> unmatchedIds = new HashSet<String>();

        InterMineBag tempBag = profile.createBag(
                input.getTemporaryListName(), type, input.getDescription());
        String line;
        while ((line = r.readLine()) != null) {
            StrTokenizer st = new StrTokenizer(line, matcher, StrMatcher.doubleQuoteMatcher());
            while (st.hasNext()) {
                String token = st.nextToken();
                ids.add(token);
            }
            if (ids.size() >= BAG_QUERY_MAX_BATCH_SIZE) {
                addIdsToList(ids, tempBag, type, input.getExtraValue(),
                        unmatchedIds);
                ids.clear();
            }
        }
        if (ids.size() > 0) {
            addIdsToList(ids, tempBag, type, input.getExtraValue(), unmatchedIds);
        }

        setListSize(tempBag.size());

        for (Iterator<String> i = unmatchedIds.iterator(); i.hasNext();) {
            List<String> row = new ArrayList<String>(Arrays.asList(i.next()));
            if (i.hasNext()) {
                row.add("");
            }
            output.addResultItem(row);
        }
        if (input.doReplace()) {
            ListServiceUtils.ensureBagIsDeleted(profile, input.getListName());
        }
        if (!input.getTags().isEmpty()) {
            im.getBagManager().addTagsToBag(input.getTags(), tempBag, profile);
        }
        profile.renameBag(input.getTemporaryListName(), input.getListName());
    }

    protected BufferedReader getReader(HttpServletRequest request)
        throws IOException, FileUploadException {
        BufferedReader r = null;

        if (ServletFileUpload.isMultipartContent(request)) {
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iter = upload.getItemIterator(request);
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String fieldName = item.getFieldName();
                if (!item.isFormField() && "identifiers".equalsIgnoreCase(fieldName)) {
                    InputStream stream = item.openStream();
                    InputStreamReader in = new InputStreamReader(stream);
                    r = new BufferedReader(in);
                    break;
                }
            }
        } else {
            if (!requestIsOfSuitableType()) {
                throw new BadRequestException("Bad content type - "
                        + request.getContentType() + USAGE);
            }
            r = request.getReader();
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
        String mimetype = request.getContentType();
        return ("application/octet-stream".equals(mimetype) || mimetype.startsWith("text"));
    }

    /**
     * Adds objects to the a bag for the matches against a set of identifiers.
     * @param ids A collection of identifiers
     * @param bag The bag to add the objects to
     * @param type The type of this bag
     * @param extraFieldValue An extra value for disambiguation.
     * @param unmatchedIds An accumulator to store the failed matches.
     * @throws ClassNotFoundException if the type is not a valid class.
     * @throws InterMineException If something goes wrong building the bag.
     * @throws ObjectStoreException If there is a problem on the database level.
     */
    protected void addIdsToList(Collection<? extends String> ids, InterMineBag bag,
            String type, String extraFieldValue, Set<String> unmatchedIds)
            throws ClassNotFoundException, InterMineException, ObjectStoreException {
        BagQueryResult result = runner.searchForBag(
                type, new ArrayList<String>(ids), extraFieldValue, false);
        bag.addIdsToBag(result.getMatches().keySet(), type);

        for (String issueType: result.getIssues().keySet()) {
            @SuppressWarnings("rawtypes")
            Map<String, Map<String, List>> issueMap = result.getIssues().get(issueType);
            for (String query: issueMap.keySet()) {
                unmatchedIds.addAll(issueMap.get(query).keySet());
            }
        }
        unmatchedIds.addAll(result.getUnresolved().keySet());
    }

}
