package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.MessageResources;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.path.Path;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.results.PagedTable;
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
        String organism = request.getParameter("organism");
        if ((extId == null) || (extId.length() <= 0)) {
            extId = request.getParameter("externalids");
        }
        // Add a message to welcome the user
        Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String welcomeMsg = properties.getProperty("portal.welcome" + origin);
        if (welcomeMsg == null || welcomeMsg.length() == 0) {
            welcomeMsg = properties.getProperty("portal.welcome");
        }
        SessionMethods.recordMessage(welcomeMsg, session);
 
        if (extId == null || extId.length() == 0) {
            recordError(new ActionMessage("errors.badportalquery"), request);
            return mapping.findForward("failure");
        }
        origin = "." + origin;

        session.setAttribute(Constants.PORTAL_QUERY_FLAG, Boolean.TRUE);

        // Set collapsed/uncollapsed state of object details UI
        // TODO This might not be used anymore
        Map collapsed = SessionMethods.getCollapsedMap(session);
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
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        Model model = os.getModel();
        BagQueryConfig bagQueryConfig = 
                (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG);
        BagQueryRunner bagRunner =
                new BagQueryRunner(os, classKeys, bagQueryConfig, servletContext);

        // If the class is not in the model, we can't continue
        try {
            className = StringUtil.capitalise(className);
            Class.forName(model.getPackageName() + "." + className);
        } catch (ClassNotFoundException clse) {
            recordError(new ActionMessage("errors.badportalclass"), request);
            return mapping.findForward("results");
        }
        ObjectStoreWriter uosw = profile.getProfileManager().getUserProfileObjectStore();
        String bagName = null;
        Map<String, InterMineBag> profileBags = profile.getSavedBags();
        boolean bagExists = true;
        int number = 0;
        while (bagExists) {
            bagName = "temp" + origin + "_" + number;
            bagExists = false;
            for (String name : profileBags.keySet()) {
                if (bagName.equals(name)) {
                    bagExists = true;
                }
            }
            number++;
        }
        InterMineBag imBag = new InterMineBag(bagName, 
                                              className , null , new Date() ,
                                              os , profile.getUserId() , uosw);
        
        BagQueryResult bagQueryResult = 
            bagRunner.searchForBag(className, Arrays.asList(idList), organism, false);
        
        List <Integer> bagList = new ArrayList <Integer> ();
        bagList.addAll(bagQueryResult.getMatchAndIssueIds());
        int matches = bagQueryResult.getMatchAndIssueIds().size();
        Set<String> unresolved = bagQueryResult.getUnresolved().keySet();
        Set<String> duplicates = new HashSet<String>();
        Set<String> lowQuality = new HashSet<String>();
        Set<String> translated = new HashSet<String>();
        Map<String, List> wildcards = new HashMap<String, List>();
        Map<String, Map<String, List>> duplicateMap = bagQueryResult.getIssues().get(BagQueryResult
                .DUPLICATE);
        if (duplicateMap != null) {
            for (Map.Entry<String, Map<String, List>> queries : duplicateMap.entrySet()) {
                duplicates.addAll(queries.getValue().keySet());
            }
        }
        Map<String, Map<String, List>> translatedMap = bagQueryResult.getIssues().get(BagQueryResult
                .TYPE_CONVERTED);
        if (translatedMap != null) {
            for (Map.Entry<String, Map<String, List>> queries : translatedMap.entrySet()) {
                translated.addAll(queries.getValue().keySet());
            }
        }
        Map<String, Map<String, List>> lowQualityMap = bagQueryResult.getIssues().get(BagQueryResult
                .OTHER);
        if (lowQualityMap != null) {
            for (Map.Entry<String, Map<String, List>> queries : lowQualityMap.entrySet()) {
                lowQuality.addAll(queries.getValue().keySet());
            }
        }
        Map<String, Map<String, List>> wildcardMap = bagQueryResult.getIssues().get(BagQueryResult
                                                          .WILDCARD);
        if (wildcardMap != null) {
            for (Map.Entry<String, Map<String, List>> queries : wildcardMap.entrySet()) {
                wildcards.putAll(queries.getValue());
            }
        }
        
        DisplayLookup displayLookup = new DisplayLookup(className, matches, unresolved, 
                                                        duplicates, translated, 
                          lowQuality, wildcards, null);

        List<DisplayLookup> lookupResults = new ArrayList<DisplayLookup>();
        lookupResults.add(displayLookup);
        request.setAttribute("lookupResults", lookupResults);

        // Go to the object details page
        if ((bagList.size() == 1) && (idList.length == 1)) {
            return new ForwardParameters(mapping.findForward("objectDetails"))
            .addParameter("id", bagList.get(0).toString()).forward();
        /// Go to results page
        } else if ((idList.length == 1)) {
              List intermineObjectList = os.getObjectsByIds(bagList);
              WebPathCollection webPathCollection = 
                  new WebPathCollection(os, new Path(model, className), intermineObjectList
                                        , model, webConfig,
                                        classKeys);
              PagedTable pc = new PagedTable(webPathCollection);
              String identifier = "col" + index++;
              SessionMethods.setResultsTable(session, identifier, pc);
              return new ForwardParameters(mapping.findForward("results"))
              .addParameter("table", identifier)
              .addParameter("size", "10")
              .addParameter("trail", "").forward();
        // Make a bag
        } else if (bagList.size() > 1) {
            ObjectStoreWriter osw = new ObjectStoreWriterInterMineImpl(os);
            osw.addAllToBag(imBag.getOsb(), bagList);
            osw.close();
            profile.saveBag(imBag.getName(), imBag);
            return new ForwardParameters(mapping.findForward("bagDetails"))
            .addParameter("bagName", imBag.getName()).forward();
        // No matches
        } else {
            recordMessage(new ActionMessage("portal.nomatches", extId), request);

            WebPathCollection webPathCollection = 
                new WebPathCollection(os, new Path(model, className), new ArrayList()
                                      , model, webConfig,
                                  classKeys);
            PagedTable pc = new PagedTable(webPathCollection);
            String identifier = "col" + index++;
            SessionMethods.setResultsTable(session, identifier, pc);
            return new ForwardParameters(mapping.findForward("results"))
            .addParameter("noSelect", "true")
            .addParameter("table", identifier)
            .addParameter("size", "10")
            .addParameter("trail", "").forward();
        }
    }
    
    private String loadObjectDetails(ServletContext servletContext,
                                                HttpSession session, HttpServletRequest request,
                                                HttpServletResponse response, String userName, 
                                                String extId, String origin) 
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
