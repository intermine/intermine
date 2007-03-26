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
 * A representation of a table that can be present in the FROM section of an SQL query.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class Table extends AbstractTable
{
    protected String name;

    /**
     * Constructor for this Table object.
     *
     * @param name the name of the table, as the database knows it
     * @param alias an arbitrary name that the table has been renamed to for the rest of the query
     */
    public Table(String name, String alias) {
        if (name == null) {
            throw (new NullPointerException("Table names cannot be null"));
        }
        this.name = name;
        if (alias == null) {
            alias = name;
        }
        this.alias = alias;
    }

    /**
     * Constructor for this Table object, without an alias.
     *
     * @param name the name of the table, as the database knows it. This name is also used in
     * the rest of the query.
     */
    public Table(String name) {
        this(name, null);
    }

    /**
     * Returns the name of the table
     *
     * @return the Table name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a String representation of this Table object, suitable for forming part of an SQL
     * query.
     *
     * @return the String representation
     */
    public String getSQLString() {
        return ((alias == null) || (alias.equals(name)) ? name : name + " AS " + alias);
    }

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if the object is of the same class, and with the same name
     */
    public boolean equals(Object obj) {
        if (obj instanceof Table) {
            Table objTable = (Table) obj;
            return name.equals(objTable.name)
                   && (((alias == null) && (objTable.alias == null))
                       || ((alias != null) && (alias.equals(objTable.alias))));
        }
        return false;
    }

    /**
     * Overrides Object.hashcode().
     *
     * @return an arbitrary integer based on the name of the Table
     */
    public int hashCode() {
        return name.hashCode() + (alias == null ? 0 : alias.hashCode());
    }

    /**
     * Compare this Table to another AbstractTable, ignoring alias.
     *
     * @param obj an AbstractTable to compare to
     * @return true if the object is of the same class, and with the same value
     */
    public boolean equalsIgnoreAlias(AbstractTable obj) {
        if (obj instanceof Table) {
            return name.equals(((Table) obj).name);
        }
        return false;
    }

    /**
     * A toString method, which helps us when debugging.
     *
     * @return a String representation of the object
     */
    public String toString() {
        return getSQLString();
    }
}
