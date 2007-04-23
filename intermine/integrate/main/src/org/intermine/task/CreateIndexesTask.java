package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.DatabaseSchema;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.SynchronisedIterator;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

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
 * @author Kim Rutherford
 */
public class CreateIndexesTask extends Task
{
    private String alias;
    private Connection c;
    private boolean attributeIndexes = false;
    private DatabaseSchema schema = null;
    private Database database = null;
    private static final Logger LOG = Logger.getLogger(CreateIndexesTask.class);
    private Map tableIndexesDone = Collections.synchronizedMap(new HashMap());
    private Set indexesMade = Collections.synchronizedSet(new HashSet());
    private static final int POSTGRESQL_INDEX_NAME_LIMIT = 63;
    private int extraThreads = 3;
    private ObjectStore objectStore;

    /**
     * Set the ObjectStore alias.  Currently the ObjectStore must be an ObjectStoreInterMineImpl.
     *
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        if (objectStore != null) {
            throw new BuildException("set one of alias and objectStore, not both");
        }
        this.alias = alias;
    }

    /**
     * Set the ObjectStore to use.  Can be set instead of alias.
     *
     * @param objectStore ObjectStore to create indexes on
     */
    public void setObjectStore(ObjectStore objectStore) {
        if (alias != null) {
            throw new BuildException("set one of alias and objectStore, not both");
        }
        this.objectStore = objectStore;
    }

    /**
     * Set the attributeIndexes flag.  Index the attributes that are not part of the
     * primary key if and only if the flag is set.
     *
     * @param attributeIndexes flag for attribute indexes
     */
    public void setAttributeIndexes(boolean attributeIndexes) {
        this.attributeIndexes = attributeIndexes;
    }

    /**
     * Set the number of extra worker threads. If the database server is multi-CPU, it might help to
     * have multiple threads hitting it.
     *
     * @param extraThreads number of extra threads apart from the main thread
     */
    public void setExtraThreads(int extraThreads) {
        this.extraThreads = extraThreads;
    }

    /**
     * Sets up the instance variables
     *
     * @throws BuildException if something is wrong
     */
    public void setUp() throws BuildException {
        if (alias == null && objectStore == null) {
            throw new BuildException("exactly one of alias and objectStore must be set");
        }

        if (objectStore == null) {
            try {
                objectStore = ObjectStoreFactory.getObjectStore(alias);
            } catch (Exception e) {
                throw new BuildException("Exception while creating ObjectStore", e);
            }
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
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        setUp();
        Model m = schema.getModel();
        Map statements = new TreeMap();
        Map clds = new TreeMap();

        ClassDescriptor cld = null;
        try {
            for (Iterator i = m.getClassDescriptors().iterator(); i.hasNext();) {
                cld = (ClassDescriptor) i.next();
                Map cldIndexes = new TreeMap();
                if (attributeIndexes) {
                    getAttributeIndexStatements(cld, cldIndexes);
                } else {
                    getStandardIndexStatements(cld, cldIndexes);
                }
                if (!cldIndexes.isEmpty()) {
                    compressNames(cldIndexes);
                    statements.putAll(cldIndexes);
                    clds.put(cld.getName(), cldIndexes);
                }
            }
        } catch (MetaDataException e) {
            String message = "Error creating indexes for " + cld.getType();
            throw new BuildException(message, e);
        }

        
        checkForIndexNameClashes(statements);

        IndexStatement indexStatement = null;

        try {
            c = database.getConnection();
            c.setAutoCommit(true);

            // Drop all the indexes first, then re-create them.  That ensures that if we try to
            // create an index with the same name twice we get an exception.  Postgresql has a
            // limit on index name length (63) and will truncate longer names with a NOTICE rather
            // than an error.

            Iterator statementsIter = clds.keySet().iterator();

            while (statementsIter.hasNext()) {
                String indexName = (String) statementsIter.next();
                indexStatement = (IndexStatement) statements.get(indexName);
                dropIndex(indexName);
            }

            Iterator cldsIter = new SynchronisedIterator(clds.entrySet().iterator());
            Set threads = new HashSet();

            synchronized (threads) {
                for (int i = 1; i <= extraThreads; i++) {
                    Thread worker = new Thread(new Worker(threads, cldsIter, i));
                    threads.add(new Integer(i));
                    worker.start();
                }
            }

            try {
                while (cldsIter.hasNext()) {
                    Map.Entry cldEntry = (Map.Entry) cldsIter.next();
                    String cldName = (String) cldEntry.getKey();
                    LOG.info("Thread 0 processing class " + cldName);
                    statementsIter = ((Map) cldEntry.getValue()).entrySet().iterator();
                    while (statementsIter.hasNext()) {
                        Map.Entry statementEntry = (Map.Entry) statementsIter.next();
                        String indexName = (String) statementEntry.getKey();
                        indexStatement = (IndexStatement) statementEntry.getValue();
                        createIndex(c, indexName, indexStatement, 0);
                    }
                }
            } catch (NoSuchElementException e) {
                // This is fine - just a consequence of concurrent access to the iterator. It means
                // the end of the iterator has been reached, so there is no more work to do.
            }
            LOG.info("Thread 0 finished");
            synchronized (threads) {
                while (threads.size() != 0) {
                    LOG.info(threads.size() + " threads left");
                    threads.wait();
                }
            }
            LOG.info("All threads finished");
        } catch (Exception e) {
            String message = "Error creating indexes";
            if (indexStatement != null) {
                message = "Error creating indexes for " + indexStatement.getTableName() + "("
                    + indexStatement.getColumnNames() + ")";
            }
            throw new BuildException(message, e);
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    // empty
                }
            }
        }
    }

    private class Worker implements Runnable
    {
        private int threadNo;
        private Set threads;
        private Iterator cldsIter;

        /**
         * Create a new Worker object.
         * @param threads the Thread indexes
         * @param cldsIter an Iterator over the classes to index
         * @param threadNo the thread index of this thread
         */
        public Worker(Set threads, Iterator cldsIter, int threadNo) {
            this.threads = threads;
            this.cldsIter = cldsIter;
            this.threadNo = threadNo;
        }

        public void run() {
            Connection conn = null;
            try {
                try {
                    conn = database.getConnection();
                    conn.setAutoCommit(true);
                    while (cldsIter.hasNext()) {
                        Map.Entry cldEntry = (Map.Entry) cldsIter.next();
                        String cldName = (String) cldEntry.getKey();
                        LOG.info("Thread " + threadNo + " processing class " + cldName);
                        Iterator statementsIter = ((Map) cldEntry.getValue()).entrySet().iterator();
                        while (statementsIter.hasNext()) {
                            Map.Entry statementEntry = (Map.Entry) statementsIter.next();
                            String indexName = (String) statementEntry.getKey();
                            IndexStatement st = (IndexStatement) statementEntry.getValue();
                            createIndex(conn, indexName, st, threadNo);
                        }
                    }
                } catch (NoSuchElementException e) {
                    // rmpty
                } finally {
                    try {
                        if (conn != null) {
                            conn.close();
                        }
                    } finally {
                        LOG.info("Thread " + threadNo + " finished");
                        synchronized (threads) {
                            threads.remove(new Integer(threadNo));
                            threads.notify();
                        }
                    }
                }
            } catch (SQLException e) {
                LOG.error("Thread " + threadNo + " failed", e);
            }
        }
    }

    private static final int MAX_ITERATIONS = 10;

    /**
     * If an index name is longer than the Postgres limit (63), try shortening it by removing
     * the last lowercase letters in each part of the name.  eg. change 
     * TransposableElementInsertionSite__LocatedSequenceFeature__key_indentifer_org to
     * TranspElemenInsertSite__LocateSequenFeatur__key_indentifer_org 
     */
    private void compressNames(Map statements) {
        Set statementNames = new HashSet(statements.keySet());

        Iterator statementsIter = statementNames.iterator();

        while (statementsIter.hasNext()) {
            String origIndexName = (String) statementsIter.next();

            if (origIndexName.length() > POSTGRESQL_INDEX_NAME_LIMIT) {
                String indexName = origIndexName;
            
                // Don't compress the class names too match - start by shortening the longest parts
                // of the class names
                for (int i = MAX_ITERATIONS; i > 0; i--) {
                    Pattern pattern = Pattern.compile("([A-Z][a-z]{1," + i + "})[a-z]*");
                    Matcher matcher = pattern.matcher(indexName);
                    String newIndexName = matcher.replaceAll("$1");
                    
                    if (newIndexName.length() <= POSTGRESQL_INDEX_NAME_LIMIT) {
                        Object indexStatement = statements.get(origIndexName);
                        statements.remove(origIndexName);
                        statements.put(newIndexName, indexStatement);
                        break;
                    }
                }
            }
        }        
    }

    private void checkForIndexNameClashes(Map statements) throws BuildException {
        // index names truncated to 63 characters
        Map truncNames = new HashMap();

        Iterator statementsIter = statements.keySet().iterator();

        while (statementsIter.hasNext()) {
            String indexName = (String) statementsIter.next();
            String truncName;
            if (indexName.length() > POSTGRESQL_INDEX_NAME_LIMIT) {
                truncName = indexName.substring(0, POSTGRESQL_INDEX_NAME_LIMIT);
            } else {
                truncName = indexName;
            }
            if (truncNames.containsKey(truncName)) {
                throw new BuildException("tried to create a non-unique index name: "
                                           + truncName + " from " + indexName + " and "
                                           + truncNames.get(truncName));
            } else {
                truncNames.put(truncName, indexName);
            }
        }
    }

    /**
     * Add indexes for primary keys, indirection tables and 1-N relations to the relevant tables for
     * a given ClassDescriptor
     * @param cld the ClassDescriptor
     * @param statements the index creation statements for the given cld are added to this Map.
     * The key is the index name, the value is a IndexStatement.
     * @throws MetaDataException if a field os not found in model
     */
    protected void getStandardIndexStatements(ClassDescriptor cld, Map statements)
        throws MetaDataException {
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
                if (firstOne) {
                    if (fd instanceof AttributeDescriptor) {
                        doNulls = !((AttributeDescriptor) fd).isPrimitive();
                    } else if (fd instanceof ReferenceDescriptor) {
                        doNulls = true;
                    }
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
                    String indexNameBase;
                    if (tableName.equals(cldTableName)) {
                        indexNameBase = tableName + "__" + keyName;

                    } else {
                        indexNameBase = tableName + "__" + cldTableName.subSequence(0,
                                (cldTableName.length() > 16 ? 15 : cldTableName.length())) 
                            + "__" + keyName;
                    }
                    addStatement(statements, indexNameBase, tableName,
                                 StringUtil.join(fieldNames, ", ") + ", id", nextCld, tableMaster);
                    if (doNulls) {
                        addStatement(statements,
                                     indexNameBase + "__nulls",
                                     tableName, "(" + fieldNames.get(0) + " IS NULL)",
                                     nextCld, tableMaster);
                    }
                    doneFieldNames.add(fieldNames.get(0));
                }
            }
        }

        //and one for each bidirectional N-to-1 relation to increase speed of
        //e.g. company.getDepartments
        for (Iterator i = cld.getAllReferenceDescriptors().iterator(); i.hasNext();) {
            ReferenceDescriptor ref = (ReferenceDescriptor) i.next();
            ClassDescriptor tableMaster = schema.getTableMaster(cld);
            String tableName = DatabaseUtil.getTableName(tableMaster);
            if (FieldDescriptor.N_ONE_RELATION == ref.relationType()) {
                if (!schema.getMissingTables().contains(tableName.toLowerCase())) {
                    String fieldName = DatabaseUtil.getColumnName(ref);
                    if (!doneFieldNames.contains(fieldName)) {
                        addStatement(statements,
                                     cldTableName + "__"  + ref.getName(), tableName,
                                     fieldName + ", id", cld, tableMaster);
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
                    addStatement(statements,
                                 tableName + "__"  + columnName, tableName,
                                 columnName + ", " + columnName2, cld, null);
                    addStatement(statements,
                                 tableName + "__"  + columnName2, tableName,
                                 columnName2 + ", " + columnName, cld, null);
                }
            }
        }
    }

    /**
     * Add indexes for all fields to the relevant tables for a given ClassDescriptor.  Skip those
     * fields that have indexes created by createStandardIndexes().
     * @param cld the ClassDescriptor
     * @param statements the index creation statements for the given cld are added to this Map.
     * The key is the index name, the value is a IndexStatement.
     */
    protected void getAttributeIndexStatements(ClassDescriptor cld, Map statements) {

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
                        PrimaryKey key = (PrimaryKey) primaryKeyEntry.getValue();

                        String firstKeyField = (String) key.getFieldNames().iterator().next();

                        if (firstKeyField.equals(att.getName())) {
                            continue ATTRIBUTE;
                        }
                    }
                }

                String indexName = tableName + "__"  + att.getName();
                if (att.getType().equals("java.lang.String")) {
                    addStatement(statements, indexName, tableName, "lower(" + fieldName + ")",
                                 cld, null);
                } else {
                    addStatement(statements, indexName, tableName, fieldName, cld, null);
                }
                if (!att.isPrimitive()) {
                    addStatement(statements,
                                 indexName + "__nulls", tableName, "(" + fieldName + " IS NULL)",
                                 cld, null);
                }
            }
        }
    }

    /**
     * Create an IndexStatement object and add it to the statements Map.
     * @param the Map to add to
     * @param indexName the key to use in the Map - an IllegalArgumentException is thrown if an
     * Entry already exists for the indexName.
     * @param tableName the name of the table we are creating the index for
     * @param columns the columns to index
     * @param cld the ClassDescriptor describing the objects stored in tableName
     * @param tableMaster the master table class for cld
     */
    private void addStatement(Map statements, String indexName, String tableName,
                              String columnNames,
                              ClassDescriptor cld, ClassDescriptor tableMaster) {
        if (statements.containsKey(indexName)) {
            IndexStatement indexStatement = (IndexStatement) statements.get(indexName);

            if (!indexStatement.getColumnNames().equals(columnNames)
                || !indexStatement.getTableName().equals(tableName)) {
                throw new IllegalArgumentException("Tried to created two indexes with the "
                                                   + "same name: " + indexName);
            }
        }

        IndexStatement indexStatement =
            new IndexStatement(tableName, columnNames, cld, tableMaster);

        statements.put(indexName, indexStatement);
    }

    /**
     * Drop an index by name, ignoring any resulting errors
     * @param indexName the index name
     */
    protected void dropIndex(String indexName) {
        try {
            if (!indexesMade.contains(indexName)) {
                execute(c, "drop index " + indexName);
            }
        } catch (SQLException e) {
            // ignore because the exception is probably because the index doesn't exist
        }
    }

    /**
     * Create an named index on the specified columns of a table.
     *
     * @param conn a Connection
     * @param indexName the index name
     * @param indexStatement the IndexStatement
     * @param threadNo the number of the calling thread
     */
    protected void createIndex(Connection conn, String indexName, IndexStatement indexStatement,
            int threadNo) {
        String tableName = indexStatement.getTableName();
        LOG.info("Thread " + threadNo + " creating index: " + indexName);
        Set indexesForTable = (Set) tableIndexesDone.get(tableName);
        if (indexesForTable == null) {
            indexesForTable = Collections.synchronizedSet(new HashSet());
            tableIndexesDone.put(tableName, indexesForTable);
        }
        if (!indexesForTable.contains(indexStatement.getColumnNames())) {
            try {
                execute(conn, indexStatement.getStatementString(indexName));
            } catch (SQLException e) {
                // ignore - we just don't create this index
                LOG.error("failed to create index " + indexName + " for " + tableName + "("
                        + indexStatement.getColumnNames() + ")", e);
                System.err .println("Failed to create index " + indexName);
            }
        }
        indexesForTable.add(indexStatement.getColumnNames());
        indexesMade.add(indexName);
    }

    /**
     * Execute an sql statement.
     *
     * @param conn a Connection
     * @param sql the sql string for the statement to execute
     * @throws SQLException if an error occurs
     */
    protected void execute(Connection conn, String sql) throws SQLException {
        conn.createStatement().execute(sql);
    }
}

/**
 * A simple representation of an SQL index statement.
 * @author Kim Rutherford
 */
class IndexStatement
{
    private String tableName;
    private String columnNames;
    private ClassDescriptor cld;
    private ClassDescriptor tableMaster;

    /**
     * Return an IndexStatement that can be used to create an index on the specified columns of a
     * table.
     * @param tableName the table name
     * @param columnNames the column names
     * @param cld the class descriptor of the class to index
     * @param tableMaster the class descriptor of the table master to index
     */
    IndexStatement(String tableName, String columnNames, ClassDescriptor cld,
                   ClassDescriptor tableMaster) {
        this.tableName = tableName;
        this.columnNames = columnNames;
        this.cld = cld;
        this.tableMaster = tableMaster;
    }

    /**
     * Return the columnNames argument that was passed to the constructor.
     * @return the columnNames
     */
    String getColumnNames() {
        return columnNames;
    }

    /**
     * Return the tableName argument that was passed to the constructor.
     * @return the tableName
     */
    String getTableName() {
        return tableName;
    }

    /**
     * Return the cld argument that was passed to the constructor.
     * @return the cld
     */
    ClassDescriptor getCld() {
        return cld;
    }

    /**
     * Return the tableMaster that was passed to the constructor.
     * @return the tableMaster
     */
    ClassDescriptor getTableMaster() {
        return tableMaster;
    }

    /**
     * Return the SQL String to use to create the index.
     * @param indexName the index name to substitute into the statement.
     * @return the SQL String
     */
    String getStatementString(String indexName) {
        return "create index " + indexName + " on " + tableName + "(" + columnNames + ")";
    }
}
