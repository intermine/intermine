package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2013 FlyMine
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;

/**
 * DataConverter to parse a HPO annotation file into Items.
 *
 * @author Fengyuan Hu
 */
public class HpoConverter extends BioDirectoryConverter
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(HpoConverter.class);

    private static final String DATASET_TITLE = "HPO Annotation";
    private static final String DATA_SOURCE_NAME = "HPO";

    private List<String> ignoreDbList = Arrays.asList("DECIPHER", "ORPHANET");

    private static final String HPOTEAM_FILE = "phenotype_annotation_hpoteam.tab";
    private static final String NEG_FILE = "negative_phenotype_annotation.tab";
    private static final String GENE_FILE = "ALL_SOURCES_ALL_FREQUENCIES_phenotype_to_genes.txt";

    private Map<String, List<String[]>> diseaseToAnnoMap = new HashMap<String, List<String[]>>();
    private Map<String, String> eviMap = new HashMap<String, String>();
    private Map<String, String> hpoTermMap = new HashMap<String, String>();
    private Map<String, String> diseaseMap = new HashMap<String, String>();
    private Map<String, String> diseaseIdNameMap = new HashMap<String, String>();
    private Map<String, String> publicationMap = new HashMap<String, String>();
    private Map<String, String> hpoTermToHpoAnnoItemIdMap = new HashMap<String, String>();
    private String ontologyItemId = null;

    private static final String HUMAN_TAXON = "9606";
    private String organism = getOrganism(HUMAN_TAXON);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
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
        processAnnoFile(new FileReader(files.get(HPOTEAM_FILE)));
        processAnnoFile(new FileReader(files.get(NEG_FILE)));
        processGeneFile(new FileReader(files.get(GENE_FILE)));
    }

    private Map<String, File> readFilesInDir(File dir) {
        Map<String, File> files = new HashMap<String, File>();
        for (File file : dir.listFiles()) {
            files.put(file.getName(), file);
        }
        return files;
    }

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
            // Save id and namne to map for future use
            if (!dbName.isEmpty()) {
                if (dbName.matches("^(#|%|[0-9]).*$")) {
                    dbName = dbName.substring(dbName.indexOf(" "));
                }
                diseaseIdNameMap.put(dbId, dbName.trim());
            }

            String qualifier = array[3];
            String hpoId = array[4];
            String dbRef = array[5];
            String strEvidence = array[6];

            storeHpoTerm(hpoId);
            storeEvidenceCode(strEvidence);

            String[] annoInfo = {qualifier, hpoId, dbRef, strEvidence};

            if (diseaseToAnnoMap.get(dbId) == null) {
                List<String[]> annoInfoList = new ArrayList<String[]>();
                annoInfoList.add(annoInfo);
                diseaseToAnnoMap.put(dbId, annoInfoList);
            } else {
                diseaseToAnnoMap.get(dbId).add(annoInfo);
            }
        }

        for (String dbId : diseaseToAnnoMap.keySet()) {
            List<String> annoRefIds = new ArrayList<String>();
            for (String[] infoBits : diseaseToAnnoMap.get(dbId)) {
                // Create Evidence item
                Item eviItem = createItem("HPOEvidence");
                if (infoBits[2].isEmpty()) {
                    // DB reference field is empty as assigned by HPO:curators
                    eviItem.setAttribute("source", "HPO:curators");
                } else {
                    eviItem.setAttribute("source", infoBits[2]);
                    if (infoBits[2].toUpperCase().startsWith("PMID")) {
                        eviItem.setCollection("publications", storePublication(infoBits[2]));
                    }
                }
                eviItem.setReference("code", eviMap.get(infoBits[3]));
                store(eviItem);

                // Create HPOAnnotation item
                Item annoItem = createItem("HPOAnnotation");
                if (!infoBits[0].isEmpty()) {
                    annoItem.setAttribute("qualifier", infoBits[0]);
                }
                String hpoTerm = hpoTermMap.get(infoBits[1]);
                annoItem.setReference("hpoTerm", hpoTerm);
                annoItem.setCollection("evidence", Arrays.asList(eviItem.getIdentifier()));
                annoRefIds.add(annoItem.getIdentifier());
                store(annoItem);
                hpoTermToHpoAnnoItemIdMap.put(infoBits[1], annoItem.getIdentifier());
            }
            storeDisease(dbId, annoRefIds);
        }
    }

    protected void processGeneFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<?> lineIter = FormattedTextParser.
                parseTabDelimitedReader(new BufferedReader(reader));

        Map<String, Set<String>> geneToHpoAnnoItemIdsMap = new HashMap<String, Set<String>>();

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line[0].startsWith("#")) {
                continue;
            }

            String hpoTerm = line[0];
            String symbol = line[3];

            String hpoAnnoItemId = hpoTermToHpoAnnoItemIdMap.get(hpoTerm);
            if (hpoAnnoItemId == null) {
                continue;
            }

            if (geneToHpoAnnoItemIdsMap.get(symbol) == null) {
                Set<String> hpoAnnoItemIdsSet = new HashSet<String>();
                hpoAnnoItemIdsSet.add(hpoAnnoItemId);
                geneToHpoAnnoItemIdsMap.put(symbol, hpoAnnoItemIdsSet);
            } else {
                geneToHpoAnnoItemIdsMap.get(symbol).add(hpoAnnoItemId);
            }
        }

        for (Entry<String, Set<String>> e : geneToHpoAnnoItemIdsMap.entrySet()) {
            Item gene = createItem("Gene");
            gene.setAttribute("symbol", e.getKey());
            gene.setReference("organism", organism);
            gene.setCollection("hpoAnnotations", new ArrayList<String>(e.getValue()));
            store(gene);
        }
    }

    private String storeOntology() throws ObjectStoreException {
        Item item = createItem("Ontology");
        item.setAttribute("name", "Human Phenotype Ontology");
        item.setAttribute("url", "http://www.human-phenotype-ontology.org");
        store(item);
        return item.getIdentifier();
    }

    private void storeDisease(String dbId, List<String> annoRefIds) throws ObjectStoreException {
        if (diseaseMap.get(dbId) == null) {
            Item item = createItem("Disease");
            item.setAttribute("identifier", dbId);
            if (diseaseIdNameMap.get(dbId) != null) {
                item.setAttribute("name", diseaseIdNameMap.get(dbId));
            }
            item.setCollection("hpoAnnotations", annoRefIds);
            diseaseMap.put(dbId, item.getIdentifier());
            store(item);
        }
    }

    private void storeHpoTerm(String hpoTerm) throws ObjectStoreException {
        if (hpoTermMap.get(hpoTerm) == null) {
            Item item = createItem("HPOTerm");
            item.setAttribute("identifier", hpoTerm);
            item.setReference("ontology", ontologyItemId);
            hpoTermMap.put(hpoTerm, item.getIdentifier());
            store(item);
        }
    }

    private void storeEvidenceCode(String code) throws ObjectStoreException {
        if (eviMap.get(code) == null) {
            Item item = createItem("GOEvidenceCode");
            item.setAttribute("code", code);
            eviMap.put(code, item.getIdentifier());
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
