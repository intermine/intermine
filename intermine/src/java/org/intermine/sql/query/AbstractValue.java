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
    protected String alias;

    /**
     * Returns the alias for this AbstractValue object.
     *
     * @return the alias of this value
     */
    public String getAlias() {
        return alias;
    }

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
     * Compare this AbstractValue to another, ignoring little details like aliases.
     *
     * @param obj an AbstractValue to compare to
     * @return true if obj is equal
     */
    public abstract boolean equalsIgnoreAlias(AbstractValue obj);
}
