package org.flymine.objectstore.query;

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

}
