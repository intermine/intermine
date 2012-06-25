package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.CompletelyFalseException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.query.result.PathQueryBuilder;

/**
 * A service for transforming a query, represented as a PathQuery serialised
 * to XML, to a list.
 * @author Alex Kalderimis
 *
 */
public class QueryToListService extends AbstractQueryService
{

    private static final String XML_PARAM = "query";
    private static final String NAME_PARAM = "listName";
    private static final String DESC_PARAM = "description";

    private static final String TEMP = "_temp";

    protected final BagManager bagManager;

    /**
     * Constructor.
     * @param im The intermine settings bundle
     */
    public QueryToListService(InterMineAPI im) {
        super(im);
        bagManager = im.getBagManager();
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated()) {
            throw new ServiceForbiddenException("All requests to list operation services must"
                    + " be authenticated.");
        }
        if (!getPermission().isRW()) {
            throw new ServiceForbiddenException("This request does not have RW permission");
        }
    }

    @Override
    protected void execute() throws Exception {

        Profile profile = getPermission().getProfile();

        String name = request.getParameter(NAME_PARAM);
        String description = request.getParameter(DESC_PARAM);
        String[] tags = StringUtils.split(request.getParameter("tags"), ';');
        List<String> tagList = (tags == null) ? Collections.EMPTY_LIST : Arrays.asList(tags);

        if (StringUtils.isEmpty(name)) {
            setHeaderAttributes("none-given");
            throw new BadRequestException("name is blank");
        }

        setHeaderAttributes(name);

        PathQuery pq = getQuery(request);
        generateListFromQuery(pq, name, description, tagList, profile);

    }

    /**
     * Get the pathquery to use for this request.
     * @param request The http request.
     * @return A pathquery
     */
    protected PathQuery getQuery(HttpServletRequest request) {
        String xml = request.getParameter(XML_PARAM);

        if (StringUtils.isEmpty(xml)) {
            throw new BadRequestException("query is blank");
        }

        PathQueryBuilder builder = getQueryBuilder(xml);
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

    /**
     * Generate a list from a pathquery.
     * @param pq The pathquery
     * @param name The name of the list
     * @param description The description of the list
     * @param tags A list of tags to add to the list
     * @param profile The profile the list should belong to
     * @throws ObjectStoreException If there is an issue running the queries that generate the list.
     * @throws PathException If the paths supplied are illegal.
     */
    protected void generateListFromQuery(PathQuery pq,
            String name, String description, List<String> tags,
            Profile profile) throws ObjectStoreException, PathException {

        Query q = getQuery(pq, profile);

        String tempName = name + TEMP;

        String viewPathString = pq.getView().get(0);
        Path viewPath = new Path(pq.getModel(), viewPathString);
        String type = viewPath.getLastClassDescriptor().getUnqualifiedName();

        try {
            InterMineBag newList =
                    profile.createBag(tempName, type, description, im.getClassKeys());
            newList.addToBagFromQuery(q);
            try {
                im.getBagManager().addTagsToBag(tags, newList, profile);
            } catch (TagManager.TagNameException e) {
                throw new BadRequestException(e.getMessage());
            } catch (TagManager.TagNamePermissionException e) {
                throw new ServiceForbiddenException(e.getMessage());
            }
            profile.renameBag(tempName, name);

            output.addResultItem(Arrays.asList("" + newList.size()));

        } catch (CompletelyFalseException e) {
            output.addResultItem(Arrays.asList("0"));
            throw new BadRequestException("List not created - it would be of size 0");
        } catch (UnknownBagTypeException e) {
            output.addResultItem(Arrays.asList("0"));
            throw new InternalErrorException(e.getMessage(), e);
        } finally {
            if (profile.getSavedBags().containsKey(tempName)) {
                profile.deleteBag(tempName);
            }
        }
    }

    /**
     * Get the Objectstore Query to run to generate the list.
     * @param pq The pathquery to generate the query from.
     * @param profile The profile to search for bags.
     * @return A query.
     */
    protected Query getQuery(PathQuery pq, Profile profile) {
        Query ret;
        try {
            ret = MainHelper.makeQuery(pq,
                bagManager.getUserAndGlobalBags(profile),
                new HashMap<String, QuerySelectable>(),
                im.getBagQueryRunner(),
                new HashMap<String, BagQueryResult>());
        } catch (ObjectStoreException e) {
            throw new InternalErrorException(e);
        }
        return ret;
    }

    /**
     * Sets the header attributes map on the current output object.
     * @param name The name of the list.
     */
    protected void setHeaderAttributes(String name) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, getCallback());
        }
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"listSize\":");
        }
        Map<String, String> kvPairs = new HashMap<String, String>();
        kvPairs.put("listName", name);
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
        output.setHeaderAttributes(attributes);
    }

}
