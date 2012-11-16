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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.util.BioUtil;
import org.intermine.dataconversion.DataConverter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;


/**
 * CLASS NOT IN USE SINCE IM 1.1
 *
 * Processor of file with information about genes. Format of file:
 * <tt>
 * tax_id GeneID Symbol LocusTag Synonyms dbXrefs chromosome map_location description
 * type_of_gene Symbol_from_nomenclature_authority Full_name_from_nomenclature_authority
 * Nomenclature_status Other_designations Modification_date (tab is used as a separator,
 * pound sign - start of a comment)
 * </tt>
 * @author Jakub Kulaviak
 **/
public class GenesFileProcessor
{
    private BufferedReader infoReader;
    private int lineCounter = 0;
    private Map<String, Item> genes = new HashMap<String, Item>();
    private String lastLine = null;
    private DataConverter converter;
    private Set<String> genesToRemove = new TreeSet<String>();
    private IdResolver rslv;
    private static final Logger LOG = Logger.getLogger(GenesFileProcessor.class);
    private String datasetRefId;
    private static final String DUMMY = "NEWENTRY";
    private Set<Integer> alreadyProcessedGenes = new  HashSet<Integer>();

    /**
     * Constructor.
     * @param fileReader file reader, this class is not responsible for closing fileReader
     * @param converter associated converter that is used for creating and saving items
     * @param datasetRefId reference to dataset object for the gene
     * @param resolverFactory the FlyBase id resolver factory
     */
    public GenesFileProcessor(Reader fileReader, DataConverter converter, String datasetRefId,
                              IdResolver rslv) {
        // converter is needed  for creating items method
        // all converters must used one central converter for creating items because
        // to be sure, that created items will have unique id
        this.converter = converter;
        this.datasetRefId = datasetRefId;
        initReader(fileReader);
        this.rslv = rslv;

    }

    private void initReader(Reader fileReader) {
        infoReader = new BufferedReader(fileReader);
    }

    /**
     * @param geneToPub map between gene and list of publication that mentions this gene
     * @param orgToProcessId taxonID for organism of this gene
     * @param organismRefId ID representing the organism object
     * @param taxonIds list of taxons from config file, if empty all processed
     * @throws IOException when error happens during reading from file
     */
    public void processGenes(Map<Integer, List<String>> geneToPub, Integer orgToProcessId,
            String organismRefId, Set<String> taxonIds)
        throws IOException {
        String line;
        while ((line = getLine()) != null) {
            lineCounter++;
            line = line.trim();
            if (line.startsWith("#") || line.length() == 0) {
                continue;
            }
            String[] parts = line.split("\\t");
            if (parts.length < 6) {
                throw new GenesProcessorException("Invalid line - line " + lineCounter
                        + ". There are " + parts.length + " bits but there should be more than 6");
            }
            if (DUMMY.equals(parts[2])) {
                /* dummy lines in file,
                 4932 "Record to support submission of GeneRIFs for a gene not in Gene "
                (Candida robusta; Saccaromyces cerevisiae; Saccharomyces capensis; Saccharomyces
                italicus; Saccharomyces oviformis; Saccharomyces uvarum var. melibiosus;
                Saccharomyes cerevisiae; Sccharomyces cerevisiae; baker's yeast; brewer's yeast;
                lager beer yeast; yeast). other   -       -       -       -     */
                continue;
            }
            Integer organismId, ncbiGeneId;
            try {
                organismId = new Integer(parts[0].trim());
                ncbiGeneId = new Integer(parts[1].trim());
            } catch (NumberFormatException ex) {
                throw new GenesProcessorException("Invalid identifiers at line " + line);
            }

            if (!taxonIds.isEmpty() && !taxonIds.contains(organismId.toString())) {
                // this isn't an organism of interest, keep going
                continue;
            }

            //String identifier = parts[3].trim();
            String xrefs = parts[5].trim();
            if (orgToProcessId.intValue() == organismId.intValue()) {
                processGeneInfo(ncbiGeneId, organismId, xrefs, geneToPub.get(ncbiGeneId),
                        organismRefId);
                geneToPub.remove(ncbiGeneId);
            } else {
                // new organism found, we're done processing, store all
                lastLine = line;
                storeGenes();
                checkGenesProcessed(geneToPub);
                return;
            }
        }
        storeGenes();
        checkGenesProcessed(geneToPub);
    }

    private void checkGenesProcessed(Map<Integer, List<String>> geneToPub) {
        if (geneToPub.size() != 0) {
            throw new GenesProcessorException("These " + geneToPub.size() + " genes were in the "
                    + "PubMed2Gene file but not in the gene info file: "
                    + formatGeneNames(geneToPub.keySet()));
        }
    }

    private String formatGeneNames(Set<Integer> keySet) {
        StringBuilder sb = new StringBuilder();
        for (Integer id : keySet) {
            sb.append(id + ", ");
        }
        return sb.toString();
    }

    private String getLine() throws IOException {
        if (lastLine != null) {
            String tmp = lastLine;
            lastLine = null;
            return tmp;
        }
        return infoReader.readLine();
    }

    private void storeGenes()  {
        for (String id : genesToRemove) {
            genes.remove(id);
        }
        try {
            List<Item> gs = new ArrayList<Item>();
            for (String id : genes.keySet()) {
                gs.add(genes.get(id));
            }
            store(gs);
        } catch (ObjectStoreException e) {
            throw new GenesProcessorException(e);
        }
        genes = new HashMap<String, Item>();
    }

    private void store(List<Item> genes2) throws ObjectStoreException {
        converter.store(genes2);
    }

    private void processGeneInfo(Integer ncbiGeneId, Integer taxonId, String xrefs,
                                 List<String> publications, String organismRefId) {

        String primIdentifier = xrefs;
        Integer organismId = BioUtil.replaceStrain(taxonId);

        // If gene was already mentioned in gene info file then is skipped
        if (alreadyProcessedGenes.contains(ncbiGeneId)) {
            return;
        }

        // If there is a gene in gene information file that doesn't have
        // any publications then the gene is skipped
        // if there isn't primary identifier gene is skipped
        if (publications != null) {
            if (setPrimaryIdentifier(organismId.toString()) && !"-".equals(primIdentifier)) {
                primIdentifier = removeDatabasePrefix(primIdentifier);
                if (StringUtils.isEmpty(primIdentifier) || !isValidPrimIdentifier(primIdentifier)) {
                    return;
                }

                if (isDrosophilaMelanogaster(organismId.toString()) && rslv != null) {
                    primIdentifier = resolvePrimIdentifier(organismId.toString(), primIdentifier);
                }

                if (primIdentifier == null) {
                    LOG.warn("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                            + xrefs + ". Number of matched ids: "
                            + rslv.countResolutions(organismId.toString(), primIdentifier));
                    return;
                }
            } else {
                primIdentifier = null;
            }

            Item gene = createGene(ncbiGeneId, primIdentifier, organismRefId);
            for (String writerPubId : publications) {
                gene.addToCollection("publications", writerPubId);
            }
            // checks gene duplicates - if there are two or more same genes with
            // the same primIdentifier but different ncbi gene id then all these genes are removed
            if (primIdentifier != null) {
                if (genes.get(primIdentifier) == null) {
                    genes.put(primIdentifier, gene);
                } else {
                    genesToRemove.add(primIdentifier);
                }
            } else if (!setPrimaryIdentifier(organismId.toString())) {
                genes.put("" + ncbiGeneId, gene);
            }
            alreadyProcessedGenes.add(ncbiGeneId);
        }
    }

    // don't set primaryidentifier for mouse or people <- why???
    private boolean setPrimaryIdentifier(String taxonId) {
        if (isHomoSapiens(taxonId) || isMusMusculus(taxonId)) {
            return false;
        }
        return true;
    }

    private boolean isValidPrimIdentifier(String primIdentifier) {
        return !primIdentifier.contains("|");
    }

    private String resolvePrimIdentifier(String taxonId, String primIdentifier) {
        int resCount = rslv.countResolutions(taxonId, primIdentifier);
        if (resCount != 1) {
            return null;
        }
        return rslv.resolveId(taxonId, primIdentifier).iterator().next();
    }

    private boolean isDrosophilaMelanogaster(String taxonId) {
        return "7227".equals(taxonId);
    }

    private boolean isHomoSapiens(String taxonId) {
        return "9606".equals(taxonId);
    }

    private boolean isMusMusculus(String taxonId) {
        return "10090".equals(taxonId);
    }

    private Item createGene(Integer ncbiGeneId, String primaryIdentifier, String organismRefId) {
        Item gene = createItem("Gene");
        if (primaryIdentifier != null) {
            gene.setAttribute("primaryIdentifier", primaryIdentifier);
        }
        gene.setReference("organism", organismRefId);
        gene.setCollection("dataSets", new ArrayList<String>(Collections.singleton(datasetRefId)));
        return gene;
    }

    private String removeDatabasePrefix(String id) {
        String dbId = id;
        if (dbId.toUpperCase().startsWith("SGD:")) {
            dbId = dbId.substring(4);
        } else if (dbId.toUpperCase().startsWith("WORMBASE:")) {
            dbId = dbId.substring(9);
        } else if (dbId.toUpperCase().startsWith("FLYBASE:")) {
            dbId = dbId.substring(8);
        } else if (dbId.toUpperCase().startsWith("VECTORBASE:")) {
            dbId = dbId.substring(11);
        } else if (dbId.toUpperCase().startsWith("TAIR:")) {
            dbId = dbId.substring(5);
        } else if (dbId.toUpperCase().startsWith("MGI:")) {
            String[] bits = dbId.split(":");
            if (bits.length == 0) {
                LOG.warn("Not using mouse identifier in pubmed gene file:" + id);
                return null;
            }
            //MGI:895149|Ensembl:ENSMUSG0000001857
            for (String bit : bits) {
                if (bit.toUpperCase().startsWith("ENSMUSG")) {
                    return bit;
                }
            }
            LOG.warn("Not using mouse identifier in pubmed gene file:" + id);
            return null;
        }
        return dbId;
    }

    private Item createItem(String className) {
        return converter.createItem(className);
    }
}
