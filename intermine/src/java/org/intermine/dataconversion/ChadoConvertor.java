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

import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;

import org.apache.log4j.Logger;

/**
 * Class to read a Chado database and produce a data representation
 * 
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ChadoConvertor extends DataConvertor
{
    protected static final Logger LOG = Logger.getLogger(ChadoConvertor.class);
    protected static final String ENDL = System.getProperty("line.separator");
    protected Connection c = null;
    protected Writer writer;
    protected Model model;
    protected Database db;

    /**
     * Constructor
     *
     * @param model the Model
     * @param db the Database
     * @param processor the ItemProcessor used to handle the resultant Items
     */
    protected ChadoConvertor(Model model, Database db, ItemProcessor processor) {
        super(processor);
        this.model = model;
        this.db = db;
    }

    /**
     * Process representations of  all the instances of all the classes in the model present in a
     * database
     *
     * @throws Exception if an error occurs in processing
     */
    public void process() throws Exception {
        processor.preProcess();
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
        }
        processor.postProcess();
    }

    /** 
     * Process the items in the database for a given ClassDescriptor
     *
     * @param cld the ClassDescriptor
     * @throws Exception if an error occurs in processing
     */
     protected void processClassDescriptor(ClassDescriptor cld) throws Exception {
        List items = new ArrayList();
        String clsName = TypeUtil.unqualifiedName(cld.getName());
        ResultSet r = executeQuery(c, "SELECT * FROM " + clsName
                                   + " ORDER BY " + clsName + "_id LIMIT 1");
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
                    Attribute attr = new Attribute();
                    attr.setName(fieldName);
                    Object value = r.getObject(fieldName);
                    if (value != null) {
                        attr.setValue(TypeUtil.objectToString(value));
                        item.addAttribute(attr);
                    }
                } else if (fd.isReference()) {
                    Reference ref = new Reference();
                    ref.setName(fieldName);
                    Object value = r.getObject(fieldName + "_id");
                    if (value != null) {
                        ref.setRefId(TypeUtil.objectToString(value));
                        item.addReference(ref);
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
                        refs.addRefId(idSet.getObject(1).toString());
                    }
                    if (refs.getRefIds().size() > 0) {
                        item.addCollection(refs);
                    }
                }
            }
            processor.process(item);
            r.close();
            r = executeQuery(c, "SELECT * FROM " + clsName + " WHERE " + clsName + "_id > " + clsId
                             + " ORDER BY " + clsName + "_id LIMIT 1");
        }
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
}
