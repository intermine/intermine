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

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.apache.log4j.Logger;

/**
 * A converter/retriever for the WormCds dataset via GFF files.
 */

public class WormCdsGFF3RecordHandler extends GFF3RecordHandler
{
    private static final Logger LOG = Logger.getLogger(WormCdsGFF3RecordHandler.class);

    /**
     * Create a new WormCdsGFF3RecordHandler for the given data model.
     * @param model the model for which items will be created
     */
    public WormCdsGFF3RecordHandler (Model model) {
        super(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {

         LOG.info ("worm-cds R-HANDLER rec: " + record);
        
        String term = record.getType();
        if (!"CDS".equals(term)) {
            LOG.info("SKIPPING " + term );
            return;
        }
        
        Item feature = getFeature();
        String wormpep = record.getAttributes().get('wormpep').get(0);
        feature.setAttribute("wormpep", wormpep);

        String status = record.getAttributes().get("status").get(0);
        feature.setAttribute("status", status);

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
