package org.intermine.bio.networkview.network;

/*
 * Copyright (C) 2002-2007 FlyMine
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
     * returns the value for the given key (name)
     * @param name the name of the attribute
     * @return the value of the attribute or null if non was found for the specified name
     */
    public Object getAttributeValue(String name) {
        if (attributes.containsKey(name)) {
            return ((FlyValueWrapper) attributes.get(name)).getValue();
        } else {
            return null;
        }
    }
    
    /**
     * @param name the name of the attribute
     * @return the flag for this value or -1 if non was found for the specified name
     */
    public int getAttributeFlag(String name) {
        if (attributes.containsKey(name)) {
            return ((FlyValueWrapper) attributes.get(name)).getFlag();
        } else {
            return -1;
        }

    }

    /**
     * Adds a name-value-pair attribute to this element. This will 
     * update/overwrite values for already existing attributes and use a default flag.
     * The default flag is currently set to FlyValueWrapper.OVERWRITE. Other values 
     * for this flag are: FlyValueWrapper.NOT_OVERWRITE and FlyValueWrapper.ADD
     * @param name name of the attribute
     * @param value value of the attribute
     * @return true if the attribute was successfully added
     * @see #setAttribute(String, Object, int, boolean)
     */
    public boolean setAttribute(String name, Object value) {
        return this.setAttribute(name, value, FlyValueWrapper.OVERWRITE, true);
    }
    
    /**
     * Adds a name-value-pair attribute to this element.
     * @param name name of the attribute
     * @param value value of the attribute
     * @param flag flag indicating what to do if a value for this attribute already exists
     * @return true if the attribute was successfully added
     */
    public boolean setAttribute(String name, Object value, int flag) {
        return this.setAttribute(name, value, flag, true);
    }
    
    /**
     * Adds a name-value-pair attribute to this element.
     * @param name name of the attribute
     * @param value value of the attribute
     * @param update whether to update this value or not, default is true
     * @return true if the attribute was successfully added
     */
    public boolean setAttribute(String name, Object value, boolean update) {
        // TODO: do we need the update flag?
        return this.setAttribute(name, value, FlyValueWrapper.OVERWRITE, update);
    }
    
    /**
     * Adds a name-value-pair attribute to this element.
     * @param name name of the attribute
     * @param value value of the attribute
     * @param flag flag indicating what to do if a value for this attribute already exists
     * @param update whether to update this value or not, default is true
     * @return true if the attribute was successfully added
     */
    public boolean setAttribute(String name, Object value, int flag, boolean update) {
        if (attributes.containsKey(name) && !update) {
            return false;
        } else {
            attributes.put(name, new FlyValueWrapper(value, flag));
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
            Object value1 = this.getAttributeValue(attName);
            Object value2 = g.getAttributeValue(attName);
            if (!value1.equals(
                    value2)) {
                return false;
            }
        }
        return true;
    }
    
    //TODO: change to equals() and hashcode()
    
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
