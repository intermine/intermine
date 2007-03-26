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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

import org.intermine.bio.dataconversion.GFF3RecordHandler;
import org.intermine.bio.io.gff3.GFF3Record;

/**
 * A converter/retriever for flyreg GFF3 files.
 *
 * @author Kim Rutherford
 */

public class FlyRegGFF3RecordHandler extends GFF3RecordHandler
{
    private final Map pubmedIdMap = new HashMap();
    private final Map geneIdMap = new HashMap();

    /**
     * Create a new FlyRegGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public FlyRegGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);
    }

    /**
     * @see GFF3RecordHandler#process()
     */
    public void process(GFF3Record record) {
        String geneNs = getTargetModel().getNameSpace() + "Gene";
        String publicationNs = getTargetModel().getNameSpace() + "Publication";

        String pmid = (String) ((List) record.getAttributes().get("PMID")).get(0);
        Item pubmedItem;

        if (pubmedIdMap.containsKey(pmid)) {
            pubmedItem = (Item) pubmedIdMap.get(pmid);
        } else {
            pubmedItem = getItemFactory().makeItemForClass(publicationNs);
            pubmedIdMap.put(pmid, pubmedItem);
            pubmedItem.setAttribute("pubMedId", pmid);
            addItem(pubmedItem);
        }

        Item bindingSite = getFeature();

        addEvidence(pubmedItem);

        String factorGeneName = (String) ((List) record.getAttributes().get("Factor")).get(0);

        if (!factorGeneName.toLowerCase().equals("unknown")
            && !factorGeneName.toLowerCase().equals("unspecified")) {
            Item gene;

            if (geneIdMap.containsKey(factorGeneName)) {
                gene = (Item) geneIdMap.get(factorGeneName);
            } else {
                gene = getItemFactory().makeItemForClass(geneNs);
                geneIdMap.put(factorGeneName, gene);
                gene.setAttribute("symbol", factorGeneName);
                gene.setReference("organism", getOrganism().getIdentifier());
                addItem(gene);
            }

            bindingSite.setReference("factor", gene.getIdentifier());
        }

        String targetGeneName = (String) ((List) record.getAttributes().get("Target")).get(0);

        if (!targetGeneName.toLowerCase().equals("unknown")
            && !targetGeneName.toLowerCase().equals("unspecified")) {
            Item gene;

            if (geneIdMap.containsKey(targetGeneName)) {
                gene = (Item) geneIdMap.get(targetGeneName);
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
