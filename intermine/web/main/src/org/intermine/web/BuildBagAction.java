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
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        BuildBagForm buildBagForm = (BuildBagForm) form;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        String newBagName = buildBagForm.getBagName();
        String type = buildBagForm.getType();
        
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        BagQueryRunner bagRunner = new BagQueryRunner(os, classKeys, new HashMap());
        
        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", 100000);

        BufferedReader reader = null;

        if (request.getParameter("paste") != null) {
        	String trimmedText = buildBagForm.getText().trim();
        	if (trimmedText.length() == 0) {
        		recordError(new ActionMessage("bagBuild.noBagPaste"), request);
        		return mapping.findForward("mymine");
        	} else {
        		reader = new BufferedReader(new StringReader(trimmedText));
        	}
        } else if (request.getParameter("file") != null) {
        	FormFile formFile = buildBagForm.getFormFile();
        	if (formFile == null
        			|| formFile.getFileName() == null || formFile.getFileName().length() == 0) {
        		recordError(new ActionMessage("bagBuild.noBagFile"), request);
        		return mapping.findForward("mymine");
        	} else {
        		reader = new BufferedReader(new InputStreamReader(formFile.getInputStream()));
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

                    return mapping.findForward("mymine");
                }
            }
        }

        BagQueryResult bagQueryResult = bagRunner.searchForBag(type, list);
        session.setAttribute("bagQueryResult", bagQueryResult);
        request.setAttribute("bagName", newBagName);
        request.setAttribute("bagType", type);
        
        return mapping.findForward("bagUploadConfirm");
    }
}
