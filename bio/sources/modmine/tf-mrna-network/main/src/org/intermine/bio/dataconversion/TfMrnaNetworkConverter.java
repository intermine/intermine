package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * DataConverter to create items from modENCODE fly TF-miRNA regulatory network files.
 * TFs have three data fields: symbol, primaryId, level.
 * miRNAs have three data fields: symbol, full names, position.
 *
 * @author Fengyuan Hu
 */
public class TfMrnaNetworkConverter extends BioDirectoryConverter
{
    private static final Logger LOG = Logger.getLogger(TfMrnaNetworkConverter.class);

    private static final String DATASET_TITLE =
        "Fly Transcription Factor/microRNA Regulatory Network";
    private static final String DATA_SOURCE_NAME = "Manolis Kellis";
    private static final String FLY_TAXON_ID = "7227";

    protected IdResolverFactory resolverFactory;

    private static final String INTERACTION_TYPE_TF_MIRNA = "TF-miRNA";
    private static final String INTERACTION_TYPE_MIRNA_TF = "miRNA-TF";
    private static final String INTERACTION_TYPE_TF_TF = "TF-TF";
    private static final String INTERACTION_TYPE_MIRNA_MIRNA = "miRNA-miRNA";
    private static final String TOPO_TYPE_LEVEL = "level";
    private static final String TOPO_TYPE_POSITION = "position";

    private Map<String, Map<String, String>> tfMap = new HashMap<String, Map<String, String>>();
    private Map<String, Map<String, String>> miRNAMap = new HashMap<String, Map<String, String>>();

    // remove duplication when creating gene items
    // key:primaryId - value:bioenetity.identifier
    private Map<String, String> geneItems = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public TfMrnaNetworkConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);

        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    /**
     * Called for each file found by ant.
     *
     * {@inheritDoc}
     */
    public void process(File dataDir) throws Exception {
        // There are three files:
        // - topos_tf.tsv - with topology information for transcription factors.
        // - topos_mirna.tsv - with topology information for miRNAs.
        // - PRN-allEdges.txt - full interaction data set.

        // the files need to be ordered as topos_tf.tsv, topos_mirna.tsv and PRN-allEdges.txt.
        List<File> orderedFiles  = new ArrayList<File>();
        File[] files = dataDir.listFiles();
        if (files == null) {
            throw new IllegalArgumentException("The directory has no data files");
        } else {
            orderedFiles = parseFileNames(files);
        }

        tfMap = processTopoFile(orderedFiles.get(0));
        miRNAMap = processTopoFile(orderedFiles.get(1));

        processEdgeFile(orderedFiles.get(2), tfMap, miRNAMap);
    }

    /**
     * The network files need to be read and parsed in an order.
     *
     * @param fileList the files in a random order.
     * @return files the files in an order as desire.
     */
    private List<File> parseFileNames(File[] fileList) {
        List<File> files = new ArrayList<File>();
        File tfTopo = null;
        File miRNATopo = null;
        File edges = null;

        for (File file : fileList) {
            if ("topos_tf.tsv".equals(file.getName())) {
                tfTopo = file;
            }
            if ("topos_mirna.tsv".equals(file.getName())) {
                miRNATopo = file;
            }
            if ("PRN-allEdges.txt".equals(file.getName())) {
                edges = file;
            }
        }

        files.add(tfTopo);
        files.add(miRNATopo);
        files.add(edges);

        if (files.contains(null)) {
            throw new IllegalArgumentException("Some data file missing");
        }

        return files;
    }

    /**
     * Process Topo files and create two maps.
     *
     * @param file a topo file.
     * @return Map<key:primaryId/fullName, value:Map<key:symbol/symbol, value:level/position>>
     */
    private Map<String, Map<String, String>> processTopoFile(File file) {
        Map<String, Map<String, String>> topoMap = new HashMap<String, Map<String, String>>();

        try {
            Reader reader = new FileReader(file);
            Iterator<?> tsvIter;
            try {
                tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            while (tsvIter.hasNext()) {
                String[] line = (String[]) tsvIter.next();
                if (line.length > 1) { // the file could end with an empty line
                    if ("topos_tf.tsv".equals(file.getName())) {
                        String symbol = line[0];
                        String primaryId = line[1];
                        String level = line[2];

                        Map<String, String> aMap = new HashMap<String, String>();
                        aMap.put(symbol, level);
                        topoMap.put(primaryId, aMap);
                    } else if ("topos_mirna.tsv".equals(file.getName())) {
                        String symbol = line[0];
                        String fullName = line[1];
                        String position = line[2];

                        Map<String, String> aMap = new HashMap<String, String>();
                        aMap.put(symbol, position);
                        topoMap.put(fullName, aMap);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return topoMap;
    }

    /**
     * Process the edge data file.
     *
     * @param file the edge data file
     * @param tfMap a customized map with TF information
     * @param miRNAMap a customized map with miRNA information
     */
    private void processEdgeFile(File file,
            Map<String, Map<String, String>> tfMap,
            Map<String, Map<String, String>> miRNAMap) {

        try {
            Reader reader = new FileReader(file);
            Iterator<?> tsvIter;
            try {
                tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            while (tsvIter.hasNext()) {
                String[] line = (String[]) tsvIter.next();
                if (line.length > 1) {
                    String sourceIdentifier = line[0];
                    String targetIdentifier = line[1];

                    try {
                        if (tfMap.containsKey(sourceIdentifier)) {
                            // Create source gene
                            String sourceSymbol =
                                tfMap.get(sourceIdentifier).keySet().iterator().next();
                            String sourceLevel = tfMap.get(sourceIdentifier).get(sourceSymbol);
                            String sourceGenePid = createGene(sourceIdentifier, sourceSymbol);

                            // Create networkProperty for source gene
                            Item sourceNetworkProperty =
                                createNetworkProperty(TOPO_TYPE_LEVEL, sourceLevel);
                            sourceNetworkProperty.setReference("node",
                                    geneItems.get(sourceGenePid));
                            store(sourceNetworkProperty);

                            if (tfMap.containsKey(targetIdentifier)) {
                                // Create regulation for both genes
                                Item regulation = createRegulation(INTERACTION_TYPE_TF_TF);

                                // Create target gene
                                String targetSymbol =
                                    tfMap.get(targetIdentifier).keySet().iterator().next();
                                String targetLevel =
                                    tfMap.get(targetIdentifier).get(targetSymbol);
                                String targetGenePid =
                                    createGene(targetIdentifier, targetSymbol);

                                // Create networkProperty for target gene
                                Item targetNetworkProperty =
                                    createNetworkProperty(TOPO_TYPE_LEVEL, targetLevel);
                                targetNetworkProperty.setReference("node",
                                        geneItems.get(targetGenePid));
                                store(targetNetworkProperty);

                                regulation.setReference("source", geneItems.get(sourceGenePid));
                                regulation.setReference("target", geneItems.get(targetGenePid));
                                store(regulation);
                            } else if (miRNAMap.containsKey(targetIdentifier)) {
                                // Create regulation for both genes
                                Item regulation = createRegulation(INTERACTION_TYPE_TF_MIRNA);

                                // Create target gene
                                String targetSymbol =
                                    miRNAMap.get(targetIdentifier).keySet().iterator().next();
                                String targetPosition =
                                    miRNAMap.get(targetIdentifier).get(targetSymbol);
                                String targetGenePid = createGene(null, targetSymbol);

                                // Create networkProperty for target gene
                                Item targetNetworkProperty =
                                    createNetworkProperty(TOPO_TYPE_POSITION, targetPosition);
                                targetNetworkProperty.setReference("node",
                                        geneItems.get(targetGenePid));
                                store(targetNetworkProperty);

                                regulation.setReference("source", geneItems.get(sourceGenePid));
                                regulation.setReference("target", geneItems.get(targetGenePid));
                                store(regulation);
                            } else { continue; }
                        } else if (miRNAMap.containsKey(sourceIdentifier)) {
                            // Create source gene
                            String sourceSymbol =
                                miRNAMap.get(sourceIdentifier).keySet().iterator().next();
                            String sourcePosition =
                                miRNAMap.get(sourceIdentifier).get(sourceSymbol);
                            String sourceGenePid = createGene(null, sourceSymbol);

                            // Create networkProperty for source gene
                            Item sourceNetworkProperty =
                                createNetworkProperty(TOPO_TYPE_POSITION, sourcePosition);
                            sourceNetworkProperty.setReference("node",
                                    geneItems.get(sourceGenePid));
                            store(sourceNetworkProperty);

                            if (tfMap.containsKey(targetIdentifier)) {
                                // Create regulation for both genes
                                Item regulation = createRegulation(INTERACTION_TYPE_MIRNA_TF);

                                // Create target gene
                                String targetSymbol =
                                    tfMap.get(targetIdentifier).keySet().iterator().next();
                                String targetLevel =
                                    tfMap.get(targetIdentifier).get(targetSymbol);
                                String targetGenePid =
                                    createGene(targetIdentifier, targetSymbol);

                                // Create networkProperty for target gene
                                Item targetNetworkProperty =
                                    createNetworkProperty(TOPO_TYPE_LEVEL, targetLevel);
                                targetNetworkProperty.setReference("node",
                                        geneItems.get(targetGenePid));
                                store(targetNetworkProperty);

                                regulation.setReference("source", geneItems.get(sourceGenePid));
                                regulation.setReference("target", geneItems.get(targetGenePid));
                                store(regulation);
                            } else if (miRNAMap.containsKey(targetIdentifier)) {
                                // Create regulation for both genes
                                Item regulation = createRegulation(INTERACTION_TYPE_MIRNA_MIRNA);

                                // Create target gene
                                String targetSymbol =
                                    miRNAMap.get(targetIdentifier).keySet().iterator().next();
                                String targetPosition =
                                    miRNAMap.get(targetIdentifier).get(targetSymbol);
                                String targetGenePid = createGene(null, targetSymbol);

                                // Create networkProperty for target gene
                                Item targetNetworkProperty =
                                    createNetworkProperty(TOPO_TYPE_POSITION, targetPosition);
                                targetNetworkProperty.setReference("node",
                                        geneItems.get(targetGenePid));
                                store(targetNetworkProperty);

                                regulation.setReference("source", geneItems.get(sourceGenePid));
                                regulation.setReference("target", geneItems.get(targetGenePid));
                                store(regulation);
                            } else { continue; }
                        } else { continue; }
                    } catch (ObjectStoreException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Create and store a NetworkProperty item on the first time called.
     *
     * @param type e.g. "level", "position"
     * @param value the value of level or position
     * @return an Item representing the NetworkProperty
     */
    private Item createNetworkProperty(String type, String value) throws ObjectStoreException {
        Item networkproperty = createItem("NetworkProperty");
        networkproperty.setAttribute("type", type);
        networkproperty.setAttribute("value", value);

        return networkproperty;
    }

    /**
     * Create and store a Regulation item on the first time called.
     *
     * @param type the type of interaction, e.g. "TF-TF", "TF-miRNA", etc.
     * @return an Item representing the Regulation
     */
    private Item createRegulation(String type) throws ObjectStoreException {
        Item regulation = createItem("Regulation");
        regulation.setAttribute("type", type);

        return regulation;
    }

    /**
     * Resolve Gene primaryId from a symbol.
     *
     * @param primaryId the fly gene primaryId
     * @param symbol the fly gene symbol
     * @return gene primaryId
     * @throws ObjectStoreException
     */
    private String createGene(String primaryId, String symbol) throws ObjectStoreException {

        if (primaryId == null) { // for miRNA case
            if ("mlc-c_in1".endsWith(symbol)) {
                symbol = "Mlc-c";
            }
            if ("mir-iab-4as".endsWith(symbol)) {
                symbol = "mir-iab-8";
            }
            IdResolver resolver = resolverFactory.getIdResolver();
            int resCount = resolver.countResolutions(FLY_TAXON_ID, symbol);
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                         + symbol + " count: " + resCount + " FBgn: "
                         + resolver.resolveId(FLY_TAXON_ID, symbol));
            }
            // TOFIX: temp shortcut caused by an error in FB2012_04 (missing this record)
            if ("mir-316".endsWith(symbol)) {
                primaryId = "FBgn0262417";
            } else {
            	primaryId = resolver.resolveId(FLY_TAXON_ID, symbol).iterator().next();
            }
        }

        if (!geneItems.containsKey(primaryId)) {
            createBioEntity("Gene", primaryId, symbol);
        }

        return primaryId;
    }

    /**
     * Create and store a BioEntity item on the first time called.
     *
     * @param type gene
     * @param primaryId the gene primaryIdentifier
     * @param symbol the gene symbol
     * @throws ObjectStoreException
     */
    private void createBioEntity(String type, String primaryId, String symbol)
        throws ObjectStoreException {
        Item bioentity = null;

        bioentity = createItem("Gene");
        bioentity.setAttribute("primaryIdentifier", primaryId);
        bioentity.setAttribute("symbol", symbol);
        store(bioentity);

        geneItems.put(primaryId, bioentity.getIdentifier());
    }
}
