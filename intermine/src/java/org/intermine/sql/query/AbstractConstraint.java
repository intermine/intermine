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
     * Returns a String representation of this AbstractConstraint object, suitable for forming
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
}
