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

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;

/**
 * A CollatedResult wraps the result of a Query along with another
 * Query that can be used by the ObjectStore to retrieve all the
 * objects that this result refers to.
 *
 * In the case of an aggregate this would be (e.g.) the count, along
 * with a query to retrieve the objects counted.
 *
 * CollatedResult is a Results object, which itself is a List, so we have all the
 * usual methods of interacting with the Results.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Andrew Varley
 */
public class CollatedResult extends Results
{
    private Object result;

    /**
     * No argument constructor for testing purposes
     */
    protected CollatedResult() {
    }

    /**
     * Construct a collated result
     *
     * @param row the result row that this is a collated result for
     * @param qn the node in the result row
     * @param q the Query that generated the original result row
     * @param os the ObjectStore to use to run the Query
     * @throws NullPointerException if any parameters are null
     * @throws IllegalArgumentException if qn not in select part of q
     */
    public CollatedResult(ResultsRow row, QueryNode qn, Query q, ObjectStore os) {
         if ((row == null) || (qn == null) || (q == null) || (os == null)) {
             throw new NullPointerException("Arguments must not be null");
         }

         // Test that the row is sensible for the query, ie. has the
         // same number of entries as the select part of query
         if (q.getSelect().size() != row.size()) {
             throw new IllegalArgumentException("row cannot have been generated "
                                                + "by the Query q - it is the wrong length");
         }

         this.query = getMatchesQuery(qn, q, row);
         this.os = os;
         int index = q.getSelect().indexOf(qn);
         if (index < 0) {
             throw new IllegalArgumentException("Cannot find qn in select part of q");
         }
         result = row.get(index);
         if (result == null) {
             throw new NullPointerException("result must not be null");
         }
    }

    /**
     * Get the result
     *
     * @return the result
     */
    public Object getResult() {
        return result;
    }

    /**
     * Build a new Query that will return the matches for a given QueryNode in a Query
     *
     * In OQL:
     * select a, b, count(*)
     * from a, b, c
     * where a.b = b
     * and b.c = c
     * group by a, b
     *
     * would lead to the following Query being built to return the matches for c for a
     * given row
     *
     * select a, b, c
     * from a, b, c
     * where a.b = b
     * and b.c = c
     * and a = <a from row>
     * and b = <b from row>
     *
     * @param qn the QueryNode for which to build a new "get matches" Query
     * @param q the original Query
     * @param row the ResultsRow in the original Query
     * @return the new Query that will return the matches or null if a query cannot be constructed
     */

    protected Query getMatchesQuery(QueryNode qn, Query q, ResultsRow row) {

        Query matchesQuery = new Query();

        // Select all the QueryNodes in original from clause
        Iterator i = q.getFrom().iterator();
        while (i.hasNext()) {
            FromElement fe = (FromElement) i.next();
            if (fe instanceof QueryClass) {
                matchesQuery.addToSelect((QueryClass) fe);
            } else {
                // Have a subquery in the from clause - do not deal with these
                return null;
            }
        }

        // From all the original QueryNodes
        i = q.getFrom().iterator();
        while (i.hasNext()) {
            matchesQuery.addFrom((FromElement) i.next());
        }
        // Where all the original constraints
        ConstraintSet constraintSet = new ConstraintSet(ConstraintOp.AND);
        constraintSet.addConstraint(q.getConstraint());

        // if this query is a "group by" add one for each in the group by
        if (q.getGroupBy().size() > 0) {

            i = q.getGroupBy().iterator();

            while (i.hasNext()) {
                QueryNode node = (QueryNode) i.next();
                // Get the correct result from the ResultRow
                int index = q.getSelect().indexOf(node);

                if (node instanceof QueryClass) {
                    // add a ClassConstraint, ie. this QueryClass = example
                    InterMineObject value = (InterMineObject) row.get(index);
                    constraintSet.addConstraint(new ClassConstraint((QueryClass) node,
                                ConstraintOp.EQUALS, value));
                } else {
                    // Add a new SimpleConstraint
                    QueryValue value = new QueryValue(row.get(index));
                    constraintSet.addConstraint(new SimpleConstraint((QueryEvaluable) node,
                                                                     ConstraintOp.EQUALS,
                                                                     value));
                }
            }

            // If the function is a min/max/avg etc, then also have to select the matching items
            if (qn instanceof QueryFunction) {
                QueryFunction qf = (QueryFunction) qn;
                if ((qf.getOperation() == QueryFunction.AVERAGE)
                    || (qf.getOperation() == QueryFunction.MIN)
                    || (qf.getOperation() == QueryFunction.MAX)) {
                    QueryValue value = new QueryValue(row.get(q.getSelect().indexOf(qn)));
                    QueryEvaluable evaluable = qf.getParam();
                    constraintSet.addConstraint(new SimpleConstraint(evaluable,
                                                                     ConstraintOp.EQUALS,
                                                                     value));
                }
            }
        } else if (q.isDistinct()) {
            // One constraint for each item in the select list
            i = q.getSelect().iterator();
            while (i.hasNext()) {
                QuerySelectable node = (QuerySelectable) i.next();
                int index = q.getSelect().indexOf(node);
                if (node instanceof QueryClass) {
                    // add a ClassConstraint, ie. this QueryClass = example
                    InterMineObject value = (InterMineObject) row.get(index);
                    constraintSet.addConstraint(new ClassConstraint((QueryClass) node,
                                                                    ConstraintOp.EQUALS,
                                                                    value));
                } else {
                    QueryValue value = new QueryValue(row.get(index));
                    constraintSet.addConstraint(new SimpleConstraint((QueryField) node,
                                                                     ConstraintOp.EQUALS,
                                                                     value));
                }
            }
        } else {
            return null;
        }

        matchesQuery.setConstraint(constraintSet);

        // No group by
        // No having
        // No order by (how do we know what order is wanted)

        return matchesQuery;

    }

}
