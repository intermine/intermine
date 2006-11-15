package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

import org.intermine.InterMineException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMineIdBag;
import org.intermine.web.bag.InterMinePrimitiveBag;

/**
 * An action that makes a bag from text.
 *
 * @author Kim Rutherford
 */

public class BuildBagAction extends InterMineLookupDispatchAction
{
    /**
     * Action for creating a bag of Strings from identifiers in text field.
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
    public ActionForward makeStringBag(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception {
        return makeBag(mapping, form, request, response, null);
    }

    /**
     * Action for creating a bag of InterMineObjects or Strings from identifiers in text field.
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
    public ActionForward makeObjectBag(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String templateName = properties.getProperty("begin.browse.template");
        return makeBag(mapping, form, request, response, templateName);
    }

    /**
     * Action for creating a bag of InterMineObjects or Strings from identifiers in text field.
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
    private ActionForward makeBag(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 String converterTemplateName)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        BuildBagForm buildBagForm = (BuildBagForm) form;
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        String newBagName = buildBagForm.getBagName();
        ObjectStore userProfileOs = ((ProfileManager) servletContext.getAttribute(Constants
                    .PROFILE_MANAGER)).getUserProfileObjectStore();
        
        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", 100000);

        InterMineBag identifierBag = new InterMinePrimitiveBag(profile.getUserId(), newBagName,
                userProfileOs, Collections.EMPTY_SET);
        String trimmedText = buildBagForm.getText().trim();
        FormFile formFile = buildBagForm.getFormFile();

        BufferedReader reader = null;
        
        if (trimmedText.length() == 0) {
            if (formFile == null
                || formFile.getFileName() == null || formFile.getFileName().length() == 0) {
                recordError(new ActionMessage("bagBuild.noBagToSave"), request);
                return mapping.findForward("buildBag");
            } else {
                reader = new BufferedReader(new InputStreamReader(formFile.getInputStream()));
            }
        } else {
            if (formFile == null
                || formFile.getFileName() == null || formFile.getFileName().length() == 0) {
                reader = new BufferedReader(new StringReader(trimmedText));
            } else {
                recordError(new ActionMessage("bagBuild.textAndFilePresent"), request);
                return mapping.findForward("buildBag");
            }
        }

        String thisLine;
        while ((thisLine = reader.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(thisLine, " \n\t,");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                identifierBag.add(token);

                if (identifierBag.size() > maxBagSize) {
                    ActionMessage actionMessage =
                        new ActionMessage("bag.tooBig", new Integer(maxBagSize));
                    recordError(actionMessage, request);

                    return mapping.findForward("buildBag");
                }
            }
        }        

        InterMineBag bagToSave = null;
        
        if (converterTemplateName != null) {
            String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
            Integer inOp = ConstraintOp.IN.getIndex();
            TemplateQuery template =
                TemplateHelper.findTemplate(servletContext, session, userName, 
                                            converterTemplateName, "global");

            if (template == null) {
                throw new IllegalStateException("Could not find template \"" 
                                                + converterTemplateName + "\"");
            }

            InterMineIdBag idBag = new InterMineIdBag(profile.getUserId(), newBagName,
                    userProfileOs, Collections.EMPTY_SET);

            String queryBagName = "id_bag_name";

            // Map from bag name to bag for passing to makeQuery
            Map bagMap = new HashMap();
            bagMap.put(queryBagName, identifierBag);
            
            // Populate template form bean
            TemplateForm tf = new TemplateForm();
            tf.setBagOp("1", inOp.toString());
            tf.setBag("1", queryBagName);
            
            tf.setUseBagConstraint("1", true);
            tf.parseAttributeValues(template, session, new ActionErrors(), false);

            PathQuery pathQuery = TemplateHelper.templateFormToTemplateQuery(tf, template);
            Query query = MainHelper.makeQuery(pathQuery, bagMap);
            Results results = os.execute(query);

            Iterator resultsIter = results.iterator();
            
            while (resultsIter.hasNext()) {
                ResultsRow rr = (ResultsRow) resultsIter.next();
                InterMineObject o = (InterMineObject) (rr.get(0));
                
                idBag.add(o.getId().intValue());
            }

            bagToSave = idBag;
        } else {
            bagToSave = identifierBag;
        }

        int maxNotLoggedSize = WebUtil.getIntSessionProperty(session, "max.bag.size.notloggedin",
                                                             Constants.MAX_NOT_LOGGED_BAG_SIZE);
        try {
            profile.saveBag(newBagName, bagToSave, maxNotLoggedSize);
        } catch (InterMineException e) {
            recordError(new ActionMessage(e.getMessage(), String.valueOf(maxNotLoggedSize)),
                        request);
            return mapping.findForward("buildBag");
        }

        recordMessage(new ActionMessage("bagBuild.saved", newBagName,
                                        new Integer(bagToSave.size())), request);

        return mapping.findForward("buildBag");
    }

    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("bagBuild.makeStringBag", "makeStringBag");
        map.put("bagBuild.makeObjectBag", "makeObjectBag");
        return map;
    }
}
