package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

/**
 * A representation of an item that can be present in the ORDER BY section of an SQL query, but
 * ordered descending..
 *
 * @author Matthew Wakeling
 */
public class OrderDescending extends AbstractValue
{
    private AbstractValue value;

    /**
     * Constructor for OrderDescending.
     *
     * @param value an AbstractValue
     */
    public OrderDescending(AbstractValue value) {
        if (value instanceof OrderDescending) {
            throw new IllegalArgumentException("Cannot nest OrderDescending objects");
        }
        this.value = value;
    }

    /**
     * Returns the value.
     *
     * @return an AbstractValue
     */
    public AbstractValue getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public String getSQLString() {
        return value.getSQLString() + " DESC";
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof OrderDescending) {
            return value.equals(((OrderDescending) o).getValue());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return value.hashCode() + 1;
    }

    /**
     * {@inheritDoc}
     */
    public int compare(AbstractValue obj, Map tableMap, Map reverseTableMap) {
        throw new UnsupportedOperationException("Cannot compare OrderDescending objects");
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return getSQLString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAggregate() {
        return value.isAggregate();
    }
}
