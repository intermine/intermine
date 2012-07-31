package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.bio.web.logic.CytoscapeNetworkService;

/**
 * Network data will be created and loaded by calling this class via ajax.
 *
 * @author Fengyuan Hu
 */
public class CytoscapeNetworkAjaxAction extends Action
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeNetworkAjaxAction.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession(); // Get HttpSession

        // === Get request paras ===
        // gene ids
        String fullInteractingGeneSetStr = request.getParameter("fullInteractingGeneSet");

        // === Create network data ===
        CytoscapeNetworkService networkSrv = new CytoscapeNetworkService();
        String network = networkSrv.getNetwork(
                fullInteractingGeneSetStr, session, false);

        // === Print out network data ===
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();

        out.println(network);
        out.flush();
        out.close();

        return null;
    }

}
