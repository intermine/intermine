package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.AdditionalConverter;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.util.NameUtil;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.PortalHelper;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * The portal query action handles links into flymine from external sites.
 * The action expects 'class' and 'externalid' or 'externalids' parameters
 * it handles the creation of lists if linking in with multiple identifiers
 * and is capable of converting types (e.g. Protein into Gene).
 *
 * @author Thomas Riley
 * @author Xavier Watkins
 */

public class PortalQueryAction extends InterMineAction
{
    private static int index = 0;

    /**
     * Link-ins from other sites end up here (after some redirection).
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ServletContext servletContext = session.getServletContext();

        String origin = request.getParameter("origin");
        String className = request.getParameter("class");
        String extId = request.getParameter("externalid");
        if ((extId == null) || (extId.length() <= 0)) {
            extId = request.getParameter("externalids");
        }

        // Add a message to welcome the user
        Properties properties = SessionMethods.getWebProperties(servletContext);
        String welcomeMsg = properties.getProperty("portal.welcome." + origin);
        if (StringUtils.isEmpty(welcomeMsg)) {
            welcomeMsg = properties.getProperty("portal.welcome");
        }
        if (!StringUtils.isBlank(welcomeMsg)) {
            SessionMethods.recordMessage(welcomeMsg, session);
        }

        if (extId == null || extId.length() == 0) {
            recordError(new ActionMessage("errors.badportalidentifiers"), request);
            return mapping.findForward("failure");
        }

        String[] idList = extId.split(",");

        // Use the old way = quicksearch template in case some people used to link in
        // without class name
        if ((idList.length == 1) && (className == null || className.length() == 0)) {
            BagQueryRunner bagRunner = im.getBagQueryRunner();
            BagQueryResult bqr
                = bagRunner.searchForBag("BioEntity", Arrays.asList(idList), null, false);

            Map<Integer, List> matches = bqr.getMatches();
            Map<String, Map<String, Map<String, List>>> issues = bqr.getIssues();
            if (matches.isEmpty() && issues.isEmpty()) {
                return new ForwardParameters(mapping.findForward("noResults")).forward();
            }

            // check the matches first...
            for (Map.Entry<Integer, List> entry : matches.entrySet()) {
                String id = entry.getKey().toString();
                return new ForwardParameters(mapping.findForward("report"))
                    .addParameter("id", id).forward();
            }

            // and if there are none check the issues
            for (Entry<String, Map<String, Map<String, List>>> issue : issues.entrySet()) {

                Set<String> queryType = issue.getValue().keySet();
                for (String qt : queryType) {
                    Object obj = issue.getValue().get(qt).get(idList[0]).get(0);

                    // parse the string representation of the object
                    String ob = obj.toString().substring(obj.toString().indexOf('[') + 1);
                    String id = null;
                    String[] result = ob.split(", ");
                    for (String token : result) {
                        String[] pair = token.split("=");
                        if ("id".equalsIgnoreCase(pair[0])) {
                            id = pair[1].replaceAll("\"", "").replaceAll("]", "");
                            return new ForwardParameters(mapping.findForward("report"))
                                .addParameter("id", id).forward();
                        }
                        continue;
                    }
                }
            }
        }

        Model model = im.getModel();
        WebConfig webConfig = SessionMethods.getWebConfig(request);
        BagQueryConfig bagQueryConfig = im.getBagQueryConfig();

        // If the class is not in the model, we can't continue
        className = StringUtil.capitalise(className);
        if (model.getClassDescriptorByName(className) == null) {
            recordError(new ActionMessage("errors.badportalclass"), request);
            return goToNoResults(mapping, session);
        }

        PathQuery pathQuery = new PathQuery(model);
        pathQuery.addViews(PathQueryResultHelper.getDefaultViewForClass(className, model,
                webConfig, null));
        pathQuery.addConstraint(Constraints.lookup(className, extId, null));

        Map<String, BagQueryResult> returnBagQueryResults = new HashMap<String, BagQueryResult>();
        Profile profile = SessionMethods.getProfile(session);
        WebResultsExecutor executor = im.getWebResultsExecutor(profile);
        WebResults webResults = executor.execute(pathQuery, returnBagQueryResults);

        String bagName = NameUtil.generateNewName(profile.getSavedBags().keySet(), "link");
        List<Integer> bagList = new ArrayList<Integer>();

        // There's only one node, get the first value
        BagQueryResult bagQueryResult = returnBagQueryResults.values().iterator().next();
        bagList.addAll(bagQueryResult.getMatchAndIssueIds());

        DisplayLookupMessageHandler.handleMessages(bagQueryResult, session, properties, className,
                null);

        ActionMessages actionMessages = new ActionMessages();

        // Use custom converters
        Set<AdditionalConverter> additionalConverters =
            bagQueryConfig.getAdditionalConverters(className);
        if (additionalConverters != null) {
            for (AdditionalConverter additionalConverter : additionalConverters) {

                // constraint value, eg. organism name
                String extraValue = PortalHelper.getAdditionalParameter(request,
                        additionalConverter.getUrlField());

                if (StringUtils.isNotEmpty(extraValue)) {
                    BagConverter bagConverter = PortalHelper.getBagConverter(im, webConfig,
                            additionalConverter.getClassName());
                    List<Integer> converted = bagConverter.getConvertedObjectIds(profile,
                            className, bagList, extraValue);
                    // No matches
                    if (converted.size() <= 0) {
                        actionMessages.add(Constants.PORTAL_MSG,
                            new ActionMessage("portal.noorthologues", extraValue, extId));
                        session.setAttribute(Constants.PORTAL_MSG, actionMessages);
                        return goToResults(mapping, session, webResults);
                    }
                    actionMessages.add(Constants.PORTAL_MSG, bagConverter.getActionMessage(extId,
                            converted.size(), className, extraValue));
                    session.setAttribute(Constants.PORTAL_MSG, actionMessages);

                    if (converted.size() == 1) {
                        return goToReport(mapping, converted.get(0).toString());
                    }
                    InterMineBag imBag = profile.createBag(bagName, className, "",
                            im.getClassKeys());
                    return createBagAndGoToBagDetails(mapping, imBag, converted);
                }
            }
        }

        attachMessages(actionMessages, className, bagQueryResult.getMatches().size(),
                bagList.size(), extId);

        session.setAttribute(Constants.PORTAL_MSG, actionMessages);

        // more than one result but only one ID
        if ((bagList.size() > 1) && (idList.length == 1)) {
            return goToResults(mapping, session, webResults);
        // one ID searched for one ID found
        } else if ((bagList.size() == 1) && (idList.length == 1)) {
            return goToReport(mapping, bagList.get(0).toString());
        // lots of results, make a list
        } else if (bagList.size() >= 1) {
            InterMineBag imBag = profile.createBag(bagName, className, "", im.getClassKeys());
            return createBagAndGoToBagDetails(mapping, imBag, bagList);
        // No matches
        } else {
            return goToResults(mapping, session, webResults);
        }
    }

    private ActionForward goToResults(ActionMapping mapping, HttpSession session,
            WebResults webResults) {
        PagedTable pc = new PagedTable(webResults);
        String identifier = "col" + index++;
        SessionMethods.setResultsTable(session, identifier, pc);
        return new ForwardParameters(mapping.findForward("results"))
            .addParameter("table", identifier).addParameter("trail", "").forward();
    }

    private ActionForward goToReport(ActionMapping mapping, String id) {
        return new ForwardParameters(mapping.findForward("report"))
            .addParameter("id", id).forward();
    }

    private ActionForward goToNoResults(ActionMapping mapping, HttpSession session) {
        ActionForward forward = mapping.findForward("noResults");
        return new ForwardParameters(forward).addParameter("trail", "").forward();
    }

    private ActionForward createBagAndGoToBagDetails(ActionMapping mapping, InterMineBag imBag,
            List<Integer> bagList) throws ObjectStoreException {
        imBag.addIdsToBag(bagList, imBag.getType());
        return new ForwardParameters(mapping.findForward("bagDetails"))
            .addParameter("bagName", imBag.getName()).forward();
    }

    private void attachMessages(ActionMessages actionMessages, String className,
            int bagQueryResultSize,
            int bagListSize, String extId) {
        // Attach messages
        if (bagListSize == 0 && bagQueryResultSize == 1) {
            ActionMessage msg = new ActionMessage("results.lookup.noresults.one",
                    new Integer(bagQueryResultSize), className);
            actionMessages.add(Constants.PORTAL_MSG, msg);
        } else if (bagListSize == 0 && bagQueryResultSize > 1) {
            ActionMessage msg = new ActionMessage("results.lookup.noresults.many",
                    new Integer(bagQueryResultSize), className);
            actionMessages.add(Constants.PORTAL_MSG, msg);
        } else if (bagListSize > 0) {
            ActionMessage msg = new ActionMessage("results.lookup.matches.many",
                    new Integer(bagListSize));
            actionMessages.add(Constants.PORTAL_MSG, msg);
        } else if (bagListSize == 0) {
            ActionMessage msg = new ActionMessage("portal.nomatches", extId);
            actionMessages.add(Constants.PORTAL_MSG, msg);
        }
    }
}
