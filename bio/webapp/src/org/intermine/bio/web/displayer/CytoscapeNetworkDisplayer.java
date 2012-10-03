package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.bio.web.logic.CytoscapeNetworkDBQueryRunner;
import org.intermine.bio.web.logic.CytoscapeNetworkUtil;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.StringUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Displayer for gene/protein interactions using cytoscape plugin
 * @author Fengyuan
 */
public class CytoscapeNetworkDisplayer extends ReportDisplayer
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeNetworkDisplayer.class);

    private static final String DATA_NOT_INTEGRATED = "Interaction data is not integrated.";
    private static final String EXCEPTION_OCCURED = "An exception occured";

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public CytoscapeNetworkDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        ObjectStore os = im.getObjectStore(); // Get OS
        Model model = im.getModel(); // Get Model
        Profile profile = SessionMethods.getProfile(request.getSession()); // Get Profile
        PathQueryExecutor executor = im.getPathQueryExecutor(profile); // Get PathQueryExecutor

        Set<Integer> startingFeatureSet = new LinkedHashSet<Integer>(); // feature: gene or protein
        String featureType = "";

        //=== Get Interaction information ===
        Map<String, Set<String>> interactionInfoMap = CytoscapeNetworkUtil
                .getInteractionInfo(model, executor);

        if (interactionInfoMap == null) {
            request.setAttribute("dataNotIncludedMessage", DATA_NOT_INTEGRATED);
            return;
        }

        //=== Handle object ===
        // From gene report page
        InterMineObject object = reportObject.getObject();
        request.setAttribute("cytoscapeInteractionObjectId", object.getId());
        // From list analysis page
        InterMineBag bag = (InterMineBag) request.getAttribute("bag"); // OrthologueLinkController

        if (bag != null) {
            startingFeatureSet.addAll(bag.getContentsAsIds());
            if ("Gene".equals(bag.getType())) {
                // Nodes are modelled as Gene instead sub types of Gene
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

        //=== Validation ===
        // Check if interaction data available for the organism
        BioEntity hubNode;

        try {
            if (bag != null) {
                hubNode = (BioEntity) os.getObjectById((Integer) startingFeatureSet.toArray()[0]);
            } else {
                hubNode = (BioEntity) object;
            }

            String orgName = hubNode.getOrganism().getName();
            if (!interactionInfoMap.containsKey(orgName)) {
                String orgWithNoDataMessage = "No interaction data found for "
                        + orgName + " genes";
                request.setAttribute("orgWithNoDataMessage", orgWithNoDataMessage);
                return;
            }
        } catch (ObjectStoreException e) {
            request.setAttribute("exception", EXCEPTION_OCCURED);
            e.printStackTrace();
        }


        //=== Query a full set of interacting genes ===
        CytoscapeNetworkDBQueryRunner queryRunner = new CytoscapeNetworkDBQueryRunner();
        Set<Integer> fullInteractingGeneSet = queryRunner.getInteractingGenes(
                featureType, startingFeatureSet, model, executor);

        // set fullInteractingGeneSet in request
        request.setAttribute("fullInteractingGeneSet",
                StringUtil.join(fullInteractingGeneSet, ","));

        // === Create inline table query ===
        PathQuery q = new PathQuery(model);
        q.addViews("Gene.symbol",
                "Gene.primaryIdentifier",
                "Gene.interactions.details.type",
                "Gene.interactions.gene2.symbol",
                "Gene.interactions.gene2.primaryIdentifier",
                "Gene.interactions.details.dataSets.dataSource.name",
                "Gene.interactions.details.experiment.publication.title",
                "Gene.interactions.details.experiment.publication.pubMedId");

        q.addOrderBy("Gene.symbol", OrderDirection.ASC);
        q.addConstraint(Constraints.inIds("Gene", fullInteractingGeneSet), "B");
        q.addConstraint(Constraints.inIds("Gene.interactions.gene2",
                fullInteractingGeneSet), "A");
        q.setConstraintLogic("B and A");

        request.setAttribute("cytoscapeNetworkQueryXML", q.toXml());
        request.setAttribute("cytoscapeNetworkQueryJson", q.toJson());
    }
}
