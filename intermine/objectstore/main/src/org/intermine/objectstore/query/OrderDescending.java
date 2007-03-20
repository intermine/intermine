package org.intermine.objectstore.query;

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
 * An element that can appear in the ORDER BY clause of a query, to reverse the order.
 *
 * @author Matthew Wakeling
 */
public class OrderDescending implements QueryOrderable
{
    private QueryOrderable qo;

    /**
     * Creates a new OrderDescending object from the given QueryOrderable.
     *
     * @param qo a QueryOrderable
     * @throws IllegalArgumentException if the argument is already an OrderDescending
     */
    public OrderDescending(QueryOrderable qo) {
        if (qo instanceof OrderDescending) {
            throw new IllegalArgumentException("Cannot nest OrderDescending objects");
        }
        this.qo = qo;
    }

    /**
     * Return the encapsulated QueryOrderable.
     *
     * @return qo
     */
    public QueryOrderable getQueryOrderable() {
        return qo;
    }

    /**
     * Get Java type represented by this item.
     *
     * @return class describing the type
     */
    public Class getType() {
        return qo.getType();
    }
}
