package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Representation of an Attribute in an object
 *
 * @author Andrew Varley
 */
public class Attribute
{
    private String name = "";
    private String value = "";

    /**
     * Constructor
     */
    public Attribute() {
    }

    /**
     * Construnctor
     * @param name the name
     * @param value the value
     */
    public Attribute(String name, String value) {
        this.name = name;
        setValue(value);
    }

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
        return name;
    }

    /**
     * Set the value of this field
     *
     * @param value the value
     */
    public void setValue(String value) {
        if (value == null) {
            throw new RuntimeException("value null while calling setValue() on " + this);
        }
        this.value = value;
    }

    /**
     * Get the value of this field
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if (o instanceof Attribute) {
            Attribute a = (Attribute) o;
            return name.equals(a.name)
            && value.equals(a.value);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return name.hashCode()
        + 3 * value.hashCode();
    }
}
