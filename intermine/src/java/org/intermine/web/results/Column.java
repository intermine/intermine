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
    protected int index;

    /**
     * Update the user-selectable attributes of this column from another
     *
     * @param other the column to update from
     */
    public void update(Column other) {
        if (!(this.equals(other))) {
            throw new IllegalArgumentException("Cannot update a column from one"
                                               + " with a different columnName");
        }
        setVisible(other.isVisible());
    }

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
     * Get the index of the column. This is the index that we use in a
     * call to the get() method of a ResultsRow.
     *
     * @return the index of the column
     */
    public int getIndex() {
        return index;
    }

    /**
     * Set the index of the column
     *
     * @param index the index for the column
     */
    public void setIndex(int index) {
        this.index = index;
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
            + name + " at " + index + (visible ? "visible" : "not visible") + "]";
    }
}
