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

import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;

/**
 * Read from "EcoData-identifiers.txt" generated from EcoGene to create
 * canonical E. coli genes with correct identifiers.
 *
 * @author Fengyuan Hu
 */
public class EcogeneIdentifiersConverter extends BioFileConverter {
    protected static final Logger LOG = Logger
            .getLogger(EcogeneIdentifiersConverter.class);

    private static final String DATASET_TITLE = "EcoGene gene identifiers";
    private static final String DATA_SOURCE_NAME = "EcoGene";
    private static final String ECOLI_TAXON = "83333";
    private static final String CHROMOSOME_PID = "U00096.2";
    private static final String HEADER_LINE = "EG";
    private static final String NULL_STRING = "Null";
    private static final String NONE_STRING = "None";
    private static final String TYPE_GENE = "aa";
    private static final String TYPE_RNA = "nt";
    private static final String CLOCKWISE = "Clockwise";
    private static final String COUNTER_CLOCKWISE = "Counterclockwise";
    private static final String DIGIT_REGEX = "^\\d+$";

    private Map<String, Item> proteinMap = new HashMap<String, Item>();

    /**
     * Constructor
     *
     * @param writer
     *            the ItemWriter used to handle the resultant items
     * @param model
     *            the Model
     */
    public EcogeneIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        // Create a chromosome
        Item chromosome = createItem("Chromosome");
        chromosome.setAttribute("primaryIdentifier", CHROMOSOME_PID);
        store(chromosome);

        @SuppressWarnings("rawtypes")
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            // remove header line
            if (!line[0].equals(HEADER_LINE)) {
                String ecogeneId = line[0];
                String geneName = line[1];
                String eCK = line[2];
                String swissProtId = line[3];
                String wisconsinGenBankId = line[4];
                String genBankProteinId = line[5];
                String genoBaseId = line[6];
                String type = line[7];
                String strand = line[8];
                String start = line[9];
                String end = line[10];
                String synonym = line[11];

                Set<String> symSet = new TreeSet<String>();

                if (!eCK.equals(NULL_STRING)) {
                    symSet.add(eCK);
                }

                if (!genoBaseId.equals(NULL_STRING)) {
                    symSet.addAll(Arrays.asList(StringUtil.split(genoBaseId,
                            "; ")));
                }

                if (!synonym.equals(NONE_STRING)) {
                    symSet.addAll(Arrays.asList(synonym.split(", ")));
                }

                if (type.equals(TYPE_GENE)) {

                    Item gene = createItem("Gene");
                    gene.setReference("chromosome", chromosome);
                    gene.setReference("organism", getOrganism(ECOLI_TAXON));
                    gene.setAttribute("primaryIdentifier", ecogeneId);
                    gene.setAttribute("secondaryIdentifier", wisconsinGenBankId);
                    gene.setAttribute("name", geneName);
                    gene.setAttribute("symbol", geneName);



                    if (symSet.size() > 0) {
                        for (String sym : symSet) {
                            createSynonym(gene, sym, true);
                        }
                    }

                    if (!swissProtId.equals(NULL_STRING)) {

                        if (proteinMap.containsKey(swissProtId)) {
                            // Reference a protein to a gene (a gene has proteins
                            // collection)
                            gene.addToCollection("proteins",
                                    proteinMap.get(swissProtId));
                        } else {
                            Item protein = createItem("Protein");
                            protein.setAttribute("primaryAccession", swissProtId);
                            // NCBI Protein id, remove "g"
                            protein.setAttribute("secondaryIdentifier",
                                    genBankProteinId.substring(1));
                            gene.addToCollection("proteins", protein);
                            store(protein);
                            proteinMap.put(swissProtId, protein);
                        }
                    }

                    // Create chromosome location
                    if (start.matches(DIGIT_REGEX) && end.matches(DIGIT_REGEX)) {

                        Item location = createItem("Location");
                        location.setAttribute("start", start);
                        location.setAttribute("end", end);
                        location.setReference("feature", gene);
                        location.setReference("locatedOn", chromosome);

                        if (strand.equals(CLOCKWISE)) {
                            location.setAttribute("strand", "+1");
                        } else if (strand.equals(COUNTER_CLOCKWISE)) {
                            location.setAttribute("strand", "-1");
                        } else {
                            location.setAttribute("strand", "0");
                        }

                        gene.setReference("chromosomeLocation", location);

                        store(location);
                    }

                    store(gene);

                } else if (type.equals(TYPE_RNA)) { // TODO code refactory

                    Item rna = createItem("NcRNA");
                    rna.setReference("chromosome", chromosome);
                    rna.setReference("organism", getOrganism(ECOLI_TAXON));
                    rna.setAttribute("primaryIdentifier", ecogeneId);
                    rna.setAttribute("secondaryIdentifier", wisconsinGenBankId);
                    rna.setAttribute("name", geneName);
                    rna.setAttribute("symbol", geneName);

                    if (symSet.size() > 0) {
                        for (String sym : symSet) {
                            createSynonym(rna, sym, true);
                        }
                    }

                    // Create chromosome location
                    if (start.matches(DIGIT_REGEX) && end.matches(DIGIT_REGEX)) {

                        Item location = createItem("Location");
                        location.setAttribute("start", start);
                        location.setAttribute("end", end);
                        location.setReference("feature", rna);
                        location.setReference("locatedOn", chromosome);

                        if (strand.equals(CLOCKWISE)) {
                            location.setAttribute("strand", "+1");
                        } else if (strand.equals(COUNTER_CLOCKWISE)) {
                            location.setAttribute("strand", "-1");
                        } else {
                            location.setAttribute("strand", "0");
                        }

                        rna.setReference("chromosomeLocation", location);

                        store(location);
                    }

                    store(rna);

                }
            }
        }
    }
}
