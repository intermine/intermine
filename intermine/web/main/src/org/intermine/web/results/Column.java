package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Configuration information for a column in a table
 *
 * @author Andrew Varley
 */
public class Column
{
    protected boolean visible;
    protected String name = "";
    protected int index;
    protected Object type;

    /**
     * Gets the value of visible
     *
     * @return the value of visible
     */
    public boolean isVisible()  {
        return visible;
    }

    /**
     * Sets the value of visible
     *
     * @param visible value to assign to visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Gets the value of name
     *
     * @return the value of name
     */
    public String getName()  {
        return name;
    }

    /**
     * Sets the value of name
     *
     * @param name value to assign to name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the value of index
     *
     * @return the value of index
     */
    public int getIndex()  {
        return index;
    }

    /**
     * Sets the value of index
     *
     * @param index value to assign to index
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Return the type of this Column
     * @return a Class or a FieldDescriptor
     */
    public Object getType() {
        return type;
    }

    /**
     * Set the type of this Column
     * @param type a Class or a FieldDescriptor
     */
    public void setType(Object type) {
        this.type = type;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object other) {
        if (other instanceof Column) {
            return name.equals(((Column) other).getName());
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "[Column " + name + " " + (visible ? "visible" : "not visible") + "]";
    }
}
