package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.sql.query.AbstractTable;
import org.intermine.sql.query.AbstractValue;
import org.intermine.sql.query.Field;
import org.intermine.sql.query.OrderDescending;
import org.intermine.sql.query.Query;
import org.intermine.sql.query.SelectValue;
import org.intermine.sql.query.SQLStringable;
import org.intermine.sql.query.Table;

import org.apache.log4j.Logger;

/**
 * Represents a Precomputed table in a database. A precomputed table is a materialised SQL query.
 * Note - the query encapsulated in this PrecomputedTable should not be altered.
 *
 * @author Andrew Varley
 */
public class PrecomputedTable implements SQLStringable, Comparable
{
    private static final Logger LOG = Logger.getLogger(PrecomputedTable.class);
    /** The name of the field that is generated as the order by field */
    public static final String ORDERBY_FIELD = "orderby_field";
    protected Query q;
    protected String originalSql;
    protected String name;
    protected String category;
    protected Map valueMap;
    protected String orderByField;
    protected String generationSqlString;
    protected boolean firstOrderByHasNoNulls = false;

    /**
     * Construct a new PrecomputedTable
     *
     * @param q the Query that this PrecomputedTable materialises
     * @param originalSql the original SQL text that appears in the index table
     * @param name the name of this PrecomputedTable
     * @param category the type of PrecomputedTable this is. Note that a value of "template" denotes
     * PrecomputedTables that are managed automatically by the webapp. Such tables are liable to be
     * created and deleted at random
     * @param conn a Connection to use to work out if the order by fields are compatible with a
     * unified orderby_field
     */
    public PrecomputedTable(Query q, String originalSql, String name, String category,
            Connection conn) {
        if (q == null) {
            throw (new NullPointerException("q cannot be null"));
        }
        if (name == null) {
            throw (new NullPointerException("the name of a precomputed table cannot be null"));
        }
        if (originalSql == null) {
            throw new NullPointerException("Original sql string cannot be null");
        }
        this.q = q;
        this.originalSql = originalSql;
        this.name = name;
        this.category = category;
        // Now build the valueMap. Do not alter this Query from now on...
        valueMap = new HashMap();
        Iterator valueIter = q.getSelect().iterator();
        while (valueIter.hasNext()) {
            SelectValue value = (SelectValue) valueIter.next();
            valueMap.put(value.getValue(), value);
        }

        // Now we should work out if we can create an order by field. First, we need to make sure
        // that all the fields in the order by list are integer numbers (that is SMALLINT, INTEGER,
        // and BIGINT).
        boolean useOrderByField = (q.getOrderBy().size() > 1) && (q.getUnion().size() == 1);
        try {
            if (useOrderByField) {
                Iterator orderByIter = q.getOrderBy().iterator();
                while (orderByIter.hasNext() && useOrderByField) {
                    AbstractValue column = (AbstractValue) orderByIter.next();
                    if (column instanceof OrderDescending) {
                        column = ((OrderDescending) column).getValue();
                    }
                    if (valueMap.containsKey(column)) {
                        if (column instanceof Field) {
                            AbstractTable table = ((Field) column).getTable();
                            if (table instanceof Table) {
                                String tableName = ((Table) table).getName().toLowerCase();
                                String columnName = ((Field) column).getName().toLowerCase();
                                ResultSet r = conn.getMetaData().getColumns(null, null, tableName,
                                        columnName);
                                if (r.next()) {
                                    if (tableName.equals(r.getString(3))
                                            && columnName.equals(r.getString(4))) {
                                        int columnType = r.getInt(5);
                                        if (!((columnType == Types.SMALLINT)
                                                    || (columnType == Types.INTEGER)
                                                    || (columnType == Types.BIGINT))) {
                                            useOrderByField = false;
                                            LOG.debug("Cannot generate order field for precomputed"
                                                    + " table - column " + column.getSQLString()
                                                    + " is type " + columnType);
                                        }
                                    } else {
                                        useOrderByField = false;
                                        LOG.error("getColumns returned wrong data for column "
                                                + column.getSQLString());
                                    }
                                } else {
                                    useOrderByField = false;
                                    LOG.error("getColumns return no data for column "
                                            + column.getSQLString() + " in table " + tableName);
                                }
                                if (r.next()) {
                                    useOrderByField = false;
                                    LOG.error("getColumns returned too much data for column "
                                            + column.getSQLString());
                                }
                            } else {
                                useOrderByField = false;
                                LOG.debug("Cannot generate order field for precomputed table -"
                                        + "column " + column.getSQLString()
                                        + " does not belong to a Table");
                            }
                        } else {
                            useOrderByField = false;
                            LOG.debug("Cannot generate order field for precomputed table - column "
                                    + column.getSQLString() + " is not a Field");
                        }
                    } else {
                        useOrderByField = false;
                        LOG.debug("Cannot generate order field for precomputed table - column "
                                + column.getSQLString() + " is not present in the precomputed"
                                + " table");
                    }
                }
            }
            Iterator orderByIter = q.getOrderBy().iterator();
            if (orderByIter.hasNext()) {
                AbstractValue column = (AbstractValue) orderByIter.next();
                if (column instanceof OrderDescending) {
                    column = ((OrderDescending) column).getValue();
                }
                if (column instanceof Field) {
                    AbstractTable table = ((Field) column).getTable();
                    if (table instanceof Table) {
                        String tableName = ((Table) table).getName().toLowerCase();
                        String columnName = ((Field) column).getName().toLowerCase();
                        ResultSet r = conn.getMetaData().getColumns(null, null, tableName,
                                columnName);
                        if (r.next()) {
                            if (tableName.equals(r.getString(3))
                                    && columnName.equals(r.getString(4))) {
                                int nullable = r.getInt(11);
                                if (DatabaseMetaData.columnNoNulls == nullable) {
                                    firstOrderByHasNoNulls = true;
                                }
                            } else {
                                LOG.error("getColumns returned wrong data for column "
                                        + column.getSQLString());
                            }
                        } else {
                            LOG.error("getColumns returned no data for column "
                                    + column.getSQLString());
                        }
                        if (r.next()) {
                            LOG.error("getColumns returned too much data for column "
                                    + column.getSQLString());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            useOrderByField = false;
            LOG.error("Caught SQLException while examining order by fields: " + e);
        }

        if (useOrderByField) {
            orderByField = ORDERBY_FIELD;
            List orderBy = q.getOrderBy();
            StringBuffer extraBuffer = new StringBuffer();
            for (int i = 0; i < orderBy.size(); i++) {
                AbstractValue orderByField = (AbstractValue) orderBy.get(i);
                if (orderByField instanceof OrderDescending) {
                    orderByField = ((OrderDescending) orderByField).getValue();
                    if (i == 0) {
                        extraBuffer.append("-");
                    } else {
                        extraBuffer.append(" - ");
                    }
                } else {
                    if (i != 0) {
                        extraBuffer.append(" + ");
                    }
                }
                if (i < orderBy.size() - 1) {
                    extraBuffer.append("(");
                }
                extraBuffer.append("COALESCE(" + orderByField.getSQLString() + "::numeric, 49999999999999999999)");
                if (i < orderBy.size() - 1) {
                    extraBuffer.append(" * 1");
                }
                for (int o = 0; o < orderBy.size() - 1 - i; o++) {
                    extraBuffer.append("00000000000000000000");
                }
                if (i < orderBy.size() - 1) {
                    extraBuffer.append(")");
                }
            }
            extraBuffer.append(" AS " + ORDERBY_FIELD);
            generationSqlString = q.getSQLStringForPrecomputedTable(extraBuffer.toString());
        } else {
            orderByField = null;
            generationSqlString = q.getSQLString();
        }
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
     * Returns the original SQL text stored in the index for this PrecomputedTable
     *
     * @return an SQL String
     */
    public String getOriginalSql() {
        return originalSql;
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
     * Returns the category of this PrecomputedTable
     *
     * @return a String
     */
    public String getCategory() {
        return category;
    }

    /**
     * Gets a Map from AbstractValue to SelectValue for the Query in this PrecomputedTable.
     *
     * @return the valueMap
     */
    public Map getValueMap() {
        return valueMap;
    }

    /**
     * Get a "CREATE TABLE" SQL statement for this PrecomputedTable.
     *
     * @return this PrecomputedTable as an SQL statement
     */
    public String getSQLString() {
        return generationSqlString;
    }

    /**
     * Returns the name of the order by field, if it exists.
     *
     * @return orderByField
     */
    public String getOrderByField() {
        return orderByField;
    }

    /**
     * Returns true if the first element of the ORDER BY clause is a column that contains no nulls.
     *
     * @return a boolean
     */
    public boolean getFirstOrderByHasNoNulls() {
        return firstOrderByHasNoNulls;
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

    /**
     * Implements Comparable's method, so we can put PrecomputedTable objects into SortedMaps.
     *
     * @param obj an Object to compare to
     * @return an integer based on the comparison
     * @throws ClassCastException if obj is not a PrecomputedTable
     */
    public int compareTo(Object obj) {
        PrecomputedTable objPt = (PrecomputedTable) obj;
        int retval = objPt.q.getFrom().size() - q.getFrom().size();
        if (retval == 0) {
            retval = name.compareTo(objPt.name);
        }
        return retval;
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return name + "/" + category + " (" + q.getFrom().size() + " tables)";
    }
}
