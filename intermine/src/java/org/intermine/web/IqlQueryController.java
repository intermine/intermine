package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionForward;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

import org.flymine.objectstore.query.Query;

/**
 * Perform the initialisation for the FqlQueryAction.
 *
 * @author Kim Rutherford
 */

public class FqlQueryController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();

        FqlQueryForm fqlQueryForm = (FqlQueryForm) form;

        Query q = (Query) session.getAttribute(Constants.QUERY);

        if (q == null || q.getFrom().size () == 0) {
             fqlQueryForm.setQuerystring("");
        } else {
            fqlQueryForm.setQuerystring(q.toString());
        }

        return null;
    }
}
