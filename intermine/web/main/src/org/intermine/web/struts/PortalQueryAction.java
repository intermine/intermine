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
import java.util.ArrayList;
import java.util.Date;
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
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.query.WebResultsExecutor;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

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
    public ActionForward execute(ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String extId = request.getParameter("externalid");
        String origin = request.getParameter("origin");
        String className = request.getParameter("class");
        //String organism = request.getParameter("organism");
        if ((extId == null) || (extId.length() <= 0)) {
            extId = request.getParameter("externalids");
        }
        // Add a message to welcome the user
        Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String welcomeMsg = properties.getProperty("portal.welcome." + origin);
        if (welcomeMsg == null || welcomeMsg.length() == 0) {
            welcomeMsg = properties.getProperty("portal.welcome");
        }
        SessionMethods.recordMessage(welcomeMsg, session);

        if (extId == null || extId.length() == 0) {
            recordError(new ActionMessage("errors.badportalquery"), request);
            return mapping.findForward("failure");
        }
        ActionMessages actionMessages = new ActionMessages();
        session.setAttribute(Constants.PORTAL_QUERY_FLAG, Boolean.TRUE);

        // Set collapsed/uncollapsed state of object details UI
        // TODO This might not be used anymore
        Map<String, Boolean> collapsed = SessionMethods.getCollapsedMap(session);
        collapsed.put("fields", Boolean.TRUE);
        collapsed.put("further", Boolean.FALSE);
        collapsed.put("summary", Boolean.FALSE);

        session.setAttribute(Constants.PORTAL_QUERY_FLAG, Boolean.TRUE);

        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        String[] idList = extId.split(",");

        // Use the old way = quicksearch template in case some people used to link in
        // without class name
        if ((idList.length == 1) && (className == null || className.length() == 0)) {
            String qid = loadObjectDetails(servletContext, session, request, response,
                                           profile.getUsername(), extId, origin);
            return new ForwardParameters(mapping.findForward("waiting"))
            .addParameter("qid", qid).forward();
        }

        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Model model = os.getModel();
        BagQueryConfig bagQueryConfig =
                (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG);

        // If the class is not in the model, we can't continue
        try {
            className = StringUtil.capitalise(className);
            Class.forName(model.getPackageName() + "." + className);
        } catch (ClassNotFoundException clse) {
            recordError(new ActionMessage("errors.badportalclass"), request);
            return goToNoResults(mapping, session);
        }
        ObjectStoreWriter uosw = profile.getProfileManager().getProfileObjectStoreWriter();
        String bagName = null;
        Map<String, InterMineBag> profileBags = profile.getSavedBags();
        boolean bagExists = true;
        int number = 0;
        while (bagExists) {
            bagName = "link";
            if (origin != null) {
                bagName += origin;
            }
            bagName += "_" + number;
            bagExists = false;
            for (String name : profileBags.keySet()) {
                if (bagName.equals(name)) {
                    bagExists = true;
                }
            }
            number++;
        }
        // NEW STUFF
        PathQuery pathQuery = new PathQuery(model);

        List<Path> view = PathQueryResultHelper.getDefaultView(className, model, webConfig,
            null, true);

        pathQuery.setViewPaths(view);
        String label = null, id = null, code = pathQuery.getUnusedConstraintCode();
        Constraint c = new Constraint(ConstraintOp.LOOKUP, StringUtils.replace(extId, ",", "\t"),
                        false, label, code, id, null);
        pathQuery.addNode(className).getConstraints().add(c);
        pathQuery.setConstraintLogic("A and B and C");
        pathQuery.syncLogicExpression("and");

        Map<String, BagQueryResult> returnBagQueryResults = new HashMap<String, BagQueryResult>();
        
        WebResultsExecutor executor = SessionMethods.getWebResultsExecutor(session);
        WebResults webResults = executor.execute(pathQuery, returnBagQueryResults);

        InterMineBag imBag = new InterMineBag(bagName,
                        className , null , new Date() ,
                        os , profile.getUserId() , uosw);

        List <Integer> bagList = new ArrayList <Integer> ();

        // There's only one node, get the first value
        BagQueryResult bagQueryResult = returnBagQueryResults.values().iterator().next();
        bagList.addAll(bagQueryResult.getMatchAndIssueIds());

        DisplayLookupMessageHandler.handleMessages(bagQueryResult, session,
                                                   properties, className, null);

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
                //String urlField = null;
                for (int i = 0; i < urlFields.length; i++) {
                    if (request.getParameter(urlFields[i]) != null) {
                        addparameter = request.getParameter(urlFields[i]);
                        //urlField = urlFields[i];
                        break;
                    }
                }
                if (addparameter != null && addparameter.length() != 0) {
                    BagConverter bagConverter = (BagConverter) constructor.newInstance();
                    //ObjectStoreSummary oss = (ObjectStoreSummary) servletContext
                    //                          .getAttribute(Constants.OBJECT_STORE_SUMMARY);

                    WebResults convertedWebResult = bagConverter.getConvertedObjects(session,
                        addparameter, bagList, className);
                    imBag = new InterMineBag(bagName, className, null, new Date(), os,
                                             profile.getUserId(), uosw);
                    List<Integer> converted = new ArrayList<Integer>();
                    for (List resRow : convertedWebResult) {
                        ResultElement resElement = (ResultElement) resRow.get(0);
                        Object obj = resElement.getObject();
                        if (obj instanceof InterMineObject) {
                            converted.add(((InterMineObject) obj).getId());
                        }
                    }
                    // No matches
                    if (converted.size() <= 0) {
                        actionMessages.add(Constants.PORTAL_MSG,
                            new ActionMessage("portal.nomatches.orthologues", addparameter, extId));
                        session.setAttribute(Constants.PORTAL_MSG, actionMessages);
                        return goToResults(mapping, session, webResults);
                    }
                    actionMessages.add(Constants.PORTAL_MSG, bagConverter.getActionMessage(model,
                        extId, converted.size(), className, addparameter));
                    session.setAttribute(Constants.PORTAL_MSG, actionMessages);

                    // Object details
                    if (converted.size() == 1) {
                        return goToObjectDetails(mapping, converted.get(0).toString());
                        // Bag Details
                    }
                    return goToBagDetails(mapping, os, imBag, converted, profile);
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
            return goToBagDetails(mapping, os, imBag, bagList, profile);
        // No matches
        } else {
            return goToResults(mapping, session, webResults);
        }
    }

    private ActionForward goToBagDetails(ActionMapping mapping, ObjectStore os, InterMineBag imBag,
                                         List bagList, Profile profile)
                                         throws ObjectStoreException {
        ObjectStoreWriter osw = new ObjectStoreWriterInterMineImpl(os);
        osw.addAllToBag(imBag.getOsb(), bagList);
        osw.close();
        profile.saveBag(imBag.getName(), imBag);
        return new ForwardParameters(mapping.findForward("bagDetails"))
        .addParameter("bagName", imBag.getName()).forward();
    }

    private ActionForward goToResults(ActionMapping mapping,
                                      HttpSession session, WebResults webResults) {
        PagedTable pc = new PagedTable(webResults);
        String identifier = "col" + index++;
        SessionMethods.setResultsTable(session, identifier, pc);
        return new ForwardParameters(mapping.findForward("results"))
        .addParameter("table", identifier)
        .addParameter("trail", "").forward();
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

    /**
     * @deprecated Uses the BagQueryRunner instead
     */
    private String loadObjectDetails(ServletContext servletContext,
                                                HttpSession session, HttpServletRequest request,
                                                HttpServletResponse response, String userName,
                                                String extId,
                                                @SuppressWarnings("unused") String origin)
                                                throws InterruptedException {
        Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String templateName = properties.getProperty("begin.browse.template");
        Integer op = ConstraintOp.EQUALS.getIndex();
        TemplateQuery template = TemplateHelper.findTemplate(servletContext, session, userName,
                                                             templateName, "global");

        if (template == null) {
            throw new IllegalStateException("Could not find template \"" + templateName + "\"");
        }

        // Populate template form bean
        TemplateForm tf = new TemplateForm();
        tf.setAttributeOps("1", op.toString());
        tf.setAttributeValues("1", extId);
        tf.parseAttributeValues(template, session, new ActionErrors(), false);

        // Convert form to path query
        PathQuery queryCopy = TemplateHelper.templateFormToTemplateQuery(tf, template,
                                                                         new HashMap());
        // Convert path query to intermine query
        SessionMethods.loadQuery(queryCopy, request.getSession(), response);

        QueryMonitorTimeout clientState
                = new QueryMonitorTimeout(Constants.QUERY_TIMEOUT_SECONDS * 1000);
        MessageResources messages = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
        String qid = SessionMethods.startQuery(clientState, session, messages, false, queryCopy);
        Thread.sleep(200); // slight pause in the hope of avoiding holding page
        return qid;
    }

}
