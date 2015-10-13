package org.intermine.sql;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.StringUtil;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;
import org.intermine.sql.writebatch.FlushJob;
import org.intermine.sql.writebatch.TableBatch;

/**
 * Collection of commonly used Database utilities
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public final class DatabaseUtil
{
    private static final Logger LOG = Logger.getLogger(DatabaseUtil.class);
    private static final Set<String> RESERVED_WORDS = new HashSet<String>(Arrays.asList(
            "ABS",
        "ABSOLUTE",
        "ACTION",
        "ADD",
        "ADMIN",
        "AFTER",
        "AGGREGATE",
        "ALIAS",
        "ALL",
        "ALLOCATE",
        "ALTER",
        "ANALYSE",
        "ANALYZE",
        "AND",
        "ANY",
        "ARE",
        "ARRAY",
        "AS",
        "ASC",
        "ASENSITIVE",
        "ASSERTION",
        "ASYMMETRIC",
        "AT",
        "ATOMIC",
        "AUTHORIZATION",
        "AVG",
        "BEFORE",
        "BEGIN",
        "BETWEEN",
        "BIGINT",
        "BINARY",
        "BIT",
        "BIT_LENGTH",
        "BLOB",
        "BOOLEAN",
        "BOTH",
        "BREADTH",
        "BY",
        "CALL",
        "CALLED",
        "CARDINALITY",
        "CASCADE",
        "CASCADED",
        "CASE",
        "CAST",
        "CATALOG",
        "CEIL",
        "CEILING",
        "CHAR",
        "CHARACTER",
        "CHARACTER_LENGTH",
        "CHAR_LENGTH",
        "CHECK",
        "CLASS",
        "CLOB",
        "CLOSE",
        "COALESCE",
        "COLLATE",
        "COLLATION",
        "COLLECT",
        "COLUMN",
        "COMMIT",
        "COMPLETION",
        "CONDITION",
        "CONNECT",
        "CONNECTION",
        "CONSTRAINT",
        "CONSTRAINTS",
        "CONSTRUCTOR",
        "CONTINUE",
        "CONVERT",
        "CORR",
        "CORRESPONDING",
        "COUNT",
        "COVAR_POP",
        "COVAR_SAMP",
        "CREATE",
        "CROSS",
        "CUBE",
        "CUME_DIST",
        "CURRENT",
        "CURRENT_DATE",
        "CURRENT_DEFAULT_TRAN",
        "CURRENT_PATH",
        "CURRENT_ROLE",
        "CURRENT_TIME",
        "CURRENT_TIMESTAMP",
        "CURRENT_TRANSFORM_GR",
        "CURRENT_USER",
        "CURSOR",
        "CYCLE",
        "DATA",
        "DATABASE",
        "DATE",
        "DAY",
        "DEALLOCATE",
        "DEC",
        "DECIMAL",
        "DECLARE",
        "DEFAULT",
        "DEFERRABLE",
        "DEFERRED",
        "DELETE",
        "DENSE_RANK",
        "DEPTH",
        "DEREF",
        "DESC",
        "DESCRIBE",
        "DESCRIPTOR",
        "DESTROY",
        "DESTRUCTOR",
        "DETERMINISTIC",
        "DIAGNOSTICS",
        "DICTIONARY",
        "DISCONNECT",
        "DISTINCT",
        "DO",
        "DOMAIN",
        "DOUBLE",
        "DROP",
        "DYNAMIC",
        "EACH",
        "ELEMENT",
        "ELSE",
        "END",
        "END-EXEC",
        "EQUALS",
        "ESCAPE",
        "EVERY",
        "EXCEPT",
        "EXCEPTION",
        "EXEC",
        "EXECUTE",
        "EXISTS",
        "EXP",
        "EXTERNAL",
        "EXTRACT",
        "FALSE",
        "FETCH",
        "FILTER",
        "FIRST",
        "FLOAT",
        "FLOOR",
        "FOR",
        "FOREIGN",
        "FOUND",
        "FREE",
        "FREEZE",
        "FROM",
        "FULL",
        "FUNCTION",
        "FUSION",
        "GENERAL",
        "GET",
        "GLOBAL",
        "GO",
        "GOTO",
        "GRANT",
        "GROUP",
        "GROUPING",
        "HAVING",
        "HOLD",
        "HOST",
        "HOUR",
        "IDENTITY",
        "IGNORE",
        "ILIKE",
        "IMMEDIATE",
        "IN",
        "INDICATOR",
        "INITIALIZE",
        "INITIALLY",
        "INNER",
        "INOUT",
        "INPUT",
        "INSENSITIVE",
        "INSERT",
        "INT",
        "INTEGER",
        "INTERSECT",
        "INTERSECTION",
        "INTERVAL",
        "INTO",
        "IS",
        "ISNULL",
        "ISOLATION",
        "ITERATE",
        "JOIN",
        "KEY",
        "LANGUAGE",
        "LARGE",
        "LAST",
        "LATERAL",
        "LEADING",
        "LEFT",
        "LESS",
        "LEVEL",
        "LIKE",
        "LIMIT",
        "LN",
        "LOCAL",
        "LOCALTIME",
        "LOCALTIMESTAMP",
        "LOCATOR",
        "LOWER",
        "MAP",
        "MATCH",
        "MAX",
        "MEMBER",
        "MERGE",
        "METHOD",
        "MIN",
        "MINUTE",
        "MOD",
        "MODIFIES",
        "MODIFY",
        "MODULE",
        "MONTH",
        "MULTISET",
        "NAMES",
        "NATIONAL",
        "NATURAL",
        "NCHAR",
        "NCLOB",
        "NEW",
        "NEXT",
        "NO",
        "NONE",
        "NORMALIZE",
        "NOT",
        "NOTNULL",
        "NULL",
        "NULLIF",
        "NUMERIC",
        "OBJECT",
        "OBJECTCLASS",
        "OCTET_LENGTH",
        "OF",
        "OFF",
        "OFFSET",
        "OLD",
        "ON",
        "ONLY",
        "OPEN",
        "OPERATION",
        "OPTION",
        "OR",
        "ORDER",
        "ORDINALITY",
        "OUT",
        "OUTER",
        "OUTPUT",
        "OVER",
        "OVERLAPS",
        "OVERLAY",
        "PAD",
        "PARAMETER",
        "PARAMETERS",
        "PARTIAL",
        "PARTITION",
        "PATH",
        "PERCENTILE_CONT",
        "PERCENTILE_DISC",
        "PERCENT_RANK",
        "PLACING",
        "POSITION",
        "POSTFIX",
        "POWER",
        "PRECISION",
        "PREFIX",
        "PREORDER",
        "PREPARE",
        "PRESERVE",
        "PRIMARY",
        "PRIOR",
        "PRIVILEGES",
        "PROCEDURE",
        "PUBLIC",
        "RANGE",
        "READ",
        "READS",
        "REAL",
        "RECURSIVE",
        "REF",
        "REFERENCES",
        "REFERENCING",
        "REGR_AVGX",
        "REGR_AVGY",
        "REGR_COUNT",
        "REGR_INTERCEPT",
        "REGR_R2",
        "REGR_SLOPE",
        "REGR_SXX",
        "REGR_SXY",
        "REGR_SYY",
        "RELATIVE",
        "RELEASE",
        "RESTRICT",
        "RESULT",
        "RETURN",
        "RETURNS",
        "REVOKE",
        "RIGHT",
        "ROLE",
        "ROLLBACK",
        "ROLLUP",
        "ROUTINE",
        "ROW",
        "ROWS",
        "ROW_NUMBER",
        "SAVEPOINT",
        "SCHEMA",
        "SCOPE",
        "SCROLL",
        "SEARCH",
        "SECOND",
        "SECTION",
        "SELECT",
        "SENSITIVE",
        "SEQUENCE",
        "SESSION",
        "SESSION_USER",
        "SET",
        "SETOF",
        "SETS",
        "SIMILAR",
        "SIZE",
        "SMALLINT",
        "SOME",
        "SPACE",
        "SPECIFIC",
        "SPECIFICTYPE",
        "SQL",
        "SQLCODE",
        "SQLERROR",
        "SQLEXCEPTION",
        "SQLSTATE",
        "SQLWARNING",
        "SQRT",
        "START",
        "STATE",
        "STATEMENT",
        "STATIC",
        "STDDEV_POP",
        "STDDEV_SAMP",
        "STRUCTURE",
        "SUBMULTISET",
        "SUBSTRING",
        "SUM",
        "SYMMETRIC",
        "SYSTEM",
        "SYSTEM_USER",
        "TABLE",
        "TABLESAMPLE",
        "TEMPORARY",
        "TERMINATE",
        "THAN",
        "THEN",
        "TIME",
        "TIMESTAMP",
        "TIMEZONE_HOUR",
        "TIMEZONE_MINUTE",
        "TO",
        "TRAILING",
        "TRANSACTION",
        "TRANSLATE",
        "TRANSLATION",
        "TREAT",
        "TRIGGER",
        "TRIM",
        "TRUE",
        "UESCAPE",
        "UNDER",
        "UNION",
        "UNIQUE",
        "UNKNOWN",
        "UNNEST",
        "UPDATE",
        "UPPER",
        "USAGE",
        "USER",
        "USING",
        "VALUE",
        "VALUES",
        "VARCHAR",
        "VARIABLE",
        "VARYING",
        "VAR_POP",
        "VAR_SAMP",
        "VERBOSE",
        "VIEW",
        "WHEN",
        "WHENEVER",
        "WHERE",
        "WIDTH_BUCKET",
        "WINDOW",
        "WITH",
        "WITHIN",
        "WITHOUT",
        "WORK",
        "WRITE",
        "YEAR",
        "ZONE"));

    /**
     * @author Matthew
     */
    public enum Type {
        /**
         * text
         */
        text("TEXT"),
        /**
         * integer
         */
        integer("integer"),
        /**
         * big int
         */
        bigint("bigint"),
        /**
         * real
         */
        real("real"),
        /**
         * double
         */
        double_precision("double precision"),
        /**
         * timestampe
         */
        timestamp("timestamp"),
        /**
         * boolean
         */
        boolean_type("boolean"),
        /**
         * uuid
         */
        uuid("uuid");

        private final String sqlType;

        /**
         * @param sqlType set sql type
         */
        Type(String sqlType) {
            this.sqlType = sqlType;
        }

        /**
         * @return sql type
         */
        String getSQLType() {
            return sqlType;
        }
    }

    private DatabaseUtil() {
        // empty
    }

    /**
     * Tests if a table exists in the database
     *
     * @param con a connection to a database
     * @param tableName the name of a table to test for
     * @return true if the table exists, false otherwise
     * @throws SQLException if an error occurs in the underlying database
     * @throws NullPointerException if tableName is null
     */
    public static boolean tableExists(Connection con, String tableName) throws SQLException {
        if (tableName == null) {
            throw new NullPointerException("tableName cannot be null");
        }

        ResultSet res = con.getMetaData().getTables(null, null, null, new String[] {"TABLE"});

        while (res.next()) {
            if (res.getString(3).equalsIgnoreCase(tableName) && "TABLE".equals(res.getString(4))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests if a column exists in the database
     *
     * @param con a connection to a database
     * @param tableName the name of a table containing the column
     * @param columnName the name of the column to test for
     * @return true if the column exists, false otherwise
     * @throws SQLException if an error occurs in the underlying database
     * @throws NullPointerException if tableName is null
     */
    public static boolean columnExists(Connection con, String tableName, String columnName)
        throws SQLException {
        if (tableName == null) {
            throw new NullPointerException("tableName cannot be null");
        }
        if (columnName == null) {
            throw new NullPointerException("columnName cannot be null");
        }

        ResultSet res = con.getMetaData().getColumns(null, null, tableName, columnName);

        while (res.next()) {
            if (res.getString(3).equals(tableName)
                && res.getString(4).equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes every single table from the database given.
     *
     * @param con the Connection to the database
     * @throws SQLException if an error occurs in the underlying database
     */
    public static void removeAllTables(Connection con) throws SQLException {
        ResultSet res = con.getMetaData().getTables(null, null, "%", null);
        Set<String> tablenames = new HashSet<String>();
        while (res.next()) {
            String tablename = res.getString(3);
            if ("TABLE".equals(res.getString(4))) {
                tablenames.add(tablename);
            }
            if ("VIEW".equals(res.getString(4))) {
                LOG.info("Dropping view " + tablename);
                con.createStatement().execute("DROP VIEW " + tablename);
            }
        }
        for (String tablename : tablenames) {
            LOG.info("Dropping table " + tablename);
            con.createStatement().execute("DROP TABLE " + tablename);
        }
    }

    /**
     * Remove the sequence from the database given.
     *
     * @param con the Connection to the database
     * @param sequence the sequence to remove
     * @throws SQLException if an error occurs in the underlying database
     */
    public static void removeSequence(Connection con, String sequence) throws SQLException {
        LOG.info("Dropping sequence " + sequence);
        con.createStatement().execute("DROP SEQUENCE " + sequence);
    }

    /**
     * Remove the view from the database given.
     *
     * @param con the Connection to the database
     * @param view the view to remove
     * @throws SQLException if an error occurs in the underlying database
     */
    public static void removeView(Connection con, String view) throws SQLException {
        LOG.info("Dropping view " + view);
        con.createStatement().execute("DROP VIEW IF EXISTS " + view);
    }


    /**
     * Creates a table name for a class descriptor
     *
     * @param cld ClassDescriptor
     * @return a valid table name
     */
    public static String getTableName(ClassDescriptor cld) {
        return generateSqlCompatibleName(cld.getUnqualifiedName());
    }

    /**
     * Creates a column name for a field descriptor
     *
     * @param fd FieldDescriptor
     * @return a valid column name
     */
    public static String getColumnName(FieldDescriptor fd) {
        if (fd instanceof AttributeDescriptor) {
            return generateSqlCompatibleName(fd.getName());
        }
        if (fd instanceof CollectionDescriptor) {
            return null;
        }
        if (fd instanceof ReferenceDescriptor) {
            return fd.getName() + "Id";
        }
        return null;
    }

    /**
     * Creates an indirection table name for a many-to-many collection descriptor
     *
     * @param col CollectionDescriptor
     * @return a valid table name
     */
    public static String getIndirectionTableName(CollectionDescriptor col) {
        if (FieldDescriptor.M_N_RELATION != col.relationType()) {
            throw new IllegalArgumentException("Argument must be a CollectionDescriptor for a "
                                               + "many-to-many relation");
        }

        String name1 = getInwardIndirectionColumnName(col, 0);
        String name2 = getOutwardIndirectionColumnName(col, 0);
        return name1.compareTo(name2) < 0 ? name1 + name2 : name2 + name1;
    }

    /**
     * Creates a column name for the "inward" key of a many-to-many collection descriptor.
     *
     * @param col CollectionDescriptor
     * @param version the database version number
     * @return a valid column name
     */
    public static String getInwardIndirectionColumnName(CollectionDescriptor col, int version) {
        if (FieldDescriptor.M_N_RELATION != col.relationType()) {
            throw new IllegalArgumentException("Argument must be a CollectionDescriptor for a "
                                               + "many-to-many relation");
        }

        if (version == 0) {
            return StringUtil.capitalise(generateSqlCompatibleName(col.getName()));
        } else if (version == 1) {
            ReferenceDescriptor rd = col.getReverseReferenceDescriptor();
            String colName = (rd == null
                ? TypeUtil.unqualifiedName(col.getClassDescriptor().getName())
                : rd.getName());
            return StringUtil.capitalise(generateSqlCompatibleName(colName));
        } else {
            throw new IllegalArgumentException("Database version number " + version
                    + " not recognised");
        }
    }

    /**
     * Creates a column name for the "outward" key of a many-to-many collection descriptor.
     *
     * @param col CollectionDescriptor
     * @param version the database version number
     * @return a valid column name
     */
    public static String getOutwardIndirectionColumnName(CollectionDescriptor col, int version) {
        if (FieldDescriptor.M_N_RELATION != col.relationType()) {
            throw new IllegalArgumentException("Argument must be a CollectionDescriptor for a "
                                               + "many-to-many relation");
        }

        if (version == 0) {
            ReferenceDescriptor rd = col.getReverseReferenceDescriptor();
            String colName = (rd == null
                ? TypeUtil.unqualifiedName(col.getClassDescriptor().getName())
                : rd.getName());
            return StringUtil.capitalise(generateSqlCompatibleName(colName));
        } else if (version == 1) {
            return StringUtil.capitalise(generateSqlCompatibleName(col.getName()));
        } else {
            throw new IllegalArgumentException("Database version number " + version
                    + " not recognised");
        }
    }

    /**
     * Convert any sql keywords to valid names for tables/columns.
     * @param n the string to convert
     * @return a valid sql name
     */
    public static String generateSqlCompatibleName(String n) {
        String upper = n.toUpperCase();
        if (upper.startsWith("INTERMINE_") || RESERVED_WORDS.contains(upper)) {
            return "intermine_" + n;
        } else {
            return n;
        }
    }

    /**
     * Generate an SQL compatible representation of an object.
     *
     * @param o the Object
     * @return a valid SQL String
     * @throws IllegalArgumentException if the object is not representable
     */
    public static String objectToString(Object o) {
        if (o instanceof Float) {
            return o.toString() + "::REAL";
        } else if (o instanceof Number) {
            return o.toString();
        } else if (o instanceof String) {
            String s = (String) o;
            if (s.indexOf('\\') != -1) {
                return "E'" + StringUtil.escapeWithBackslashes(s) + "'";
            } else {
                return "'" + StringUtil.duplicateQuotes(s) + "'";
            }
        } else if (o instanceof CharSequence) {
            return objectToString(((CharSequence) o).toString());
        } else if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue() ? "'true'" : "'false'";
        } else if (o instanceof Class<?>) {
            return objectToString(((Class<?>) o).getName());
        } else if (o == null) {
            return "NULL";
        } else {
            throw new IllegalArgumentException("Can't convert " + o + " into an SQL String");
        }
    }

    /**
     * Analyse given database, perform vacuum full analyse if full parameter true.
     * WARNING: currently PostgreSQL specific
     * @param db the database to analyse
     * @param full if true perform VACUUM FULL ANALYSE
     * @throws SQLException if db problem
     */
    public static void analyse(Database db, boolean full) throws SQLException {
        Connection conn = db.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(true);
            Statement s = conn.createStatement();
            if (full) {
                s.execute("VACUUM FULL ANALYSE");
            } else {
                s.execute("ANALYSE");
            }
            conn.setAutoCommit(autoCommit);
        } finally {
            conn.setAutoCommit(autoCommit);
            conn.close();
        }
    }


    /**
     * Analyse database table for a given class and all associated indirection tables.
     * WARNING: currently PostgreSQL specific
     * @param db the database to analyse
     * @param cld description of class to analyse
     * @param full if true perform VACUUM FULL ANALYSE
     * @throws SQLException if db problem
     */
    public static void analyse(Database db, ClassDescriptor cld, boolean full) throws SQLException {
        Set<String> tables = new HashSet<String>();
        tables.add(getTableName(cld));
        tables.addAll(getIndirectionTableNames(cld));

        Connection conn = db.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(true);
            Statement s = conn.createStatement();
            for (String table : tables) {
                if (full) {
                    String sql = "VACUUM FULL ANALYSE " + table;
                    LOG.info(sql);
                    s.execute(sql);
                } else {
                    String sql = "ANALYSE " + table;
                    LOG.info(sql);
                    s.execute(sql);
                }
            }
            conn.setAutoCommit(autoCommit);
        } finally {
            conn.setAutoCommit(autoCommit);
            conn.close();
        }
    }


    /**
     * Given a ClassDescriptor find names of all related indirection tables.
     * @param cld class to find tables for
     * @return a set of all indirection table names
     */
    public static Set<String> getIndirectionTableNames(ClassDescriptor cld) {
        Set<String> tables = new HashSet<String>();
        for (CollectionDescriptor col : cld.getAllCollectionDescriptors()) {
            if (FieldDescriptor.M_N_RELATION == col.relationType()) {
                tables.add(getIndirectionTableName(col));
            }
        }
        return tables;
    }

    /**
     * Grant permission on all tables for given user on specified database.
     * @param db the database to grant permissions on
     * @param user the username to grant permission to
     * @param perm permission to grant
     * @throws SQLException if db problem
     */
    public static void grant(Database db, String user, String perm) throws SQLException {
        Connection conn = db.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(true);
            Statement s = conn.createStatement();
            ResultSet res = conn.getMetaData().getTables(null, null, null, null);
            while (res.next()) {
                if ("TABLE".equals(res.getString(4))) {
                    String sql = "GRANT " + perm + " ON " + res.getString(3) + " TO " + user;
                    LOG.debug(sql);
                    s.execute(sql);
                }
            }
            conn.setAutoCommit(autoCommit);
        } finally {
            conn.setAutoCommit(autoCommit);
            conn.close();
        }
    }

    /**
     * Create a new table the holds the contents of the given Collection (bag).  The "Class c"
     * parameter selects which objects from the bag are put in the new table.  eg. if the bag
     * contains Integers and Strings and the parameter is Integer.class then the table will contain
     * only the Integers from the bag.  A Class of InterMineObject is handled specially: the new
     * table will contain the IDs of the objects, not the objects themselves.  The table will have
     * one column ("value").
     *
     * @param db the Database to access
     * @param con the Connection to use
     * @param tableName the name to use for the new table
     * @param bag the Collection to create a table for
     * @param c the type of objects to put in the new table
     * @throws SQLException if there is a database problem
     */
    public static void createBagTable(Database db, Connection con, String tableName,
            Collection<?> bag, Class<?> c)
        throws SQLException {

        String typeString;

        if (InterMineObject.class.isAssignableFrom(c)) {
            typeString = db.getColumnTypeString(Integer.class);
        } else {
            typeString = db.getColumnTypeString(c);

            if (typeString == null) {
                throw new IllegalArgumentException("unknown Class passed to createBagTable(): "
                                                   + c.getName());
            }
        }

        String tableCreateSql = "CREATE TABLE " + tableName + " (value " + typeString + ")";

        Statement s = con.createStatement();
        s.execute(tableCreateSql);

        TableBatch tableBatch = new TableBatch();
        String[] colNames = new String[] {"value"};

        for (Object o : bag) {
            if (c.isInstance(o) || (InterMineObject.class.isAssignableFrom(c)
                    && ProxyReference.class.isInstance(o))) {
                if (o instanceof InterMineObject) {
                    o = ((InterMineObject) o).getId();
                } else if (o instanceof Date) {
                    o = new Long(((Date) o).getTime());
                }
                tableBatch.addRow(o, colNames, new Object[] {o});
            }
        }
        List<FlushJob> flushJobs = (new BatchWriterPostgresCopyImpl()).write(con, Collections
             .singletonMap(tableName, tableBatch), null);
        for (FlushJob fj : flushJobs) {
            fj.flush();
        }

        String indexCreateSql = "CREATE INDEX " + tableName + "_index ON " + tableName + "(value)";

        s.execute(indexCreateSql);

        s.execute("ANALYSE " + tableName);
    }

    /**
     * Create the table 'bagvalues' containing the values of the key field objects
     * contained in a bag and an extra values
     * @param con the Connection to use
     * @throws SQLException if there is a database problem
     */
    public static void createBagValuesTables(Connection con)
        throws SQLException {
        String sqlTable = "CREATE TABLE bagvalues (savedbagid integer, value text, extra text)";
        String sqlIndex = "CREATE UNIQUE INDEX bagvalues_index1 ON bagvalues "
            + "(savedbagid, value, extra)";
        con.createStatement().execute(sqlTable);
        con.createStatement().execute(sqlIndex);
    }

    /**
     * Verify if 'bagvalues' table is empty
     * @param con the Connection to use
     * @return true if empty
     * @throws SQLException if there is a database problem
     */
    public static boolean isBagValuesEmpty(Connection con)
        throws SQLException {
        String sqlCount = "select count(*) from bagvalues";
        ResultSet result = con.createStatement().executeQuery(sqlCount);
        result.next();
        int bagValuesSize = result.getInt(1);
        return bagValuesSize == 0;
    }

    /**
     * Add a column in the table specified in input. A connection is obtained to the database
     * and automatically released after the addition of the column.
     * @param database the database to use
     * @param tableName the table where to add the column
     * @param columnName the column to add
     * @param type the type
     * @throws SQLException if there is a database problem
     */
    public static void addColumn(Database database, String tableName, String columnName,
                                Type type)
        throws SQLException {
        Connection connection = database.getConnection();
        if (DatabaseUtil.tableExists(connection, tableName)) {
            try {
                addColumn(connection, tableName, columnName, type);
            } finally {
                connection.close();
            }
        }
    }

    /**
     * Add a column to an existing database table, if it does not already exist.
     * It is the users responsibility to close the connection after use.
     * @param con A connection to the database.
     * @param tableName The table to add the database too
     * @param columnName The column to add
     * @param type The SQL type to add
     * @throws SQLException if something goes wrong
     */
    public static void addColumn(Connection con, String tableName, String columnName, Type type)
        throws SQLException {
        if (!DatabaseUtil.tableExists(con, tableName)) {
            throw new IllegalArgumentException("there is no table named " + tableName + " in this"
                    + " database to add a new column to");
        }
        if (DatabaseUtil.columnExists(con, tableName, columnName)) {
            return;
        }
        if (!DatabaseUtil.isLegalColumnName(columnName)) {
            throw new IllegalArgumentException("This is not a legal column name: " + columnName);
        }
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName
                      + " " + type.getSQLType();
        PreparedStatement stmt = con.prepareStatement(sql);
        LOG.info(stmt.toString());
        stmt.executeUpdate();
    }

    /**
     * Check that a column name provided to us is a legal column name, to prevent SQL injection.
     * @param name The desired column name.
     * @return Whether or not we should accept it.
     */
    protected static boolean isLegalColumnName(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        boolean isValid = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            isValid = isValid && (CharUtils.isAsciiAlphaLower(c) || CharUtils.isAsciiNumeric(c)
                    || c == '_');
        }
        return isValid;
    }

    /**
     * Set the default value in a column for all values where the current value is null.
     * @param database the database to use
     * @param tableName the table where update the column
     * @param columnName the column to Update
     * @param newValue the value to update
     * @throws SQLException if there is a database problem
     */
    public static void updateColumnValue(Database database, String tableName, String columnName,
                                         Object newValue)
        throws SQLException {
        Connection connection = database.getConnection();
        try {
            updateColumnValue(connection, tableName, columnName, newValue);
        } finally {
            connection.close();
        }
    }

    /**
     * Set the default value in a column for all values.
     * @param con A connection to the database to use
     * @param tableName the table where update the column
     * @param columnName the column to Update
     * @param newValue the value to update
     * @throws SQLException if there is a database problem
     *
     * Note, it is the user's responsibility to ensure the connection given is closed.
     */
    public static void updateColumnValue(Connection con, String tableName, String columnName,
            Object newValue) throws SQLException {
        if (DatabaseUtil.columnExists(con, tableName, columnName)) {
            String sql = "UPDATE " + tableName + " SET " + columnName + " = ?";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setObject(1, newValue);
            LOG.info(stmt.toString());
            stmt.executeUpdate();
        }
    }

    /**
     *
     * @param con database connection
     * @param tableName table name
     * @param columnName column name
     * @param columnType column type
     * @return true if column exists
     */
    public static boolean verifyColumnType (Connection con, String tableName,
            String columnName, int columnType) {
        try {
            if (DatabaseUtil.tableExists(con, tableName)) {
                ResultSet res = con.getMetaData().getColumns(null, null,
                                                            tableName, columnName);

                while (res.next()) {
                    if (res.getString(3).equals(tableName)
                        && columnName.equals(res.getString(4))
                        && res.getInt(5) == columnType) {
                        return true;
                    }
                    return false;
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return true;
    }

    /**
     * @param db database
     * @param cd class descriptor
     * @return SQL to create table
     * @throws ClassNotFoundException table isn't in database
     */
    public static String getTableDefinition(Database db, ClassDescriptor cd)
        throws ClassNotFoundException {
        StringBuffer sb = new StringBuffer("CREATE TABLE " + DatabaseUtil.getTableName(cd) + " (");
        boolean needsComma = false;
        for (AttributeDescriptor ad: cd.getAllAttributeDescriptors()) {
            if (needsComma) {
                sb.append(",");
            }
            sb.append(" ");
            sb.append(DatabaseUtil.getColumnName(ad));
            sb.append(" ");
            sb.append(db.getColumnTypeString(Class.forName(ad.getType())));
            needsComma = true;
        }
        for (ReferenceDescriptor rd: cd.getAllReferenceDescriptors()) {
            if (needsComma) {
                sb.append(",");
            }
            sb.append(" ");
            sb.append(DatabaseUtil.getColumnName(rd));
            sb.append(" ");
            sb.append(db.getColumnTypeString(Integer.class));
            needsComma = true;
        }
        sb.append(")");
        return sb.toString();
    }
}

