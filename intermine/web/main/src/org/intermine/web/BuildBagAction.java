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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

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
        return makeBag(mapping, form, request, null);
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
        return makeBag(mapping, form, request, templateName);
    }

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
    private ActionForward makeBag(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 String converterTemplateName)
        throws Exception {
        throw new RuntimeException("not implemented");
        /*
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        BuildBagForm buildBagForm = (BuildBagForm) form;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        String newBagName = buildBagForm.getBagName();
        ObjectStore userProfileOs = ((ProfileManager) servletContext.getAttribute(Constants
                    .PROFILE_MANAGER)).getUserProfileObjectStore();
        
        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", 100000);

        InterMineIdBag identifierBag = new InterMinePrimitiveBag(profile.getUserId(), newBagName,
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

        int lineCount = 0;
        String thisLine;
        while ((thisLine = reader.readLine()) != null) {
            List list = new ArrayList();

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
            
            if (list.size() > 0) {
                lineCount++;
                identifierBag.add(list);
            }
        }        

        if (identifierBag.width() > 10 && lineCount == 1) {
            // flatten the bag into one column - the user is unlikely to want a single row bag
            Collection collection = (Collection) identifierBag.asListOfLists().get(0);
            identifierBag =  new InterMinePrimitiveBag(profile.getUserId(), newBagName, 
                                                       new Integer(identifierBag.width()), os);
            identifierBag.addAll(collection);
        }
        
        InterMineIdBag bagToSave = null;
        bagToSave = identifierBag;
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
        */
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
