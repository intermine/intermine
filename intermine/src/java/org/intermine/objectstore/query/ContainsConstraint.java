package org.flymine.objectstore.query;



/**
 * Constrain whether a QueryClass is member of a QueryReference or not.
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */

public class ContainsConstraint implements Constraint
{

    /**
     * QueryCollection does contain the specified QueryClass.
     */
    public static final int CONTAINS = 1;

    /**
     * QueryCollection does not contain the specified QueryClass.
     */
    public static final int DOES_NOT_CONTAIN = 2;

    protected boolean negated;
    protected QueryReference ref;
    protected QueryClass cls;
    protected int type;

    /**
     * Constructor for ContainsConstraint.
     *
     * @param ref the target QueryReference
     * @param type specify CONTAINS or DOES_NOT_CONTAIN
     * @param cls the QueryClass to be tested
     */
    public ContainsConstraint(QueryReference ref, int type, QueryClass cls) {
        this(ref, type, cls, false);
    }

    /**
     * Constructor for ContainsConstraint.
     *
     * @param ref the target QueryReference
     * @param type specify CONTAINS or DOES_NOT_CONTAIN
     * @param cls the QueryClass to be tested
     * @param negated reverse the constraint logic if true
     */
    public ContainsConstraint(QueryReference ref, int type, QueryClass cls, boolean negated) {
        if (ref == null) {
            throw (new NullPointerException("ref cannot be null"));
        }
        if (cls == null) {
            throw (new NullPointerException("cls cannot be null"));
        }

        this.ref = ref;
        this.cls = cls;
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



