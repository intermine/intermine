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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.Query;

/**
 * Static class, includes methods to create a List of PrintableConstraint objects from a Constraint.
 *
 * @author Matthew Wakeling
 */
public class ConstraintListCreator
{
    /**
     * Converts a constraint from a query into a List of PrintableConstraint objects.
     *
     * @param query a Query object to embed into PrintableConstraint objects
     * @return a List of PrintableConstraint objects
     */
    public static List createList(Query query) {
        List retval = new ArrayList();
        if (query != null) {
            addToList(retval, query, query.getConstraint());
        }
        return retval;
    }

    /**
     * Adds all the constraints present in the argument into the given List, as PrintableConstraint
     * objects.
     *
     * @param list a List of PrintableConstraints, to which to add more entries
     * @param query a Query object to embed into PrintableConstraint objects
     * @param constraint a Constraint to pick apart
     */
    public static void addToList(List list, Query query, Constraint constraint) {
        if (constraint != null) {
            if (constraint instanceof ConstraintSet) {
                if ((!((ConstraintSet) constraint).getDisjunctive())
                        && (!((ConstraintSet) constraint).isNegated())) {
                    Set constraints = ((ConstraintSet) constraint).getConstraints();
                    Iterator conIter = constraints.iterator();
                    while (conIter.hasNext()) {
                        addToList(list, query, (Constraint) conIter.next());
                    }
                } else {
                    list.add(new PrintableConstraint(query, (ConstraintSet) constraint));
                }
            } else {
                list.add(new AssociatedConstraint(query, constraint));
            }
        }
    }
}
