package org.flymine.sql.query;

/**
 * An abstract representation of an item that can be present in the WHERE or HAVING
 * sections of an SQL query.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public abstract class AbstractConstraint
{
    /**
     * Describes two constraints as being independent.
     * That is, knowing the result of one constraint tells one nothing about the result of another
     * constraint.
     * <br>For example, "a &gt; 5" and "b &lt; 4".
     */
    public static final int INDEPENDENT = 0;
    /**
     * Describes one constraint as being implied by another constraint.
     * That is, if the second constraint is true, then the first constraint must also be true.
     * This can also be thought of as the first constraint being less restrictive than the second.
     * <br>For example, "a &gt; 5" and "a = 8".
     */
    public static final int IMPLIED_BY = 1;
    /**
     * Describes one constraint as implying another constraint.
     * That is, if the first constraint is true, then the second constraint must also be true.
     * This can also be thought of as the first constraint being more restrictive than the second.
     * <br>For example, "a &gt; 5" and "a &gt; 2".
     */
    public static final int IMPLIES = 2;
    /**
     * Describes one constraint as being equal to another constraint.
     * That is, the result of the first constraint is always the same as the result of the second
     * constraint.
     */
    public static final int EQUAL = 3;
    /**
     * Describes two constraints as being opposite to each other.
     * That is, the result of the first constraint is always the opposite of the result of the
     * second constraint.
     * <br>For example, "a &gt; 5" and "a &le; 5".
     */
    public static final int OPPOSITE = 4;
    /**
     * Describes two constraints as being mutually exclusive.
     * That is, the results of both constraints cannot be true at the same time.
     * <br>For example, "a &gt; 5" and "a &lt; 5".
     */
    public static final int EXCLUDES = 5;
    /**
     * Describes two constraints as following "constraint 1 OR constraint 2 IS TRUE".
     * That is, at least one of the results of the two constraints must be true.
     * <br>For example, "a &gt; 5" and "a &lt; 8".
     */
    public static final int OR = 6;
    
    /**
     * Returns a String representation of this AbstractConstraint object, suitable for forming
     * part of an SQL query.
     *
     * @return the String representation
     */
    public abstract String getSQLString();

    /**
     * Compare this AbstractConstraint with another, ignoring aliases in member fields and tables.
     *
     * @param obj an AbstractConstraint to compare to
     * @return INDEPENDENT, IMPLIED_BY, IMPLIES, EQUAL, OPPOSITE, EXCLUDES, or OR, depending on the
     * constraints.
     */
    public abstract int compare(AbstractConstraint obj);

    /**
     * Take an integer as if it is a value returned by compare, and return the value that compare
     * would have returned if first constraint (this) had been NOTed.
     * For example, if one passes in the value EQUAL, this method will return OPPOSITE.
     *
     * @param comp a previous comparison value returned by compare
     * @return an alternative comparison value, for if this had been NOTed
     */
    protected static int alterComparisonNotThis(int comp) {
        switch (comp) {
            case EQUAL:
                return OPPOSITE;
            case OPPOSITE:
                return EQUAL;
            case IMPLIED_BY:
                return EXCLUDES;
            case IMPLIES:
                return OR;
            case EXCLUDES:
                return IMPLIED_BY;
            case OR:
                return IMPLIES;
        }
        return INDEPENDENT;
    }

    /**
     * Take an integer as if it is a value returned by compare, and return the value that compare
     * would have returned if second constraint (obj) had been NOTed.
     * For example, if one passes in the value IMPLIES, this method will return EXCLUSIVE (as it
     * would now mean that the first implies not the second).
     *
     * @param comp a previous comparison value returned by compare
     * @return an alternative comparison value, for if this had been NOTed
     */
    protected static int alterComparisonNotObj(int comp) {
        switch (comp) {
            case EQUAL:
                return OPPOSITE;
            case OPPOSITE:
                return EQUAL;
            case IMPLIED_BY:
                return OR;
            case IMPLIES:
                return EXCLUDES;
            case OR:
                return IMPLIED_BY;
            case EXCLUDES:
                return IMPLIES;
        }
        return INDEPENDENT;
    }

    /**
     * Take an integer as if it is a value returned by compare, and return the value that compare
     * would have returned if this and obj had been switched.
     * For example, IMPLIES gets translated to IMPLIED_BY.
     *
     * @param comp a previous comparison value returned by compare
     * @return an alternative comparison value, for if this and obj had been switched
     */
    protected static int alterComparisonSwitch(int comp) {
        switch (comp) {
            case IMPLIES:
                return IMPLIED_BY;
            case IMPLIED_BY:
                return IMPLIES;
        }
        return comp;
    }
}
