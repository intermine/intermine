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
import java.util.LinkedHashMap;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionError;
import org.apache.struts.actions.LookupDispatchAction;

/**
 * An action that makes a bag from text.
 *
 * @author Kim Rutherford
 */

public class BuildBagAction extends LookupDispatchAction
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
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);
        BuildBagForm buildBagForm = (BuildBagForm) form;

        InterMineBag bag = new InterMineBag();
        String trimmedText = buildBagForm.getText().trim();

        if (trimmedText.length() == 0) {

        } else {
            for (StringTokenizer st = new StringTokenizer(trimmedText, " \t");
                 st.hasMoreTokens();) {
                String token = st.nextToken();

                bag.add(token);
            }
        }

        String newBagName = "new_bag";

        if (savedBags == null) {
            savedBags = new LinkedHashMap();
            session.setAttribute(Constants.SAVED_BAGS, savedBags);
        }

        BagHelper.saveBag(bag, newBagName, savedBags);

        String forwardPath = mapping.findForward("bagDetails").getPath() + "?bagName=" + newBagName;

        ActionMessages actionMessages = new ActionMessages();
        actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
                           new ActionError("bagBuilder.saved", newBagName));
        saveMessages(request, actionMessages);

        return new ActionForward(forwardPath);
    }

    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("bagBuilder.makeStringBag", "makeStringBag");
        return map;
    }
}
