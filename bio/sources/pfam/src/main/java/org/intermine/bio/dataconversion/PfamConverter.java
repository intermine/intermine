package org.intermine.bio.dataconversion;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * Loads Pfam data from a pfamA.txt file into OntologyTerm.
 * https://www.ebi.ac.uk/interpro/entry/pfam/
 *
 * @author Sam Hokin
 */
public class PfamConverter extends BioFileConverter {
    
    private static final Logger LOG = Logger.getLogger(PfamConverter.class);

    static final String DATASOURCE_NAME = "EMBL-EBI";
    static final String DATASOURCE_URL = "https://www.ebi.ac.uk/";
    static final String DATASOURCE_DESCRIPTION = "EMBL-EBI is international, innovative and interdisciplinary, and a champion of open data in the life sciences. We are part of the European Molecular Biology Laboratory (EMBL), an intergovernmental research organisation funded by over 20 member states, prospect and associate member states.";

    static final String DATASET_NAME = "Pfam";
    static final String DATASET_URL = "https://ftp.ebi.ac.uk/pub/databases/Pfam/current_release/database_files/";
    static final String DATASET_DESCRIPTION = "Pfam is a large collection of multiple sequence alignments and hidden Markov models covering many common protein domains.";

    Item dataSource;
    Item dataSet;
    Item ontology;
    Map<String,Item> ontologyTerms = new HashMap<>();

    /**
     * Create a new PfamConverter
     * @param writer the ItemWriter to write out new items
     * @param model the data model
     */
    public PfamConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);
        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", DATASOURCE_NAME);
        dataSource.setAttribute("url", DATASOURCE_URL);
        dataSource.setAttribute("description", DATASOURCE_DESCRIPTION);
        dataSet = createItem("DataSet");
        dataSet.setAttribute("name", DATASET_NAME);
        dataSet.setAttribute("url", DATASET_URL);
        dataSet.setAttribute("description", DATASET_DESCRIPTION);
        dataSet.setReference("dataSource", dataSource);
        ontology = createItem("Ontology");
        ontology.setAttribute("name", DATASOURCE_NAME);
        ontology.setAttribute("url", DATASOURCE_URL);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws IOException {
        if (getCurrentFile().getName().equals("pfamA.txt")) {
            System.out.println("## Processing "+getCurrentFile().getName());
            processPfamFile(reader);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ObjectStoreException, RuntimeException {
        store(dataSource);
        store(dataSet);
        store(ontology);
        store(ontologyTerms.values());
    }

    /**
     * Process a pfamA.txt file.
     * 0          1         2            3            4             5            6          7           8...
     * pfamA_acc  pfamA_id  previous_id  description  deposited_by  seed_source  type       comment     ....
     * OntologyTerm.
     * identifier .         .            name         .             .            namespace  description ....
     *
     * NOTE: LIS prepends Pfam: to the accession/identifier!
     */
    void processPfamFile(Reader reader) throws IOException {
        // spin through the file
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        while ((line=br.readLine())!=null) {
            String[] fields = line.split("\t");
            String identifier = "Pfam:" + fields[0];
            String name = fields[3];
            String namespace = fields[6];
            String description = fields[7];
            Item ontologyTerm = createItem("OntologyTerm");
            ontologyTerm.setReference("ontology", ontology);
            ontologyTerm.setAttribute("identifier", identifier);
            ontologyTerm.setAttribute("name", name);
            ontologyTerm.setAttribute("namespace", namespace);
            ontologyTerm.setAttribute("description", description);
            ontologyTerms.put(identifier, ontologyTerm);
        }
    }

}
