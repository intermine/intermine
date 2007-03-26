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

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Static methods to assist with query generation from front end.
 *
 * @author Richard Smith
 */
public abstract class QueryHelper
{


    /**
     * Add a constraint to those present in a Query.  If the Query currently
     * has no constraints just add the newConstraint.  If q.getConstraint()
     * returns a ConstraintSet add newConstraint to it.  If q.getConstraint()
     * returns something that isn't a ConstraintSet make and add a new
     * ConstraintSet that contains both the old and new constraints.
     *
     * @param q the query in question
     * @param constraint the constraint to add
     */
    public static void addConstraint(Query q, Constraint constraint) {
        if (q == null) {
            throw new NullPointerException("q cannot be null");
        }
        if (constraint == null) {
            throw new NullPointerException("constraint cannot be null");
        }

        Constraint queryConstraint = q.getConstraint();

        if (queryConstraint == null) {
            q.setConstraint(constraint);
        } else if (queryConstraint instanceof ConstraintSet) {
            // add all constraints, avoid nesting ConstraintSets
            if (constraint instanceof ConstraintSet) {
                Iterator iter = ((ConstraintSet) constraint).getConstraints().iterator();
                while (iter.hasNext()) {
                    ((ConstraintSet) queryConstraint).addConstraint((Constraint) iter.next());
                }
            } else {
                ((ConstraintSet) queryConstraint).addConstraint(constraint);
            }
        } else { // any other type of constraint, avoid nesting ConstraintSets
            ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);
            constraints.addConstraint(queryConstraint);
            if (constraint instanceof ConstraintSet) {
                Iterator iter = ((ConstraintSet) constraint).getConstraints().iterator();
                while (iter.hasNext()) {
                   constraints.addConstraint((Constraint) iter.next());
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
    public static List getColumnAliases(Query query) {
        List columnAliases = new ArrayList();
        Iterator selectIter = query.getSelect().iterator();
        while (selectIter.hasNext()) {
            QuerySelectable node = (QuerySelectable) selectIter.next();
            String alias = (String) query.getAliases().get(node);
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
    public static List getColumnTypes(Query query) {
        List columnTypes = new ArrayList();
        Iterator selectIter = query.getSelect().iterator();
        while (selectIter.hasNext()) {
            QuerySelectable node = (QuerySelectable) selectIter.next();
            Class type = node.getType();
            columnTypes.add(type);
        }
        return columnTypes;
    }
}
