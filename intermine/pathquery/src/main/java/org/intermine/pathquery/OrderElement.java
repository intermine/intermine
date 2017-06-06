package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * An element of the order by list of a PathQuery, containing a path and a direction. This class
 * is immutable.
 *
 * @author Matthew Wakeling
 */
public class OrderElement
{
    private String orderPath;
    private OrderDirection direction;

    /**
     * Constructor, taking a path and a direction. The path should be a normal path expression
     * with dots separating the parts. Do not use colons to represent outer joins, and do not
     * use square brackets to represent subclass constraints. The path will be checked for
     * format.
     *
     * @param orderPath the path to order by
     * @param direction the direction to order in
     * @throws NullPointerException if orderPath or direction are null
     * @throws IllegalArgumentException if the orderPath contains colons or square brackets, or
     * is otherwise in a bad format
     */
    public OrderElement(String orderPath, OrderDirection direction) {
        if (orderPath == null) {
            throw new NullPointerException("Cannot create an OrderElement with a null orderPath");
        }
        if (direction == null) {
            throw new NullPointerException("Cannot create an OrderElement with a null direction");
        }
        PathQuery.checkPathFormat(orderPath);
        this.orderPath = orderPath;
        this.direction = direction;
    }

    /**
     * Returns the path of this OrderElement.
     *
     * @return a String path
     */
    public String getOrderPath() {
        return orderPath;
    }

    /**
     * Returns the order direction of this OrderElement.
     *
     * @return an OrderDirection
     */
    public OrderDirection getDirection() {
        return direction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return orderPath.hashCode() + direction.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof OrderElement) {
            OrderElement oe = (OrderElement) o;
            return orderPath.equals(oe.orderPath) && direction.equals(oe.direction);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return orderPath + " " + direction;
    }
}
