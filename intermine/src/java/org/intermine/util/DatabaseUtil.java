package org.intermine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.CollectionDescriptor;

/**
 * Collection of commonly used Database utilities
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class DatabaseUtil
{
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
        //n should start with a lower case letter
        if (n.equalsIgnoreCase("end")) {
            return StringUtil.toSameInitialCase("finish", n);
        }
        if (n.equalsIgnoreCase("index")) {
            return StringUtil.toSameInitialCase("indx", n);
        }
        if (n.equalsIgnoreCase("order")) {
            return StringUtil.toSameInitialCase("ordr", n);
        }
        if (n.equalsIgnoreCase("full")) {
            return StringUtil.toSameInitialCase("complete", n);
        }
        if (n.equalsIgnoreCase("offset")) {
            return StringUtil.toSameInitialCase("offst", n);
        }
        if (n.equalsIgnoreCase("references")) {
            return StringUtil.toSameInitialCase("refs", n);
        }
        return n;
    }

    /**
     * Generate an SQL compatible representation of an object.
     *
     * @param o the Object
     * @return a valid SQL String
     * @throws IllegalArgumentException if the object is not representable
     */
    public static String objectToString(Object o) throws IllegalArgumentException {
        if (o instanceof Date) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return "'" + format.format((Date) o) + "'";
        } else if (o instanceof Float) {
            return o.toString() + "::REAL";
        } else if (o instanceof Number) {
            return o.toString();
        } else if (o instanceof String) {
            return "'" + StringUtil.duplicateQuotes((String) o) + "'";
        } else if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue() ? "'true'" : "'false'";
        } else {
            throw new IllegalArgumentException("Can't convert " + o + " into an SQL String");
        }
    }
}
