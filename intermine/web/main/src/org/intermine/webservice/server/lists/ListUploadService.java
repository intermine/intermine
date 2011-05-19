package org.intermine.webservice.server.lists;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import org.intermine.api.profile.BagDoesNotExistException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.exceptions.BadRequestException;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.output.JSONFormatter;

public class ListUploadService extends WebService {

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
        + "\n"
        + "Content: text/plain - list of ids\n";

    /**
     * The maximum number of ids to query for each batch.
     */
    public static final int BAG_QUERY_MAX_BATCH_SIZE = 10000;

    private final BagQueryRunner runner;
    protected static final String TEMP_SUFFIX = "_temp";
    protected static final String PLAIN_TEXT = "text/plain";
    protected static final String LIST_NAME_KEY = "listName";
    protected static final String LIST_SIZE_KEY = "listSize";

    protected Map<String, String> kvPairs = new HashMap<String, String>();

    /**
     * Constructor
     * @param im A reference to the main settings bundle
     */
    public ListUploadService(InterMineAPI im) {
        super(im);
        this.runner = im.getBagQueryRunner();
    }

    protected void setHeaderAttributes(List<String> requiredParams) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"unmatchedIdentifiers\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
            attributes.put(JSONFormatter.KEY_QUOTE, Boolean.TRUE);
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, getCallback());
        }
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
        output.setHeaderAttributes(attributes);

        for (String param: requiredParams) {
            if (StringUtils.isEmpty(param)) {
                kvPairs.put(LIST_NAME_KEY, "unknown");
                throw new BadRequestException(USAGE);
            }
        } 
    }

    protected void setListName(String name) {
        kvPairs.put(LIST_NAME_KEY, name);
    }

    protected void setListSize(Integer size) {
        kvPairs.put(LIST_SIZE_KEY, size.toString());
    }

    protected StrMatcher getMatcher(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);
        Properties webProperties
            = (Properties) session.getServletContext().getAttribute(Constants.WEB_PROPERTIES);;

        String bagUploadDelims =
            (String) webProperties.get("list.upload.delimiters") + " ";
        StrMatcher matcher = StrMatcher.charSetMatcher(bagUploadDelims);
        return matcher;
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        if (!this.isAuthenticated()) {
            throw new BadRequestException("Not authenticated." + USAGE);
        }
        StrMatcher matcher = getMatcher(request);
        Profile profile = SessionMethods.getProfile(request.getSession());

        String type = request.getParameter("type");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String extraFieldValue = request.getParameter("extraValue");
        boolean replace = Boolean.parseBoolean("replaceExisting");
    
        setListName(name);
        setHeaderAttributes(Arrays.asList(type, name));

        String tempName = name + TEMP_SUFFIX;

        BufferedReader r = getReader(request);
        Set<String> ids = new LinkedHashSet<String>();
        Set<String> unmatchedIds = new HashSet<String>();

        try {
            InterMineBag tempBag = profile.createBag(tempName, type, description);
            String line;
            while ((line = r.readLine()) != null) {
                StrTokenizer st = new StrTokenizer(line, matcher, StrMatcher.doubleQuoteMatcher());
                while (st.hasNext()) {
                    String token = st.nextToken();
                    ids.add(token);
                }
                if (ids.size() >= BAG_QUERY_MAX_BATCH_SIZE) {
                    addIdsToList(ids, tempBag, type, extraFieldValue, unmatchedIds);
                    ids.clear();
                }
            }
            if (ids.size() > 0) {
                addIdsToList(ids, tempBag, type, extraFieldValue, unmatchedIds);
            }

            setListSize(tempBag.size());

            for (Iterator<String> i = unmatchedIds.iterator(); i.hasNext();) {
                List<String> row = new ArrayList<String>(Arrays.asList(i.next()));
                if (i.hasNext()) {
                    row.add("");
                }
                output.addResultItem(row);
            }
            if (replace) {
                try {
                    profile.deleteBag(name);
                } catch(BagDoesNotExistException e) {
                    // Ignore
                }
            }
            profile.renameBag(tempName, name);
        } finally {
            try {
                profile.deleteBag(tempName);
            } catch(BagDoesNotExistException e) {
                // Ignore
            }
        }
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
            if (!requestIsOfSuitableType(request)) {
                throw new BadRequestException("Bad content type - " +
                        request.getContentType() + USAGE);
            }
            r = request.getReader();
        }
        if (r == null) {
            throw new BadRequestException("No identifiers found in request." + USAGE);
        }
        return r;
    }

    protected boolean requestIsOfSuitableType(HttpServletRequest request) {
        String mimetype = request.getContentType();
        return ("application/octet-stream".equals(mimetype) || mimetype.startsWith("text"));
    }

    protected void addIdsToList(Collection<? extends String> ids, InterMineBag bag,
            String type, String extraFieldValue, Set<String> unmatchedIds)
            throws ClassNotFoundException, InterMineException, ObjectStoreException {
        BagQueryResult result = runner.searchForBag(
                type, new ArrayList<String>(ids), extraFieldValue, false);
        bag.addIdsToBag(result.getMatches().keySet(), type);

        for (String issueType: result.getIssues().keySet()) {
            Map<String, Map<String, List>> issueMap = result.getIssues().get(issueType);
            for (String query: issueMap.keySet()) {
                unmatchedIds.addAll(issueMap.get(query).keySet());
            }
        }
        unmatchedIds.addAll(result.getUnresolved().keySet());
    }

}
