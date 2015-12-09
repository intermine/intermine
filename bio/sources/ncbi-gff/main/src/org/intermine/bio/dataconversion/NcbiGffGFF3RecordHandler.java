package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the NcbiGff dataset via GFF files.
 * @author julie
 */

public class NcbiGffGFF3RecordHandler extends GFF3RecordHandler
{

    /**
     * Create a new NcbiGffGFF3RecordHandler for the given data model.
     * @param model the model for which items will be created
     */
    public NcbiGffGFF3RecordHandler (Model model) {
        super(model);
        refsAndCollections.put("Exon", "transcripts");
        refsAndCollections.put("Transcript", "gene");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        // This method is called for every line of GFF3 file(s) being read.  Features and their
        // locations are already created but not stored so you can make changes here.  Attributes
        // are from the last column of the file are available in a map with the attribute name as
        // the key.   For example:
        //
        //     Item feature = getFeature();
        //     String symbol = record.getAttributes().get("symbol");
        //     feature.setAttribute("symbol", symbol);
        //
        // Any new Items created can be stored by calling addItem().  For example:
        //
        //     String geneIdentifier = record.getAttributes().get("gene");
        //     gene = createItem("Gene");
        //     gene.setAttribute("primaryIdentifier", geneIdentifier);
        //     addItem(gene);
        //
        // You should make sure that new Items you create are unique, i.e. by storing in a map by
        // some identifier.
        Item feature = getFeature();

        String type = record.getType();

        if ("gene".equals(type)) {
            feature.setClassName("Gene");
            for (String identifier : record.getDbxrefs()) {
                if (identifier.contains("GeneID")) {
                    String[] bits = identifier.split(":");
                    feature.setAttribute("primaryIdentifier", bits[1]);
                }
            }
            String symbol = record.getAttributes().get("Name").iterator().next();
            feature.setAttribute("symbol", symbol);
            if (record.getAttributes().get("description") != null) {
                String description = record.getAttributes().get("description").iterator().next();
                feature.setAttribute("briefDescription", description);
            }
        } else if ("transcript".equals(type)) {
            feature.setClassName("Transcript");
            String identifier = record.getAttributes().get("transcript_id").iterator().next();
            feature.setAttribute("primaryIdentifier", identifier);
            if (record.getAttributes().get("product") != null) {
                String description = record.getAttributes().get("product").iterator().next();
                feature.setAttribute("name", description);
            }
        } else if ("exon".equals(type)) {
            feature.setClassName("Exon");
            String identifier = record.getId();
            String[] bits = identifier.split("id");
            String exonNumber = bits[1];
            if (record.getAttributes().get("transcript_id") != null) {
                String transcriptId = record.getAttributes().get("transcript_id").iterator().next();
                feature.setAttribute("primaryIdentifier", transcriptId + "." + exonNumber);
            } else {
                // ncRNA
            }

            if (record.getAttributes().get("product") != null) {
                String description = record.getAttributes().get("product").iterator().next();
                feature.setAttribute("name", description);
            }
        }
    }
}
