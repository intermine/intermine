package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.ResultElement;
import org.intermine.api.results.WebResults;
import org.intermine.api.results.flatouterjoins.MultiRow;
import org.intermine.api.results.flatouterjoins.MultiRowValue;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.util.NameUtil;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.query.QueryMonitorTimeout;
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
//    private static final Logger LOG = Logger.getLogger(PortalQueryAction.class);
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
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ServletContext servletContext = session.getServletContext();
        String extId = request.getParameter("externalid");
        String origin = request.getParameter("origin");
        String className = request.getParameter("class");
        //String organism = request.getParameter("organism");
        if ((extId == null) || (extId.length() <= 0)) {
            extId = request.getParameter("externalids");
        }
        // Add a message to welcome the user
        Properties properties = SessionMethods.getWebProperties(servletContext);
        String welcomeMsg = properties.getProperty("portal.welcome." + origin);
        if (StringUtil.isEmpty(welcomeMsg)) {
            welcomeMsg = properties.getProperty("portal.welcome");
        }
        SessionMethods.recordMessage(welcomeMsg, session);

        if (extId == null || extId.length() == 0) {
            recordError(new ActionMessage("errors.badportalquery"), request);
            return mapping.findForward("failure");
        }

        session.setAttribute(Constants.PORTAL_QUERY_FLAG, Boolean.TRUE);

        // Set collapsed/uncollapsed state of object details UI
        // TODO This might not be used anymore
        Map<String, Boolean> collapsed = SessionMethods.getCollapsedMap(session);
        collapsed.put("fields", Boolean.TRUE);
        collapsed.put("further", Boolean.FALSE);
        collapsed.put("summary", Boolean.FALSE);

        Profile profile = SessionMethods.getProfile(session);
        String[] idList = extId.split(",");

        // Use the old way = quicksearch template in case some people used to link in
        // without class name
        if ((idList.length == 1) && (className == null || className.length() == 0)) {
            String qid = loadObjectDetails(servletContext, session, request, response,
                                           profile.getUsername(), extId, origin);
            return new ForwardParameters(mapping.findForward("waiting"))
                .addParameter("qid", qid).forward();
        }

        Model model = im.getModel();
        WebConfig webConfig = SessionMethods.getWebConfig(request);
        BagQueryConfig bagQueryConfig = im.getBagQueryConfig();

        // If the class is not in the model, we can't continue
        try {
            className = StringUtil.capitalise(className);
            Class.forName(model.getPackageName() + "." + className);
        } catch (ClassNotFoundException clse) {
            recordError(new ActionMessage("errors.badportalclass"), request);
            return goToNoResults(mapping, session);
        }
        String bagName = NameUtil.generateNewName(profile.getSavedBags().keySet(), "link");

        PathQuery pathQuery = new PathQuery(model);
        List<Path> view = PathQueryResultHelper.getDefaultView(className, model, webConfig, null,
                true);
        pathQuery.setViewPaths(view);
        pathQuery.addConstraint(className, Constraints.lookup(StringUtils.replace(extId, ",",
                        "\t")));

        Map<String, BagQueryResult> returnBagQueryResults = new HashMap();
        WebResultsExecutor executor = im.getWebResultsExecutor(profile);
        WebResults webResults = executor.execute(pathQuery, returnBagQueryResults);

        InterMineBag imBag = profile.createBag(bagName, className, "");
        List<Integer> bagList = new ArrayList();

        // There's only one node, get the first value
        BagQueryResult bagQueryResult = returnBagQueryResults.values().iterator().next();
        bagList.addAll(bagQueryResult.getMatchAndIssueIds());

        DisplayLookupMessageHandler.handleMessages(bagQueryResult, session, properties, className,
                null);

        ActionMessages actionMessages = new ActionMessages();

        // Use custom converters
        Map<String, String []> additionalConverters =
            bagQueryConfig.getAdditionalConverters(imBag.getType());
        if (additionalConverters != null) {
            for (String converterClassName : additionalConverters.keySet()) {
                Class clazz = Class.forName(converterClassName);
                Constructor constructor = clazz.getConstructor();
                String [] paramArray = additionalConverters.get(converterClassName);
                String [] urlFields = paramArray[0].split(",");
                String addparameter = null;
                for (int i = 0; i < urlFields.length; i++) {
                    if (request.getParameter(urlFields[i]) != null) {
                        addparameter = request.getParameter(urlFields[i]);
                        // the spaces in organisms, eg. D.%20rerio, need to be handled
                        URLDecoder.decode(addparameter, "UTF-8");
                        break;
                    }
                }
                if (addparameter != null && addparameter.length() != 0) {
                    BagConverter bagConverter = (BagConverter) constructor.newInstance();
                    WebResults convertedWebResult = bagConverter.getConvertedObjects(session,
                        addparameter, bagList, className);
                    imBag = profile.createBag(bagName, className, "");
                    List<Integer> converted = new ArrayList<Integer>();
                    for (MultiRow<ResultsRow<MultiRowValue<ResultElement>>> resRow
                            : convertedWebResult) {
                        ResultElement resElement = resRow.get(0).get(0).getValue();
                        Object obj = resElement.getObject();
                        if (obj instanceof InterMineObject) {
                            converted.add(((InterMineObject) obj).getId());
                        }
                    }
                    // No matches
                    if (converted.size() <= 0) {
                        actionMessages.add(Constants.PORTAL_MSG,
                            new ActionMessage("portal.noorthologues", addparameter, extId));
                        session.setAttribute(Constants.PORTAL_MSG, actionMessages);
                        return goToResults(mapping, session, webResults);
                    }
                    actionMessages.add(Constants.PORTAL_MSG, bagConverter.getActionMessage(model,
                        extId, converted.size(), className, addparameter));
                    session.setAttribute(Constants.PORTAL_MSG, actionMessages);

                    if (converted.size() == 1) {
                        return goToObjectDetails(mapping, converted.get(0).toString());
                    }
                    return createBagAndGoToBagDetails(mapping, imBag, converted);
                }
            }
        }
        // Attach messages
        if (bagList.size() == 0 && bagQueryResult.getMatches().size() == 1) {
            ActionMessage msg = new ActionMessage("results.lookup.noresults.one",
                                                  new Integer(bagQueryResult.getMatches().size()),
                                                  className);
            actionMessages.add(Constants.PORTAL_MSG, msg);
        } else if (bagList.size() == 0 && bagQueryResult.getMatches().size() > 1) {
            ActionMessage msg = new ActionMessage("results.lookup.noresults.many",
                                                  new Integer(bagQueryResult.getMatches().size()),
                                                  className);
            actionMessages.add(Constants.PORTAL_MSG, msg);
        } else if (bagList.size() > 0) {
            ActionMessage msg = new ActionMessage("results.lookup.matches.many",
                                                  new Integer(bagList.size()));
            actionMessages.add(Constants.PORTAL_MSG, msg);
        } else if (bagList.size() == 0) {
            ActionMessage msg = new ActionMessage("portal.nomatches", extId);
            actionMessages.add(Constants.PORTAL_MSG, msg);
        }
        session.setAttribute(Constants.PORTAL_MSG, actionMessages);

        // Go to results page
        if ((bagList.size() > 1) && (idList.length == 1)) {
            return goToResults(mapping, session, webResults);
        // Go to the object details page
        } else if ((bagList.size() == 1) && (idList.length == 1)) {
            return goToObjectDetails(mapping, bagList.get(0).toString());
        // Make a bag
        } else if (bagList.size() >= 1) {
            return createBagAndGoToBagDetails(mapping, imBag, bagList);
        // No matches
        } else {
            return goToResults(mapping, session, webResults);
        }
    }

    private ActionForward goToResults(ActionMapping mapping,
                                      HttpSession session, WebResults webResults) {
        PagedTable pc = new PagedTable(webResults);
        String identifier = "col" + index++;
        SessionMethods.setResultsTable(session, identifier, pc);
        return new ForwardParameters(mapping.findForward("results"))
            .addParameter("table", identifier).addParameter("trail", "").forward();
    }

    private ActionForward goToObjectDetails(ActionMapping mapping, String id) {
        return new ForwardParameters(mapping.findForward("objectDetails"))
            .addParameter("id", id).forward();
    }

    private ActionForward goToNoResults(ActionMapping mapping,
            @SuppressWarnings("unused") HttpSession session) {
        ActionForward forward = mapping.findForward("noResults");
        return new ForwardParameters(forward).addParameter("trail", "").forward();
    }

    private ActionForward createBagAndGoToBagDetails(ActionMapping mapping, InterMineBag imBag,
            List<Integer> bagList) throws ObjectStoreException {
        imBag.addIdsToBag(bagList, imBag.getType());
        return new ForwardParameters(mapping.findForward("bagDetails"))
            .addParameter("bagName", imBag.getName()).forward();
    }

    /**
     * @deprecated Use the BagQueryRunner instead
     */
    private String loadObjectDetails(ServletContext servletContext, HttpSession session,
            HttpServletRequest request, HttpServletResponse response, String userName,
            String extId, @SuppressWarnings("unused") String origin) throws InterruptedException {
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Properties properties = SessionMethods.getWebProperties(servletContext);
        String templateName = properties.getProperty("begin.browse.template");
        TemplateManager templateManager = im.getTemplateManager();
        TemplateQuery template = templateManager.getGlobalTemplate(templateName);

        if (template == null) {
            throw new IllegalStateException("Could not find template \"" + templateName + "\"");
        }

        TemplateQuery populatedTemplate = TemplatePopulator.populateTemplateOneConstraint(template,
                ConstraintOp.EQUALS, extId);

        SessionMethods.loadQuery(populatedTemplate, request.getSession(), response);

        QueryMonitorTimeout clientState
            = new QueryMonitorTimeout(Constants.QUERY_TIMEOUT_SECONDS * 1000);
        MessageResources messages = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
        String qid = SessionMethods.startQuery(clientState, session, messages, false,
                populatedTemplate);
        Thread.sleep(200); // slight pause in the hope of avoiding holding page
        return qid;
    }
}
