package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
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
import java.util.Map.Entry;
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

//    private List<String> ignoreDbList = Arrays.asList("DECIPHER", "ORPHANET");
    private List<String> ignoreDbList = Arrays.asList("DECIPHER");

    private static final String HPOTEAM_FILE = "phenotype_annotation_hpoteam.tab";
    private static final String NEG_FILE = "negative_phenotype_annotation.tab";
    private static final String GENE_FILE =
            "ALL_SOURCES_ALL_FREQUENCIES_diseases_to_genes_to_phenotypes.txt";

    private Map<String, Set<String>> geneToHpoTermMap = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> geneToDiseaseMap = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> hpoTermToGeneMap = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> hpoTermToDiseaseMap = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> diseaseToGeneMap = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> diseaseToHpoTermMap = new HashMap<String, Set<String>>();

    private Map<String, Item> diseaseMap = new HashMap<String, Item>();
    private Map<String, Item> hpoTermMap = new HashMap<String, Item>();
    private Map<String, Item> geneMap = new HashMap<String, Item>();

    private Map<String, String> eviCodeMap = new HashMap<String, String>();
    private Map<MultiKey, Set<String[]>> annoMap = new HashMap<MultiKey, Set<String[]>>();
    private Map<String, String> diseaseIdNameMap = new HashMap<String, String>();
    private Map<String, String> publicationMap = new HashMap<String, String>();
    private Map<String, Set<String>> diseaseToHpoAnnoItemMap = new HashMap<String, Set<String>>();
    private String ontologyItemId = null;

    private static final String HUMAN_TAXON = "9606";
    private String organism = getOrganism(HUMAN_TAXON);

    private String regex = "^(\\*|\\+|#|%)*[0-9]{6,}";
    private String toDiscard = "MOVED TO";

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
            String geneSymbol = line[1];
            String hpoId = line[3];

            if (geneToHpoTermMap.get(geneSymbol) == null) {
                Set<String> hpoTermSet = new HashSet<String>();
                hpoTermSet.add(hpoId);
                geneToHpoTermMap.put(geneSymbol, hpoTermSet);
            } else {
                geneToHpoTermMap.get(geneSymbol).add(hpoId);
            }

            if (geneToDiseaseMap.get(geneSymbol) == null) {
                Set<String> diseaseSet = new HashSet<String>();
                diseaseSet.add(diseaseId);
                geneToDiseaseMap.put(geneSymbol, diseaseSet);
            } else {
                geneToDiseaseMap.get(geneSymbol).add(diseaseId);
            }

            if (hpoTermToGeneMap.get(hpoId) == null) {
                Set<String> geneSet = new HashSet<String>();
                geneSet.add(geneSymbol);
                hpoTermToGeneMap.put(hpoId, geneSet);
            } else {
                hpoTermToGeneMap.get(hpoId).add(geneSymbol);
            }

            if (hpoTermToDiseaseMap.get(hpoId) == null) {
                Set<String> diseaseSet = new HashSet<String>();
                diseaseSet.add(diseaseId);
                hpoTermToDiseaseMap.put(hpoId, diseaseSet);
            } else {
                hpoTermToDiseaseMap.get(hpoId).add(diseaseId);
            }

            if (diseaseToGeneMap.get(diseaseId) == null) {
                Set<String> geneSet = new HashSet<String>();
                geneSet.add(geneSymbol);
                diseaseToGeneMap.put(diseaseId, geneSet);
            } else {
                diseaseToGeneMap.get(diseaseId).add(geneSymbol);
            }

            if (diseaseToHpoTermMap.get(diseaseId) == null) {
                Set<String> hpoTermSet = new HashSet<String>();
                hpoTermSet.add(hpoId);
                diseaseToHpoTermMap.put(diseaseId, hpoTermSet);
            } else {
                diseaseToHpoTermMap.get(diseaseId).add(hpoId);
            }
        }

        // Create items for genes, hpo terms, diseases
        createDisease(diseaseToGeneMap.keySet());
        createHpoTerm(hpoTermToDiseaseMap.keySet());
        storeGene(geneToDiseaseMap);
    }

    private void createDisease(Set<String> dbIdSet) {
        for (String dbId : dbIdSet) {
            if (diseaseMap.get(dbId) == null) {
                Item diseaseItem = createItem("Disease");
                diseaseItem.setAttribute("identifier", dbId);
                diseaseMap.put(dbId, diseaseItem);
            }
        }
    }

    private void createHpoTerm(Set<String> hpoTermSet) {
        for (String hpoTerm : hpoTermSet) {
            if (hpoTermMap.get(hpoTerm) == null) {
                Item item = createItem("HPOTerm");
                item.setAttribute("identifier", hpoTerm);
                item.setReference("ontology", ontologyItemId);
                hpoTermMap.put(hpoTerm, item);
            }
        }
    }

    private void storeGene(Map<String, Set<String>> geneDisease) throws ObjectStoreException {
        for (Entry<String, Set<String>> e : geneDisease.entrySet()) {
            Item gene = createItem("Gene");
            gene.setAttribute("symbol", e.getKey());
            gene.setReference("organism", organism);

            List<String> dList = new ArrayList<String>();
            for (String dbId : e.getValue()) {
                dList.add(diseaseMap.get(dbId).getIdentifier());
            }
            gene.setCollection("diseases", dList);
            store(gene);
            geneMap.put(e.getKey(), gene);
        }
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

            if (dbName.contains(toDiscard)) {
                continue;
            }

            // Save id and namne to map for future use
            dbName = dbName.replaceAll(regex, "").replaceAll("@", "");
            diseaseIdNameMap.put(dbId, dbName.trim());

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

            // Create HPOAnnotation item
            Item annoItem = createItem("HPOAnnotation");

//            if (diseaseMap.get((String)mKey.getKey(0)) == null) {
//                Item diseaseItem = createItem("Disease");
//                diseaseItem.setAttribute("identifier", (String)mKey.getKey(0));
//                diseaseMap.put((String)mKey.getKey(0), diseaseItem);
//            }
//            annoItem.setReference("disease", diseaseMap.get((String)mKey.getKey(0)));

            if (diseaseToHpoAnnoItemMap.get(mKey.getKey(0)) == null) {
                Set<String> annoItemSet = new HashSet<String>();
                annoItemSet.add(annoItem.getIdentifier());
                diseaseToHpoAnnoItemMap.put((String) mKey.getKey(0), annoItemSet);
            } else {
                diseaseToHpoAnnoItemMap.get(mKey.getKey(0)).add(annoItem.getIdentifier());
            }

            if (hpoTermMap.get(mKey.getKey(1)) == null) {
                Item item = createItem("HPOTerm");
                item.setAttribute("identifier", (String) mKey.getKey(1));
                item.setReference("ontology", ontologyItemId);
                hpoTermMap.put((String) mKey.getKey(1), item);
            }
            annoItem.setReference("hpoTerm", hpoTermMap.get(mKey.getKey(1)));
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
                            if (diseaseMap.get(eviInfoBits[0].trim()) == null) {
                                Item diseaseItem = createItem("Disease");
                                diseaseItem.setAttribute("identifier", eviInfoBits[0].trim());
                                diseaseMap.put(eviInfoBits[0].trim(), diseaseItem);
                            }
                            eviItem.setReference("diseaseReference",
                                    diseaseMap.get(eviInfoBits[0].trim()));
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

        storeHpoTerm();
        storeDisease();
    }

    private String storeOntology() throws ObjectStoreException {
        Item item = createItem("Ontology");
        item.setAttribute("name", "Human Phenotype Ontology");
        item.setAttribute("url", "http://www.human-phenotype-ontology.org");
        store(item);
        return item.getIdentifier();
    }

    private void storeHpoTerm() throws ObjectStoreException {
        for (Entry<String, Item> e : hpoTermMap.entrySet()) {
            List<String> dList = new ArrayList<String>();
            if (hpoTermToDiseaseMap.get(e.getKey()) != null) {
                for (String dbId : hpoTermToDiseaseMap.get(e.getKey())) {
                    dList.add(diseaseMap.get(dbId).getIdentifier());
                }
                e.getValue().setCollection("diseases", dList);
            }
            store(e.getValue());
        }
    }

    private void storeDisease() throws ObjectStoreException {
        for (Entry<String, Item> e : diseaseMap.entrySet()) {
            if (diseaseIdNameMap.get(e.getKey()) != null) {
                String rawName = diseaseIdNameMap.get(e.getKey());

                String[] names = rawName.split(";;");
                String dName = names[0].trim();

                // parse alternative titles
                List<String> synItemIds = new ArrayList<String>();
                for (int i = 1; i < names.length; i++) {
                    Item dsItem = createItem("DiseaseSynonym");
                    dsItem.setAttribute("name", names[i].trim());
                    synItemIds.add(dsItem.getIdentifier());
                    store(dsItem);
                }
                e.getValue().setCollection("synonyms", synItemIds);

                e.getValue().setAttribute("name", dName);
            }

            List<String> gList = new ArrayList<String>();
            if (diseaseToGeneMap.get(e.getKey()) != null) {
                for (String g : diseaseToGeneMap.get(e.getKey())) {
                    gList.add(geneMap.get(g).getIdentifier());
                }
                e.getValue().setCollection("genes", gList);
            }

            if (diseaseToHpoAnnoItemMap.get(e.getKey()) != null) {
                e.getValue().setCollection(
                        "hpoAnnotations",
                        new ArrayList<String>(diseaseToHpoAnnoItemMap.get(e
                                .getKey())));
            }

            store(e.getValue());
        }
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
