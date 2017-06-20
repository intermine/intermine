package org.intermine.web.logic.results;

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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.objectstore.query.Results;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.WebConfig;

/**
 * Class to represent a field of an object for the webapp
 * @author Kim Rutherford
 */

public class DisplayField
{
    FieldDescriptor fd;
    int size = -1;
    InlineResultsTable table = null;
    Collection<?> collection = null;
    WebConfig webConfig = null;

    protected static final Logger LOG = Logger.getLogger(DisplayField.class);
    private final Map<String, List<FieldDescriptor>> classKeys;

    /** @List<Class<?> PathQueryResultHelper resolved */
    private List<Class<?>> listOfTypes = null;

    /** @var webProperties so we can resolve # of rows to show in Collections */
    private Properties webProperties;
    private String parentClass = null;

    /**
     * Create a new DisplayField object.
     * @param collection the List the holds the object(s) to display
     * @param fd metadata for the referenced object
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties telling us how many Collection rows to show
     * @param classKeys Map of class name to set of keys
     * @param listOfTypes as determined using PathQueryResultHelper on a Collection
     * @throws Exception if an error occurs
     */
    public DisplayField(Collection<?> collection, FieldDescriptor fd,
                        WebConfig webConfig, Properties webProperties,
                        Map<String, List<FieldDescriptor>> classKeys,
                        List<Class<?>> listOfTypes) throws Exception {

        this.listOfTypes = listOfTypes;

        this.collection = collection;
        this.fd = fd;
        this.webConfig = webConfig;
        this.webProperties = webProperties;
        this.classKeys = classKeys;
    }

    /**
     * Create a new DisplayField object.
     * @param collection the List the holds the object(s) to display
     * @param fd metadata for the referenced object
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties telling us how many Collection rows to show
     * @param classKeys Map of class name to set of keys
     * @param listOfTypes as determined using PathQueryResultHelper on a Collection
     * @param objectType the type of the object.
     * @throws Exception if an error occurs
     */
    public DisplayField(Collection<?> collection,
                        FieldDescriptor fd,
                        WebConfig webConfig,
                        Properties webProperties,
                        Map<String, List<FieldDescriptor>> classKeys,
                        List<Class<?>> listOfTypes,
                        String objectType) throws Exception {
        this(collection, fd, webConfig, webProperties, classKeys, listOfTypes);
        this.parentClass = objectType;
    }

    /**
     * Get the inline results table for this collection
     * @return the results table
     */
    public InlineResultsTable getTable() {
        if (table == null && collection.size() > 0) {
            // on References we will have 1 row
            Integer tableSize = 1;
            if (webProperties != null) {
                // resolve max table size to show from properties
                String maxInlineTableSizeString =
                    (String) webProperties.get(Constants.INLINE_TABLE_SIZE);
                try {
                    tableSize = Integer.parseInt(maxInlineTableSizeString);
                } catch (NumberFormatException e) {
                    LOG.warn("Failed to parse " + Constants.INLINE_TABLE_SIZE + " property: "
                             + maxInlineTableSizeString);
                }
            }

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

            table = new InlineResultsTable(collection,
                    fd.getClassDescriptor().getModel(),
                    webConfig,
                    classKeys,
                    tableSize,
                    false,
                    listOfTypes,
                    parentClass,
                    fd);
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
                    LazyCollection<?> lazyCollection = (LazyCollection<?>) collection;
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
