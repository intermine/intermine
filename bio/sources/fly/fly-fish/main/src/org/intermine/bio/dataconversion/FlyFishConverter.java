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
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter to parse Fly-FISH data file into Items.
 *
 * @author Kim Rutherford
 */
public class FlyFishConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(FlyFishConverter.class);

    private Map<String, Item> geneItems = new HashMap<String, Item>();
    private Map<String, Item> termItems = new HashMap<String, Item>();

    Item orgDrosophila;
    private Item pub, ontology, devOntology;
    private String[] stages;
    private static final String TAXON_FLY = "7227";
    protected IdResolver rslv;

    /**
     * Construct a new instance of flyfishconverter.
     *
     * @param model the Model
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public FlyFishConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model, "fly-FISH", "fly-Fish data set");

        orgDrosophila = createItem("Organism");
        orgDrosophila.addAttribute(new Attribute("taxonId", TAXON_FLY));
        store(orgDrosophila);

        pub = createItem("Publication");
        pub.setAttribute("pubMedId", "17923096");
        store(pub);

        ontology = createItem("Ontology");
        ontology.setAttribute("name", "ImaGO");
        store(ontology);

        devOntology = createItem("Ontology");
        devOntology.setAttribute("name", "Fly Development");
        store(devOntology);

        stages = getStages();
    }

    private class HeaderConfig
    {
        int stage;
        String expression;
    }

    /**
     * Process the results matrix from Fly-FISH.
     * @param reader the Reader
     * @see DataConverter#process
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        String[] headerArray = StringUtils.split(line, ';');

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
            String expression = thisHeader.substring(2);
            config[i] = new HeaderConfig();
            config[i].stage = Integer.parseInt(String.valueOf(stageTag));
            config[i].expression = expression;
        }
        while ((line = br.readLine()) != null) {
            String[] lineBits = StringUtils.split(line, ';');
            String geneCG = lineBits[0];
            Item gene = getGene(geneCG);

            // try to create a gene from this CG, if not resolved to a single primaryIdentifier
            // the ignore this line of input
            if (gene == null) {
                continue;
            }

            Item[] mRNAExpressionResults = new Item[4];
            for (int stageNum = 1; stageNum <= 4; stageNum++) {

                Item result = createItem("MRNAExpressionResult");
                result.setReference("gene", gene);
                result.setReference("publication", pub);
                ReferenceList mRNAExpressionTerms = new ReferenceList("mRNAExpressionTerms",
                                                                      new ArrayList<String>());
                result.addCollection(mRNAExpressionTerms);
                ReferenceList stagesColl = new ReferenceList("stages", new ArrayList<String>());
                mRNAExpressionResults[stageNum - 1] = result;
                if (stageNum == 1) {
                    result.setAttribute("stageRange", "stage 1-3 (fly-FISH)");
                    stagesColl.addRefId(stages[1]);
                    stagesColl.addRefId(stages[2]);
                    stagesColl.addRefId(stages[3]);
                } else {
                    if (stageNum == 2) {
                        result.setAttribute("stageRange", "stage 4-5 (fly-FISH)");
                        stagesColl.addRefId(stages[4]);
                        stagesColl.addRefId(stages[5]);
                    } else {
                        if (stageNum == 3) {
                            result.setAttribute("stageRange", "stage 6-7 (fly-FISH)");
                            stagesColl.addRefId(stages[6]);
                            stagesColl.addRefId(stages[7]);
                        } else {
                            result.setAttribute("stageRange", "stage 8-9 (fly-FISH)");
                            stagesColl.addRefId(stages[8]);
                            stagesColl.addRefId(stages[9]);
                        }
                    }
                }
                result.addCollection(stagesColl);
            }

            for (int column = 1; column < lineBits.length; column++) {
                String value = lineBits[column];
                if ("1".equals(value)) {
                    int configIndex = column - 1;
                    if (configIndex >= config.length) {
                        throw new RuntimeException("line too long: " + line);
                    }
                    HeaderConfig hc = config[configIndex];
                    if (hc == null) {
                        // we ignore stage10- results
                        continue;
                    }
                    Item term = getMRNAExpressionTerm(hc.expression);
                    term.setAttribute("type", "fly-FISH");
                    Item result = mRNAExpressionResults[hc.stage - 1];
                    result.addToCollection("mRNAExpressionTerms", term);
                }
            }

            for (Item result: mRNAExpressionResults) {
                ReferenceList resultTerms = result.getCollection("mRNAExpressionTerms");
                if (resultTerms == null || resultTerms.getRefIds().size() == 0) {
                    result.setAttribute("expressed", "false");
                    Item nonExpressedTerm = getMRNAExpressionTerm("Non-expressed");
                    result.addToCollection("mRNAExpressionTerms", nonExpressedTerm);
                } else {
                    result.setAttribute("expressed", "true");
                }
                store(result);
            }
        }
    }

    /**
     * @param expression
     * @return expression term
     * @throws ObjectStoreException
     */
    private Item getMRNAExpressionTerm(String expression) throws ObjectStoreException {
        String name = expression.toLowerCase();
        if (termItems.containsKey(name)) {
            return termItems.get(name);
        }
        Item term = createItem("MRNAExpressionTerm");
        term.setAttribute("name", name);
        term.setReference("ontology", ontology);
        store(term);
        termItems.put(name, term);
        return term;

    }

    private Item getGene(String geneCG) throws ObjectStoreException {
        if (rslv == null || !rslv.hasTaxon(TAXON_FLY)) {
            return null;
        }
        int resCount = rslv.countResolutions(TAXON_FLY, geneCG);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + geneCG + " count: " + resCount + " FBgn: "
                     + rslv.resolveId(TAXON_FLY, geneCG));
            return null;
        }
        String primaryIdentifier = rslv.resolveId(TAXON_FLY, geneCG).iterator().next();
        if (geneItems.containsKey(primaryIdentifier)) {
            return geneItems.get(primaryIdentifier);
        }
        Item gene = createItem("Gene");
        gene.setAttribute("primaryIdentifier", primaryIdentifier);
        gene.setReference("organism", orgDrosophila);
        geneItems.put(primaryIdentifier, gene);
        store(gene);
        return gene;

    }

    private String[] getStages() throws ObjectStoreException {
        String[] stageItems = new String[17];
        for (int i = 1; i <= 16; i++) {
            Item stage = createItem("DevelopmentTerm");
            stage.setAttribute("name", "embryonic stage " + i);
            stage.setReference("ontology", devOntology);
            stageItems[i] = stage.getIdentifier();
            store(stage);
        }
        return stageItems;
    }
}

