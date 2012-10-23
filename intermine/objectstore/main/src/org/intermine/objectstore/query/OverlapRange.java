package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Represents a range value on an object.
 *
 * @author Matthew Wakeling
 */
public class OverlapRange
{
    protected QueryEvaluable start, end;
    protected QueryObjectReference parent;

    /**
     * Constructor for this object.
     *
     * @param start the start value of this range, inclusive, of type int
     * @param end the end value of this range, inclusive, of type int
     * @param parent the object that the range is associated with
     * @throws IllegalArgumentException if one of the parameters is invalid
     */
    public OverlapRange(QueryEvaluable start, QueryEvaluable end, QueryObjectReference parent) {
        if (start == null) {
            throw new NullPointerException("Start parameter cannot be null");
        }
        if (end == null) {
            throw new NullPointerException("End parameter cannot be null");
        }
        if (parent == null) {
            throw new NullPointerException("Parent parameter cannot be null");
        }
        if (start.getType().equals(UnknownTypeValue.class)) {
            start.youAreType(Integer.class);
        }
        if (end.getType().equals(UnknownTypeValue.class)) {
            end.youAreType(Integer.class);
        }
        if (!Integer.class.equals(start.getType())) {
            throw new IllegalArgumentException("Start parameter (" + start.getType()
                    + ") is not an Integer");
        }
        if (!Integer.class.equals(end.getType())) {
            throw new IllegalArgumentException("End parameter (" + end.getType()
                    + ") is not an Integer");
        }
        this.start = start;
        this.end = end;
        this.parent = parent;
    }

    /**
     * Returns the left parameter.
     *
     * @return a QueryEvaluable
     */
    public QueryEvaluable getStart() {
        return start;
    }

    /**
     * Returns the right parameter.
     *
     * @return a QueryEvaluable
     */
    public QueryEvaluable getEnd() {
        return end;
    }

    /**
     * Returns the parent parameter.
     *
     * @return a QueryReference
     */
    public QueryObjectReference getParent() {
        return parent;
    }
}
