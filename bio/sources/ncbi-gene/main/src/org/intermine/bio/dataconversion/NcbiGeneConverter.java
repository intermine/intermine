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
import org.intermine.objectstore.ObjectStoreException;
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
                Item gene = createItem("Gene");
                gene.setReference("organism", getOrganism(taxonId));
                gene.setAttribute("ncbiGeneNumber", record.entrez);
                gene.setAttribute("secondaryIdentifier", record.entrez);

                // SYMBOL
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

                // ENSEMBL ID become primaryIdentifier or CrossReference
                // TODO this currently doesn't load any Ensembl ids.
                boolean loadEnsembl = false;
                if (loadEnsembl) {
                    if (record.ensemblIds.size() == 1) {
                        String ensemblId = record.ensemblIds.iterator().next();
                        if (parser.isUniquelyMappedEnsemblId(taxonId, ensemblId)) {
                            LOG.info("EnsemblId " + ensemblId + " is assigned to multiple genes, "
                                    + "observed for: " + record.entrez + ", "
                                    + gene.getAttribute("symbol").getValue());
                            gene.setAttribute("primaryIdentifier", ensemblId);
                        } else {
                            createCrossReference(gene, ensemblId, "Ensembl");
                        }
                    } else {
                        // TODO this doesn't check for uniquely mapped ensembl ids, needs to log
                        if (record.ensemblIds.size() > 0) {
                            LOG.info("E2 Gene " + record.entrez + ", "
                                    + gene.getAttribute("symbol").getValue()
                                    + " is assigned more than one Ensembl id: "
                                    + record.ensemblIds);
                        }
                        for (String ensemblId : record.ensemblIds) {
                            createCrossReference(gene, ensemblId, "Ensembl");
                        }
                    }
                }

                // SYNONYMS
                for (String synonym : record.synonyms) {
                    createSynonym(gene, synonym, true);
                }

                // MAP LOCATION
                if (record.mapLocation != null) {
                    gene.setAttribute("mapLocation", record.mapLocation);
                }
                store(gene);
            }
        }
    }

    private void createCrossReference(Item subject, String identifier, String dataSource)
    throws ObjectStoreException {
        Item crossRef = createItem("CrossReference");
        crossRef.setAttribute("identifier", identifier);
        crossRef.setReference("source", getDataSource(dataSource));
        crossRef.setReference("subject", subject);
        store(crossRef);
    }
}
