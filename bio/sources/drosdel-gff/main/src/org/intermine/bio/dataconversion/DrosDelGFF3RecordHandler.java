package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the DrosDel dataset via GFF files.
 *
 * @author Kim Rutherford
 */

public class DrosDelGFF3RecordHandler extends GFF3RecordHandler
{
    private Map elementsMap = new HashMap();
    
    /**
     * Create a new DrosDelGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public DrosDelGFF3RecordHandler (Model tgtModel) {
        super(tgtModel);
    }

    /**
     * @see GFF3RecordHandler#process(GFF3Record)
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();

        List availableList = (List) record.getAttributes().get("available");
        if (record.getType().equals("ArtificialDeletion")) {
            feature.setAttribute("available", (String) availableList.get(0));
            List element1List = (List) record.getAttributes().get("Element1");
            if (element1List != null) {
                String elem1Identifier = (String) element1List.get(0);
                Item elem1 = (Item) elementsMap.get(elem1Identifier);
                if (elem1 == null) {
                    throw new RuntimeException("TransposableElementInsertionSite features must "
                                               + "be first in the GFF file - can't find: "
                                               + elem1Identifier);
                }
                elem1.setAttribute("identifier", elem1Identifier);
                feature.setReference("element1", elem1);
            }
            List element2List = (List) record.getAttributes().get("Element2");
            if (element2List != null) {
                String elem2Identifier = (String) element2List.get(0);
                Item elem2 = (Item) elementsMap.get(elem2Identifier);
                if (elem2 == null) {
                    throw new RuntimeException("TransposableElementInsertionSite features must "
                                               + "be first in the GFF file - can't find: " 
                                               + elem2Identifier);
                }
                elem2.setAttribute("identifier", elem2Identifier);
                feature.setReference("element2", elem2);
            }
        } else {
            if (record.getAttributes().get("type") != null) {
                String type = (String) ((List) record.getAttributes().get("type")).get(0);
                feature.setAttribute("type", type);
            }
            if (record.getAttributes().get("subtype") != null) {
                String type = (String) ((List) record.getAttributes().get("subtype")).get(0);
                feature.setAttribute("subType", type);
            }
            // save and don't store so we can fix up element references
            String identifier = feature.getAttribute("identifier").getValue();
            elementsMap.put(identifier, feature);
            feature.setAttribute("symbol", identifier);
            removeFeature();
        }
    }
    
    /**
     * Return items that need extra processing that can only be done after all other GFF features
     * have been read.  For this class TransposableElementInsertionSite items 
     * @return the final Items
     */
    public Collection getFinalItems() {
        return elementsMap.values();
    }
}
