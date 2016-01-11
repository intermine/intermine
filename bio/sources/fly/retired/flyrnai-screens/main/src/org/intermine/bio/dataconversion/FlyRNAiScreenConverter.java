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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
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

/**
 * DataConverter to create items from DRSC RNAi screen date files.
 * NOTE: RETIRED
 *
 * @author Kim Rutherford
 * @author Richard Smith
 */
public class FlyRNAiScreenConverter extends BioFileConverter
{
    protected Item organism, hfaSource;

    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> publications = new HashMap<String, String>();
    private Map<String, String> screenMap = new HashMap<String, String>();
    private static final String TAXON_ID = "7227";
    private File screenDetailsFile;
    private String[] hitScreenNames;
    protected IdResolver rslv;

    protected static final Logger LOG = Logger.getLogger(FlyRNAiScreenConverter.class);
    /**
     * Create a new FlyRNAiScreenConverter object.
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public FlyRNAiScreenConverter(ItemWriter writer, Model model) {
        super(writer, model, "DRSC", "DRSC data set");
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
     * Set the screen details input file. This file contains the details for each screen, only hits
     * from screens in this file will be processed
     *
     * @param screenDetailsFile screen input file
     */
    public void setScreenDetailsFile(File screenDetailsFile) {
        this.screenDetailsFile = screenDetailsFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }
        if (organism == null) {
            organism = createItem("Organism");
            organism.setAttribute("taxonId", TAXON_ID);
            store(organism);
        }
        try {
            readScreenDetails(new FileReader(screenDetailsFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading screen details", err);
        }
        processHits(reader);
    }

    private void processHits(Reader reader)
        throws ObjectStoreException {

        boolean readingData = false;
        int headerLength = 0;
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
                    hitScreenNames = new String[headerLength];
                    for (int i = 2; i < line.length; i++) {
                        String screenName = line[i].trim();
                        if (!StringUtils.isEmpty(screenName)) {
                            hitScreenNames[i] = screenName;
                        }
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

                Set<String> ampliconGenes = new LinkedHashSet<String>();

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
                        String geneRefId = getGene(geneSymbol);
                        if (geneRefId != null) {
                            ampliconGenes.add(geneRefId);
                            amplicon.addToCollection("genes", geneRefId);
                        }
                    }
                }

                // column 0 : screen identifier
                // column 1 : genes
                // loop over screens to create results
                for (int i = 2; i < hitScreenNames.length; i++) {
                    String resultValue = RESULTS_KEY.get(line[i].trim());
                    String screenName = hitScreenNames[i];
                    if (resultValue == null) {
                        throw new RuntimeException("Unrecogised result symbol '" + line[i]
                            + "' in line: " + Arrays.asList(line));
                    }
                    if (screenMap.get(screenName) == null) {
                        // we don't have details for all screens
                        continue;
                    }
                    if (genes.isEmpty()) {
                        // create a hit that doesn't reference a gene
                        storeScreen(screenName, amplicon.getIdentifier(), resultValue, null);
                    } else {
                        // create one hit for each gene targeted
                        for (String geneRefId : ampliconGenes) {
                            storeScreen(screenName, amplicon.getIdentifier(), resultValue,
                                    geneRefId);
                        }
                    }
                }
                store(amplicon);
            }
        }
    }

    private void storeScreen(String screenName, String amplicon, String resultValue,
            String geneRefId) throws ObjectStoreException {
        Item screenHit = createItem("RNAiScreenHit");
        if (geneRefId != null) {
            screenHit.setReference("gene", geneRefId);
        }
        screenHit.setAttribute("result", resultValue);
        screenHit.setReference("pcrProduct", amplicon);
        screenHit.setReference("rnaiScreen", screenMap.get(screenName));
        store(screenHit);
    }

    private void readScreenDetails(Reader reader) throws ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        while (tsvIter.hasNext()) {
            String [] line = (String[]) tsvIter.next();

            if (line.length != 3) {
                throw new RuntimeException("Did not find three elements in line, found "
                          + line.length + ": " + Arrays.asList(line));
            }
            String pubmedId = line[0].trim();

            if ("Pubmed_ID".equals(pubmedId)) {
                // skip header
                continue;
            }
            String publicationRefId = getPublication(pubmedId);

            String screenName = line[2].trim();
            if (StringUtils.isEmpty(screenName)) {
                continue;
            }
            Item screen = createItem("RNAiScreen");
            screen.setAttribute("name", screenName);
            screen.setReference("organism", organism);
            screen.setReference("publication", publicationRefId);
            store(screen);
            screenMap.put(screenName, screen.getIdentifier());
        }
    }

    private String getPublication(String pubmedId) throws ObjectStoreException {
        String refId = publications.get(pubmedId);
        if (refId == null) {
            Item publication = createItem("Publication");
            publication.setAttribute("pubMedId", pubmedId);
            refId = publication.getIdentifier();
            publications.put(pubmedId, refId);
            store(publication);
        }
        return refId;
    }

    private String getGene(String geneSymbol) throws ObjectStoreException {
        if (geneSymbol == null) {
            throw new RuntimeException("geneSymbol can't be null");
        }
        if (rslv == null || !rslv.hasTaxon(TAXON_ID)) {
            return null;
        }
        int resCount = rslv.countResolutions(TAXON_ID, geneSymbol);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + geneSymbol + " count: " + resCount + " FBgn: "
                     + rslv.resolveId(TAXON_ID, geneSymbol));
            return null;
        }
        String primaryIdentifier = rslv.resolveId(TAXON_ID, geneSymbol).iterator().next();
        String refId = genes.get(primaryIdentifier);
        if (refId == null) {
            Item item = createItem("Gene");
            item.setAttribute("primaryIdentifier", primaryIdentifier);
            item.setReference("organism", organism);
            refId = item.getIdentifier();
            store(item);
            genes.put(primaryIdentifier, refId);
        }
        return refId;
    }
}
