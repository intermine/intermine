package org.intermine.bio.dataconversion;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
    
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.intermine.bio.util.BioConverterUtil;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * Loads GFA data from a gfa.tsv file.
 *
 * gene                                    family                  protein                                   e-value   score  best-domain-score
 * 0                                       1                       2                                         3         4      5
 * vigun.CB5-2.gnm1.ann1.VuCB5-2.03G238200 legfed_v1_0.L_JSS52Z    vigun.CB5-2.gnm1.ann1.VuCB5-2.03G238200.1 5.3e-199  660.9  660.8
 *
 * @author Sam Hokin
 */
public class GFAConverter extends BioFileConverter {
	
    private static final Logger LOG = Logger.getLogger(GFAConverter.class);

    // It's too much of a pain in the ass to get project.xml variables loaded in here since BioFileConverter is designed to use static values.
    // We'll use the filename as the dataset name.
    static final String DATA_SOURCE_NAME = "Legume Information System";
    static final String DATA_SET_DESCRIPTION = "LIS gene family assignments (GFA) file";
    static final String LICENCE = "public";

    // things to store
    Item geneSOTerm;
    List<Item> geneFamilyAssignments = new ArrayList<>();
    Map<String,Item> geneFamilies = new HashMap<>();
    Map<String,Item> genes = new HashMap<>();
    Map<String,Item> proteins = new HashMap<>();
    Map<String,Item> cdses = new HashMap<>();

    // store these so we can set the BioStoreHook during file processing.
    ItemWriter writer;
    Model model;
    
    /**
     * Create a new GFAConverter
     * @param writer the ItemWriter to write out new items
     * @param model the data model
     */
    public GFAConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);
        this.writer = writer;
        this.model = model;
        geneSOTerm = createItem("SOTerm");
        geneSOTerm.setAttribute("name", "gene");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws IOException {
        if (getCurrentFile().getName().endsWith(".gfa.tsv")) {
            System.out.println("## Processing "+getCurrentFile().getName());
            // Note: these are RefIDs!
            String dataSource = getDataSource(DATA_SOURCE_NAME);
            String dataSet = getDataSet(getCurrentFile().getName(), dataSource, DATA_SET_DESCRIPTION, LICENCE);
            // String sequenceOntologyRefId = BioConverterUtil.getOntology(this);
            setStoreHook(new BioStoreHook(model, dataSet, dataSource, null));
            // now process this GFA file
            processGFAFile();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ObjectStoreException, RuntimeException {
        store(geneSOTerm);
        store(genes.values());
        store(proteins.values());
        store(geneFamilies.values());
        store(geneFamilyAssignments);
    }

    /**
     * Process a gfa.tsv file which contains relationships between gene families, genes and proteins, along with an e-value, score, and best-domain score.
     */
    void processGFAFile() throws IOException, RuntimeException {
        // spin through the file
        BufferedReader br = new BufferedReader(new FileReader(getCurrentFile()));
        String line = null;
        while ((line=br.readLine())!=null) {
            if (line.startsWith("#")) continue;
            String[] fields = line.split("\t");
            if (fields.length==2) continue; // probably a second header with some item of non-interest
            String geneIdentifier = fields[0];
            String geneFamilyIdentifier = fields[1];
            String proteinIdentifier = fields[2];
            // data columns are optional
            double evalue = 0.0;
            double score = 0.0;
            double bestDomainScore = 0.0;
            if (fields.length>3) evalue = Double.parseDouble(fields[3]);
            if (fields.length>4) score = Double.parseDouble(fields[4]);
            if (fields.length>5) bestDomainScore = Double.parseDouble(fields[5]);
            // Gene Family
            Item geneFamily = getGeneFamily(geneFamilyIdentifier);
            // Gene
            Item gene = getGene(geneIdentifier);
            // Protein - this is just the primary transcript; there are other proteins associated with this gene that don't get the geneFamily reference
            Item protein = getProtein(proteinIdentifier);
            // GeneFamily collections
            geneFamily.addToCollection("genes", gene);
            geneFamily.addToCollection("proteins", protein);
            // scores go into GeneFamilyAssignment along with gene and protein
            createGeneFamilyAssignment(geneFamily, gene, protein, evalue, score, bestDomainScore);
        }
    }

    /**
     * Get/add a Gene Item, keyed by primaryIdentifier, which is the only attribute we use here.
     */
    Item getGene(String primaryIdentifier) {
        if (genes.containsKey(primaryIdentifier)) {
            return genes.get(primaryIdentifier);
        } else {
            Item gene = createItem("Gene");
            gene.setAttribute("primaryIdentifier", primaryIdentifier);
            gene.setReference("sequenceOntologyTerm", geneSOTerm);
            genes.put(primaryIdentifier, gene);
            return gene;
        }
    }

    /**
     * Get/add a Protein Item, keyed by primaryIdentifier, which is the only attribute we use here.
     */
    Item getProtein(String primaryIdentifier) {
        if (proteins.containsKey(primaryIdentifier)) {
            return proteins.get(primaryIdentifier);
        } else {
            Item protein = createItem("Protein");
            protein.setAttribute("primaryIdentifier", primaryIdentifier);
            proteins.put(primaryIdentifier, protein);
            return protein;
        }
    }

    /**
     * Get/add a GeneFamily, keyed by primaryIdentifier.
     */
    Item getGeneFamily(String primaryIdentifier) {
        if (geneFamilies.containsKey(primaryIdentifier)) {
            return geneFamilies.get(primaryIdentifier);
        } else {
            Item geneFamily = createItem("GeneFamily");
            geneFamily.setAttribute("primaryIdentifier", primaryIdentifier);
            geneFamilies.put(primaryIdentifier, geneFamily);
            return geneFamily;
        }
    }

    /**
     * Create a GeneFamilyAssignment and add it to the List.
     */
    void createGeneFamilyAssignment(Item geneFamily, Item gene, Item protein, double evalue, double score, double bestDomainScore) {
            Item gfa = createItem("GeneFamilyAssignment");
            geneFamilyAssignments.add(gfa);
            if (evalue>0.0) gfa.setAttribute("evalue", String.valueOf(evalue));
            if (score>0.0) gfa.setAttribute("score", String.valueOf(score));
            if (bestDomainScore>0.0) gfa.setAttribute("bestDomainScore", String.valueOf(bestDomainScore));
            gfa.setReference("geneFamily", geneFamily);
            gfa.setReference("gene", gene);
            gfa.setReference("protein", protein);
            gene.addToCollection("geneFamilyAssignments", gfa);
            protein.addToCollection("geneFamilyAssignments", gfa);
    }

}
