package org.flymine.networkview.network;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Class representing the elements of the FlyNetwork
 * @author Florian Reisinger
 */
public abstract class FlyHashGraphElement implements Serializable 
    {

    //private static final long serialVersionUID = 999999999001L; // not needed -> abstract
    protected String label;

    protected Hashtable attributes;

    /**
     * Constructor
     * @param label name of this element
     */
    protected FlyHashGraphElement(String label) {
        this.label = label;
        this.attributes = new Hashtable();
    }

    /**
     * @return the label of that element
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return a Collection af all attribute names
     */
    public Collection getAttributeNames() {
        return attributes.keySet();
    }

    /**
     * returns the value of a name-value-pair for the given name
     * @param name the name of the attribute
     * @return the value of the attribute or null if no attribute 
     * with the specified name was found
     */
    public Object getAttributeValue(String name) {
        if (attributes.containsKey(name)) {
            return attributes.get(name);
        } else {
            return null;
        }
    }

    /**
     * Adds a name-value-pair attribute to this element. This will 
     * update/overwrite values for already existing attributes.
     * @param name name of the attribute
     * @param value value of the attribute
     * @return true if the attribute was successfully added
     * @see #setAttribute(String, Object, boolean)
     */
    public boolean setAttribute(String name, Object value) {
        return this.setAttribute(name, value, true);
    }

    /**
     * Adds a name-value-pair attribute to this element. Set update to 
     * false if you don't want to overwrite existing values.
     * @param name name of the attribute
     * @param value value of the attribute
     * @param update update flag, whether to overwrite values or not
     * @return true if the attribute was successfully added/updated
     */
    public boolean setAttribute(String name, Object value, boolean update) {
        // TODO: check validity of values types. values have to be 
        // Integer, Double, Boolean or String to be conform with cytoscape
        if (attributes.containsKey(name) && !update) {
            return false;
        } else {
            attributes.put(name, value);
            return true;
        }
    }

    /**
     * Compares the specified element with this element for equality.
     * This will compare the labels of the elements and their attributes.
     * @param g FlyHashGraphElement to compare with
     * @return true if both elements contain same information
     */
    public boolean isEqual(FlyHashGraphElement g) {
        if (!this.getLabel().equalsIgnoreCase(g.getLabel())) {
            return false;
        }

        if (g.attributes.isEmpty() && this.attributes.isEmpty()) {
            return true;
        }
        if (g.attributes.isEmpty() || this.attributes.isEmpty()) {
            return false;
        }

        Collection atts = this.getAttributeNames();
        Collection gAtts = g.getAttributeNames();

        // check if both elements have same number of attributes
        if (gAtts.size() != atts.size()) {
            return false;
        }

        // compare the values of all attributes
        for (Iterator iterator = atts.iterator(); iterator.hasNext();) {
            String attName = (String) iterator.next();
            if (!this.getAttributeValue(attName).equals(
                    g.getAttributeValue(attName))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see java.lang.Object#toString()
     * @return a String representing this element
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("label: " + this.label + "\n");
        if (this.attributes.size() == 0) {
            sb.append("  No attributes.\n");
        } else {
            sb.append("  Attributes (name -> value):\n");
            for (Iterator iter = this.getAttributeNames().iterator(); iter
                    .hasNext();) {
                String name = (String) iter.next();
                sb.append("  " + name + " -> " + this.getAttributeValue(name)
                        + "\n");
            }
        }
        return sb.toString();
    }
}
