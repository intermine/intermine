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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * DataConverter to parse a HPO annotation file into Items.
 *
 * @author Fengyuan Hu
 */
public class HpoConverter extends BioDirectoryConverter
{

    private static final String DATASET_TITLE = "HPO Annotation";
    private static final String DATA_SOURCE_NAME = "HPO";

    private List<String> ignoreDbList = Arrays.asList("DECIPHER");

    private static final String HPOTEAM_FILE = "phenotype_annotation_hpoteam.tab";
    private static final String NEG_FILE = "negative_phenotype_annotation.tab";
    private static final String GENE_FILE =
            "ALL_SOURCES_ALL_FREQUENCIES_diseases_to_genes_to_phenotypes.txt";

    private Map<String, Item> diseases = new HashMap<String, Item>();
    private Map<String, Item> hpoTerms = new HashMap<String, Item>();
    private Map<String, Item> genes = new HashMap<String, Item>();

    private Map<String, String> evidenceCodes = new HashMap<String, String>();
    private Map<MultiKey, Item> annotations = new HashMap<MultiKey, Item>();
    private Map<String, String> publications = new HashMap<String, String>();
    private String ontologyItemId = null;

    private static final String HUMAN_TAXON = "9606";
    private String organism = getOrganism(HUMAN_TAXON);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws Exception if something goes wrong
     */
    public HpoConverter(ItemWriter writer, Model model) throws Exception {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    @Override
    public void process(File dataDir) throws Exception {
        Map<String, File> files = readFilesInDir(dataDir);

        String[] requiredFiles = new String[] {HPOTEAM_FILE, NEG_FILE};
        Set<String> missingFiles = new HashSet<String>();
        for (String requiredFile : requiredFiles) {
            if (!files.containsKey(requiredFile)) {
                missingFiles.add(requiredFile);
            }
        }

        if (!missingFiles.isEmpty()) {
            throw new RuntimeException("Not all required files for the OMIM sources were found in: "
                    + dataDir.getAbsolutePath() + ", was missing " + missingFiles);
        }

        ontologyItemId = storeOntology();
        processGeneFile(new FileReader(files.get(GENE_FILE)));
        processAnnotationFile(new FileReader(files.get(HPOTEAM_FILE)));
        processAnnotationFile(new FileReader(files.get(NEG_FILE)));
    }

    private static Map<String, File> readFilesInDir(File dir) {
        Map<String, File> files = new HashMap<String, File>();
        for (File file : dir.listFiles()) {
            files.put(file.getName(), file);
        }
        return files;
    }

    /**
     * @param reader file reader
     * @throws IOException if can't read file
     * @throws ObjectStoreException if can't store to db
     */
    protected void processGeneFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<?> lineIter = FormattedTextParser.
                parseTabDelimitedReader(new BufferedReader(reader));

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line[0].startsWith("#")) {
                continue;
            }

            String diseaseId = line[0];
            String identifier = line[2];
            String hpoId = line[3];

            Item disease = getDisease(diseaseId);
            Item gene = getGene(identifier);
            gene.addToCollection("diseases", disease);
            disease.addToCollection("genes", gene);
            Item term = getTerm(hpoId);
            term.addToCollection("diseases", disease);
        }
    }

    private Item getDisease(String omimId) {
        Item item = diseases.get(omimId);
        if (item == null) {
            item = createItem("Disease");
            item.setAttribute("identifier", omimId);
            diseases.put(omimId, item);
        }
        return item;
    }

    private Item getTerm(String hpoTerm) {
        Item item = hpoTerms.get(hpoTerm);
        if (item == null) {
            item = createItem("HPOTerm");
            item.setAttribute("identifier", hpoTerm);
            item.setReference("ontology", ontologyItemId);
            hpoTerms.put(hpoTerm, item);
        }
        return item;
    }

    private Item getGene(String identifier) throws ObjectStoreException {
        Item item = genes.get(identifier);
        if (item == null) {
            item = createItem("Gene");
            item.setAttribute("primaryIdentifier", identifier);
            item.setReference("organism", organism);
            genes.put(identifier, item);
        }
        return item;
    }

    /**
     * @param reader file reader
     * @throws IOException if can't read file
     * @throws ObjectStoreException if can't store to db
     */
    protected void processAnnotationFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] array = lineIter.next();
            // HPO Annotation File Format:
            // http://human-phenotype-ontology.github.io/documentation.html
            if (array.length < 9) {
                throw new IllegalArgumentException("Not enough elements (should be > 8 not "
                        + array.length + ")");
            }

            // e.g. OMIM
            String db = array[0];
            if (ignoreDbList.contains(db)) {
                continue;
            }

            // e.g. OMIM:100200
            String dbId = db + ":" + array[1];
            // e.g. NOT
            String qualifier = array[3];
            // HP:0000028
            String hpoIdentifier = array[4];
            //  PMID:17088400 OR OMIM:100050
            String dbRef = array[5];
            // e.g. IEA
            String evidenceCode = array[6];
            // e.g RARE
            String frequency = array[8];
            // HPO:curators
            String assignedBy = array[13];

            String evidenceCodeRefId = getEvidenceCode(evidenceCode);

            Item evidence = createItem("HPOEvidence");
            Item disease = getDisease(dbId);
            evidence.setReference("diseaseReference", disease);
            if (dbRef.isEmpty()) {
                dbRef = dbId;
            }
            evidence.setAttribute("source", dbRef);
            if (StringUtils.isNotEmpty(dbRef)) {
                String[] bits = dbRef.split(";");
                for (String bit : bits) {
                    if (bit.toUpperCase().startsWith("PMID")) {
                        String refId = getPublication(bit);
                        if (refId != null) {
                            evidence.addToCollection("publications", refId);
                        }
                    }
                }
            }

            evidence.setReference("code", evidenceCodeRefId);
            if (!frequency.isEmpty()) {
                evidence.setAttribute("frequencyModifier", frequency);
            }
            if (!assignedBy.isEmpty()) {
                evidence.setAttribute("assignedBy", assignedBy);
            }
            store(evidence);

            Item annotation = getAnnotation(hpoIdentifier, dbId, qualifier);
            annotation.addToCollection("evidences", evidence);
            if (disease != null) {
                disease.addToCollection("hpoAnnotations", annotation);
            }
        }
    }

    private Item getAnnotation(String hpoId, String diseaseId, String qualifier)
        throws ObjectStoreException {
        MultiKey key = new MultiKey(hpoId, diseaseId, qualifier);
        Item annotation = annotations.get(key);
        if (annotation == null) {
            annotation = createItem("HPOAnnotation");
            Item hpoTerm = getTerm(hpoId);
            annotation.setReference("hpoTerm", hpoTerm);
            hpoTerm.addToCollection("hpoAnnotations", annotation);
            if (!qualifier.isEmpty()) {
                annotation.setAttribute("qualifier", qualifier);
            }
            annotations.put(key, annotation);
        }
        return annotation;
    }

    @Override
    public void close() throws Exception {
        store(hpoTerms.values());
        store(diseases.values());
        store(genes.values());
        store(annotations.values());
        super.close();
    }

    private String storeOntology() throws ObjectStoreException {
        Item item = createItem("Ontology");
        item.setAttribute("name", "Human Phenotype Ontology");
        item.setAttribute("url", "http://www.human-phenotype-ontology.org");
        store(item);
        return item.getIdentifier();
    }

    private String getEvidenceCode(String code) throws ObjectStoreException {
        String refId = evidenceCodes.get(code);
        if (refId == null) {
            Item item = createItem("GOEvidenceCode");
            item.setAttribute("code", code);
            refId = item.getIdentifier();
            evidenceCodes.put(code, refId);
            store(item);
        }
        return refId;
    }

    private String getPublication(String ref) throws ObjectStoreException {
        String pubMedId = ref.trim().substring(5);
        if (StringUtil.allDigits(pubMedId)) {
            String pubItemId = publications.get(pubMedId);
            if (pubItemId == null) {
                Item item = createItem("Publication");
                item.setAttribute("pubMedId", pubMedId);
                pubItemId = item.getIdentifier();
                publications.put(pubMedId, pubItemId);
                store(item);
            }
            return pubItemId;
        }
        return null;
    }
}
