package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Arrays;

/**
 * Constrain whether a QueryClass is member of a QueryReference or not.
 * QueryReference can refer to an object or a collection, test whether
 * QueryClass is a member of the collection or an instance of the object
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */
public class ContainsConstraint extends Constraint
{
    protected QueryReference ref;
    protected QueryClass cls;
    protected ConstraintOp type;

    /**
     * Constructor for ContainsConstraint.
     *
     * @param ref the target QueryReference
     * @param type specify CONTAINS or DOES_NOT_CONTAIN
     * @param cls the QueryClass to to be tested against reference
     */
    public ContainsConstraint(QueryReference ref, ConstraintOp type, QueryClass cls) {
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
    public ContainsConstraint(QueryReference ref, ConstraintOp type, QueryClass cls,
                              boolean negated) {
        if (ref == null) {
            throw new NullPointerException("ref cannot be null");
        }

        if (type == null) {
            throw new NullPointerException("type cannot be null");
        }

        if (!validOps().contains(type)) {
            throw new IllegalArgumentException("type cannot be " + type);
        }

        if (cls == null) {
            throw new NullPointerException("cls cannot be null");
        }

        if (ref instanceof QueryObjectReference && !ref.getType().equals(cls.getType())) {
            throw new IllegalArgumentException("Invalid constraint: "
                                               + ref.getType()
                                               + " " + type
                                               + " " + cls.getType());
        }
        
        this.ref = ref;
        this.type = type;
        this.cls = cls;
        this.negated = negated;
    }

    /**
     * Return the operation type
     *
     * @return the operation type
     */
    public ConstraintOp getType() {
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
            return ref.equals(cc.ref)
                    && type == cc.type
                    && negated == cc.negated
                    && cls.equals(cc.cls);
        }
        return false;
    }

    /**
     * Get the hashCode for this object, overrides Object.hashCode()
     *
     * @return the hashCode
     */
    public int hashCode() {
        return ref.hashCode()
            + 3 * type.hashCode()
            + 5 * (negated ? 1 : 0)
            + 7 * cls.hashCode();
    }

    //-------------------------------------------------------------------------
    
    /**
     * QueryCollection does contain the specified QueryClass.
     */
    public static final ConstraintOp CONTAINS = ConstraintOp.CONTAINS;

    /**
     * QueryCollection does not contain the specified QueryClass.
     */
    public static final ConstraintOp DOES_NOT_CONTAIN = ConstraintOp.DOES_NOT_CONTAIN;

    protected static final ConstraintOp[] VALID_OPS = new ConstraintOp[] {CONTAINS,
                                                                          DOES_NOT_CONTAIN};

    /**
     * Return a list of the valid operations for constructing a constraint of this type
     * @return a List of operation codes
     */
    public static List validOps() {
        return Arrays.asList(VALID_OPS);
    }
}
