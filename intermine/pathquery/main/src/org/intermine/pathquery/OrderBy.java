package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.path.Path;

/**
 * A representation of a field in the order by list
 * @author Julie Sullivan
 */
public class OrderBy
{
    private Path field;
    private String direction;

    /**
     * Constructs a new order by with the default direction, ascending
     * @param field A field in the query
     */
    public OrderBy(Path field) {
        this.field = field;
        this.direction = "asc";
    }

    /**
     * Construct a new instance
     * @param field A field in the query
     * @param direction Either ascending or descending
     */
    public OrderBy(Path field, String direction) {
        this.field = field;
        this.direction = direction;
    }

    /**
     * Construct a new instance
     * @param field A field in the query
     * @param sortAscending true = sort ascending, false = sort descending
     */
    public OrderBy(Path field, Boolean sortAscending) {
        this.field = field;
        direction = (sortAscending.booleanValue() ? "asc" : "desc");
    }

    /**
     * Returns which way this field will be sorted, ascending or descending
     * @return direction Either asc or desc
     */
    public String getDirection() {
        return direction;
    }

    /**
     * Set which way this field will be sorted, ascending or descending
     * @param direction Either asc or desc
     */
    public void setDirection(String direction) {
        this.direction = direction;
    }

    /**
     * Returns which field will be sorted
     * @return which field will be sorted
     */
    public Path getField() {
        return field;
    }

    /**
     * @param field Field to be sorted
     */
    public void setSortField(Path field) {
        this.field = field;
    }

    /**
     * Returns a representation of the Path and direction as a String
     * @return a String version of the order by
     */
    public String toString() {
        return field + " " + direction;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return (o instanceof OrderBy)
            && field.equals(((OrderBy) o).field)
            && direction.equals(((OrderBy) o).direction);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return 2 * field.hashCode() + 3 * direction.hashCode();
    }
}
