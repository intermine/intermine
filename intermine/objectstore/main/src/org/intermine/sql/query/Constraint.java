package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

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
    /**
     * An operation for this constraint indicating that the left is greater than the right, or
     * the left is null.
     */
    public static final int GORNULL = 4;
    
    
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
        if ((operation < 1) || (operation > 4)) {
            throw (new IllegalArgumentException("Invalid value for operation: " + operation));
        }
        this.operation = operation;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return getSQLString();
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
        } else if (operation == GORNULL) {
            return "(" + left.getSQLString() + " > " + right.getSQLString() + " OR "
                + left.getSQLString() + " IS NULL)";
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
     * {@inheritDoc}
     */
    public int compare(AbstractConstraint obj, Map tableMap, Map reverseTableMap) {
        if (obj instanceof Constraint) {
            Constraint objC = (Constraint) obj;
            switch (operation) {
                case EQ:
                    switch (objC.operation) {
                        case EQ:
                            if (left.valueEquals(objC.left, tableMap, reverseTableMap)) {
                                return (right.valueEquals(objC.right, tableMap, reverseTableMap)
                                        ? EQUAL : (right.notEqualTo(objC.right, tableMap,
                                                reverseTableMap) ? EXCLUDES : INDEPENDENT));
                            } else if (left.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                return (right.valueEquals(objC.left, tableMap, reverseTableMap)
                                        ? EQUAL : (right.notEqualTo(objC.left, tableMap,
                                                reverseTableMap) ? EXCLUDES : INDEPENDENT));
                            } else if (right.valueEquals(objC.left, tableMap, reverseTableMap)) {
                                return (left.notEqualTo(objC.right, tableMap, reverseTableMap)
                                        ? EXCLUDES : INDEPENDENT);
                            } else if (right.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                return (left.notEqualTo(objC.left, tableMap, reverseTableMap)
                                        ? EXCLUDES : INDEPENDENT);
                            } else {
                                return INDEPENDENT;
                            }
                        case LT:
                            if (left.valueEquals(objC.left, tableMap, reverseTableMap)) {
                                return (right.lessThan(objC.right, tableMap, reverseTableMap)
                                        ? IMPLIES : (right.greaterOrEqual(objC.right, tableMap,
                                                reverseTableMap) ? EXCLUDES : INDEPENDENT));
                            } else if (left.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                return (right.greaterThan(objC.left, tableMap, reverseTableMap)
                                        ? IMPLIES : (right.lessOrEqual(objC.left, tableMap,
                                                reverseTableMap) ? EXCLUDES : INDEPENDENT));
                            } else if (right.valueEquals(objC.left, tableMap, reverseTableMap)) {
                                return (left.lessThan(objC.right, tableMap, reverseTableMap)
                                        ? IMPLIES : (left.greaterOrEqual(objC.right, tableMap,
                                                reverseTableMap) ? EXCLUDES : INDEPENDENT));
                            } else if (right.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                return (left.greaterThan(objC.left, tableMap, reverseTableMap)
                                        ? IMPLIES : (left.lessOrEqual(objC.left, tableMap,
                                                reverseTableMap) ? EXCLUDES : INDEPENDENT));
                            } else {
                                return INDEPENDENT;
                            }
                    }
                    break;
                case LT:
                    switch (objC.operation) {
                        case EQ:
                            if (left.valueEquals(objC.left, tableMap, reverseTableMap)) {
                                return (objC.right.lessThan(right, reverseTableMap, tableMap)
                                        ? IMPLIED_BY : (objC.right.greaterOrEqual(right,
                                                reverseTableMap, tableMap) ? EXCLUDES
                                            : INDEPENDENT));
                            } else if (left.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                return (objC.left.lessThan(right, reverseTableMap, tableMap)
                                        ? IMPLIED_BY : (objC.left.greaterOrEqual(right,
                                                reverseTableMap, tableMap) ? EXCLUDES
                                            : INDEPENDENT));
                            } else if (right.valueEquals(objC.left, tableMap, reverseTableMap)) {
                                return (objC.right.greaterThan(left, reverseTableMap, tableMap)
                                        ? IMPLIED_BY : (objC.right.lessOrEqual(left,
                                                reverseTableMap, tableMap) ? EXCLUDES
                                            : INDEPENDENT));
                            } else if (right.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                return (objC.left.greaterThan(left, reverseTableMap, tableMap)
                                        ? IMPLIED_BY : (objC.left.lessOrEqual(left,
                                                reverseTableMap, tableMap) ? EXCLUDES
                                            : INDEPENDENT));
                            } else {
                                return INDEPENDENT;
                            }
                        case LT:
                            if (left.valueEquals(objC.left, tableMap, reverseTableMap)) {
                                if (right.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                    return EQUAL;
                                } else if (right.lessThan(objC.right, tableMap, reverseTableMap)) {
                                    return IMPLIES;
                                } else if (right.greaterThan(objC.right, tableMap,
                                            reverseTableMap)) {
                                    return IMPLIED_BY;
                                } else {
                                    return INDEPENDENT;
                                }
                            } else if (left.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                if (right.valueEquals(objC.left, tableMap, reverseTableMap)
                                        || right.lessThan(objC.left, tableMap, reverseTableMap)) {
                                    return EXCLUDES;
                                } else if (right.greaterThan(objC.left, tableMap,
                                            reverseTableMap)) {
                                    return OR;
                                } else {
                                    return INDEPENDENT;
                                }
                            } else if (right.valueEquals(objC.left, tableMap, reverseTableMap)) {
                                if (left.valueEquals(objC.right, tableMap, reverseTableMap)
                                        || left.greaterThan(objC.right, tableMap,
                                            reverseTableMap)) {
                                    return EXCLUDES;
                                } else if (left.lessThan(objC.right, tableMap, reverseTableMap)) {
                                    return OR;
                                } else {
                                    return INDEPENDENT;
                                }
                            } else if (right.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                if (left.valueEquals(objC.left, tableMap, reverseTableMap)) {
                                    return EQUAL;
                                } else if (left.greaterThan(objC.left, tableMap, reverseTableMap)) {
                                    return IMPLIES;
                                } else if (left.lessThan(objC.left, tableMap, reverseTableMap)) {
                                    return IMPLIED_BY;
                                } else {
                                    return INDEPENDENT;
                                }
                            }
                            break;
                    }
                    break;
                case LIKE:
                    if (objC.operation == LIKE) {
                        return (left.valueEquals(objC.left, tableMap, reverseTableMap)
                                && right.valueEquals(objC.right, tableMap, reverseTableMap)
                                ? EQUAL : INDEPENDENT);
                    }
                    break;
                case GORNULL:
                    if (objC.operation == GORNULL) {
                        if (left.valueEquals(objC.left, tableMap, reverseTableMap)) {
                            if (right.valueEquals(objC.right, tableMap, reverseTableMap)) {
                                return EQUAL;
                            } else if (right.lessThan(objC.right, tableMap, reverseTableMap)) {
                                return IMPLIED_BY;
                            } else if (right.greaterThan(objC.right, tableMap, reverseTableMap)) {
                                return IMPLIES;
                            }
                        }
                    }
                    break;
            }
        } else if (obj instanceof NotConstraint) {
            NotConstraint objNC = (NotConstraint) obj;
            return alterComparisonNotObj(compare(objNC.con));
        } else if (obj instanceof ConstraintSet) {
            return alterComparisonSwitch(obj.compare(this));
        }
        // We reach this bit of code if we see unmatched LIKEs or GORNULLs
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
