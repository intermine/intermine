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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.modmine.web.model.RegulatoryNetworkEdgeData;

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

    private static Set<CytoscapeNetworkNodeData> interactionNodeSetFly = null;
    private static Set<CytoscapeNetworkEdgeData> interactionEdgeSetFly = null;

    private static Set<CytoscapeNetworkNodeData> interactionNodeSetWorm = null;
    private static Set<CytoscapeNetworkEdgeData> interactionEdgeSetWorm = null;

    private RegulatoryNetworkDBUtil() { }

    // TODO use InterMine Id as identifier, fly is not applied.

    //========== for Fly miRNA/gene - transcription factor interaction ==========

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
        } else if (interactionNodeSetFly == null) {
            queryFlyRegulatoryNodes(model, executor);
        }

        return interactionNodeSetFly;
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
        } else if (interactionEdgeSetFly == null) {
            queryFlyRegulatoryEdges(model, executor);
        }

        return interactionEdgeSetFly;
    }

    /**
     * Query all nodes of fly regulatory network.
     *
     * @param model the Model
     * @param executor to run the query
     * @return interactionNodeSet
     */
    private static void queryFlyRegulatoryNodes(Model model, PathQueryExecutor executor) {

        interactionNodeSetFly = new LinkedHashSet<CytoscapeNetworkNodeData>();

        PathQuery query = new PathQuery(model);
        query.addViews(
                "NetworkProperty.node.primaryIdentifier",
                "NetworkProperty.node.symbol",
                "NetworkProperty.node.id",
                "NetworkProperty.value"
        );

        query.addConstraint(Constraints.eq(
                "NetworkProperty.node.primaryIdentifier", "FBgn*"));

        ExportResultsIterator result = executor.execute(query);

        while (result.hasNext()) {
            List<ResultElement> row = result.next();

            String featurePId = (String) row.get(0).getField();
            String featureSymbol = (String) row.get(1).getField();
            Integer featureIMId = (Integer) row.get(2).getField();
            String position = (String) row.get(3).getField();

            CytoscapeNetworkNodeData aNode = new CytoscapeNetworkNodeData();
            aNode.setInterMineId(featureIMId);
            aNode.setSourceId(featurePId);

            if (featureSymbol == null || featureSymbol.length() < 1) {
                aNode.setSourceLabel(featurePId);
            } else {
                aNode.setSourceLabel(featureSymbol);
            }

            aNode.setPosition(position);

            interactionNodeSetFly.add(aNode);
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

        interactionEdgeSetFly = new LinkedHashSet<CytoscapeNetworkEdgeData>();

        // TODO fly doesn't use IM Object id
        PathQuery query = new PathQuery(model);
        query.addViews(
                "Regulation.type", // interaction type, e.g. TF-TF
                "Regulation.source.primaryIdentifier",
                "Regulation.source.symbol",
                "Regulation.target.primaryIdentifier",
                "Regulation.target.symbol"
        );

        query.addConstraint(
                Constraints.eq("Regulation.source.primaryIdentifier", "FBgn*"),
                "A");
        query.addConstraint(
                Constraints.eq("Regulation.target.primaryIdentifier", "FBgn*"),
                "B");

        query.setConstraintLogic("A and B");

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
            if (interactionEdgeSetFly.isEmpty()) {
                aEdge.setSourceId(sourceNodePId);

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

                interactionEdgeSetFly.add(aEdge);
            } else {
                aEdge.setSourceId(sourceNodePId);

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
                for (CytoscapeNetworkEdgeData edgedata : interactionEdgeSetFly) {
                    intcStrSet.add(edgedata.generateInteractionString());
                }
                // A none dulipcated edge
                if (!(intcStrSet.contains(interactingString) || intcStrSet
                        .contains(interactingStringRev))) {
                    aEdge.setDirection("one");
                    interactionEdgeSetFly.add(aEdge);
                } else { // duplicated edge
                    // Pull out the CytoscapeNetworkEdgeData which contains the current
                    // interactionString
                    for (CytoscapeNetworkEdgeData edgedata : interactionEdgeSetFly) {
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

    //========== for Worm miRNA/gene - transcription factor interaction ==========
    /**
     * Get the worm network property/node information.
     *
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return a set of CytoscapeNetworkNodeData obj
     */
    public static synchronized Set<CytoscapeNetworkNodeData> getWormRegulatoryNodes(
            Model model, PathQueryExecutor executor) {
        // Check NetworkProperty class in the model
        if (!model.getClassNames().contains(model.getPackageName() + ".NetworkProperty")) {
            return null;
        } else if (interactionNodeSetWorm == null) {
            queryWormRegulatoryNodes(model, executor);
        }

        return interactionNodeSetWorm;
    }

    /**
     * Get the worm network regulation/edge information.
     *
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return a set of CytoscapeNetworkEdgeData obj
     */
    public static synchronized Set<CytoscapeNetworkEdgeData> getWormRegulatoryEdges(
            Model model, PathQueryExecutor executor) {
        // Check Regulation class in the model
        if (!model.getClassNames().contains(model.getPackageName() + ".Regulation")) {
            return null;
        } else if (interactionEdgeSetWorm == null) {
            queryWormRegulatoryEdges(model, executor);
        }

        return interactionEdgeSetWorm;
    }

    /**
     * Query all nodes of worm regulatory network.
     *
     * @param model the Model
     * @param executor to run the query
     * @return interactionNodeSet
     */
    private static void queryWormRegulatoryNodes(Model model, PathQueryExecutor executor) {

        interactionNodeSetWorm = new LinkedHashSet<CytoscapeNetworkNodeData>();

        PathQuery query = new PathQuery(model);
        query.addViews(
                "NetworkProperty.node.primaryIdentifier",
                "NetworkProperty.node.symbol",
                "NetworkProperty.node.id",
                "NetworkProperty.type",
                "NetworkProperty.value"
        );

        query.addConstraint(Constraints.eq(
                "NetworkProperty.node.organism.shortName", "C. elegans"));

        ExportResultsIterator result = executor.execute(query);

        while (result.hasNext()) {
            List<ResultElement> row = result.next();

            String featurePId = (String) row.get(0).getField();
            String featureSymbol = (String) row.get(1).getField();
            Integer id = (Integer) row.get(2).getField();
            String key = (String) row.get(3).getField();
            String value = (String) row.get(4).getField();

            CytoscapeNetworkNodeData aNode = new CytoscapeNetworkNodeData();
            aNode.setInterMineId(id);
            aNode.setSourceId(String.valueOf(id)); // Use IM Id instead of PId

            if (featureSymbol == null || featureSymbol.length() < 1) {
                aNode.setSourceLabel(featurePId);
            } else {
                aNode.setSourceLabel(featureSymbol);
            }

            if (interactionNodeSetWorm.contains(aNode)) {
                for (CytoscapeNetworkNodeData n : interactionNodeSetWorm) {
                    if (n.getInterMineId() == id) {
                        n.getExtraInfo().put(key, value);
                    }
                }
            } else {
                Map<String, String> extraInfo = new HashMap<String, String>();
                extraInfo.put(key, value);
                aNode.setExtraInfo(extraInfo);
            }

            interactionNodeSetWorm.add(aNode);
        }
    }

    /**
     * Query all edges of worm regulatory network.
     *
     * @param model the Model
     * @param executor to run the query
     * @return interactionEdgeSet
     */
    private static void queryWormRegulatoryEdges(Model model, PathQueryExecutor executor) {

        interactionEdgeSetWorm = new LinkedHashSet<CytoscapeNetworkEdgeData>();

        PathQuery query = new PathQuery(model);
        query.addViews(
                "Regulation.type", // interaction type, e.g. TF-TF
                "Regulation.source.primaryIdentifier",
                "Regulation.source.symbol",
                "Regulation.source.id",
                "Regulation.target.primaryIdentifier",
                "Regulation.target.symbol",
                "Regulation.target.id"
        );

        query.addConstraint(Constraints.eq("Regulation.source.organism.shortName", "C. elegans"));

        ExportResultsIterator result = executor.execute(query);

        while (result.hasNext()) {
            List<ResultElement> row = result.next();

            String interactionType = (String) row.get(0).getField();
            String sourcePId = (String) row.get(1).getField();
            String sourceSymbol = (String) row.get(2).getField();
            Integer sourceId = (Integer) row.get(3).getField();
            String targetPId = (String) row.get(4).getField();
            String targetSymbol = (String) row.get(5).getField();
            Integer targetId = (Integer) row.get(6).getField();

            // TODO Hack for a database issue
//            if ("blmp-1".equals(targetSymbol)) {
//                targetId = 1644007904;
//            }
//
//            if ("unc-130".equals(targetSymbol)) {
//                targetId = 1644015202;
//            }
//
//            if ("mab-5".equals(targetSymbol)) {
//                targetId = 1644006300;
//            }
//
//            if ("mdl-1".equals(targetSymbol)) {
//                targetId = 1644006399;
//            }
//
//            if ("elt-3".equals(targetSymbol)) {
//                targetId = 1644003204;
//            }
//
//            if ("lin-11".equals(targetSymbol)) {
//                targetId = 1644006039;
//            }
//
//            if ("skn-1".equals(targetSymbol)) {
//                targetId = 1644009973;
//            }
//
//            if ("egl-27".equals(targetSymbol)) {
//                targetId = 1644003074;
//            }

            CytoscapeNetworkEdgeData aEdge = new RegulatoryNetworkEdgeData();

            aEdge.setSourceId(String.valueOf(sourceId));

            if (sourceSymbol == null || sourceSymbol.length() < 1) {
                aEdge.setSourceLabel(sourcePId);
            } else {
                aEdge.setSourceLabel(sourceSymbol);
            }

            aEdge.setTargetId(String.valueOf(targetId));

            if (targetSymbol == null || targetSymbol.length() < 1) {
                aEdge.setTargetLabel(targetPId);
            } else {
                aEdge.setTargetLabel(targetSymbol);
            }

            aEdge.setInteractionType(interactionType);

            interactionEdgeSetWorm.add(aEdge);
        }
    }

}
