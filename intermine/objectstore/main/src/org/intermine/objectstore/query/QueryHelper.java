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

import java.util.ArrayList;
import java.util.List;

/**
 * Static methods to assist with query generation from front end.
 *
 * @author Richard Smith
 */
public abstract class QueryHelper
{
    private QueryHelper() {
    }

    /**
     * Add a constraint to be ANDed with those present in a Query.  If the Query currently
     * has no constraints just add the newConstraint.  If q.getConstraint()
     * returns a ConstraintSet of type AND, add newConstraint to it.  If q.getConstraint()
     * returns something make other than an AND constraint, add new
     * ConstraintSet of type AND that contains both the old and new constraints.
     *
     * @param q the query in question
     * @param constraint the constraint to add
     */
    public static void addAndConstraint(Query q, Constraint constraint) {
        if (q == null) {
            throw new NullPointerException("q cannot be null");
        }
        if (constraint == null) {
            throw new NullPointerException("constraint cannot be null");
        }

        Constraint queryConstraint = q.getConstraint();

        if (queryConstraint == null) {
            q.setConstraint(constraint);
        } else if (queryConstraint instanceof ConstraintSet
                   && queryConstraint.getOp().equals(ConstraintOp.AND)) {
            // add all constraints, avoid nesting ConstraintSets
            if (constraint instanceof ConstraintSet
                && constraint.getOp().equals(ConstraintOp.AND)) {
                for (Constraint subC : ((ConstraintSet) constraint).getConstraints()) {
                    ((ConstraintSet) queryConstraint).addConstraint(subC);
                }
            } else {
                ((ConstraintSet) queryConstraint).addConstraint(constraint);
            }
        } else { // any other type of constraint, avoid nesting ConstraintSets
            ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);
            constraints.addConstraint(queryConstraint);
            if (constraint instanceof ConstraintSet
                && constraint.getOp().equals(ConstraintOp.AND)) {
                for (Constraint subC : ((ConstraintSet) constraint).getConstraints()) {
                    constraints.addConstraint(subC);
                }
            } else {
                constraints.addConstraint(constraint);
            }

            q.setConstraint(constraints);
        }
    }

    /**
     * Returns a list of aliases, where each alias corresponds to each element of the SELECT list
     * of the Query object. This is effectively a list of column headings for the results object.
     * @param query the Query object
     * @return a List of Strings, each of which is the alias of the column
     */
    public static List<String> getColumnAliases(Query query) {
        List<String> columnAliases = new ArrayList<String>();
        for (QuerySelectable node : query.getSelect()) {
            String alias = query.getAliases().get(node);
            columnAliases.add(alias);
        }
        return columnAliases;
    }

    /**
     * Returns a list of Class objects, where each object corresponds to the type of each element
     * of the SELECT list of the Query object. This is effectively a list of column types for the
     * results object.
     * @param query the Query object
     * @return a List of Class objects
     */
    public static List<Class<?>> getColumnTypes(Query query) {
        List<Class<?>> columnTypes = new ArrayList<Class<?>>();
        for (QuerySelectable node : query.getSelect()) {
            Class<?> type = node.getType();
            columnTypes.add(type);
        }
        return columnTypes;
    }
}
