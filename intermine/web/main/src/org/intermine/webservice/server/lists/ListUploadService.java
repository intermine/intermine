package org.intermine.webservice.server.lists;

import java.io.BufferedReader;
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
    private static final String TEMP_SUFFIX = "_temp";

    /**
     * Constructor
     * @param im A reference to the main settings bundle
     */
    public ListUploadService(InterMineAPI im) {
        super(im);
        this.runner = im.getBagQueryRunner();
    }

    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"unmatchedIdentifiers\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
            attributes.put(JSONFormatter.KEY_QUOTE, Boolean.TRUE);
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, getCallback());
        }
        return attributes;
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        if (!this.isAuthenticated()) {
            throw new BadRequestException("Not authenticated." + USAGE);
        }
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);
        Properties webProperties
            = (Properties) session.getServletContext().getAttribute(Constants.WEB_PROPERTIES);;

        String type = request.getParameter("type");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String extraFieldValue = request.getParameter("extraValue");
        boolean replace = Boolean.parseBoolean("replaceExisting");

        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(name)) {
            throw new BadRequestException("Name or type is blank." + USAGE);
        }
        Map<String, Object> attributes = getHeaderAttributes();
        Map<String, String> kvPairs = new HashMap<String, String>();
        kvPairs.put("newListName", name);
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);

        String tempName = name + TEMP_SUFFIX;
        String bagUploadDelims =
            (String) webProperties.get("list.upload.delimiters") + " ";
        StrMatcher matcher = StrMatcher.charSetMatcher(bagUploadDelims);
        if (!"text/plain".equals(request.getContentType())) {
            throw new BadRequestException("Bad content type - " + request.getContentType() + USAGE);
        }
        BufferedReader r = request.getReader();

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
            
            kvPairs.put("newListSize", Integer.toString(tempBag.size()));
	        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
            output.setHeaderAttributes(attributes);
            
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
