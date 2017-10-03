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

import java.util.Collection;
import java.util.HashMap;
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
        if ("ChromosomalDeletion".equals(record.getType())) {
            if ("made".equals(note)) {
                feature.setAttribute("available", "true");
            } else {
                feature.setAttribute("available", "false");
            }
            String identifier = feature.getAttribute("primaryIdentifier").getValue();
            // don't need a primaryIdentifier
            feature.removeAttribute("primaryIdentifier");
            feature.setAttribute("secondaryIdentifier", identifier);
        } else {
            throw new RuntimeException("unknown type: " + record.getType());
        }
    }

    /**
     * Return false - get locations from FlyBase.
     * {@inheritDoc}
     */
    @Override
    protected boolean createLocations(@SuppressWarnings("unused") GFF3Record record) {
        return false;
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
