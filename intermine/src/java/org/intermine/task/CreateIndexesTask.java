package org.flymine.task;

/*
 * Copyright (C) 2002-2003 FlyMine
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
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.flymine.sql.DatabaseFactory;
import org.flymine.dataloader.DataLoaderHelper;
import org.flymine.dataloader.PrimaryKey;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.CollectionDescriptor;
import org.flymine.util.DatabaseUtil;
import org.flymine.util.StringUtil;


/**
 * Task to create indexes on a database holding objects conforming to a given model by
 * reading that model's primary key configuration information.
 * Three types of index are created: for the specified primary key fields, for all N-1 relations,
 * and for the indirection table columns of M-N relations.
 * This should speed up primary key queries, and other common queries
 * Note that all "id" columns are indexed automatically by virtue of FlyMineTorqueModelOuput
 * specifying them as primary key columns.
 * @author Mark Woodbridge
 */
public class CreateIndexesTask extends Task
{
    protected String database, model;
    protected Connection c;

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
                        fieldNames.add(DatabaseUtil.getColumnName(fd));
                    }
                    String tableName = DatabaseUtil.getTableName(cld);
                    dropIndex(tableName + "__" + keyName);
                    createIndex(tableName + "__" + keyName, tableName,
                                StringUtil.join(fieldNames, ", "));
                }
                //and one for each N-to-1 relation to increase speed of e.g. company.getDepartments
                for (Iterator j = cld.getAllReferenceDescriptors().iterator(); j.hasNext();) {
                    ReferenceDescriptor ref = (ReferenceDescriptor) j.next();
                    if (FieldDescriptor.N_ONE_RELATION == ref.relationType()) {
                        String tableName = DatabaseUtil.getTableName(cld);                      
                        dropIndex(tableName + "__"  + ref.getName());
                        createIndex(tableName + "__"  + ref.getName(), tableName,
                                    DatabaseUtil.getColumnName(ref));
                    }
                }
                //finally add an index to all M-to-N indirection table columns
                for (Iterator j = cld.getAllCollectionDescriptors().iterator(); j.hasNext();) {
                    CollectionDescriptor col = (CollectionDescriptor) j.next();
                    if (FieldDescriptor.M_N_RELATION == col.relationType()) {
                        String tableName = DatabaseUtil.getIndirectionTableName(col);
                        String columnName = DatabaseUtil.getInwardIndirectionColumnName(col);
                        dropIndex(tableName + "__"  + columnName);
                        createIndex(tableName + "__"  + columnName, tableName, columnName);
                    }
                }
            }
        } catch (Exception e) {
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
     * Drop an index by name, ignoring errors
     * @param indexName the index name
     */
    protected void dropIndex(String indexName) {
        try {
            String sql = "drop index " + indexName;
            System .out.println(sql);
            c.createStatement().execute(sql);
        } catch (SQLException e) {
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
        String sql = "create index " + indexName + " on " + tableName + "(" + columnNames + ")";
        System .out.println(sql);
        c.createStatement().execute(sql);
    }
}
