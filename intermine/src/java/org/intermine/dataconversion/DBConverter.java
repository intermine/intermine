package org.flymine.dataconversion;

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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.sql.Database;
import org.flymine.util.TypeUtil;
import org.flymine.util.DatabaseUtil;
import org.flymine.util.StringUtil;
import org.flymine.model.fulldata.Attribute;
import org.flymine.model.fulldata.Item;
import org.flymine.model.fulldata.Reference;
import org.flymine.model.fulldata.ReferenceList;

import org.apache.log4j.Logger;

/**
 * Class to read a source database and produce a data representation
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class DBConverter extends DataConverter
{
    protected static final Logger LOG = Logger.getLogger(DBConverter.class);
    protected Connection c = null;
    protected Model model;
    protected Database db;
    protected DBReader reader;

    /**
     * Constructor
     *
     * @param model the Model
     * @param db the Database
     * @param reader the DBReader used to retrieve Items
     * @param writer the ItemWriter used to handle the resultant Items
     */
    protected DBConverter(Model model, Database db, DBReader reader, ItemWriter writer) {
        super(writer);
        this.model = model;
        this.db = db;
        this.reader = reader;
    }

    /**
     * Process representations of  all the instances of all the classes in the model present in a
     * database
     *
     * @throws Exception if an error occurs in processing
     */
    public void process() throws Exception {
        try {
            c = db.getConnection();
            for (Iterator cldIter = model.getClassDescriptors().iterator(); cldIter.hasNext();) {
                ClassDescriptor cld = (ClassDescriptor) cldIter.next();
                if (!cld.getName().equals("org.flymine.model.FlyMineBusinessObject")) {
                    processClassDescriptor(cld);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
            writer.close();
        }
    }

    /**
     * Process a single class from the model
     * @param cld the metadata for the class
     * @throws Exception if an error occurs when reading or writing data
     */
    protected void processClassDescriptor(ClassDescriptor cld) throws Exception {
        String clsName = TypeUtil.unqualifiedName(cld.getName());
        Iterator iter;

        LOG.error("Processing class: " + clsName);

        boolean idsProvided = idsProvided(cld);
        if (idsProvided) {
            iter = reader.sqlIterator("SELECT * FROM " + clsName, clsName + "_id");
        } else {
            iter = reader.execute("SELECT * FROM " + clsName).iterator();
        }

        int identifier = 0;
        while (iter.hasNext()) {
            Map row = (Map) iter.next();
            String clsId = idsProvided ? "" + row.get(clsName + "_id") : "" + (identifier++);
            writer.store(getItem(cld, clsId, row));
        }

        LOG.error("Aliases now: " + aliases);
     }

    /**
     * Check whether this class has a single primary key column named className_id
     * @param cld the metadata for the class
     * @return whether such a column exists
     * @throws SQLException if an error occurs when accessing the database
     */
    protected boolean idsProvided(ClassDescriptor cld) throws SQLException {
        String clsName = TypeUtil.unqualifiedName(cld.getName());
        ResultSet r = executeQuery(c, "SELECT * FROM " + clsName + " LIMIT 1");
        boolean idsProvided = false;
        ResultSetMetaData rsmd = r.getMetaData();
        for (int i = rsmd.getColumnCount(); i > 0; i--) { //cols start at 1
            if (rsmd.getColumnName(i).equals(clsName + "_id")) {
                idsProvided = true;
            }
        }
        return idsProvided;
    }

    /**
     * Use a ResultSet and some metadata for a Class and materialise an Item corresponding
     * to that class
     * @param cld metadata for the class
     * @param clsId the id to use as the basis for the Item's identifier
     * @param row the Map from which to retrieve data
     * @return the Item that has been constructed
     * @throws SQLException if an error occurs when accessing the database
     */
    protected Item getItem(ClassDescriptor cld, String clsId, Map row) throws SQLException {
        String clsName = TypeUtil.unqualifiedName(cld.getName());
        Item item = new Item();
        item.setIdentifier(alias(clsName) + "_" + clsId);
        item.setClassName(cld.getModel().getNameSpace() + clsName);
        item.setImplementations("");
        for (Iterator fdIter = cld.getFieldDescriptors().iterator(); fdIter.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) fdIter.next();
            String fieldName = fd.getName();
            if (fd.isAttribute()) {
                Attribute attr = new Attribute();
                attr.setItem(item);
                attr.setName(fieldName);
                Object value = row.get(fieldName);
                if (value != null) {
                    attr.setValue(StringUtil.duplicateQuotes(TypeUtil.objectToString(value)));
                    item.addAttributes(attr);
                }
            } else if (fd.isReference()) {
                Reference ref = new Reference();
                ref.setItem(item);
                ref.setName(fieldName);
                Object value = row.get(fieldName + "_id");
                if (value != null) {
                    String refClsName = TypeUtil.unqualifiedName(
                        ((ReferenceDescriptor) fd).getReferencedClassDescriptor().getName());
                    ref.setRefId(alias(refClsName) + "_" + TypeUtil.objectToString(value));
                    item.addReferences(ref);
                }
            } else if (fd.isCollection()) {
                String sql, refClsName;
                if (fd.relationType() == FieldDescriptor.ONE_N_RELATION) {
                    ReferenceDescriptor rd =
                        ((ReferenceDescriptor) fd).getReverseReferenceDescriptor();
                    refClsName =  TypeUtil.unqualifiedName(
                                                           rd.getClassDescriptor().getName());
                    sql = "SELECT " + refClsName + "_id FROM " + refClsName + " WHERE "
                        + rd.getName() + "_id = " + clsId;
                } else {
                    refClsName = TypeUtil.unqualifiedName(
                        ((ReferenceDescriptor) fd).getReferencedClassDescriptor().getName());
                    String tableName = findIndirectionTable(clsName, refClsName);
                    sql = "SELECT " + refClsName + "_id FROM " + tableName + " WHERE "
                        + clsName + "_id = " + clsId;
                }
                Iterator idSet = reader.execute(sql).iterator();
                ReferenceList refs = new ReferenceList();
                refs.setItem(item);
                refs.setName(fieldName);
                StringBuffer refIds = new StringBuffer();
                while (idSet.hasNext()) {
                    Map idRow = (Map) idSet.next();
                    refIds.append(alias(refClsName) + "_" + idRow.get(refClsName + "_id") + " ");
                }
                if (refIds.length() > 0) {
                    refs.setRefIds(refIds.toString());
                    item.addCollections(refs);
                }
            }
        }
        return item;
    }

    /**
     * Try to find the indirection table for a many-to-many relationship
     *
     * @param clsName the name of one of the classes
     * @param otherClsName the name of the other class
     * @return the name of the indirection table
     * @throws SQLException if an error occurs
     */
    protected String findIndirectionTable(String clsName, String otherClsName) throws SQLException {
        String tableName = clsName + "_" + otherClsName;
        if (!DatabaseUtil.tableExists(c, tableName)) {
            tableName = otherClsName + "_" + clsName;
        }
        return tableName;
    }

    /**
     * Execute an SQL query on a specified Connection
     *
     * @param c the Connection
     * @param sql the statement to execute
     * @return a ResultSet
     * @throws SQLException if an error occurs
     */
    protected  ResultSet executeQuery(Connection c, String sql) throws SQLException {
        Statement s = c.createStatement();
        return s.executeQuery(sql);
    }
    
    protected Map aliases = new HashMap();
    protected static int index = 0;

    /**
     * Provide a (short) alias for a table name
     * @param className the name of the class
     * @return the alias for the class
     */
    protected String alias(String className) {
        String alias = (String) aliases.get(className);
        if (alias != null) {
            return alias;
        }
        String nextIndex = "" + (index++);
        aliases.put(className, nextIndex);
        return nextIndex;
    }
}
