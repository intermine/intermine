package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.ClassKeysNotFoundException;
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
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.exceptions.UnauthorizedException;
import org.intermine.webservice.server.lists.ListInput;
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
    protected Format getDefaultFormat() {
        return Format.JSON;
    }

    @Override
    protected boolean canServe(Format format) {
        return format == Format.JSON || format == Format.TEXT;
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated()) {
            throw new UnauthorizedException();
        }
        if (!getPermission().isRW()) {
            throw new ServiceForbiddenException("This request does not have RW permission");
        }
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        ListInput input = new ListInput(request, bagManager, profile);
        PathQuery pq = getQuery(request);

        setHeaderAttributes(input.getListName());
        generateListFromQuery(
                pq,
                input.getListName(),
                input.getDescription(),
                input.getTags(),
                profile);

    }

    /**
     * Get the pathquery to use for this request.
     * @param request The http request.
     * @return A pathquery
     */
    protected PathQuery getQuery(HttpServletRequest request) {
        String xml = new QueryRequestParser(im.getQueryStore(), request).getQueryXml();
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
            String name, String description, Collection<String> tags,
            Profile profile) throws ObjectStoreException, PathException {

        Query q = getQuery(pq, profile);

        String tempName = name + TEMP;

        String viewPathString = pq.getView().get(0);
        Path viewPath = pq.makePath(viewPathString);
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
            throw new ServiceException(e.getMessage(), e);
        } catch (ClassKeysNotFoundException cke) {
            throw new BadRequestException("Bag has not class key set", cke);
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
                bagManager.getBags(profile),
                new HashMap<String, QuerySelectable>(),
                im.getBagQueryRunner(),
                new HashMap<String, BagQueryResult>());
        } catch (ObjectStoreException e) {
            throw new ServiceException(e);
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
