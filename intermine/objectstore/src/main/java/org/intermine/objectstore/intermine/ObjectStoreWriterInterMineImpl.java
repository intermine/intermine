package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.objectstore.query.Clob.CLOB_PAGE_SIZE;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.Util;
import org.intermine.model.InterMineObject;
import org.intermine.model.StringConstructor;
import org.intermine.objectstore.DataChangedException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.proxy.Lazy;
import org.intermine.objectstore.query.Clob;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.PendingClob;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.sql.DatabaseUtil;
import org.intermine.sql.precompute.BestQuery;
import org.intermine.sql.precompute.OptimiserCache;
import org.intermine.sql.precompute.PrecomputedTable;
import org.intermine.sql.precompute.QueryOptimiser;
import org.intermine.sql.precompute.QueryOptimiserContext;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriter;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;
import org.intermine.util.DynamicUtil;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.ShutdownHook;

/**
 * An SQL-backed implementation of the ObjectStoreWriter interface, backed by
 * ObjectStoreInterMineImpl.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ObjectStoreWriterInterMineImpl extends ObjectStoreInterMineImpl
    implements ObjectStoreWriter
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreWriterInterMineImpl.class);
    private static final String[] CLOB_COLUMNS = new String[] {CLOBID_COLUMN, CLOBPAGE_COLUMN,
        CLOBVAL_COLUMN};
    protected Connection conn = null;
    protected boolean connInUse = false;
    protected ObjectStoreInterMineImpl os;
    protected Batch batch;
    protected String createSituation;
    protected String closeSituation;
    protected Map<Integer, Boolean> recentSequences;
    protected Map<String, TableInfo> tableToInfo;
    protected Map<String, String[]> tableToColNameArray;
    protected Map<String, Set<CollectionDescriptor>> tableToCollections;
    protected String connectionTakenBy = null;
    protected Set<Object> tablesAltered = new HashSet<Object>();

    private Long cumulativeWait = new Long(0);    // just for diagnostic, can be removed
    private Integer getConnectionCalls = 0;       // as above

    // if the property is set to true (recommended for the webapp), getConncetion() will get a new
    // connection if the current one has been closed by the back-end.
    // add osw.userprofile-production.robustConnection=true to default.intermine.webapp.properties
    // default is false
    Properties props = PropertiesUtil.getPropertiesStartingWith("osw.userprofile-production");
    private String robustConnection =
            PropertiesUtil.stripStart("osw.userprofile-production", props).
            getProperty("robustConnection", "false");

    /**
     * Constructor for this ObjectStoreWriter. This ObjectStoreWriter is bound to a single SQL
     * Connection, grabbed from the provided ObjectStore.
     *
     * @param os an ObjectStoreInterMineImpl
     * @throws ObjectStoreException if a problem occurs
     */
    public ObjectStoreWriterInterMineImpl(ObjectStore os) throws ObjectStoreException {
        super(((ObjectStoreInterMineImpl) os).getModel());
        schema = ((ObjectStoreInterMineImpl) os).getSchema();
        limitedContext = ((ObjectStoreInterMineImpl) os).limitedContext;
        description = "Writer(" + ((ObjectStoreInterMineImpl) os).description + ")";
        if (os instanceof ObjectStoreWriter) {
            throw new ObjectStoreException("Cannot create an ObjectStoreWriterInterMineImpl from "
                    + "another ObjectStoreWriter. Call osw.getObjectStore() and construct from "
                    + "the ObjectStore instead.");
        }
        this.os = (ObjectStoreInterMineImpl) os;
        db = this.os.db;
        try {
            conn = this.os.getConnection();
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not obtain connection to database "
                                           + db.getURL() + "(user=" + db.getUser()
                                           + ")", e);
        }
        this.os.writers.add(this);
        ShutdownHook.registerObject(new WeakReference<Object>(this));
        Exception e = new Exception();
        e.fillInStackTrace();
        StringWriter message = new StringWriter();
        PrintWriter pw = new PrintWriter(message);
        e.printStackTrace(pw);
        pw.close();
        createSituation = message.toString();
        int index = createSituation.indexOf("at junit.framework.TestCase.runBare");
        createSituation = (index < 0 ? createSituation : createSituation.substring(0, index));
        recentSequences = Collections.synchronizedMap(new WeakHashMap<Integer, Boolean>());
        batch = new Batch(new BatchWriterPostgresCopyImpl());
        tableToInfo = new HashMap<String, TableInfo>();
        tableToColNameArray = new HashMap<String, String[]>();
        tableToCollections = new HashMap<String, Set<CollectionDescriptor>>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectStoreWriterInterMineImpl getNewWriter() throws ObjectStoreException {
        throw new UnsupportedOperationException("Cannot get an ObjectStoreWriter from an existing "
                + "ObjectStoreWriter");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Results execute(Query q, int batchSize, boolean optimise, boolean explain,
            boolean prefetch) {
        if (tablesAltered.isEmpty()) {
            return super.execute(q, batchSize, optimise, explain, prefetch);
        } else {
            Results retval = new Results(q, this, getSequence(getComponentsForQuery(q)));
            if (batchSize != 0) {
                retval.setBatchSize(batchSize);
            }
            if (!optimise) {
                retval.setNoOptimise();
            }
            if (!explain) {
                retval.setNoExplain();
            }
            if (!prefetch) {
                retval.setNoPrefetch();
            }
            retval.setImmutable();
            //LOG.error("Results cache not used for " + q);
            return retval;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingletonResults executeSingleton(Query q, int batchSize, boolean optimise,
            boolean explain, boolean prefetch) {
        if (tablesAltered.isEmpty()) {
            return super.executeSingleton(q, batchSize, optimise, explain, prefetch);
        } else {
            SingletonResults retval = new SingletonResults(q, this, getSequence(
                        getComponentsForQuery(q)));
            if (batchSize != 0) {
                retval.setBatchSize(batchSize);
            }
            if (!optimise) {
                retval.setNoOptimise();
            }
            if (!explain) {
                retval.setNoExplain();
            }
            if (!prefetch) {
                retval.setNoPrefetch();
            }
            retval.setImmutable();
            //LOG.error("Results cache not used for " + q);
            return retval;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean everOptimise() {
        return tablesAltered.isEmpty();
    }

    /**
     * Returns the log used by this objectstore.
     *
     * @return the log
     */
    @Override
    public synchronized Writer getLog() {
        return os.getLog();
    }

    /**
     * Not implemented.
     *
     * @param log ignored
     */
    @Override
    public synchronized void setLog(Writer log) {
        throw new UnsupportedOperationException("Cannot change the log on a writer");
    }

    /**
     * Not implemented.
     *
     * @param tableName ignored
     */
    @Override
    public synchronized void setLogTableName(String tableName) {
        throw new UnsupportedOperationException("Cannot change the log table name on a writer");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void dbLog(long optimise, long estimated, long execute,
            long permitted, long convert, Query q, String sql) {
        os.dbLog(optimise, estimated, execute, permitted, convert, q, sql);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLogEverything(boolean logEverything) {
        throw new UnsupportedOperationException("Cannot change logEverything on a writer");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getLogEverything() {
        return os.getLogEverything();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVerboseQueryLog(boolean verboseQueryLog) {
        throw new UnsupportedOperationException("Cannot change verboseQueryLog on a writer");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getVerboseQueryLog() {
        return os.getVerboseQueryLog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLogExplains(boolean logExplains) {
        throw new UnsupportedOperationException("Cannot change logExplains on a writer");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getLogExplains() {
        return os.getLogExplains();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLogBeforeExecute(boolean logBeforeExecute) {
        throw new UnsupportedOperationException("Cannot change logBeforeExecute on a writer");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getLogBeforeExecute() {
        return os.getLogBeforeExecute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDisableResultsCache(boolean disableResultsCache) {
        throw new UnsupportedOperationException("Cannot change disableResultsCache on a writer");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getDisableResultsCache() {
        return os.getDisableResultsCache();
    }

    /**
     * Allows the changing of the BatchWriter that this ObjectStoreWriter uses.
     *
     * @param batchWriter the new BatchWriter - use BatchWriterSimpleImpl for writers likely to see
     * small batches, and optimised (eg BatchWriterPostgresCopyImpl) implementations for bulk-write
     * writers.
     * @throws ObjectStoreException if something goes wrong
     */
    public void setBatchWriter(BatchWriter batchWriter) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection(); // Must get connection - it is our concurrency control.
            batch.setBatchWriter(batchWriter);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Set the cutoff value used to decide if a bag should be put in a table.
     *
     * @param minBagTableSize don't use a table to represent bags if the bag is smaller than this
     * value
     */
    @Override
    public void setMinBagTableSize(int minBagTableSize) {
        os.setMinBagTableSize(minBagTableSize);
    }

    /**
     * Returns the cutoff value used to decide if a bag should be put in a table.
     *
     * @return an int
     */
    @Override
    public int getMinBagTableSize() {
        return os.getMinBagTableSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Connection getConnection() throws SQLException {
        int loops = 0;
        while (connInUse || (conn == null)) {
            if (conn == null) {
                throw new SQLException("This ObjectStoreWriter is closed");
            }
            /*Exception trace = new Exception();
            trace.fillInStackTrace();
            LOG.debug("Connection in use - entering wait", trace);*/
            if (loops > 100) {
                LOG.error("Waited for connection for 100 seconds - probably a deadlock"
                        + " - throwing exception");
                //LOG.error("The connection was taken out by stack trace: " + connectionTakenBy);
                throw new SQLException("This ObjectStoreWriter appears to be dead due to"
                        + " deadlock");
            } else if (loops > 1) {
                LOG.info("Waited for connection for " + loops + " seconds - perhaps there's"
                        + " a deadlock");
            } else {
                LOG.debug("Connection in use - entering wait");
            }
            try {
                wait(1000L);
            } catch (InterruptedException e) {
                // ignore
            }
            LOG.debug("Notified or timed out");
            loops++;
        }

        Long start = System.currentTimeMillis();
        // If the connection has been closed by the back-end replace it with a new connection.
        // NOTE this has a timeout of 30 seconds. Should this check happens during builds
        // (robustConnection=true, mis-configuration) it would increase significantly building time.

        if ("true".equals(robustConnection) && !conn.isValid(30)) {
            LOG.info("ObjectStoreWriter connection was closed, fetching new connection");
            conn = this.os.getConnection();
        }
        Long end = System.currentTimeMillis();
        getConnectionCalls++;

        if (end > start) {
            cumulativeWait = cumulativeWait + (end - start);
            LOG.debug("Spent " + (end - start) + " ms checking connections, for a total of "
                    + cumulativeWait + " ms after " + getConnectionCalls + " getConnection calls");
        }
        connInUse = true;

        // //
        //Exception trace = new Exception();
        //trace.fillInStackTrace();
        //StringWriter message = new StringWriter();
        //PrintWriter pw = new PrintWriter(message);
        //trace.printStackTrace(pw);
        //pw.println("In Thread " + Thread.currentThread().getName());
        //pw.flush();
        //connectionTakenBy = message.toString();
        //LOG.debug("getConnection returning connection");
        return conn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void releaseConnection(Connection c) {
        if ((conn == null) && (c != null)) {
            // In this situation, the writer has been closed while this operation was still
            // happening. We should return the Connection back to the ObjectStore.
            try {
                if (isInTransactionWithConnection(c)) {
                    abortTransactionWithConnection(c);
                    LOG.error("ObjectStoreWriterInterMineImpl closed in unfinished transaction"
                            + " - transaction aborted");
                }
            } catch (Exception e) {
                LOG.error("Exception caught when destroying transaction while closing"
                        + " ObjectStoreWriter", e);
            }
            try {
                batch.close(c);
            } catch (Exception e) {
                LOG.error("Exception caught when closing Batch while closing ObjectStoreWriter", e);
            }
            try {
                os.releaseConnection(c);
            } catch (Exception e) {
                // ignore
            }
        } else if (c == conn) {
            connInUse = false;
            //LOG.debug("Released connection - notifying");
            notify();
        } else if (c != null) {
            Exception trace = new Exception();
            trace.fillInStackTrace();
            LOG.warn("Attempt made to release the wrong connection", trace);
        }
    }

    /**
     * Overrides Object.finalize - release the connection back to the objectstore.
     */
    @Override
    protected synchronized void doFinalise() {
        if (conn != null) {
            LOG.error("Garbage collecting open ObjectStoreWriterInterMineImpl with sequence = "
                    + sequenceNumber + " and Database " + os.getDatabase().getURL()
                    + ", createSituation: " + createSituation);
            try {
                close();
            } catch (ObjectStoreException e) {
                LOG.error("Exception while garbage-collecting ObjectStoreWriterInterMineImpl: "
                        + e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() throws ObjectStoreException {
        if (conn == null) {
            // This writer is already closed
            throw new ObjectStoreException("This ObjectStoreWriter is already closed in situation: "
                    + closeSituation + ", present stack trace:");
        }
        Exception est = new Exception();
        est.fillInStackTrace();
        StringWriter message = new StringWriter();
        PrintWriter pw = new PrintWriter(message);
        est.printStackTrace(pw);
        pw.close();
        closeSituation = message.toString();
        int index = closeSituation.indexOf("at junit.framework.TestCase.runBare");
        closeSituation = (index < 0 ? closeSituation : closeSituation.substring(0, index));
        if (connInUse) {
            conn = null;
            throw new ObjectStoreException("Closed ObjectStoreWriter while it is being used. Note"
                    + " this writer will be automatically closed when the current operation"
                    + " finishes");
        } else {
            try {
                if (isInTransactionWithConnection(conn)) {
                    abortTransactionWithConnection(conn);
                    LOG.error("ObjectStoreWriterInterMineImpl closed in unfinished transaction"
                            + " - transaction aborted");
                }
            } catch (Exception e) {
                LOG.error("Exception caught when destroying transaction while closing"
                        + " ObjectStoreWriter", e);
            }
            try {
                batch.close(conn);
            } catch (Exception e) {
                LOG.error("Exception caught when closing Batch while closing ObjectStoreWriter", e);
            }
            try {
                os.releaseConnection(conn);
            } catch (Exception e) {
                // ignore
            }

            conn = null;
            connInUse = true;
            // remove reference to this writer from the parent ObjectStore
            this.os.writers.remove(this);
            notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectStore getObjectStore() {
        return os;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store(Object o) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            storeWithConnection(c, o);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Performs store with a given Connection.
     *
     * @param c the Connection
     * @param o the object to store
     * @throws ObjectStoreException sometimes
     */
    protected void storeWithConnection(Connection c, Object o) throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(c);
        if (!wasInTransaction) {
            beginTransactionWithConnection(c);
        }
        try {
            boolean doDeletes = (o instanceof InterMineObject ? populateIds(c,
                        (InterMineObject) o) : false);
            writePendingClobs(c, o);
            StringConstructor xml = null;
            String objectClass = null;
            Set<ClassDescriptor> classDescriptors = model.getClassDescriptorsForClass(o.getClass());

            if (doDeletes) {
                for (ClassDescriptor cld : classDescriptors) {
                    ClassDescriptor tableMaster = schema.getTableMaster(cld);
                    String tableName = DatabaseUtil.getTableName(tableMaster);
                    if (!schema.getMissingTables().contains(tableName.toLowerCase())) {
                        batch.deleteRow(c, tableName, "id", ((InterMineObject) o).getId());
                        tablesAltered.add(tableName);
                    }
                }
            }
            int tablesWritten = 0;
            Set<String> validFieldNames = new HashSet<String>();
            for (Map.Entry<String, TypeUtil.FieldInfo> entry : TypeUtil.getFieldInfos(o.getClass())
                    .entrySet()) {
                String fieldName = entry.getKey();
                TypeUtil.FieldInfo fieldInfo = entry.getValue();
                if (!Collection.class.isAssignableFrom(fieldInfo.getType())) {
                    validFieldNames.add(fieldName);
                }
            }
            for (ClassDescriptor cld : classDescriptors) {
                ClassDescriptor tableMaster = schema.getTableMaster(cld);
                TableInfo tableInfo = getTableInfo(tableMaster);
                Set<CollectionDescriptor> collections = tableToCollections.get(cld.getName());
                if (collections == null) {
                    LOG.info("Generating cached metadata for ClassDescriptor " + cld.getName());
                    collections = new HashSet<CollectionDescriptor>();
                    for (FieldDescriptor field : cld.getAllFieldDescriptors()) {
                        if (field instanceof CollectionDescriptor) {
                            collections.add((CollectionDescriptor) field);
                        }
                    }
                    tableToCollections.put(cld.getName(), collections);
                }

                if (!schema.getMissingTables().contains(tableInfo.tableName.toLowerCase())) {
                    tablesWritten++;
                    if (schema.isFlatMode(cld.getType()) && (!schema.isTruncated(schema
                                    .getTableMaster(cld)))
                            && (!(cld.getType().equals(o.getClass())))) {
                        Set<Class<?>> decomposed = Util.decomposeClass(o.getClass());
                        if (!((decomposed.size() == 1) && cld.getType().equals(decomposed.iterator()
                                        .next()))) {
                            throw new ObjectStoreException("Non-flat model heirarchy used in flat "
                                    + "mode. Cannot store object with classes = " + decomposed);
                        }
                    }
                    Object[] values = new Object[tableInfo.colNames.length];
                    Set<String> fieldNamesWritten = new HashSet<String>();
                    for (int colNo = 0; colNo < tableInfo.colNames.length; colNo++) {
                        Object value = null;
                        if ("tableclass".equals(tableInfo.colNames[colNo])) {
                            value = cld.getName();
                        } else if ("class".equals(tableInfo.colNames[colNo])) {
                            if (objectClass == null) {
                                StringBuffer sb = new StringBuffer();
                                boolean needComma = false;
                                for (Class<?> objectClazz : Util.decomposeClass(o
                                        .getClass())) {
                                    if (needComma) {
                                        sb.append(" ");
                                    }
                                    needComma = true;
                                    sb.append(objectClazz.getName());
                                }
                                objectClass = sb.toString();
                            }
                            value = objectClass;
                        } else if ("OBJECT".equals(tableInfo.colNames[colNo])) {
                            if (xml == null) {
                                if (o instanceof InterMineObject) {
                                    xml = ((InterMineObject) o).getoBJECT();
                                } else {
                                    xml = NotXmlRenderer.render(o);
                                }
                            }
                            value = xml;
                        } else if (validFieldNames.contains(tableInfo.fieldNames[colNo])) {
                            if (o instanceof InterMineObject) {
                                value = ((InterMineObject) o).getFieldProxy(tableInfo
                                        .fieldNames[colNo]);
                            } else {
                                value = TypeUtil.getFieldProxy(o, tableInfo.fieldNames[colNo]);
                            }
                            if (value instanceof Date) {
                                value = new Long(((Date) value).getTime());
                            }
                            if (value instanceof ClobAccess) {
                                value = ((ClobAccess) value).getDbDescription();
                            }
                            if ((value instanceof InterMineObject)
                                    && (colNo >= tableInfo.referencesFrom)) {
                                value = ((InterMineObject) value).getId();
                            } else if ((value instanceof InterMineObject)
                                    || (colNo >= tableInfo.referencesFrom)) {
                                value = null;
                            }
                            fieldNamesWritten.add(tableInfo.fieldNames[colNo]);
                        } else {
                            FieldDescriptor fieldDescriptor = tableInfo.fields[colNo];
                            if (fieldDescriptor instanceof AttributeDescriptor) {
                                String fieldType = ((AttributeDescriptor) fieldDescriptor)
                                    .getType();
                                if ("boolean".equals(fieldType)) {
                                    value = Boolean.FALSE;
                                } else if ("short".equals(fieldType)) {
                                    value = new Short((short) 0);
                                } else if ("int".equals(fieldType)) {
                                    value = new Integer(0);
                                } else if ("long".equals(fieldType)) {
                                    value = new Long(0L);
                                } else if ("float".equals(fieldType)) {
                                    value = new Float(0.0F);
                                } else if ("double".equals(fieldType)) {
                                    value = new Double(0.0);
                                }
                            }
                        }
                        values[colNo] = value;
                    }
                    if (schema.isFlatMode(cld.getType())) {
                        for (String validFieldName : validFieldNames) {
                            if (!fieldNamesWritten.contains(validFieldName)) {
                                Set<Class<?>> decomposed = Util.decomposeClass(o.getClass());
                                throw new ObjectStoreException("Cannot store object " + decomposed
                                        + " - no column for field " + validFieldName + " in table "
                                        + tableInfo.tableName);
                            }
                        }
                    }
                    batch.addRow(c, tableInfo.tableName, (o instanceof InterMineObject
                                ? ((InterMineObject) o).getId() : null), tableInfo.colNames,
                            values);
                    tablesAltered.add(tableInfo.tableName);
                }

                writeCollections(c, o, collections);
            }
            if (tablesWritten < 1) {
                throw new ObjectStoreException("Object " + Util.decomposeClass(o.getClass())
                        + " does not map onto any database table.");
            }
            if (o instanceof InterMineObject) {
                invalidateObjectById(((InterMineObject) o).getId());
            }
        } catch (SQLException e) {
            if (e.getNextException() == null) {
                throw new ObjectStoreException("Error while storing", e);
            } else {
                StringBuilder message = new StringBuilder();
                SQLException e2 = e;
                int messageNo = 1;
                while (e2 != null) {
                    if (messageNo > 1) {
                        message.append(", ");
                    }
                    message.append("Error " + messageNo + ": \"")
                        .append(e2.getMessage())
                        .append("\"");
                    messageNo++;
                }
                throw new ObjectStoreException("Error while storing. " + message, e);
            }
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException("Illegal access to value while storing", e);
        } finally {
            if (!wasInTransaction) {
                try {
                    commitTransactionWithConnection(c);
                } catch (ObjectStoreException e) {
                    abortTransactionWithConnection(c);
                    throw e;
                }
            }
        }
    }

    private void writeCollections(Connection c, Object o, Set<CollectionDescriptor> collections)
        throws IllegalAccessException, SQLException {
        for (CollectionDescriptor collection : collections) {
            @SuppressWarnings("unchecked") Collection<InterMineObject> coll
                = (Collection<InterMineObject>) ((InterMineObject) o)
                .getFieldValue(collection.getName());
            boolean needToStoreCollection = true;

            if (coll instanceof Lazy) {
                ObjectStore testOS = ((Lazy) coll).getObjectStore();
                if (testOS instanceof ObjectStoreWriter) {
                    testOS = ((ObjectStoreWriter) testOS).getObjectStore();
                }
                if (testOS.equals(getObjectStore())) {
                    needToStoreCollection = false;
                }
            }

            if (needToStoreCollection) {
                // Collection - if it's many to many, then write indirection table.
                if (collection.relationType() == FieldDescriptor.M_N_RELATION) {
                    String indirectTableName =
                        DatabaseUtil.getIndirectionTableName(collection);
                    String inwardColumnName =
                        DatabaseUtil.getInwardIndirectionColumnName(collection,
                                schema.getVersion());
                    String outwardColumnName =
                        DatabaseUtil.getOutwardIndirectionColumnName(collection,
                                schema.getVersion());
                    boolean swap = (inwardColumnName.compareTo(outwardColumnName) > 0);
                    String[] indirColNames = tableToColNameArray.get(indirectTableName);
                    if (indirColNames == null) {
                        indirColNames = new String[2];
                        indirColNames[0] = (swap ? inwardColumnName : outwardColumnName);
                        indirColNames[1] = (swap ? outwardColumnName : inwardColumnName);
                        tableToColNameArray.put(indirectTableName, indirColNames);
                    }
                    for (InterMineObject inCollection : coll) {
                        batch.addRow(c, indirectTableName, indirColNames[0],
                                     indirColNames[1],
                                     (swap ? ((InterMineObject) o).getId()
                                      : inCollection.getId()).intValue(),
                                     (swap ? inCollection.getId()
                                      : ((InterMineObject) o).getId()).intValue());
                        tablesAltered.add(indirectTableName);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addToCollection(Integer hasId, Class<?> clazz, String fieldName, Integer hadId)
        throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            addToCollectionWithConnection(c, hasId, clazz, fieldName, hadId);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Performs addToCollection with given connection
     *
     * @param c the Connection
     * @param hasId the ID of the object that has the collection
     * @param clazz the class of the object that has the collection
     * @param fieldName the name of the collection
     * @param hadId the ID of the object to place in the collection
     * @throws ObjectStoreException if an error occurs
     */
    protected void addToCollectionWithConnection(Connection c, Integer hasId, Class<?> clazz,
            String fieldName, Integer hadId) throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(c);
        if (!wasInTransaction) {
            beginTransactionWithConnection(c);
        }

        try {
            FieldDescriptor field = model.getFieldDescriptorsForClass(clazz).get(fieldName);
            if (field == null) {
                throw new ObjectStoreException("Field " + clazz.getName() + "." + fieldName
                        + " does not exist in the model.");
            }
            if (field.relationType() == FieldDescriptor.M_N_RELATION) {
                invalidateObjectById(hasId);
                invalidateObjectById(hadId);
                CollectionDescriptor coll = (CollectionDescriptor) field;
                String indirectTableName = DatabaseUtil.getIndirectionTableName(coll);
                String inwardColumnName = DatabaseUtil.getInwardIndirectionColumnName(coll,
                        schema.getVersion());
                String outwardColumnName = DatabaseUtil.getOutwardIndirectionColumnName(coll,
                        schema.getVersion());
                boolean swap = (inwardColumnName.compareTo(outwardColumnName) > 0);
                String[] indirColNames = tableToColNameArray.get(indirectTableName);
                if (indirColNames == null) {
                    indirColNames = new String[2];
                    indirColNames[0] = (swap ? inwardColumnName : outwardColumnName);
                    indirColNames[1] = (swap ? outwardColumnName : inwardColumnName);
                    tableToColNameArray.put(indirectTableName, indirColNames);
                }
                batch.addRow(c, indirectTableName, indirColNames[0], indirColNames[1],
                             (swap ? hasId : hadId).intValue(), (swap ? hadId : hasId).intValue());
                tablesAltered.add(indirectTableName);
            } else {
                throw new ObjectStoreException("Field " + clazz.getName() + "." + fieldName
                        + " is not a many-to-many collection.");
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Error while storing", e);
        } finally {
            if (!wasInTransaction) {
                try {
                    commitTransactionWithConnection(c);
                } catch (ObjectStoreException e) {
                    abortTransactionWithConnection(c);
                    throw e;
                }
            }
        }
    }

    /**
     * Produces metadata for a given table, caching it to save time.
     *
     * @param tableMaster the ClassDescriptor describing the table
     * @return a TableInfo object
     * @throws ObjectStoreException if something goes wrong
     */
    protected TableInfo getTableInfo(ClassDescriptor tableMaster)
        throws ObjectStoreException {
        String tableName = DatabaseUtil.getTableName(tableMaster);
        TableInfo retval = tableToInfo.get(tableName);
        if (retval == null) {
            retval = new TableInfo();
            retval.tableName = tableName;
            LOG.info("Generating cached metadata for table " + tableName);
            DatabaseSchema.Fields allColumns = schema.getTableFields(tableMaster);
            int colCount = allColumns.getAttributes().size() + allColumns.getReferences().size();
            boolean isTruncated = schema.isTruncated(tableMaster);
            boolean hasObject = "InterMineObject".equals(tableName) || (!(schema.isMissingNotXml()
                        || schema.isFlatMode(tableMaster.getType())));
            if (isTruncated) {
                colCount += 2;
            } else {
                if (!schema.isFlatMode(tableMaster.getType())) {
                    colCount++;
                }
            }
            if (hasObject) {
                colCount++;
            }
            retval.colNames = new String[colCount];
            retval.fieldNames = new String[colCount];
            retval.fields = new FieldDescriptor[colCount];
            int colNo = 0;
            if (hasObject) {
                retval.colNames[colNo] = "OBJECT";
                colNo++;
            }
            if (isTruncated) {
                retval.colNames[colNo] = "class";
                colNo++;
                retval.colNames[colNo] = "tableclass";
                colNo++;
            } else {
                if (!schema.isFlatMode(tableMaster.getType())) {
                    retval.colNames[colNo] = "class";
                    colNo++;
                }
            }
            for (AttributeDescriptor field : allColumns.getAttributes()) {
                retval.colNames[colNo] = DatabaseUtil.getColumnName(field);
                retval.fieldNames[colNo] = field.getName();
                retval.fields[colNo] = field;
                colNo++;
            }
            retval.referencesFrom = colNo;
            for (ReferenceDescriptor field : allColumns.getReferences()) {
                retval.colNames[colNo] = DatabaseUtil.getColumnName(field);
                retval.fieldNames[colNo] = field.getName();
                retval.fields[colNo] = field;
                colNo++;
            }
            tableToInfo.put(tableName, retval);
        }
        return retval;
    }

    /**
     * Populates the object o with IDs.
     *
     * @param c a Connection with which to fetch more IDs
     * @param o the InterMineObject
     * @return true if the object will need to be deleted from the DB before a store
     * @throws SQLException if the database cannot produce a new ID
     * @throws IllegalAccessException if the ID field cannot be set
     */
    protected boolean populateIds(Connection c, InterMineObject o) throws SQLException,
              IllegalAccessException {
        boolean doDeletes = true;
        // Make sure this object has an ID
        if (o.getId() == null) {
            o.setId(getSerialWithConnection(c));
            doDeletes = false;
        } else {
            doDeletes = !recentSequences.containsKey(o.getId());
        }
        recentSequences.remove(o.getId());

        // Make sure all objects pointed to have IDs
        for (Map.Entry<String, TypeUtil.FieldInfo> fieldEntry
                : TypeUtil.getFieldInfos(o.getClass()).entrySet()) {
            TypeUtil.FieldInfo fieldInfo = fieldEntry.getValue();
            if (InterMineObject.class.isAssignableFrom(fieldInfo.getType())) {
                InterMineObject obj = (InterMineObject) TypeUtil.getFieldProxy(o,
                        fieldInfo.getName());
                if ((obj != null) && (obj.getId() == null)) {
                    obj.setId(getSerialWithConnection(c));
                }
            } else if (Collection.class.isAssignableFrom(fieldInfo.getType())) {
                @SuppressWarnings("unchecked") Collection<Object> coll
                    = (Collection<Object>) o.getFieldValue(fieldInfo.getName());

                if (!(coll instanceof Lazy)) {
                    for (Object obj : coll) {
                        // the collection may contain simple objects which don't have ids
                        if (obj instanceof InterMineObject) {
                            InterMineObject imo = (InterMineObject) obj;
                            if (imo.getId() == null) {
                                imo.setId(getSerialWithConnection(c));
                            }
                        }
                    }
                }
            }
        }
        return doDeletes;
    }

    /**
     * Writes the contents of any pending Clobs to the database, and replaces them in the objects
     * with a real ClobAccess object.
     *
     * @param c a connection
     * @param o the object to transform
     * @throws ObjectStoreException if something goes wrong
     * @throws SQLException if something goes wrong
     * @throws IllegalAccessException if something goes wrong
     */
    protected void writePendingClobs(Connection c, Object o) throws ObjectStoreException,
        SQLException, IllegalAccessException {
        for (Map.Entry<String, TypeUtil.FieldInfo> fieldEntry
                : TypeUtil.getFieldInfos(o.getClass()).entrySet()) {
            TypeUtil.FieldInfo fieldInfo = fieldEntry.getValue();
            if (ClobAccess.class.isAssignableFrom(fieldInfo.getType())) {
                ClobAccess ca = (ClobAccess) TypeUtil.getFieldValue(o, fieldInfo.getName());
                if (ca instanceof PendingClob) {
                    // We can't call createClob here - we already have a connection, and
                    // we must use that one.
                    Clob clob = new Clob(getSerialWithConnection(c));
                    replaceClobWithConnection(c, clob, ((PendingClob) ca)
                            .toString());
                    DynamicUtil.setFieldValue(o, fieldInfo.getName(), new ClobAccess(this, clob));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addToBag(ObjectStoreBag osb, Integer element) throws ObjectStoreException {
        addAllToBag(osb, Collections.singleton(element));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAllToBag(ObjectStoreBag osb,
            Collection<Integer> coll) throws ObjectStoreException {
        try {
            Connection c = null;
            try {
                c = getConnection();
                addAllToBagWithConnection(c, osb, coll);
            } finally {
                releaseConnection(c);
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        }
    }

    /**
     * Adds elements to the given bag.
     *
     * @param c a Connection
     * @param osb an ObjectStoreBag
     * @param coll a Collection of Integers
     * @throws ObjectStoreException if there is an error in the underlying database
     */
    protected void addAllToBagWithConnection(Connection c, ObjectStoreBag osb,
            Collection<Integer> coll) throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(c);
        if (!wasInTransaction) {
            beginTransactionWithConnection(c);
        }

        try {
            for (Integer element : coll) {
                batch.addRow(c, INT_BAG_TABLE_NAME, BAGID_COLUMN, BAGVAL_COLUMN, osb.getBagId(),
                        element.intValue());
                tablesAltered.add(osb);
                tablesAltered.add(INT_BAG_TABLE_NAME);
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Error adding to bag", e);
        } finally {
            if (!wasInTransaction) {
                try {
                    commitTransactionWithConnection(c);
                } catch (ObjectStoreException e) {
                    abortTransactionWithConnection(c);
                    throw e;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromBag(ObjectStoreBag osb, Integer element) throws ObjectStoreException {
        removeAllFromBag(osb, Collections.singleton(element));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllFromBag(ObjectStoreBag osb,
            Collection<Integer> coll) throws ObjectStoreException {
        try {
            Connection c = null;
            try {
                c = getConnection();
                removeAllFromBagWithConnection(c, osb, coll);
            } finally {
                releaseConnection(c);
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        }
    }

    /**
     * Removes elements from the given bag.
     *
     * @param c a Connection
     * @param osb an ObjectStoreBag
     * @param coll a Collection of Integers
     * @throws ObjectStoreException if there is an error in the underlying database
     */
    protected void removeAllFromBagWithConnection(Connection c, ObjectStoreBag osb,
            Collection<Integer> coll) throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(c);
        if (!wasInTransaction) {
            beginTransactionWithConnection(c);
        }

        try {
            for (Integer element : coll) {
                batch.deleteRow(c, INT_BAG_TABLE_NAME, BAGID_COLUMN, BAGVAL_COLUMN, osb.getBagId(),
                        element.intValue());
                tablesAltered.add(osb);
                tablesAltered.add(INT_BAG_TABLE_NAME);
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Error removing from bag", e);
        } finally {
            if (!wasInTransaction) {
                try {
                    commitTransactionWithConnection(c);
                } catch (ObjectStoreException e) {
                    abortTransactionWithConnection(c);
                    throw e;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addToBagFromQuery(ObjectStoreBag osb, Query query) throws ObjectStoreException {
        List<QuerySelectable> select = query.getSelect();
        if (select.size() != 1) {
            throw new IllegalArgumentException("Query has incorrect number of SELECT elements.");
        }
        Class<?> type = select.get(0).getType();
        if (!(Integer.class.equals(type) || InterMineObject.class.isAssignableFrom(type))) {
            throw new IllegalArgumentException("The type of the result colum (" + type.getName()
                    + ") is not an Integer or InterMineObject");
        }
        try {
            Connection c = null;
            try {
                c = getConnection();
                Set<String> readTables = SqlGenerator.findTableNames(query, getSchema());
                readTables.add(INT_BAG_TABLE_NAME);
                batch.flush(c, readTables);
                addToBagFromQueryWithConnection(c, osb, query);
            } finally {
                releaseConnection(c);
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        }
    }

    /**
     * Adds elements to a bag from the results of a query.
     *
     * @param c a Connection
     * @param osb an ObjectStoreBag
     * @param query a Query with only one column
     * @throws ObjectStoreException if there is an error in the underlying database
     */
    protected void addToBagFromQueryWithConnection(Connection c, ObjectStoreBag osb,
            Query query) throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(c);
        if (!wasInTransaction) {
            beginTransactionWithConnection(c);
        }

        if (getMinBagTableSize() != -1) {
            createTempBagTables(c, query);
            flushOldTempBagTables(c);
        }

        // Queries may be on classes (where the select is a class) or a normal field query
        int kind;
        if (query.getSelect().get(0) instanceof QueryClass) {
            kind = SqlGenerator.ID_ONLY;
        } else {
            kind = SqlGenerator.QUERY_NORMAL;
        }

        String sql = SqlGenerator.generate(query, schema, db, null, kind, bagConstraintTables);

        try {
            if (everOptimise()) {
                PrecomputedTable pt = (PrecomputedTable) goFasterMap.get(query);
                BestQuery bestQuery;
                if (pt != null) {
                    OptimiserCache oCache = goFasterCacheMap.get(query);
                    bestQuery = QueryOptimiser.optimiseWith(sql, null, db, c,
                            QueryOptimiserContext.DEFAULT, Collections.singleton(pt), oCache);
                } else {
                    bestQuery = QueryOptimiser.optimise(sql, null, db, c,
                            QueryOptimiserContext.DEFAULT);
                }
                sql = bestQuery.getBestQueryString();
            }
            Statement s = c.createStatement();
            registerStatement(s);
            try {
                String alias = query.getAliases().get(query.getSelect().get(0));
                // Queries on QueryClasses don't have nice aliases for us to use...
                if (kind == SqlGenerator.ID_ONLY) {
                    alias = "id";
                }
                sql = "INSERT INTO " + INT_BAG_TABLE_NAME + " (" + BAGID_COLUMN + ", "
                        + BAGVAL_COLUMN + ") SELECT DISTINCT " + osb.getBagId() + ", sub."
                        + alias + " FROM (" + sql + ") AS sub WHERE sub." + alias
                        + " NOT IN (SELECT " + BAGVAL_COLUMN + " FROM " + INT_BAG_TABLE_NAME
                        + " WHERE " + BAGID_COLUMN + " = " + osb.getBagId() + ")";
                s.execute(sql);
                tablesAltered.add(osb);
                tablesAltered.add(INT_BAG_TABLE_NAME);
            } finally {
                deregisterStatement(s);
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Error running query: " + sql, e);
        } finally {
            if (!wasInTransaction) {
                try {
                    commitTransactionWithConnection(c);
                } catch (ObjectStoreException e) {
                    abortTransactionWithConnection(c);
                    throw e;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void replaceClob(Clob clob, String text) throws ObjectStoreException {
        try {
            Connection c = null;
            try {
                c = getConnection();
                replaceClobWithConnection(c, clob, text);
            } finally {
                releaseConnection(c);
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        }
    }

    /**
     * Replaces the contents of the given Clob with the given String.
     *
     * @param c a Connection
     * @param clob the Clob to write to
     * @param text the text to write to the Clob
     * @throws ObjectStoreException if something goes wrong
     */
    public void replaceClobWithConnection(Connection c, Clob clob, String text)
        throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(c);
        if (!wasInTransaction) {
            beginTransactionWithConnection(c);
        }

        try {
            Integer clobId = new Integer(clob.getClobId());
            batch.deleteRow(c, CLOB_TABLE_NAME, CLOBID_COLUMN, clobId);
            int length = text.length();
            for (int i = 0; i < length; i += CLOB_PAGE_SIZE) {
                batch.addRow(c, CLOB_TABLE_NAME, clobId, CLOB_COLUMNS, new Object[] {clobId,
                    new Integer(i / CLOB_PAGE_SIZE), text.substring(i, Math.min(i + CLOB_PAGE_SIZE,
                            length))});
            }
            tablesAltered.add(clob);
            tablesAltered.add(CLOB_TABLE_NAME);
        } catch (SQLException e) {
            throw new ObjectStoreException("Error adding to bag", e);
        } finally {
            if (!wasInTransaction) {
                try {
                    commitTransactionWithConnection(c);
                } catch (ObjectStoreException e) {
                    abortTransactionWithConnection(c);
                    throw e;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(InterMineObject o) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            deleteWithConnection(c, o);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Performs a delete, with a connection.
     *
     * @param c the Connection
     * @param o the object to delete
     * @throws ObjectStoreException sometimes
     */
    protected void deleteWithConnection(Connection c,
            InterMineObject o) throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(c);
        if (!wasInTransaction) {
            beginTransactionWithConnection(c);
        }

        try {
            // Make sure this object has an ID
            if (o.getId() == null) {
                throw new IllegalArgumentException("Attempt to delete an object without an ID: "
                        + o.toString());
            }

            for (ClassDescriptor cld : model.getClassDescriptorsForClass(o.getClass())) {
                ClassDescriptor tableMaster = schema.getTableMaster(cld);
                String tableName = DatabaseUtil.getTableName(tableMaster);
                if (!schema.getMissingTables().contains(tableName.toLowerCase())) {
                    batch.deleteRow(c, tableName, "id", o.getId());
                    tablesAltered.add(tableName);
                }
            }
            invalidateObjectById(o.getId());
        } catch (SQLException e) {
            throw new ObjectStoreException("Error while deleting", e);
        } finally {
            if (!wasInTransaction) {
                try {
                    commitTransactionWithConnection(c);
                } catch (ObjectStoreException e) {
                    abortTransactionWithConnection(c);
                    throw e;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(QueryClass qc, Constraint c) throws ObjectStoreException {
        Connection con = null;
        try {
            con = getConnection();
            deleteWithConnection(con, qc, c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(con);
        }
    }

    /**
     * Performs a delete, with a connection.
     *
     * @param con the Connection
     * @param qc the QueryClass in which to delete - note that this must currently be a Simple
     * Object class
     * @param c the Constraint to limit the deletes, or null to delete everything
     * @throws ObjectStoreException if something goes wrong
     */
    public void deleteWithConnection(Connection con, QueryClass qc,
            Constraint c) throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(con);
        if (!wasInTransaction) {
            beginTransactionWithConnection(con);
        }

        try {
            if (InterMineObject.class.isAssignableFrom(qc.getType())) {
                throw new ObjectStoreException("Cannot delete by query from " + qc.getType());
            }
            String tableName = DatabaseUtil.getTableName(getSchema().getModel()
                    .getClassDescriptorByName(qc.getType().getName()));
            batch.flush(con, Collections.singleton(tableName));
            StringBuffer sql = new StringBuffer("DELETE FROM " + tableName);
            if (c != null) {
                sql.append(" WHERE ");
                SqlGenerator.constraintToString(null, sql, c, null, getSchema(),
                        SqlGenerator.SAFENESS_SAFE, true);
            }
            con.createStatement().execute(sql.toString());
            tablesAltered.add(tableName);
        } catch (SQLException e) {
            throw new ObjectStoreException("Error while deleting", e);
        } finally {
            if (!wasInTransaction) {
                try {
                    commitTransactionWithConnection(con);
                } catch (ObjectStoreException e) {
                    abortTransactionWithConnection(con);
                    throw e;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            return isInTransactionWithConnection(c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Finds if we are in a transaction.
     *
     * @param c the Connection
     * @return true or false
     * @throws ObjectStoreException sometimes
     */
    protected boolean isInTransactionWithConnection(Connection c) throws ObjectStoreException {
        try {
            return !c.getAutoCommit();
        } catch (SQLException e) {
            throw new ObjectStoreException("Error finding transaction status", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beginTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            beginTransactionWithConnection(c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Begins a transaction.
     *
     * @param c the Connection
     * @throws ObjectStoreException if we are already in a transaction
     */
    protected void beginTransactionWithConnection(Connection c) throws ObjectStoreException {
        try {
            if (!c.getAutoCommit()) {
                throw new ObjectStoreException("beginTransaction called, but already in"
                        + " transaction");
            }
            c.setAutoCommit(false);
        } catch (SQLException e) {
            throw new ObjectStoreException("Error beginning transaction", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            commitTransactionWithConnection(c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Commits a transaction.
     *
     * @param c the Connection
     * @throws ObjectStoreException if we are not in a transaction
     */
    protected void commitTransactionWithConnection(Connection c) throws ObjectStoreException {
        try {
            batch.flush(c);
            if (c.getAutoCommit()) {
                throw new ObjectStoreException("commitTransaction called, but not in transaction");
            }
            c.commit();
            c.setAutoCommit(true);
            os.databaseAltered(tablesAltered);
            tablesAltered.clear();
        } catch (SQLException e) {
            throw new ObjectStoreException("Error committing transaction", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void abortTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            abortTransactionWithConnection(c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Aborts a transaction.
     *
     * @param c the Connection
     * @throws ObjectStoreException if we are not in a transaction
     */
    public void abortTransactionWithConnection(Connection c) throws ObjectStoreException {
        try {
            batch.clear();
            if (c.getAutoCommit()) {
                throw new ObjectStoreException("abortTransaction called, but not in transaction");
            }
            c.rollback();
            c.setAutoCommit(true);
            os.flushObjectById();
            tablesAltered.clear();
        } catch (SQLException e) {
            throw new ObjectStoreException("Error aborting transaction", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void batchCommitTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            batchCommitTransactionWithConnection(c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Commits a transaction and opens a new one, without guaranteeing the operation is finished
     * before this method returns.
     *
     * @param c the Connection
     * @throws ObjectStoreException if an error occurs
     */
    public void batchCommitTransactionWithConnection(Connection c) throws ObjectStoreException {
        try {
            batch.batchCommit(c);
            os.databaseAltered(tablesAltered);
            tablesAltered.clear();
        } catch (SQLException e) {
            throw new ObjectStoreException("Error batch-committing transaction", e);
        }
    }

    /**
     * {@inheritDoc}
     * This method should never be called on an ObjectStoreWriter.
     */
    @Override
    public void databaseAltered(Set<Object> tablesChanged) {
        throw new IllegalArgumentException("databaseAltered should never be called on an "
                + "ObjectStoreWriter");
    }

    /**
     * {@inheritDoc}
     * Delegate to the parent ObjectStore.
     */
    @Override
    public synchronized Map<Object, Integer> getSequence(Set<Object> tables) {
        return os.getSequence(tables);
    }

    /**
     * {@inheritDoc}
     * Delegate to the parent ObjectStore.
     */
    @Override
    public synchronized void checkSequence(Map<Object, Integer> sequence, Query q,
            String message) throws DataChangedException {
        //if ((!tablesAltered.isEmpty()) && (!sequence.isEmpty())) {
        //    throw new DataChangedException("Cannot query a writer with uncommitted changes");
        //}
        os.checkSequence(sequence, q, message);
    }

    /**
     * {@inheritDoc}
     *
     * This method is overridden in order to flush batches properly before the read.
     */
    @Override
    public List<ResultsRow<Object>> execute(Query q, int start, int limit, boolean optimise,
            boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            Set<String> readTables = SqlGenerator.findTableNames(q, getSchema());
            batch.flush(c, readTables);
            return executeWithConnection(c, q, start, limit, optimise, explain, sequence);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Delegates to the parent ObjectStore
     */
    @Override
    protected String generateSql(Connection c, Query q, int start, int limit)
        throws ObjectStoreException {
        return os.generateSql(c, q, start, limit);
    }

    /**
     * {@inheritDoc}
     *
     * This method is overridden in order to flush batches properly before the read.
     */
    @Override
    public int count(Query q, Map<Object, Integer> sequence) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            Set<String> readTables = SqlGenerator.findTableNames(q, getSchema());
            batch.flush(c, readTables);
            return countWithConnection(c, q, sequence);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * {@inheritDoc}
     *
     * This method is overridden in order to flush matches properly before the read.
     */
    @Override
    protected InterMineObject internalGetObjectById(Integer id,
            Class<? extends InterMineObject> clazz) throws ObjectStoreException {
        if (schema.isFlatMode(clazz)) {
            return super.internalGetObjectById(id, clazz);
        }
        Connection c = null;
        try {
            c = getConnection();
            String readTable = SqlGenerator.tableNameForId(clazz, getSchema());
            batch.flush(c, Collections.singleton(readTable));
            return internalGetObjectByIdWithConnection(c, id, clazz);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Called by the StatsShutdownHook on shutdown
     */
    @Override
    public synchronized void shutdown() {
        if (conn != null) {
            LOG.error("Shutting down open ObjectStoreWriterInterMineImpl with sequence = "
                    + sequenceNumber + " and Database " + os.getDatabase().getURL()
                    + ", createSituation = " + createSituation);
            try {
                close();
            } catch (ObjectStoreException e) {
                LOG.error("Exception caught while shutting down ObjectStoreWriterInterMineImpl: "
                        + e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultiConnection() {
        return false;
    }

    /**
     * Overrides, in order to store recentSequences.
     *
     * @param c the Connection
     * @return an Integer
     * @throws SQLException if an error occurs
     */
    @Override
    protected Integer getSerialWithConnection(Connection c) throws SQLException {
        Integer retval = super.getSerialWithConnection(c);
        recentSequences.put(retval, Boolean.TRUE);
        return retval;
    }

    private static class TableInfo
    {
        String tableName;
        String[] colNames;
        String[] fieldNames;
        FieldDescriptor[] fields;
        int referencesFrom;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Writer(" + os + ")";
    }
}
