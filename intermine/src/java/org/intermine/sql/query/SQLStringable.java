package org.flymine.sql.query;

/**
 * An interface for all classes that implement the getSQLString method.
 *
 * @author Matthew Wakeling
 */
public interface SQLStringable
{
    /**
     * Returns a String representation of this object, suitable for forming part of an SQL query.
     *
     * @return the String representation
     */
    public String getSQLString();
}
