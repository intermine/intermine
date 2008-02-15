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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

import java.net.URI;

/**
 * A converter/retriever for flyreg GFF3 files.
 *
 * @author Kim Rutherford
 */

public class FlyRegGFF3RecordHandler extends GFF3RecordHandler
{
    private final Map<String, Item> pubmedIdMap = new HashMap<String, Item>();
    private final Map<String, Item> geneIdMap = new HashMap<String, Item>();

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
        final URI nameSpace = getTargetModel().getNameSpace();
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

        bindingSite.setAttribute("secondaryIdentifier", primaryIdentifier);
        addSynonym(bindingSite, "identifier", primaryIdentifier);

        bindingSite.setAttribute("name", name);

        String geneNs = nameSpace + "Gene";
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

        bindingSite.setAttribute("primaryIdentifier", redflyID);
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

        addEvidence(pubmedItem);

        String factorGeneName = record.getAttributes().get("Factor").get(0);

        if (!factorGeneName.toLowerCase().equals("unknown")
            && !factorGeneName.toLowerCase().equals("unspecified")) {
            Item gene;

            if (geneIdMap.containsKey(factorGeneName)) {
                gene = geneIdMap.get(factorGeneName);
            } else {
                gene = getItemFactory().makeItemForClass(geneNs);
                geneIdMap.put(factorGeneName, gene);
                gene.setAttribute("symbol", factorGeneName);
                gene.setReference("organism", getOrganism().getIdentifier());
                addItem(gene);
            }

            bindingSite.setReference("factor", gene.getIdentifier());
        }

        String targetGeneName = record.getAttributes().get("Target").get(0);

        if (!targetGeneName.toLowerCase().equals("unknown")
            && !targetGeneName.toLowerCase().equals("unspecified")) {
            Item gene;

            if (geneIdMap.containsKey(targetGeneName)) {
                gene = geneIdMap.get(targetGeneName);
            } else {
                gene = getItemFactory().makeItemForClass(geneNs);
                geneIdMap.put(targetGeneName, gene);
                gene.setAttribute("symbol", targetGeneName);
                gene.setReference("organism", getOrganism().getIdentifier());
                addItem(gene);
            }

            bindingSite.setReference("gene", gene.getIdentifier());
        }
    }
}
