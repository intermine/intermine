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
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.logic.CytoscapeInteractionDBQuerier;
import org.intermine.bio.web.logic.CytoscapeInteractionDataGenerator;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        // Get InterMineAPI
        HttpSession session = request.getSession();

        // Get object from request
        InterMineObject object = (InterMineObject) request
                .getAttribute("object");

        // Network data as a string in different formats
        String theNetwork = new String();

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

            ExportResultsIterator rawIntData = queryInteractionRawDataFromDB(gene, session);

            if (rawIntData == null) {
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

            ExportResultsIterator rawIntData = queryInteractionRawDataFromDB(gene, session);

            if (rawIntData == null) {
                return null;
            }

            prepareNetworkData(rawIntData);

            request.setAttribute("hubGene", gene.getPrimaryIdentifier());
        }

        theNetwork = dataGen.createGeneNetworkInXGMML(interactionEdgeSet, interactionNodeSet);

        request.setAttribute("networkdata", theNetwork);

        return null;
    }

    /**
     * Query the interactions among a set of genes.
     *
     * @param gene a gene pid
     * @param session
     * @return query results
     */
    private ExportResultsIterator queryInteractionRawDataFromDB(Gene gene, HttpSession session) {

        // A list of genes including the hub and its interacting genes
        Set<String> keySet = new HashSet<String>();
        keySet.add(gene.getPrimaryIdentifier());

        // Get all the genes that interact with the hub gene
        CytoscapeInteractionDBQuerier dbQuerier = new CytoscapeInteractionDBQuerier();
        Set<String> interactingGeneSet = dbQuerier.findInteractingGenes(
                gene.getPrimaryIdentifier(), session);
        if (interactingGeneSet == null) {
            return null;
        }

        keySet.addAll(interactingGeneSet);

        // Get all interactions between a set of genes
        ExportResultsIterator results = dbQuerier.queryInteractions(keySet, session);
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

            CytoscapeNetworkNodeData aNode = new CytoscapeNetworkNodeData();

            aNode.setSoureceId(genePID);

            if (geneSymbol == null || geneSymbol.length() < 1) {
                aNode.setSourceLabel(genePID);
            } else {
                aNode.setSourceLabel(geneSymbol);
            }

            interactionNodeSet.add(aNode);

            CytoscapeNetworkEdgeData aEdge = new CytoscapeNetworkEdgeData();
            LinkedHashMap<String, Set<String>> dataSources =
                new LinkedHashMap<String, Set<String>>();
            LinkedHashSet<String> interactionShortNames = new LinkedHashSet<String>();

            if (interactionEdgeSet.isEmpty()) {
                aEdge.setSoureceId(genePID);

                if (geneSymbol == null || geneSymbol.length() < 1) {
                    aEdge.setSourceLabel(genePID);
                } else {
                    aEdge.setSourceLabel(geneSymbol);
                }

                aEdge.setTargetId(interactingGenePID);

                if (interactingGeneSymbol == null || interactingGeneSymbol.length() < 1) {
                    aEdge.setTargetLabel(interactingGenePID);
                } else {
                    aEdge.setTargetLabel(interactingGeneSymbol);
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
                    aEdge.setSourceLabel(genePID);
                } else {
                    aEdge.setSourceLabel(geneSymbol);
                }

                aEdge.setTargetId(interactingGenePID);

                if (interactingGeneSymbol == null || interactingGeneSymbol.length() < 1) {
                    aEdge.setTargetLabel(interactingGenePID);
                } else {
                    aEdge.setTargetLabel(interactingGeneSymbol);
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
