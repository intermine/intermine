package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2004 FlyMine
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreAbstractImpl;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryOrderable;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.precompute.PrecomputedTable;
import org.intermine.sql.precompute.PrecomputedTableManager;
import org.intermine.sql.precompute.QueryOptimiser;
import org.intermine.sql.query.ExplainResult;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;
import org.intermine.util.DatabaseUtil;
import org.intermine.util.ShutdownHook;
import org.intermine.util.Shutdownable;
import org.intermine.modelproduction.ModelParser;
import org.intermine.modelproduction.xml.InterMineModelParser;

import org.apache.log4j.Logger;

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
    protected Set missingTables = new HashSet();
    protected Set noObjectTables = new HashSet();
    protected Writer log = null;
    protected DatabaseSchema schema;
    protected Connection logTableConnection = null;
    protected Batch logTableBatch = null;
    protected String logTableName = null;
    private static final String[] LOG_TABLE_COLUMNS = new String[] {"optimise", "estimated",
        "execute", "permitted", "convert", "iql", "sql"};
    
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
         schema = new DatabaseSchema(model, Collections.EMPTY_LIST);
         ShutdownHook.registerObject(new WeakReference(this));
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
    }
  
    /**
     * Read the Model from the intermine_metadata table of the given Database.
     * @param db the Database to read from
     * @return a Model
     */
    private static Model getModelFromDatabase(Database db)
        throws SQLException {
        Connection connection = null;
        try {
            connection = db.getConnection();
            connection.setAutoCommit(true);
            Statement statement = connection.createStatement();
            statement.execute("SELECT value FROM intermine_metadata WHERE key = 'model'");
            ResultSet rs = statement.getResultSet();
            if (rs.next()) {
                String modelXML = rs.getString(1);
                
                ModelParser parser = new InterMineModelParser();
                StringReader reader = new StringReader(modelXML);
                try {
                    return parser.process(reader);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to read model from the database", e);
                }
            } else {
                throw new IllegalArgumentException("Failed to get model from: " + db.getURL()
                                                   + " - no model found");
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
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
                    LOG.error("releaseConnection called while in transaction - rolling back");
                    c.rollback();
                    c.setAutoCommit(true);
                }
                c.close();
            } catch (SQLException e) {
                StringWriter message = new StringWriter();
                PrintWriter pw = new PrintWriter(message);
                e.printStackTrace(pw);
                pw.flush();
                LOG.error("Could not release SQL connection " + c + ": " + message.toString());
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
                                           + " objectstore (check properties file)");
        }

        String missingTablesString = props.getProperty("missingTables");
        String noObjectTablesString = props.getProperty("noObjectTables");
        String logfile = props.getProperty("logfile");
        String truncatedClassesString = props.getProperty("truncatedClasses");
        String logTable = props.getProperty("logTable");

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
                if (truncatedClassesString != null) {
                    Model osModel;
                    try {
                        osModel = getModelFromDatabase(database);
                    } catch (Exception e) {
                        try {
                            osModel = getModelFromClasspath(osAlias, props);
                        } catch (MetaDataException metaDataException) {
                            throw new ObjectStoreException("Cannot load model", metaDataException);
                        }
                    }
                    List truncatedClasses = new ArrayList();
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
                    DatabaseSchema databaseSchema = new DatabaseSchema(osModel, truncatedClasses);
                    os = new ObjectStoreInterMineImpl(database, databaseSchema);
                } else {
                    try {
                        os = new ObjectStoreInterMineImpl(database, getModelFromDatabase(database));
                    } catch (Exception e) {
                        Model osModel;
                        
                        try {
                            osModel = getModelFromClasspath(osAlias, props);
                        } catch (MetaDataException metaDataException) {
                            throw new ObjectStoreException("Cannot load model", metaDataException);
                        }
                        os = new ObjectStoreInterMineImpl(database, osModel);
                    }
                }
                if (missingTablesString != null) {
                    String tables[] = missingTablesString.split(",");
                    for (int i = 0; i < tables.length; i++) {
                        os.missingTables.add(tables[i]);
                    }
                }
                if (noObjectTablesString != null) {
                    String tables[] = noObjectTablesString.split(",");
                    for (int i = 0; i < tables.length; i++) {
                        os.noObjectTables.add(tables[i]);
                    }
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
                            + "(optimise bigint, estimated bigint, execute bigint, "
                            + "permitted bigint, convert bigint, iql text, sql text)");
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
                        new Object[] {new Long(optimise), new Long(estimated), new Long(execute),
                            new Long(permitted), new Long(convert), q.toString(), sql});
            } catch (SQLException e) {
                LOG.error("Failed to write to log table: " + e);
            }
        }
    }

    /**
     * 
    /**
     * @see ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
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
        if (logTableName != null) {
            LOG.error("Garbage collecting ObjectStoreInterMineImpl with sequence = " + sequence
                    + " and Database " + getDatabase().getURL());
            close();
        }
    }

    /**
     * Closes this ObjectStore's DB log connection.
     */
    public void close() {
        LOG.info("Close called on ObjectStoreInterMineImpl with sequence = " + sequence);
        try {
            setLogTableName(null);
        } catch (SQLException e) {
            LOG.error("Failed to close log table connection: " + e);
        }
    }

    /**
     * Called by the ShutdownHook on shutdown.
     */
    public synchronized void shutdown() {
        if (logTableName != null) {
            LOG.error("Shutting down open ObjectStoreInterMineImpl with sequence = " + sequence
                    + " and Database " + getDatabase().getURL());
            close();
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
        checkStartLimit(start, limit);
        checkSequence(sequence, q, "Execute (START " + start + " LIMIT " + limit + ") ");

        String sql = SqlGenerator.generate(q, start, limit, schema, db);
        try {
            long estimatedTime = 0;
            long startOptimiseTime = System.currentTimeMillis();
            if (optimise && everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            long endOptimiseTime = System.currentTimeMillis();
            if (explain) {
                //System//.out.println(getModel().getName() + ": Executing SQL: EXPLAIN " + sql);
                //long time = (new Date()).getTime();
                ExplainResult explainResult = ExplainResult.getInstance(sql, c);
                //long now = (new Date()).getTime();
                //if (now - time > 10) {
                //    LOG.debug(getModel().getName() + ": Executed SQL (time = "
                //            + (now - time) + "): EXPLAIN " + sql);
                //}

                estimatedTime = explainResult.getTime();
                if (explainResult.getTime() > getMaxTime()) {
                    throw (new ObjectStoreQueryDurationException("Estimated time to run query("
                                + explainResult.getTime() + ") greater than permitted maximum ("
                                + getMaxTime() + "): IQL query: " + q + ", SQL query: " + sql));
                }
            }

            long preExecute = System.currentTimeMillis();
            ResultSet sqlResults = c.createStatement().executeQuery(sql);
            long postExecute = System.currentTimeMillis();
            List objResults = ResultsConverter.convert(sqlResults, q, this);
            long postConvert = System.currentTimeMillis();
            long permittedTime = (objResults.size() * 2) - 100 + start + (150 * q.getFrom().size())
                    + (sql.length() / 20);
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
            if (estimatedTime > 0) {
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
            QueryOrderable firstOrderBy = null;
            try {
                firstOrderBy = (QueryOrderable) q.getOrderBy().iterator().next();
            } catch (NoSuchElementException e) {
                firstOrderBy = (QueryNode) q.getSelect().iterator().next();
            }
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
                        SqlGenerator.registerOffset(q, start + rowNo + 1, schema, db,
                                (thisObj instanceof InterMineObject
                                    ? ((InterMineObject) thisObj).getId() : thisObj));
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
     * @see ObjectStore#estimate
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
        String sql = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db);
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
     * @see ObjectStore#count
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

        String sql = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db);
        try {
            if (everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            sql = "SELECT COUNT(*) FROM (" + sql + ") as fake_table";
            //long time = (new Date()).getTime();
            ResultSet sqlResults = c.createStatement().executeQuery(sql);
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
     * @see ObjectStoreAbstractImpl#internalGetObjectById
     *
     * This method is overridden in order to improve the performance of the operation - this
     * implementation does not bother with the EXPLAIN call to the underlying SQL database.
     */
    protected InterMineObject internalGetObjectById(Integer id,
            Class clazz) throws ObjectStoreException {
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
            ResultSet sqlResults = c.createStatement().executeQuery(sql);
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
     * @see ObjectStore#isMultiConnection
     */
    public boolean isMultiConnection() {
        return true;
    }

    /**
     * Creates a precomputed table for the given query.
     *
     * @param q the Query for which to create the precomputed table
     * @throws ObjectStoreException if anything goes wrong
     */
    public void precompute(Query q) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            precomputeWithConnection(c, q);
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
     * @throws ObjectStoreException if anything goes wrong
     */
    public void precomputeWithConnection(Connection c, Query q) throws ObjectStoreException {
        try {
            int tableNumber = -1;
            try {
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery("SELECT nextval('precomputedtablenumber')");
                if (!r.next()) {
                    throw new ObjectStoreException("No result while attempting to get a unique"
                            + " precomputed table name");
                }
                tableNumber = r.getInt(1);
            } catch (SQLException e) {
                Statement s = c.createStatement();
                s.execute("CREATE SEQUENCE precomputedtablenumber");
                ResultSet r = s.executeQuery("SELECT nextval('precomputedtablenumber')");
                if (!r.next()) {
                    throw new ObjectStoreException("No result while attempting to get a unique"
                            + " precomputed table name");
                }
                tableNumber = r.getInt(1);
            }
            String sql = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db);
            PrecomputedTable pt = new PrecomputedTable(new org.intermine.sql.query.Query(sql),
                    "pt_" + tableNumber, c);
            PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(db);
            ptm.add(pt);
        } catch (SQLException e) {
            throw new ObjectStoreException(e);
        }
    }
}
