package org.flymine.objectstore.ojb;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Properties;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryFactory;

import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.query.ExplainResult;
import org.flymine.objectstore.ObjectStoreAbstractImpl;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryHelper;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.metadata.Model;

/**
 * Implementation of ObjectStore that uses OJB as its underlying store.
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ObjectStoreOjbImpl extends ObjectStoreAbstractImpl
{
    protected static Map instances = new HashMap();
    protected Database db;
    protected PersistenceBrokerFactoryFlyMineImpl pbf = null;

    /**
     * Constructs an ObjectStoreOjbImpl interfacing with an OJB instance
     * NB There is one ObjectStore per Database, and it holds a PersistenceBrokerFactory
     *
     * @param db the database in which the model resides
     * @param model the name of the model
     * @throws NullPointerException if repository is null
     * @throws IllegalArgumentException if repository is invalid
     */
    protected ObjectStoreOjbImpl(Database db, Model model) {
        super(model);
        this.db = db;
        //should the factory be created for a given model?
        pbf = (PersistenceBrokerFactoryFlyMineImpl) PersistenceBrokerFactoryFactory.instance();
    }

    /**
     * Gets the PersistenceBroker used by this ObjectStoreOjbImpl.
     * This should only be used in testing - if a broker pool is in use then these brokers
     * are neither deleted or returned to the pool, which is wasteful.
     * Besides, usage presumes that OJB is the underlying mapping tool.
     *
     * @return the PersistenceBroker this object is using
     */
    public PersistenceBroker getPersistenceBroker() {
        return pbf.createPersistenceBroker(db, model.getName());
    }

    /**
     * Gets a ObjectStoreOjbImpl instance for the given underlying repository
     *
     * @param props The properties used to configure an OJB-based objectstore
     * @param model the metadata associated with this objectstore
     * @return the ObjectStoreOjbImpl for this repository
     * @throws IllegalArgumentException if repository is invalid
     * @throws ObjectStoreException if there is any problem with the underlying OJB instance
     */
    public static ObjectStoreOjbImpl getInstance(Properties props, Model model)
        throws ObjectStoreException {
        String dbAlias = props.getProperty("db");
        if (dbAlias == null) {
            throw new ObjectStoreException("No 'db' property specified for OJB"
                                           + " objectstore (check properties file)");
        }
        Database db;
        try {
            db = DatabaseFactory.getDatabase(dbAlias);
        } catch (Exception e) {
            throw new ObjectStoreException("Unable to get database for OJB ObjectStore: " + e);
        }
        synchronized (instances) {
            if (!(instances.containsKey(db))) {
                instances.put(db, new ObjectStoreOjbImpl(db, model));
            }
        }
        return (ObjectStoreOjbImpl) instances.get(db);
    }

    /**
     * Execute a Query on this ObjectStore, asking for a certain range of rows to be returned.
     * This will usually only be called by the Results object returned from execute().
     * <code>execute(Query q)</code>.
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

        PersistenceBrokerFlyMine pb = pbf.createPersistenceBroker(db, model.getName());
        ExplainResult explain = pb.explain(q, start, limit, optimise);

        if (explain.getTime() > maxTime) {
            throw (new ObjectStoreException("Estimated time to run query(" + explain.getTime()
                                            + ") greater than permitted maximum ("
                                            + maxTime + ")"));
        }

        List res = pb.execute(q, start, limit, optimise);
        for (int i = 0; i < res.size(); i++) {
            res.set(i, new ResultsRow(Arrays.asList((Object[]) res.get(i))));
        }
        pb.close();
        return res;
    }

    /**
     * Runs an EXPLAIN for the given query with specified start and limit parameters.  This
     * gives estimated time for a single 'page' of the query.
     *
     * @param q the query to explain
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ResultsInfo estimate(Query q)
            throws ObjectStoreException {
        PersistenceBrokerFlyMine pb = pbf.createPersistenceBroker(db, model.getName());
        ExplainResult result = pb.explain(q, 0, Integer.MAX_VALUE - 1, true);
        pb.close();
        return new ResultsInfo(result.getStart(), result.getComplete(),
                (int) result.getEstimatedRows());
    }

    /**
     * Execute a COUNT(*) on a query, returns the number of row the query will produce
     *
     * @param q Flymine Query on which to run COUNT(*)
     * @return the number of row to be produced by query
     * @throws ObjectStoreException if an error occurs counting the query
     */
    public int count(Query q) throws ObjectStoreException {
        PersistenceBrokerFlyMine pb = pbf.createPersistenceBroker(db, model.getName());
        int count = pb.count(q);
        pb.close();
        return count;
    }

    /**
     * @see ObjectStoreAbstractImpl#internalGetObjectByExample
     */
    protected Object internalGetObjectByExample(Object obj) throws ObjectStoreException {
        PersistenceBrokerFlyMine pb = pbf.createPersistenceBroker(db, model.getName());
        try {
            Query q = QueryHelper.createQueryForExampleObject(obj, model);
            List results = pb.execute(q, 0, 2, false);
            if (results.size() > 1) {
                throw new IllegalArgumentException("More than one object in the database has "
                                                   + "this primary key");
            }
            if (results.size() == 1) {
                Object ret = ((Object []) results.get(0))[0];
                try {
                    Results.promoteProxiesInObject(ret, this);
                } catch (Exception e) {
                    throw new ObjectStoreException("Problem promoting proxies", e);
                }
                return ret;
            }
        } finally {
            pb.close();
        }
        return null;
    }

}

