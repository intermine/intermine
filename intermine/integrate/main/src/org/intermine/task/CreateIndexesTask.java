package org.intermine.task;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.DatabaseSchema;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.SynchronisedIterator;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
 *
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
    private Map<String, Set<String>> tableIndexesDone = Collections.synchronizedMap(
            new HashMap<String, Set<String>>());
    private Set<String> indexesMade = Collections.synchronizedSet(new HashSet<String>());
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
    public void setUp() {
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
    @Override
    public void execute() {
        setUp();
        Model m = schema.getModel();
        Map<String, IndexStatement> statements = new TreeMap<String, IndexStatement>();
        Map<String, Map<String, IndexStatement>> clds =
            new TreeMap<String, Map<String, IndexStatement>>();

        for (ClassDescriptor cld : m.getClassDescriptors()) {
            try {
                Map<String, IndexStatement> cldIndexes = new TreeMap<String, IndexStatement>();
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
            } catch (MetaDataException e) {
                String message = "Error creating indexes for " + cld.getType();
                throw new BuildException(message, e);
            }
        }


        checkForIndexNameClashes(statements);

        IndexStatement indexStatement = null;

        Map<String, Set<String>> existingIndexes = new HashMap<String, Set<String>>();

        try {
            c = database.getConnection();
            c.setAutoCommit(true);

            // Find the names of all existing indexes so we can drop each index immediately before
            // attempting to create it.  That ensures that if we try to create an index with the
            // same name twice we get an exception.  Postgresql has a limit on index name length
            // (63) and will truncate longer names with a NOTICE rather than an error.

            Set<ClassDescriptor> masters = new HashSet<ClassDescriptor>();
            for (ClassDescriptor cld : m.getClassDescriptors()) {
                // For each class descriptor, remove all indexes from the master table
                ClassDescriptor master = schema.getTableMaster(cld);
                masters.add(master);
            }

            for (ClassDescriptor cld : masters) {
                String tableName = DatabaseUtil.getTableName(cld).toLowerCase();
                DatabaseMetaData metadata = c.getMetaData();
                ResultSet r = metadata.getIndexInfo(null, null, tableName, false, false);
                Set<String> indexNames = new HashSet<String>();
                while (r.next()) {
                    if ((r.getShort(7) != DatabaseMetaData.tableIndexStatistic)
                            && r.getBoolean(4)) {
                        String indexName = r.getString(6);
                        indexNames.add(indexName);
                    }
                }
                existingIndexes.put(cld.getName(), indexNames);
            }

            Iterator<Map.Entry<String, Map<String, IndexStatement>>> cldsIter =
                new SynchronisedIterator<Map.Entry<String, Map<String, IndexStatement>>>(clds
                        .entrySet().iterator());
            Set<Integer> threads = new HashSet<Integer>();

            synchronized (threads) {
                for (int i = 1; i <= extraThreads; i++) {
                    Thread worker = new Thread(new Worker(threads, cldsIter, existingIndexes, i));
                    threads.add(new Integer(i));
                    worker.setName("CreateIndexesTask extra thread " + i);
                    worker.start();
                }
            }

            try {
                while (cldsIter.hasNext()) {
                    Map.Entry<String, Map<String, IndexStatement>> cldEntry = cldsIter.next();
                    String cldName = cldEntry.getKey();
                    LOG.info("Thread 0 processing class " + cldName);
                    for (Map.Entry<String, IndexStatement> statementEntry : cldEntry.getValue()
                            .entrySet()) {
                        Set<String> existingCldIndexes = existingIndexes.get(cldName);
                        String indexName = statementEntry.getKey();
                        indexStatement = statementEntry.getValue();
                        if (existingCldIndexes != null
                                && existingCldIndexes.contains(indexName)) {
                            dropIndex(indexName, 0);
                        }
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
        private Set<Integer> threads;
        private Iterator<Map.Entry<String, Map<String, IndexStatement>>> cldsIter;
        private final Map<String, Set<String>> existingIndexes;
        /**
         * Create a new Worker object.
         * @param threads the Thread indexes
         * @param cldsIter an Iterator over the classes to index
         * @param existingIndexes a map from qualified to class name to a list of existing indexes
         * @param threadNo the thread index of this thread
         */
        public Worker(Set<Integer> threads,
                Iterator<Map.Entry<String, Map<String, IndexStatement>>> cldsIter,
                Map<String, Set<String>> existingIndexes, int threadNo) {
            this.threads = threads;
            this.cldsIter = cldsIter;
            this.threadNo = threadNo;
            this.existingIndexes = existingIndexes;
        }

        public void run() {
            Connection conn = null;
            try {
                try {
                    conn = database.getConnection();
                    conn.setAutoCommit(true);
                    while (cldsIter.hasNext()) {
                        Map.Entry<String, Map<String, IndexStatement>> cldEntry = cldsIter.next();
                        String cldName = cldEntry.getKey();
                        Set<String> existingCldIndexes = existingIndexes.get(cldName);
                        LOG.info("Thread " + threadNo + " processing class " + cldName);
                        for (Map.Entry<String, IndexStatement> statementEntry : cldEntry.getValue()
                                .entrySet()) {
                            String indexName = statementEntry.getKey();
                            IndexStatement st = statementEntry.getValue();
                            if (existingCldIndexes != null
                                    && existingCldIndexes.contains(indexName)) {
                                dropIndex(indexName, threadNo);
                            }
                            createIndex(conn, indexName, st, threadNo);
                        }
                    }
                } catch (NoSuchElementException e) {
                    // empty
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
    private void compressNames(Map<String, IndexStatement> statements) {
        Set<String> statementNames = new HashSet<String>(statements.keySet());

        for (String origIndexName : statementNames) {
            if (origIndexName.length() > POSTGRESQL_INDEX_NAME_LIMIT) {
                String indexName = origIndexName;

                // Don't compress the class names too match - start by shortening the longest parts
                // of the class names
                for (int i = MAX_ITERATIONS; i > 0; i--) {
                    Pattern pattern = Pattern.compile("([A-Z][a-z]{1," + i + "})[a-z]*");
                    Matcher matcher = pattern.matcher(indexName);
                    String newIndexName = matcher.replaceAll("$1");

                    if (newIndexName.length() <= POSTGRESQL_INDEX_NAME_LIMIT) {
                        IndexStatement indexStatement = statements.get(origIndexName);
                        statements.remove(origIndexName);
                        statements.put(newIndexName, indexStatement);
                        break;
                    }
                }
            }
        }
    }

    private void checkForIndexNameClashes(Map<String, IndexStatement> statements) {
        // index names truncated to 63 characters
        Map<String, String> truncNames = new HashMap<String, String>();

        for (String indexName : statements.keySet()) {
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
    protected void getStandardIndexStatements(ClassDescriptor cld,
            Map<String, IndexStatement> statements) throws MetaDataException {
        // Set of field names that already are the first element of an index.
        Set<String> doneFieldNames = new HashSet<String>();
        String cldTableName = DatabaseUtil.getTableName(cld).toLowerCase();

        boolean simpleClass = !InterMineObject.class.isAssignableFrom(cld.getType());

        //add an index for each primary key
        for (Map.Entry<String, PrimaryKey> entry : PrimaryKeyUtil.getPrimaryKeys(cld).entrySet()) {
            String keyName = entry.getKey();
            PrimaryKey key = entry.getValue();
            List<String> fieldNames = new ArrayList<String>();
            for (String fieldName : key.getFieldNames()) {
                FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
                if (fd == null) {
                    throw new MetaDataException("field (" + fieldName + ") not found for class: "
                                                + cld.getName() + " for key name " + keyName + ".");
                }
                fieldNames.add(DatabaseUtil.getColumnName(fd));
            }

            // create indexes on this class and on all subclasses
            Set<ClassDescriptor> clds = new HashSet<ClassDescriptor>(Collections.singleton(cld));
            clds.addAll(cld.getModel().getAllSubs(cld));
            for (ClassDescriptor nextCld : clds) {
                ClassDescriptor tableMaster = schema.getTableMaster(nextCld);
                String tableName = DatabaseUtil.getTableName(tableMaster).toLowerCase();
                if (!schema.getMissingTables().contains(tableName)) {
                    String indexNameBase;
                    if (tableName.equals(cldTableName)) {
                        indexNameBase = tableName + "__" + keyName;

                    } else {
                        indexNameBase = tableName + "__" + cldTableName.subSequence(0,
                                (cldTableName.length() > 16 ? 15 : cldTableName.length()))
                            + "__" + keyName;
                    }
                    addStatement(statements, indexNameBase, tableName,
                                 StringUtil.join(fieldNames, ", ") + (simpleClass ? "" : ", id"),
                                 nextCld, tableMaster);
                    doneFieldNames.add(fieldNames.get(0));
                }
            }
        }

        //and one for each bidirectional N-to-1 relation to increase speed of
        //e.g. company.getDepartments
        for (ReferenceDescriptor ref : cld.getAllReferenceDescriptors()) {
            ClassDescriptor tableMaster = schema.getTableMaster(cld);
            String tableName = DatabaseUtil.getTableName(tableMaster).toLowerCase();
            if (FieldDescriptor.N_ONE_RELATION == ref.relationType()) {
                if (!schema.getMissingTables().contains(tableName)) {
                    String fieldName = DatabaseUtil.getColumnName(ref);
                    if (!doneFieldNames.contains(fieldName)) {
                        addStatement(statements,
                                     cldTableName + "__"  + ref.getName(), tableName,
                                     fieldName + (simpleClass ? "" : ", id"), cld, tableMaster);
                    }
                }
            }
        }

        // finally add an index to all M-to-N indirection table columns
        for (CollectionDescriptor col : cld.getCollectionDescriptors()) {
            if (FieldDescriptor.M_N_RELATION == col.relationType()) {
                String tableName = DatabaseUtil.getIndirectionTableName(col).toLowerCase();
                String columnName = DatabaseUtil.getInwardIndirectionColumnName(col,
                        schema.getVersion());
                String columnName2 = DatabaseUtil.getOutwardIndirectionColumnName(col,
                        schema.getVersion());
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
    protected void getAttributeIndexStatements(ClassDescriptor cld,
            Map<String, IndexStatement> statements) {

        Map<String, PrimaryKey> primaryKeys = PrimaryKeyUtil.getPrimaryKeys(cld);
        String tableName = DatabaseUtil.getTableName(cld).toLowerCase();
        if (!schema.getMissingTables().contains(tableName)) {

        ATTRIBUTE:
            for (AttributeDescriptor att : cld.getAllAttributeDescriptors()) {
                if ("id".equals(att.getName())) {
                    continue;
                }

                String fieldName = DatabaseUtil.getColumnName(att);

                if (!"java.lang.String".equals(att.getType())) {
                    // if the attribute is the first column of a primary key, don't bother creating
                    // another index for it - unless it's a String attribute in which case we want
                    // to create a LOWER() index
                    for (Map.Entry<String, PrimaryKey> primaryKeyEntry : primaryKeys.entrySet()) {
                        PrimaryKey key = primaryKeyEntry.getValue();
                        String firstKeyField = key.getFieldNames().iterator().next();

                        if (firstKeyField.equals(att.getName())) {
                            continue ATTRIBUTE;
                        }
                    }
                }

                String indexName = tableName + "__"  + att.getName();
                if ("java.lang.String".equals(att.getType())) {
                    // we add 'text_pattern_ops' so that LIKE queries will use the index.  see:
                    // http://www.postgresql.org/docs/8.2/static/indexes-opclass.html
                    addStatement(statements, indexName + "_like", tableName,
                                 "lower(" + fieldName + ") text_pattern_ops",
                                 cld, null);
                    // this index is used by = and IN constraints
                    addStatement(statements, indexName + "_equals", tableName,
                                 "lower(" + fieldName + ")",
                                 cld, null);

                } else {
                    addStatement(statements, indexName, tableName, fieldName, cld, null);
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
    private void addStatement(Map<String, IndexStatement> statements, String indexName,
            String tableName, String columnNames, ClassDescriptor cld,
            ClassDescriptor tableMaster) {
        // Lower case the index so it matches names returned from Postgres
        indexName = indexName.toLowerCase();
        if (statements.containsKey(indexName)) {
            IndexStatement indexStatement = statements.get(indexName);

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
     * @param threadNo the thread number included for logging
     */
    protected void dropIndex(String indexName, int threadNo) {
        try {
            if (!indexesMade.contains(indexName)) {
                LOG.info("Thread " + threadNo + " dropping index: " + indexName);
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
        Set<String> indexesForTable = tableIndexesDone.get(tableName);
        if (indexesForTable == null) {
            indexesForTable = Collections.synchronizedSet(new HashSet<String>());
            tableIndexesDone.put(tableName, indexesForTable);
        }
        if (!indexesForTable.contains(indexStatement.getColumnNames())) {
            try {
                execute(conn, indexStatement.getStatementString(indexName));
            } catch (SQLException e) {
                // ignore - we just don't create this index
                LOG.info("failed to create index " + indexName + " for " + tableName + "("
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
