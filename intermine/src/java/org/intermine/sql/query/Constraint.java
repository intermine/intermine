package org.flymine.sql.query;

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
                            if (left.equalsIgnoreAlias(objC.left)) {
                                return (right.equalsIgnoreAlias(objC.right) ? EQUAL : EXCLUDES);
                            } else if (left.equalsIgnoreAlias(objC.right)) {
                                return (right.equalsIgnoreAlias(objC.left) ? EQUAL : EXCLUDES);
                            } else if (right.equalsIgnoreAlias(objC.left)) {
                                return EXCLUDES;
                            } else if (right.equalsIgnoreAlias(objC.right)) {
                                return EXCLUDES;
                            } else {
                                return INDEPENDENT;
                            }
                        case LT:
                            if (left.equalsIgnoreAlias(objC.left)) {
                                return (right.lessThan(objC.right) ? IMPLIES : EXCLUDES);
                            } else if (left.equalsIgnoreAlias(objC.right)) {
                                return (right.greaterThan(objC.left) ? IMPLIES : EXCLUDES);
                            } else if (right.equalsIgnoreAlias(objC.left)) {
                                return (left.lessThan(objC.right) ? IMPLIES : EXCLUDES);
                            } else if (right.equalsIgnoreAlias(objC.right)) {
                                return (left.greaterThan(objC.left) ? IMPLIES : EXCLUDES);
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
                            if (left.equalsIgnoreAlias(objC.left)) {
                                return (objC.right.lessThan(right) ? IMPLIED_BY : EXCLUDES);
                            } else if (left.equalsIgnoreAlias(objC.right)) {
                                return (objC.left.lessThan(right) ? IMPLIED_BY : EXCLUDES);
                            } else if (right.equalsIgnoreAlias(objC.left)) {
                                return (objC.right.greaterThan(left) ? IMPLIED_BY : EXCLUDES);
                            } else if (right.equalsIgnoreAlias(objC.right)) {
                                return (objC.left.greaterThan(left) ? IMPLIED_BY : EXCLUDES);
                            } else {
                                return INDEPENDENT;
                            }
                        case LT:
                            if (left.equalsIgnoreAlias(objC.left)) {
                                if (right.equalsIgnoreAlias(objC.right)) {
                                    return EQUAL;
                                } else if (right.lessThan(objC.right)) {
                                    return IMPLIES;
                                } else if (right.greaterThan(objC.right)) {
                                    return IMPLIED_BY;
                                } else {
                                    // TODO: This shouldn't really happen in valid SQL. Log?
                                    return INDEPENDENT;
                                }
                            } else if (left.equalsIgnoreAlias(objC.right)) {
                                if (right.equalsIgnoreAlias(objC.left)
                                        || right.lessThan(objC.left)) {
                                    return EXCLUDES;
                                } else if (right.greaterThan(objC.left)) {
                                    return OR;
                                } else {
                                    // TODO: This shouldn't really happen in valid SQL. Log?
                                    return INDEPENDENT;
                                }
                            } else if (right.equalsIgnoreAlias(objC.left)) {
                                if (left.equalsIgnoreAlias(objC.right)
                                        || left.greaterThan(objC.right)) {
                                    return EXCLUDES;
                                } else if (left.lessThan(objC.right)) {
                                    return OR;
                                } else {
                                    // TODO: This shouldn't really happen in valid SQL. Log?
                                    return INDEPENDENT;
                                }
                            } else if (right.equalsIgnoreAlias(objC.right)) {
                                if (left.equalsIgnoreAlias(objC.left)) {
                                    return EQUAL;
                                } else if (left.greaterThan(objC.left)) {
                                    return IMPLIES;
                                } else if (left.lessThan(objC.left)) {
                                    return IMPLIED_BY;
                                } else {
                                    // TODO: This shouldn't really happen in valid SQL. Log?
                                    return INDEPENDENT;
                                }
                            }
                        case LIKE:
                            return INDEPENDENT;
                    }
                case LIKE:
                    switch (objC.operation) {
                        case EQ:
                        case LT:
                            return INDEPENDENT;
                        case LIKE:
                            return (left.equalsIgnoreAlias(objC.left)
                                    && right.equalsIgnoreAlias(objC.right)
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
}
