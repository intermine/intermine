package org.flymine.sql.precompute;

import org.flymine.sql.query.Query;

/**
 * Represents a Precomputed table in a database. A precomputed table is a materialised SQL query.
 *
 * @author Andrew Varley
 */
public class PrecomputedTable implements SQLStringable
{
    protected Query q;
    protected String name;

    /**
     * Construct a new PrecomputedTable
     *
     * @param q the Query that this PrecomputedTable materialises
     * @param name the name of this PrecomputedTable
     */
    public PrecomputedTable(Query q, String name) {
        if (q == null) {
            throw (new NullPointerException("q cannot be null"));
        }
        if (name == null) {
            throw (new NullPointerException("the name of a precomputed table cannot be null"));
        }
        this.q = q;
        this.name = name;
    }

    /**
     * Gets the Query that is materialised in this PrecomputedTable
     *
     * @return the Query that this PrecomputedTable materialises
     */
    public Query getQuery() {
        return q;
    }

    /**
     * Gets the name of this PrecomputedTable
     *
     * @return the name of the PrecomputedTable
     */
    public String getName() {
        return name;
    }

    /**
     * Get a "CREATE TABLE" SQL statement for this PrecomputedTable
     *
     * @return this PrecomputedTable as an SQL statement
     */
    public String getSQLString() {
        return "CREATE TABLE " + name + " AS " + q.getSQLString();
    }

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if the object is equal
     */
    public boolean equals(Object obj) {
        if (obj instanceof PrecomputedTable) {
            PrecomputedTable objTable = (PrecomputedTable) obj;
            return q.equals(objTable.q) && name.equals(objTable.name);
        }
        return false;
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer based on the contents of the Query and the name
     */
    public int hashCode() {
        return (3 * q.hashCode()) + (5 * name.hashCode());
    }
}
