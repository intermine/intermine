package org.flymine.objectstore.query;

/**
 * Constrain whether a QueryClass is member of a QueryReference or not.
 * QueryReference can refer to an object or a collection, test whether
 * QueryClass is a member of the collection or an instance of the object
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
     * @param cls the QueryClass to to be tested against reference
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

        if (QueryObjectReference.class.isAssignableFrom(ref.getClass())) {
            if (ref.getType() != cls.getType()) {
                throw (new IllegalArgumentException("QueryObjectReference type(" + ref.getType()
                                                    + ") not equal to QueryClass type("
                                                    + cls.getType() + ")"));
            }
        }

        if ((type < 1) || (type > 2)) {
            throw (new IllegalArgumentException("Invalid value for type: " + type));
        }

        this.ref = ref;
        this.cls = cls;
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

    /**
     * Returns the QueryReference of the constraint.
     *
     * @return the QueryReference
     */
    public QueryReference getReference() {
        return ref;
    }

    /**
     * Returns the QueryClass of the constraint.
     *
     * @return the QueryClass
     */
    public QueryClass getQueryClass() {
        return cls;
    }

    /**
     * Returns true if the constraint is effectively "DOES_NOT_CONTAIN", taking negated into
     * account.
     *
     * @return true if it is DOES_NOT_CONTAIN
     */
    public boolean isNotContains() {
        return (type == CONTAINS ? negated : !negated);
    }

    /**
     * Test whether two ContainsConstraints are equal, overrides Object.equals()
     *
     * @param obj the object to compare with
     * @return true if objects are equal
     */
    public boolean equals(Object obj) {
        if (obj instanceof ContainsConstraint) {
            ContainsConstraint cc = (ContainsConstraint) obj;
            return (ref.equals(cc.ref)
                    && (type == cc.type)
                    && (negated == cc.negated)
                    && cls.equals(cc.cls));
        }
        return false;
    }


    /**
     * Get the hashCode for this object, overrides Object.hashCode()
     *
     * @return the hashCode
     */
    public int hashCode() {
        return ref.hashCode() + (3 * type) + ((negated) ? 29 : 0)
            + (5 * cls.hashCode());
    }


}
