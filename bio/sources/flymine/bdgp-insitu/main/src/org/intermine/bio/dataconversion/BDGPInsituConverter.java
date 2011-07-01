package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter to parse BDGP insitu data file into Items.
 *
 * @author Julie Sullivan
 */
public class BDGPInsituConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(BDGPInsituConverter.class);

    private static final String URL
        = "http://www.fruitfly.org/insituimages/insitu_images/thumbnails/";
    private Map<String, Item> genes = new HashMap<String, Item>();
    private Map<String, Item> terms = new HashMap<String, Item>();
    private Map<String, Item> results = new HashMap<String, Item>();
    private Map<String, Item> imgs = new HashMap<String, Item>();

    protected Item orgDrosophila;
    private Item pub;
    private String[] stages;
    private String[] stageDescriptions;
//    private Set<String> badTerms;
    protected IdResolverFactory resolverFactory;
    private static final String TAXON_ID = "7227";
    private Item ontology = null;

    /**
     * Construct a new instance of BDGPInsituConverter.
     *
     * @param model the Model
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public BDGPInsituConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, "BDGP", "BDGP in situ data set");

        orgDrosophila = createItem("Organism");
        orgDrosophila.setAttribute("taxonId", TAXON_ID);
        store(orgDrosophila);

        pub = createItem("Publication");
        pub.setAttribute("pubMedId", "17645804");
        store(pub);

        stages = getStages();
        stageDescriptions = getStageDescriptions();
//        badTerms = getBadTerms();

        ontology = createItem("Ontology");
        ontology.setAttribute("name", "ImaGO");
        store(ontology);

        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    /**
     * Process the csv file
     * @param reader the Reader
     * @see DataConverter#process
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {

        Iterator<String[]> it = FormattedTextParser.parseTabDelimitedReader(reader);

        while (it.hasNext()) {

            String[] lineBits = it.next();
            String geneCG = lineBits[0];

            if (!geneCG.startsWith("CG")) {
                // ignore clones for now
                continue;
            }

            // Try to create/fetch gene, if null the IdResolver failed so do nothing for this row
            Item gene = getGene(geneCG);
            if (gene == null) {
                continue;
            }

            String stage = lineBits[1];

            String resultKey = geneCG + stage;
            Item result = getResult(resultKey, gene.getIdentifier(), pub.getIdentifier(), stage);

            if (lineBits.length > 2) {
                String image = lineBits[2];
                if (StringUtils.isNotEmpty(image)) {
                    setImage(result, URL + image);
                }
            }
            if (lineBits.length > 3) {
                String term = lineBits[3];
                Item termItem = getTerm(term);
                if (termItem != null) {
                    result.addToCollection("mRNAExpressionTerms", termItem);
                }
                if (term.equalsIgnoreCase("no staining")) {
                    result.setAttribute("expressed", "false");
                }
            }
        }

        for (Item result: results.values()) {
            if (result.getCollection("mRNAExpressionTerms").getRefIds().isEmpty()) {
                result.setAttribute("expressed", "false");
            }
        }

        storeAll(imgs);
        storeAll(results);

    }

    private void storeAll(Map<String, Item> map) throws ObjectStoreException {
        for (Item item: map.values()) {
            store(item);
        }
    }

    private Item getResult(String key, String geneId, String pubId, String stage)
    {
        if (results.containsKey(key)) {
            return results.get(key);
        }
        Item result = createItem("MRNAExpressionResult");
        result.setAttribute("expressed", "true");
        result.setReference("gene", geneId);
        result.setReference("publication", pubId);
        setTheStage(result, stage);
        result.setCollection("images", new ArrayList<String>());
        result.setCollection("mRNAExpressionTerms", new ArrayList<String>());
        results.put(key, result);
        return result;
    }

    private void setTheStage(Item result, String stage) {

        ReferenceList stagesColl = new ReferenceList("stages", new ArrayList<String>());

        Integer stageNumber = null;
        try {
            stageNumber = new Integer(stage);
        } catch (NumberFormatException e) {
            // bad line in file, just keep going
            return;
        }

        result.setAttribute("stageRange", stageDescriptions[stageNumber.intValue()]
                                                            + " (BDGP in situ)");
        switch (stageNumber.intValue()) {
            case 1:
                stagesColl.addRefId(stages[1]);
                stagesColl.addRefId(stages[2]);
                stagesColl.addRefId(stages[3]);
                break;
            case 2:
                stagesColl.addRefId(stages[4]);
                stagesColl.addRefId(stages[5]);
                stagesColl.addRefId(stages[6]);
                break;
            case 3:
                stagesColl.addRefId(stages[7]);
                stagesColl.addRefId(stages[8]);
                break;
            case 4:
                stagesColl.addRefId(stages[9]);
                stagesColl.addRefId(stages[10]);
                break;
            case 5:
                stagesColl.addRefId(stages[11]);
                stagesColl.addRefId(stages[12]);
                break;
            case 6:
                stagesColl.addRefId(stages[13]);
                stagesColl.addRefId(stages[14]);
                stagesColl.addRefId(stages[15]);
                stagesColl.addRefId(stages[16]);
                break;
            default:
                throw new IllegalArgumentException("bad stage value " + stageNumber.intValue());
        }

        result.addCollection(stagesColl);
    }

    private Item getTerm(String name) throws ObjectStoreException {
        if (!isValidTerm(name)) {
            return null;
        } else if (terms.containsKey(name)) {
            return terms.get(name);
        }
        Item termItem = createItem("OntologyTerm");
        termItem.setAttribute("name", name);
        termItem.setReference("ontology", ontology);
        store(termItem);
        terms.put(name, termItem);
        return termItem;

    }

    private Item getGene(String geneCG) throws ObjectStoreException {
        IdResolver resolver = resolverFactory.getIdResolver();
        int resCount = resolver.countResolutions(TAXON_ID, geneCG);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + geneCG + " count: " + resCount + " FBgn: "
                     + resolver.resolveId(TAXON_ID, geneCG));
            return null;
        }
        String primaryIdentifier = resolver.resolveId(TAXON_ID, geneCG).iterator().next();

        if (genes.containsKey(primaryIdentifier)) {
            return genes.get(primaryIdentifier);
        }
        Item gene = createItem("Gene");
        gene.setAttribute("primaryIdentifier", primaryIdentifier);
        gene.setReference("organism", orgDrosophila);
        genes.put(primaryIdentifier, gene);
        store(gene);
        return gene;
    }

    private void setImage(Item result, String img) {
        if (!imgs.containsKey(img)) {
            Item item = createItem("Image");
            item.setAttribute("url", img);
            imgs.put(img, item);
            result.addToCollection("images", item.getIdentifier());
        }
    }

    private String[] getStageDescriptions() {
        String[] stageLabels = new String[7];
        stageLabels[0] = "";
        stageLabels[1] = "stage 1-3";
        stageLabels[2] = "stage 4-6";
        stageLabels[3] = "stage 7-8";
        stageLabels[4] = "stage 9-10";
        stageLabels[5] = "stage 11-12";
        stageLabels[6] = "stage 13-16";
        return stageLabels;
    }

    private String[] getStages() throws ObjectStoreException {
        String[] stageItems = new String[17];
        Item item = createItem("Ontology");
        item.setAttribute("name", "Fly Development");
        store(item);
        for (int i = 1; i <= 16; i++) {
            Item stage = createItem("DevelopmentTerm");
            stage.setAttribute("name", "embryonic stage " + i);
            stage.setReference("ontology", item);
            stageItems[i] = stage.getIdentifier();
            store(stage);
        }
        return stageItems;
    }

    private boolean isValidTerm(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        if (name.contains("_")) {
            return false;
        }
        return true;
    }

/* These terms may not be in the updated file
    private Set<String> getBadTerms() {
        Set<String> forbiddenTerms = new HashSet<String>();
        forbiddenTerms.add("does_not_fit_array");
        forbiddenTerms.add("epi_combo");
        forbiddenTerms.add("flag_as_conflicting");
        forbiddenTerms.add("flag_as_incompleto");
        forbiddenTerms.add("flag_as_nonspecific");
        forbiddenTerms.add("flag_for_volker");
        forbiddenTerms.add("go_term");
        return forbiddenTerms;
    }
    */
}

