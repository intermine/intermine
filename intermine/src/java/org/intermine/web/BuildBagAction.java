package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

/**
 * An action that makes a bag from text.
 *
 * @author Kim Rutherford
 */

public class BuildBagAction extends InterMineLookupDispatchAction
{
    /**
     * Action for creating a bag of Strings from a text field.
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
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        BuildBagForm buildBagForm = (BuildBagForm) form;

        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", 100000);

        InterMineBag bag = new InterMineBag();
        String trimmedText = buildBagForm.getText().trim();
        FormFile formFile = buildBagForm.getFormFile();

        if (trimmedText.length() == 0) {
            if (formFile.getFileName() == null || formFile.getFileName().length() == 0) {
                recordError(new ActionMessage("bagBuild.noBagToSave"), request);

                return mapping.findForward("buildBag");
            } else {
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(formFile.getInputStream()));

                StringBuffer buffer = new StringBuffer();
                String thisLine;
                while ((thisLine = reader.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(thisLine, " \n\t");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        bag.add(token);

                        if (bag.size() > maxBagSize) {
                            ActionMessage actionMessage =
                                new ActionMessage("bag.tooBig", new Integer(maxBagSize));
                            recordError(actionMessage, request);

                            return mapping.findForward("buildBag");
                        }
                    }
                }
            }
        } else {
            if (formFile.getFileName() == null || formFile.getFileName().length() == 0) {
                StringTokenizer st = new StringTokenizer(trimmedText, " \n\t");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    bag.add(token);

                    if (bag.size() > maxBagSize) {
                        ActionMessage actionMessage =
                            new ActionMessage("bag.tooBig", new Integer(maxBagSize));
                        recordError(actionMessage, request);

                        return mapping.findForward("buildBag");
                    }

                }
            } else {
                recordError(new ActionMessage("bagBuild.textAndFilePresent"), request);

                return mapping.findForward("buildBag");
            }
        }

        String newBagName = buildBagForm.getBagName();
        profile.saveBag(newBagName, bag);

        recordMessage(new ActionMessage("bagBuild.saved", newBagName), request);

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
        return map;
    }
}
