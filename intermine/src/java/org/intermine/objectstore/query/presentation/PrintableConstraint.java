package org.flymine.objectstore.query.presentation;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.FromElement;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.fql.FqlQuery;

import java.util.ArrayList;

/**
 * Printable representation of a query constraint.
 *
 * @author Matthew Wakeling
 */
public class PrintableConstraint
{
    protected Query query;
    protected Constraint constraint;
    
    /**
     * No-argument constructor, to allow overriding.
     */
    protected PrintableConstraint() {
    }

    /**
     * Constructor, takes a Query and a Constraint.
     *
     * @param query a Query with which to look up aliases
     * @param constraint a ConstraintSet to encapsulate
     */
    public PrintableConstraint(Query query, ConstraintSet constraint) {
        this.query = query;
        this.constraint = constraint;
    }

    /**
     * Returns a String describing the constraint.
     *
     * @return a String
     */
    public String toString() {
        return FqlQuery.constraintToString(query, constraint, new ArrayList());
    }

    /**
     * Returns true if this constraint is associated with the given FromElement.
     *
     * @param fromItem the FromElement to check
     * @return true if associated
     */
    public boolean isAssociatedWith(FromElement fromItem) {
        return false;
    }

    /**
     * Returns true if this constraint is associated with no particular FromElement.
     *
     * @return true if no particular FromElement is associated
     */
    public boolean isAssociatedWithNothing() {
        return true;
    }
}
