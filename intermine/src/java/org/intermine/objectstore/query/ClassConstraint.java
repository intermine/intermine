package org.flymine.objectstore.query;

/**
 * Constrain whether a QueryClass is equal/not equal to another
 * QueryClass or an example of an object belonging to a
 * QueryClass. Note: QueryClass = QueryClass makes no sense, but is
 * allowed.
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 * @author Andrew Varley
 */

public class ClassConstraint implements Constraint
{

    /**
     * Classes are equal to one another
     */
    public static final int EQUALS = 1;

    /**
     * Classes are not equal to one another
     */
    public static final int NOT_EQUALS = 2;

    protected boolean negated;
    protected QueryClass qc1;
    protected QueryClass qc2;
    protected Object obj;
    protected int type;

    /**
     * Construct ClassConstraint
     *
     * @param qc1 first QueryClass for comparison
     * @param type define EQUALS or NOT_EQUALS
     * @param qc2 second QueryClass for comparison
     */
    public ClassConstraint(QueryClass qc1, int type, QueryClass qc2) {
        this(qc1, type, qc2, false);
    }

    /**
     * Construct ClassConstraint
     *
     * @param qc1 first QueryClass for comparison
     * @param type define EQUALS or NOT_EQUALS
     * @param qc2 second QueryClass for comparison
     * @param negated reverse the constraint logic if true
     */
    public ClassConstraint(QueryClass qc1, int type, QueryClass qc2, boolean negated) {
        if (qc1 == null) {
            throw (new NullPointerException("qc1 cannot be null"));
        }
        if (qc2 == null) {
            throw (new NullPointerException("qc2 cannot be null"));
        }

        if ((!(qc1.getType().isAssignableFrom(qc2.getType()))
             && !(qc2.getType().isAssignableFrom(qc1.getType())))) {
            throw (new IllegalArgumentException("Classes: " + qc1.getType() + " and "
                                                + qc2.getType() + "cannot be compared"));
        }
        this.qc1 = qc1;
        this.qc2 = qc2;
        if ((type < 1) || (type > 2)) {
            throw (new IllegalArgumentException("Invalid value for type: " + type));
        }
        this.type = type;
        this.negated = negated;
        this.obj = null;
    }

    /**
     * Construct ClassConstraint
     *
     * @param qc QueryClass for comparison
     * @param type define EQUALS or NOT_EQUALS
     * @param obj example object
     */
    public ClassConstraint(QueryClass qc, int type, Object obj) {
        this(qc, type, obj, false);
    }

    /**
     * Construct ClassConstraint
     *
     * @param qc QueryClass for comparison
     * @param type define EQUALS or NOT_EQUALS
     * @param obj example object
     * @param negated reverse the constraint logic if true
     */
    public ClassConstraint(QueryClass qc, int type, Object obj, boolean negated) {
        if (qc == null) {
            throw (new NullPointerException("obj cannot be null"));
        }
        if (obj == null) {
            throw (new NullPointerException("obj cannot be null"));
        }

        if ((!(qc.getType().isAssignableFrom(obj.getClass()))
             && !(obj.getClass().isAssignableFrom(qc.getType())))) {
            throw (new IllegalArgumentException("Classes: " + qc.getType() + " and "
                                                + obj.getClass() + "cannot be compared"));
        }
        this.qc1 = qc;
        this.obj = obj;
        if ((type < 1) || (type > 2)) {
            throw (new IllegalArgumentException("Invalid value for type: " + type));
        }
        this.type = type;
        this.negated = negated;
        this.qc2 = null;
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
     * Return the operation type
     *
     * @return the operation type
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the QueryClass argument 1
     *
     * @return QueryClass arg1
     */
    public QueryClass getArg1() {
        return qc1;
    }

    /**
     * Returns the QueryClass argument 2
     *
     * @return QueryClass arg2
     */
    public QueryClass getArg2QueryClass() {
        return qc2;
    }

    /**
     * Returns the Object argument 2
     *
     * @return Object arg2
     */
    public Object getArg2Object() {
        return obj;
    }

    /**
     * Tests whether two ClassConstraints are equal.
     *
     * @param obj the object to compare with
     * @return true if objects are equal
     */
    public boolean equals(Object obj) {
        if (obj instanceof ClassConstraint) {
            ClassConstraint objCC = (ClassConstraint) obj;
            return  (qc1.equals(objCC.qc1)
                     && (type == objCC.type)
                     && ((qc2 != null) ? qc2.equals(objCC.qc2) : (objCC.qc2 == null))
                     && ((this.obj != null) ? this.obj.equals(objCC.obj) : (objCC.obj == null)));
        }
        return false;
    }

    /**
     * Get the hashCode for this object
     *
     * @return the hashCode
     */
    public int hashCode() {
        return qc1.hashCode() + (3 * type)
            + (5 * ((qc2 != null) ? qc2.hashCode() : obj.hashCode()));
    }

    /**
     * Returns a boolean whether or not the constraint is effectively "NOT EQUALS", rather than
     * "EQUALS".
     *
     * @return true if the the query is NOT EQUALS
     */
    public boolean isNotEqual() {
        return (type == EQUALS ? negated : !negated);
    }
}
