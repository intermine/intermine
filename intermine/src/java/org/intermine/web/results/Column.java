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

/**
 * Configuration information for a column in a table
 *
 * @author Andrew Varley
 */
public class Column
{
    protected boolean visible = true;
    protected String name = "";

    /**
     * Is the column visible
     *
     * @return true if the column is visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set the visibility of the column
     *
     * @param visible true if visible, false if not
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Get the name of the column
     *
     * @return the name of the column
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the column
     *
     * @param name the name for the column
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @see Object#equals
     *
     * @param other the object to compare with
     * @return true if the objects are equal
     */
    public boolean equals(Object other) {
        if (other instanceof Column) {
            return name.equals(((Column) other).getName());
        }
        return false;
    }

    /**
     * @see Object#hashCode
     *
     * @return a hashCode for this column
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "[Column " + super.toString() + " "
            + name + " " + (visible ? "visible" : "not visible") + "]";
    }
}
