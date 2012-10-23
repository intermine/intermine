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

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * This class will take ajax calls by POST to extent the network.
 *
 * @author Fengyuan Hu
 *
 */
public class CytoscapeInteractionExtentionAction extends Action
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeInteractionExtentionAction.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        //========== input ==========
        // List of all nodes in the network, the current list should be always saved in the jsp
        @SuppressWarnings("unused")
        String nodeList = request.getParameter("nodeList");
        // The node is one of the list
        @SuppressWarnings("unused")
        String nodeToExtent = request.getParameter("nodeToExtent");

        //========== logics ==========
        // TODO add logics

        //========== output ==========
        ServletInputStream is = request.getInputStream();
        ServletOutputStream out = response.getOutputStream();

        byte[] b = new byte[16384];

        int i = 0;
        while ((i = is.read(b)) != -1) {
            out.write(b, 0, i);
        }

        out.flush();
        out.close();

        return null;
    }
}
