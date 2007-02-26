package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.HashMap;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * Abstract parent class of all DataConverters
 * @author Mark Woodbridge
 */
public abstract class DataConverter
{
    private static final Logger LOG = Logger.getLogger(DataConverter.class);

    protected ItemWriter writer;
    protected Map aliases = new HashMap();
    protected int nextClsId = 0;

    /**
    * Constructor that should be called by children
    * @param writer an ItemWriter used to handle the resultant Items
    */
    public DataConverter(ItemWriter writer) {
        this.writer = writer;
    }

    /**
     * Uniquely alias a className
     * @param className the class name
     * @return the alias
     */
    protected String alias(String className) {
        String alias = (String) aliases.get(className);
        if (alias != null) {
            return alias;
        }
        String nextIndex = "" + (nextClsId++);
        aliases.put(className, nextIndex);
        LOG.info("Aliasing className " + className + " to index " + nextIndex);
        return nextIndex;
    }
    
    /**
     * Add an Item to a named collection on another Item. If the collection does not exist
     * if will be created.
     * 
     * @param item item with collection
     * @param collection collection name
     * @param addition item to add to collection
     * @throws ObjectStoreException if something goes wrong
     */
    protected void addToCollection(Item item, String collection, Item addition)
        throws ObjectStoreException {
        ReferenceList coll = item.getCollection(collection);
        if (coll == null) {
            coll = new ReferenceList(collection);
            item.addCollection(coll);
        }
        coll.addRefId(addition.getIdentifier());
    }
}
