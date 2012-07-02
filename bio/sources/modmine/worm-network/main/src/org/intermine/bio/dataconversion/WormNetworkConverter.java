package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
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
 * DataConverter to create items from modENCODE worm TF-miRNA regulatory network files.
 * TFs have three data fields: symbol, primaryId, level.
 * miRNAs have three data fields: symbol, full names, position.
 *
 * @author
 *
 * TODO: merge with the fly one
 */

public class WormNetworkConverter extends BioDirectoryConverter
{
    private static final Logger LOG = Logger.getLogger(WormNetworkConverter.class);

    private String orgRefId;
    private static final String WORM_DATASET_TITLE =
        "Worm Transcription Factor/microRNA Regulatory Network";
    private static final String WORM_DATA_SOURCE_NAME = "Mark Gerstein";
    private static final String WORM_TAXON_ID = "6239";

    protected IdResolverFactory resolverFactory;

    private static final String INTERACTION_TYPE_TF_MIRNA = "TF-miRNA";
    private static final String INTERACTION_TYPE_MIRNA_TF = "miRNA-TF";
    private static final String INTERACTION_TYPE_TF_TF = "TF-TF";
    private static final String INTERACTION_TYPE_MIRNA_MIRNA = "miRNA-miRNA";
//    private static final String TOPO_TYPE_LEVEL = "level";
    private static final String TOPOS_VPOS = "vposition";
    private static final String TOPOS_HPOS = "hposition";
    private static final String TOPOS_TYPE = "TF_type";

    // key:symbol [vpos, type]
    private Map<String, String[]> tfMap = new HashMap<String, String[]>();
    // key:symbol [vpos, hpos]
    private Map<String, String[]> miRNAMap = new HashMap<String, String[]>();


    // remove duplication when creating gene items
    // key:primaryId/symbol - value:bioentity.identifier
    private Map<String, String> geneItems = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public WormNetworkConverter(ItemWriter writer, Model model) {
        super(writer, model, WORM_DATA_SOURCE_NAME, WORM_DATASET_TITLE);
        resolverFactory = new WormBaseChadoIdResolverFactory("gene");
        orgRefId = getOrganism(WORM_TAXON_ID);
    }


    /**
     * Called for each file found by ant.
     *
     *
     * {@inheritDoc}
     */
    public void process(File dataDir) throws Exception {
        // There are three files:
        // - topos_tf.tsv - with topology information for transcription factors.
        // - topos_mirna.tsv - with topology information for miRNAs.
        // - edges.txt - full interaction data set.

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
            if ("edges.txt".equals(file.getName())) {
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
    private Map<String, String[]> processTopoFile(File file) {
        Map<String, String[]> topoMap = new HashMap<String, String[]>();

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
                        String vposition = line[1];
                        String type = line[2];

                        String[] aCoord = {vposition, type};
                        topoMap.put(symbol, aCoord);
                    } else if ("topos_mirna.tsv".equals(file.getName())) {
                        String symbol = line[0];
                        String vposition = line[1];
                        String hposition = line[2];

                        String[] aCoord = {vposition, hposition};
                        topoMap.put(symbol, aCoord);
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
            Map<String, String[]> tfMap,
            Map<String, String[]> miRNAMap) {
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
                    LOG.debug("WN edge: " + sourceIdentifier + "->" + targetIdentifier);
                    try {
                        if (tfMap.containsKey(sourceIdentifier)) {
                            String sourceVpos = tfMap.get(sourceIdentifier)[0];
                            String sourceType = tfMap.get(sourceIdentifier)[1];
                            // NOTE: identifiers are symbols for worm
                            String sourceGenePid = createGene(null, sourceIdentifier);

                           // if (sourceGenePid != null) {
                            Item sourceNetworkPropertyVpos = addNetworkProperty(TOPOS_VPOS,
                                    sourceVpos, sourceGenePid);
                            Item sourceNetworkPropertyType = addNetworkProperty(TOPOS_TYPE,
                                    sourceType, sourceGenePid);

                            if (tfMap.containsKey(targetIdentifier)) {
                                // Create regulation for both genes
                                Item regulation = createRegulation(INTERACTION_TYPE_TF_TF);

                                // Create target gene
                                String targetVpos =
                                    tfMap.get(targetIdentifier)[0];
                                String targetType =
                                    tfMap.get(targetIdentifier)[1];
                                String targetGenePid =
                                    createGene(null, targetIdentifier);

                                Item targetNetworkPropertyVpos = addNetworkProperty(TOPOS_VPOS,
                                        targetVpos, targetGenePid);
                                Item targetNetworkPropertyType = addNetworkProperty(TOPOS_TYPE,
                                        targetType, targetGenePid);

                                regulation.setReference("source", geneItems.get(sourceGenePid));
                                regulation.setReference("target", geneItems.get(targetGenePid));
                                store(regulation);
                            }else if (miRNAMap.containsKey(targetIdentifier)) {
                                // Create regulation for both genes
                                Item regulation = createRegulation(INTERACTION_TYPE_TF_MIRNA);

                                // Create target gene
                                String targetVpos =
                                    miRNAMap.get(targetIdentifier)[0];
                                String targetHpos =
                                    miRNAMap.get(targetIdentifier)[1];
                                String targetGenePid = createGene(null, targetIdentifier);

                                // Create networkProperty for target gene
                                Item targetNetworkPropertyVpos = addNetworkProperty(TOPOS_VPOS,
                                        targetVpos, targetGenePid);
                                Item targetNetworkPropertyHpos = addNetworkProperty(TOPOS_HPOS,
                                        targetHpos, targetGenePid);

                                regulation.setReference("source", geneItems.get(sourceGenePid));
                                regulation.setReference("target", geneItems.get(targetGenePid));
                                store(regulation);
                            } else { continue; } //}
                        } else if (miRNAMap.containsKey(sourceIdentifier)) {
                            // Create source gene
                            String sourceVpos =
                                miRNAMap.get(sourceIdentifier)[0];
                            String sourceHpos =
                                miRNAMap.get(sourceIdentifier)[1];
                            String sourceGenePid = createGene(null, sourceIdentifier);

                            // Create networkProperty for source gene
                            Item sourceNetworkPropertyVpos = addNetworkProperty(TOPOS_VPOS,
                                    sourceVpos, sourceGenePid);
                            Item sourceNetworkPropertyHpos = addNetworkProperty(TOPOS_HPOS,
                                    sourceHpos, sourceGenePid);

                            if (tfMap.containsKey(targetIdentifier)) {
                                // Create regulation for both genes
                                Item regulation = createRegulation(INTERACTION_TYPE_MIRNA_TF);

                                // Create target gene
                                String targetVpos =
                                    tfMap.get(targetIdentifier)[0];
                                String targetType =
                                    tfMap.get(targetIdentifier)[1];
                                String targetGenePid =
                                    createGene(null, targetIdentifier);

                                Item targetNetworkPropertyVpos = addNetworkProperty(TOPOS_VPOS,
                                        targetVpos, targetGenePid);
                                Item targetNetworkPropertyType = addNetworkProperty(TOPOS_TYPE,
                                        targetType, targetGenePid);

                                regulation.setReference("source", geneItems.get(sourceGenePid));
                                regulation.setReference("target", geneItems.get(targetGenePid));
                                store(regulation);
                            } else if (miRNAMap.containsKey(targetIdentifier)) {
                                // Create regulation for both genes
                                Item regulation = createRegulation(INTERACTION_TYPE_MIRNA_MIRNA);

                                // Create target gene
                                String targetVpos =
                                    miRNAMap.get(targetIdentifier)[0];
                                String targetHpos =
                                    miRNAMap.get(targetIdentifier)[1];
                                String targetGenePid = createGene(null, targetIdentifier);

                                // Create networkProperty for target gene
                                Item targetNetworkPropertyVpos = addNetworkProperty(TOPOS_VPOS,
                                        targetVpos, targetGenePid);
                                Item targetNetworkPropertyHpos = addNetworkProperty(TOPOS_HPOS,
                                        targetHpos, targetGenePid);

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
     * @param type    the type of property
     * @param value   the value of property
     * @param genePid the gene primaryId (if resolved) or symbol (otherwise)
     * @return
     * @throws ObjectStoreException
     */
    private Item addNetworkProperty(String type, String value, String genePid)
            throws ObjectStoreException {
        // Create networkProperties for source gene
        Item sourceNetworkProperty =
            createNetworkProperty(type, value);
        sourceNetworkProperty.setReference("node",
                geneItems.get(genePid));
        store(sourceNetworkProperty);

        return sourceNetworkProperty;
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

        if (primaryId == null) {
            // a few symbols are secondary identifiers for WB, let's use the primary one
            // TODO improve resolver
            if ("mir-239a".equalsIgnoreCase(symbol)) {
                symbol = "mir-239.1";
            }
            if ("mir-239b".equalsIgnoreCase(symbol)) {
                symbol = "mir-239.2";
            }
            if ("mir-48".equalsIgnoreCase(symbol)) {
                symbol = "mir-58";
            }
            if ("lin-15b".equalsIgnoreCase(symbol)) {
                symbol = "lin-15B";
            }


            IdResolver resolver = resolverFactory.getIdResolver();
            int resCount = resolver.countResolutions(WORM_TAXON_ID, symbol);
            if (resCount == 0) {
                LOG.warn("RESOLVER: failed to find existing gene for symbol: " + symbol);
                if (!geneItems.containsKey(symbol)) {
                    createBioEntity(symbol);
                }
                return symbol;
            }
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                         + symbol + " count: " + resCount + " WBgn: "
                         + resolver.resolveId(WORM_TAXON_ID, symbol));
            }
            primaryId = resolver.resolveId(WORM_TAXON_ID, symbol).iterator().next();
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

    /**
     * Create and store a BioEntity item on the first time called.
     *
     * @param type gene
     * @param primaryId the gene primaryIdentifier
     * @param symbol the gene symbol
     * @throws ObjectStoreException
     */
    private void createBioEntity(String symbol)
        throws ObjectStoreException {
        Item bioentity = null;

        bioentity = createItem("Gene");
        bioentity.setReference("organism", orgRefId);
        bioentity.setAttribute("symbol", symbol);
        store(bioentity);

        geneItems.put(symbol, bioentity.getIdentifier());
    }



}

