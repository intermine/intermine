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
import java.util.Set;
import java.util.Collection;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Implementation of <strong>Action</strong> to modify bags
 * @author Mark Woodbridge
 */
public class ModifyBagAction extends Action
{
    /**
     * Forward to the correct method based on the button pressed
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
                                 HttpServletResponse response)
        throws Exception {
        if (request.getParameter("union") != null) {
            union(mapping, form, request, response);
        } if (request.getParameter("intersect") != null) {
            intersect(mapping, form, request, response);
        } if (request.getParameter("delete") != null) {
            delete(mapping, form, request, response);
        }

        return mapping.findForward("history");
    }

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

        String[] selectedBags = mbf.getSelectedBags();
        Set combined = new InterMineBag();
        for (int i = 0; i < mbf.getSelectedBags().length; i++) {
            combined.addAll((Collection) savedBags.get(selectedBags[i]));
        }
        savedBags.put(BagHelper.findNewBagName(savedBags), combined);

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

        String[] selectedBags = mbf.getSelectedBags();
        Set combined = new InterMineBag((Collection) savedBags.get(selectedBags[0]));
        for (int i = 1; i < selectedBags.length; i++) {
            combined.retainAll((Collection) savedBags.get(selectedBags[i]));
        }
        savedBags.put(BagHelper.findNewBagName(savedBags), combined);

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
}