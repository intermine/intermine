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

import java.util.HashMap;
import java.util.Map;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

import java.io.BufferedReader;
import java.io.Reader;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * DataConverter to parse Fly-FISH data file into Items.
 *
 * @author Kim Rutherford
 */
public class FlyFishConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(FlyFishConverter.class);

    private Map<String, Item> geneItems = new HashMap<String, Item>();
    private Map<String, Item> termItems = new HashMap<String, Item>();

    Item orgDrosophila;
    private Item dataSet;
    private Item pub;

    /**
     * Construct a new instance of HomophilaCnoverter.
     *
     * @param model the Model
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public FlyFishConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);

        orgDrosophila = createItem("Organism");
        orgDrosophila.addAttribute(new Attribute("taxonId", "7227"));
        store(orgDrosophila);

        // TODO these need to be re-added
//        dataSet = createItem("DataSet");
//        dataSet.addAttribute(new Attribute("title", "Fly-FISH"));
//        store(dataSet);

//        pub = createItem("Publication");
//        pub.addAttribute(new Attribute("pubMedId", "17923096"));
//        store(pub);
    }

    private class HeaderConfig
    {
        int stage;
        String localisation;
    }

    /**
     * Process the results matrix from Fly-FISH.
     * @param reader the Reader
     * @see DataConverter#process
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        String headerArray[] = StringUtils.split(line, ';');

        HeaderConfig[] config = new HeaderConfig[headerArray.length];
        for (int i = 0; i < headerArray.length; i++) {
            String thisHeader = headerArray[i];
            char stageTag = thisHeader.charAt(0);
            if (stageTag < '1' || stageTag > '5') {
                throw new RuntimeException("unknown stage " + stageTag + " in header of "
                                           + getCurrentFile());
            }
            if (stageTag == '5') {
                // ignore "stage10-"
                continue;
            }
            if (thisHeader.charAt(1) != ' ') {
                throw new RuntimeException("parse error in header of " + getCurrentFile());
            }
            String localisation = thisHeader.substring(2);
            config[i] = new HeaderConfig();
            config[i].stage = Integer.parseInt(String.valueOf(stageTag));
            config[i].localisation = localisation;
        }
        while ((line = br.readLine()) != null) {
            String lineBits[] = StringUtils.split(line, ';');
            String geneCG = lineBits[0];
            Item gene = getGene(geneCG);
            Item mRNALocalisationResults[] = new Item[4];
            for (int stageNum = 1; stageNum <= 4; stageNum++) {
                Item result = createItem("MRNALocalisationResult");
                result.setReference("gene", gene);
                mRNALocalisationResults[stageNum - 1] = result;
                if (stageNum == 1) {
                    result.setAttribute("stage", "stage 1-3");
                } else {
                    if (stageNum == 2) {
                        result.setAttribute("stage", "stage 4-5");
                    } else {
                        if (stageNum == 3) {
                            result.setAttribute("stage", "stage 6-7");
                        } else {
                            result.setAttribute("stage", "stage 8-9");
                        }
                    }
                }
            }

            for (int column = 1; column < lineBits.length; column++) {
                String value = lineBits[column];
                if (value.equals("1")) {
                    int configIndex = column - 1;
                    if (configIndex >= config.length) {
                        throw new RuntimeException("line too long: " + line);
                    }
                    HeaderConfig hc = config[configIndex];
                    if (hc == null) {
                        // we ignore stage10- results
                        continue;
                    }
                    String localisation = hc.localisation;
                    Item localisationTerm = getMRNALocalisationTerm(localisation);
                    Item result = mRNALocalisationResults[hc.stage - 1];
                    result.addToCollection("mRNALocalisationTerms", localisationTerm);
                }
            }

            for (Item result: mRNALocalisationResults) {
                ReferenceList resultTerms = result.getCollection("mRNALocalisationTerms");
                if (resultTerms == null || resultTerms.getRefIds().size() == 0) {
                    result.setAttribute("expressed", "false");
                } else {
                    result.setAttribute("expressed", "true");
                }
                store(result);
            }
        }
    }

    /**
     * @param localisation
     * @return
     * @throws ObjectStoreException
     */
    private Item getMRNALocalisationTerm(String localisation) throws ObjectStoreException {
        if (termItems.containsKey(localisation)) {
            return termItems.get(localisation);
        } else {
            Item localisationTermItem = createItem("MRNALocalisationTerm");
            localisationTermItem.setAttribute("localisation", localisation);
            store(localisationTermItem);
            termItems.put(localisation, localisationTermItem);
            return localisationTermItem;
        }
    }

    private Item getGene(String geneCG) throws ObjectStoreException {
        if (geneItems.containsKey(geneCG)) {
            return geneItems.get(geneCG);
        } else {
            Item gene = createItem("Gene");
            gene.setAttribute("identifier", geneCG);
            gene.setReference("organism", orgDrosophila);
            geneItems.put(geneCG, gene);
            store(gene);
            return gene;
        }
    }
}

