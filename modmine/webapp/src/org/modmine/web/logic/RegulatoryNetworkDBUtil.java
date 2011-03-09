package org.modmine.web.logic;

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
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;
import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;

/**
 * This class has the logics to query the database for modMine regulatory network information.
 *
 * @author Fengyuan Hu
 *
 */
public final class RegulatoryNetworkDBUtil
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(RegulatoryNetworkDBUtil.class);

    private static Set<CytoscapeNetworkNodeData> interactionNodeSet = null;
    private static Set<CytoscapeNetworkEdgeData> interactionEdgeSet = null;

    private RegulatoryNetworkDBUtil() {

    }

    //========== for FLy miRNA/gene - transcription factor interaction ==========

    /**
     * Get the fly network property/node information.
     *
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return a set of CytoscapeNetworkNodeData obj
     */
    public static synchronized Set<CytoscapeNetworkNodeData> getFlyRegulatoryNodes(
            Model model, PathQueryExecutor executor) {
        // Check NetworkProperty class in the model
        if (!model.getClassNames().contains(model.getPackageName() + ".NetworkProperty")) {
            return null;
        } else if (interactionNodeSet == null) {
            queryFlyRegulatoryNodes(model, executor);
        }

        return interactionNodeSet;
    }

    /**
     * Get the fly network regulation/edge information.
     *
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return a set of CytoscapeNetworkEdgeData obj
     */
    public static synchronized Set<CytoscapeNetworkEdgeData> getFlyRegulatoryEdges(
            Model model, PathQueryExecutor executor) {
        // Check Regulation class in the model
        if (!model.getClassNames().contains(model.getPackageName() + ".Regulation")) {
            return null;
        } else if (interactionEdgeSet == null) {
            queryFlyRegulatoryEdges(model, executor);
        }

        return interactionEdgeSet;
    }

    /**
     * Query all nodes of fly regulatory network.
     *
     * @param model the Model
     * @param executor to run the query
     * @return interactionNodeSet
     */
    private static void queryFlyRegulatoryNodes(Model model, PathQueryExecutor executor) {

        interactionNodeSet = new LinkedHashSet<CytoscapeNetworkNodeData>();

        PathQuery query = new PathQuery(model);
        query.addViews(
                "NetworkProperty.node.primaryIdentifier",
                "NetworkProperty.node.symbol",
                "NetworkProperty.node.id",
                "NetworkProperty.value"
        );

        ExportResultsIterator result = executor.execute(query);

        while (result.hasNext()) {
            List<ResultElement> row = result.next();

            String featurePId = (String) row.get(0).getField();
            String featureSymbol = (String) row.get(1).getField();
            Integer featureIMId = (Integer) row.get(2).getField();
            String position = (String) row.get(3).getField();

            CytoscapeNetworkNodeData aNode = new CytoscapeNetworkNodeData();
            aNode.setInterMineId(featureIMId);
            aNode.setSoureceId(featurePId);

            if (featureSymbol == null || featureSymbol.length() < 1) {
                aNode.setSourceLabel(featurePId);
            } else {
                aNode.setSourceLabel(featureSymbol);
            }

            aNode.setPosition(position);

            interactionNodeSet.add(aNode);
        }
    }

    /**
     * Query all edges of fly regulatory network.
     *
     * @param model the Model
     * @param executor to run the query
     * @return interactionEdgeSet
     */
    private static void queryFlyRegulatoryEdges(Model model, PathQueryExecutor executor) {

        interactionEdgeSet = new LinkedHashSet<CytoscapeNetworkEdgeData>();

        PathQuery query = new PathQuery(model);
        query.addViews(
                "Regulation.type", // interaction type, e.g. TF-TF
                "Regulation.source.primaryIdentifier",
                "Regulation.source.symbol",
                "Regulation.target.primaryIdentifier",
                "Regulation.target.symbol"
        );

        ExportResultsIterator result = executor.execute(query);

        while (result.hasNext()) {
            List<ResultElement> row = result.next();

            String interactionType = (String) row.get(0).getField();
            String sourceNodePId = (String) row.get(1).getField();
            String sourceNodeSymbol = (String) row.get(2).getField();
            String targetNodePId = (String) row.get(3).getField();
            String targetNodeSymbol = (String) row.get(4).getField();

            CytoscapeNetworkEdgeData aEdge = new CytoscapeNetworkEdgeData();

            // Handle bidirectional edges
            if (interactionEdgeSet.isEmpty()) {
                aEdge.setSoureceId(sourceNodePId);

                if (sourceNodeSymbol == null || sourceNodeSymbol.length() < 1) {
                    aEdge.setSourceLabel(sourceNodePId);
                } else {
                    aEdge.setSourceLabel(sourceNodeSymbol);
                }

                aEdge.setTargetId(targetNodePId);

                if (targetNodeSymbol == null || targetNodeSymbol.length() < 1) {
                    aEdge.setTargetLabel(targetNodePId);
                } else {
                    aEdge.setTargetLabel(targetNodeSymbol);
                }

                aEdge.setInteractionType(interactionType);
                aEdge.setDirection("one");

                interactionEdgeSet.add(aEdge);
            } else {
                aEdge.setSoureceId(sourceNodePId);

                if (sourceNodeSymbol == null || sourceNodeSymbol.length() < 1) {
                    aEdge.setSourceLabel(sourceNodePId);
                } else {
                    aEdge.setSourceLabel(sourceNodeSymbol);
                }

                aEdge.setTargetId(targetNodePId);

                if (targetNodeSymbol == null || targetNodeSymbol.length() < 1) {
                    aEdge.setTargetLabel(targetNodePId);
                } else {
                    aEdge.setTargetLabel(targetNodeSymbol);
                }

                // miRNA-TF and TF-miRNA are the same interaction type
                if ("TF-miRNA".equals(interactionType) || "miRNA-TF".equals(interactionType)) {
                    String uniType = "miRNA-TF";
                    aEdge.setInteractionType(uniType);
                } else {
                    aEdge.setInteractionType(interactionType);
                }

                String interactingString = aEdge.generateInteractionString();
                String interactingStringRev = aEdge.generateReverseInteractionString();

                // Get a list of interactionString from interactionSet
                LinkedHashSet<String> intcStrSet = new LinkedHashSet<String>();
                for (CytoscapeNetworkEdgeData edgedata : interactionEdgeSet) {
                    intcStrSet.add(edgedata.generateInteractionString());
                }
                // A none dulipcated edge
                if (!(intcStrSet.contains(interactingString) || intcStrSet
                        .contains(interactingStringRev))) {
                    aEdge.setDirection("one");
                    interactionEdgeSet.add(aEdge);
                } else { // duplicated edge
                    // Pull out the CytoscapeNetworkEdgeData which contains the current
                    // interactionString
                    for (CytoscapeNetworkEdgeData edgedata : interactionEdgeSet) {
                        if (edgedata.generateInteractionString().equals(interactingString)
                            || edgedata.generateInteractionString().equals(interactingStringRev)) {
                            edgedata.setDirection("both");
                            aEdge.setInteractionType(interactionType);
                        }
                    }
                }
            }
        }
    }
}
