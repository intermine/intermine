package org.flymine.web.results;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.struts.actions.LookupDispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Implementation of <strong>LookupDispatchAction</strong>. Changes the
 * size of the results displayed.
 *
 * @author Andrew Varley
 */
public class ChangeResultsSizeAction extends LookupDispatchAction
{
    protected static final String DISPLAYABLERESULTS_NAME = "resultsTable";

    /**
     * Move a column nearer to the bottom of the list of columns. Must
     * pass in a parameter "columnAlias" to indicate the column being
     * moved.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward changePageSize(ActionMapping mapping, ActionForm form,
                                        HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(DISPLAYABLERESULTS_NAME);
        ChangeResultsForm changeResultsForm = (ChangeResultsForm) form;

        dr.setPageSize(Integer.parseInt(changeResultsForm.getPageSize()));

        // Need to set the start so that we are on the page containing the current start item
        dr.setStart((dr.getStart() / dr.getPageSize()) * dr.getPageSize());

        return mapping.findForward("results");
    }

    /**
     * Move a column nearer to the bottom of the list of columns. Must
     * pass in a parameter "columnAlias" to indicate the column being
     * moved.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward saveCollection(ActionMapping mapping, ActionForm form,
                                        HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        return mapping.findForward("results");
    }


    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("button.change", "changePageSize");
        map.put("button.save", "saveCollection");
        return map;
    }


}
