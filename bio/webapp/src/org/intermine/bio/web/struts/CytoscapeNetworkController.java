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

import java.util.HashSet;
import java.util.Map;
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
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.bio.web.logic.CytoscapeNetworkDBQueryRunner;
import org.intermine.bio.web.logic.CytoscapeNetworkUtil;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * This class contains the logics for interaction validation.
 *
 * @author Julie Sullivan
 * @author Fengyuan Hu
 *
 */
public class CytoscapeNetworkController extends TilesAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeNetworkController.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession(); // Get HttpSession
        final InterMineAPI im = SessionMethods.getInterMineAPI(session); // Get InterMineAPI
        ObjectStore os = im.getObjectStore(); // Get OS
        Model model = im.getModel(); // Get Model
        Profile profile = SessionMethods.getProfile(session); // Get Profile
        PathQueryExecutor executor = im.getPathQueryExecutor(profile); // Get PathQueryExecutor

        Set<Integer> startingFeatureSet = new HashSet<Integer>(); // feature: gene or protein
        String featureType = "";

        //=== Get Interaction information ===
        Map<String, Set<String>> interactionInfoMap = CytoscapeNetworkUtil
                .getInteractionInfo(model, executor);

        //=== Handle object ===
        // From gene report page
        InterMineObject object = (InterMineObject) request.getAttribute("object");
        // From list analysis page
        InterMineBag bag = (InterMineBag) request.getAttribute("bag"); // OrthologueLinkController

        if (bag != null) {
            startingFeatureSet.addAll(bag.getContentsAsIds());
            if ("Gene".equals(bag.getType())) {
                featureType = "Gene";
            } else if ("Protein".equals(bag.getType())) {
                featureType = "Protein";
            }
        } else {
            startingFeatureSet.add(object.getId());
            if (object instanceof Gene) {
                featureType = "Gene";
            } else if (object instanceof Protein) {
                featureType = "Protein";
            }
        }

        //=== Query a full set of interacting genes ===
        CytoscapeNetworkDBQueryRunner queryRunner = new CytoscapeNetworkDBQueryRunner();
        Set<Integer> fullInteractingGeneSet = queryRunner.getInteractingGenes(
                featureType, startingFeatureSet, model, executor);
        request.setAttribute("fullInteractingGeneSet",
                StringUtil.join(fullInteractingGeneSet, ","));

        //=== Validation ===
        if (interactionInfoMap == null) {
            String dataNotIncludedMessage = "Interaction data is not included.";
            request.setAttribute("dataNotIncludedMessage", dataNotIncludedMessage);
            return null;
        }

        // Check if interaction data available for the organism
        Gene aTestGene = (Gene) os.getObjectById(fullInteractingGeneSet.iterator().next());
        String orgName = aTestGene.getOrganism().getName();
        if (!interactionInfoMap.containsKey(orgName)) {
            String orgWithNoDataMessage = "No interaction data found for "
                    + orgName + " genes";
            request.setAttribute("orgWithNoDataMessage", orgWithNoDataMessage);
            return null;
        }

        return null;
    }


}
