package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A representation of a normal constraint, comparing two AbstractValue objects.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class Constraint extends AbstractConstraint
{
    /**
     * An operation for this constraint indicating that left is equal to right.
     */
    public static final int EQ = 1;
    /**
     * An operation for this constraint indicating that left is less than right.
     */
    public static final int LT = 2;
    /**
     * An operation for this constraint indicating that left should be pattern-matched against
     * right.
     */
    public static final int LIKE = 3;
    
    
    protected AbstractValue left;
    protected AbstractValue right;
    protected int operation;
    
    /**
     * Constructor for a Constraint object.
     *
     * @param left the AbstractValue on the left of the constraint
     * @param operation EQ for an equivalence constraint,
     *                  LT for a Less-Than constraint,
     *                  LIKE for a String pattern match constraint - right must be the pattern to
     *                  match against the value of left
     * @param right the AbstractValue on the right of the constraint
     * @throws IllegalArgumentException if operation is not EQ, LT, or LIKE
     */
    public Constraint(AbstractValue left, int operation, AbstractValue right) {
        this.left = left;
        this.right = right;
        if ((operation < 1) || (operation > 3)) {
            throw (new IllegalArgumentException("Invalid value for operation: " + operation));
        }
        this.operation = operation;
    }

    /**
     * Returns a String representation of this Constraint object, suitable for forming part of an
     * SQL query.
     *
     * @return the String representation
     */
    public String getSQLString() {
        if (right.getSQLString().equals("null")) {
            return left.getSQLString() + " IS NULL";
        } else {
            String op = null;
            switch (operation) {
                case EQ:
                    op = " = ";
                    break;
                case LT:
                    op = " < ";
                    break;
                case LIKE:
                    op = " LIKE ";
                    break;
            }

            return left.getSQLString() + op + right.getSQLString();
        }
    }

    /**
     * Compare this Constraint with another AbstractConstraint, ignoring aliases in member fields
     * and tables.
     *
     * @see AbstractConstraint#compare
     */
    public int compare(AbstractConstraint obj) {
        if (obj instanceof Constraint) {
            Constraint objC = (Constraint) obj;
            // This is a normal constraint object.
            switch (operation) {
                case EQ:
                    switch (objC.operation) {
                        case EQ:
                            if (left.equals(objC.left)) {
                                return (right.equals(objC.right) ? EQUAL
                                        : (right.notEqualTo(objC.right) ? EXCLUDES : INDEPENDENT));
                            } else if (left.equals(objC.right)) {
                                return (right.equals(objC.left) ? EQUAL
                                        : (right.notEqualTo(objC.left) ? EXCLUDES : INDEPENDENT));
                            } else if (right.equals(objC.left)) {
                                return (left.notEqualTo(objC.right) ? EXCLUDES : INDEPENDENT);
                            } else if (right.equals(objC.right)) {
                                return (left.notEqualTo(objC.left) ? EXCLUDES : INDEPENDENT);
                            } else {
                                return INDEPENDENT;
                            }
                        case LT:
                            if (left.equals(objC.left)) {
                                return (right.lessThan(objC.right) ? IMPLIES
                                        : (right.greaterOrEqual(objC.right) ? EXCLUDES
                                            : INDEPENDENT));
                            } else if (left.equals(objC.right)) {
                                return (right.greaterThan(objC.left) ? IMPLIES
                                        : (right.lessOrEqual(objC.left) ? EXCLUDES : INDEPENDENT));
                            } else if (right.equals(objC.left)) {
                                return (left.lessThan(objC.right) ? IMPLIES
                                        : (left.greaterOrEqual(objC.right) ? EXCLUDES
                                            : INDEPENDENT));
                            } else if (right.equals(objC.right)) {
                                return (left.greaterThan(objC.left) ? IMPLIES
                                        : (left.lessOrEqual(objC.left) ? EXCLUDES : INDEPENDENT));
                            } else {
                                return INDEPENDENT;
                            }
                        case LIKE:
                            return INDEPENDENT;
                    }
                    break;
                case LT:
                    switch (objC.operation) {
                        case EQ:
                            if (left.equals(objC.left)) {
                                return (objC.right.lessThan(right) ? IMPLIED_BY
                                        : (objC.right.greaterOrEqual(right) ? EXCLUDES
                                            : INDEPENDENT));
                            } else if (left.equals(objC.right)) {
                                return (objC.left.lessThan(right) ? IMPLIED_BY
                                        : (objC.left.greaterOrEqual(right) ? EXCLUDES
                                            : INDEPENDENT));
                            } else if (right.equals(objC.left)) {
                                return (objC.right.greaterThan(left) ? IMPLIED_BY
                                        : (objC.right.lessOrEqual(left) ? EXCLUDES : INDEPENDENT));
                            } else if (right.equals(objC.right)) {
                                return (objC.left.greaterThan(left) ? IMPLIED_BY
                                        : (objC.left.lessOrEqual(left) ? EXCLUDES : INDEPENDENT));
                            } else {
                                return INDEPENDENT;
                            }
                        case LT:
                            if (left.equals(objC.left)) {
                                if (right.equals(objC.right)) {
                                    return EQUAL;
                                } else if (right.lessThan(objC.right)) {
                                    return IMPLIES;
                                } else if (right.greaterThan(objC.right)) {
                                    return IMPLIED_BY;
                                } else {
                                    return INDEPENDENT;
                                }
                            } else if (left.equals(objC.right)) {
                                if (right.equals(objC.left)
                                        || right.lessThan(objC.left)) {
                                    return EXCLUDES;
                                } else if (right.greaterThan(objC.left)) {
                                    return OR;
                                } else {
                                    return INDEPENDENT;
                                }
                            } else if (right.equals(objC.left)) {
                                if (left.equals(objC.right)
                                        || left.greaterThan(objC.right)) {
                                    return EXCLUDES;
                                } else if (left.lessThan(objC.right)) {
                                    return OR;
                                } else {
                                    return INDEPENDENT;
                                }
                            } else if (right.equals(objC.right)) {
                                if (left.equals(objC.left)) {
                                    return EQUAL;
                                } else if (left.greaterThan(objC.left)) {
                                    return IMPLIES;
                                } else if (left.lessThan(objC.left)) {
                                    return IMPLIED_BY;
                                } else {
                                    return INDEPENDENT;
                                }
                            }
                            break;
                        case LIKE:
                            return INDEPENDENT;
                    }
                    break;
                case LIKE:
                    switch (objC.operation) {
                        case EQ:
                        case LT:
                            return INDEPENDENT;
                        case LIKE:
                            return (left.equals(objC.left)
                                    && right.equals(objC.right)
                                    ? EQUAL : INDEPENDENT);
                    }
                    break;
            }
        } else if (obj instanceof NotConstraint) {
            NotConstraint objNC = (NotConstraint) obj;
            return alterComparisonNotObj(compare(objNC.con));
        } else if (obj instanceof ConstraintSet) {
            return alterComparisonSwitch(obj.compare(this));
        }
        // TODO: we shouldn't ever reach this bit of the code. Log?
        return INDEPENDENT;
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer based on the contents of the Constraint
     */
    public int hashCode() {
        return (3 * left.hashCode()) + (5 * operation) 
            + ((operation == EQ ? 3 : 7) * right.hashCode());
    }

    /**
     * Returns the left argument of the constraint.
     *
     * @return left
     */
    public AbstractValue getLeft() {
        return left;
    }

    /**
     * Returns the right argument of the constraint.
     *
     * @return right
     */
    public AbstractValue getRight() {
        return right;
    }

    /**
     * Returns the operation of the constraint.
     *
     * @return operation
     */
    public int getOperation() {
        return operation;
    }
}
