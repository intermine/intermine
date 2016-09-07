package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 *
 * @author Julie
 */
public class ClinvarConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(ClinvarConverter.class);
    private static final String DATASET_TITLE = "ClinVar data set";
    private static final String DATA_SOURCE_NAME = "ClinVar";
    private static final String ASSEMBLY = "GRCh38";
    private static final String TAXON_ID = "9606";
    protected Map<String, String> genes = new HashMap<String, String>();
    protected Map<String, String> diseases = new HashMap<String, String>();
    protected Set<String> alleles = new HashSet<String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ClinvarConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length < 26) {
                LOG.error("Allele not processed, only had " + line.length + " columns");
                continue;
            }

            String alleleId = line [0];

            if (alleleId.startsWith("#")) {
                // skip header
                continue;
            }

            String type = line[1];
            String geneId = line[3];
            String clinicalSignificance = line[5];
            String dbSNPXref = line[6];
            String ncbiXref = line[7];
            String clinVarXref = line[8];

            String diseaseString = line[10];    // parse for OMIM
            String assemblyString = line[12];

            // only load GRCh38
            if (!ASSEMBLY.equals(assemblyString)) {
                continue;
            }

            if (alleles.contains(alleleId)) {
                LOG.error("Duplicate alleles found for " + alleleId);
                continue;
            }
            alleles.add(alleleId);

            String referenceAllele = line[25];
            String alternateAllele = line[26];

            String geneRefId = getGene(geneId);

            Item item = createItem("Allele");
            item.setAttribute("primaryIdentifier", alleleId);
            item.setAttribute("type", type);
            item.setAttribute("clinicalSignificance", clinicalSignificance);
            item.setAttribute("reference", referenceAllele);
            item.setAttribute("alternate", alternateAllele);
            item.setReference("organism", getOrganism(TAXON_ID));
            item.setReference("gene", geneRefId);
            String diseaseRefId = getDisease(diseaseString);
            if (diseaseRefId != null) {
                item.addToCollection("diseases", diseaseRefId);
            }
            store(item);

            if (!"-".equals(dbSNPXref)) {
                createCrossReference(item.getIdentifier(), dbSNPXref, "dbSNP", true);
            }
            if (!"-".equals(ncbiXref)) {
                createCrossReference(item.getIdentifier(), ncbiXref, "NCBI", true);
            }
            if (!"-".equals(clinVarXref)) {
                createCrossReference(item.getIdentifier(), clinVarXref, "ClinVar", true);
            }
        }

    }

    private String getGene(String identifier) throws ObjectStoreException {
        String refId = genes.get(identifier);
        if (refId != null) {
            // we've already seen this gene
            return refId;
        }
        Item item = createItem("Gene");
        item.setAttribute("primaryIdentifier", identifier);
        genes.put(identifier, item.getIdentifier());
        store(item);
        return item.getIdentifier();
    }

    // MedGen:C3150901,OMIM:613647,ORPHA:306511
    private String getDisease(String diseaseString) throws ObjectStoreException {
        String[] identifiers = diseaseString.split(",");
        for (String identifier : identifiers) {
            if (identifier.startsWith("OMIM")) {
                String diseaseRefId = diseases.get(identifier);
                if (diseaseRefId != null) {
                    return diseaseRefId;
                }
                // had issues with the data file. "OMIM:^@" was a value.
                String[] bits = identifier.split(":");
                if (bits.length != 2 || !StringUtils.isNumeric(bits[1])) {
                    return null;
                }
                Item item = createItem("Disease");
                item.setAttribute("identifier", identifier);
                diseases.put(identifier, item.getIdentifier());
                store(item);
                return item.getIdentifier();
            }
        }
        return null;
    }
}
