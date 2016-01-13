package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.sql.query.AbstractTable;
import org.intermine.sql.query.AbstractValue;
import org.intermine.sql.query.Field;
import org.intermine.sql.query.OrderDescending;
import org.intermine.sql.query.Query;
import org.intermine.sql.query.SQLStringable;
import org.intermine.sql.query.SelectValue;
import org.intermine.sql.query.Table;

/**
 * Represents a Precomputed table in a database. A precomputed table is a materialised SQL query.
 * Note - the query encapsulated in this PrecomputedTable should not be altered.
 *
 * @author Andrew Varley
 */
public class PrecomputedTable implements SQLStringable, Comparable<PrecomputedTable>
{
    private static final Logger LOG = Logger.getLogger(PrecomputedTable.class);
    /** The name of the field that is generated as the order by field */
    public static final String ORDERBY_FIELD = "orderby_field";
    protected Query q;
    protected String originalSql;
    protected String name;
    protected String category;
    protected Map<AbstractValue, SelectValue> valueMap;
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
        valueMap = new HashMap<AbstractValue, SelectValue>();
        for (SelectValue value : q.getSelect()) {
            valueMap.put(value.getValue(), value);
        }

        // Now we should work out if we can create an order by field. First, we need to make sure
        // that all the fields in the order by list are integer numbers (that is SMALLINT, INTEGER,
        // and BIGINT).
        boolean useOrderByField = (q.getOrderBy().size() > 1) && (q.getUnion().size() == 1);
        try {
            if (useOrderByField) {
                for (AbstractValue column : q.getOrderBy()) {
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
                                            break;
                                        }
                                    } else {
                                        useOrderByField = false;
                                        LOG.warn("getColumns returned wrong data for column "
                                                + column.getSQLString());
                                        break;
                                    }
                                } else {
                                    useOrderByField = false;
                                    LOG.warn("getColumns return no data for column "
                                            + column.getSQLString() + " in table " + tableName);
                                    break;
                                }
                                if (r.next()) {
                                    useOrderByField = false;
                                    LOG.warn("getColumns returned too much data for column "
                                            + column.getSQLString());
                                    break;
                                }
                            } else {
                                useOrderByField = false;
                                LOG.debug("Cannot generate order field for precomputed table -"
                                        + "column " + column.getSQLString()
                                        + " does not belong to a Table");
                                break;
                            }
                        } else {
                            useOrderByField = false;
                            LOG.debug("Cannot generate order field for precomputed table - column "
                                    + column.getSQLString() + " is not a Field");
                            break;
                        }
                    } else {
                        useOrderByField = false;
                        LOG.debug("Cannot generate order field for precomputed table - column "
                                + column.getSQLString() + " is not present in the precomputed"
                                + " table");
                        break;
                    }
                }
            }
            for (AbstractValue column : q.getOrderBy()) {
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
                                LOG.warn("getColumns returned wrong data for column "
                                        + column.getSQLString());
                            }
                        } else {
                            LOG.warn("getColumns returned no data for column "
                                    + column.getSQLString());
                        }
                        if (r.next()) {
                            LOG.warn("getColumns returned too much data for column "
                                    + column.getSQLString());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            useOrderByField = false;
            LOG.warn("Caught SQLException while examining order by fields: " + e);
        }

        if (useOrderByField) {
            orderByField = ORDERBY_FIELD;
            List<AbstractValue> orderBy = q.getOrderBy();
            StringBuffer extraBuffer = new StringBuffer();
            for (int i = 0; i < orderBy.size(); i++) {
                AbstractValue newOrderByField = orderBy.get(i);
                if (newOrderByField instanceof OrderDescending) {
                    newOrderByField = ((OrderDescending) newOrderByField).getValue();
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
                extraBuffer.append("COALESCE(" + newOrderByField.getSQLString()
                        + "::numeric, 49999999999999999999)");
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
    public Map<AbstractValue, SelectValue> getValueMap() {
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
    @Override
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
    @Override
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
    public int compareTo(PrecomputedTable obj) {
        int retval = obj.q.getFrom().size() - q.getFrom().size();
        if (retval == 0) {
            retval = name.compareTo(obj.name);
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name + "/" + category + " (" + q.getFrom().size() + " tables)";
    }
}
