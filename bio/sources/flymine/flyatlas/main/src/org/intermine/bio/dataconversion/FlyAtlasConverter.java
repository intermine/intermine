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

import java.io.Reader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * DataConverter to parse FlyAtlas expression data into items
 * @author Richard Smith
 */
public class FlyAtlasConverter extends BioFileConverter
{
    private Item expt, org;
    protected Map<String, Item> tissues = new HashMap<String, Item>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the target data model
     */
    public FlyAtlasConverter(ItemWriter writer, Model model) {
        super(writer, model, "University of Glasgow", "FlyAtlas", null);
        setupItems();
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        String [] headers = null;
        int lineNo = 0;
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            if (lineNo == 0) {
                // column headers - strip off any extra columns
                int end = 0;
                for (int i = 0; i < line.length; i++) {
                    if (StringUtils.isEmpty(line[i])) {
                        break;
                    }
                    end++;
                }
                headers = new String[end];
                System.arraycopy(line, 0, headers, 0, end);
            } else {
                String probeId = line[0];
                // there seems to be some empty lines at the end of the file
                if (StringUtils.isEmpty(probeId)) {
                    break;
                }
                Item probe = createProbe(probeId);
                store(probe);
                // some rows have extra info on end, just look at length of headers
                int i = 1;
                for (i = 1; (i + 4) <= headers.length; i += 5) {
                    String col = headers[i];
                    col = col.replaceAll("\"", "");

                    String tissue = null;

                    /* get the tissue name from the header, using the part before the "vs", eg:
                     * "larvae hindgut vs whole fly  - T-Test_Change Direction"
                     * there are two headers without the vs, just use space as the delimiter.
                     * We can't always use ' ' to identify the tissue because there are duplicates.
                     * */
                    if (col.contains(" vs ")) {
                        tissue = col.substring(0, col.indexOf(" vs ")).toLowerCase();
                    } else {
                        tissue = col.substring(0, col.indexOf(' ')).toLowerCase();
                    }
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
     * {@inheritDoc}
     */
    public void close() throws ObjectStoreException {
        store(org);
        store(expt);
        store(tissues.values());
    }

    private Item createProbe(String probeId) {
        Item probe = createItem("ProbeSet");
        probe.setAttribute("primaryIdentifier", probeId);
        probe.setReference("organism", org.getIdentifier());
        return probe;
    }

    private Item createFlyAtlasResult(Item probe, String tissue, String[] results) {
        Item result = createItem("FlyAtlasResult");
        result.setReference("material", probe.getIdentifier());
        if (results[0] != null) {
            result.setAttribute("affyCall", results[0]);
        }
        result.setAttribute("mRNASignal", round(results[1], 2));
        result.setAttribute("mRNASignalSEM", round(results[2], 2));
        result.setAttribute("presentCall", results[3]);
        if (results[4] != null) {
            result.setAttribute("enrichment", round(results[4], 2));
        }

        // set tissue
        if (!tissues.containsKey(tissue)) {
            throw new IllegalArgumentException("Unrecognised tissue type read from file: '"
                                               + tissue + "'.");
        }
        result.setReference("tissue", tissues.get(tissue).getIdentifier());

        // set experiment
        result.setReference("experiment", expt.getIdentifier());

        return result;
    }

    /**
     * Set up the items that are common to all orthologues/paralogues
     */
    protected void setupItems() {
        org = createItem("Organism");
        org.setAttribute("taxonId", "7227");

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


        tissues = new HashMap<String, Item>();
        // names of tissues from column headings, could be made more descriptive
        tissues.put("brain", createTissue("Brain"));
        tissues.put("head", createTissue("Head"));
        tissues.put("crop", createTissue("Crop"));
        tissues.put("midgut", createTissue("Midgut"));
        tissues.put("hind", createTissue("Hindgut"));
        tissues.put("tubule t test", createTissue("Tubule"));
        tissues.put("ovary", createTissue("Ovary"));
        tissues.put("testis", createTissue("Testis"));
        tissues.put("acc", createTissue("Male accessory glands"));
        tissues.put("lt", createTissue("Larval tubule"));
        tissues.put("fb", createTissue("Larval fat body"));
        tissues.put("tag", createTissue("Thoracicoabdominal ganglion"));
        tissues.put("car", createTissue("Adult carcass"));
        tissues.put("sg", createTissue("Salivary gland"));
        tissues.put("l_sg", createTissue("Larval salivary gland"));
        tissues.put("l_mid", createTissue("Larval midgut"));

        tissues.put("larvae hindgut", createTissue("Larvae hindgut"));
        tissues.put("sptv", createTissue("Virgin spermatheca"));
        tissues.put("sptm", createTissue("Mated spermatheca"));

        // is this used?
        tissues.put("FlyMean", createTissue("Whole Fly"));

        // new 2009-05-19
        tissues.put("feeded larvae central nerve system", createTissue("Larval CNS"));
        tissues.put("adult fat body", createTissue("Adult fat body"));
        tissues.put("feeded larvae carcuss", createTissue("Larval carcass"));
        tissues.put("eye", createTissue("Adult eye"));
        tissues.put("heart", createTissue("Adult heart"));
        tissues.put("lftrachea", createTissue("Larval trachea"));
        tissues.put("drosophila s2 cells", createTissue("S2 cells"));

    }

    private Item createTissue(String name) {
        Item tissue = createItem("Tissue");
        tissue.setAttribute("name", name);
        return tissue;
    }

    private String round(String num, int dp) {
        double d = Double.parseDouble(num);
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
        format.setMaximumFractionDigits(dp);
        format.setGroupingUsed(false);
        return format.format(d);
    }
}
