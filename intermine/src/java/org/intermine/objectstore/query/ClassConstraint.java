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

}
