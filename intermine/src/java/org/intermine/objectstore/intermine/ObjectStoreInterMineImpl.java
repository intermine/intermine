package org.flymine.objectstore.flymine;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStoreAbstractImpl;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.precompute.QueryOptimiser;
import org.flymine.sql.query.ExplainResult;
import org.flymine.xml.lite.LiteParser;

import org.apache.log4j.Logger;

/**
 * An SQL-backed implementation of the ObjectStore interface. The schema is oriented towards data
 * retrieval and multiple inheritance, rather than efficient data storage.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ObjectStoreFlyMineImpl extends ObjectStoreAbstractImpl
{
    protected static final Logger LOG = Logger.getLogger(ObjectStoreFlyMineImpl.class);
    protected static final int CACHE_LARGEST_OBJECT = 5000000;
    protected static Map instances = new HashMap();
    protected Database db;
    protected boolean everOptimise = true;
    protected Set writers = new HashSet();

    /**
     * Constructs an ObjectStoreFlyMineImpl.
     *
     * @param db the database in which the model resides
     * @param model the name of the model
     * @throws NullPointerException if db or model are null
     * @throws IllegalArgumentException if db or model are invalid
     */
    protected ObjectStoreFlyMineImpl(Database db, Model model) {
        super(model);
        this.db = db;
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
     * Gets a ObjectStoreFlyMineImpl instance for the given underlying properties
     *
     * @param props The properties used to configure a FlyMine-based objectstore
     * @param model the metadata associated with this objectstore
     * @return the ObjectStoreFlyMineImpl for this repository
     * @throws IllegalArgumentException if props or model are invalid
     * @throws ObjectStoreException if there is any problem with the instance
     */
    public static ObjectStoreFlyMineImpl getInstance(Properties props, Model model)
        throws ObjectStoreException {
        String dbAlias = props.getProperty("db");
        if (dbAlias == null) {
            throw new ObjectStoreException("No 'db' property specified for FlyMine"
                                           + " objectstore (check properties file)");
        }
        Database db;
        try {
            db = DatabaseFactory.getDatabase(dbAlias);
        } catch (Exception e) {
            throw new ObjectStoreException("Unable to get database for FlyMine ObjectStore", e);
        }
        synchronized (instances) {
            if (!(instances.containsKey(db))) {
                instances.put(db, new ObjectStoreFlyMineImpl(db, model));
            }
        }
        return (ObjectStoreFlyMineImpl) instances.get(db);
    }

    /**
     * @see ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        checkStartLimit(start, limit);
        checkSequence(sequence, q, "Execute (START " + start + " LIMIT " + limit + ") ");

        String sql = SqlGenerator.generate(q, start, limit, model, db);
        Connection c = null;
        try {
            if (optimise && everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            c = getConnection();
            if (explain) {
                //System//.out.println(getModel().getName() + ": Executing SQL: EXPLAIN " + sql);
                //long time = (new Date()).getTime();
                ExplainResult explainResult = ExplainResult.getInstance(sql, c);
                //long now = (new Date()).getTime();
                //if (now - time > 10) {
                //    System//.out.println(getModel().getName() + ": Executed SQL (time = "
                //            + (now - time) + "): EXPLAIN " + sql);
                //}

                if (explainResult.getTime() > maxTime) {
                    throw (new ObjectStoreException("Estimated time to run query("
                                + explainResult.getTime() + ") greater than permitted maximum ("
                                + maxTime + "): FQL query: " + q + ", SQL query: " + sql));
                }
            }

            long time = (new Date()).getTime();
            ResultSet sqlResults = c.createStatement().executeQuery(sql);
            List objResults = ResultsConverter.convert(sqlResults, q, this);
            long now = (new Date()).getTime();
            long permittedTime = (objResults.size() * 2) - 100 + start + (150 * q.getFrom().size())
                    + (sql.length() / 20);
            if (now - time > permittedTime) {
                if (now - time > 100000) {
                    LOG.error(getModel().getName() + ": Executed SQL (time = "
                            + (now - time) + " > " + permittedTime + ", rows = " + objResults.size()
                            + "): " + sql);
                } else {
                    LOG.error(getModel().getName() + ": Executed SQL (time = "
                            + (now - time) + " > " + permittedTime + ", rows = " + objResults.size()
                            + "): " + (sql.length() > 1000 ? sql.substring(0, 1000) : sql));
                }
            }
            QueryNode firstOrderBy = null;
            try {
                firstOrderBy = (QueryNode) q.getOrderBy().iterator().next();
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
                    if (!lastObj.equals(thisObj)) {
                        done = true;
                        SqlGenerator.registerOffset(q, start + rowNo + 1, model, db,
                                (thisObj instanceof FlyMineBusinessObject
                                    ? ((FlyMineBusinessObject) thisObj).getId() : thisObj));
                    }
                    rowNo--;
                }
            }
            return objResults;
        } catch (SQLException e) {
            throw new ObjectStoreException("Problem running SQL statement \"" + sql + "\"", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Runs an EXPLAIN for the given query.
     *
     * @param q the Query to explain
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        String sql = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, model, db);
        Connection c = null;
        try {
            if (everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            c = getConnection();
            //long time = (new Date()).getTime();
            ExplainResult explain = ExplainResult.getInstance(sql, c);
            //long now = (new Date()).getTime();
            //if (now - time > 10) {
            //    System//.out.println(getModel().getName() + ": Executed SQL (time = "
            //            + (now - time) + "): EXPLAIN " + sql);
            //}
            return new ResultsInfo(explain.getStart(), explain.getComplete(),
                    (int) explain.getEstimatedRows());
        } catch (SQLException e) {
            throw new ObjectStoreException("Problem explaining SQL statement \"" + sql + "\"", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * @see ObjectStore#count
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        checkSequence(sequence, q, "COUNT ");

        String sql = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, model, db);
        Connection c = null;
        try {
            if (everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            sql = "SELECT COUNT(*) FROM (" + sql + ") as fake_table";
            c = getConnection();
            //long time = (new Date()).getTime();
            ResultSet sqlResults = c.createStatement().executeQuery(sql);
            //long now = (new Date()).getTime();
            //if (now - time > 10) {
            //    System//.out.println(getModel().getName() + ": Executed SQL (time = "
            //            + (now - time) + "): " + sql);
            //}
            sqlResults.next();
            return sqlResults.getInt(1);
        } catch (SQLException e) {
            throw new ObjectStoreException("Problem counting SQL statement \"" + sql + "\"", e);
        } finally {
            releaseConnection(c);
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
    }

    /**
     * @see ObjectStoreAbstractImpl#internalGetObjectById
     *
     * This method is overridden in order to improve the performance of the operation - this
     * implementation does not bother with the EXPLAIN call to the underlying SQL database.
     */
    protected FlyMineBusinessObject internalGetObjectById(Integer id) throws ObjectStoreException {
        String sql = SqlGenerator.generateQueryForId(id);
        String currentColumn = null;
        Connection c = null;
        try {
            c = getConnection();
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
                FlyMineBusinessObject retval = LiteParser.parse(currentColumn, this);
                //if (currentColumn.length() < CACHE_LARGEST_OBJECT) {
                    cacheObjectById(retval.getId(), retval);
                //} else {
                //    LOG.error("Not cacheing large object " + retval.getId() + " on getObjectById"
                //            + " (size = " + (currentColumn.length() / 512) + " kB)");
                //}
                return retval;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Problem running SQL statement \"" + sql + "\"", e);
        } catch (IOException e) {
            throw new ObjectStoreException("Impossible IO error reading from ByteArrayInputStream"
                    + " while converting results: " + currentColumn, e);
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Unknown class mentioned in database OBJECT field"
                    + " while converting results: " + currentColumn, e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * @see ObjectStore#isMultiConnection
     */
    public boolean isMultiConnection() {
        return true;
    }
}
