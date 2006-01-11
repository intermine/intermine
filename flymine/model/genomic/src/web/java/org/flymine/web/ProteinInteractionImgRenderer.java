package org.flymine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.Image;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.flymine.model.genomic.Protein;
import org.flymine.networkview.CyNet2Image;
import org.flymine.networkview.FlyNetworkIntegrator;
import org.flymine.networkview.ProteinInteractionRetriever;
import org.flymine.networkview.network.FlyNetwork;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.Constants;
import org.intermine.web.InterMineAction;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;

/**
 * used to render a image of a protein interaction network
 * that looks the same as the network created by cytoscape
 * @author Florian Reisinger
 *
 */
public class ProteinInteractionImgRenderer extends InterMineAction
{
    //    private int imgHeight = 300;
    //    private int imgWidht = 500;

    /**
     * Method called to export a Image of protein interations to the reults page.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        StringBuffer msg = new StringBuffer();
        String id = request.getParameter("object");

        // get protein interactions, build interaction network and render image
        HttpSession session = request.getSession();

        ServletContext ct = session.getServletContext();
        ObjectStore os = (ObjectStore) ct.getAttribute(Constants.OBJECTSTORE);
        Protein p = (Protein) os.getObjectById(Integer.valueOf(id));

        ProteinInteractionRetriever pir = new ProteinInteractionRetriever(os);
        Collection c = new ArrayList();
        c.add(p.getPrimaryAccession());
        FlyNetwork fn = pir.expandNetworkFromProteins(c);
        if (fn == null) {
            msg.append("expandNetworkFromProteins returned null\n");
        }
        //String vizFile = "/home/flo/.cytoscape/vizmap.props";

        Collection nc = new ArrayList();
        Collection ec = new ArrayList();
        CyNetwork net = null;

        try {
            nc = FlyNetworkIntegrator.convertNodesFly2Cy(fn.getNodes());
            ec = FlyNetworkIntegrator.convertEdgesFly2Cy(fn.getEdges());

            if (!nc.isEmpty() || !ec.isEmpty()) {
                net = Cytoscape.createNetwork(nc, ec, "tmpNet");
            } else {
                msg.append("conversion of nodes/edges failed<br>");
            }
        } catch (Throwable e) {
            StackTraceElement[] trace = e.getStackTrace();
            for (int i = 0; i < trace.length; i++) {
                msg.append(trace[i] + "<br>");
            }
            Throwable eCause = e.getCause();
            if (eCause != null) {
                StackTraceElement[] eCauseTrace = eCause.getStackTrace();
                for (int i = 0; i < eCauseTrace.length; i++) {
                    msg.append(eCauseTrace[i] + "<br>");
                }
            }
            msg.append("Error converting Fl2Cy<br>");
        }

        if (net != null) {
            // Image img = CyNet2Image.convertNetwork2Image(net, imgWidht, imgHeight, null, null);
            Image img = CyNet2Image.convertNetwork2Image(net);
            response.setContentType("image/png");
            OutputStream out = response.getOutputStream();
            CyNet2Image.imageOut(img, out);

        } else {
            msg.append("network was null");
            response.getWriter().write(msg.toString());
        }
        return null;
    }

}
