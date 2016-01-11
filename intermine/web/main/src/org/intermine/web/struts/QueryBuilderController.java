package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.Model;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.metadata.TypeUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.query.MetadataNode;
import org.intermine.web.logic.querybuilder.ModelBrowserHelper;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the main query builder tile. Generally, request attributes that are required by
 * multiple tiles on the query builder are synthesized here.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 * @see org.intermine.web.struts.QueryBuilderConstraintController
 * @see org.intermine.web.struts.QueryBuilderPathsController
 */
public class QueryBuilderController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(QueryBuilderController.class);
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        Profile profile = SessionMethods.getProfile(session);
        if (im.getBagManager().isAnyBagToUpgrade(profile)) {
            ActionMessages actionErrors = getErrors(request);
            actionErrors.add(ActionMessages.GLOBAL_MESSAGE,
                new ActionMessage("login.upgradeListManually"));
            saveErrors(request, actionErrors);
        }
        ActionMessages msgs = this.getMessages(request);
        for (String removedOrderBy: populateRequest(request, response)) {
            msgs.add(ActionMessages.GLOBAL_MESSAGE,
                new ActionMessage("querybuilder.assuresortorder.removeorderby", removedOrderBy));
        }
        this.saveMessages(request, msgs);
        return null;
    }

    /**
     * Populate the request with the necessary attributes to render the query builder page. This
     * method is static so that it can be called from the AJAX actions in MainChange.java
     *
     * @param request
     *            the current request
     * @param response
     *            the current response
     * @return A collection of strings that have been removed from the sort-order.
     */
    public static Collection<String> populateRequest(
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Model model = im.getModel();
        PathQuery query = SessionMethods.getQuery(session);
        Collection<String> removedOrderBys = assureCorrectSortOrder(query);

        try {
            // Create a Map from view path to sort style (disabled, asc, desc, none). At the moment,
            // the order by list will only contain one element, as the web representation is not
            // able to represent multiple order elements
            request.setAttribute("viewStrings", findViewSortOrders(query));
            request.setAttribute("viewPaths", listToMap(query.getView()));

            // set up the metadata
            WebConfig webConfig = SessionMethods.getWebConfig(request);
            boolean isSuperUser = SessionMethods.isSuperUser(session);

            String prefix = (String) session.getAttribute("prefix");
            String path = (String) session.getAttribute("path");
            if (path == null) {
                path = prefix;
            }

            // nodes for the model browser
            Collection<MetadataNode> nodes = ModelBrowserHelper.makeSelectedNodes(path, prefix,
                    model, isSuperUser, query, webConfig, im.getClassKeys(), im.getBagManager(),
                    SessionMethods.getProfile(session), im.getObjectStoreSummary());

            // set nodes
            request.setAttribute("nodes", nodes);

            Map<String, String> prefixes = getViewPathLinkPaths(query);
            request.setAttribute("viewPathLinkPrefixes", prefixes);
            request.setAttribute("viewPathLinkPaths", getPathTypes(prefixes.values(), query));
        } catch (PathException e) {
            LOG.error("PathQuery is invalid: " + query, e);
        }
        return removedOrderBys;
    }

    /**
     * This method takes a PathQuery and ensures that no elements are present on the order by list
     * that are not also on the view list.
     *
     * @param pathQuery a PathQuery object to manipulate
     * @param request needed in case we need to add any messages.
     * @return A collection of strings representing paths removed from the sort order.
     */
    private static Collection<String> assureCorrectSortOrder(PathQuery pathQuery) {
        Collection<String> ret = new HashSet<String>();
        for (OrderElement order : pathQuery.getOrderBy()) {
            if (!pathQuery.getView().contains(order.getOrderPath())) {
                pathQuery.removeOrderBy(order.getOrderPath());
                ret.add(order.getOrderPath());
            }
        }
        return ret;
    }

    /**
     * Returns a Map from each element in the view list of a query to its sorting status. Sorting
     * status is any of the four:
     * <UL><LI><B>disabled</B> - this means that this view element is in an outer join, so we cannot
     * sort by it</LI>
     *     <LI><B>none</B> - this means that this view element is not in the order by list</LI>
     *     <LI><B>asc</B> - this means that the query is ordered by this view element,
     * ascending</LI>
     *     <LI><B>desc</B> - this means that the query is ordered by this view element,
     * descending</LI>
     * </UL>
     * Note that the query is ordered by only one element.
     *
     * @param pathQuery a PathQuery to process
     * @return a Map from String view path to sort status String
     * @throws PathException if something goes wrong
     */
    private static Map<String, String> findViewSortOrders(PathQuery pathQuery)
        throws PathException {
        Map<String, String> retval = new LinkedHashMap<String, String>();
        List<OrderElement> orderList = pathQuery.getOrderBy();
        OrderElement order;
        if (orderList.isEmpty()) {
            order = null;
        } else {
            order = orderList.get(0);
        }
        for (String view : pathQuery.getView()) {
            if (pathQuery.isPathCompletelyInner(view)) {
                if ((order != null) && (view.equals(order.getOrderPath()))) {
                    retval.put(view, order.getDirection().toString().toLowerCase());
                } else {
                    retval.put(view, "none");
                }
            } else {
                retval.put(view, "disabled");
            }
        }
        return retval;
    }

    /**
     * Return a Map from path to unqualified type name.
     *
     * @param paths collection of paths
     * @param pathQuery related PathQuery
     * @return Map from path to type
     */
    private static Map<String, String> getPathTypes(Collection<String> paths,
            PathQuery pathQuery) {
        Map<String, String> viewPathTypes = new HashMap<String, String>();
        for (String path : paths) {
            try {
                String unqualifiedName = TypeUtil.unqualifiedName(pathQuery.makePath(path)
                        .getEndType().getName());
                viewPathTypes.put(path, unqualifiedName);
            } catch (PathException e) {
                throw new Error("There must be a bug", e);
            }
        }
        return viewPathTypes;
    }

    /**
     * Return a Map from path to path/subpath pointing to the nearest not attribute for each path on
     * the select list. In practice this results in the same path or the path with an attribute name
     * chopped off the end.
     *
     * @param pathQuery the path query
     * @return mapping from select list path to non-attribute path
     * @throws PathException if something goes wrong
     */
    private static Map<String, String> getViewPathLinkPaths(PathQuery pathQuery)
        throws PathException {
        Map<String, String> linkPaths = new HashMap<String, String>();
        for (String viewString : pathQuery.getView()) {
            Path path = pathQuery.makePath(viewString);
            if (path.endIsAttribute()) {
                linkPaths.put(viewString, path.getPrefix().toStringNoConstraints());
            } else {
                linkPaths.put(viewString, viewString);
            }
        }

        return linkPaths;
    }

    /**
     * Returns a map where every item in <code>list</code> maps to Boolean TRUE.
     *
     * @param list the list of map keys
     * @param <E> the element type
     * @return a map that maps every item in list to Boolean.TRUE
     */
    private static <E> Map<E, Boolean> listToMap(Collection<E> list) {
        Map<E, Boolean> map = new HashMap<E, Boolean>();
        for (E o : list) {
            map.put(o, Boolean.TRUE);
        }
        return map;
    }
}
