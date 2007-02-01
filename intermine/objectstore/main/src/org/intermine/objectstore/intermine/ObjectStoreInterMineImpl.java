package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.intermine.log.InterMineLogger;
import org.intermine.log.InterMineLoggerFactory;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreAbstractImpl;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintHelper;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ConstraintTraverseAction;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryOrderable;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.DatabaseUtil;
import org.intermine.sql.precompute.BestQuery;
import org.intermine.sql.precompute.BestQueryExplainer;
import org.intermine.sql.precompute.OptimiserCache;
import org.intermine.sql.precompute.PrecomputedTable;
import org.intermine.sql.precompute.PrecomputedTableManager;
import org.intermine.sql.precompute.QueryOptimiser;
import org.intermine.sql.precompute.QueryOptimiserContext;
import org.intermine.sql.query.ExplainResult;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;
import org.intermine.util.ShutdownHook;
import org.intermine.util.Shutdownable;
import org.intermine.util.TypeUtil;

/**
 * An SQL-backed implementation of the ObjectStore interface. The schema is oriented towards data
 * retrieval and multiple inheritance, rather than efficient data storage.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ObjectStoreInterMineImpl extends ObjectStoreAbstractImpl implements Shutdownable
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreInterMineImpl.class);
    protected static final int CACHE_LARGEST_OBJECT = 5000000;
    protected static Map instances = new HashMap();
    protected Database db;
    protected boolean everOptimise = true;
    protected Set writers = new HashSet();
    protected Writer log = null;
    protected DatabaseSchema schema;
    protected Connection logTableConnection = null;
    protected Batch logTableBatch = null;
    protected String logTableName = null;
    protected boolean logEverything = false;
    protected long statsBagTableTime = 0;
    protected long statsGenTime = 0;
    protected long statsOptTime = 0;
    protected long statsNulTime = 0;
    protected long statsEstTime = 0;
    protected long statsExeTime = 0;
    protected long statsConTime = 0;
    protected QueryOptimiserContext limitedContext;
    protected boolean verboseQueryLog = false;

    // don't use a table to represent bags if the bag is smaller than this value
    protected int minBagTableSize = -1;
    protected Map bagConstraintTables = Collections.synchronizedMap(new WeakHashMap());
    protected Set bagTablesInDatabase = Collections.synchronizedSet(new HashSet());
    protected Map goFasterMap = Collections.synchronizedMap(new IdentityHashMap());
    protected Map goFasterCacheMap = Collections.synchronizedMap(new IdentityHashMap());
    protected ReferenceQueue bagTablesToRemove = new ReferenceQueue();

    protected InterMineLogger logger = null;

    private static final String[] LOG_TABLE_COLUMNS = new String[] {"timestamp", "optimise",
        "estimated", "execute", "permitted", "convert", "iql", "sql"};

    /**
     * The name of the SEQUENCE in the database to use when generating unique integers in
     * getUniqueInteger().
     */
    public static final String UNIQUE_INTEGER_SEQUENCE_NAME = "objectstore_unique_integer";

    /**
     * Constructs an ObjectStoreInterMineImpl.
     *
     * @param db the database in which the model resides
     * @param model the model
     * @throws NullPointerException if db or model are null
     * @throws IllegalArgumentException if db or model are invalid
     */
    protected ObjectStoreInterMineImpl(Database db, Model model) {
        super(model);
        this.db = db;
        schema = new DatabaseSchema(model, Collections.EMPTY_LIST, false, Collections.EMPTY_SET);
        ShutdownHook.registerObject(new WeakReference(this));
        limitedContext = new QueryOptimiserContext();
        limitedContext.setTimeLimit(getMaxTime() / 10);
    }

    /**
     * Constructs an ObjectStoreInterMineImpl, with a schema.
     *
     * @param db the database in which the model resides
     * @param schema the schema
     * @throws NullPointerException if db or model are null
     * @throws IllegalArgumentException if db or model are invalid
     */
    protected ObjectStoreInterMineImpl(Database db, DatabaseSchema schema) {
        super(schema.getModel());
        this.db = db;
        this.schema = schema;
        ShutdownHook.registerObject(new WeakReference(this));
        limitedContext = new QueryOptimiserContext();
        limitedContext.setTimeLimit(getMaxTime() / 10);
    }

    /**
     * Returns the DatabaseSchema used by this ObjectStore.
     *
     * @return a DatabaseSchema
     */
    public DatabaseSchema getSchema() {
        return schema;
    }

    /**
     * Returns the Database used by this ObjectStore
     *
     * @return the db
     */
    public Database getDatabase() {
        return db;
    }

    /**
     * Returns a Connection. Please put them back.
     *
     * @return a java.sql.Connection
     * @throws SQLException if there is a problem with that
     */
    public Connection getConnection() throws SQLException {
        Connection retval = db.getConnection();
        if (!retval.getAutoCommit()) {
            retval.setAutoCommit(true);
        }
        return retval;
    }

    /**
     * Allows one to put a connection back.
     *
     * @param c a Connection
     */
    public void releaseConnection(Connection c) {
        if (c != null) {
            try {
                if (!c.getAutoCommit()) {
                    Exception e = new Exception();
                    e.fillInStackTrace();
                    LOG.error("releaseConnection called while in transaction - rolling back."
                              + System.getProperty("line.separator"), e);
                    c.rollback();
                    c.setAutoCommit(true);
                }
                c.close();
            } catch (SQLException e) {
                LOG.error("Could not release SQL connection " + c, e);
            }
        }
    }

    /**
     * Gets a ObjectStoreInterMineImpl instance for the given underlying properties
     *
     * @param osAlias the alias of this objectstore
     * @param props The properties used to configure a InterMine-based objectstore
     * @return the ObjectStoreInterMineImpl for this repository
     * @throws IllegalArgumentException if props or model are invalid
     * @throws ObjectStoreException if there is any problem with the instance
     */
    public static ObjectStoreInterMineImpl getInstance(String osAlias, Properties props)
        throws ObjectStoreException {
        String dbAlias = props.getProperty("db");
        if (dbAlias == null) {
            throw new ObjectStoreException("No 'db' property specified for InterMine"
                                           + " objectstore (" + osAlias + ")."
                                           + "Check properties file");
        }

        String missingTablesString = props.getProperty("missingTables");
        String logfile = props.getProperty("logfile");
        String truncatedClassesString = props.getProperty("truncatedClasses");
        String logTable = props.getProperty("logTable");
        String minBagTableSizeString = props.getProperty("minBagTableSize");
        String noNotXmlString = props.getProperty("noNotXml");
        String logEverythingString = props.getProperty("logEverything");
        String loggerAlias = props.getProperty("logger");
        String verboseQueryLogString = props.getProperty("verboseQueryLog");

        synchronized (instances) {
            ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) instances.get(osAlias);
            if (os == null) {
                Database database;
                try {
                    database = DatabaseFactory.getDatabase(dbAlias);
                } catch (Exception e) {
                    throw new ObjectStoreException("Unable to get database for InterMine"
                            + " ObjectStore", e);
                }
                Model osModel;
                try {
                    osModel = getModelFromClasspath(osAlias, props);
                } catch (MetaDataException e) {
                    throw new ObjectStoreException("Cannot load model", e);
                }
                List truncatedClasses = new ArrayList();
                if (truncatedClassesString != null) {
                    String classes[] = truncatedClassesString.split(",");
                    for (int i = 0; i < classes.length; i++) {
                        ClassDescriptor truncatedClassDescriptor =
                            osModel.getClassDescriptorByName(classes[i]);
                        if (truncatedClassDescriptor == null) {
                            throw new ObjectStoreException("Truncated class " + classes[i]
                                                           + " does not exist in the model");
                        }
                        truncatedClasses.add(truncatedClassDescriptor);
                    }
                }
                boolean noNotXml = false;
                if ("true".equals(noNotXmlString) || (noNotXmlString == null)) {
                    noNotXml = true;
                } else if ("false".equals(noNotXmlString)) {
                    noNotXml = false;
                } else {
                    throw new ObjectStoreException("Invalid value for property noNotXml: "
                            + noNotXmlString);
                }
                HashSet missingTables = new HashSet();
                if (missingTablesString != null) {
                    String tables[] = missingTablesString.split(",");
                    for (int i = 0; i < tables.length; i++) {
                        missingTables.add(tables[i].toLowerCase());
                    }
                }
                DatabaseSchema databaseSchema = new DatabaseSchema(osModel, truncatedClasses,
                        noNotXml, missingTables);
                os = new ObjectStoreInterMineImpl(database, databaseSchema);

                if (loggerAlias != null) {
                    try {
                        LOG.debug("Intermine logger instantiated for osalias:" + loggerAlias);
                        os.logger = InterMineLoggerFactory.getInterMineLogger(loggerAlias);
                    } catch (Exception e) {
                        LOG.debug("Intermine logger unable to be instantiated!", e);
                    }
                } else {
                    LOG.debug("Intermine logger alias not set for osalias:" + osAlias);
                }

                if (logfile != null) {
                    try {
                        FileWriter fw = new FileWriter(logfile, true);
                        BufferedWriter logWriter = new BufferedWriter(fw);
                        ShutdownHook.registerObject(logWriter);
                        os.setLog(logWriter);
                    } catch (IOException e) {
                        LOG.error("Error setting up execute log in file " + logfile + ": " + e);
                    }
                }
                if (logTable != null) {
                    try {
                        os.setLogTableName(logTable);
                    } catch (SQLException e) {
                        LOG.error("Error setting up execute log in database table " + logTable + ":"
                                + e);
                    }
                }
                if (minBagTableSizeString != null) {
                    try {
                        int minBagTableSizeInt = Integer.parseInt(minBagTableSizeString);
                        os.setMinBagTableSize(minBagTableSizeInt);
                    } catch (NumberFormatException e) {
                        LOG.error("Error setting minBagTableSize: " + e);
                    }
                }
                if ("true".equals(logEverythingString)) {
                    os.setLogEverything(true);
                }
                if ("true".equals(verboseQueryLogString)) {
                    os.setVerboseQueryLog(true);
                }
                instances.put(osAlias, os);
            }
            return os;
        }
    }

    /**
     * Returns the log used by this objectstore.
     *
     * @return the log
     */
    public synchronized Writer getLog() {
        return log;
    }

    /**
     * Allows the log to be set in this objectstore.
     *
     * @param log the log
     */
    public synchronized void setLog(Writer log) {
        LOG.info("Setting log to " + log);
        this.log = log;
    }

    /**
     * Allows the log table name to be set in this objectstore.
     *
     * @param tableName the table name
     * @throws SQLException if something goes wrong
     */
    public synchronized void setLogTableName(String tableName) throws SQLException {
        try {
            if (logTableName != null) {
                logTableBatch.close(logTableConnection);
                releaseConnection(logTableConnection);
                logTableConnection = null;
                logTableBatch = null;
                logTableName = null;
            }
            if (tableName != null) {
                logTableConnection = getConnection();
                if (!DatabaseUtil.tableExists(logTableConnection, tableName)) {
                    logTableConnection.createStatement().execute("CREATE TABLE " + tableName
                        + "(timestamp bigint, optimise bigint, estimated bigint, "
                        + "execute bigint, permitted bigint, convert bigint, iql text, sql text)");
                }
                logTableBatch = new Batch(new BatchWriterPostgresCopyImpl());
                logTableName = tableName;
            }
        } catch (SQLException e) {
            logTableConnection = null;
            logTableBatch = null;
            logTableName = null;
            throw e;
        }
    }

    /**
     * Sets the logEverything configuration option.
     *
     * @param logEverything a boolean
     */
    public void setLogEverything(boolean logEverything) {
        this.logEverything = logEverything;
    }

    /**
     * Sets the verboseQueryLog configuration option.
     *
     * @param verboseQueryLog a boolean
     */
    public void setVerboseQueryLog(boolean verboseQueryLog) {
        this.verboseQueryLog = verboseQueryLog;
    }

    /**
     * Gets the logEverything configuration option.
     *
     * @return a boolean
     */
    public boolean getLogEverything() {
        return logEverything;
    }

    /**
     * Allows the log table to be flushed, guaranteeing that all log entries are committed to the
     * database.
     */
    public synchronized void flushLogTable() {
        if (logTableName != null) {
            try {
                logTableBatch.flush(logTableConnection);
            } catch (SQLException e) {
                LOG.error("Failed to flush log entries to log table: " + e);
            }
        }
    }

    /**
     * Produce an entry in the DB log.
     *
     * @param optimise the number of milliseconds used to optimise the query
     * @param estimated the estimated number of milliseconds required to run the query
     * @param execute the number of milliseconds spent executing the query
     * @param permitted an acceptable number of milliseconds for the query to take
     * @param convert the number of milliseconds spent converting the results
     * @param q the Query run
     * @param sql the SQL string executed
     */
    protected synchronized void dbLog(long optimise, long estimated, long execute, long permitted,
            long convert, Query q, String sql) {
        if (logTableName != null) {
            try {
                logTableBatch.addRow(logTableConnection, logTableName, null, LOG_TABLE_COLUMNS,
                        new Object[] {new Long(System.currentTimeMillis()), new Long(optimise),
                            new Long(estimated), new Long(execute),
                            new Long(permitted), new Long(convert), q.toString(), sql});
            } catch (SQLException e) {
                LOG.error("Failed to write to log table: " + e);
            }
        }

        if (logger != null) {
            logger.logQuery("ObjectStoreInterMineImpl", System.getProperty("user.name"),
                    q, sql, new Long(optimise), new Long(estimated), new Long(execute),
                    new Long(permitted), new Long(convert));
        }
    }

    /**
     * Set the cutoff value used to decide if a bag should be put in a table.
     *
     * @param minBagTableSize don't use a table to represent bags if the bag is smaller than this
     * value
     */
    public void setMinBagTableSize(int minBagTableSize) {
        this.minBagTableSize = minBagTableSize;
    }

    /**
     * Returns the cutoff value used to decide if a bag should be put in a table.
     *
     * @return an int
     */
    public int getMinBagTableSize() {
        return minBagTableSize;
    }

    /*
     * Now, we need some query cancellation mechanism. So, here is how it will work:
     * 1. A thread calls registerRequest(Object requestId), which creates an entry in a lookup
     *     that matches that request ID with that thread. Now, all activity performed by that
     *     thread is associated with that request ID. Only one thread can have a particular request
     *     ID at a time, and only one request ID can have a particular thread at a time.
     * 2. That thread performs some activity. The Statement that the thread uses is entered into
     *     a lookup against the request ID.
     * 3. Another thread calls the cancelRequest(Object requestId) method, which looks up the
     *     Statement and calls Statement.cancel(), and records the request ID in a Set.
     * 4. The requesting thread receives an SQLException, and re-throws it as an
     *     ObjectStoreException.
     * 5. If another request comes in on the same request ID, the presence of the ID in the Set
     *     causes an exception to be thrown before the database is even consulted.
     *
     * Some of these things will require synchronised actions. Notably:
     *
     * 1. The action of registering a Statement with a request ID, which should throw an exception
     *     immediately if that request has been cancelled.
     * 2. The action of registering a request ID to cancel, which should call Statement.cancel() on
     *     any Statement already registered for that request ID.
     * 3. The action of deregistering a Statement for your request ID, which will remove all
     *     records.
     *
     * We don't want to do this registration etc. every single time anything happens - only if we
     * are supplied with a request id. Therefore requests without IDs cannot be cancelled.
     */

    private ThreadLocal requestId = new ThreadLocal();

    /**
     * This method registers a Thread with a request ID.
     *
     * @param id the request ID
     * @throws ObjectStoreException if this Thread is already registered
     */
    public void registerRequestId(Object id) throws ObjectStoreException {
        if (requestId.get() != null) {
            throw new ObjectStoreException("This Thread is already registered with a request ID");
        }
        requestId.set(id);
    }

    /**
     * This method deregisters a Thread from a request ID.
     *
     * @param id the request ID
     * @throws ObjectStoreException if the Thread is not registered with this ID
     */
    public void deregisterRequestId(Object id) throws ObjectStoreException {
        if (!id.equals(requestId.get())) {
            throw new ObjectStoreException("This Thread is not registered with ID " + id);
        }
        requestId.set(null);
    }

    private WeakHashMap cancelRegistry = new WeakHashMap();
    private static final String BLACKLISTED = "Blacklisted";

    /**
     * This method registers a Statement with the current Thread's request ID, or throws an
     * exception if that request is black-listed, or does nothing if no request ID is present
     * for this Thread.
     *
     * @param s a Statement
     * @throws ObjectStoreException if the request is black-listed
     */
    protected void registerStatement(Statement s) throws ObjectStoreException {
        Object id = requestId.get();
        if (id != null) {
            synchronized (cancelRegistry) {
                Object statement = cancelRegistry.get(id);
                if (statement == BLACKLISTED) {
                    throw new ObjectStoreException("Request id " + id + " is cancelled");
                } else if (statement != null) {
                    throw new ObjectStoreException("Request id " + id + " is currently being"
                            + " serviced in another thread. Don't share request IDs over multiple"
                            + " threads!");
                }
                cancelRegistry.put(id, s);
            }
        }
    }

    /**
     * This method cancels any Statement running in a given request ID, and blacklists that ID.
     *
     * @param id the request ID
     * @throws ObjectStoreException if the cancel fails
     */
    public void cancelRequest(Object id) throws ObjectStoreException {
        synchronized (cancelRegistry) {
            try {
                Object statement = cancelRegistry.get(id);
                if (statement instanceof Statement) {
                    ((Statement) statement).cancel();
                }
            } catch (SQLException e) {
                throw new ObjectStoreException("Statement cancel failed", e);
            } finally {
                cancelRegistry.put(id, BLACKLISTED);
            }
        }
    }

    /**
     * This method deregisters a Statement for the request ID of the current thread.
     *
     * @param s a Statement
     * @throws ObjectStoreException if this Thread does not have this Statement registered
     */
    protected void deregisterStatement(Statement s) throws ObjectStoreException {
        Object id = requestId.get();
        if (id != null) {
            synchronized (cancelRegistry) {
                Object statement = cancelRegistry.get(id);
                if ((statement != BLACKLISTED) && (statement != s)) {
                    throw new ObjectStoreException("The current thread does not have this statement"
                            + " registered");
                } else if (statement == s) {
                    cancelRegistry.remove(id);
                }
            }
        }
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        Constraint where = q.getConstraint();
        if (where instanceof ConstraintSet) {
            ConstraintSet where2 = (ConstraintSet) where;
            if (where2.getConstraints().isEmpty()
                    && (ConstraintOp.NAND.equals(where2.getOp())
                        || ConstraintOp.OR.equals(where2.getOp()))) {
                return Collections.EMPTY_LIST;
            }
        }
        Connection c = null;
        try {
            c = getConnection();
            return executeWithConnection(c, q, start, limit, optimise, explain, sequence);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Overrides Object.finalize - release the DB log connection.
     */
    protected synchronized void finalize() {
        LOG.error("Garbage collecting ObjectStoreInterMineImpl with sequence = " + sequenceNumber
                + " and Database " + getDatabase().getURL());
        try {
            close();
        } catch (ObjectStoreException e) {
            LOG.error("Exception while garbage-collecting ObjectStoreInterMineImpl: "
                    + e);
        }
    }

    /**
     * Closes this ObjectStore's DB log connection.
     *
     * @throws ObjectStoreException in subclasses
     */
    public synchronized void close() throws ObjectStoreException {
        LOG.info("Close called on ObjectStoreInterMineImpl with sequence = " + sequenceNumber
                + ", time spent: Bag Tables: " + statsBagTableTime + ", SQL Gen: " + statsGenTime
                + ", SQL Optimise: " + statsOptTime + ", Nulls: " + statsNulTime + ", Estimate: "
                + statsEstTime + ", Execute: " + statsExeTime + ", Results Convert: "
                + statsConTime);
        flushLogTable();
        Connection c = null;
        try {
            c = getConnection();
            LOG.info("Temporary tables to drop: " + bagTablesInDatabase);
            Iterator iter = bagTablesInDatabase.iterator();
            while (iter.hasNext()) {
                BagTableToRemove bttr = (BagTableToRemove) iter.next();
                try {
                    c.createStatement().execute(bttr.getDropSql());
                    LOG.info("Closing objectstore - dropped temporary table: " + bttr.getDropSql());
                } catch (SQLException e) {
                    LOG.error("Failed to drop temporary bag table: " + bttr.getDropSql()
                            + ", continuing");
                }
                iter.remove();
            }
            flushOldTempBagTables(c);
        } catch (SQLException e) {
            LOG.error("Failed to drop temporary bag tables: " + e);
        } finally {
            if (c != null) {
                releaseConnection(c);
            }
        }
    }

    /**
     * Called by the ShutdownHook on shutdown.
     */
    public synchronized void shutdown() {
        LOG.info("Shutting down open ObjectStoreInterMineImpl with sequence = " + sequenceNumber
                + " and Database " + getDatabase().getURL());
        try {
            close();
        } catch (ObjectStoreException e) {
            LOG.error("Exception caught while shutting down ObjectStoreInterMineImpl: "
                    + e);
        }
    }

    /**
     * Performs the actual execute, given a Connection.
     *
     * @param c the Connection
     * @param q the Query
     * @param start the start row number (inclusive, from zero)
     * @param limit maximum number of rows to return
     * @param optimise boolean
     * @param explain boolean
     * @param sequence int
     * @return a List of ResultRow objects
     * @throws ObjectStoreException sometimes
     */
    protected List executeWithConnection(Connection c, Query q, int start, int limit,
            boolean optimise, boolean explain, int sequence) throws ObjectStoreException {

        if (explain) {
            checkStartLimit(start, limit, q);
        }
        checkSequence(sequence, q, "Execute (START " + start + " LIMIT " + limit + ") ");

        long preBagTableTime = System.currentTimeMillis();
        if (getMinBagTableSize() != -1) {
            createTempBagTables(c, q);
            flushOldTempBagTables(c);
        }
        long preGenTime = System.currentTimeMillis();
        String sql = SqlGenerator.generate(q, start, limit, schema, db, bagConstraintTables);
        String generatedSql = sql;
        try {
            long estimatedTime = 0;
            long startOptimiseTime = System.currentTimeMillis();
            ExplainResult explainResult = null;
            if (optimise && everOptimise) {
                PrecomputedTable pt = (PrecomputedTable) goFasterMap.get(q);
                BestQuery bestQuery;
                if (pt != null) {
                    OptimiserCache oCache = (OptimiserCache) goFasterCacheMap.get(q);
                    bestQuery = QueryOptimiser.optimiseWith(sql, null, db, null,
                            (explain ? limitedContext : QueryOptimiserContext.DEFAULT),
                            Collections.singleton(pt), oCache);
                } else {
                    bestQuery = QueryOptimiser.optimise(sql, null, db, null,
                            (explain ? limitedContext : QueryOptimiserContext.DEFAULT));
                }
                sql = bestQuery.getBestQueryString();
                if (bestQuery instanceof BestQueryExplainer) {
                    explainResult = ((BestQueryExplainer) bestQuery).getBestExplainResult();
                }
            }
            long endOptimiseTime = System.currentTimeMillis();
            sql = sql.replaceAll(" ([^ ]*) IS NULL", " ($1 IS NULL) = true");
            sql = sql.replaceAll(" ([^ ]*) IS NOT NULL", " ($1 IS NOT NULL) = true");
            long postNullStuff = System.currentTimeMillis();
            if (explain) {
                //System//.out.println(getModel().getName() + ": Executing SQL: EXPLAIN " + sql);
                //long time = (new Date()).getTime();
                if (explainResult == null) {
                    explainResult = ExplainResult.getInstance(sql, c);
                }
                //long now = (new Date()).getTime();
                //if (now - time > 10) {
                //    LOG.debug(getModel().getName() + ": Executed SQL (time = "
                //            + (now - time) + "): EXPLAIN " + sql);
                //}

                //System .out.println("Explain result for " + sql + "\n"
                //        + ((PostgresExplainResult) explainResult).getExplainText());

                estimatedTime = explainResult.getTime();
                if (explainResult.getTime() > getMaxTime()) {
                    throw (new ObjectStoreQueryDurationException("Estimated time to run query("
                                + explainResult.getTime() + ") greater than permitted maximum ("
                                + getMaxTime() + "): IQL query: " + q + ", SQL query: " + sql));
                }
            }
            long preExecute = System.currentTimeMillis();
            Statement s = c.createStatement();
            registerStatement(s);
            ResultSet sqlResults;
            try {
                sqlResults = s.executeQuery(sql);
            } finally {
                deregisterStatement(s);
            }
            long postExecute = System.currentTimeMillis();
            List objResults = ResultsConverter.convert(sqlResults, q, this, c);
            long postConvert = System.currentTimeMillis();
            long permittedTime = (objResults.size() * 2) + start + (150 * q.getFrom().size())
                    + (sql.length() / 20) - (q.getFrom().size() == 0 ? 0 : 100);
            if (postExecute - preExecute > permittedTime) {
                if (postExecute - preExecute > sql.length()) {
                    LOG.info(getModel().getName() + ": Executed SQL (time = "
                            + (postExecute - preExecute) + " > " + permittedTime + ", rows = "
                            + objResults.size() + "): " + sql);
                } else {
                    LOG.info(getModel().getName() + ": Executed SQL (time = "
                            + (postExecute - preExecute) + " > " + permittedTime + ", rows = "
                            + objResults.size() + "): " + (sql.length() > 1000
                            ? sql.substring(0, 1000) : sql));
                }
            }
            if ((estimatedTime > 0) || getLogEverything()) {
                Writer executeLog = getLog();
                if (executeLog != null) {
                    try {
                        executeLog.write("EXECUTE\toptimise: " + (endOptimiseTime
                                    - startOptimiseTime) + "\testimated: " + estimatedTime
                                + "\texecute: " + (postExecute - preExecute) + "\tpermitted: "
                                + permittedTime + "\tconvert: " + (postConvert - postExecute) + "\t"
                                + q + "\t" + sql + "\n");
                    } catch (IOException e) {
                        LOG.error("Error writing to execute log " + e);
                    }
                }
                dbLog(endOptimiseTime - startOptimiseTime, estimatedTime, postExecute - preExecute,
                        permittedTime, postConvert - postExecute, q, sql);
            }
            long bagTableTime = preGenTime - preBagTableTime;
            statsBagTableTime += bagTableTime;
            long genTime = startOptimiseTime - preGenTime;
            statsGenTime += genTime;
            long optTime = endOptimiseTime - startOptimiseTime;
            statsOptTime += optTime;
            long nulTime = postNullStuff - endOptimiseTime;
            statsNulTime += nulTime;
            long estTime = preExecute - postNullStuff;
            statsEstTime += estTime;
            long exeTime = postExecute - preExecute;
            statsExeTime += exeTime;
            long conTime = postConvert - postExecute;
            statsConTime += conTime;
            if (verboseQueryLog) {
                LOG.info("(VERBOSE) iql: " + q + "\n"
                         + "generated sql: " + generatedSql + "\n"
                         + "optimised sql: " + sql + "\n"
                         + "bag tables: " + bagTableTime + "generate: " + genTime
                         + " ms, optimise: " + optTime + " ms, " + "replace nulls: " + nulTime
                         + " ms,  estimate: " + estTime + " ms, " + "execute: " + exeTime
                         + " ms, convert results: " + conTime + " ms, total: "
                         + (postConvert - preBagTableTime) + " ms");
            }
            QueryOrderable firstOrderBy = null;
            firstOrderBy = (QueryOrderable) q.getEffectiveOrderBy().iterator().next();
            if (q.getSelect().contains(firstOrderBy) && (objResults.size() > 1)) {
                int colNo = q.getSelect().indexOf(firstOrderBy);
                int rowNo = objResults.size() - 1;
                Object lastObj = ((List) objResults.get(rowNo)).get(colNo);
                rowNo--;
                boolean done = false;
                while ((!done) && (rowNo >= 0)) {
                    Object thisObj = ((List) objResults.get(rowNo)).get(colNo);
                    if ((lastObj != null) && (thisObj != null) && !lastObj.equals(thisObj)) {
                        done = true;
                        Object value = (thisObj instanceof InterMineObject
                                        ? ((InterMineObject) thisObj).getId() : thisObj);
                        SqlGenerator.registerOffset(q, start + rowNo + 1, schema, db,
                                                    value, bagConstraintTables);
                    }
                    rowNo--;
                }
            }
            return objResults;
        } catch (SQLException e) {
            throw new ObjectStoreException("Problem running SQL statement \"" + sql + "\"", e);
        }
    }

    /**
     * Generate sql from a Query
     *
     * @param q the Query
     * @return an SQL String
     * @throws ObjectStoreException if something goes wrong
     */
    public String generateSql(Query q) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            return generateSql(c, q, 0, Integer.MAX_VALUE);
        } catch (SQLException e) {
            throw new ObjectStoreException("Failed to get connection", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Create temporary tables for the bag in the BagConstraints of the given Query, then call
     * SqlGenerator.generate().  Entries are placed in the bagConstraintTables Map, which is a
     * WeakHashMap from BagConstraint -&gt; table name. When the BagConstraint is garbage-
     * collected, or when the JVM exits, the table associated with the table name is dropped from
     * the database.
     *
     * @param c a Connection to use
     * @param q the Query
     * @param start the start row number (inclusive, from zero)
     * @param limit maximum number of rows to return
     * @return the SQL for the Query
     * @throws ObjectStoreException if an error occurs
     */
    protected String generateSql(Connection c, Query q, int start, int limit)
        throws ObjectStoreException {

        if (getMinBagTableSize() != -1) {
            // We have a strong reference to the Query, and therefore all the BagConstraints. We can
            // count on the bagConstraintTables Map to be sane.

            createTempBagTables(c, q);
            flushOldTempBagTables(c);
        }

        return SqlGenerator.generate(q, start, limit, schema, db, bagConstraintTables);
    }

    /**
     * Create temporary tables for use with Query that use bags.  Each BagConstraint in the Query is
     * examined and a temporary table containing values of the appropriate type from the bag is
     * created. The new table names will be the values of the bagConstraintTables Map and the
     * BagConstraint references will be the keys.
     *
     * @param c a Connection to use
     * @param q the Query
     * @throws ObjectStoreException if there is a error in the ObjectStore
     */
    protected void createTempBagTables(Connection c, Query q)
        throws ObjectStoreException {

        final List bagConstraints = new ArrayList();

        ConstraintHelper.traverseConstraints(q.getConstraint(), new ConstraintTraverseAction() {
            public void apply(Constraint constraint) {
                if (constraint instanceof BagConstraint) {
                    BagConstraint bagConstraint = (BagConstraint) constraint;
                    bagConstraints.add(bagConstraint);
                }
            }
        });

        Iterator bagConstraintIterator = bagConstraints.iterator();

        boolean wasNotInTransaction = false;

        try {
            wasNotInTransaction = c.getAutoCommit();
            if (wasNotInTransaction) {
                c.setAutoCommit(false);
            }

            while (bagConstraintIterator.hasNext()) {
                BagConstraint bagConstraint = (BagConstraint) bagConstraintIterator.next();
                if (!bagConstraintTables.containsKey(bagConstraint)) {
                    Collection bag = bagConstraint.getBag();

                    if (bag.size() >= getMinBagTableSize()) {
                        createTempBagTable(c, bagConstraint, true,
                                new IqlQuery(q).getQueryString());
                    }
                }
            }
            if (wasNotInTransaction) {
                c.commit();
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("database error while creating temporary "
                                           + "table for bag", e);
        } finally {
            try {
                if (wasNotInTransaction) {
                    c.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new ObjectStoreException("database error while creating temporary "
                                               + "table for bag", e);
            }
        }
    }

    /**
     * Creates a temporary bag table for the given BagConstraint.
     *
     * @param c a Connection
     * @param bagConstraint a BagConstraint
     * @param log true to log this action
     * @param text extra data to place in the log
     * @return a BagTableToRemove object
     * @throws SQLException if an error occurs
     */
    protected BagTableToRemove createTempBagTable(Connection c, BagConstraint bagConstraint,
            boolean log, String text) throws SQLException {
        Class type = bagConstraint.getQueryNode().getType();
        String tableName =
            TypeUtil.unqualifiedName(type.getName()) + "_bag_" + getUniqueInteger(c);
        if (log) {
            LOG.info("Creating temporary table " + tableName + " of size "
                    + bagConstraint.getBag().size() + " for " + text);
        }
        DatabaseUtil.createBagTable(db, c, tableName, bagConstraint.getBag(), type);
        bagConstraintTables.put(bagConstraint, tableName);
        BagTableToRemove bagTableToRemove = new BagTableToRemove(tableName,
                bagTablesToRemove);
        bagTablesInDatabase.add(bagTableToRemove);
        return bagTableToRemove;
    }

    /**
     * Removes any temporary bag tables that are no longer reachable.
     *
     * @param c the Connection to use
     */
    public synchronized void flushOldTempBagTables(Connection c) {
        BagTableToRemove bttr = (BagTableToRemove) bagTablesToRemove.poll();
        while (bttr != null) {
            if (bagTablesInDatabase.contains(bttr)) {
                removeTempBagTable(c, bttr);
                LOG.info("Dropped unreachable temporary table: " + bttr.getDropSql());
            }
            bttr = (BagTableToRemove) bagTablesToRemove.poll();
        }
    }

    /**
     * Removes a temporary bag table, given a BagTableToRemove object.
     *
     * @param c the Connection to use
     * @param bttr the BagTableToRemove object
     */
    protected synchronized void removeTempBagTable(Connection c, BagTableToRemove bttr) {
        if (bagTablesInDatabase.contains(bttr)) {
            try {
                c.createStatement().execute(bttr.getDropSql());
            } catch (SQLException e) {
                LOG.error("Failed to drop temporary bag table: " + bttr.getDropSql()
                        + ", continuing");
            }
            bagTablesInDatabase.remove(bttr);
        }
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#estimate(Query)
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            return estimateWithConnection(c, q);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Runs an EXPLAIN for the given query.
     *
     * @param c the Connection
     * @param q the Query to explain
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    protected ResultsInfo estimateWithConnection(Connection c,
            Query q) throws ObjectStoreException {
        String sql = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db,
                                           bagConstraintTables);
        try {
            if (everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            //long time = (new Date()).getTime();
            ExplainResult explain = ExplainResult.getInstance(sql, c);
            //long now = (new Date()).getTime();
            //if (now - time > 10) {
            //    LOG.debug(getModel().getName() + ": Executed SQL (time = "
            //            + (now - time) + "): EXPLAIN " + sql);
            //}
            return new ResultsInfo(explain.getStart(), explain.getComplete(),
                    (int) explain.getEstimatedRows());
        } catch (SQLException e) {
            throw new ObjectStoreException("Problem explaining SQL statement \"" + sql + "\"", e);
        }
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#count(Query, int)
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            return countWithConnection(c, q, sequence);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Counts the results in a query, given a Connection.
     *
     * @param c the Connection
     * @param q the Query
     * @param sequence int
     * @return an int
     * @throws ObjectStoreException sometimes
     */
    protected int countWithConnection(Connection c, Query q,
            int sequence) throws ObjectStoreException {
        checkSequence(sequence, q, "COUNT ");

        String sql =
            SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, bagConstraintTables);
        try {
            if (everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            sql = "SELECT COUNT(*) FROM (" + sql + ") as fake_table";
            //long time = (new Date()).getTime();
            ResultSet sqlResults;
            Statement s = c.createStatement();
            registerStatement(s);
            try {
                sqlResults = s.executeQuery(sql);
            } finally {
                deregisterStatement(s);
            }
            //long now = (new Date()).getTime();
            //if (now - time > 10) {
            //    LOG.debug(getModel().getName() + ": Executed SQL (time = "
            //            + (now - time) + "): " + sql);
            //}
            sqlResults.next();
            return sqlResults.getInt(1);
        } catch (SQLException e) {
            throw new ObjectStoreException("Problem counting SQL statement \"" + sql + "\"", e);
        }
    }

    /**
     * @see ObjectStoreAbstractImpl#flushObjectById
     */
    public void flushObjectById() {
        super.flushObjectById();
        Iterator writerIter = writers.iterator();
        while (writerIter.hasNext()) {
            ObjectStoreWriter writer = (ObjectStoreWriter) writerIter.next();
            if (writer != this) {
                writer.flushObjectById();
            }
        }
        try {
            PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(db);
            ptm.dropEverything();
        } catch (SQLException e) {
            throw new Error("Problem with precomputed tables", e);
        }
    }

    /**
     * @see ObjectStoreAbstractImpl#internalGetObjectById(Integer, Class)
     *
     * This method is overridden in order to improve the performance of the operation - this
     * implementation does not bother with the EXPLAIN call to the underlying SQL database.
     */
    protected InterMineObject internalGetObjectById(Integer id,
            Class clazz) throws ObjectStoreException {
        if (schema.isFlatMode()) {
            return super.internalGetObjectById(id, clazz);
        }
        Connection c = null;
        try {
            c = getConnection();
            return internalGetObjectByIdWithConnection(c, id, clazz);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Gets an object by id given a Connection.
     *
     * @param c the Connection
     * @param id the id
     * @param clazz a Class of the object
     * @return the object
     * @throws ObjectStoreException if an error occurs
     */
    protected InterMineObject internalGetObjectByIdWithConnection(Connection c,
            Integer id, Class clazz) throws ObjectStoreException {
        String sql = SqlGenerator.generateQueryForId(id, clazz, schema);
        String currentColumn = null;
        try {
            //System//.out.println(getModel().getName() + ": Executing SQL: " + sql);
            //long time = (new Date()).getTime();
            ResultSet sqlResults;
            Statement s = c.createStatement();
            registerStatement(s);
            try {
                sqlResults = s.executeQuery(sql);
            } finally {
                deregisterStatement(s);
            }
            //long now = (new Date()).getTime();
            //if (now - time > 10) {
            //    System//.out.println(getModel().getName() + ": Executed SQL (time = "
            //            + (now - time) + "): " + sql);
            //}
            if (sqlResults.next()) {
                currentColumn = sqlResults.getString("a1_");
                if (sqlResults.next()) {
                    throw new ObjectStoreException("More than one object in the database has this"
                            + " primary key");
                }
                InterMineObject retval = NotXmlParser.parse(currentColumn, this);
                //if (currentColumn.length() < CACHE_LARGEST_OBJECT) {
                    cacheObjectById(retval.getId(), retval);
                //} else {
                //    LOG.debug("Not cacheing large object " + retval.getId() + " on getObjectById"
                //            + " (size = " + (currentColumn.length() / 512) + " kB)");
                //}
                return retval;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Problem running SQL statement \"" + sql + "\"", e);
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Unknown class mentioned in database OBJECT field"
                    + " while converting results: " + currentColumn, e);
        }
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#isMultiConnection()
     */
    public boolean isMultiConnection() {
        return true;
    }

    /**
     * Creates a precomputed table for the given query.
     *
     * @param q the Query for which to create the precomputed table
     * @param category a String describing the category of the precomputed table
     * @return the name of the new precomputed table
     * @throws ObjectStoreException if anything goes wrong
     */
    public String precompute(Query q, String category) throws ObjectStoreException {
        return precompute(q, null, false, category);
    }

    /**
     * Creates a precomputed table for the given query.
     *
     * @param q the Query for which to create the precomputed table
     * @param allFields true if all fields of QueryClasses in the SELECT list should be included in
     * the precomputed table's SELECT list.
     * @param category a String describing the category of the precomputed table
     * @return the name of the new precomputed table
     * @throws ObjectStoreException if anything goes wrong
     */
    public String precompute(Query q, boolean allFields,
            String category) throws ObjectStoreException {
        return precompute(q, null, allFields, category);
    }

    /**
     * Creates a precomputed table for the given query.
     *
     * @param q the Query for which to create the precomputed table
     * @param indexes a Collection of QueryOrderables for which to create indexes
     * @return the name of the new precomputed table
     * @param category a String describing the category of the precomputed table
     * @throws ObjectStoreException if anything goes wrong
     */
    public String precompute(Query q, Collection indexes,
            String category) throws ObjectStoreException {
        return precompute(q, indexes, false, category);
    }

    /**
     * Creates a precomputed table for the given query.
     *
     * @param q the Query for which to create the precomputed table
     * @param indexes a Collection of QueryOrderables for which to create indexes
     * @param allFields true if all fields of QueryClasses in the SELECT list should be included in
     * the precomputed table's SELECT list.
     * @param category a String describing the category of the precomputed table
     * @return the name of the new precomputed table
     * @throws ObjectStoreException if anything goes wrong
     */
    public String precompute(Query q, Collection indexes,
            boolean allFields, String category) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            return precomputeWithConnection(c, q, indexes, allFields, category);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Creates a precomputed table with the given query and connection.
     *
     * @param c the Connection
     * @param q the Query
     * @param indexes a Collection of QueryNodes for which to create indexes - they must all exist
     * in the SELECT list of the query
     * @param allFields true if all fields of QueryClasses in the SELECT list should be included in
     * the precomputed table's SELECT list.
     * @param category a String describing the category of the precomputed table
     * @return the name of the new precomputed table
     * @throws ObjectStoreException if anything goes wrong
     */
    public String precomputeWithConnection(Connection c, Query q, Collection indexes,
            boolean allFields, String category) throws ObjectStoreException {
        QueryNode qn = null;
        String sql = null;
        try {
            int tableNumber = getUniqueInteger(c);
            if (allFields) {
                sql = SqlGenerator.generate(q, schema, db, null, SqlGenerator.QUERY_FOR_PRECOMP,
                        Collections.EMPTY_MAP);
            } else {
                sql = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db,
                        Collections.EMPTY_MAP);
            }
            PrecomputedTable pt = new PrecomputedTable(new org.intermine.sql.query.Query(sql),
                    sql, "precomputed_table_" + tableNumber, category, c);
            Set stringIndexes = new HashSet();
            if (indexes != null && !indexes.isEmpty()) {
                Map aliases = q.getAliases();
                stringIndexes = new HashSet();
                String all = null;
                Iterator indexIter = indexes.iterator();
                while (indexIter.hasNext()) {
                    qn = (QueryNode) indexIter.next();
                    String alias = DatabaseUtil.generateSqlCompatibleName((String) aliases.get(qn));
                    if (qn instanceof QueryClass) {
                        alias += "id";
                    } else if (qn instanceof QueryField) {
                        if (String.class.equals(((QueryField) qn).getType())) {
                            alias = "lower(" + alias + ")";
                        }
                    }
                    if (all == null) {
                        all = alias;
                    } else {
                        stringIndexes.add(alias);
                        all += ", " + alias;
                    }
                }
                stringIndexes.add(all);
                LOG.info("Creating precomputed table for query " + q + " with indexes "
                        + stringIndexes);
            }
            PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(db);
            ptm.add(pt, stringIndexes);
            return pt.getName();
        } catch (NullPointerException e) {
            throw new ObjectStoreException("QueryNode " + qn + " (to be indexed) is not present in"
                                           + " the SELECT list of query " + q, e);
        } catch (SQLException e) {
            throw new ObjectStoreException(e);
        } catch (RuntimeException e) {
            throw new ObjectStoreException("Query SQL cannot be parsed, so cannot be precomputed: "
                    + sql + ", IQL: " + q);
        }
    }

    /**
     * Checks if a query is precomputed or not for the given type
     *
     * @param query the query
     * @param type the type
     * @return true if and only if the given query is (already) precomputed
     * @throws ObjectStoreException if the is a database problem
     */
    public boolean isPrecomputed(Query query, String type) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            return isPrecomputedWithConnection(c, query, type);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Checks if a query is precomputed or not for the given type and connection
     *
     * @param c the connection
     * @param query the query
     * @param type the type
     * @return true if and only if the given query is (already) precomputed
     * @throws ObjectStoreException if the is a database problem
     * @throws SQLException if the is a database problem
     */
    public boolean isPrecomputedWithConnection(Connection c, Query query, String type)
            throws ObjectStoreException, SQLException {
        PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(db);
        String sqlQuery = generateSql(c, query, 0, Integer.MAX_VALUE);
        return (ptm.lookupSql(type, sqlQuery) != null);
    }

    /**
     * Makes a certain Query go faster, using extra resources. The user should release
     * the resources later by calling releaseGoFaster on the same Query. Failure to release
     * resources may result in an overall degradation in performance.
     *
     * @param q the Query to speed up
     * @throws ObjectStoreException if something is wrong
     */
    public void goFaster(Query q) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            goFasterWithConnection(q, c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Makes a certain Query go faster, using extra resources. The user should release
     * the resources later by calling releaseGoFaster on the same Query. Failure to release
     * resources may result in an overall degradation in performance.
     *
     * @param q the Query to speed up
     * @param c the Connection to use
     * @throws ObjectStoreException if something is wrong
     */
    public void goFasterWithConnection(Query q, Connection c) throws ObjectStoreException {
        if (goFasterMap.containsKey(q)) {
            throw new ObjectStoreException("Error - this Query is already going faster");
        }
        try {
            String sql = SqlGenerator.generate(q, schema, db, null,
                    SqlGenerator.QUERY_FOR_PRECOMP, Collections.EMPTY_MAP);
            PrecomputedTable pt = new PrecomputedTable(new org.intermine.sql.query.Query(sql), sql,
                    "temporary_precomp_" + getUniqueInteger(c), "goFaster", c);
            PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(db);
            ptm.addTableToDatabase(pt, new HashSet(), false);
            goFasterMap.put(q, pt);
            goFasterCacheMap.put(q, new OptimiserCache());
        } catch (SQLException e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * Releases the resources used by goFaster().
     *
     * @param q the Query for which to release resources
     * @throws ObjectStoreException if something goes wrong
     */
    public void releaseGoFaster(Query q) throws ObjectStoreException {
        try {
            PrecomputedTable pt = (PrecomputedTable) goFasterMap.remove(q);
            if (pt != null) {
                goFasterCacheMap.remove(q);
                PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(db);
                ptm.deleteTableFromDatabase(pt.getName());
            }
        } catch (SQLException e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * Return a unique integer from a SEQUENCE in the database.
     *
     * @param c a Connection to use
     * @return an integer that is unique in this database
     * @throws SQLException if something goes wrong
     */
    public int getUniqueInteger(Connection c) throws SQLException {
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery("SELECT nextval('" + UNIQUE_INTEGER_SEQUENCE_NAME + "')");
        if (!r.next()) {
            throw new RuntimeException("No result while attempting to get a unique"
                                       + " integer from " + UNIQUE_INTEGER_SEQUENCE_NAME);
        }
        return r.getInt(1);
    }

    /**
     * Class describing a temporary bag table, which can be removed. A bag table can be forcibly
     * dropped by passing one of these objects to the removeTempBagTable method. Alternatively,
     * the table will be automatically dropped after the table name is garbage collected.
     *
     * @author Matthew Wakeling
     */
    protected class BagTableToRemove extends WeakReference
    {
        String dropSql;

        private BagTableToRemove(String tableName, ReferenceQueue refQueue) {
            super(tableName, refQueue);
            dropSql = "DROP TABLE " + tableName;
        }

        private String getDropSql() {
            return dropSql;
        }

        /**
         * Returns the SQL statement that will drop the table.
         *
         * @return a String
         */
        public String toString() {
            return dropSql;
        }
    }
}
