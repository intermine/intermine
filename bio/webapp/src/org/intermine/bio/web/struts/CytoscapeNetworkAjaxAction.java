package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.bio.web.logic.CytoscapeNetworkService;
import org.intermine.util.StringUtil;

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
        String fullInteractingGeneSetStr = (String) request.getParameter("fullInteractingGeneSet");

        // === Prepare data ===
        List<String> fullInteractingGeneList = StringUtil.tokenize(fullInteractingGeneSetStr, ",");

        Set<Integer> fullInteractingGeneSet = new HashSet<Integer>();
        for (String s : fullInteractingGeneList) {
            fullInteractingGeneSet.add(Integer.valueOf(s));
        }

        // === Create network data ===
        CytoscapeNetworkService networkSrv = new CytoscapeNetworkService();
        String network = networkSrv.getNetwork(fullInteractingGeneSet, session);

        // === Print out network data ===
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        out.println(network);
        out.flush();
        out.close();

        return null;
    }

}
