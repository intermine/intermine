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

import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

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

    /**
     * Constructor
     * @param c the actual collection
     * @param cld the type of this collection
     * @param model the metadata for the collection
     * @throws Exception if an error occurs
     */
    public DisplayCollection(Collection c, ClassDescriptor cld, Model model) throws Exception {
        this.cld = cld;
        size = c.size();
        for (Iterator i = c.iterator(); i.hasNext();) {
            Set clds = ObjectViewController.getLeafClds(i.next().getClass(), model);
            if (clds.size() == 1 && clds.iterator().next().equals(cld)) {
                break;
            }
            if (classes.containsKey(clds)) {
                classes.put(clds, new Integer(((Integer) classes.get(clds)).intValue() + 1));
            } else {
                classes.put(clds, new Integer(1));
            }
        }
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