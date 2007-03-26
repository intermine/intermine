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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.Util;

/**
 * Constrain whether a QueryClass is member of a QueryReference or not.
 * QueryReference can refer to an object or a collection, test whether
 * QueryClass is a member of the collection or an instance of the object
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 */
public class ContainsConstraint extends Constraint
{
    protected QueryReference ref;
    protected QueryClass cls;
    protected InterMineObject obj;

    /**
     * Constructor for ContainsConstraint.
     *
     * @param ref the target QueryReference
     * @param op specify CONTAINS or DOES_NOT_CONTAIN
     * @param cls the QueryClass to to be tested against reference
     */
    public ContainsConstraint(QueryReference ref, ConstraintOp op, QueryClass cls) {
        if (ref == null) {
            throw new NullPointerException("ref cannot be null");
        }

        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }

        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("op cannot be " + op);
        }

        if (cls == null) {
            throw new NullPointerException("cls cannot be null");
        }

        Class c1 = ref.getType();
        Class c2 = cls.getType();
        Set cs1 = DynamicUtil.decomposeClass(c1);
        Set cs2 = DynamicUtil.decomposeClass(c2);
        if ((cs1.size() == 1) && (cs2.size() == 1) && (!c1.isInterface()) && (!c2.isInterface())) {
            if (!(c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1))) {
                throw new IllegalArgumentException("Invalid constraint: "
                        + c1 + " " + op + " " + c2);
            }
        }

        this.ref = ref;
        this.op = op;
        this.cls = cls;
        this.obj = null;
    }

    /**
     * Constructor for ContainsConstraint.
     *
     * @param ref the target QueryReference
     * @param op specify CONTAINS or DOES_NOT_CONTAIN
     * @param obj the InterMineObject to be tested against reference
     */
    public ContainsConstraint(QueryReference ref, ConstraintOp op, InterMineObject obj) {
        if (ref == null) {
            throw new NullPointerException("ref cannot be null");
        }
        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }
        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("op cannot be " + op);
        }
        if (obj == null) {
            throw new NullPointerException("obj cannot be null");
        }

        this.ref = ref;
        this.op = op;
        this.obj = obj;
        this.cls = null;
    }

    /**
     * Constructor for ContainsConstraint.
     *
     * @param ref the target QueryObjectReference
     * @param op specify IS_NULL or IS_NOT_NULL
     */
    public ContainsConstraint(QueryObjectReference ref, ConstraintOp op) {
        if (ref == null) {
            throw new NullPointerException("ref cannot be null");
        }

        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }

        if (!VALID_OPS_NULL.contains(op)) {
            throw new IllegalArgumentException("op cannot be " + op);
        }

        this.ref = ref;
        this.op = op;
        this.cls = null;
        this.obj = null;
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
     * Returns the InterMineObject of the constraint.
     *
     * @return the InterMineObject
     */
    public InterMineObject getObject() {
        return obj;
    }

    /**
     * Test whether two ContainsConstraints are equal, overrides Object.equals()
     *
     * @param obj1 the object to compare with
     * @return true if objects are equal
     */
    public boolean equals(Object obj1) {
        if (obj1 instanceof ContainsConstraint) {
            ContainsConstraint cc = (ContainsConstraint) obj1;
            return this.ref.equals(cc.ref)
                    && this.op == cc.op
                    && Util.equals(this.obj, cc.obj)
                    && Util.equals(this.cls, cc.cls);
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
            + 3 * op.hashCode()
            + 7 * (cls == null ? 0 : cls.hashCode())
            + 11 * (obj == null ? 0 : obj.hashCode());
    }

    /** List of possible operations */
    public static final List VALID_OPS = Arrays.asList(new ConstraintOp[] {ConstraintOp.CONTAINS,
        ConstraintOp.DOES_NOT_CONTAIN});
    /** List of possible null operations */
    public static final List VALID_OPS_NULL = Arrays.asList(new ConstraintOp[] {
        ConstraintOp.IS_NULL, ConstraintOp.IS_NOT_NULL});
}
