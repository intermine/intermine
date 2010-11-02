package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

import quicktime.app.display.GroupController;


/**
 *
 * @author
 */
public class OmimConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "OMIM diseases";
    private static final String DATA_SOURCE_NAME = "Online Mendelian Inheritance in Man";
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, Item> diseases = new HashMap<String, Item>();

    private String organism;
    private boolean readOmimTxt = false;
    private boolean readMorbidMap = false;
    private boolean readPubmedCited = false;
    private HgncIdResolverFactory hgncGeneResolverFactory = null;
    private IdResolver geneResolver = null;

    private static final Logger LOG = Logger.getLogger(OmimConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public OmimConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        hgncGeneResolverFactory = new HgncIdResolverFactory();
    }

    @Override
    public void close() throws Exception {
        // TODO if we haven't seen all files throw an error
//        if (!readOmimTxt || !readMorbidMap | !readPubmedCited) {
//            throw new RuntimeException("Did not read all three files!");
//        }
        store(diseases.values());
        super.close();
    }



    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        organism = getOrganism("9606");
        File currentFile = getCurrentFile();
        if ("omim.txt".equals(currentFile.getName())) {
            //processOmimTxtFile(reader);
            readOmimTxt = true;
        } else if ("morbidmap".equals(currentFile.getName())) {
            processMorbidMapFile(reader);
            readMorbidMap = true;
        } else if ("pubmed_cited".equals(currentFile.getName())) {
            processPubmedCitedFile(reader);
            //readPubmedCited = true;
        } else {
            throw new IllegalArgumentException("Unexpected file: " + currentFile.getName());
        }
    }

    private void processOmimTxtFile(Reader reader) throws IOException {
        final BufferedReader br = new BufferedReader(reader);

        String line = null;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("%")) {
                System.out.println(line);
                String[] parts = line.split(" ", 2);
                System.out.println(Arrays.asList(parts));
                String mimNumber = parts[0].substring(1);
                String name = parts[1];

                Item disease = getDisease(mimNumber);
                disease.setAttribute("name", name);
            }
        }
    }



    private void processMorbidMapFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseDelimitedReader(reader, '|');

        int lineCount = 0;
        int resolvedCount = 0;
        class CountPair {
            protected int resolved = 0;
            protected int total = 0;
        }
        Map<String, CountPair> counts = new HashMap<String, CountPair>();
        counts.put("(1)", new CountPair());
        counts.put("(2)", new CountPair());
        counts.put("(3)", new CountPair());
        counts.put("(4)", new CountPair());

        int noMapType = 0;
        int diseaseMatches = 0;
        
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
            System.out.println(Arrays.asList(bits));

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
            String symbol = resolveGene(symbolFromFile);
            if (symbol != null) {
                resolvedCount++;
                String gene = getGeneId(symbol);
                if (geneMapType != null) {
                    counts.get(geneMapType).resolved++;
                }
            }

            m = matchMajorDiseaseNumber.matcher(first);
            List<String> diseaseNumbers = new ArrayList<String>();
            while (m.find()) {
                diseaseMatches++;
                diseaseNumbers.add(m.group(1));
                System.out.println("matched: " + m.group(1) + " in string: " + first);
            }

//            String diseaseName = bits[7];
//            Item disease = createItem("Disease");
//            disease.setAttribute("name", diseaseName);
//            disease.setAttribute("source", "OMIM");

            // start with basic rules and count how many columns are parsed
            // if gene is an HGNC symbol - create a gene

            // if disease id in first column, create a disease object

            // if not create a region

        }
        LOG.info("Resolved " + resolvedCount + " of " + lineCount + " gene symbols from file.");
        String mapTypesMessage = "Counts of resolved genes/ total for each map type: ";
        for (Map.Entry<String, CountPair> pair : counts.entrySet()) {
            mapTypesMessage += pair.getKey() + ": " + pair.getValue().resolved + " / " + pair.getValue().total + "  ";
        }
        LOG.info(mapTypesMessage);
        LOG.info("Found " + diseaseMatches + " disease matches from " + lineCount + " line file.");
    }

    private String resolveGene(String fromFile) {
        if (geneResolver == null) {
            geneResolver = hgncGeneResolverFactory.getIdResolver(true);
        }
        int resCount = geneResolver.countResolutions("9606", fromFile);
        if (resCount == 1) {
            return geneResolver.resolveId("9606", fromFile).iterator().next();
        }
        LOG.warn("Could not resolve identifier to a single gene: " + fromFile + " count: "
                + resCount);
        return null;
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
            if ("1".equals(pos)) {
                addPubCollection(mimNumber, currentPubs);
                currentPubs = new ArrayList<String>();
            }
            currentPubs.add(getPubId(pubmedId));
        }
        addPubCollection(mimNumber, currentPubs);
    }

    private void addPubCollection(String mimNumber, List<String> pubs) {
        if (!pubs.isEmpty()) {
            Item disease = getDisease(mimNumber);
            disease.setCollection("publications", pubs);
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

    private String getGeneId(String symbol) throws ObjectStoreException {
        String geneId = genes.get(symbol);
        if (geneId == null) {
            Item gene = createItem("Gene");
            gene.setAttribute("primaryIdentifier", symbol);
            gene.setReference("organism", organism);
            geneId = gene.getIdentifier();
            genes.put(symbol, geneId);
            store(gene);
        }
        return geneId;
    }
}
