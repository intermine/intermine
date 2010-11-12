package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;

/**
 * DataConverter to create items from DRSC RNAi screen date files.
 *
 * @author Kim Rutherford
 * @author Richard Smith
 */
public class FlyRNAiScreenConverter extends BioFileConverter
{
    protected Item organism, hfaSource;

    private Map<String, Item> genes = new HashMap<String, Item>();
    private Map<String, Item> publications = new HashMap<String, Item>();
    private Map<String, Item> screenMap = new HashMap<String, Item>();
    private static final String TAXON_ID = "7227";
    // access to current file for error messages
    private String fileName;
    private Set<String> hitScreenNames = new HashSet<String>();
    private Set<String> detailsScreenNames = new HashSet<String>();
    protected IdResolverFactory resolverFactory;

    protected static final Logger LOG = Logger.getLogger(FlyRNAiScreenConverter.class);
    /**
     * Create a new FlyRNAiScreenConverter object.
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public FlyRNAiScreenConverter(ItemWriter writer, Model model) {
        super(writer, model, "DRSC", "DRSC data set");

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }
    private static final Map<String, String> RESULTS_KEY = new HashMap<String, String>();

    static {
        RESULTS_KEY.put("N", "Not a Hit");
        RESULTS_KEY.put("Y", "Hit");
        RESULTS_KEY.put("S", "Strong Hit");
        RESULTS_KEY.put("M", "Medium Hit");
        RESULTS_KEY.put("W", "Weak Hit");
        RESULTS_KEY.put("NS", "Not Screened");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        // set up common items
        if (organism == null) {
            organism = createItem("Organism");
            organism.setAttribute("taxonId", TAXON_ID);
            store(organism);
        }

        fileName = getCurrentFile().getName();
        if (fileName.startsWith("RNAi_all_hits")) {
            processHits(reader);
        } else if (fileName.startsWith("RNAi_screen_details")) {
            processScreenDetails(reader);
        }
    }

    /**
     * Check that we have seen the same screen names in the hits and details files.
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {

        for (Item screen : screenMap.values()) {
            store(screen);
        }

        Set<String> noDetails = new HashSet<String>();
        for (String screenName : hitScreenNames) {
            if (!detailsScreenNames.contains(screenName)) {
                noDetails.add(screenName);
            }
        }

        Set<String> noHits = new HashSet<String>();
        for (String screenName : detailsScreenNames) {
            if (!hitScreenNames.contains(screenName)) {
                noHits.add(screenName);
            }
        }

        if (!noHits.isEmpty() || !noDetails.isEmpty()) {
            String msg = "Screen names from hits file and details file did not match."
                    + (noHits.isEmpty()
                       ? ""
                       : "  No hits found for screen detail: '" + noHits + "'")
                    + (noDetails.isEmpty()
                       ? ""
                       : "  No details found for screen hit: '" + noDetails + "'");
            throw new RuntimeException(msg);
            //LOG.error(msg);
        }
        super.close();
    }

    private void processHits(Reader reader)
        throws ObjectStoreException {

        boolean readingData = false;
        int headerLength = 0;
        Item[] screens = null;

        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }
        int lineNumber = 0;
        while (tsvIter.hasNext()) {
            lineNumber++;
            String [] line = (String[]) tsvIter.next();
            if (!readingData) {
                if ("Amplicon".equals(line[0].trim())) {
                    readingData = true;
                    headerLength = line.length;
                    screens = new Item[headerLength - 2];
                    for (int i = 2; i < line.length; i++) {
                        // create an array of screen item identifiers (first two slots empty)
                        String screenName = line[i].trim();
                        screens[i - 2] = getScreen(screenName);
                        hitScreenNames.add(screenName);
                    }
                }
            } else {
                if (line.length != headerLength) {
                    String msg = "Incorrect number of entries in line number " + lineNumber
                        + ": " + line.toString()
                        + ".  Should be " + headerLength + " but is " + line.length + " instead."
                        + "  content:" + line[0];
                    throw new RuntimeException(msg);
                }

                Set<Item> ampliconGenes = new LinkedHashSet<Item>();

                String ampliconIdentifier = line[0].trim();
                Item amplicon = createItem("PCRProduct");
                amplicon.setAttribute("primaryIdentifier", ampliconIdentifier);
                amplicon.setReference("organism", organism);

                // the amplicon may target zero or more genes, a gene can be targeted
                // by more than one amplicon.
                if (StringUtils.isNotEmpty(line[1])) {
                    String [] geneNames = line[1].split(",");
                    for (int i = 0; i < geneNames.length; i++) {
                        String geneSymbol = geneNames[i].trim();
                        Item gene = newGene(geneSymbol);
                        if (gene != null) {
                            ampliconGenes.add(gene);
                            amplicon.addToCollection("genes", gene);
                        }
                    }
                }

                // loop over screens to create results
                for (int j = 0; j < screens.length; j++) {
                    String resultValue = RESULTS_KEY.get(line[j + 2].trim());
                    if (resultValue == null) {
                        throw new RuntimeException("Unrecogised result symbol '" + line[j + 2]
                            + "' in line: " + Arrays.asList(line));
                    }

                    if (genes.isEmpty()) {
                        // create a hit that doesn't reference a gene
                        Item screenHit = createItem("RNAiScreenHit");
                        String refId = screenHit.getIdentifier();
                        screenHit.setReference("rnaiScreen", screens[j]);
                        screenHit.setAttribute("result", resultValue);
                        screenHit.setReference("pcrProduct", amplicon);
                        screens[j].addToCollection("rnaiScreenHits", refId);
                        store(screenHit);
                    } else {
                        // create one hit for each gene targeted
                        for (Item gene : ampliconGenes) {
                            Item screenHit = createItem("RNAiScreenHit");
                            String refId = screenHit.getIdentifier();
                            screenHit.setReference("rnaiScreen", screens[j]);
                            screenHit.setReference("gene", gene);
                            screenHit.setAttribute("result", resultValue);
                            screenHit.setReference("pcrProduct", amplicon);
                            //screens[j].getCollection("genes").addRefId(gene.getIdentifier());
                            gene.addToCollection("rnaiResults", screenHit.getIdentifier());
                            screens[j].addToCollection("rnaiScreenHits", refId);
                            store(screenHit);
                        }
                    }
                }
                store(amplicon);
            }
        }

        for (Item gene : genes.values()) {
            store(gene);
        }
    }

    private void processScreenDetails(Reader reader) throws ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        while (tsvIter.hasNext()) {
            String [] line = (String[]) tsvIter.next();

            if (line.length != 5) {
                throw new RuntimeException("Did not find five elements in line, found "
                          + line.length + ": " + Arrays.asList(line));
            }
            String pubmedId = line[0].trim();

            if (pubmedId.equals("Pubmed_ID")) {
                // skip header
                continue;
            }
            Item publication = getPublication(pubmedId);

            String screenName = line[2].trim();
            detailsScreenNames.add(screenName);
            Item screen = getScreen(screenName);
            screen.setAttribute("name", screenName);
            screen.setAttribute("cellLine", line[3].trim());
            String analysisDescr = line[4].trim();
            if (StringUtils.isNotEmpty(analysisDescr)) {
                screen.setAttribute("analysisDescription", line[4].trim());
            }
            screen.setReference("organism", organism);
            screen.setReference("publication", publication);

            // the hits file may be processed first
            screenMap.remove(screenName);
            screenMap.put(screenName, screen);
        }
    }


    // Fetch or create a Publication
    private Item getPublication(String pubmedId) throws ObjectStoreException {
        Item publication = publications.get(pubmedId);
        if (publication == null) {
            publication = createItem("Publication");
            publication.setAttribute("pubMedId", pubmedId);
            publications.put(pubmedId, publication);
            store(publication);
        }
        return publication;
    }

    // Fetch of create an RNAiScreen
    private Item getScreen(String screenName) {
        Item screen = screenMap.get(screenName);
        if (screen == null) {
            screen = createItem("RNAiScreen");
            screenMap.put(screenName, screen);
        }
        return screen;
    }


    /**
     * Convenience method to create a new gene Item
     * @param geneSymbol the gene symbol
     * @return a new gene Item
     */
    protected Item newGene(String geneSymbol) {
        if (geneSymbol == null) {
            throw new RuntimeException("geneSymbol can't be null");
        }
        IdResolver resolver = resolverFactory.getIdResolver();
        int resCount = resolver.countResolutions(TAXON_ID, geneSymbol);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + geneSymbol + " count: " + resCount + " FBgn: "
                     + resolver.resolveId(TAXON_ID, geneSymbol));
            return null;
        }
        String primaryIdentifier = resolver.resolveId(TAXON_ID, geneSymbol).iterator().next();
        Item item = genes.get(primaryIdentifier);
        if (item == null) {
            item = createItem("Gene");
            item.setAttribute("primaryIdentifier", primaryIdentifier);
            item.setReference("organism", organism);
            genes.put(primaryIdentifier, item);
        }
        return item;
    }
}

