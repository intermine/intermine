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

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.actions.LookupDispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Implementation of <strong>Action</strong> to modify bags
 * @author Mark Woodbridge
 */
public class ModifyBagAction extends LookupDispatchAction
{
    /**
     * Union the selected bags
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward union(ActionMapping mapping,
                               ActionForm form,
                               HttpServletRequest request,
                               HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);

        ModifyBagForm mbf = (ModifyBagForm) form;
        
        String bag1Name = mbf.getSelectedBags()[0];
        String bag2Name = mbf.getSelectedBags()[0];
        Collection bag1 = (Collection) savedBags.get(bag1Name);
        Collection bag2 = (Collection) savedBags.get(bag2Name);
        
        Set combined = new InterMineBag();
        combined.addAll(bag1);
        combined.addAll(bag2);

        savedBags.put(bag1Name + " u " + bag2Name, combined);

        return mapping.findForward("history");
    }

    /**
     * Intersect the selected bags
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward intersect(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);

        ModifyBagForm mbf = (ModifyBagForm) form;
        
        String bag1Name = mbf.getSelectedBags()[0];
        String bag2Name = mbf.getSelectedBags()[0];
        Collection bag1 = (Collection) savedBags.get(bag1Name);
        Collection bag2 = (Collection) savedBags.get(bag2Name);
        
        Set combined = new InterMineBag();
        combined.addAll(bag1);
        combined.retainAll(bag2);

        savedBags.put(bag1Name + " n " + bag2Name, combined);

        return mapping.findForward("history");
    }

    /**
     * Delete the selected bags
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward delete(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);

        ModifyBagForm mbf = (ModifyBagForm) form;
        
        for (int i = 0; i < mbf.getSelectedBags().length; i++) {
            savedBags.remove(mbf.getSelectedBags()[i]);
        }

        return mapping.findForward("history");
    }

    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("history.union", "union");
        map.put("history.intersect", "intersect");
        map.put("history.delete", "delete");
        return map;
    }
}