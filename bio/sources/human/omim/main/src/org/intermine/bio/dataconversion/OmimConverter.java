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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * Load disease data from OMIM and relationship to genes, publications and SNPs.
 * @author Richard Smith
 */
public class OmimConverter extends BioDirectoryConverter
{

    private static final Logger LOG = Logger.getLogger(OmimConverter.class);

    private static final String DATASET_TITLE = "OMIM diseases";
    private static final String DATA_SOURCE_NAME = "Online Mendelian Inheritance in Man";
    private static final String HUMAN_TAXON = "9606";

    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, Item> diseases = new HashMap<String, Item>();

    private String organism;

    private static final String OMIM_TXT_FILE = "omim.txt";
    private static final String MORBIDMAP_FILE = "morbidmap";
    private static final String PUBMED_FILE = "pubmed_cited";

    private IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public OmimConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    @Override
    public void close() throws Exception {
        store(diseases.values());
        super.close();
    }

    /**
     * {@inheritDoc}
     */
    public void process(File dataDir) throws Exception {
        Map<String, File> files = readFilesInDir(dataDir);

        organism = getOrganism(HUMAN_TAXON);

        rslv = IdResolverService.getIdResolverByOrganism(HUMAN_TAXON);

        String[] requiredFiles = new String[] {OMIM_TXT_FILE, MORBIDMAP_FILE, PUBMED_FILE};
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

        processMorbidMapFile(new FileReader(files.get(MORBIDMAP_FILE)));
        processOmimTxtFile(new FileReader(files.get(OMIM_TXT_FILE)));
        processPubmedCitedFile(new FileReader(files.get(PUBMED_FILE)));
    }

    private Map<String, File> readFilesInDir(File dir) {
        Map<String, File> files = new HashMap<String, File>();
        for (File file : dir.listFiles()) {
            files.put(file.getName(), file);
        }
        return files;
    }

    private void processOmimTxtFile(Reader reader) throws IOException {
        final BufferedReader br = new BufferedReader(reader);

        String line = null;

        StringBuilder sb = new StringBuilder();
        boolean readingTitle = false;
        while ((line = br.readLine()) != null) {
            if (readingTitle) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(line.trim());
            }
            if (line.startsWith("*FIELD* TI")) {
                readingTitle = true;
            } else if (line.startsWith("*FIELD* TX")) {
                readingTitle = false;

                // s contains line after start of TI and includes TX header line
                String s = sb.toString();
                // check if this is a deprecated entry
                if (s.startsWith("^")) {
                    sb = new StringBuilder();
                    continue;
                }

                // if first character is not a digit, remove it
                if (!Character.isDigit(s.charAt(0))) {
                    s = s.substring(1);
                }

                // MIM number is now first thing on line
                String[] parts = s.split(" ", 2);
                String mimNumber = parts[0];
                String text = parts[1];

                // if this isn't a disease we need we can just ignore
                if (diseases.containsKey(mimNumber)) {
                    int terminateAt = s.length();
                    for (int i = 0; i < s.length(); i++) {
                        if (text.charAt(i) == '*'
                            || (text.charAt(i) == ';' && text.charAt(i + 1) == ';')) {
                            terminateAt = i;
                            break;
                        }
                    }
                    // title is text until ;; or the terminating *
                    String title = text.substring(0, terminateAt);

                    Item disease = getDisease(mimNumber);
                    disease.setAttribute("name", title);
                }

                sb = new StringBuilder();
            }
        }
    }

    private void processMorbidMapFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseDelimitedReader(reader, '|');

        int lineCount = 0;
        int resolvedCount = 0;

        /**
         * a class to represent CountPair
         */
        class CountPair
        {
            protected int resolved = 0;
            protected int total = 0;
        }
        Map<String, CountPair> counts = new HashMap<String, CountPair>();
        List<String> diseaseNumbers = new ArrayList<String>();

        int noMapType = 0;
        int diseaseMatches = 0;

        File f = new File("build/omim_not_loaded.txt");
        FileWriter fw = new FileWriter(f);

        // extract e.g. (3)
        Pattern matchNumberInBrackets = Pattern.compile("(\\(.\\))$");

        // pull out OMIM id of disease
        Pattern matchMajorDiseaseNumber = Pattern.compile("(\\d{6})");

        while (lineIter.hasNext()) {
            lineCount++;

            String[] bits = lineIter.next();
            if (bits.length == 0) {
                continue;
            }

            String first = bits[0].trim();

            Matcher m = matchNumberInBrackets.matcher(first);
            String geneMapType = null;
            if (m.find()) {
                geneMapType = m.group(1);
            }
            if (geneMapType == null) {
                noMapType++;
            } else {
                if (!counts.containsKey(geneMapType)) {
                    counts.put(geneMapType, new CountPair());
                }
                counts.get(geneMapType).total++;
            }
            String symbolStr = bits[1];
            String[] symbols = symbolStr.split(",");
            // main HGNC symbols is first, others are synonyms
            String symbolFromFile = symbols[0].trim();
            String ncbiGeneNumber = resolveGene(symbolFromFile);
            if (ncbiGeneNumber != null) {
                resolvedCount++;
                //String gene = getGeneId(symbol);
                if (geneMapType != null) {
                    counts.get(geneMapType).resolved++;
                }
            }
            String geneId = getGeneIdentifier(symbolFromFile);
            m = matchMajorDiseaseNumber.matcher(first);
            String diseaseMimId = null;
            while (m.find()) {
                diseaseMatches++;
                diseaseMimId = m.group(1);
            }

            if (diseaseMimId != null && geneId != null) {
                Item disease = getDisease(diseaseMimId);
                disease.addToCollection("genes", geneId);
            } else {
                StringBuilder sb = new StringBuilder();
                for (String bit : bits) {
                    if (sb.length() > 0) {
                        sb.append("|");
                    }
                    sb.append(bit);
                }
                sb.append(System.getProperty("line.separator"));
                fw.write(sb.toString());
            }

            // start with basic rules and count how many columns are parsed
            // if gene is an HGNC symbol - create a gene

            // if disease id in first column, create a disease object

            // if not create a region

        }
        fw.flush();
        fw.close();
        LOG.info("Resolved " + resolvedCount + " of " + lineCount + " gene symbols from file.");
        String mapTypesMessage = "Counts of resolved genes/ total for each map type: ";
        for (Map.Entry<String, CountPair> pair : counts.entrySet()) {
            mapTypesMessage += pair.getKey() + ": " + pair.getValue().resolved
                + " / " + pair.getValue().total + "  ";
        }
        LOG.info(mapTypesMessage);
        LOG.info("Found " + diseaseMatches + " to " + diseaseNumbers.size()
                + " unique diseases from " + lineCount + " line file.");
    }

    private String resolveGene(String symbol) {
        int resCount = rslv.countResolutions("" + HUMAN_TAXON, symbol);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + symbol + " count: " + resCount + " - "
                     + rslv.resolveId("" + HUMAN_TAXON, symbol));
            return null;
        }
        return rslv.resolveId("" + HUMAN_TAXON, symbol).iterator().next();
    }

    private void processPubmedCitedFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        List<String> currentPubs = new ArrayList<String>();
        String mimNumber = null;
        while (lineIter.hasNext()) {
            String[] bits = lineIter.next();
            mimNumber = bits[0];
            String pos = bits[1];
            String pubmedId = bits[2];
            // all the diseases we need are already create from morbidmap file
            if (diseases.containsKey(mimNumber)) {
                // are we on the first row for a particular MIM number
                if ("1".equals(pos)) {
                    addPubCollection(mimNumber, currentPubs);
                    currentPubs = new ArrayList<String>();
                }
                currentPubs.add(getPubId(pubmedId));
            }
        }
        if (diseases.containsKey(mimNumber)) {
            addPubCollection(mimNumber, currentPubs);
        }
    }

    private void addPubCollection(String mimNumber, List<String> newPubs) {
        if (!pubs.isEmpty()) {
            Item disease = getDisease(mimNumber);
            disease.setCollection("publications", newPubs);
        }
    }

    private Item getDisease(String mimNumber) {
        Item disease = diseases.get(mimNumber);
        if (disease == null) {
            disease = createItem("Disease");
            disease.setAttribute("identifier", mimNumber);
            diseases.put(mimNumber, disease);
        }
        return disease;
    }

    private String getPubId(String pubmed) throws ObjectStoreException {
        String pubId = pubs.get(pubmed);
        if (pubId == null) {
            Item pub = createItem("Publication");
            pub.setAttribute("pubMedId", pubmed);
            pubId = pub.getIdentifier();
            pubs.put(pubmed, pubId);
            store(pub);
        }
        return pubId;
    }
    private String getGeneIdentifier(String symbol) throws ObjectStoreException {
        String geneIdentifier = null;
        String entrezGeneNumber = resolveGene(symbol);
        if (entrezGeneNumber != null) {
            geneIdentifier = genes.get(entrezGeneNumber);
            if (geneIdentifier == null) {
                Item gene = createItem("Gene");
                gene.setAttribute("primaryIdentifier", entrezGeneNumber);
                gene.setReference("organism", organism);
                store(gene);
                geneIdentifier = gene.getIdentifier();
                genes.put(entrezGeneNumber, geneIdentifier);
            }
        }
        return geneIdentifier;
    }
}
