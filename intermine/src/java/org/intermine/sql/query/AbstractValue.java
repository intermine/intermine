package org.flymine.sql.query;

/**
 * An abstract representation of an item that can be present in the SELECT, GROUP BY,
 * or ORDER BY sections of an SQL query, as well as in a constraint.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public abstract class AbstractValue
{
    /**
     * Describes two AbstractValues as being equal.
     */
    public static final int EQUAL = 0;
    /**
     * Describes one AbstractValue as being less than another AbstractValue.
     */
    public static final int LESS = 1;
    /**
     * Describes one AbstractValue as being greater than another AbstractValue.
     */
    public static final int GREATER = 2;
    /**
     * Describes one AbstractValue as being incomparable to another AbstractValue.
     */
    public static final int INCOMPARABLE = 3;

    /**
     * Returns a String representation of this AbstractValue object, suitable for forming
     * part of an SQL query.
     *
     * @return the String representation
     */
    public abstract String getSQLString();

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if obj is equal
     */
    public abstract boolean equals(Object obj);

    /**
     * Overrides Object.hashcode().
     *
     * @return an arbitrary integer based on the contents of the object
     */
    public abstract int hashCode();
 
    /**
     * Compare the value of this AbstractValue with another. This only really makes sense with
     * Constants.
     *
     * @param obj an AbstractValue to compare to
     * @return EQUAL, LESS, GREATER, or INCOMPARABLE
     */
    public int compare(AbstractValue obj) {
        return (equals(obj) ? EQUAL : INCOMPARABLE);
    }

    /**
     * Compare the value of this AbstractValue with another to see if it is less. This only really
     * makes sense with Constants. It uses the compare method of AbstractValue. Note that the
     * result being false does not imply greater than or equal - it may be incomparable.
     *
     * @param obj an AbstractValue to compare to
     * @return true if this is less than obj
     */
    public boolean lessThan(AbstractValue obj) {
        return (compare(obj) == LESS);
    }

    /**
     * Compare this value of this AbstractValue with another to see if it is more. This only really
     * makes sense with Constants. It uses the compare method of AbstractValue. Note that the
     * result being false does not imply less than or equal - it may be incomparable.
     *
     * @param obj an AbstractValue to compare to
     * @return true if this is more than obj
     */
    public boolean greaterThan(AbstractValue obj) {
        return (compare(obj) == GREATER);
    }
}
