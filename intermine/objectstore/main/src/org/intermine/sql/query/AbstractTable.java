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

/**
 * An abstract representation of an item that can be present in the FROM section of an
 * SQL query.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public abstract class AbstractTable implements SQLStringable
{
    protected String alias;

    /**
     * Returns the alias for this AbstractTable object.
     *
     * @return the alias of this "table"
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Sets the alias for this AbstractTable object.
     *
     * @param alias the alias of this "table"
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Returns a String representation of this AbstractTable object, suitable for forming
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
     * Compare this AbstractTable to another, ignoring little details like aliases.
     *
     * @param obj an AbstractTable to compare to
     * @return true if obj is equal
     */
    public abstract boolean equalsIgnoreAlias(AbstractTable obj);

    /**
     * Compare this AbstractTable to another, only comparing the alias.
     *
     * @param obj an AbstractTable to compare to
     * @return true if obj has the same alias
     */
    public boolean equalsOnlyAlias(AbstractTable obj) {
        return alias.equals(obj.alias);
    }
}
