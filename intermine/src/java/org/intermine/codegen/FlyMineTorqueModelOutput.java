package org.intermine.codegen;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.intermine.util.DatabaseUtil;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.CollectionDescriptor;

/**
 * Map FlyMine metadata to a Torque database schema in FlyMine format
 *
 * @author Andrew Varley
 */
public class FlyMineTorqueModelOutput extends ModelOutput
{
    protected Set indirections = new HashSet();
    protected String className = "";
    protected Set columns = new HashSet();
    protected StringBuffer indices;

    /**
     * @see ModelOutput#ModelOutput(Model, File)
     */
    public FlyMineTorqueModelOutput(Model model, File file) throws Exception {
        super(model, file);
    }

    /**
     * @see ModelOutput#process
     */
    public void process() {
        File path = new File(file, model.getName() + "-schema.xml");
        initFile(path);
        outputToFile(path, generate(model));
    }

    /**
     * @see ModelOutput#generate(Model)
     */
    protected String generate(Model model) {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>" + ENDL)
            .append("<!DOCTYPE database SYSTEM \""
                    + "http://jakarta.apache.org/turbine/dtd/database.dtd\">" + ENDL)
            .append("<database name=\"\">" + ENDL);

        Iterator iter = model.getClassDescriptors().iterator();
        while (iter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            sb.append(generate(cld));
        }

        Iterator indirectionIter = indirections.iterator();
        while (indirectionIter.hasNext()) {
            sb.append(generateIndirectionTable((CollectionDescriptor) indirectionIter.next()));
        }

        sb.append("</database>" + ENDL);
        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(ClassDescriptor)
     */
    protected String generate(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();
        indices = new StringBuffer();
        columns = new HashSet();
        className = DatabaseUtil.getTableName(cld);

        // Every class and interface has a separate table
        sb.append(INDENT + "<table name=\"" + className + "\">" + ENDL)
            .append(generateColumn("OBJECT", "java.lang.String"))
            .append(generateSuperclasses(cld))
            .append(indices)
            .append(INDENT + "</table>" + ENDL);
        return sb.toString();
    }

    /**
     * @see ModelOutput#generate(AttributeDescriptor)
     */
    protected String generate(AttributeDescriptor attr) {
        return null;
    }

    /**
     * @see ModelOutput#generate(ReferenceDescriptor)
     */
    protected String generate(ReferenceDescriptor ref) {
        return null;
    }

    /**
     * @see ModelOutput#generate(CollectionDescriptor)
     */
    protected String generate(CollectionDescriptor col) {
        return null;
    }

    private String generateSuperclasses(ClassDescriptor cld) {
        StringBuffer sb = new StringBuffer();
        Iterator fieldIter = cld.getFieldDescriptors().iterator();
        while (fieldIter.hasNext()) {
            FieldDescriptor fd = (FieldDescriptor) fieldIter.next();
            if (fd instanceof CollectionDescriptor) {
                CollectionDescriptor col = (CollectionDescriptor) fd;
                if (FieldDescriptor.M_N_RELATION == col.relationType()) {
                    if (!indirections.contains(col.getReverseReferenceDescriptor())) {
                        indirections.add(col);
                    }
                }
            } else {
                String type = fd instanceof AttributeDescriptor
                    ? ((AttributeDescriptor) fd).getType()
                    : "int";
                String colName = DatabaseUtil.getColumnName(fd);
                if (!columns.contains(colName)) {
                    sb.append(generateColumn(colName, type));
                    columns.add(colName);
                    // note that primary key indices are built automatically by postgres
                }
            }
        }
        // All interfaces and classes recursively to top of inheritance tree
        Iterator interfaceIter = cld.getSuperDescriptors().iterator();
        while  (interfaceIter.hasNext()) {
            sb.append(generateSuperclasses((ClassDescriptor) interfaceIter.next()));
        }
        return sb.toString();
    }

    private String generateColumn(String name, String type) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT + INDENT + "<column name=\"")
            .append(name)
            .append("\" type=\"")
            .append(generateJdbcType(type))
            .append("\"")
            .append("id".equals(name) ? " required=\"true\" primaryKey=\"true\"" : "")
            .append("/>" + ENDL);
        return sb.toString();
    }

    /**
     * Generate an indirection table
     *
     * @param col the CollectionDescriptor to generate table for
     * @return a String representing the schema for the indirection table
     */
    protected String generateIndirectionTable(CollectionDescriptor col) {
        StringBuffer sb = new StringBuffer();
        String table = DatabaseUtil.getIndirectionTableName(col);
        String column1 = DatabaseUtil.getInwardIndirectionColumnName(col);
        String column2 = DatabaseUtil.getOutwardIndirectionColumnName(col);
        sb.append(INDENT + "<table name=\"")
            .append(table)
            .append("\">" + ENDL)
            .append(generateColumn(column1, "int"))
            .append(generateColumn(column2, "int"))
            //.append(generateIndex(table, column1))
            //.append(generateIndex(table, column2))
            .append(INDENT + "</table>" + ENDL);
        return sb.toString();
    }

    /**
     * Generate an index for a specifed column of a table
     *
     * @param table the name of the table
     * @param column the name of the column
     * @return the index XML fragment
     */
    protected String generateIndex(String table, String column) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT + INDENT + "<index name=\"")
            .append(table)
            .append("_")
            .append(column)
            .append("\">" + ENDL)
            .append(INDENT + INDENT + INDENT + "<index-column name=\"")
            .append(column)
            .append("\"/>" + ENDL)
            .append(INDENT + INDENT + "</index>" + ENDL);
        return sb.toString();
    }

    /**
     * Convert java primitive and object names to those compatible
     * with torque.  Returns unaltered string if no
     * conversion is required.
     * @param type the string to convert
     * @return torque compatible name
     */
    public static String generateJdbcType(String type) {
        if (type.equals("short") || type.equals("java.lang.Short")) {
            return "SMALLINT";
        } else if (type.equals("int") || type.equals("java.lang.Integer")) {
            return "INTEGER";
        } else if (type.equals("long") || type.equals("java.lang.Long")) {
            return "BIGINT";
        } else if (type.equals("java.lang.String")) {
            return "LONGVARCHAR";
        } else if (type.equals("boolean") || type.equals("java.lang.Boolean")) {
            return "BIT";
        } else if (type.equals("float") || type.equals("java.lang.Float")) {
            return "REAL";
        } else if (type.equals("double") || type.equals("java.lang.Double")) {
            return "DOUBLE";
        } else if (type.equals("java.util.Date")) {
            return "DATE";
        } else if (type.equals("java.math.BigDecimal")) {
            return "NUMERIC";
        }
        return type;
    }
}
