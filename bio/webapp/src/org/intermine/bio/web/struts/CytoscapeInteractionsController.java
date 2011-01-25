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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.logic.CytoscapeInteractionDBQuerier;
import org.intermine.bio.web.logic.CytoscapeInteractionDataGenerator;
import org.intermine.bio.web.logic.CytoscapeInteractionUtil;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Set up interaction network data for the cytoscapeInteractionsDisplayer.jsp
 *
 * @author Julie Sullivan
 * @author Fengyuan Hu
 *
 */
public class CytoscapeInteractionsController extends TilesAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeInteractionsController.class);

    private Set<CytoscapeNetworkNodeData> interactionNodeSet =
        new LinkedHashSet<CytoscapeNetworkNodeData>();
    private Set<CytoscapeNetworkEdgeData> interactionEdgeSet =
        new HashSet<CytoscapeNetworkEdgeData>();

    private Set<Integer> geneIdSet = new HashSet<Integer>(); // asset contains gene object store ids

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession(); // Get HttpSession
        final InterMineAPI im = SessionMethods.getInterMineAPI(session); // Get InterMineAPI
        Model model = im.getModel(); // Get Model
        Profile profile = SessionMethods.getProfile(session); // Get Profile
        PathQueryExecutor executor = im.getPathQueryExecutor(profile); // Get PathQueryExecutor

        Map<String, Set<String>> interactionInfoMap = CytoscapeInteractionUtil
                .getInteractionInfo(model, executor);

        if (interactionInfoMap == null) {
            String dataNotIncludedMessage = "Interaction data is not included.";
            request.setAttribute("dataNotIncludedMessage", dataNotIncludedMessage);
            return null;
        }

        InterMineObject object = (InterMineObject) request
                .getAttribute("object"); // Get object from request

        String theNetwork = new String(); // Network data as a string in different formats

        CytoscapeInteractionDataGenerator dataGen = new CytoscapeInteractionDataGenerator();

        // Whether the object is a Gene or Protein
        if (object instanceof Protein) {
            Protein protein = (Protein) object;
            // TODO In most cases, there is only 1 gene in the collect, but rare cases with more
            // than 1, any examples?
            Set<Gene> genes = protein.getGenes();

            // TODO How to handle a protein with two or more genes - generate several networks?
            // for (Gene gene : genes) {
            Gene gene = genes.iterator().next();
//            Set<CytoscapeNetworkEdgeData> interactionEdgeSubSet =
//                new HashSet<CytoscapeNetworkEdgeData>();
//            Set<CytoscapeNetworkNodeData> interactionNodeSubSet =
//                new HashSet<CytoscapeNetworkNodeData>();

            // Check if interaction data available for the organism
            String orgName = gene.getOrganism().getName();
            if (!interactionInfoMap.containsKey(orgName)) {
                String orgWithNoDataMessage = "No interaction data found for "
                        + orgName + " genes";
                request.setAttribute("orgWithNoDataMessage", orgWithNoDataMessage);
                return null;
            }

            ExportResultsIterator rawIntData = queryInteractionRawDataFromDB(gene, model, executor);

            if (rawIntData == null) {
                String dataSourceStr = StringUtil.join(interactionInfoMap.get(orgName), ",");

                String geneWithNoDatasourceMessage = "No interaction data found for "
                        + gene.getSymbol() + " (" + gene.getPrimaryIdentifier()
                        + ") from data sources: " + dataSourceStr;
                request.setAttribute("geneWithNoDatasourceMessage", geneWithNoDatasourceMessage);
                return null;
            }

            // Add the interaction network for this gene to the whole network
            prepareNetworkData(rawIntData);

//            interactionEdgeSet.addAll(interactionEdgeSubSet);
//            interactionNodeSet.addAll(interactionNodeSubSet);

            //}
            // TODO should be a set of hub-genes
            request.setAttribute("hubGene", gene.getPrimaryIdentifier());
        } else if (object instanceof Gene) {
            Gene gene = (Gene) object;

            // Check if interaction data available for the organism
            String orgName = gene.getOrganism().getName();
            if (!interactionInfoMap.containsKey(orgName)) {
                String orgWithNoDataMessage = "No interaction data found for "
                        + orgName + " genes";
                request.setAttribute("orgWithNoDataMessage", orgWithNoDataMessage);
                return null;
            }

            ExportResultsIterator rawIntData = queryInteractionRawDataFromDB(gene, model, executor);

            if (rawIntData == null) {
                String dataSourceStr = StringUtil.join(interactionInfoMap.get(orgName), ",");

                String geneWithNoDatasourceMessage = "No interaction data found for "
                        + gene.getSymbol() + " (" + gene.getPrimaryIdentifier()
                        + ") from data sources: " + dataSourceStr;
                request.setAttribute("geneWithNoDatasourceMessage", geneWithNoDatasourceMessage);
                return null;
            }

            prepareNetworkData(rawIntData);

            request.setAttribute("hubGene", gene.getPrimaryIdentifier());
        }

        theNetwork = dataGen.createGeneNetworkInXGMML(interactionEdgeSet, interactionNodeSet);

        request.setAttribute("geneOSIds", StringUtil.join(geneIdSet, ","));
        request.setAttribute("networkdata", theNetwork);

        return null;
    }

    /**
     * Query the interactions among a set of genes.
     *
     * @param gene a gene pid
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return query results
     */
    private ExportResultsIterator queryInteractionRawDataFromDB(Gene gene,
            Model model, PathQueryExecutor executor) {

        // A list of genes including the hub and its interacting genes
        Set<String> keySet = new HashSet<String>();
        keySet.add(gene.getPrimaryIdentifier());

        // Get all the genes that interact with the hub gene
        CytoscapeInteractionDBQuerier dbQuerier = new CytoscapeInteractionDBQuerier();
        Set<String> interactingGeneSet = dbQuerier.findInteractingGenes(
                String.valueOf(gene.getId()), model, executor);
        if (interactingGeneSet.size() < 1) {
            return null;
        }

        keySet.addAll(interactingGeneSet);

        // Get all interactions between a set of genes
        ExportResultsIterator results = dbQuerier.queryInteractions(keySet, model, executor);
        if (results == null) {
            return null;
        }

        return results;
    }

    /**
     * Create a set of CytoscapeNetworkNodeData and CytoscapeNetworkEdgeData objects for parsing
     * them to xgmml.
     *
     * @param results raw data queried back from database
     * @return A set of CytoscapeNetworkEdgeData objects
     */
    private void prepareNetworkData(ExportResultsIterator results) {

        // Handle results
        while (results.hasNext()) {
            List<ResultElement> row = results.next();

            String genePID = (String) row.get(0).getField();
            String geneSymbol = (String) row.get(1).getField();
            String interactionType = (String) row.get(2).getField();
            String interactingGenePID = (String) row.get(3).getField();
            String interactingGeneSymbol = (String) row.get(4).getField();
            String dataSourceName = (String) row.get(5).getField();
            String interactionShortName = (String) row.get(6).getField();
            Integer geneOSId = (Integer) row.get(7).getField();
            Integer interactingGeneOSId = (Integer) row.get(8).getField();

            geneIdSet.add(geneOSId);
            geneIdSet.add(interactingGeneOSId);

            CytoscapeNetworkNodeData aNode = new CytoscapeNetworkNodeData();

            aNode.setSoureceId(genePID);

            if (geneSymbol == null || geneSymbol.length() < 1) {
                aNode.setSourceLabel(genePID); // use primary ID
            } else if (geneSymbol != null) {
                aNode.setSourceLabel(geneSymbol); // use gene symbol
            } else {
                aNode.setSourceLabel(Integer.toString(geneOSId)); // use gene ID
            }

            interactionNodeSet.add(aNode);

            CytoscapeNetworkEdgeData aEdge = new CytoscapeNetworkEdgeData();
            LinkedHashMap<String, Set<String>> dataSources =
                new LinkedHashMap<String, Set<String>>();
            LinkedHashSet<String> interactionShortNames = new LinkedHashSet<String>();

            if (interactionEdgeSet.isEmpty()) {
                aEdge.setSoureceId(genePID);

                if (geneSymbol == null || geneSymbol.length() < 1) {
                    aEdge.setSourceLabel(genePID); // use primary ID
                } else if (geneSymbol != null) {
                    aEdge.setSourceLabel(geneSymbol); // use gene symbol
                } else {
                    aEdge.setSourceLabel(Integer.toString(geneOSId)); // use gene ID
                }

                aEdge.setTargetId(interactingGenePID);

                if (interactingGeneSymbol == null || interactingGeneSymbol.length() < 1) {
                    aEdge.setTargetLabel(interactingGenePID); // use primary ID
                } else if (interactingGeneSymbol != null) {
                    aEdge.setTargetLabel(interactingGeneSymbol); // use gene symbol
                } else {
                    aEdge.setTargetLabel(Integer.toString(interactingGeneOSId)); // use gene ID
                }

                aEdge.setInteractionType(interactionType);
                interactionShortNames.add(interactionShortName);
                dataSources.put(dataSourceName, interactionShortNames);
                aEdge.setDataSources(dataSources);
                aEdge.setDirection("one");

                interactionEdgeSet.add(aEdge);
            } else {
                // You can't add to the HashSet while iterating. You have to either
                // keep track of where you want to add, and then do that after
                // you're done iterating, or else create a new HashSet on the fly,
                // copying from the old one and filling in the holes as they occur.
                // Thrown - java.util.ConcurrentModificationException

                aEdge.setSoureceId(genePID);

                if (geneSymbol == null || geneSymbol.length() < 1) {
                    aEdge.setSourceLabel(genePID); // use primary ID
                } else if (geneSymbol != null) {
                    aEdge.setSourceLabel(geneSymbol); // use gene symbol
                } else {
                    aEdge.setSourceLabel(Integer.toString(geneOSId)); // use gene ID
                }

                aEdge.setTargetId(interactingGenePID);

                if (interactingGeneSymbol == null || interactingGeneSymbol.length() < 1) {
                    aEdge.setTargetLabel(interactingGenePID); // use primary ID
                } else if (interactingGeneSymbol != null) {
                    aEdge.setTargetLabel(interactingGeneSymbol); // use gene symbol
                } else {
                    aEdge.setTargetLabel(Integer.toString(interactingGeneOSId)); // use gene ID
                }

                aEdge.setInteractionType(interactionType);
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
                    interactionShortNames.add(interactionShortName);
                    dataSources.put(dataSourceName, interactionShortNames);
                    aEdge.setDataSources(dataSources);

                    interactionEdgeSet.add(aEdge);
                } else { // duplicated edge
                    // Pull out the CytoscapeNetworkEdgeData which contains the current
                    // interactionString
                    for (CytoscapeNetworkEdgeData edgedata : interactionEdgeSet) {
                        if (edgedata.generateInteractionString().equals(interactingString)
                            || edgedata.generateInteractionString().equals(interactingStringRev)) {
                            edgedata.setDirection("both");
                            if (edgedata.getDataSources().containsKey(dataSourceName)) {
                                edgedata.getDataSources().get(dataSourceName)
                                        .add(interactionShortName);
                            } else {
                                LinkedHashSet<String> intNames = new LinkedHashSet<String>();
                                intNames.add(interactionShortName);
                                edgedata.getDataSources().put(dataSourceName, intNames);
                            }
                        }
                    }
                }
            }
        }
    }
}
