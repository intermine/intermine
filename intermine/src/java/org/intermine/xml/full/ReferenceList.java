package org.flymine.xml.full;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.ArrayList;

/**
 * Representation of a field in an object.
 *
 * @author Andrew Varley
 */
public class ReferenceList
{

    private String name = "";
    private List references = new ArrayList();

    /**
     * Set the name of this field.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of this field.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Add a value to the list of references.
     *
     * @param value the value
     */
    public void addValue(String value) {
        this.references.add(value);
    }

    /**
     * Get the references in this collection.
     *
     * @return the list of references
     */
    public List getReferences() {
        return this.references;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if (o instanceof ReferenceList) {
            ReferenceList r = (ReferenceList) o;
            return name.equals(r.name) && references.equals(r.references);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return name.hashCode() + 3 * references.hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return name + ", " + references;
    }
}
