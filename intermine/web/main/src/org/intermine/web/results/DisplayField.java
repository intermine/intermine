package org.intermine.web.results;

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
import java.util.Map;
import java.util.NoSuchElementException;

import org.intermine.objectstore.query.Results;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.web.Constants;
import org.intermine.web.config.WebConfig;

import org.apache.log4j.Logger;

/**
 * Class to represent a field of an object for the webapp
 * @author Kim Rutherford
 */

public class DisplayField
{
    FieldDescriptor fd;
    int size = -1;
    InlineResultsTable table = null;
    Collection collection = null;
    WebConfig webConfig = null;
    Map webProperties = null;
    
    protected static final Logger LOG = Logger.getLogger(DisplayField.class);
    private final Map classKeys;
    
    /**
     * Create a new DisplayField object.
     * @param collection the List the holds the object(s) to display
     * @param fd metadata for the referenced object
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties the web properties from the session
     * @param classKeys Map of class name to set of keys
     * @throws Exception if an error occurs
     */
    public DisplayField(Collection collection, FieldDescriptor fd,
                        WebConfig webConfig, Map webProperties,
                        Map classKeys) throws Exception {
        this.collection = collection;
        this.fd = fd;
        this.webConfig = webConfig;
        this.webProperties = webProperties;
        this.classKeys = classKeys;
    }

    /**
     * Get the inline results table for this collection
     * @return the results table
     */
    public InlineResultsTable getTable() {
        if (table == null && collection.size() > 0) {
            // default
            int maxInlineTableSize = 30;
            String maxInlineTableSizeString = 
                (String) webProperties.get(Constants.INLINE_TABLE_SIZE);

            try {
                maxInlineTableSize = Integer.parseInt(maxInlineTableSizeString);
            } catch (NumberFormatException e) {
                LOG.warn("Failed to parse " + Constants.INLINE_TABLE_SIZE + " property: "
                         + maxInlineTableSizeString);
            }

            int tableSize = maxInlineTableSize;

            try {
                // don't call size unless we have to - it's slow
                if (collection instanceof Results) {
                    ((Results) collection).get(tableSize);
                } else {
                    if (collection.size() < tableSize) {
                        tableSize = collection.size();
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                tableSize = collection.size();
            }


            table = new InlineResultsTable(collection, fd.getClassDescriptor().getModel(),
                                           webConfig, webProperties, classKeys, tableSize);
        }
        return table;
    }
    
    /**
     * Return true if the collection is empty or reference null.
     * @return true if collection/reference is empty
     */
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    /**
     * Get the size of this collection
     * @return the size
     */
    public int getSize() {
        if (size == -1) {
            if (collection instanceof LazyCollection) {
                try {
                    LazyCollection lazyCollection = (LazyCollection) collection;
                    try {
                        // get the first batch to make sure that small collections have an accurate
                        // size
                        // see also ticket #267
                        lazyCollection.iterator().next();
                        size = lazyCollection.getInfo().getRows();
                    } catch (IndexOutOfBoundsException err) {
                        size = 0;
                    } catch (NoSuchElementException err) {
                        size = 0;
                    }
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("unable to find the size of a collection", e);
                }
            } else {
                size = collection.size();
            }
        }
        return size;
    }
}
