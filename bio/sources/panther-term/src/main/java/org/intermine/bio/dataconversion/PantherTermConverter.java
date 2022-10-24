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
 * Loads Panther terms from a PANTHERnn.n_HMM_classifications file into OntologyTerm.
 * http://data.pantherdb.org/ftp/hmm_classifications/current_release/
 * Stores cross references to GO terms and Panther classes (PC).
 *
 * @author Sam Hokin
 */
public class PantherTermConverter extends BioFileConverter {
    
    private static final Logger LOG = Logger.getLogger(PantherTermConverter.class);

    static final String DATASOURCE_NAME = "PANTHER";
    static final String DATASOURCE_URL = "https://www.pantherdb.org/";
    static final String DATASOURCE_DESCRIPTION = "The mission of the PANTHER knowledgebase is to support biomedical " +
        "and other research by providing comprehensive information about the evolution of protein-coding gene families, " +
        "particularly protein phylogeny, function and genetic variation impacting that function.";

    static final String DATASET_URL = "http://data.pantherdb.org/ftp/hmm_classifications/current_release/";
    static final String DATASET_DESCRIPTION = "PANTHER HMM Classification file. Contains the PANTHER family/subfamily name, " +
        "and the molecular function, biological process, and pathway classifications for every PANTHER protein family " +
        "and subfamily in the PANTHER HMM library.";

    Item dataSource;
    Item dataSet;
    Item pantherOntology;
    Map<String,Item> pantherTerms = new HashMap<>();
    Map<String,Item> goTerms = new HashMap<>();
    Map<String,Item>  pcTerms = new HashMap<>();

    String dataSetName; // equals first file processed

    /**
     * Create a new PantherTermConverter
     * @param writer the ItemWriter to write out new items
     * @param model the data model
     */
    public PantherTermConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);
        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", DATASOURCE_NAME);
        dataSource.setAttribute("url", DATASOURCE_URL);
        dataSource.setAttribute("description", DATASOURCE_DESCRIPTION);
        dataSet = createItem("DataSet");
        dataSet.setAttribute("url", DATASET_URL);
        dataSet.setAttribute("description", DATASET_DESCRIPTION);
        dataSet.setReference("dataSource", dataSource);
        pantherOntology = createItem("Ontology");
        pantherOntology.setAttribute("name", DATASOURCE_NAME);
        pantherOntology.setAttribute("url", DATASOURCE_URL);
        pantherOntology.addToCollection("dataSets", dataSet);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws IOException {
        if (getCurrentFile().getName().startsWith("PANTHER") && getCurrentFile().getName().endsWith("classifications")) {
            System.out.println("## Processing "+getCurrentFile().getName());
            if (dataSetName==null) {
                // PANTHER17.0_HMM_classifications
                dataSetName = getCurrentFile().getName();
                dataSet.setAttribute("name", dataSetName);
                String[] parts = getCurrentFile().getName().split("_");
                String version = parts[0].substring(7);
                dataSet.setAttribute("version", version);
            }
            processPantherFile(reader);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ObjectStoreException, RuntimeException {
        store(dataSource);
        store(dataSet);
        store(pantherOntology);
        store(pantherTerms.values());
        store(pcTerms.values());
        store(goTerms.values());
    }

    /**
     * Process a PANTHER classifications file.
     * 0          1     2   3   4   5
     * identifier name  GO  GO  GO  PC
     *
     * GO terms are stored as crossReferences.
     * NOTE: we prepend PANTHER: to the PTHR and PC identifiers.
     */
    void processPantherFile(Reader reader) throws IOException {
        // spin through the file
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        while ((line=br.readLine())!=null) {
            String[] fields = line.split("\t");
            if (fields.length<2) continue;
            String identifier = "PANTHER:" + fields[0];
            String name = fields[1];
            if (name.length()==0) continue;
            if (pantherTerms.containsKey(identifier)) continue;
            Item pantherTerm = createItem("OntologyTerm");
            pantherTerm.setReference("ontology", pantherOntology);
            pantherTerm.setAttribute("identifier", identifier);
            pantherTerm.setAttribute("name", name);
            pantherTerms.put(identifier, pantherTerm);
            // parse GO and PC crossReferences
            for (int i=2; i<fields.length; i++) {
                // cis-regulatory region sequence-specific DNA binding#GO:0000987;transcription regulatory region nucleic acid binding#GO:0001067;...
                // homeodomain transcription factor#PC00119;DNA-binding transcription factor#PC00218;helix-turn-helix transcription factor#PC00116
                String[] terms = fields[i].split(";");
                for (String term : terms) {
                    // transcription regulatory region nucleic acid binding#GO:0001067
                    // helix-turn-helix transcription factor#PC00116
                    String[] parts = term.split("#");
                    if (parts.length==2) {
                        String crName = parts[0];
                        String crIdentifier = parts[1];
                        if (i<5) {
                            pantherTerm.addToCollection("crossReferences", getGOTerm(crIdentifier));
                        } else {
                            pantherTerm.addToCollection("crossReferences", getPCTerm("PANTHER:"+crIdentifier, crName, pantherOntology));
                        }
                    }
                }
            }
        }
    }

    /**
     * Create or retrieve a GO term.
     */
    Item getGOTerm(String identifier) {
        if (goTerms.containsKey(identifier)) {
            return goTerms.get(identifier);
        } else {
            Item goTerm = createItem("GOTerm");
            goTerm.setAttribute("identifier", identifier);
            goTerms.put(identifier, goTerm);
            return goTerm;
        }
    }       

    /**
     * Create or retrieve a PC term.
     */
    Item getPCTerm(String identifier, String name, Item pantherOntology) {
        if (pcTerms.containsKey(identifier)) {
            return pcTerms.get(identifier);
        } else {
            Item pcTerm = createItem("OntologyTerm");
            pcTerm.setAttribute("identifier", identifier);
            pcTerm.setAttribute("name", name);
            pcTerm.setReference("ontology", pantherOntology);
            pcTerms.put(identifier, pcTerm);
            return pcTerm;
        }
    }       

}
