package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.bio.web.logic.CytoscapeNetworkDBQueryRunner;
import org.intermine.bio.web.logic.CytoscapeNetworkUtil;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Displayer for gene/protein interactions using cytoscape plugin
 * @author Fengyuan
 */
public class CytoscapeNetworkDisplayer extends CustomDisplayer
{

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

        //=== Handle object ===
        // From gene report page
        InterMineObject object = (InterMineObject) reportObject.getObject();
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
            String dataNotIncludedMessage = "Interaction data is not integrated.";
            request.setAttribute("dataNotIncludedMessage", dataNotIncludedMessage);
        }

        // Check if interaction data available for the organism
        Gene hubGene;
        try {
            hubGene = (Gene) os.getObjectById((Integer) fullInteractingGeneSet.toArray()[0]);
            String orgName = hubGene.getOrganism().getName();
            if (!interactionInfoMap.containsKey(orgName)) {
                String orgWithNoDataMessage = "No interaction data found for "
                        + orgName + " genes";
                request.setAttribute("orgWithNoDataMessage", orgWithNoDataMessage);
            }
        } catch (ObjectStoreException e) {
            request.setAttribute("exception", "An exception occured");
            e.printStackTrace();
        }

        // Add view interaction inline table
        PathQuery q = new PathQuery(model);
        q.addViews("Gene.symbol",
                "Gene.primaryIdentifier",
                "Gene.interactions.interactionType",
                "Gene.interactions.interactingGenes.symbol",
                "Gene.interactions.interactingGenes.primaryIdentifier",
                "Gene.interactions.dataSets.dataSource.name",
                "Gene.interactions.experiment.publication.title",
                "Gene.interactions.experiment.publication.pubMedId");

        q.addOrderBy("Gene.symbol", OrderDirection.ASC);
        q.addConstraint(Constraints.inIds("Gene", fullInteractingGeneSet), "B");
        q.addConstraint(Constraints.inIds("Gene.interactions.interactingGenes",
                fullInteractingGeneSet), "A");
        q.setConstraintLogic("B and A");

        try {
            WebResultsExecutor we = im.getWebResultsExecutor(profile);
            WebResults webResults = we.execute(q);
            PagedTable pagedResults = new PagedTable(webResults, 10); // TODO only display 10 recs?
            pagedResults.setTableid("CytoscapeNetworkDisplayer");
            request.setAttribute("cytoscapeNetworkPagedResults", pagedResults);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

    }
}
