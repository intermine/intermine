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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

/**
 * DataConverter to parse an FlyAtlas expression data into items
 * @author Richard Smith
 */
public class FlyAtlasConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    private Item expt, org, dataSet;
    private Map assays, ids = new HashMap();
    private ItemFactory itemFactory;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public FlyAtlasConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
        setupItems();
    }

    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = TextFileUtil.parseTabDelimitedReader(reader);
        String [] headers = null;
        int lineNo = 0;
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            if (lineNo == 0) {
                // column headers - strip off any extra columns
                int end = 0;
                for (int i = 0; i < line.length; i++) {
                    if (line[i] == null || line[i].equals("")) {
                        break;
                    }
                    end++;
                }
                headers = new String[end];
                System.arraycopy(line, 0, headers, 0, end);
            } else {
                String probeId = line[0];
                Item probe = createProbe(probeId);
                store(probe);
                // some rows have extra info on end, just look at length of headers
                int i = 1;
                for (i = 1; (i + 4) <= headers.length; i += 5) {
                    String col = headers[i];
                    col = col.replaceAll("\"", "");
                    String tissue = col.substring(0, col.indexOf(' ')).toLowerCase();
                    String[] results = new String[5];
                    System.arraycopy(line, i, results, 0, 5);
                    Item result = createFlyAtlasResult(probe, tissue, results);
                    store(result);
                }
                // whole fly data is in final three columns, doesn't have all values
                String tissue = headers[i];
                String[] results = new String[5];
                System.arraycopy(line, i, results, 1, 3);
                Item result = createFlyAtlasResult(probe, tissue, results);
                store(result);
            }
            lineNo++;
        }
    }

    /**
     * @see FileConverter#close()
     */
    public void close() throws ObjectStoreException {
        store(org);
        store(expt);
        store(assays.values());
        store(dataSet);
    }

    private String newId(String className) {
        Integer id = (Integer) ids.get(className);
        if (id == null) {
            id = new Integer(0);
            ids.put(className, id);
        }
        id = new Integer(id.intValue() + 1);
        ids.put(className, id);
        return id.toString();
    }


    private Item createProbe(String probeId) {
        Item probe = createItem("ProbeSet");
        probe.setAttribute("identifier", probeId);
        probe.setReference("organism", org.getIdentifier());
        return probe;
    }

    private Item createFlyAtlasResult(Item probe, String tissue, String[] results) {
        Item result = createItem("FlyAtlasResult");
        result.setReference("material", probe.getIdentifier());
        if (results[0] != null) {
            result.setAttribute("affyCall", results[0]);
        }
        result.setAttribute("MRNASignal", round(results[1], 2));
        result.setAttribute("MRNASignalSEM", round(results[2], 2));
        result.setAttribute("presentCall", results[3]);
        if (results[4] != null) {
            result.setAttribute("enrichment", round(results[4], 2));
        }
        result.setReference("source", dataSet.getIdentifier());

        // set assay
        if (!assays.containsKey(tissue)) {
            throw new IllegalArgumentException("Unrecognised tissue type read from file: '"
                                               + tissue + "'.");
        }
        result.setCollection("assays", new ArrayList(Collections.singleton(((Item)
                                               assays.get(tissue)).getIdentifier())));

        // set experiment
        result.setReference("experiment", expt.getIdentifier());

        return result;
    }

    /**
     * Set up the items that are common to all orthologues/paralogues
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected void setupItems() throws ObjectStoreException {
        org = createItem("Organism");
        org.setAttribute("taxonId", "7227");

        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "FlyAtlas");

        expt = createItem("MicroArrayExperiment");
        expt.setAttribute("name", "FlyAtlas: Gene expression in the adult fly.");
        expt.setAttribute("description", "Gene expression in the adult fly.  This dataset was"
                + " generated by Venkat Chintapalli, Jing Wang & Julian Dow at the University of"
                + " Glasgow with funding from the UK's BBSRC.  The dataset comprises 36 Affymetrix"
                + " Dros2 expression arrays, each mapping the expression of over 18500 transcripts"
                + " - the vast majority of known Drosophila genes. The starting material was"
                + " wild-type Oregon R adult flies, reared at 22C on a 12:12h light regime, on"
                + " standard Drosophila diet, 1 week after adult emergence. Tissues were dissected"
                + " out (from equal numbers of male and female flies) and pooled to make at least"
                + " 1500 ng mRNA, then amplified and hybridised using the Affymetrix standard"
                + " protocol. For each tissue, 4 independent biological replicates were obtained."
                + " Each array thus corresponds to one biological replicate.  The tissues chosen"
                + " were: brain, head (including brain), midgut, Malpighian tubule and hindgut"
                + " (including rectum). The reference sample is whole fly. The mean SIGNAL tells"
                + " how abundant the gene's mRNA is in each tissue - you could consider anything"
                + " over 100 as being abundant. The SEM value shows how consistent or variable"
                + " the answer is. The PRESENT call tells how many of the four arrays for each"
                + " sample actually gave a detectable expression, according to Affymetrix's GCOS"
                + " software. If you get an average signal of 2 and a present call of 0/4, you"
                + " shouldn't consider this gene to be truly expressed in that tissue. The RATIO"
                + " (enrichment) tells how much higher the signal is in a particular tissue than"
                + " in the whole fly, i.e. whether the gene is tissue-specific. This data is also"
                + " available at http://www.flyatlas.org.");


        assays = new HashMap();
        // names of assays from column headins, could be made more descriptive
        assays.put("brain", createAssay("Brain"));
        assays.put("head", createAssay("Head"));
        assays.put("midgut", createAssay("Midgut"));
        assays.put("hind", createAssay("Hindgut"));
        assays.put("tubule", createAssay("Tubule"));
        assays.put("ovary", createAssay("Ovary"));
        assays.put("testis", createAssay("Testis"));
        assays.put("FlyMean", createAssay("Whole Fly"));
        assays.put("acc", createAssay("Male accessory glands"));
        assays.put("lt", createAssay("Tubule (larval)"));
        assays.put("fb", createAssay("Fat Body (larval)"));
        assays.put("crop", createAssay("Crop"));
        assays.put("tag", createAssay("Thoracicoabdominal ganglion"));
        assays.put("car", createAssay("Adult carcass"));
    }

    private Item createAssay(String name) {
        Item assay = createItem("MicroArrayAssay");
        assay.setAttribute("name", name);
        return assay;
    }

    private String round(String num, int dp) {
        double d = Double.parseDouble(num);
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
        format.setMaximumFractionDigits(dp);
        format.setGroupingUsed(false);
        return format.format(d);
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item createItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + newId(className),
                                    GENOMIC_NS + className, "");
    }
}
