package org.intermine.bio.web.logic;

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

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Gene;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
* This service class contains the logics of calling querying database and creating network data.
*
* @author Fengyuan Hu
*/
public class CytoscapeNetworkService
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeNetworkService.class);

    /**
     * {@inheritDoc}
     * @throws ObjectStoreException
     */
    public String getNetwork(Set<Integer> fullInteractingGeneSet,
            HttpSession session) throws ObjectStoreException {

        final InterMineAPI im = SessionMethods.getInterMineAPI(session); // Get InterMineAPI
        ObjectStore os = im.getObjectStore(); // Get OS
        Model model = im.getModel(); // Get Model
        Profile profile = SessionMethods.getProfile(session); // Get Profile
        PathQueryExecutor executor = im.getPathQueryExecutor(profile); // Get PathQueryExecutor

        Map<String, Set<String>> interactionInfoMap = CytoscapeNetworkUtil
        .getInteractionInfo(model, executor);

        //=== Query interactions ===
        CytoscapeNetworkDBQueryRunner queryRunner = new CytoscapeNetworkDBQueryRunner();
        ExportResultsIterator rawIntData = queryRunner.getInteractions(
                fullInteractingGeneSet, model, executor);

        //=== Validation ===
        if (rawIntData == null) {
            Gene aTestGene = (Gene) os.getObjectById(fullInteractingGeneSet.iterator().next());
            String orgName = aTestGene.getOrganism().getName();
            String dataSourceStr = StringUtil.join(interactionInfoMap.get(orgName), ",");
            String geneWithNoDatasourceMessage = "No interaction data found from data sources: "
                + dataSourceStr;

            return geneWithNoDatasourceMessage;
        }

        //=== Parse raw data ===
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
        Set<CytoscapeNetworkNodeData> interactionNodeSet = getInteractionNodeSet(results, im);
        Set<CytoscapeNetworkEdgeData> interactionEdgeSet = getInteractionEdgeSet(results, im);

        CytoscapeNetworkGenerator dataGen = new CytoscapeNetworkGenerator();
        String networkdata = dataGen.createGeneNetworkInXGMML(
                interactionNodeSet, interactionEdgeSet);

        return networkdata;
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
