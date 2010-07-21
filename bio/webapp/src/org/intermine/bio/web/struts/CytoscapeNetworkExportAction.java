package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
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

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Export network from Cytoscape Web as in different formats:
 * "png", "pdf", "xgmml", "graphml", "sif".
 *
 * @author Fengyuan Hu
 */
public class CytoscapeNetworkExportAction extends Action
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        String type = request.getParameter("type");

        if (type.equals("sif")) {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.sif\"");
        }
        else if (type.equals("pdf")) {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.pdf\"");
        }
        else if (type.equals("xgmml")) {
            response.setContentType("text/xml");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.xgmml\"");
        }
        else if (type.equals("png")) {
            response.setContentType("image/png");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.png\"");
        }
        else if (type.equals("graphml")) {
            response.setContentType("text/xml");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.graphml\"");
        }

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
