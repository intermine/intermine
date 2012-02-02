package org.modmine.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;
import org.intermine.metadata.Model;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.logic.RegulatoryNetworkDBUtil;
import org.modmine.web.logic.RegulatoryNetworkDataFormatUtil;

/**
 * Controller Action for flyRegulatoryNetwork.jsp to prepare the regulatory network
 * data and display in cytoscape web.
 *
 * @author Fengyuan Hu
 *
 */
public class FlyRegulatoryNetworkController extends TilesAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger
            .getLogger(FlyRegulatoryNetworkController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        Profile profile = SessionMethods.getProfile(session);
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);

        Set<CytoscapeNetworkNodeData> interactionNodeSet = RegulatoryNetworkDBUtil
                .getFlyRegulatoryNodes(model, executor);
        Set<CytoscapeNetworkEdgeData> interactionEdgeSet = RegulatoryNetworkDBUtil
                .getFlyRegulatoryEdges(model, executor);

        if (interactionNodeSet == null || interactionEdgeSet == null) {
            request.setAttribute("classMissingMessage",
                "Interaction Class is missing in the model...");
            return null;
        }

        String flyRegulatoryNetwork = RegulatoryNetworkDataFormatUtil
                .createFlyRegulatoryNetworkInXGMML(interactionNodeSet, interactionEdgeSet);

        request.setAttribute("flyRegulatoryNetwork", flyRegulatoryNetwork);

        return null;
    }
}
