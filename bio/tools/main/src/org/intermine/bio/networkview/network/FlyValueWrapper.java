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

/**
 * Wrapper to the attribute values
 * this is used to add a flag that can indicate what to do if a 
 * attribute value already exists when updating the network
 * TODO: !currently does NOT support lists!
 * @author Florian Reisinger
 */
public class FlyValueWrapper implements Serializable
{
    
    /**
     * own declaration of the serialVersionUID, as recommended
     */
    private static final long serialVersionUID = 9999902907901L;
    /**
     * Flag for overwriting existing value
     */
    public static final int OVERWRITE = 0;
    /**
     * Flag for not overwriting existing value
     */
    public static final int NOT_OVERWRITE = 1;
    /**
     * Flag for adding value to existing value
     */
    public static final int ADD = 2;
    /**
     * Flag for counting the elements in a List
     * use only with String values in lists
     * Bypass to be able to filter cytoscape attriutes by the number of values in a List
     * This will create a new attribute that counts the elements of a list attribute
     */
    public static final int COUNT = 3;
    
    private Object value;
    private int flag;
    
    /**
     * Constructor with default setting to overwrite existing values
     * @param value the actual value of a attribute
     */
    public FlyValueWrapper(Object value) {
        if (value instanceof Integer || value instanceof Double 
         || value instanceof Boolean || value instanceof String) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Only objects of types: " 
                    + "Boolean, Integer, Double and String allowed!" 
                    + "\nThis value is of type: " + value.getClass().toString());
        }
        this.flag = FlyValueWrapper.OVERWRITE;
    }
    
    /**
     * Constructor
     * @param value the actual value of a attribute
     * @param flag indicates what to do with this value
     */
    public FlyValueWrapper(Object value, int flag) {
        if (value instanceof Integer || value instanceof Double 
         || value instanceof Boolean || value instanceof String) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Only objects of types: " 
                    + "Boolean, Integer, Double and String allowed!" 
                    + "\nThis value is of type: " + value.getClass().toString());
        }
        this.flag = flag;
    }
    
    /**
     * Getter for the flag
     * @return an int representing the flag
     */
    public int getFlag() {
        return flag;
    }

    /**
     * Setter for the flag
     * @param flag indicates what to do with this value
     */
    public void setFlag(int flag) {
        this.flag = flag;
    }

    /**
     * Getter for the value
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Setter for the value
     * @param value set the value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    //TODO: add equals() and hashcode()
    
    /**
     * String representation of this wrappers value
     * @return the String representing this wrappers value
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return value.toString();
    }
}
