package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
    private static final int LARGE_NETWORK_ELEMENT_COUNT = 2000;;
    private static final String LARGE_NETWORK = "large_network";
    private static final String NO_INTERACTION_FROM =
        "No interaction data found from data sources: ";
    private static final String NO_INTERACTION_FOR_INPUT_GENE =
        "No interaction data found";

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeNetworkService.class);

    /**
     * {@inheritDoc}
     * @throws ObjectStoreException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public String getNetwork(String fullInteractingGeneSetStr,
            HttpSession session, boolean ignoreLargeNetworkTest) throws ObjectStoreException,
            InterruptedException, ExecutionException {

        final InterMineAPI im = SessionMethods.getInterMineAPI(session); // Get InterMineAPI
        ObjectStore os = im.getObjectStore(); // Get OS
        Model model = im.getModel(); // Get Model
        Profile profile = SessionMethods.getProfile(session); // Get Profile
        PathQueryExecutor executor = im.getPathQueryExecutor(profile); // Get PathQueryExecutor

        Map<String, Set<String>> interactionInfoMap = CytoscapeNetworkUtil
        .getInteractionInfo(model, executor);

        // === Prepare data ===
        List<String> fullInteractingGeneList = StringUtil.tokenize(fullInteractingGeneSetStr, ",");

        if (fullInteractingGeneList.size() >= LARGE_NETWORK_ELEMENT_COUNT) {
            return LARGE_NETWORK;
        }

        Set<Integer> fullInteractingGeneSet = new HashSet<Integer>();
        for (String s : fullInteractingGeneList) {
            fullInteractingGeneSet.add(Integer.valueOf(s));
        }

        //=== Query interactions ===
        CytoscapeNetworkDBQueryRunner queryRunner = new CytoscapeNetworkDBQueryRunner();
        ExportResultsIterator rawIntData = queryRunner.getInteractions(
                fullInteractingGeneSet, model, executor);

        //=== Validation ===
        if (rawIntData == null) {
            Gene aTestGene = (Gene) os.getObjectById(fullInteractingGeneSet.iterator().next());
            String orgName = aTestGene.getOrganism().getName();
            String dataSourceStr = StringUtil.join(interactionInfoMap.get(orgName), ",");
            String geneWithNoDatasourceMessage = NO_INTERACTION_FROM + dataSourceStr;

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
        Map<String, CytoscapeNetworkNodeData> interactionNodeMap = getInteractionNodeMap(
                results, im);
        Map<String, CytoscapeNetworkEdgeData> interactionEdgeMap = getInteractionEdgeMap(
                results, im);

        // case: input genes have no interactions
        if (interactionEdgeMap.size() == 0) {
            return NO_INTERACTION_FOR_INPUT_GENE;
        }

        if (!ignoreLargeNetworkTest) {
            // Simple network filter
            if (interactionNodeMap.size() + interactionEdgeMap.size()
                    >= LARGE_NETWORK_ELEMENT_COUNT) {
                return LARGE_NETWORK;
            }
        }

        CytoscapeNetworkGenerator dataGen = new CytoscapeNetworkGenerator();

        String networkdata = dataGen.createGeneNetworkInXGMML(
                interactionNodeMap, interactionEdgeMap);

        return networkdata;
    }

    /**
     * Create a map of CytoscapeNetworkNodeData objects for parsing them to xgmml.
     *
     * @param results raw data queried back from database
     * @param im InterMineAPI
     * @return A map of CytoscapeNetworkNodeData objects
     * @throws ObjectStoreException
     */
    private Map<String, CytoscapeNetworkNodeData> getInteractionNodeMap(
            List<List<Object>> results, InterMineAPI im) throws ObjectStoreException  {

        Map<String, CytoscapeNetworkNodeData> interactionNodeMap =
            new HashMap<String, CytoscapeNetworkNodeData>();


        for (List<Object> aRecode : results) {

            Integer sourceId = (Integer) aRecode.get(7);
            Integer targetId = (Integer) aRecode.get(8);

            if (!interactionNodeMap.containsKey(String.valueOf(sourceId))
                    && !interactionNodeMap.containsKey(String.valueOf(targetId))) {
                String sourcePID = (String) aRecode.get(0);
                String sourceSymbol = (String) aRecode.get(1);
                String targetPID = (String) aRecode.get(3);
                String targetSymbol = (String) aRecode.get(4);
                // === New Node ===
                CytoscapeNetworkNodeData sourceNode = new CytoscapeNetworkNodeData();
                CytoscapeNetworkNodeData targetNode = new CytoscapeNetworkNodeData();

                sourceNode.setInterMineId(sourceId);
                sourceNode.setSourceId(String.valueOf(sourceId)); // Use intermine id for source id

                Object sourceGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(im.getObjectStore()
                        .getObjectById(sourceId), im.getClassKeys());

                if (sourceSymbol != null) {
                    sourceNode.setSourceLabel(sourceSymbol);
                } else if (sourcePID != null) {
                    sourceNode.setSourceLabel(sourcePID);
                } else if (sourceGenekeyFldVal != null) {
                    sourceNode.setSourceLabel(String.valueOf(sourceGenekeyFldVal));
                } else {
                    sourceNode.setSourceLabel("(Unknown Name)");
                }

                interactionNodeMap.put(String.valueOf(sourceId), sourceNode);

                targetNode.setInterMineId(targetId);
                targetNode.setSourceId(String.valueOf(targetId)); // Use intermine id for source id

                Object targetGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(im.getObjectStore()
                        .getObjectById(targetId), im.getClassKeys());

                if (targetSymbol != null) {
                    targetNode.setSourceLabel(targetSymbol);
                } else if (targetPID != null) {
                    targetNode.setSourceLabel(targetPID);
                } else if (targetGenekeyFldVal != null) {
                    targetNode.setSourceLabel(String.valueOf(targetGenekeyFldVal));
                } else {
                    targetNode.setSourceLabel("(Unknown Name)");
                }

                interactionNodeMap.put(String.valueOf(targetId), targetNode);
            }

            if (!interactionNodeMap.containsKey(String.valueOf(sourceId))
                    && interactionNodeMap.containsKey(String.valueOf(targetId))) {
                String sourcePID = (String) aRecode.get(0);
                String sourceSymbol = (String) aRecode.get(1);

                // === New Node ===
                CytoscapeNetworkNodeData sourceNode = new CytoscapeNetworkNodeData();

                sourceNode.setInterMineId(sourceId);
                sourceNode.setSourceId(String.valueOf(sourceId)); // Use intermine id for source id

                Object sourceGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(im.getObjectStore()
                        .getObjectById(sourceId), im.getClassKeys());

                if (sourceSymbol != null) {
                    sourceNode.setSourceLabel(sourceSymbol);
                } else if (sourcePID != null) {
                    sourceNode.setSourceLabel(sourcePID);
                } else if (sourceGenekeyFldVal != null) {
                    sourceNode.setSourceLabel(String.valueOf(sourceGenekeyFldVal));
                } else {
                    sourceNode.setSourceLabel("(Unknown Name)");
                }

                interactionNodeMap.put(String.valueOf(sourceId), sourceNode);
            }

            if (interactionNodeMap.containsKey(String.valueOf(sourceId))
                    && !interactionNodeMap.containsKey(String.valueOf(targetId))) {

                String targetPID = (String) aRecode.get(3);
                String targetSymbol = (String) aRecode.get(4);

                // === New Node ===
                CytoscapeNetworkNodeData targetNode = new CytoscapeNetworkNodeData();

                targetNode.setInterMineId(targetId);
                targetNode.setSourceId(String.valueOf(targetId)); // Use intermine id for source id

                Object targetGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(im.getObjectStore()
                        .getObjectById(targetId), im.getClassKeys());

                if (targetSymbol != null) {
                    targetNode.setSourceLabel(targetSymbol);
                } else if (targetPID != null) {
                    targetNode.setSourceLabel(targetPID);
                } else if (targetGenekeyFldVal != null) {
                    targetNode.setSourceLabel(String.valueOf(targetGenekeyFldVal));
                } else {
                    targetNode.setSourceLabel("(Unknown Name)");
                }

                interactionNodeMap.put(String.valueOf(targetId), targetNode);
            }

        }

        return interactionNodeMap;
    }

    /**
     * Create a map of CytoscapeNetworkEdgeData objects for parsing them to xgmml.
     *
     * @param results raw data queried back from database
     * @param im InterMineAPI
     * @return A map of CytoscapeNetworkEdgeData objects
     * @throws ObjectStoreException
     */
    private Map<String, CytoscapeNetworkEdgeData> getInteractionEdgeMap(List<List<Object>> results,
            InterMineAPI im) throws ObjectStoreException {

        Map<String, CytoscapeNetworkEdgeData> interactionEdgeMap =
            new HashMap<String, CytoscapeNetworkEdgeData>();

        for (List<Object> aRecode : results) {

            String sourcePID = (String) aRecode.get(0);
            String sourceSymbol = (String) aRecode.get(1);
            String interactionType = (String) aRecode.get(2);
            String targetPID = (String) aRecode.get(3);
            String targetSymbol = (String) aRecode.get(4);
            String dataSourceName = (String) aRecode.get(5);
            String interactionShortName = (String) aRecode.get(6);
            Integer sourceId = (Integer) aRecode.get(7);
            Integer targetId = (Integer) aRecode.get(8);

            Object sourceGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(im.getObjectStore()
                    .getObjectById(sourceId), im.getClassKeys());
            Object targetGenekeyFldVal = ClassKeyHelper.getKeyFieldValue(im.getObjectStore()
                    .getObjectById(targetId), im.getClassKeys());

            // === New Edge ===
            CytoscapeNetworkEdgeData aEdge = new CytoscapeNetworkEdgeData();

            LinkedHashMap<String, Set<String>> dataSources =
                new LinkedHashMap<String, Set<String>>();

            LinkedHashSet<String> interactionShortNames = new LinkedHashSet<String>();

            aEdge.setSourceId(String.valueOf(sourceId));
            aEdge.setTargetId(String.valueOf(targetId));

            if (sourceSymbol != null) {
                aEdge.setSourceLabel(sourceSymbol);
            } else if (sourcePID != null) {
                aEdge.setSourceLabel(sourcePID);
            } else if (sourceGenekeyFldVal != null) {
                aEdge.setSourceLabel(String.valueOf(sourceGenekeyFldVal));
            } else {
                aEdge.setSourceLabel("(Unknown Name)");
            }

            if (targetSymbol != null) {
                aEdge.setTargetLabel(targetSymbol);
            } else if (targetPID != null) {
                aEdge.setTargetLabel(targetPID);
            } else if (targetGenekeyFldVal != null) {
                aEdge.setTargetLabel(String.valueOf(targetGenekeyFldVal));
            } else {
                aEdge.setTargetLabel("(Unknown Name)");
            }

            aEdge.setInteractionType(interactionType);

            String id = sourceId + "-" + targetId;
            String idRev = targetId + "-" + sourceId;
            aEdge.setId(id);

            if (!interactionEdgeMap.containsKey(id) && !interactionEdgeMap.containsKey(idRev)) {
                interactionShortNames.add(interactionShortName);
                dataSources.put(dataSourceName, interactionShortNames);
                aEdge.setDataSources(dataSources);
                aEdge.setDirection("one");

                interactionEdgeMap.put(id, aEdge);
            } else {
                // Dulipcated edge
                if (interactionEdgeMap.containsKey(id)) {
                    if (interactionEdgeMap.get(id).getDataSources().containsKey(dataSourceName)) {
                        interactionEdgeMap.get(id).getDataSources().get(dataSourceName)
                                .add(interactionShortName);
                    } else {
                        LinkedHashSet<String> intNames = new LinkedHashSet<String>();
                        intNames.add(interactionShortName);
                        interactionEdgeMap.get(id).getDataSources().put(dataSourceName, intNames);
                    }
                } else if (interactionEdgeMap.containsKey(idRev)) {
                    interactionEdgeMap.get(idRev).setId(idRev);
                    interactionEdgeMap.get(idRev).setDirection("both");
                    if (interactionEdgeMap.get(idRev).getDataSources()
                            .containsKey(dataSourceName)) {
                        interactionEdgeMap.get(idRev).getDataSources().get(dataSourceName)
                                .add(interactionShortName);
                    } else {
                        LinkedHashSet<String> intNames = new LinkedHashSet<String>();
                        intNames.add(interactionShortName);
                        interactionEdgeMap.get(idRev).getDataSources()
                                .put(dataSourceName, intNames);
                    }
                }
            }
        }

        return interactionEdgeMap;
    }
}
