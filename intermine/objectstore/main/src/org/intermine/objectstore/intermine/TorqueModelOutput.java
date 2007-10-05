package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.DatabaseUtil;

import org.apache.log4j.Logger;

/**
 * Map InterMine metadata to a Torque database schema in InterMine format
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class TorqueModelOutput
{
    private static final Logger LOG = Logger.getLogger(TorqueModelOutput.class);

    protected static final String INDENT = "    ";
    protected static final String ENDL = System.getProperty("line.separator");

    protected DatabaseSchema schema;
    protected File file;
    protected Set indirections = new HashSet();
    protected String className = "";
    protected Set columns = new HashSet();

    /**
     * Constructor for this class
     *
     * @param schema a DatabaseSchema
     * @param file the file to which the torque data should be written
     */
    public TorqueModelOutput(DatabaseSchema schema, File file) {
        this.schema = schema;
        this.file = file;
    }

    /**
     * Process the schema and put the output in the file.
     */
    public void process() {
        LOG.info("Generating " + file.getPath());
        BufferedWriter fos = null;
        try {
            fos = new BufferedWriter(new FileWriter(file, false));
            fos.write(generate());
        } catch (IOException e) {
            LOG.error("Failed to output torque data to file " + file.getPath());
        } catch (ObjectStoreException e) {
            LOG.error("Schema invalid");
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                LOG.warn("Failed to close file " + file.getPath());
            }
        }
    }

    /**
     * Generate a string that contains the torque data for the given schema.
     *
     * @return a String containing torque data
     * @throws ObjectStoreException if the schema is invalid
     */
    protected String generate() throws ObjectStoreException {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>" + ENDL)
            // ignore DTD - could get copy from classpath and write to tempfile adds a
            // lot of hassle
            //.append("<!DOCTYPE database SYSTEM \""
            //        + "http://jakarta.apache.org/turbine/dtd/database.dtd\">" + ENDL)
            .append("<database name=\"\">" + ENDL);

        Iterator iter = schema.getModel().getClassDescriptors().iterator();
        while (iter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            ClassDescriptor tableMaster = schema.getTableMaster(cld);
            if (cld == tableMaster) {
                sb.append(generate(cld));
            }
            Iterator collectionIter = cld.getCollectionDescriptors().iterator();
            while (collectionIter.hasNext()) {
                CollectionDescriptor collection = (CollectionDescriptor) collectionIter.next();
                if (FieldDescriptor.M_N_RELATION == collection.relationType()) {
                    if (!indirections.contains(collection.getReverseReferenceDescriptor())) {
                        indirections.add(collection);
                    }
                }
            }
        }

        Iterator indirectionIter = indirections.iterator();
        while (indirectionIter.hasNext()) {
            sb.append(generateIndirectionTable((CollectionDescriptor) indirectionIter.next()));
        }

        // create a metadata table, ensuring keys are unique
        sb.append(INDENT + "<table name=\"" + MetadataManager.METADATA_TABLE + "\">" + ENDL)
            .append(generateColumn("key", "java.lang.String"))
            .append(generateColumn("value", "java.lang.String"))
            .append(INDENT + "<unique name=\"" + MetadataManager.METADATA_TABLE + "_key\">" + ENDL)
            .append(INDENT + INDENT + "<unique-column name=\"key\"/>" + ENDL)
            .append(INDENT + "</unique>" + ENDL)
            .append(INDENT + "</table>" + ENDL);
        // Create the integer bag table
        sb.append(INDENT + "<table name=\"" + ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME + "\">"
                + ENDL)
            .append(generateColumn(ObjectStoreInterMineImpl.BAGID_COLUMN, "java.lang.Integer"))
            .append(generateColumn(ObjectStoreInterMineImpl.BAGVAL_COLUMN, "java.lang.Integer"))
            .append(INDENT + INDENT + "<unique name=\""
                    + ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME + "_index1\">" + ENDL)
            .append(INDENT + INDENT + INDENT + "<unique-column name=\""
                    + ObjectStoreInterMineImpl.BAGID_COLUMN + "\"/>" + ENDL)
            .append(INDENT + INDENT + INDENT + "<unique-column name=\""
                    + ObjectStoreInterMineImpl.BAGVAL_COLUMN + "\"/>" + ENDL)
            .append(INDENT + INDENT + "</unique>" + ENDL)
            .append(INDENT + INDENT + "<index name=\"" + ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME
                    + "_index2\">" + ENDL)
            .append(INDENT + INDENT + INDENT + "<index-column name=\""
                    + ObjectStoreInterMineImpl.BAGVAL_COLUMN + "\"/>" + ENDL)
            .append(INDENT + INDENT + INDENT + "<index-column name=\""
                    + ObjectStoreInterMineImpl.BAGID_COLUMN + "\"/>" + ENDL)
            .append(INDENT + INDENT + "</index>" + ENDL)
            .append(INDENT + "</table>" + ENDL);

        sb.append("</database>" + ENDL);
        return sb.toString();
    }

    /**
     * Generate a string that describes the given ClassDescriptor.
     *
     * @param cld the ClassDescriptor
     * @return a String
     * @throws ObjectStoreException if the schema is invalid
     */
    protected String generate(ClassDescriptor cld) throws ObjectStoreException {
        StringBuffer sb = new StringBuffer();
        columns = new HashSet();
        className = DatabaseUtil.getTableName(cld);
        if (!schema.getMissingTables().contains(className.toLowerCase())) {
            // Every class and interface has a separate table
            sb.append(INDENT + "<table name=\"" + className + "\">" + ENDL);
            if ((!(schema.isMissingNotXml() || schema.isFlatMode(cld.getType())))
                    || InterMineObject.class.equals(cld.getType())) {
                sb.append(generateColumn("OBJECT", "java.lang.String"));
            }
            DatabaseSchema.Fields fields = schema.getTableFields(cld);
            Iterator fieldIter = fields.getAttributes().iterator();
            while (fieldIter.hasNext()) {
                AttributeDescriptor field = (AttributeDescriptor) fieldIter.next();
                sb.append(generateColumn(DatabaseUtil.getColumnName(field), field.getType()));
            }
            fieldIter = fields.getReferences().iterator();
            while (fieldIter.hasNext()) {
                ReferenceDescriptor field = (ReferenceDescriptor) fieldIter.next();
                sb.append(generateColumn(DatabaseUtil.getColumnName(field), "java.lang.Integer"));
            }
            if (cld.getFieldDescriptorByName("id") != null) {
                if (schema.isTruncated(cld)) {
                    if (schema.isFlatMode(cld.getType())) {
                        sb.append(generateColumn("objectClass", "java.lang.String"));
                    }
                    sb.append(generateColumn("class", "java.lang.String"));
                    sb.append(INDENT + INDENT + "<unique name=\"" + className + "_pkey\">" + ENDL
                            + INDENT + INDENT + INDENT + "<unique-column name=\"id\"/>" + ENDL
                            + INDENT + INDENT + INDENT + "<unique-column name=\"class\"/>" + ENDL
                            + INDENT + INDENT + "</unique>" + ENDL);
                } else {
                    sb.append(INDENT + INDENT + "<unique name=\"" + className + "_pkey\">" + ENDL
                            + INDENT + INDENT + INDENT + "<unique-column name=\"id\"/>" + ENDL
                            + INDENT + INDENT + "</unique>" + ENDL);
                }
            }
            sb.append(INDENT + "</table>" + ENDL);
            return sb.toString();
        } else {
            return "";
        }
    }

    private String generateColumn(String name, String type) {
        StringBuffer sb = new StringBuffer();
        sb.append(INDENT + INDENT + "<column name=\"")
            .append(name)
            .append("\" type=\"")
            .append(generateJdbcType(type))
            .append("\"");
        if ((type.indexOf(".") == -1) || ("id".equals(name))) {
            sb.append(" required=\"true\"");
        }
        sb.append("/>" + ENDL);
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
            .append(INDENT + "</table>" + ENDL);
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
            return "BIGINT";
        } else if (type.equals("java.math.BigDecimal")) {
            return "NUMERIC";
        }
        return type;
    }
}
