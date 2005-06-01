package org.intermine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import org.intermine.dataloader.DataLoaderHelper;
import org.intermine.dataloader.PrimaryKey;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.util.DatabaseUtil;
import org.intermine.util.StringUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.DatabaseSchema;
import org.intermine.sql.Database;

import org.apache.log4j.Logger;

/**
 * Task to create indexes on a database holding objects conforming to a given model by
 * reading that model's primary key configuration information.
 * By default three types of index are created: for the specified primary key fields, for all N-1
 * relations, and for the indirection table columns of M-N relations.
 * Alternatively, if attributeIndexes is true, indexes are created for all non-primary key
 * attributes instead.
 * Note that all "id" columns are indexed automatically by virtue of InterMineTorqueModelOuput
 * specifying them as primary key columns.
 * @author Mark Woodbridge
 */
public class CreateIndexesTask extends Task
{
    protected String alias;
    protected Connection c;
    protected boolean attributeIndexes = false;
    protected DatabaseSchema schema = null;
    protected Database database = null;
    private static final Logger LOG = Logger.getLogger(CreateIndexesTask.class);
    protected Map tableIndexesDone = new HashMap();
    protected Set indexesMade = new HashSet();

    /**
     * Set the ObjectStore alias.  Currently the ObjectStore must be an ObjectStoreInterMineImpl.
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
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
     * Sets up the instance variables
     *
     * @throws BuildException if something is wrong
     */
    public void setUp() throws BuildException {
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        ObjectStore objectStore;

        try {
            objectStore = ObjectStoreFactory.getObjectStore(alias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }

        if (objectStore instanceof ObjectStoreInterMineImpl) {
            ObjectStoreInterMineImpl osii = ((ObjectStoreInterMineImpl) objectStore);

            database = osii.getDatabase();
            schema = osii.getSchema();
        } else {
            // change comment on setAlias() when this changes
            throw new BuildException("the alias (" + alias + ") does not refer to an "
                                     + "ObjectStoreInterMineImpl");
        }
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        setUp();
        Model m = schema.getModel();

        ClassDescriptor tmpCldA = null;
        ClassDescriptor tmpCldB = null;
        try {
            c = database.getConnection();
            c.setAutoCommit(true);
            for (Iterator i = m.getClassDescriptors().iterator(); i.hasNext();) {
                ClassDescriptor cld = (ClassDescriptor) i.next();
                tmpCldA = cld;
                tmpCldB = schema.getTableMaster(tmpCldA);

                //if (cld == schema.getTableMaster(cld)) {
                    if (attributeIndexes) {
                        createAttributeIndexes(cld);
                    } else {
                        createStandardIndexes(cld);
                    }
                //}
            }
        } catch (Exception e) {
            System.out .println("Error creating indexes for " + tmpCldA.getType()
                    + ", table master = " + tmpCldB.getType());
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
        String cldTableName = DatabaseUtil.getTableName(cld);

        //add an index for each primary key
        Map primaryKeys = PrimaryKeyUtil.getPrimaryKeys(cld);
        for (Iterator i = primaryKeys.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String keyName = (String) entry.getKey();
            PrimaryKey key = (PrimaryKey) entry.getValue();
            List fieldNames = new ArrayList();
            boolean firstOne = true;
            boolean doNulls = false;
            for (Iterator j = key.getFieldNames().iterator(); j.hasNext();) {
                String fieldName = (String) j.next();
                FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
                if (firstOne && (fd instanceof AttributeDescriptor)) {
                    doNulls = !((AttributeDescriptor) fd).isPrimitive();
                }
                if (fd == null) {
                    throw new MetaDataException("field (" + fieldName + ") not found for class: "
                                                + cld.getName() + ".");
                }
                fieldNames.add(DatabaseUtil.getColumnName(fd));
                firstOne = false;
            }

            // create indexes on this class and on all subclasses
            Set clds = new HashSet(Collections.singleton(cld));
            clds.addAll(cld.getModel().getAllSubs(cld));
            for (Iterator k = clds.iterator(); k.hasNext();) {
                ClassDescriptor nextCld = (ClassDescriptor) k.next();
                ClassDescriptor tableMaster = schema.getTableMaster(nextCld);
                String tableName = DatabaseUtil.getTableName(tableMaster);
                if (!schema.getMissingTables().contains(tableName.toLowerCase())) {
                    dropIndex(tableName + "__" + cldTableName + "__" + keyName);
                    createIndex(tableName + "__" + cldTableName + "__" + keyName, tableName,
                                StringUtil.join(fieldNames, ", ") + ", id");
                    if (doNulls) {
                        dropIndex(tableName + "__" + cldTableName + "__" + keyName + "__nulls");
                        createIndex(tableName + "__" + cldTableName + "__" + keyName + "__nulls",
                                tableName, "(" + fieldNames.get(0) + " IS NULL)");
                    }
                    doneFieldNames.add(fieldNames.get(0));
                }
            }
        }

        //and one for each bidirectional N-to-1 relation to increase speed of
        //e.g. company.getDepartments
        for (Iterator i = cld.getAllReferenceDescriptors().iterator(); i.hasNext();) {
            ReferenceDescriptor ref = (ReferenceDescriptor) i.next();
            ClassDescriptor refMaster = ref.getClassDescriptor();
            ClassDescriptor tableMaster = schema.getTableMaster(cld);
            String tableName = DatabaseUtil.getTableName(tableMaster);
            if ((FieldDescriptor.N_ONE_RELATION == ref.relationType())
                    && (ref.getReverseReferenceDescriptor() != null)) {
                if (!schema.getMissingTables().contains(tableName.toLowerCase())) {
                    String fieldName = DatabaseUtil.getColumnName(ref);
                    if (!doneFieldNames.contains(fieldName)) {
                        dropIndex(cldTableName + "__"  + ref.getName());
                        createIndex(cldTableName + "__"  + ref.getName(), tableName,
                                    fieldName + ", id");
                    }
                }
            }
        }

        //finally add an index to all M-to-N indirection table columns
        //for (Iterator i = cld.getAllCollectionDescriptors().iterator(); i.hasNext();) {
        for (Iterator i = cld.getCollectionDescriptors().iterator(); i.hasNext();) {
            CollectionDescriptor col = (CollectionDescriptor) i.next();
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

        Map primaryKeys = PrimaryKeyUtil.getPrimaryKeys(cld);
        String tableName = DatabaseUtil.getTableName(cld);
        if (!schema.getMissingTables().contains(tableName.toLowerCase())) {

            ATTRIBUTE:
            for (Iterator attributeIter = cld.getAllAttributeDescriptors().iterator();
                    attributeIter.hasNext();) {
                AttributeDescriptor att = (AttributeDescriptor) attributeIter.next();
                
                if (att.getName().equals("id")) {
                    continue;
                }
                
                String fieldName = DatabaseUtil.getColumnName(att);
                
                if (!att.getType().equals("java.lang.String")) {
                    // if the attribute is the first column of a primary key, don't bother creating
                    // another index for it - unless it's a String attribute in which case we want
                    // to create a LOWER() index
                    for (Iterator primaryKeyIter = primaryKeys.entrySet().iterator();
                            primaryKeyIter.hasNext();) {
                        Map.Entry primaryKeyEntry = (Map.Entry) primaryKeyIter.next();
                        String keyName = (String) primaryKeyEntry.getKey();
                        PrimaryKey key = (PrimaryKey) primaryKeyEntry.getValue();
                        
                        String firstKeyField = (String) ((Set) key.getFieldNames()).iterator()
                            .next();
                        
                        if (firstKeyField.equals(att.getName())) {
                            continue ATTRIBUTE;
                        }
                    }
                }
                
                String indexName = tableName + "__"  + att.getName();
                LOG.info("creating index: " + indexName);
                dropIndex(indexName);
                if (att.getType().equals("java.lang.String")) {
                    try {
                        createIndex(indexName, tableName, "lower(" + fieldName + ")");
                    } catch (SQLException e) {
                        if (e.getMessage().matches("ERROR: index row requires \\d+ bytes, "
                                    + "maximum size is 8191")) {
                            // ignore - we just don't create this index
                            LOG.error("failed to create index for "
                                    + tableName + "(" + fieldName + ")");
                        } else {
                            throw e;
                        }
                    }
                } else {
                    createIndex(indexName, tableName, fieldName);
                }
                if (!att.isPrimitive()) {
                    dropIndex(indexName + "__nulls");
                    createIndex(indexName + "__nulls", tableName, "(" + fieldName + " IS NULL)");
                }
            }
        }
    }

    /**
     * Drop an index by name, ignoring any resulting errors
     * @param indexName the index name
    */
    protected void dropIndex(String indexName) {
        try {
            if (!indexesMade.contains(indexName)) {
                execute("drop index " + indexName);
            }
        } catch (SQLException e) {
            // ignore because the exception is probably because the index doesn't exist
        }
    }

    /**
     * Create an named index on the specified columns of a table
     * @param indexName the index name
     * @param tableName the table name
     * @param columnNames the column names
     * @throws SQLException if an error occurs
     */
    protected void createIndex(String indexName, String tableName, String columnNames)
        throws SQLException {
        Set indexesForTable = (Set) tableIndexesDone.get(tableName);
        if (indexesForTable == null) {
            indexesForTable = new HashSet();
            tableIndexesDone.put(tableName, indexesForTable);
        }
        String indexText = "create index " + indexName + " on " + tableName + "(" + columnNames
            + ")";
        if (!indexesForTable.contains(columnNames)) {
            execute(indexText);
        }
        indexesForTable.add(columnNames);
        indexesMade.add(indexName);
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
