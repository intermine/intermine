package org.flymine.util;

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

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.CollectionDescriptor;

/**
 * Collection of commonly used Database utilities
 *
 * @author Andrew Varley
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
        String tableName = null;
        if (!cld.isInterface()) {
            while (cld.getSuperclassDescriptor() != null) {
                cld = cld.getSuperclassDescriptor();
            }
            tableName = TypeUtil.unqualifiedName(cld.getClassName());
        }
        return tableName;
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
        String indirectionTableName = null;
        if (FieldDescriptor.M_N_RELATION == col.relationType()) {
            ReferenceDescriptor rd = col.getReverseReferenceDescriptor();
            String cldName = col.getClassDescriptor().getClassName();
            String name1 = StringUtil.capitalise(rd == null 
                                      ? TypeUtil.unqualifiedName(col.getClassDescriptor()
                                                                 .getClassName())
                                      : rd.getName());
            String name2 = StringUtil.capitalise(col.getName());
            indirectionTableName = name1.compareTo(name2) < 0 ? name1 + name2 : name2 + name1;
        }
        return indirectionTableName;
    }
    
    /**
     * Creates a column name for the "inward" key of a many-to-many collection descriptor
     *
     * @param col CollectionDescriptor
     * @return a valid column name
     */
    public static String getInwardIndirectionColumnName(CollectionDescriptor col) {
        return TypeUtil.unqualifiedName(col.getClassDescriptor().getClassName()) + "Id";
    }

    /**
     * Creates a column name for the "outward" key of a many-to-many collection descriptor
     *
     * @param col CollectionDescriptor
     * @return a valid column name
     */
    public static String getOutwardIndirectionColumnName(CollectionDescriptor col) {
        return  TypeUtil.unqualifiedName(col.getReferencedClassDescriptor().getClassName()) + "Id";
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
        if (n.equalsIgnoreCase("id")) {
            return StringUtil.toSameInitialCase("identifier", n);
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
        return n;
    }
}
