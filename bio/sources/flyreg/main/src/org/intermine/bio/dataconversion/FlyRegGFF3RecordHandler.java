package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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
    private URI nameSpace;
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
        nameSpace = getTargetModel().getNameSpace();
        getFeature().setClassName(nameSpace + "TFBindingSite");

        Item bindingSite = getFeature();

        String name = record.getId();

        Pattern p = Pattern.compile(".*:REDFLY:(.*)");
        Matcher m = p.matcher(name);

        String primaryIdentifier = null;

        if (m.matches()) {
            primaryIdentifier = m.group(1);
        } else {
            throw new RuntimeException("can't find identifier in " + name
                                       + " - pattern doesn't match");
        }

        bindingSite.setAttribute("primaryIdentifier", primaryIdentifier);
        addSynonym(bindingSite, "identifier", primaryIdentifier);

        bindingSite.setAttribute("name", name);

        if (record.getAttributes().containsKey("Evidence")) {
            List<String> evidenceList = record.getAttributes().get("Evidence");
            String elementEvidence = evidenceList.get(0);
            bindingSite.setAttribute("evidenceMethod", elementEvidence);
        }

        String publicationNs = nameSpace + "Publication";

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
        addSynonym(bindingSite, "internal_id", "REDfly:" + redflyID);

        Item pubmedItem;

        if (pubmedIdMap.containsKey(pmid)) {
            pubmedItem = pubmedIdMap.get(pmid);
        } else {
            pubmedItem = getItemFactory().makeItemForClass(publicationNs);
            pubmedIdMap.put(pmid, pubmedItem);
            pubmedItem.setAttribute("pubMedId", pmid);
            addItem(pubmedItem);
        }

        addPublication(pubmedItem);

        String factorGeneName = record.getAttributes().get("Factor").get(0);

        if (!factorGeneName.toLowerCase().equals("unknown")
            && !factorGeneName.toLowerCase().equals("unspecified")) {
            Item gene = getGene(factorGeneName);
            if (gene != null) {
                bindingSite.setReference("factor", gene.getIdentifier());
            }
        }

        String targetGeneName = record.getAttributes().get("Target").get(0);

        if (!targetGeneName.toLowerCase().equals("unknown")
            && !targetGeneName.toLowerCase().equals("unspecified")) {
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
            gene = getItemFactory().makeItemForClass(nameSpace + "Gene");
            geneIdMap.put(primaryIdentifier, gene);
            gene.setAttribute("primaryIdentifier", primaryIdentifier);
            gene.setReference("organism", getOrganism().getIdentifier());
            gene.setCollection("dataSets",
                               new ArrayList(Collections.singleton(getDataSet().getIdentifier())));
            addItem(gene);
        }
        return gene;
    }
}
