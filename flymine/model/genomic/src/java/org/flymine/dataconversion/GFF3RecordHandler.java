package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import org.intermine.xml.full.Item;
import org.intermine.metadata.Model;

import org.flymine.io.gff3.GFF3Record;

/**
 * Permits specific operations to be performed when processing an line of GFF3.
 * GFF3Converter sets the core items created in the handler, the handler can
 * alter, remove from or add to the set of items.
 *
 * @author Richard Smith
 */
public class GFF3RecordHandler
{
    private Map items = new HashMap();
    private Item sequence;
    private Item analysis;
    private Model tgtModel;

    /**
     * Construct with the model to create items in (for type checking).
     * @param tgtModel the model for which items will be created
     */
    public GFF3RecordHandler(Model tgtModel) {
        this.tgtModel = tgtModel;
    }

    /**
     * Method to perform additional operations for a GFF3Record.  Access to core items
     * is possible via getters() (sequence and coumputatinal analysis items cannot be
     * altered but are avalable read-only).  Additional items to store can be placed
     * in the items map, keys can be anything except a string starting with '_'.  idMap
     * should be used to get/add any item identifiers for features used.
     * @param record the GFF line being processed
     * @param idMap map from GFF ID to item identifier for all features
     */
    public void process(GFF3Record record, Map idMap) {
    }

    /**
     * Set sequence item created for this record, should not be edited in handler.
     * @param sequence the sequence item
     */
    public void setSequence(final Item sequence) {
        this.sequence = sequence;
    }

    /**
     * Set location item for this record.
     * @param location the location item
     */
    public void setLocation(Item location) {
        items.put("_location", location);
    }

    /**
     * Set feature item for this record.
     * @param feature the feature item
     */
    public void setFeature(Item feature) {
        items.put("_feature", feature);
    }

    /**
     * Set ComputationalAnalysis item created for this record, should not be edited in handler.
     * @param analysis the ComputationalAnalysis item
     */
    public void setAnalysis(final Item analysis) {
        this.analysis = analysis;
    }

    /**
     * Set ComputationalResult item for this record.
     * @param result the ComputationalResult item
     */
    public void setResult(Item result) {
        items.put("_result", result);
    }

    /**
     * Set SimpleRelation item from feature to parent feature for this record.
     * @param relation the relation item
     */
    public void setParentRelation(Item relation) {
        items.put("_relation", relation);
    }

    /**
     * Remove all items held locally in handler.
     */
    public void clear() {
        this.items = new HashMap();
        this.sequence = null;
        this.analysis = null;
    }

    /**
     * Return all items set and created in handler - excludes sequence and
     * ComputationalAnalysis items.
     * @return a set of items
     */
    public Collection getItems() {
        return items.values();
    }
}
