package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;


/**
 *
 * @author
 */
public class NcbiGeneConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "NCBI Entrez Gene identifiers";
    private static final String DATA_SOURCE_NAME = "NCBI Entrez Gene";
    private Set<String> taxonIds = null;

    protected static final Logger LOG = Logger.getLogger(NcbiGeneConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public NcbiGeneConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Set the organisms to include by a space separated list of taxon ids.
     * @param taxonIds the organisms to include
     */
    public void setOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
    }

    /**
     * Read the NCBI gene_info file and create genes setting identifiers, organism and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        if (this.taxonIds == null) {
            throw new IllegalArgumentException("No organisms passed to NcbiGeneConverter.");
        }

        NcbiGeneInfoParser parser = new NcbiGeneInfoParser(reader);
        LOG.info("DUPLICATE symbols: " + parser.findDuplicateSymbols("9606"));
        Map<String, Set<GeneInfoRecord>> records = parser.getGeneInfoRecords();
        for (String taxonId : records.keySet()) {
            if (!taxonIds.contains(taxonId)) {
                continue;
            }
            for (GeneInfoRecord record : records.get(taxonId)) {
                // gene type - http://www.ncbi.nlm.nih.gov/IEB/ToolBox/CPP_DOC/lxr/source/src/objects/entrezgene/entrezgene.asn
                if (record.geneType == null) {
                    continue;
                } else if ("ncRNA".equals(record.geneType)) {

                    Item ncRNA = createItem("NcRNA");
                    ncRNA.setReference("organism", getOrganism(taxonId));

                    ncRNA.setAttribute("ncbiGeneNumber", record.entrez);
                    ncRNA.setAttribute("secondaryIdentifier", record.entrez);

                    // SYMBOL
                    if (record.officialSymbol != null) {
                        ncRNA.setAttribute("symbol", record.officialSymbol);
                        // if NCBI symbol is different add it as a synonym
                        if (record.defaultSymbol != null &&
                                !record.officialSymbol.equals(record.defaultSymbol)) {
                            createSynonym(ncRNA, record.defaultSymbol, true);
                            LOG.info("NcRNA official symbol " + record.officialSymbol
                                    + " does not match " + record.defaultSymbol);
                        }
                    } else {
                        if (parser.isUniqueSymbol(taxonId, record.defaultSymbol)) {
                            ncRNA.setAttribute("symbol", record.defaultSymbol);
                        } else {
                            createSynonym(ncRNA, record.defaultSymbol, true);
                        }
                    }
                    if (StringUtils.isBlank(record.officialSymbol)) {
                        LOG.info("NcRNA has no official symbol: " + record.entrez + " "
                                + record.defaultSymbol);
                    }

                    // NAME
                    if (record.officialName != null) {
                        ncRNA.setAttribute("name", record.officialName);
                        if (record.defaultName != null &&
                                !record.officialName.equals(record.defaultName)) {
                            createSynonym(ncRNA, record.defaultName, true);
                        }
                    } else if (record.defaultName != null) {
                        ncRNA.setAttribute("name", record.defaultName);
                    }

                    // ENSEMBL ID become primaryIdentifier or CrossReference
                    // TODO this currently doesn't load any Ensembl ids.
                    boolean loadEnsembl = false;
                    if (loadEnsembl) {
                        if (record.ensemblIds.size() == 1) {
                            String ensemblId = record.ensemblIds.iterator().next();
                            if (parser.isUniquelyMappedEnsemblId(taxonId, ensemblId)) {
                                LOG.info("EnsemblId " + ensemblId
                                        + " is assigned to multiple genes, "
                                        + "observed for: " + record.entrez + ", "
                                        + ncRNA.getAttribute("symbol").getValue());
                                ncRNA.setAttribute("primaryIdentifier", ensemblId);
                            } else {
                                createCrossReference(ncRNA.getIdentifier(), ensemblId, "Ensembl",
                                        true);
                            }
                        } else {
                            // TODO this doesn't check for uniquely mapped ensembl ids, needs to log
                            if (record.ensemblIds.size() > 0) {
                                LOG.info("E2 Gene " + record.entrez + ", "
                                        + ncRNA.getAttribute("symbol").getValue()
                                        + " is assigned more than one Ensembl id: "
                                        + record.ensemblIds);
                            }
                            for (String ensemblId : record.ensemblIds) {
                                createCrossReference(ncRNA.getIdentifier(), ensemblId, "Ensembl", true);
                            }
                        }
                    }
                    store(ncRNA);
                } else if ("tRNA".equals(record.geneType)
                        || "protein-coding".equals(record.geneType)
                        || "miscRNA".equals(record.geneType)
                        || "rRNA".equals(record.geneType)) { // ecolimine case

                    Item gene = createItem("Gene");
                    gene.setReference("organism", getOrganism(taxonId));
                    createCrossReference(gene.getIdentifier(), record.entrez, "NCBI", true);
                    gene.setAttribute("primaryIdentifier", record.entrez);

                    if (record.officialSymbol != null) {
                        gene.setAttribute("symbol", record.officialSymbol);
                        // if NCBI symbol is different add it as a synonym
                        if (record.defaultSymbol != null &&
                                !record.officialSymbol.equals(record.defaultSymbol)) {
                            createSynonym(gene, record.defaultSymbol, true);
                            LOG.info("GENE official symbol " + record.officialSymbol
                                    + " does not match " + record.defaultSymbol);
                        }
                    } else {
                        if (parser.isUniqueSymbol(taxonId, record.defaultSymbol)) {
                            gene.setAttribute("symbol", record.defaultSymbol);
                        } else {
                            createSynonym(gene, record.defaultSymbol, true);
                        }
                    }
                    if (StringUtils.isBlank(record.officialSymbol)) {
                        LOG.info("GENE has no official symbol: " + record.entrez + " "
                                + record.defaultSymbol);
                    }

                    // NAME
                    if (record.officialName != null) {
                        gene.setAttribute("name", record.officialName);
                        if (record.defaultName != null &&
                                !record.officialName.equals(record.defaultName)) {
                            createSynonym(gene, record.defaultName, true);
                        }
                    } else if (record.defaultName != null) {
                        gene.setAttribute("name", record.defaultName);
                    }

                    boolean loadEnsembl = false;
                    if (loadEnsembl) {
                        if (record.ensemblIds != null) {
                            for (String ensemblId : record.ensemblIds) {
                                createCrossReference(gene.getIdentifier(), ensemblId, "Ensembl",
                                        true);
                            }
                        }
                    }

                    if (record.mapLocation != null) {
                        gene.setAttribute("mapLocation", record.mapLocation);
                    }
                    store(gene);
                }
            }
        }
    }
}