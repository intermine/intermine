package org.intermine.objectstore.query;

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
 * An element that can be evaluated for comparison (one that represents an atomic type)
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public interface QueryEvaluable extends QueryNode
{
    /**
     * Allows a caller to suggest to this object that it holds a value of a certain type.
     * This method should only be called on objects which report their type to be UnknownTypeValue.
     * Otherwise, this method will throw and exception.
     *
     * @param cls the Class of the type to be imposed on this object
     */
    public void youAreType(Class cls);

    /**
     * Returns an integer representing the approximate type of this QueryEvaluable, in the case
     * where an accurate type cannot be ascertained.
     *
     * @return an int, as described in UnknownTypeValue
     */
    public int getApproximateType();
}
