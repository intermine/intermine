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

/**
 * Representation of a field in an object
 *
 * @author Andrew Varley
 */
public class Field
{

    private String name = "";
    private String value = "";

    /**
     * Set the name of this field
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of this field
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the value of this field
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the value of this field
     *
     * @return the value
     */
    public String getValue() {
        return this.value;
    }


    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if (o instanceof Field) {
            Field f = (Field) o;
            return name.equals(f.name) && value.equals(f.value);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return name.hashCode() + 3 * value.hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "name=\"" + name + "\" value=\"" + value + "\"";
    }
}
