package org.intermine.task;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.sql.DatabaseFactory;
import org.intermine.dataloader.DataLoaderHelper;
import org.intermine.dataloader.PrimaryKey;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.util.DatabaseUtil;
import org.intermine.util.StringUtil;

/**
 * Task to create indexes on a database holding objects conforming to a given model by
 * reading that model's primary key configuration information.
 * Three types of index are created: for the specified primary key fields, for all N-1 relations,
 * and for the indirection table columns of M-N relations.
 * This should speed up primary key queries, and other common queries
 * Note that all "id" columns are indexed automatically by virtue of InterMineTorqueModelOuput
 * specifying them as primary key columns.
 * @author Mark Woodbridge
 */
public class CreateIndexesTask extends Task
{
    protected String database, model;
    protected Connection c;
    protected boolean attributeIndexes = false;

    /**
     * Set the database alias
     * @param database the database alias
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Set the model name
     * @param model the model name
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Set the attributeIndexes flag.  Index the attributes that are not part of the
     * primary key if and only if the flag is set.
     * @param attributeIndexes flag for attribute indexes
     */
    public void setAttributeIndexes(boolean attributeIndexes) {
        this.attributeIndexes = attributeIndexes;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }
        if (model == null) {
            throw new BuildException("model attribute is not set");
        }
        
        try {
            c = DatabaseFactory.getDatabase(database).getConnection();
            c.setAutoCommit(true);
            Model m = Model.getInstanceByName(model);
            for (Iterator i = m.getClassDescriptors().iterator(); i.hasNext();) {
                ClassDescriptor cld = (ClassDescriptor) i.next();
                if (attributeIndexes) {
                    createAttributeIndexes(cld);
                } else {
                    createStandardIndexes(cld);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new BuildException(e);
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Add indexes for primary keys, indirection tables and 1-N relations to the relevant tables for
     * a given ClassDescriptor
     * @param cld the ClassDescriptor
     * @throws SQLException if an error occurs
     * @throws MetaDataException if a field os not found in model
     */
    protected void createStandardIndexes(ClassDescriptor cld)
        throws SQLException, MetaDataException {
        // Set of fieldnames that already are the first element of an index.
        Set doneFieldNames = new HashSet();

        //add an index for each primary key
        Map primaryKeys = DataLoaderHelper.getPrimaryKeys(cld);
        for (Iterator j = primaryKeys.entrySet().iterator(); j.hasNext();) {
            Map.Entry entry = (Map.Entry) j.next();
            String keyName = (String) entry.getKey();
            PrimaryKey key = (PrimaryKey) entry.getValue();
            List fieldNames = new ArrayList();
            for (Iterator k = key.getFieldNames().iterator(); k.hasNext();) {
                String fieldName = (String) k.next();
                FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
                if (fd == null) {
                    throw new MetaDataException("field (" + fieldName + ") not found for class: "
                                                + cld.getName() + ".");
                }
                fieldNames.add(DatabaseUtil.getColumnName(fd));
            }
            String tableName = DatabaseUtil.getTableName(cld);
            dropIndex(tableName + "__" + keyName);
            createIndex(tableName + "__" + keyName, tableName,
                        StringUtil.join(fieldNames, ", ") + ", id");
            doneFieldNames.add(fieldNames.get(0));
        }

        //and one for each bidirectional N-to-1 relation to increase speed of
        //e.g. company.getDepartments
        for (Iterator j = cld.getAllReferenceDescriptors().iterator(); j.hasNext();) {
            ReferenceDescriptor ref = (ReferenceDescriptor) j.next();
            if ((FieldDescriptor.N_ONE_RELATION == ref.relationType())
                    && (ref.getReverseReferenceDescriptor() != null)) {
                String tableName = DatabaseUtil.getTableName(cld);
                String fieldName = DatabaseUtil.getColumnName(ref);
                if (!doneFieldNames.contains(fieldName)) {
                    dropIndex(tableName + "__"  + ref.getName());
                    createIndex(tableName + "__"  + ref.getName(), tableName,
                                fieldName + ", id");
                }
            }
        }

        //finally add an index to all M-to-N indirection table columns
        //for (Iterator j = cld.getAllCollectionDescriptors().iterator(); j.hasNext();) {
        for (Iterator j = cld.getCollectionDescriptors().iterator(); j.hasNext();) {
            CollectionDescriptor col = (CollectionDescriptor) j.next();
            if (FieldDescriptor.M_N_RELATION == col.relationType()) {
                String tableName = DatabaseUtil.getIndirectionTableName(col);
                String columnName = DatabaseUtil.getInwardIndirectionColumnName(col);
                String columnName2 = DatabaseUtil.getOutwardIndirectionColumnName(col);
                if ((columnName.compareTo(columnName2) < 0)
                        || (col.getReverseReferenceDescriptor() == null)) {
                    dropIndex(tableName + "__"  + columnName);
                    dropIndex(tableName + "__"  + columnName2);
                    createIndex(tableName + "__"  + columnName, tableName,
                            columnName + ", " + columnName2);
                    createIndex(tableName + "__"  + columnName2, tableName,
                            columnName2 + ", " + columnName);
                }
            }
        }
    }

    /**
     * Add indexes for all fields to the relevant tables for a given ClassDescriptor.  Skip those
     * fields that have indexes created by createStandardIndexes().
     * @param cld the ClassDescriptor
     * @throws SQLException if an error occurs
     * @throws MetaDataException if a field os not found in model
     */
    protected void createAttributeIndexes(ClassDescriptor cld)
        throws SQLException, MetaDataException {

        Map primaryKeys = DataLoaderHelper.getPrimaryKeys(cld);
        String tableName = DatabaseUtil.getTableName(cld);
        
      ATTRIBUTE: for (Iterator attributeIter = cld.getAllAttributeDescriptors().iterator();
                      attributeIter.hasNext();) {
            AttributeDescriptor att = (AttributeDescriptor) attributeIter.next();
            
            if (att.getName().equals("id")) {
                continue;
            }
            
            String fieldName = DatabaseUtil.getColumnName(att);

            for (Iterator primaryKeyIter = primaryKeys.entrySet().iterator();
                 primaryKeyIter.hasNext();) {
                Map.Entry primaryKeyEntry = (Map.Entry) primaryKeyIter.next();
                String keyName = (String) primaryKeyEntry.getKey();
                PrimaryKey key = (PrimaryKey) primaryKeyEntry.getValue();
                
                String firstKeyField = (String) ((Set) key.getFieldNames()).iterator().next();

                if (firstKeyField.equals(att.getName())) {
                    continue ATTRIBUTE;
                }
            }

            try {
                dropIndex(tableName + "__"  + att.getName());
            } catch (SQLException e) {
                // ignore - mostly the exception will happen if there is no table
                // if there is a serious problem the createIndex() will throw an exception.
            }
            createIndex(tableName + "__"  + att.getName(), tableName, fieldName + ", id");
        }
    }

    /**
     * Drop an index by name
     * @param indexName the index name
     * @throws SQLException if there is a problem while dropping the table
    */
    protected void dropIndex(String indexName) throws SQLException {
        execute("drop index " + indexName);
    }

    /**
     * Create an named index on the specified columns of a table
     * @param indexName the index name
     * @param tableName the table name
     * @param columnNames the column names
     * @throws SQLException if there is a problem while creating the table
     */
    protected void createIndex(String indexName, String tableName, String columnNames)
        throws SQLException {
        execute("create index " + indexName + " on " + tableName + "(" + columnNames + ")");
    }

    /**
     * Execute an sql statement
     * @param sql the sql string for the statement to execute
     * @throws SQLException if an error occurs
     */
    protected void execute(String sql) throws SQLException {
        c.createStatement().execute(sql);
    }
}
