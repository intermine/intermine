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
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * Read Alleles and Genotypes with associated Mammalian Phenotype ontology terms from MGI files.
 * @author Richard Smith
 */
public class MgiAllelesConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "MGI Alleles";
    private static final String DATA_SOURCE_NAME = "MGI Mouse Genome Database";
    private static final String MOUSE_TAXON = "10090";

    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> terms = new HashMap<String, String>();
    private Map<String, Item> alleles = new HashMap<String, Item>();

    private Item ontology;
    private String organismIdentifier;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public MgiAllelesConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);

    }


    @Override
    public void close() throws Exception {
        for (Item allele : alleles.values()) {
            store(allele);
        }
        super.close();
    }


    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        organismIdentifier = getOrganism(MOUSE_TAXON);
        String currentFile = getCurrentFile().getName();
        if ("MGI_PhenotypicAllele.rpt".equals(currentFile)
                || "MGI_QTLAllele.rpt".equals(currentFile)) {
            processPhenotypicAlleles(reader);
        } else if ("MGI_PhenoGenoMP.rpt".equals(currentFile)) {
            processGenotypes(reader);
        } else {
            System.out .println("Ignoring file: " + currentFile);
        }
    }
    private void processGenotypes(Reader reader) throws ObjectStoreException, IOException {
        if (ontology == null) {
            ontology = createItem("Ontology");
            ontology.setAttribute("name", "Mammalian Phenotype Ontology");
            store(ontology);
        }

        String lastGenotypeName = null;
        Item currentGenotype = null;
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String genotypeName = line[0];
            String alleleStr = line[1];
            String background = line[2];
            String termId = line[3];
            String geneStr = line[4];

            if (!genotypeName.equals(lastGenotypeName)) {
                // store
                if (currentGenotype != null) {
                    store(currentGenotype);
                }

                String[] alleleSymbols = alleleStr.split("\\|");

                currentGenotype = createItem("Genotype");
                currentGenotype.setAttribute("name", genotypeName);
                currentGenotype.setAttribute("geneticBackground", background);
                currentGenotype.setReference("organism", organismIdentifier);

                String zygosity = determineZygosity(alleleSymbols, geneStr);
                if (zygosity != null) {
                    currentGenotype.setAttribute("zygosity", zygosity);
                }
                for (String alleleSymbol : alleleSymbols) {
                    if (!isWildTypeSymbol(alleleSymbol)) {
                        Item allele = getAlleleItem(alleleSymbol);
                        currentGenotype.addToCollection("alleles", allele);
                    }
                }
            }
            currentGenotype.addToCollection("phenotypeTerms", getTermItemId(termId));
            lastGenotypeName = genotypeName;
        }
        if (currentGenotype != null) {
            store(currentGenotype);
        }
    }

    private boolean isWildTypeSymbol(String symbol) {
        return symbol.contains("<+>");
    }

    private String determineZygosity(String[] alleleSymbols, String geneStr) {
        if (alleleSymbols.length == 1) {
            return "homozygote";
        } else if (!StringUtils.isBlank(geneStr) && geneStr.contains(",")) {
            return "complex: > 1 genome feature";
        } else if (alleleSymbols.length == 2 && !(alleleSymbols[0].equals(alleleSymbols[1]))) {
            return "heterozygote";
        }

        return null;
    }

    private void processPhenotypicAlleles(Reader reader) throws ObjectStoreException, IOException {
        if (ontology == null) {
            ontology = createItem("Ontology");
            ontology.setAttribute("name", "Mammalian Phenotype Ontology");
            store(ontology);
        }

        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String alleleIdentifier = line[0];
            String alleleSymbol = line[1];
            String alleleName = line[2];
            String alleleType = line[3];

            String pubmed = line[4];
            String geneIdentifier = line[5];

            String termsStr = null;
            if (line.length >= 10) {
                termsStr = line[9];
            }

            // TODO synonyms for alleles?

            Item allele = getAlleleItem(alleleSymbol);
            allele.setAttribute("primaryIdentifier", alleleIdentifier);
            allele.setAttribute("name", alleleName);
            allele.setAttribute("type", alleleType);

            if (!StringUtils.isBlank(pubmed)) {
                String pubItemId = getPubItemId(pubmed);
                allele.setReference("publication", pubItemId);
            }

            if (!StringUtils.isBlank(geneIdentifier)) {
                String geneItemId = getGeneItemId(geneIdentifier);
                allele.setReference("gene", geneItemId);
            }

            if (!StringUtils.isBlank(termsStr)) {
                String[] termIds = termsStr.split(",");
                for (String termId : termIds) {
                    allele.addToCollection("highLevelPhenotypeTerms", getTermItemId(termId));
                }
            }
        }
    }

    private Item getAlleleItem(String alleleSymbol) throws ObjectStoreException {
        Item allele = alleles.get(alleleSymbol);
        if (allele == null) {
            allele = createItem("Allele");
            allele.setAttribute("symbol", alleleSymbol);
            allele.setReference("organism", organismIdentifier);
            alleles.put(alleleSymbol, allele);
        }
        return allele;
    }

    private String getPubItemId(String pubmed) throws ObjectStoreException {
        String pubItemId = pubs.get(pubmed);
        if (pubItemId == null) {
            Item pub = createItem("Publication");
            pub.setAttribute("pubMedId", pubmed);
            store(pub);
            pubItemId = pub.getIdentifier();
            pubs.put(pubmed, pubItemId);
        }
        return pubItemId;
    }

    private String getGeneItemId(String geneIdentifier) throws ObjectStoreException {
        String geneItemId = genes.get(geneIdentifier);
        if (geneItemId == null) {
            Item gene = createItem("Gene");
            gene.setAttribute("secondaryIdentifier", geneIdentifier);
            gene.setReference("organism", organismIdentifier);
            store(gene);
            geneItemId = gene.getIdentifier();
            genes.put(geneIdentifier, geneItemId);
        }
        return geneItemId;
    }

    private String getTermItemId(String termIdentifier) throws ObjectStoreException {
        String termItemId = terms.get(termIdentifier);
        if (termItemId == null) {
            Item term = createItem("MammalianPhenotypeTerm");
            term.setAttribute("identifier", termIdentifier);
            term.setReference("ontology", ontology);
            store(term);
            termItemId = term.getIdentifier();
            terms.put(termIdentifier, termItemId);
        }
        return termItemId;
    }
}
