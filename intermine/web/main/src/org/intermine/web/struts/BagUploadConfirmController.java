package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for the bagUploadConfirm; retrieve an IDResolutionService job id
 * @author Alex Kalderimis
 */
public class BagUploadConfirmController extends TilesAction
{

    private static final String WS_JOB_ID_KEY = "idresolutionjobid";

    /**
     * Set up the bagUploadConfirm page.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();

        String bagName = (String) request.getAttribute("newBagName");
        if (bagName != null) {
            request.setAttribute("bagName", bagName);
        }

        // Get the id of the job.
        request.setAttribute("jobUid", session.getAttribute(WS_JOB_ID_KEY));

        return null;
    }

}
