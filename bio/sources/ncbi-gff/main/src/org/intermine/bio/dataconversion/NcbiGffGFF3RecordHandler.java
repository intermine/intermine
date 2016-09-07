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

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the NcbiGff dataset via GFF files.
 * @author julie
 */

public class NcbiGffGFF3RecordHandler extends GFF3RecordHandler
{
    private static final String CHROMOSOME_PREFIX = "NC_";
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

        // only want chromsomes of interest
        if (!record.getSequenceID().startsWith(CHROMOSOME_PREFIX)) {
            /**
             * We have genes on multiple chromosomes. We are only interested in the "good" ones.
             * Thus some genes processed by this parser will not have a location.
             * In this case, we do not want to store the gene. See #1259
             */
            removeFeature();
            return;
        }

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
                // TODO ncRNA
            }

            if (record.getAttributes().get("product") != null) {
                String description = record.getAttributes().get("product").iterator().next();
                feature.setAttribute("name", description);
            }
        }
    }
}
