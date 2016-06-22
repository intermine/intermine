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
import java.util.ArrayList;
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

    private Map<String, Item> diseaseMap = new HashMap<String, Item>();
    private Map<String, Item> hpoTermMap = new HashMap<String, Item>();
    private Map<String, Item> geneMap = new HashMap<String, Item>();

    private Map<String, String> eviCodeMap = new HashMap<String, String>();
    private Map<MultiKey, Set<String[]>> annoMap = new HashMap<MultiKey, Set<String[]>>();
    private Map<String, String> publicationMap = new HashMap<String, String>();
    private Map<String, Set<String>> diseaseToHpoAnnoItemMap = new HashMap<String, Set<String>>();
    private String ontologyItemId = null;

    private static final String HUMAN_TAXON = "9606";
    private String organism = getOrganism(HUMAN_TAXON);

    private static final String REGEX = "^(\\*|\\+|#|%)*[0-9]{6,}";
    private static final String TO_DISCARD = "MOVED TO";

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
        processAnnoFile(new FileReader(files.get(HPOTEAM_FILE)));
        processAnnoFile(new FileReader(files.get(NEG_FILE)));
        parseAnnotation();
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
        Item item = diseaseMap.get(omimId);
        if (item == null) {
            item = createItem("Disease");
            item.setAttribute("identifier", omimId);
            diseaseMap.put(omimId, item);
        }
        return item;
    }

    private Item getTerm(String hpoTerm) {
        Item item = hpoTermMap.get(hpoTerm);
        if (item == null) {
            item = createItem("HPOTerm");
            item.setAttribute("identifier", hpoTerm);
            item.setReference("ontology", ontologyItemId);
            hpoTermMap.put(hpoTerm, item);
        }
        return item;
    }

    private Item getGene(String identifier) throws ObjectStoreException {
        Item item = geneMap.get(identifier);
        if (item == null) {
            item = createItem("Gene");
            item.setAttribute("primaryIdentifier", identifier);
            item.setReference("organism", organism);
            geneMap.put(identifier, item);
        }
        return item;
    }

    /**
     * @param reader file reader
     * @throws IOException if can't read file
     * @throws ObjectStoreException if can't store to db
     */
    protected void processAnnoFile(Reader reader) throws IOException, ObjectStoreException {
        BufferedReader br = new BufferedReader(reader);
        String line = null;

        // loop through entire file
        while ((line = br.readLine()) != null) {
            String[] array = line.split("\t", -1); // keep trailing empty Strings

            // HPO Annotation File Format:
            // http://www.human-phenotype-ontology.org/contao/index.php/annotation-guide.html
            if (array.length < 9) {
                throw new IllegalArgumentException("Not enough elements (should be > 8 not "
                        + array.length + ") in line: " + line);
            }

            String db = array[0];
            if (ignoreDbList.contains(db)) {
                continue;
            }

            String dbId = db + ":" + array[1];
            String dbName = array[2];

            if (dbName.contains(TO_DISCARD)) {
                continue;
            }

            dbName = dbName.replaceAll(REGEX, "").replaceAll("@", "");


            String qualifier = array[3];
            String hpoId = array[4];
            String dbRef = array[5];
            String eviCode = array[6];
            String freq = array[8];
            String assignedBy = array[13];

            storeEvidenceCode(eviCode);

            String[] eviInfo = {dbRef, eviCode, freq, assignedBy};

            if (annoMap.get(new MultiKey(dbId, hpoId, qualifier)) == null) {
                Set<String[]> eviInfoSet = new HashSet<String[]>();
                eviInfoSet.add(eviInfo);
                annoMap.put(new MultiKey(dbId, hpoId, qualifier), eviInfoSet);
            } else {
                annoMap.get(new MultiKey(dbId, hpoId, qualifier)).add(eviInfo);
            }
        }
    }

    /**
     * @throws ObjectStoreException if can't store to db
     */
    protected void parseAnnotation() throws ObjectStoreException {
        for (MultiKey mKey : annoMap.keySet()) {
            Item annoItem = createItem("HPOAnnotation");
            if (diseaseToHpoAnnoItemMap.get(mKey.getKey(0)) == null) {
                Set<String> annoItemSet = new HashSet<String>();
                annoItemSet.add(annoItem.getIdentifier());
                diseaseToHpoAnnoItemMap.put((String) mKey.getKey(0), annoItemSet);
            } else {
                diseaseToHpoAnnoItemMap.get(mKey.getKey(0)).add(annoItem.getIdentifier());
            }

            String hpoTermId = (String) mKey.getKey(1);
            Item hpoTerm = getTerm(hpoTermId);
            annoItem.setReference("hpoTerm", hpoTerm);
            hpoTermMap.get(mKey.getKey(1)).setReference("hpoAnnotation", annoItem);

            if (!((String) mKey.getKey(2)).isEmpty()) {
                annoItem.setAttribute("qualifier", (String) mKey.getKey(2));
            }

            List<String> eviIdList = new ArrayList<String>();
            for (String[] eviInfoBits : annoMap.get(mKey)) {
                // Create Evidence item
                Item eviItem = createItem("HPOEvidence");
                if (!eviInfoBits[0].isEmpty()) {
                    eviItem.setAttribute("source", eviInfoBits[0]);
                    if (eviInfoBits[0].toUpperCase().startsWith("PMID")) {
                        eviItem.setCollection("publications", storePublication(eviInfoBits[0]));
                    } else {
                        if (eviInfoBits[0].trim().matches("^(OMIM|DECIPHER|ORPHANET):[0-9]{6,}$")) {
                            String diseaseId = eviInfoBits[0].trim();
                            Item disease = getDisease(diseaseId);
                            eviItem.setReference("diseaseReference", disease);
                        }
                    }
                }

                eviItem.setReference("code", eviCodeMap.get(eviInfoBits[1]));

                if (!eviInfoBits[2].isEmpty()) {
                    eviItem.setAttribute("frequencyModifier", eviInfoBits[2]);
                }

                if (!eviInfoBits[3].isEmpty()) {
                    eviItem.setAttribute("assignedBy", eviInfoBits[3]);
                }

                store(eviItem);
                eviIdList.add(eviItem.getIdentifier());
            }

            annoItem.setCollection("evidences", eviIdList);
            store(annoItem);
        }
    }

    @Override
    public void close() throws Exception {
        store(diseaseMap.values());
        store(geneMap.values());
        store(hpoTermMap.values());
        super.close();
    }


    private String storeOntology() throws ObjectStoreException {
        Item item = createItem("Ontology");
        item.setAttribute("name", "Human Phenotype Ontology");
        item.setAttribute("url", "http://www.human-phenotype-ontology.org");
        store(item);
        return item.getIdentifier();
    }

    private void storeEvidenceCode(String code) throws ObjectStoreException {
        if (eviCodeMap.get(code) == null) {
            Item item = createItem("GOEvidenceCode");
            item.setAttribute("code", code);
            eviCodeMap.put(code, item.getIdentifier());
            store(item);
        }
    }

    private List<String> storePublication(String ref) throws ObjectStoreException {
        List<String> pubItemIdList = new ArrayList<String>();
        String[] pubs = StringUtils.split(ref, ";");
        for (String pub : pubs) {
            String pubMedId = pub.trim().substring(5);
            if (StringUtil.allDigits(pubMedId)) {
                String pubItemId = publicationMap.get(pubMedId);
                if (pubItemId == null) {
                    Item item = createItem("Publication");
                    item.setAttribute("pubMedId", pubMedId);
                    pubItemId = item.getIdentifier();
                    publicationMap.put(pubMedId, pubItemId);
                    store(item);
                }
                pubItemIdList.add(pubItemId);
            }
        }
        return pubItemIdList;
    }
}
