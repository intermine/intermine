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
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.bag.BagQueryResult;
import org.intermine.web.bag.BagQueryRunner;

/**
 * An action that makes a bag from text.
 *
 * @author Kim Rutherford
 */

public class BuildBagAction extends InterMineAction
{
    
    /**
     * Action for creating a bag of InterMineObjects or Strings from identifiers in text field.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception{
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        BuildBagForm buildBagForm = (BuildBagForm) form;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        String newBagName = buildBagForm.getBagName();
        String type = buildBagForm.getType();
        
        ObjectStore userProfileOs = ((ProfileManager) servletContext.getAttribute(Constants
                    .PROFILE_MANAGER)).getUserProfileObjectStore();
        
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        BagQueryRunner bagRunner = new BagQueryRunner(os, classKeys, new HashMap());
        
        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", 100000);

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
        List list = new ArrayList();
        while ((thisLine = reader.readLine()) != null) {

            int elementCount = 0;

            StringTokenizer st = new StringTokenizer(thisLine, " \n\t,");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                list.add(token);

                elementCount++;

                if (elementCount > maxBagSize) {
                    ActionMessage actionMessage =
                        new ActionMessage("bag.tooBig", new Integer(maxBagSize));
                    recordError(actionMessage, request);

                    return mapping.findForward("buildBag");
                }
            }
        }        

        
        
        
        BagQueryResult bagQueryResult = bagRunner.searchForBag(type, list);
        request.setAttribute("matches", bagQueryResult.getMatches());
        request.setAttribute("issues", bagQueryResult.getIssues());
        request.setAttribute("unresolved", bagQueryResult.getUnresolved());
        
        return new ForwardParameters(mapping.findForward("bagUploadConfirm"))
                .addParameter("bagName", newBagName)
                .forward();
    }
    
}