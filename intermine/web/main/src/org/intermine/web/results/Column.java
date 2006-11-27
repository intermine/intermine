package org.intermine.web.results;

import org.intermine.path.Path;

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
    protected int index;
    protected Object type;
    protected boolean selectable;
    private Path path;

    /**
     * Gets the value of selectable
     * @return a boolean
     */
    public boolean isSelectable() {
        return selectable;
    }

    /**
     * Sets the value of selectable
     * @param selectable value to assign to selectable
     */
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

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
            return getPath().toString().equals(((Column) other).getPath().toString());
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return getPath().toString().hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "[Column " + getPath() + " " + (visible ? "visible" : "not visible") + "]";
    }

    /**
     * Set the Path that this column is displaying
     * @param path the Path
     */
    public void setPath(Path path) {
        this.path = path;
    }
    
    /**
     * Get the Path set by setPath().
     * @return the Path
     */
    public Path getPath() {
        return path;
    }
}
