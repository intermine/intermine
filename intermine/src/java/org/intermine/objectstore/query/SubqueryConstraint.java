package org.flymine.objectstore.query;

import java.util.List;

/**
 * Constrain a QueryClass or QueryEvaluable to be within the select list
 * of a given subquery.
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */


public class SubqueryConstraint implements Constraint
{
    /**
     * require that argument is contained within select of subquery
     */
    public static final int CONTAINS = 1;

    /**
     * require that argument is not contained in select of subquery
     */
    public static final int DOES_NOT_CONTAIN = 2;

    protected boolean negated;
    protected Query subquery;
    protected QueryClass cls;
    protected QueryEvaluable qe;
    protected int type;


    /**
     * Construct a SubqueryConstraint with a QueryEvaluable
     *
     * @param query the subquery in question
     * @param type required type of constraint
     * @param qe item to match against subquery select
     */
    public SubqueryConstraint(Query query, int type, QueryEvaluable qe) {
        this(query, type, qe, false);
    }

    /**
     * Construct a SubqueryConstraint with a QueryEvaluable
     *
     * @param query the subquery in question
     * @param type required type of constraint
     * @param qe item to match against subquery select
     * @param negated reverse the constraint logic if true
     */
    public SubqueryConstraint(Query query, int type, QueryEvaluable qe, boolean negated) {

        if (query == null) {
            throw (new NullPointerException("query cannot be null"));
        }
        if (qe == null) {
            throw (new NullPointerException("qe cannot be null"));
        }
        if ((type < 1) || (type > 2)) {
            throw (new IllegalArgumentException("Invalid type: " + type));
        }

        // check that query only has one item in select list
        List select = query.getSelect();
        if (select.size() < 1) {
            throw (new IllegalArgumentException("Query has no items in select list."));
        }
        if (select.size() > 1) {
            throw (new IllegalArgumentException("Subquery must have only one select item."));
        }

        // check that the select node is a QueryEvaluable
        QueryNode selectNode = (QueryNode) select.get(0);
        if (!QueryEvaluable.class.isAssignableFrom(selectNode.getClass())) {
            throw (new IllegalArgumentException("Subquery select item is not a QueryEvalubale"));
        }

        // check that java types of QueryEvaluables are comparable
        if (!(selectNode.getType().isAssignableFrom(qe.getType()))
            && !(qe.getType().isAssignableFrom(selectNode.getType()))) {
            throw (new IllegalArgumentException("Type of select from subquery ("
                                                + selectNode.getType()
                                                + ") not comparable with type from argument ("
                                                + qe.getType() + ")"));
        }
        this.subquery = query;
        this.qe = qe;
        this.negated = negated;
        this.cls = null;
        this.type = type;
    }

    /**
     * Construct a SubqueryConstraint with a QueryClass
     *
     * @param query the subquery in question
     * @param type required type of constraint
     * @param cls item to match against subquery select
     */
    public SubqueryConstraint(Query query, int type, QueryClass cls) {
        this(query, type, cls, false);
    }

    /**
     * Construct a SubqueryConstraint with a QueryClass
     *
     * @param query the subquery in question
     * @param type required type of constraint
     * @param cls item to match against subquery select
     * @param negated reverse the constraint logic if true
     */
    public SubqueryConstraint(Query query, int type, QueryClass cls, boolean negated) {
        if (query == null) {
            throw (new NullPointerException("query cannot be null"));
        }
        if (cls == null) {
            throw (new NullPointerException("cls cannot be null"));
        }
        if ((type < 1) || (type > 2)) {
            throw (new IllegalArgumentException("Invalid type: " + type));
        }

        // check that query only has one item in select list and it is a QueryClass
        List select = query.getSelect();
        if (select.size() < 1) {
            throw (new IllegalArgumentException("Query has no items in select list."));
        }
        if (select.size() > 1) {
            throw (new IllegalArgumentException("Subquery must have only one "
                                                + "item in select list."));
        }
        QueryNode selectNode = (QueryNode) select.get(0);
        if (!QueryClass.class.isAssignableFrom(selectNode.getClass())) {
            throw (new IllegalArgumentException("Select item of subquery is not a QueryClass"));
        }
        if (!(selectNode.getType().isAssignableFrom(cls.getType()))
            && !(cls.getType().isAssignableFrom(selectNode.getType()))) {
            throw (new IllegalArgumentException("QueryClass select from subquery ("
                                                + selectNode.getType()
                                                + ") not comparable with QueryClass ("
                                                + cls.getType() + ")"));
        }

        this.subquery = query;
        this.cls = cls;
        this.negated = negated;
        this.qe = null;
        this.type = type;
    }

    /**
     * Set whether constraint is negated.  Negated reverses the logic of the constraint
     * i.e equals becomes not equals.
     *
     * @param negated true if constraint logic to be reversed
     */
    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    /**
     * Test if constraint logic has been reversed
     *
     * @return true if constraint is negated
     */
    public boolean isNegated() {
        return negated;
    }


    /**
     * Get type of operation (i.e. contains or does not contain)
     *
     * @return type of operation
     */
    public int getType() {
        return type;
    }

    /**
     * Get the query.
     *
     * @return the subquery of the constraint
     */
    public Query getQuery() {
        return subquery;
    }

    /**
     * Get the QueryEvaluable the query is compared with
     *
     * @return QueryEvaluable
     */
    public QueryEvaluable getQueryEvaluable() {
        return qe;
    }

    /**
     * Get the QueryClass the query is compare with
     *
     * @return QueryClass
     */
    public QueryClass getQueryClass() {
        return cls;
    }

    /**
     * Returns a boolean whether or not the constraint is effectively "NOT IN", rather than "IN".
     *
     * @return true if the query is NOT IN
     */
    public boolean isNotIn() {
        return (type == 1 ? negated : !negated);
    }
}
