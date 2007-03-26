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
 * A representation of a subquery that can be present in the FROM section of an SQL query.
 *
 * @author Matthew Wakeling
 */
public class SubQuery extends AbstractTable
{
    protected Query query;

    /**
     * Constructor for this SubQuery object.
     *
     * @param query a Query to be included as a subquery
     * @param alias an arbitrary name that the subquery has been renamed to for the rest of the
     * query
     */
    public SubQuery(Query query, String alias) {
        if (query == null) {
            throw (new NullPointerException("The query cannot be null"));
        }
        if (alias == null) {
            throw (new NullPointerException("The alias cannot be null on a SubQuery"));
        }
        this.query = query;
        this.alias = alias;
    }

    /**
     * Returns the query contained in the subquery.
     *
     * @return the subquery
     */
    public Query getQuery() {
        return query;
    }

    /**
     * Returns a String representation of this SubQuery object, auitable for forming part of an SQL
     * query.
     *
     * @return the String representation
     */
    public String getSQLString() {
        return "(" + query.getSQLString() + ") AS " + alias;
    }

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if the object is of the same class, and with an equal query and alias
     */
    public boolean equals(Object obj) {
        if (obj instanceof SubQuery) {
            SubQuery objSubQuery = (SubQuery) obj;
            return query.equals(objSubQuery.query) && alias.equals(objSubQuery.alias);
        }
        return false;
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer created from the contents of the SubQuery
     */
    public int hashCode() {
        return (3 * query.hashCode()) + (5 * alias.hashCode());
    }

    /**
     * Compare this SubQuery to another AbstractTable, ignoring alias.
     *
     * @param obj an AbstractTable to compare to
     * @return true if the object is of the same class, and with an equal query
     */
    public boolean equalsIgnoreAlias(AbstractTable obj) {
        if (obj instanceof SubQuery) {
            return query.equals(((SubQuery) obj).query);
        }
        return false;
    }
}
