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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.intermine.web.logic.bag.ConvertedObjectPair;
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
 * At the moment the action expects 'class' and 'externalid' parameters
 * the it performs some sensible query and redirects the user to the
 * results page or a tailored 'portal' page (at the moment it just goes
 * to the object details page).
 *
 * @author Thomas Riley
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
        if (extId == null || extId.length() == 0) {
            recordError(new ActionMessage("errors.badportalquery"), request);
            return mapping.findForward("failure");
        }
        if (origin == null) {
            origin = "";
        } else if (origin.length() > 0) {
            origin = "." + origin;
        }
        session.setAttribute(Constants.PORTAL_QUERY_FLAG, Boolean.TRUE);
        // Add a message to welcome the user
        Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        SessionMethods.recordMessage(properties.getProperty("portal.welcome" + origin), session);

        // Set collapsed/uncollapsed state of object details UI
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
            return mapping.findForward("failure");
        }
        ObjectStoreWriter uosw = profile.getProfileManager().getUserProfileObjectStore();
        InterMineBag imBag = new InterMineBag(origin + System.currentTimeMillis(), 
                                              className , null , new Date() ,
                                              os , profile.getUserId() , uosw);
        
        BagQueryResult bagQueryResult = 
            bagRunner.searchForBag(className, Arrays.asList(idList), organism, false);
        
        List <Integer> bagList = new ArrayList <Integer> ();
        bagList.addAll(bagQueryResult.getMatches().keySet());
        // If there are no exact matches, add converted
        if (bagList.size() == 0) {
            Map issues = bagQueryResult.getIssues();
            if (issues != null && issues.size() != 0) {
                Map converted = (Map) issues.get(BagQueryResult.TYPE_CONVERTED);
                for (Iterator iter = converted.values().iterator(); iter.hasNext();) {
                    Map queryMap = (Map) iter.next();
                    for (Iterator iterator = queryMap.values().iterator(); iterator.hasNext();) {
                        List <ConvertedObjectPair> convertedPairList = 
                            (ArrayList<ConvertedObjectPair>) iterator.next();
                        for (ConvertedObjectPair convertedObjPair : convertedPairList) {
                            bagList.add(convertedObjPair.getNewObject().getId());
                        }
                    }
                }
            }
        }
        // Go to the object details page
        if ((bagList.size() == 1) && (idList.length == 1)) {
            return new ForwardParameters(mapping.findForward("objectDetails"))
            .addParameter("id", bagList.get(0).toString()).forward();
        // More than one matches for single identifier
        } else if ((bagList.size() > 1) && (idList.length == 1)) {
            List intermineObjectList = os.getObjectsByIds(bagQueryResult.getMatches().keySet());
            WebPathCollection webPathCollection = 
                new WebPathCollection(os, new Path(model, className), intermineObjectList
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
        // Make a bag
        } else {
            ObjectStoreWriter osw = new ObjectStoreWriterInterMineImpl(os);
            osw.addAllToBag(imBag.getOsb(), bagList);
            osw.close();
            profile.saveBag(imBag.getName(), imBag);
            return new ForwardParameters(mapping.findForward("bagDetails"))
            .addParameter("bagName", imBag.getName()).forward();
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