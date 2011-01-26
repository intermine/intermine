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

import java.util.ArrayList;
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
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.logic.CytoscapeNetworkDBQueryRunner;
import org.intermine.bio.web.logic.CytoscapeNetworkGenerator;
import org.intermine.bio.web.logic.CytoscapeNetworkUtil;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Set up interaction network data for the cytoscapeInteractionsDisplayer.jsp
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
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        Gene hubGene = null;
        Set<CytoscapeNetworkNodeData> interactionNodeSet;
        Set<CytoscapeNetworkEdgeData> interactionEdgeSet;
        Set<Integer> geneIdSet; // a set contains gene object store ids

        HttpSession session = request.getSession(); // Get HttpSession
        final InterMineAPI im = SessionMethods.getInterMineAPI(session); // Get InterMineAPI
        ObjectStore os = im.getObjectStore(); // Get OS
        Model model = im.getModel(); // Get Model
        Profile profile = SessionMethods.getProfile(session); // Get Profile
        PathQueryExecutor executor = im.getPathQueryExecutor(profile); // Get PathQueryExecutor

        Map<String, Set<String>> interactionInfoMap = CytoscapeNetworkUtil
                .getInteractionInfo(model, executor);

        if (interactionInfoMap == null) {
            String dataNotIncludedMessage = "Interaction data is not included.";
            request.setAttribute("dataNotIncludedMessage", dataNotIncludedMessage);
            return null;
        }

        InterMineObject object = (InterMineObject) request
                .getAttribute("object"); // Get object from request

        String theNetwork = new String(); // Network data as a string in different formats

        CytoscapeNetworkGenerator dataGen = new CytoscapeNetworkGenerator();

        // Whether the object is a Gene or Protein
        if (object instanceof Protein) {
            Protein protein = (Protein) object;
            // TODO In most cases, there is only 1 gene in the collect, but rare cases with more
            // than 1, any examples?
            Set<Gene> genes = protein.getGenes();

            // TODO How to handle a protein with two or more genes - generate several networks?
            // for (Gene gene : genes) {
            hubGene = genes.iterator().next();
//            Set<CytoscapeNetworkEdgeData> interactionEdgeSubSet =
//                new HashSet<CytoscapeNetworkEdgeData>();
//            Set<CytoscapeNetworkNodeData> interactionNodeSubSet =
//                new HashSet<CytoscapeNetworkNodeData>();

        } else if (object instanceof Gene) {
            hubGene = (Gene) object;
        }

        Object hubGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(
                os.getObjectById(hubGene.getId()), im.getClassKeys());

        // Check if interaction data available for the organism
        String orgName = hubGene.getOrganism().getName();
        if (!interactionInfoMap.containsKey(orgName)) {
            String orgWithNoDataMessage = "No interaction data found for "
                    + orgName + " genes";
            request.setAttribute("orgWithNoDataMessage", orgWithNoDataMessage);
            return null;
        }

        ExportResultsIterator rawIntData = queryInteractionRawDataFromDB(hubGene, model, executor);

        if (rawIntData == null) {
            String dataSourceStr = StringUtil.join(interactionInfoMap.get(orgName), ",");
            String geneWithNoDatasourceMessage = "";

            if (hubGenekeyFldVal != null) {
                geneWithNoDatasourceMessage = "No interaction data found for "
                    + hubGenekeyFldVal + " from data sources: " + dataSourceStr;
            } else {
                geneWithNoDatasourceMessage = "No interaction data found for unknown gene"
                    + " from data sources: " + dataSourceStr;
            }

            request.setAttribute("geneWithNoDatasourceMessage", geneWithNoDatasourceMessage);
            return null;
        }

        // Parse raw data
        List<List<Object>> results = new ArrayList<List<Object>>();

        while (rawIntData.hasNext()) {
            List<ResultElement> row = rawIntData.next();

            List<Object> aRecord = new ArrayList<Object>();

            aRecord.add(row.get(0).getField());
            aRecord.add(row.get(1).getField());
            aRecord.add(row.get(2).getField());
            aRecord.add(row.get(3).getField());
            aRecord.add(row.get(4).getField());
            aRecord.add(row.get(5).getField());
            aRecord.add(row.get(6).getField());
            aRecord.add(row.get(7).getField());
            aRecord.add(row.get(8).getField());

            results.add(aRecord);
        }

        // Add the interaction network for this gene to the whole network
        geneIdSet = getGeneIdSet(results);
        interactionNodeSet = getInteractionNodeSet(results, im);
        interactionEdgeSet = getInteractionEdgeSet(results, im);

        theNetwork = dataGen.createGeneNetworkInXGMML(interactionNodeSet, interactionEdgeSet);

        request.setAttribute("geneOSIds", StringUtil.join(geneIdSet, ","));
        request.setAttribute("networkdata", theNetwork);

        if (hubGene.getPrimaryIdentifier() != null) {
            request.setAttribute("hubGene", hubGene.getPrimaryIdentifier());
        } else if (hubGenekeyFldVal != null) {
            request.setAttribute("hubGene", hubGenekeyFldVal);
        } else {
            request.setAttribute("hubGene", hubGene.getId());
        }

        return null;
    }

    /**
     * Query the interactions among a set of genes.
     *
     * @param gene the gene InterMine id
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return query results
     */
    private ExportResultsIterator queryInteractionRawDataFromDB(Gene gene,
            Model model, PathQueryExecutor executor) {

        // A list of gene InterMine ids including the hub and its interacting genes
        Set<Integer> keySet = new HashSet<Integer>();
        keySet.add(gene.getId());

        // Get all the genes that interact with the hub gene
        CytoscapeNetworkDBQueryRunner dbQuerier = new CytoscapeNetworkDBQueryRunner();
        Set<Integer> interactingGeneSet = dbQuerier.findInteractingGenes(
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
     * Create a set of gene intermine ids for webpage use.
     *
     * @param results raw data queried back from database
     * @return A set of gene intermine ids
     */
    private Set<Integer> getGeneIdSet(List<List<Object>> results) {

        Set<Integer> geneIdSet = new HashSet<Integer>();

        for (List<Object> aRecode : results) {

            Integer sourceGeneIMId = (Integer) aRecode.get(7);
            Integer targetGeneIMId = (Integer) aRecode.get(8);

            geneIdSet.add(sourceGeneIMId);
            geneIdSet.add(targetGeneIMId);
        }

        return geneIdSet;
    }

    /**
     * Create a set of CytoscapeNetworkNodeData objects for parsing them to xgmml.
     *
     * @param results raw data queried back from database
     * @param im InterMineAPI
     * @return A set of CytoscapeNetworkNodeData objects
     * @throws ObjectStoreException
     */
    private Set<CytoscapeNetworkNodeData> getInteractionNodeSet(
            List<List<Object>> results, InterMineAPI im) throws ObjectStoreException  {

        Set<CytoscapeNetworkNodeData> interactionNodeSet = new HashSet<CytoscapeNetworkNodeData>();

        for (List<Object> aRecode : results) {

            String sourceGenePID = (String) aRecode.get(0);
            String sourceGeneSymbol = (String) aRecode.get(1);
            Integer sourceGeneIMId = (Integer) aRecode.get(7);

            // === New Node ===
            CytoscapeNetworkNodeData aNode = new CytoscapeNetworkNodeData();

            aNode.setInterMineId(sourceGeneIMId);
            aNode.setSoureceId(String.valueOf(sourceGeneIMId)); // Use intermine id for source id

            Object sourceGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(im.getObjectStore()
                    .getObjectById(sourceGeneIMId), im.getClassKeys());

            if (sourceGeneSymbol != null) {
                aNode.setSourceLabel(sourceGeneSymbol);
            } else if (sourceGenePID != null) {
                aNode.setSourceLabel(sourceGenePID);
            } else if (sourceGenekeyFldVal != null) {
                aNode.setSourceLabel(String.valueOf(sourceGenekeyFldVal));
            } else {
                aNode.setSourceLabel("(Unknown Name)");
            }

            interactionNodeSet.add(aNode);
        }

        return interactionNodeSet;
    }

    /**
     * Create a set of CytoscapeNetworkEdgeData objects for parsing them to xgmml.
     *
     * @param results raw data queried back from database
     * @param im InterMineAPI
     * @return A set of CytoscapeNetworkEdgeData objects
     * @throws ObjectStoreException
     */
    private Set<CytoscapeNetworkEdgeData> getInteractionEdgeSet(List<List<Object>> results,
            InterMineAPI im) throws ObjectStoreException {

        Set<CytoscapeNetworkEdgeData> interactionEdgeSet = new HashSet<CytoscapeNetworkEdgeData>();

        for (List<Object> aRecode : results) {

            String sourceGenePID = (String) aRecode.get(0);
            String sourceGeneSymbol = (String) aRecode.get(1);
            String interactionType = (String) aRecode.get(2);
            String targetGenePID = (String) aRecode.get(3);
            String targetGeneSymbol = (String) aRecode.get(4);
            String dataSourceName = (String) aRecode.get(5);
            String interactionShortName = (String) aRecode.get(6);
            Integer sourceGeneIMId = (Integer) aRecode.get(7);
            Integer targetGeneIMId = (Integer) aRecode.get(8);

            Object sourceGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(im.getObjectStore()
                    .getObjectById(sourceGeneIMId), im.getClassKeys());
            Object targetGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(im.getObjectStore()
                    .getObjectById(targetGeneIMId), im.getClassKeys());

            // === New Edge ===
            CytoscapeNetworkEdgeData aEdge = new CytoscapeNetworkEdgeData();

            LinkedHashMap<String, Set<String>> dataSources =
                new LinkedHashMap<String, Set<String>>();

            LinkedHashSet<String> interactionShortNames = new LinkedHashSet<String>();

            aEdge.setSoureceId(String.valueOf(sourceGeneIMId));
            aEdge.setTargetId(String.valueOf(targetGeneIMId));

            if (sourceGeneSymbol != null) {
                aEdge.setSourceLabel(sourceGeneSymbol);
            } else if (sourceGenePID != null) {
                aEdge.setSourceLabel(sourceGenePID);
            } else if (sourceGenekeyFldVal != null) {
                aEdge.setSourceLabel(String.valueOf(sourceGenekeyFldVal));
            } else {
                aEdge.setSourceLabel("(Unknown Name)");
            }

            if (targetGeneSymbol != null) {
                aEdge.setTargetLabel(targetGeneSymbol);
            } else if (targetGenePID != null) {
                aEdge.setTargetLabel(targetGenePID);
            } else if (targetGenekeyFldVal != null) {
                aEdge.setTargetLabel(String.valueOf(targetGenekeyFldVal));
            } else {
                aEdge.setTargetLabel("(Unknown Name)");
            }

            aEdge.setInteractionType(interactionType);

            if (interactionEdgeSet.isEmpty()) {

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

        return interactionEdgeSet;
    }
}
