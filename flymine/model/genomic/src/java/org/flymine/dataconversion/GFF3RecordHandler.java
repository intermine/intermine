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
import org.intermine.xml.full.ItemFactory;
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
    private ItemFactory itemFactory;
    private Map identifierMap;

    /**
     * Construct with the model to create items in (for type checking).
     * @param tgtModel the model for which items will be created
     */
    public GFF3RecordHandler(Model tgtModel) {
        this.tgtModel = tgtModel;
    }

    /**
     * Method to perform additional operations for a GFF3Record.  Access to core items
     * is possible via getters() (sequence and computational analysis items cannot be
     * altered but are available read-only).  Additional items to store can be placed
     * in the items map, keys can be anything except a string starting with '_'.
     * @param record the GFF line being processed
     */
    public void process(GFF3Record record) {
        
    }

    /**
     * Set the Map of GFF identifiers to Item identifier.  The Map should be used to get/add any
     * item identifiers for features used so that multiple Items aren't created for the same
     * feature.
     * @param identifierMap map from GFF ID to item identifier for all features
     */
    public void setIdentifierMap(Map identifierMap) {
        this.identifierMap = identifierMap;
    }

    /**
     * Return the Model that was passed to the constructor.
     * @return the Model
     */
    public Model getTargetModel() {
        return tgtModel;
    }

    /**
     * Set sequence item created for this record, should not be edited in handler.
     * @param sequence the sequence item
     */
    public void setSequence(final Item sequence) {
        this.sequence = sequence;
    }

    /**
     * Return the sequence Item set by setSequence()
     * @return the sequence Item
     */
    protected Item getSequence() {
        return sequence;
    }

    /**
     * Set the location item for this record.
     * @param location the location item
     */
    public void setLocation(Item location) {
        items.put("_location", location);
    }

    /**
     * Return the location Item set by setLocation()
     * @return the location Item
     */
    protected Item getLocation() {
        return (Item) items.get("_location");
    }

    /**
     * Set hte feature item for this record.
     * @param feature the feature item
     */
    public void setFeature(Item feature) {
        items.put("_feature", feature);
    }

    /**
     * Return the feature Item set by setFeature()
     * @return the feature Item
     */
    protected Item getFeature() {
        return (Item) items.get("_feature");
    }

    /**
     * Set the ComputationalAnalysis item created for this record, should not be edited in handler.
     * @param analysis the ComputationalAnalysis item
     */
    public void setAnalysis(final Item analysis) {
        this.analysis = analysis;
    }

    /**
     * Set the ComputationalResult item for this record.
     * @param result the ComputationalResult item
     */
    public void setResult(Item result) {
        items.put("_result", result);
    }

    /**
     * Return the result Item set by setResult()
     * @return the result Item
     */
    protected Item getResult() {
        return (Item) items.get("_result");
    }

    /**
     * Get the SimpleRelation item from feature to parent feature for this record.
     * @param relation the relation item
     */
    public void setParentRelation(Item relation) {
        items.put("_relation", relation);
    }

    /**
     * Return the SimpleRelation Item set by setParentRelation()
     * @return the location Item
     */
    protected Item getParentRelation() {
        return (Item) items.get("_relation");
    }

    /**
     * Set the ItemFactory to use in this handler.
     * @param itemFactory the ItemFactory
     */
    public void setItemFactory(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    /**
     * Get the ItemFactory for this handler.
     * @return the ItemFactory
     */
    protected ItemFactory getItemFactory() {
        return itemFactory;
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

    /**
     * Add a new Item to the Collection returned by getItems().
     * @param item the Item to add
     */
    public void addItem(Item item) {
        items.put(item.getIdentifier(), item);
    }
}
