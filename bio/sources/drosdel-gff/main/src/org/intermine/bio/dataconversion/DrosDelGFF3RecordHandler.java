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
    private Map<String, Item> elementsMap = new HashMap<String, Item>();

    /**
     * Create a new DrosDelGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public DrosDelGFF3RecordHandler (Model tgtModel) {
        super(tgtModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        Item feature = getFeature();

        String note = record.getNote();
        if (record.getType().equals("ArtificialDeletion")) {
            if (note.equals("made")) {
                feature.setAttribute("available", "true");
            } else {
                feature.setAttribute("available", "false");
            }
            List<String> element1List = record.getAttributes().get("Element1");
            if (element1List != null) {
                String elem1Identifier = element1List.get(0);
                Item elem1 = elementsMap.get(elem1Identifier);
                if (elem1 == null) {
                    throw new RuntimeException("TransposableElementInsertionSite features must "
                                               + "be first in the GFF file - can't find: "
                                               + elem1Identifier);
                }
                feature.setReference("element1", elem1);
            }
            List<String> element2List = record.getAttributes().get("Element2");
            if (element2List != null) {
                String elem2Identifier = element2List.get(0);
                Item elem2 = elementsMap.get(elem2Identifier);
                if (elem2 == null) {
                    throw new RuntimeException("TransposableElementInsertionSite features must "
                                               + "be first in the GFF file - can't find: "
                                               + elem2Identifier);
                }
                feature.setReference("element2", elem2);
            }
            String identifier = feature.getAttribute("primaryIdentifier").getValue();
            // don't need a primaryIdentifier
            feature.removeAttribute("primaryIdentifier");
            feature.setAttribute("secondaryIdentifier", identifier);
        } else {
            if (record.getAttributes().get("type") != null) {
                String type = record.getAttributes().get("type").get(0);
                feature.setAttribute("type", type);
            }
            if (record.getAttributes().get("subtype") != null) {
                String type = record.getAttributes().get("subtype").get(0);
                feature.setAttribute("subType", type);
            }
            String identifier = feature.getAttribute("primaryIdentifier").getValue();
            // don't need a primaryIdentifier
            feature.removeAttribute("primaryIdentifier");
            elementsMap.put(identifier, feature);
            feature.setAttribute("secondaryIdentifier", identifier);
            removeFeature();
        }
    }

    /**
     * Return true for deletions, false for insertions - get insertion locations from FlyBase.
     * {@inheritDoc}
     */
    @Override
    protected boolean createLocations(GFF3Record record) {
        return record.getType().equals("ArtificialDeletion");
    }

    /**
     * Return items that need extra processing that can only be done after all other GFF features
     * have been read.  For this class TransposableElementInsertionSite items
     * @return the final Items
     */
    @Override
    public Collection<Item> getFinalItems() {
        return elementsMap.values();
    }
}
