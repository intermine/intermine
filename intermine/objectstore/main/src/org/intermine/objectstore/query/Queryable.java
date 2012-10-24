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

import java.util.List;

/**
 * An interface for an object that has a SELECT list and a WHERE clause, like a Query.
 *
 * @author Matthew Wakeling
 */
public interface Queryable
{
    /**
     * Adds an element to the SELECT list.
     *
     * @param selectable a QuerySelectable
     */
    void addToSelect(QuerySelectable selectable);

    /**
     * Returns the SELECT list.
     *
     * @return a List
     */
    List<QuerySelectable> getSelect();

    /**
     * Sets the additional constraint.
     *
     * @param c a Constraint
     */
    void setConstraint(Constraint c);

    /**
     * Returns the additional constraint.
     *
     * @return a Constraint
     */
    Constraint getConstraint();
}
