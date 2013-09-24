package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.bio.dataconversion.GFF3RecordHandler;
//import org.intermine.bio.dataconversion.GFF3RecordHandler;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the WormCds dataset via GFF files.
 */

public class WormCdsGFF3RecordHandler extends GFF3RecordHandler
{
    private static final String SOME_PREFIX = "CDS:";
    private static final Logger LOG = Logger.getLogger(WormCdsGFF3RecordHandler.class);

    /**
     * Create a new WormCdsGFF3RecordHandler for the given data model.
     * @param model the model for which items will be created
     */
    public WormCdsGFF3RecordHandler (Model model) {
        super(model);
        //TODO
        //transcripts should be a collection, but the setReference does not work for collections
        //refsAndCollections.put("CDS", "transcripts");
        refsAndCollections.put("CDS", "transcript");
        refsAndCollections.put("Transcript", "gene");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {

        String term = record.getType();
//        LOG.info ("REC: " + record);

        if ("CDS".equals(term)) {
            Item feature = getFeature();
            String wormpep = record.getAttributes().get("wormpep").get(0);
            feature.setAttribute("wormpep", wormpep);

            String status = record.getAttributes().get("status").get(0);
            feature.setAttribute("predictionStatus", status);

            String cdsLength = record.getAttributes().get("cdsLength").get(0);
            feature.setAttribute("codingSequenceLength", cdsLength);

            String primaryIdentifier =
                    feature.getAttribute("primaryIdentifier").getValue().replace(SOME_PREFIX, "");
            feature.setAttribute("primaryIdentifier", primaryIdentifier);

            //if (feature.getAttribute("Note") != null) {
            //    String note = record.getAttributes().get("Note").get(0);
            //    feature.setAttribute("Note", note);
            //}

        }
        if ("gene".equals(term)) {
            Item feature = getFeature();
            String primaryIdentifier =
                    feature.getAttribute("primaryIdentifier").getValue().replace("Gene:", "");
            feature.setAttribute("primaryIdentifier", primaryIdentifier);
            feature.setAttribute("symbol", primaryIdentifier);
            //LOG.info ("REC GGG: " + primaryIdentifier);

        }
        if ("transcript".equals(term)) {
            Item feature = getFeature();
            String primaryIdentifier =
                    feature.getAttribute("primaryIdentifier").getValue().replace("Transcript:", "");
            feature.setAttribute("primaryIdentifier", primaryIdentifier);
            //LOG.info ("REC TTT: " + primaryIdentifier);
        }
        // This method is called for every line of GFF3 file(s) being read.  Features and their
        // locations are already created but not stored so you can make changes here.  Attributes
        // are from the last column of the file are available in a map with the attribute name as
        // the key.   For example:
        //
        //     Item feature = getFeature();
        //     String symbol = record.getAttributes().get("symbol");
        //     feature.setAttrinte("symbol", symbol);
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
    }
}


