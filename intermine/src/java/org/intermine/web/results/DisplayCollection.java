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
import java.util.LinkedHashMap;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.proxy.LazyCollection;

/**
 * Class to represent a collection field of an object for the webapp
 * @author Mark Woodbridge
 */
public class DisplayCollection
{
    ClassDescriptor cld;
    int size;
    Map classes = new LinkedHashMap();
    boolean verbose = false;
    InlineResultsTable table;

    /**
     * Constructor
     * @param collection the actual collection
     * @param cld the type of this collection
     * @throws Exception if an error occurs
     */
    public DisplayCollection(LazyCollection collection, ClassDescriptor cld) throws Exception {
        this.cld = cld;
        table = new InlineResultsTable(collection, cld);
        size = collection.getInfo().getRows();
//         for (Iterator i = c.iterator(); i.hasNext();) {
//             Set clds = ObjectViewController.getLeafClds(i.next().getClass(), cld.getModel());
//             if (clds.size() == 1 && clds.iterator().next().equals(cld)) {
//                 break;
//             }
//             if (classes.containsKey(clds)) {
//                 classes.put(clds, new Integer(((Integer) classes.get(clds)).intValue() + 1));
//             } else {
//                 classes.put(clds, new Integer(1));
//             }
//         }
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
    
    /**
     * Set the verbosity level for display of this reference
     * @param verbose the verbosity level
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Get the verbosity level for display of this reference
     * @return the verbosity level
     */
    public boolean isVerbose() {
        return verbose;
    }
}
