package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;

/**
 * DataConverter to create items from DRSC RNAi screen date files.
 *
 * @author Kim Rutherford
 */
public class FlyRNAiScreenConverter extends FileConverter
{
    protected Item dataSource, organism, hfaSource;

    private Map<String,Item> genes = new HashMap<String,Item>();
    private Map<String,Item> publications = new HashMap<String,Item>();
    private Map<String,Item> screenMap = new HashMap<String,Item>();
    private Map<String, String> resultValues = new HashMap<String,String>();
    protected String taxonId = "7227";
    // access to current file for error messages
    private String fileName;
    private Set<String> hitScreenNames = new HashSet<String>();
    private Set<String> detailsScreenNames = new HashSet<String>();
    private Item dataSet;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     */
    public FlyRNAiScreenConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }

    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        // set up common items
        if (organism == null) {
            organism = createItem("Organism");
            organism.setAttribute("taxonId", taxonId);
            store(organism);
        }

        if (dataSource == null) {
            dataSource = createItem("DataSource");
            dataSource.setAttribute("name", "Drosophila RNAi Screening Center");
            store(dataSource);
        }

        if (dataSet == null) {
            dataSet = createItem("DataSet");
            dataSet.setAttribute("title", "DRSC RNAi data set");
            dataSet.setReference("dataSource", dataSource);
            store(dataSet);
        }

        fileName = getCurrentFile().getPath();
        if (fileName.endsWith("RNAi_all_hits.txt")) {
            processHits(reader);
        } else if (fileName.endsWith("RNAi_screen_details")) {
            processScreenDetails(reader);
        }
    }

    /*
     * Check that we have seen the same screen names in the hits and details files.
     * @see org.intermine.dataconversion.FileConverter#close()
     */
    public void close() throws Exception {
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
            throw new RuntimeException("Screen names from hits file and details file did not match."
                    + (noHits.isEmpty() ? "" : "  No hits found for screen detail: " + noHits)
                    + (noDetails.isEmpty() ? "" : "  No details found for screen hit: " + noDetails));
        }
        super.close();
    }

    private void processHits(Reader reader) throws ObjectStoreException {

        boolean readingData = false;
        int headerLength = 0;
        Item[] screens = null;
        
        Iterator tsvIter;
        try {
            tsvIter = TextFileUtil.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        while (tsvIter.hasNext()) {
            String [] line = (String[]) tsvIter.next();

            if (!readingData) {
                
                if (line.length == 2) {
                    System.out.println("line (" + line.length + ") - " + Arrays.asList(line));
                    // this is the key to result symbol, put them in a map.  Strip off 'A '.
                    if (!line[0].equals("") && !line[1].equals("")) {
                        String value = line[1].trim();
                        if (value.startsWith("A ")) {
                            value = value.substring(2);
                        }
                        resultValues.put(line[0].trim(), value);
                    }
                } else if (line[0].trim().equals("Amplicon")) {
                    readingData = true;
                    headerLength = line.length;
                    screens = new Item[headerLength - 2];
                    for (int i = 2; i < line.length; i++) {
                        // create an array of screen item identifers (first two slots empty)
                        screens[i-2] = getScreen(line[i], "hits");
                        hitScreenNames.add(line[i]);
                    }
                }
            } else {
                if (line.length != headerLength) {
                    throw new RuntimeException("Incorrect number of entries in line: "
                                               + line + " (should be " + headerLength);
                }

                Set<Item> genes = new HashSet<Item>();

                String ampliconIdentifier = line[0].trim();
                Item amplicon = createItem("Amplicon");
                amplicon.setAttribute("identifier", ampliconIdentifier);
                amplicon.setReference("organism", organism);
                amplicon.addToCollection("evidence", dataSet);
                store(amplicon);

                newSynonym(ampliconIdentifier, amplicon, dataSource);

                // the amplicon may target zero or more genes, a gene can be targeted
                // by more than one amplicon.  
                if (!(line[1] == null || line[1].equals(""))) {
                    String [] geneNames = line[1].split(",");

                    for (int i = 0; i < geneNames.length; i++) {
                        String geneSymbol = geneNames[i].trim();
                        genes.add(newGene(geneSymbol));
                    }
                }

                // loop over screens to create results
                for (int j = 0; j < screens.length; j++) {
                    String resultValue = resultValues.get(line[j+2]);
                    if (resultValue == null) {
                        throw new RuntimeException("Unrecogised result symbol '" + line[j+2]
                            + "' in line: " + Arrays.asList(line));
                    }
                    if (genes.isEmpty()) {
                        // create a hit that doesn't reference a gene
                        Item screenHit = createItem("RNAiScreenHit");
                        screenHit.setReference("analysis", screens[j]);
                        
                        screenHit.setAttribute("result", resultValue);
                        screenHit.setReference("amplicon", amplicon);
                        store(screenHit);
                    } else {
                        // create one hit for each gene targeted
                        for (Item gene : genes) {
                            Item screenHit = createItem("RNAiScreenHit");
                            screenHit.setReference("analysis", screens[j]);
                            screenHit.setReference("gene", gene);
                            screenHit.setAttribute("result", resultValue);
                            screenHit.setReference("amplicon", amplicon);
                            store(screenHit);
                        }
                    }
                }
            }
        }
    }


    private void processScreenDetails(Reader reader) throws ObjectStoreException {
        Iterator tsvIter;
        try {
            tsvIter = TextFileUtil.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        while (tsvIter.hasNext()) {
            String [] line = (String[]) tsvIter.next();

            String pubmedId = line[0];
            Item publication = getPublication(pubmedId);

            String screenName = line[2];
            detailsScreenNames.add(line[2]);
            Item screen = getScreen(screenName, "screen details");
            screen.setAttribute("name", screenName);
            screen.setAttribute("cellLine", line[3]);
            screen.setAttribute("analysisDescription", line[4]);
            screen.setReference("organism", organism);
            screen.setReference("publication", publication);
            store(screen);
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
    private Item getScreen(String screenName, String fileName) {
        Item screen = screenMap.get(screenName);
        if (screen == null) {
            screen = createItem("RNAiScreen");
            screenMap.put(screenName, screen);
        }
        return screen;
    }


    /**
     * Convenience method to create a new gene Item
     * @param geneName the gene name
     * @return a new gene Item
     * @throws ObjectStoreException if an error occurs when storing the Item
     */
    protected Item newGene(String geneSymbol)  throws ObjectStoreException {
        if (geneSymbol == null) {
            throw new RuntimeException("geneSymbol can't be null");
        }
        Item item = genes.get(geneSymbol);
        if (item == null) {
            item = createItem("Gene");
            item.setAttribute("symbol", geneSymbol);
            item.setReference("organism", organism);
            genes.put(geneSymbol, item);
            store(item);
            // identifier needs to be a Synonym for quick search to work
            newSynonym(geneSymbol, item, dataSource);
        }
        return item;
    }

    /**
     * Convenience method to create and store a new synonym Item
     * @param synonymName the actual synonym
     * @param subject the synonym's subject item
     * @param source the source of the Synonym
     * @return a new synonym Item
     */
    protected Item newSynonym(String synonymName, Item subject, Item source) 
    throws ObjectStoreException {
        if (synonymName == null) {
            throw new RuntimeException("synonymName can't be null");
        }
        Item item = createItem("Synonym");
        item.setAttribute("value", synonymName);
        item.setAttribute("type", "identifier");
        item.setReference("subject", subject.getIdentifier());
        item.setReference("source", source.getIdentifier());
        store(item);
        return item;
    }
}

