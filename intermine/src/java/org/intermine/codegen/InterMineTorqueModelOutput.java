package org.flymine.codegen;

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

import org.flymine.util.DatabaseUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.CollectionDescriptor;

/**
 * Map FlyMine metadata to a Torque database schema in FlyMine format
 *
 * @author Andrew Varley
 */
public class FlyMineTorqueModelOutput extends ModelOutput
{
    static final String ID = "ID";
    static final String CLASS = "CLASS";

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
        className = cld.getUnqualifiedName();

        // Every class and interface has a seperate table
        sb.append(INDENT + "<table name=\"" + className + "\">" + ENDL)
            .append(generateColumn("ID", "int", true))
            .append(generateColumn("OBJECT", "java.lang.String", false))
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
                    sb.append(generateColumn(colName, type, false));
                    columns.add(colName);
                    if (fd.isPrimaryKey()) {
                        indices.append(generateIndex(className,
                                                     colName));
                    }
                }
            }
        }
        // All interfaces recursively to top of inheritence tree
        Iterator interfaceIter = cld.getInterfaceDescriptors().iterator();
        while  (interfaceIter.hasNext()) {
            sb.append(generateSuperclasses((ClassDescriptor) interfaceIter.next()));
        }
        // All classes recursively to top of inheritence tree
        ClassDescriptor superCld = cld.getSuperclassDescriptor();
        if (superCld != null) {
            sb.append(generateSuperclasses(superCld));
        }
        return sb.toString();
    }

    private String generateColumn(String name, String type, boolean isPrimaryKey) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT + INDENT + "<column name=\"")
            .append(name)
            .append("\" type=\"")
            .append(OJBModelOutput.generateJdbcType(type))
            .append("\"")
            .append(isPrimaryKey ? " required=\"true\" primaryKey=\"true\"" : "")
            .append("/>" + ENDL);
        return sb.toString();
    }

    private String generateIndirectionTable(CollectionDescriptor col) {
        StringBuffer sb = new StringBuffer();
        String table = DatabaseUtil.getIndirectionTableName(col);
        String column1 = DatabaseUtil.getInwardIndirectionColumnName(col);
        String column2 = DatabaseUtil.getOutwardIndirectionColumnName(col);
        sb.append(INDENT + "<table name=\"")
            .append(table)
            .append("\">" + ENDL)
            .append(generateColumn(column1, "int", false))
            .append(generateColumn(column2, "int", false))
            .append(generateIndex(table, column1))
            .append(generateIndex(table, column2))
            .append(INDENT + "</table>" + ENDL);
        return sb.toString();
    }

    private String generateIndex(String table, String column) {
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
}
