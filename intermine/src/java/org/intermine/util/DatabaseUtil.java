package org.intermine.util;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.sql.Database;

import org.apache.log4j.Logger;

/**
 * Collection of commonly used Database utilities
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class DatabaseUtil
{
    private static final Logger LOG = Logger.getLogger(DatabaseUtil.class);
    private static final String[] RESERVED_WORDS = new String[] {
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
        "ASSERTION",
        "AT",
        "AUTHORIZATION",
        "AVG",
        "BEFORE",
        "BEGIN",
        "BETWEEN",
        "BINARY",
        "BIT",
        "BIT_LENGTH",
        "BLOB",
        "BOOLEAN",
        "BOTH",
        "BREADTH",
        "BY",
        "CALL",
        "CASCADE",
        "CASCADED",
        "CASE",
        "CAST",
        "CATALOG",
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
        "COLUMN",
        "COMMIT",
        "COMPLETION",
        "CONNECT",
        "CONNECTION",
        "CONSTRAINT",
        "CONSTRAINTS",
        "CONSTRUCTOR",
        "CONTINUE",
        "CONVERT",
        "CORRESPONDING",
        "COUNT",
        "CREATE",
        "CROSS",
        "CUBE",
        "CURRENT",
        "CURRENT_DATE",
        "CURRENT_PATH",
        "CURRENT_ROLE",
        "CURRENT_TIME",
        "CURRENT_TIMESTAMP",
        "CURRENT_USER",
        "CURSOR",
        "CYCLE",
        "DATA",
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
        "EXTERNAL",
        "EXTRACT",
        "FALSE",
        "FETCH",
        "FIRST",
        "FLOAT",
        "FOR",
        "FOREIGN",
        "FOUND",
        "FREE",
        "FREEZE",
        "FROM",
        "FULL",
        "FUNCTION",
        "GENERAL",
        "GET",
        "GLOBAL",
        "GO",
        "GOTO",
        "GRANT",
        "GROUP",
        "GROUPING",
        "HAVING",
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
        "LOCAL",
        "LOCALTIME",
        "LOCALTIMESTAMP",
        "LOCATOR",
        "LOWER",
        "MAP",
        "MATCH",
        "MAX",
        "MIN",
        "MINUTE",
        "MODIFIES",
        "MODIFY",
        "MODULE",
        "MONTH",
        "NAMES",
        "NATIONAL",
        "NATURAL",
        "NCHAR",
        "NCLOB",
        "NEW",
        "NEXT",
        "NO",
        "NONE",
        "NOT",
        "NOTNULL",
        "NULL",
        "NULLIF",
        "NUMERIC",
        "OBJECT",
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
        "OVERLAPS",
        "PAD",
        "PARAMETER",
        "PARAMETERS",
        "PARTIAL",
        "PATH",
        "PLACING",
        "POSITION",
        "POSTFIX",
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
        "READ",
        "READS",
        "REAL",
        "RECURSIVE",
        "REF",
        "REFERENCES",
        "REFERENCING",
        "RELATIVE",
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
        "SAVEPOINT",
        "SCHEMA",
        "SCOPE",
        "SCROLL",
        "SEARCH",
        "SECOND",
        "SECTION",
        "SELECT",
        "SEQUENCE",
        "SESSION",
        "SESSION_USER",
        "SET",
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
        "START",
        "STATE",
        "STATEMENT",
        "STATIC",
        "STRUCTURE",
        "SUBSTRING",
        "SUM",
        "SYSTEM_USER",
        "TABLE",
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
        "VERBOSE",
        "VIEW",
        "WHEN",
        "WHENEVER",
        "WHERE",
        "WITH",
        "WITHOUT",
        "WORK",
        "WRITE",
        "YEAR",
        "ZONE"};
    private static Set reservedWords = new HashSet();
    static {
        for (int i = 0; i < RESERVED_WORDS.length; i++) {
            reservedWords.add(RESERVED_WORDS[i]);
        }
    }

    private DatabaseUtil() {
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

        ResultSet res = con.getMetaData().getTables(null, null, tableName, null);

        while (res.next()) {
            if (res.getString(3).equals(tableName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a table name for a class descriptor
     *
     * @param cld ClassDescriptor
     * @return a valid table name
     */
    public static String getTableName(ClassDescriptor cld) {
        return cld.getUnqualifiedName();
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

        String cldName = col.getClassDescriptor().getName();
        String name1 = getInwardIndirectionColumnName(col);
        String name2 = getOutwardIndirectionColumnName(col);
        return name1.compareTo(name2) < 0 ? name1 + name2 : name2 + name1;
    }

    /**
     * Creates a column name for the "inward" key of a many-to-many collection descriptor
     *
     * @param col CollectionDescriptor
     * @return a valid column name
     */
    public static String getInwardIndirectionColumnName(CollectionDescriptor col) {
        if (FieldDescriptor.M_N_RELATION != col.relationType()) {
            throw new IllegalArgumentException("Argument must be a CollectionDescriptor for a "
                                               + "many-to-many relation");
        }

        return StringUtil.capitalise(generateSqlCompatibleName(col.getName()));
    }

    /**
     * Creates a column name for the "outward" key of a many-to-many collection descriptor
     *
     * @param col CollectionDescriptor
     * @return a valid column name
     */
    public static String getOutwardIndirectionColumnName(CollectionDescriptor col) {
        if (FieldDescriptor.M_N_RELATION != col.relationType()) {
            throw new IllegalArgumentException("Argument must be a CollectionDescriptor for a "
                                               + "many-to-many relation");
        }

        ReferenceDescriptor rd = col.getReverseReferenceDescriptor();
        String colName = (rd == null
            ? TypeUtil.unqualifiedName(col.getClassDescriptor().getName())
            : rd.getName());
        return StringUtil.capitalise(generateSqlCompatibleName(colName));
    }

    /**
     * Convert any sql keywords to valid names for tables/columns.
     * @param n the string to convert
     * @return a valid sql name
     */
    public static String generateSqlCompatibleName(String n) {
        String upper = n.toUpperCase();
        if (upper.startsWith("INTERMINE_") || reservedWords.contains(upper)) {
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
    public static String objectToString(Object o) throws IllegalArgumentException {
        if (o instanceof Float) {
            return o.toString() + "::REAL";
        } else if (o instanceof Number) {
            return o.toString();
        } else if (o instanceof String) {
            return "'" + StringUtil.duplicateQuotes((String) o) + "'";
        } else if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue() ? "'true'" : "'false'";
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
        Set tables = new HashSet();
        tables.add(getTableName(cld));
        Iterator iter = cld.getAllCollectionDescriptors().iterator();
        while (iter.hasNext()) {
            CollectionDescriptor col = (CollectionDescriptor) iter.next();
            if (FieldDescriptor.M_N_RELATION == col.relationType()) {
                tables.add(getIndirectionTableName(col));
            }
        }

        Connection conn = db.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(true);
            Statement s = conn.createStatement();
            Iterator tablesIter = tables.iterator();
            while (tablesIter.hasNext()) {
                if (full) {
                    String sql = "VACUUM FULL ANALYSE " + (String) tablesIter.next();
                    LOG.info(sql);
                    s.execute(sql);
                } else {
                    String sql = "ANALYSE " + (String) tablesIter.next();
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
}
