package org.intermine.web.bag;

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

import org.intermine.web.results.ResultElement;

/**
 * @author Xavier Watkins
 *
 */
public class BagElement implements Serializable
{
    protected String type;
    protected Integer id;
    
    /**
     * Constructs an element to be stored in a bag
     * @param id the InterMineObject id
     * @param type the InterMineObject type
     */
    public BagElement(Integer id, String type) {
        this.id = id;
        this.type = type;
    }

    /**
     * Constructs an element to be stored in a bag
     * @param resElt a ResultElemeent objects
     */
    public BagElement (ResultElement resElt) {
        this.id = resElt.getId();
        this.type = resElt.getType();
    }
    
    /**
     * Returns the InterMineObject id
     * @return the InterMineObject id as an Integer 
     */
    public Integer getId() {
        return id;
    }

    /**
     * Set the value of id
     * @param id the InterMineObject id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Return the InterMineObject type
     * @return the InterMineObject type as a String
     */
    public String getType() {
        return type;
    }

    /**
     * Set the value of type
     * @param type the InterMineObject type
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Returns a String representation of the BagElement
     * @return a String
     */
    public String toString() {
        return " " + id + " " + type;
    }
    
    /**
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        BagElement bagElt = (BagElement) obj;
        return (id.equals(bagElt.getId()) 
                        && type.equals(bagElt.getType()));
    }
    
    /**
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return  id.hashCode() + 3 * type.hashCode();
    }
}
