package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.metadata.ClassDescriptor;

/**
 * Class to represent a field of an object for the webapp
 * @author Kim Rutherford
 */

public class DisplayField
{
    ClassDescriptor cld;
    int size;
    Map classes = new LinkedHashMap();
    InlineResultsTable table;

    /**
     * Create a new DisplayField object.
     * @param collection the List the holds the object(s) to display
     * @param cld metadata for the referenced object
     * @param webconfigTypeMap the Type Map from the webconfig file
     * @param webProperties the web properties from the session
     * @throws Exception if an error occurs
     */
    public DisplayField(List collection, ClassDescriptor cld,
                        Map webconfigTypeMap, Map webProperties) throws Exception {
        this.cld = cld;
        table = new InlineResultsTable(collection, cld, webconfigTypeMap, webProperties);
        if (collection instanceof LazyCollection) {
            size = ((LazyCollection) collection).getInfo().getRows();
        } else {
            size = collection.size();
        }
    }

    /**
     * Get the inline results table for this collection
     * @return the results table
     */
    public InlineResultsTable getTable() {
        return table;
    }
    
    /**
     * Get the class descriptor for this collection
     * @return the class descriptor
     */
    public ClassDescriptor getCld() {
        return cld;
    }

    /**
     * Get the size of this collection
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the map of type of objects in this collection
     * @return the classes
     */
    public Map getClasses() {
        return classes;
    }
}
