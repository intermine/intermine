package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

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
    private static final String TAXON_FLY = "7227";
    private Item ontology = null;
    protected IdResolver rslv;

    static final String[] STAGE_LABELS; static {
        STAGE_LABELS = new String[7];
        STAGE_LABELS[1] = "stage 1-3";
        STAGE_LABELS[2] = "stage 4-6";
        STAGE_LABELS[3] = "stage 7-8";
        STAGE_LABELS[4] = "stage 9-10";
        STAGE_LABELS[5] = "stage 11-12";
        STAGE_LABELS[6] = "stage 13-16";
    }

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
        orgDrosophila.setAttribute("taxonId", TAXON_FLY);
        store(orgDrosophila);

        pub = createItem("Publication");
        pub.setAttribute("pubMedId", "17645804");
        store(pub);

        setStages();

        ontology = createItem("Ontology");
        ontology.setAttribute("name", "ImaGO");
        store(ontology);
    }

    /**
     * Process the csv file
     * @param reader the Reader
     * @see DataConverter#process
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }

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

            Integer stageNumber = null;
            try {
                stageNumber = new Integer(stage);
            } catch (NumberFormatException e) {
                // bad line in file, just keep going
                continue;
            }
            result.setAttribute("stageRange", STAGE_LABELS[stageNumber.intValue()]
                                                                + " (BDGP in situ)");

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
                if ("no staining".equals(term)) {
                    result.setAttribute("expressed", "false");
                }
            }
        }

        for (Item result: results.values()) {
            if (!result.hasCollection("mRNAExpressionTerms")
                    || result.getCollection("mRNAExpressionTerms").getRefIds().isEmpty()) {
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

    private Item getResult(String key, String geneId, String pubId, String stage) {
        if (results.containsKey(key)) {
            return results.get(key);
        }
        Item result = createItem("MRNAExpressionResult");
        result.setAttribute("expressed", "true");
        result.setReference("gene", geneId);
        result.setReference("publication", pubId);
        result.setCollection("stages", getStages(stage));
//        result.setCollection("images", new ArrayList<String>());
//        result.setCollection("mRNAExpressionTerms", new ArrayList<String>());
        results.put(key, result);
        return result;
    }

    private List<String> getStages(String stage) {
        List<String> stagesColl = new ArrayList<String>();
        if ("1".equals(stage)) {
            stagesColl.add(stages[1]);
            stagesColl.add(stages[2]);
            stagesColl.add(stages[3]);
        } else if ("2".equals(stage)) {
            stagesColl.add(stages[4]);
            stagesColl.add(stages[5]);
            stagesColl.add(stages[6]);
        } else if ("3".equals(stage)) {
            stagesColl.add(stages[7]);
            stagesColl.add(stages[8]);
        } else if ("4".equals(stage)) {
            stagesColl.add(stages[9]);
            stagesColl.add(stages[10]);
        } else if ("5".equals(stage)) {
            stagesColl.add(stages[11]);
            stagesColl.add(stages[12]);
        } else if ("6".equals(stage)) {
            stagesColl.add(stages[13]);
            stagesColl.add(stages[14]);
            stagesColl.add(stages[15]);
            stagesColl.add(stages[16]);
        } else {
            throw new BuildException("bad stage value " + stage);
        }
        return stagesColl;
    }

    private Item getTerm(String name) throws ObjectStoreException {
        if (!isValidTerm(name)) {
            return null;
        } else if (terms.containsKey(name)) {
            return terms.get(name);
        }
        Item termItem = createItem("MRNAExpressionTerm");
        termItem.setAttribute("name", name);
        termItem.setReference("ontology", ontology);
        store(termItem);
        terms.put(name, termItem);
        return termItem;

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

    private void setStages() throws ObjectStoreException {
        Item item = createItem("Ontology");
        item.setAttribute("name", "Fly Development");
        store(item);
        stages = new String[17];
        for (int i = 1; i <= 16; i++) {
            Item stage = createItem("DevelopmentTerm");
            stage.setAttribute("name", "embryonic stage " + i);
            stage.setReference("ontology", item);
            stages[i] = stage.getIdentifier();
            store(stage);
        }
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

