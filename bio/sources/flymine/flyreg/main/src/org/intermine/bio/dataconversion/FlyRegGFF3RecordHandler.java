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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
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
    protected IdResolverFactory resolverFactory;
    private static final String TAXON_ID = "7227";

    protected static final Logger LOG = Logger.getLogger(FlyRegGFF3RecordHandler.class);

    /**
     * Create a new FlyRegGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public FlyRegGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);
        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
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
            if (dbxref.startsWith("PMID:")) {
                pmid = dbxref.substring(5);
            } else {
                if (dbxref.startsWith("REDfly:")) {
                    redflyID = dbxref.substring(7);
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

        String factorGeneName = record.getAttributes().get("Factor").get(0);
        if (!("unknown").equals(factorGeneName.toLowerCase())
                    && !("unspecified").equals(factorGeneName.toLowerCase())) {
            Item gene = getGene(factorGeneName);
            if (gene != null) {
                bindingSite.setReference("factor", gene.getIdentifier());
            }
        }

        String targetGeneName = record.getAttributes().get("Target").get(0);

        if (!("unknown").equals(targetGeneName.toLowerCase())
                && !("unspecified").equals(targetGeneName.toLowerCase())) {
            Item gene = getGene(targetGeneName);
            if (gene != null) {
                bindingSite.setReference("gene", gene.getIdentifier());
            }
        }
    }

    private Item getGene(String symbol) {
        IdResolver resolver = resolverFactory.getIdResolver();
        int resCount = resolver.countResolutions(TAXON_ID, symbol);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + symbol + " count: " + resCount + " FBgn: "
                     + resolver.resolveId(TAXON_ID, symbol));
            return null;
        }
        String primaryIdentifier = resolver.resolveId(TAXON_ID, symbol).iterator().next();
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
