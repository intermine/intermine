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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.intermine.metadata.*;

/**
 * Static methods to assist with query generation from front end.
 *
 * @author Richard Smith
 */
public abstract class QueryHelper
{
    /**
     * Add a SimpleConstraint to a Query
     *
     * @param q a query to add QueryClass and constraints to
     * @param fieldName name of the attribute in the QueryClass to constraint
     * @param qc QueryClass to pass to the Constraint constructor
     * @param op operation for the new Constraint
     * @param qv the QueryValue to pass to the SimpleConstraint constructor
     * @throws Exception if an error occurs
     */
    public static void addConstraint(Query q, String fieldName, QueryClass qc, ConstraintOp op,
                                     QueryValue qv) throws Exception {
        if (op == null) {
            throw new NullPointerException("ConstraintOp parameter is null");
        } else if (qv == null) {
            throw new NullPointerException("QueryValue parameter is null");
        }

        QueryField qf = new QueryField(qc, fieldName);
        addConstraint(q, qc, new SimpleConstraint(qf, op, qv));
    }

    /**
     * Add a QueryField BagConstraint to a Query
     *
     * @param q a query to add QueryClass and constraints to
     * @param fieldName name of the attribute in the QueryClass to constraint
     * @param qc QueryClass to pass to the Constraint constructor
     * @param op operation for the new Constraint
     * @param collection the Collection to pass to the BagConstraint constructor
     * @throws Exception if an error occurs
     */
    public static void addConstraint(Query q, String fieldName, QueryClass qc, ConstraintOp op,
                                     Collection collection) throws Exception {
        if (qc == null) {
            throw new NullPointerException("QueryField qf parameter is null");
        } else if (fieldName == null) {
            throw new NullPointerException("fieldName parameter is null");
        } else if (op == null) {
            throw new NullPointerException("ConstraintOp parameter is null");
        } else if (collection == null) {
            throw new NullPointerException("Collection parameter is null");
        }

        QueryField qf = new QueryField(qc, fieldName);
        addConstraint(q, qc, new BagConstraint(qf, op, collection));
    }

    /**
     * Add a constraint to those present in a Query.  If the Query currently
     * has no constraints just add the newConstraint.  If q.getConstraint()
     * returns a ConstraintSet add newConstraint to it.  If q.getConstraint()
     * returns something that isn't a ConstraintSet make and add a new
     * ConstraintSet that contains both the old and new constraints.  If the
     * QueryClass
     *
     * @param q the query in question
     * @param qc the QueryClass referred to by the Constraint
     * @param constraint the constraint to add
     */
    public static void addConstraint(Query q, QueryClass qc, Constraint constraint) {
        if (q == null) {
            throw new NullPointerException("q cannot be null");
        }
        if (qc == null) {
            throw new NullPointerException("qc cannot be null");
        }
        if (constraint == null) {
            throw new NullPointerException("constraint cannot be null");
        }

        addQueryClass(q, qc);

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
     * Add a QueryClass to a Query.  Do nothing if it's already there.
     *
     * @param q a query to add QueryClass to
     * @param qc QueryClass to add
     */
    public static void addQueryClass(Query q, QueryClass qc) {
        if (!q.getFrom().contains(qc)) {
            q.addFrom(qc);
            q.addToSelect(qc);
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
            QueryNode node = (QueryNode) selectIter.next();
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
            QueryNode node = (QueryNode) selectIter.next();
            Class type = node.getType();
            columnTypes.add(type);
        }
        return columnTypes;
    }
}
