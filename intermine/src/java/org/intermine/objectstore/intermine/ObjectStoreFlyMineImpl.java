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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.flymine.metadata.Model;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreAbstractImpl;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.precompute.QueryOptimiser;
import org.flymine.sql.query.ExplainResult;

import org.apache.log4j.Logger;

/**
 * An SQL-backed implementation of the ObjectStore interface. The schema is oriented towards data
 * retrieval and multiple inheritance, rather than efficient data storage.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ObjectStoreFlyMineImpl extends ObjectStoreAbstractImpl implements ObjectStore
{
    protected static final Logger LOG = Logger.getLogger(ObjectStoreFlyMineImpl.class);
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
    protected Connection getConnection() throws SQLException {
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
    protected void releaseConnection(Connection c) {
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
     * Executes a Query on this ObjectStore, asking for a certain range of rows to be returned.
     * This will usually only be called by the Resutls object returned from execute().
     *
     * @param q the Query to execute
     * @param start the first row to return, numbered from zero
     * @param limit the maximum number of rows to return
     * @param optimise true if the query should be optimised
     * @return a List of ResultRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int limit, boolean optimise)
        throws ObjectStoreException {
        checkStartLimit(start, limit);

        String sql = SqlGenerator.generate(q, start, limit, model);
        Connection c = null;
        try {
            if (optimise && everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            c = getConnection();
            ExplainResult explain = ExplainResult.getInstance(sql, c);

            if (explain.getTime() > maxTime) {
                throw (new ObjectStoreException("Estimated time to run query(" + explain.getTime()
                                                + ") greater than permitted maximum ("
                                                + maxTime + "): FQL query: " + q + ", SQL query: "
                                                + sql));
            }

            ResultSet sqlResults = c.createStatement().executeQuery(sql);
            List objResults = ResultsConverter.convert(sqlResults, q, this);
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
        String sql = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, model);
        Connection c = null;
        try {
            if (everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            c = getConnection();
            ExplainResult explain = ExplainResult.getInstance(sql, c);
            return new ResultsInfo(explain.getStart(), explain.getComplete(),
                    (int) explain.getEstimatedRows());
        } catch (SQLException e) {
            throw new ObjectStoreException("Problem explaining SQL statement \"" + sql + "\"", e);
        } finally {
            releaseConnection(c);
        }
    }
    /**
     * Runs a COUNT(*) for the given query, returning the number of rows the query will produce.
     *
     * @param q the Query to explain
     * @return the number of rows to be produced by the query
     * @throws ObjectStoreException if an error occurs counting the query
     */
    public int count(Query q) throws ObjectStoreException {
        String sql = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, model);
        Connection c = null;
        try {
            if (everOptimise) {
                sql = QueryOptimiser.optimise(sql, db);
            }
            sql = "SELECT COUNT(*) FROM (" + sql + ") as fake_table";
            c = getConnection();
            ResultSet sqlResults = c.createStatement().executeQuery(sql);
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
        flushObjectById((ObjectStoreWriter) null);
    }

    /**
     * Does flushObjectById on itself and all ObjectStoreWriters attached to itself.
     *
     * @param except an ObjectStoreWriter to not call flush on
     */
    public void flushObjectById(ObjectStoreWriter except) {
        super.flushObjectById();
        Iterator writerIter = writers.iterator();
        while (writerIter.hasNext()) {
            ObjectStoreWriter writer = (ObjectStoreWriter) writerIter.next();
            if ((writer != this) && (writer != except)) {
                writer.flushObjectById();
            }
        }
    }
}
