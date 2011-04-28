package org.intermine.webservice.server.query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.CompletelyFalseException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.query.result.PathQueryBuilder;

public class QueryToListService extends AbstractQueryService {

    private static final String XML_PARAM = "query";
    private static final String NAME_PARAM = "listName";
    private static final String DESC_PARAM = "description";

    private static final String TEMP = "_temp";

    private final BagManager bagManager;

    public QueryToListService(InterMineAPI im) {
        super(im);
        bagManager = im.getBagManager();
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        if (!isAuthenticated()) {
            throw new BadRequestException("Not authenticated.");
        }

        Profile profile = SessionMethods.getProfile(request.getSession());

        String name = request.getParameter(NAME_PARAM);
        String description = request.getParameter(DESC_PARAM);

        if (StringUtils.isEmpty(name)) {
            setHeaderAttributes("none-given");
            throw new BadRequestException("name is blank");
        }

        setHeaderAttributes(name);

        PathQuery pq = getQuery(request);
        generateListFromQuery(pq, name, description, profile);

    }

    protected PathQuery getQuery(HttpServletRequest request) {
        String xml = request.getParameter(XML_PARAM);

        if (StringUtils.isEmpty(xml)) {
            throw new BadRequestException("query is blank");
        }

        PathQueryBuilder builder = getQueryBuilder(xml, request);
        PathQuery pq = builder.getQuery();
        if (pq.getView().size() != 1) {
            throw new BadRequestException(
                    "Queries to the query-to-list service can only have one output column");
        }

        if (!pq.getView().get(0).endsWith(".id")) {
            throw new BadRequestException(
                    "Queries to the query-to-list service must have ids in their view");
        }
        return pq;
    }

    protected void generateListFromQuery(PathQuery pq,
            String name, String description,
            Profile profile) throws ObjectStoreException, PathException {
        Query q = MainHelper.makeQuery(
                pq,
                bagManager.getUserAndGlobalBags(profile),
                new HashMap<String, QuerySelectable>(),
                im.getBagQueryRunner(),
                new HashMap<String, BagQueryResult>());

        String tempName = name + TEMP;

        String viewPathString = pq.getView().get(0);
        Path viewPath = new Path(pq.getModel(), viewPathString);
        String type = viewPath.getLastClassDescriptor().getUnqualifiedName();

        try {
            InterMineBag newList = profile.createBag(tempName, type, description);
            newList.addToBagFromQuery(q);
            profile.renameBag(tempName, name);

            output.addResultItem(Arrays.asList("" + newList.size()));

        } catch (CompletelyFalseException e) {
            output.addResultItem(Arrays.asList("0"));
            throw new BadRequestException("List not created - it would be of size 0");
        } finally {
            if (profile.getSavedBags().containsKey(tempName)) {
                profile.deleteBag(tempName);
            }
        }
    }

    protected void setHeaderAttributes(String name) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, getCallback());
        }
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"newListSize\":");
        }
        Map<String, String> kvPairs = new HashMap<String, String>();
        kvPairs.put("newListName", name);
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
        output.setHeaderAttributes(attributes);
    }

}
