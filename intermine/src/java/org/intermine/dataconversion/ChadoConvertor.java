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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.sql.Database;
import org.flymine.util.TypeUtil;
import org.flymine.util.DatabaseUtil;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Field;
import org.flymine.xml.full.ReferenceList;

import org.apache.log4j.Logger;

/**
 * Class to read a Chado database and produce a data representation
 * 
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ChadoConvertor 
{
    protected static final Logger LOG = Logger.getLogger(ChadoConvertor.class);
    protected Connection c = null;

    /**
     * Produce a list of Items representing all the instances of all the classes in the model
     * present in a database
     * @param model the Model
     * @param db the Database
     * @return a collection of all the items
     * @throws SQLException if an error occurs when accessing the Database
     */
    public Collection process(Model model, Database db) throws SQLException {
        List items = new ArrayList();
        try {
            c = db.getConnection();
            for (Iterator cldIter = model.getClassDescriptors().iterator(); cldIter.hasNext();) {
                items.addAll(processClassDescriptor((ClassDescriptor) cldIter.next()));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return items;
    }

    /** 
     * Process a ClassDescriptor to generate a Collection of Items representing all the instances
     * of this class in the target database
     * @param cld the ClassDescriptor
     * @return a Collection of Items
     * @throws SQLException if an error occurs in accessing the database
     */
    protected Collection processClassDescriptor(ClassDescriptor cld) throws SQLException {
        List items = new ArrayList();
        String clsName = TypeUtil.unqualifiedName(cld.getName());
        ResultSet r = executeQuery(c, "SELECT * FROM " + clsName);
        while (r.next()) {
            String clsId = r.getObject(clsName + "_id").toString();
            Item item = new Item();
            item.setClassName(cld.getModel().getNameSpace() + clsName);
            item.setIdentifier(clsId);
            LOG.info("Processing item: " + clsName + " " + clsId);
            for (Iterator fdIter = cld.getFieldDescriptors().iterator(); fdIter.hasNext();) {
                FieldDescriptor fd = (FieldDescriptor) fdIter.next();
                String fieldName = fd.getName();
                if (fd.isAttribute()) {
                    Field f = new Field();
                    f.setName(fieldName);
                    Object value = r.getObject(fieldName);
                    if (value != null) {
                        f.setValue(TypeUtil.objectToString(value));
                        item.addField(f);
                    }
                } else if (fd.isReference()) {
                    Field f = new Field();
                    f.setName(fieldName);
                    Object value = r.getObject(fieldName + "_id");
                    if (value != null) {
                        f.setValue(TypeUtil.objectToString(value));
                        item.addReference(f);
                    }
                } else if (fd.isCollection()) {
                    String sql;
                    if (fd.relationType() == FieldDescriptor.ONE_N_RELATION) {
                        ReferenceDescriptor rd =
                            ((ReferenceDescriptor) fd).getReverseReferenceDescriptor();
                        String refClsName =  TypeUtil.unqualifiedName(
                                                                rd.getClassDescriptor().getName());
                        sql = "SELECT " + refClsName + "_id FROM " + refClsName + " WHERE "
                            + rd.getName() + "_id = " + clsId;
                    } else {
                        String otherClsName = TypeUtil.unqualifiedName(
                             ((ReferenceDescriptor) fd).getReferencedClassDescriptor().getName());
                        String tableName = findIndirectionTable(clsName, otherClsName);
                        sql = "SELECT " + otherClsName + "_id FROM " + tableName + " WHERE "
                            + clsName + "_id = " + clsId;
                    }
                    ResultSet idSet = executeQuery(c, sql);
                    ReferenceList refs = new ReferenceList();
                    refs.setName(fieldName);
                    while (idSet.next()) {
                        refs.addValue(idSet.getObject(1).toString());
                    }
                    if (refs.getReferences().size() > 0) {
                        item.addCollection(refs);
                    }
                }
            }
            items.add(item);
        }
        return items;
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

//     public static void main(String[] args) throws Exception {
//         Database db = org.flymine.sql.DatabaseFactory.getDatabase("db.chado");
//         Model model = Model.getInstanceByName("chado");
//         Collection c = ChadoConvertor.process(model, db);
//     }
}
