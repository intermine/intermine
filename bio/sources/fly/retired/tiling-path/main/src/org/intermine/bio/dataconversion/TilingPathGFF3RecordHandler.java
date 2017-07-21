package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.List;

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the Drosophila tiling path GFF3 files.
 *
 * @author Kim Rutherford
 */

public class TilingPathGFF3RecordHandler extends GFF3RecordHandler
{


    /**
     * Create a new TilingPathGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public TilingPathGFF3RecordHandler (Model tgtModel) {
        super(tgtModel);

        // create a map of classname to reference name for parent references
        refsAndCollections = new HashMap<String, String>();
        refsAndCollections.put("PCRProduct", "tilingPathSpan");
        refsAndCollections.put("ForwardPrimer", "pcrProduct");
        refsAndCollections.put("ReversePrimer", "pcrProduct");
    }

    /**
     * {@inheritDoc}
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();
        String clsName = feature.getClassName();

        List<String> newIds = record.getAttributes().get("newID");
        if (newIds != null) {
            String newId = newIds.get(0);
            feature.setAttribute("primaryIdentifier", newId);
        }
        List<String> oldIds = record.getAttributes().get("oldID");
        if (oldIds != null) {
            String oldId = oldIds.get(0);
            if (!oldId.equals(record.getId())) {
                converter.getSynonym(feature, oldId);
            }
        }
        if ("PCRProduct".equals(clsName)) {
            List<String> promoters = record.getAttributes().get("promotor");
            if ("1".equals(promoters.get(0))) {
                feature.setAttribute("promoter", "true");
            } else {
                feature.setAttribute("promoter", "false");
            }
        }
    }
}
