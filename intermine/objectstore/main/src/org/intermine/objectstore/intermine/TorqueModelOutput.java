package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.BAGID_COLUMN;
import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.BAGVAL_COLUMN;
import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.CLOBID_COLUMN;
import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.CLOBPAGE_COLUMN;
import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.CLOBVAL_COLUMN;
import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.CLOB_TABLE_NAME;
import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.DatabaseUtil;

/**
 * Map InterMine metadata to a Torque database schema in InterMine format
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class TorqueModelOutput
{
    private static final Logger LOG = Logger.getLogger(TorqueModelOutput.class);

    /** The version number of the database format */
    public static final int FORMAT_VERSION = 1;
    protected static final String INDENT = "    ";
    protected static final String ENDL = System.getProperty("line.separator");

    protected DatabaseSchema schema;
    protected File file;
    protected Set<CollectionDescriptor> indirections = new HashSet<CollectionDescriptor>();
    protected String className = "";

    private static final String LONG_VAR_BINARY_TYPE = "LONGVARBINARY";

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
            throw new RuntimeException("Failed to output torque data to file " + file.getPath());
        } catch (ObjectStoreException e) {
            LOG.error("Schema invalid while writing to file " + file.getPath(), e);
            throw new RuntimeException("Schema invalid while writing to file " + file.getPath(),
                    e);
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

        for (ClassDescriptor cld : schema.getModel().getClassDescriptors()) {
            ClassDescriptor tableMaster = schema.getTableMaster(cld);
            if (cld == tableMaster) {
                sb.append(generate(cld));
            }
            for (CollectionDescriptor collection : cld.getCollectionDescriptors()) {
                if (FieldDescriptor.M_N_RELATION == collection.relationType()) {
                    if (!indirections.contains(collection.getReverseReferenceDescriptor())) {
                        indirections.add(collection);
                    }
                }
            }
        }

        for (CollectionDescriptor collection : indirections) {
            sb.append(generateIndirectionTable(collection));
        }

        // create a metadata table, ensuring keys are unique
        sb.append(INDENT + "<table name=\"" + MetadataManager.METADATA_TABLE + "\">" + ENDL)
            .append(generateColumn("key", "java.lang.String"))
            .append(generateColumn("value", "java.lang.String"))
            .append(generateColumn("blob_value", "LONGVARBINARY"))
            .append(INDENT + "<unique name=\"" + MetadataManager.METADATA_TABLE + "_key\">" + ENDL)
            .append(INDENT + INDENT + "<unique-column name=\"key\"/>" + ENDL)
            .append(INDENT + "</unique>" + ENDL)
            .append(INDENT + "</table>" + ENDL);
        // Create the integer bag table
        sb.append(INDENT + "<table name=\"" + INT_BAG_TABLE_NAME + "\">" + ENDL)
            .append(generateColumn(BAGID_COLUMN, "java.lang.Integer"))
            .append(generateColumn(BAGVAL_COLUMN, "java.lang.Integer"))
            .append(INDENT + INDENT + "<unique name=\"" + INT_BAG_TABLE_NAME + "_index1\">" + ENDL)
            .append(INDENT + INDENT + INDENT + "<unique-column name=\"" + BAGID_COLUMN + "\"/>"
                    + ENDL)
            .append(INDENT + INDENT + INDENT + "<unique-column name=\"" + BAGVAL_COLUMN + "\"/>"
                    + ENDL)
            .append(INDENT + INDENT + "</unique>" + ENDL)
            .append(INDENT + INDENT + "<index name=\"" + INT_BAG_TABLE_NAME + "_index2\">" + ENDL)
            .append(INDENT + INDENT + INDENT + "<index-column name=\"" + BAGVAL_COLUMN + "\"/>"
                    + ENDL)
            .append(INDENT + INDENT + INDENT + "<index-column name=\"" + BAGID_COLUMN + "\"/>"
                    + ENDL)
            .append(INDENT + INDENT + "</index>" + ENDL)
            .append(INDENT + "</table>" + ENDL);
        // Create the Clob table
        sb.append(INDENT + "<table name=\"" + CLOB_TABLE_NAME + "\">" + ENDL)
            .append(generateColumn(CLOBID_COLUMN, "java.lang.Integer"))
            .append(generateColumn(CLOBPAGE_COLUMN, "java.lang.Integer"))
            .append(generateColumn(CLOBVAL_COLUMN, "java.lang.String"))
            .append(INDENT + INDENT + "<unique name=\"" + CLOB_TABLE_NAME + "_index\">" + ENDL)
            .append(INDENT + INDENT + INDENT + "<unique-column name=\"" + CLOBID_COLUMN + "\"/>"
                    + ENDL)
            .append(INDENT + INDENT + INDENT + "<unique-column name=\"" + CLOBPAGE_COLUMN + "\"/>"
                    + ENDL)
            .append(INDENT + INDENT + "</unique>" + ENDL)
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
        className = DatabaseUtil.getTableName(cld);
        if (!schema.getMissingTables().contains(className.toLowerCase())) {
            // Every class and interface has a separate table
            sb.append(INDENT + "<table name=\"" + className + "\">" + ENDL);
            if ((!(schema.isMissingNotXml() || schema.isFlatMode(cld.getType())))
                    || InterMineObject.class.equals(cld.getType())) {
                sb.append(generateColumn("OBJECT", "java.lang.String"));
            }
            DatabaseSchema.Fields fields = schema.getTableFields(cld);
            for (AttributeDescriptor field : fields.getAttributes()) {
                sb.append(generateColumn(DatabaseUtil.getColumnName(field), field.getType()));
            }
            for (ReferenceDescriptor field : fields.getReferences()) {
                sb.append(generateColumn(DatabaseUtil.getColumnName(field), "java.lang.Integer"));
            }
            if (cld.getFieldDescriptorByName("id") != null) {
                if (schema.isTruncated(cld)) {
                    sb.append(generateColumn("class", "java.lang.String"));
                    sb.append(generateColumn("tableclass", "java.lang.String"));
                    sb.append(INDENT + INDENT + "<unique name=\"" + className + "_pkey\">" + ENDL
                            + INDENT + INDENT + INDENT + "<unique-column name=\"id\"/>" + ENDL
                            + INDENT + INDENT + INDENT + "<unique-column name=\"tableclass\"/>"
                            + ENDL + INDENT + INDENT + "</unique>" + ENDL);
                } else {
                    if (!schema.isFlatMode(cld.getType())) {
                        sb.append(generateColumn("class", "java.lang.String"));
                    }
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
        if ((type.indexOf(".") == -1 && !type.equals(LONG_VAR_BINARY_TYPE))
            || ("id".equals(name))) {
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
        String column1 = DatabaseUtil.getInwardIndirectionColumnName(col, schema.getVersion());
        String column2 = DatabaseUtil.getOutwardIndirectionColumnName(col, schema.getVersion());
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
        if ("short".equals(type) || "java.lang.Short".equals(type)) {
            return "SMALLINT";
        } else if ("int".equals(type) || "java.lang.Integer".equals(type)) {
            return "INTEGER";
        } else if ("long".equals(type) || "java.lang.Long".equals(type)) {
            return "BIGINT";
        } else if ("java.lang.String".equals(type)) {
            return "LONGVARCHAR";
        } else if ("boolean".equals(type) || "java.lang.Boolean".equals(type)) {
            return "BIT";
        } else if ("float".equals(type) || "java.lang.Float".equals(type)) {
            return "REAL";
        } else if ("double".equals(type) || "java.lang.Double".equals(type)) {
            return "DOUBLE";
        } else if ("java.util.Date".equals(type)) {
            return "BIGINT";
        } else if ("java.math.BigDecimal".equals(type)) {
            return "NUMERIC";
        } else if ("org.intermine.objectstore.query.ClobAccess".equals(type)) {
            return "LONGVARCHAR";
        } else {
            if (LONG_VAR_BINARY_TYPE.equals(type)) {
                return LONG_VAR_BINARY_TYPE;
            }
        }
        throw new IllegalArgumentException("Invalid type \"" + type + "\"");
    }
}
