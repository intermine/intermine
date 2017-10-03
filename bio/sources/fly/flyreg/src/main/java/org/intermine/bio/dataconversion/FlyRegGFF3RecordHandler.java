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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for flyreg GFF3 files.
 *
 * @author Kim Rutherford
 */

public class FlyRegGFF3RecordHandler extends GFF3RecordHandler
{
    private final Map<String, Item> pubmedIdMap = new HashMap<String, Item>();
    private final Map<String, Item> geneIdMap = new HashMap<String, Item>();
    private static final String TAXON_FLY = "7227";
    protected IdResolver rslv;

    protected static final Logger LOG = Logger.getLogger(FlyRegGFF3RecordHandler.class);

    /**
     * Create a new FlyRegGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public FlyRegGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }

        getFeature().setClassName("TFBindingSite");

        Item bindingSite = getFeature();

        String name = record.getId();

        Pattern p = Pattern.compile(".*:REDFLY:(.*)");
        Matcher m = p.matcher(name);

        if (!m.matches()) {
            LOG.warn("Binding site identifier didn't match pattern: " + name);
            bindingSite.setAttribute("primaryIdentifier", name);
        } else  {
            bindingSite.setAttribute("primaryIdentifier", m.group(1));
        }
        bindingSite.setAttribute("name", name);

        if (record.getAttributes().containsKey("Evidence")) {
            List<String> evidenceList = record.getAttributes().get("Evidence");
            String elementEvidence = evidenceList.get(0);
            bindingSite.setAttribute("evidenceMethod", elementEvidence);
        }

        List<String> dbxrefs = record.getAttributes().get("Dbxref");

        String redflyID = null;
        String pmid = null;

        for (String dbxref: dbxrefs) {
            // NB: format changed, the individual refs in DBxref used to be quoted, now they just
            // have quotes around the whole lot.
            if (dbxref.contains(",")) {
                List<String> refList = new ArrayList<String>(
                        Arrays.asList(StringUtil.split(dbxref, ",")));
                for (String ref : refList) {
                    ref = ref.trim();

                    int colonIndex = ref.indexOf(":");
                    if (colonIndex == -1) {
                        throw new RuntimeException("external reference not understood: " + ref);
                    }

                    if (ref.startsWith("PMID:")) {
                        pmid = ref.substring(colonIndex + 1);
                    } else {
                        if (ref.startsWith("REDfly:")) {
                            redflyID = ref.substring(colonIndex + 1);
                        }
                    }
                }
            } else if (dbxref.startsWith("PMID:")) {
                pmid = dbxref.substring(dbxref.indexOf(":") + 1);
            } else {
                if (dbxref.startsWith("REDfly:")) {
                    redflyID = dbxref.substring(dbxref.indexOf(":") + 1);
                }
            }
        }

        if (pmid == null) {
            throw new RuntimeException("no pubmed id for: " + bindingSite);
        }

        if (redflyID == null) {
            throw new RuntimeException("no REDfly: id for: " + bindingSite);
        }

        bindingSite.setAttribute("secondaryIdentifier", redflyID);
        Item pubmedItem;
        if (pubmedIdMap.containsKey(pmid)) {
            pubmedItem = pubmedIdMap.get(pmid);
        } else {
            pubmedItem = converter.createItem("Publication");
            pubmedIdMap.put(pmid, pubmedItem);
            pubmedItem.setAttribute("pubMedId", pmid);
            addItem(pubmedItem);
        }

        addPublication(pubmedItem);

        String factorGeneName = record.getAttributes().get("Factor") == null ? record
                .getAttributes().get("factor").get(0)
                : record.getAttributes().get("Factor").get(0);
        if (factorGeneName.contains(":")) {
            int colonIndex = factorGeneName.lastIndexOf(":");
            factorGeneName = factorGeneName.substring(colonIndex + 1);
        }

        if (!("unknown").equals(factorGeneName.toLowerCase())
                    && !("unspecified").equals(factorGeneName.toLowerCase())) {
            Item gene = getGene(factorGeneName);
            if (gene != null) {
                bindingSite.setReference("factor", gene.getIdentifier());
            }
        }

        String targetGeneName = record.getAttributes().get("Target") == null ? record
                .getAttributes().get("target").get(0)
                : record.getAttributes().get("Target").get(0);
        if (targetGeneName.contains(":")) {
            int colonIndex = targetGeneName.lastIndexOf(":");
            targetGeneName = targetGeneName.substring(colonIndex + 1);
        }

        if (!("unknown").equals(targetGeneName.toLowerCase())
                && !("unspecified").equals(targetGeneName.toLowerCase())) {
            Item gene = getGene(targetGeneName);
            if (gene != null) {
                bindingSite.setReference("gene", gene.getIdentifier());
            }
        }
    }

    private Item getGene(String symbol) {
        if (rslv == null || !rslv.hasTaxon(TAXON_FLY)) {
            return null;
        }
        int resCount = rslv.countResolutions(TAXON_FLY, symbol);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + symbol + " count: " + resCount + " FBgn: "
                     + rslv.resolveId(TAXON_FLY, symbol));
            return null;
        }
        String primaryIdentifier = rslv.resolveId(TAXON_FLY, symbol).iterator().next();
        Item gene = geneIdMap.get(primaryIdentifier);
        if (gene == null) {
            gene = converter.createItem("Gene");
            geneIdMap.put(primaryIdentifier, gene);
            gene.setAttribute("primaryIdentifier", primaryIdentifier);
            gene.setReference("organism", getOrganism());
            addItem(gene);
        }
        return gene;
    }
}
