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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.web.results.ChangeResultsForm;

/**
 * Implementation of Action that views a bag.
 * @author Kim Rutherford
 */

public class ViewBagAction extends Action
{
    /**
     * @see Action#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        ChangeResultsForm resultsForm =
            (ChangeResultsForm) session.getAttribute("changeResultsForm");

        if (resultsForm != null) {
            resultsForm.reset(mapping, request);
        }
        
        return mapping.findForward("details");
    }
}
