package org.flymine.objectstore.ojb;

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
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.util.PropertiesUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.MetaDataException;

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
    protected String model;
    protected PersistenceBrokerFactoryFlyMineImpl pbf = null;

    /**
     * No argument constructor
     *
     */
    protected ObjectStoreOjbImpl() {
        Properties props = PropertiesUtil.getPropertiesStartingWith("os.query");
        props = PropertiesUtil.stripStart("os.query", props);
        maxTime = Long.parseLong((String) props.get("max-time"));
        maxLimit = Integer.parseInt((String) props.get("max-limit"));
        maxOffset = Integer.parseInt((String) props.get("max-offset"));
    }

    /**
     * Constructs an ObjectStoreOjbImpl interfacing with an OJB instance
     * NB There is one ObjectStore per Database, and it holds a PersistenceBrokerFactory
     *
     * @param db the database in which the model resides
     * @param model the name of the model
     * @throws NullPointerException if repository is null
     * @throws IllegalArgumentException if repository is invalid
     */
    protected ObjectStoreOjbImpl(Database db, String model) {
        this();
        this.db = db;
        this.model = model;
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
        return pbf.createPersistenceBroker(db, model);
    }

    /**
     * Gets a ObjectStoreOjbImpl instance for the given underlying repository
     *
     * @param props The properties used to configure an OJB-based objectstore
     * @return the ObjectStoreOjbImpl for this repository
     * @throws IllegalArgumentException if repository is invalid
     * @throws ObjectStoreException if there is any problem with the underlying OJB instance
     */
    public static ObjectStoreOjbImpl getInstance(Properties props) throws ObjectStoreException {
        String dbAlias = props.getProperty("db");
        if (dbAlias == null) {
            throw new ObjectStoreException("No 'db' property specified for OJB"
                                           + " objectstore (check properties file)");
        }
        String modelName = props.getProperty("model");
        if (modelName == null) {
            throw new ObjectStoreException("No 'model' property specified for OJB"
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
                instances.put(db, new ObjectStoreOjbImpl(db, modelName));
            }
        }
        return (ObjectStoreOjbImpl) instances.get(db);
    }

    /**
     * Execute a Query on this ObjectStore
     *
     * @param q the Query to execute
     * @return the results of the Query
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public Results execute(Query q) throws ObjectStoreException {
        return new Results(q, this);
    }

    /**
     * Execute a Query on this ObjectStore, asking for a certain range of rows to be returned.
     * This will usually only be called by the Results object returned from execute().
     * <code>execute(Query q)</code>.
     *
     * @param q the Query to execute
     * @param start the first row to return, numbered from zero
     * @param limit the maximum number of rows to return
     * @return a List of ResultRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int limit) throws ObjectStoreException {
        checkStartLimit(start, limit);

        PersistenceBrokerFlyMine pb = pbf.createPersistenceBroker(db, model);
        ExplainResult explain = pb.explain(q, start, limit);

        if (explain.getTime() > maxTime) {
            throw (new ObjectStoreException("Estimated time to run query(" + explain.getTime()
                                            + ") greater than permitted maximum ("
                                            + maxTime + ")"));
        }

        List res = pb.execute(q, start, limit);
        for (int i = 0; i < res.size(); i++) {
            res.set(i, new ResultsRow(Arrays.asList((Object[]) res.get(i))));
        }
        pb.close();
        return res;
    }

    /**
     * @see ObjectStore#getObjectByExample
     */
    public Object getObjectByExample(Object obj) throws ObjectStoreException {
        Model model;
        try {
            model = Model.getInstance();
            if (model == null) {
                throw new MetaDataException();
            }
        } catch (MetaDataException e) {
            throw new ObjectStoreException(e);
        }
        Results res = execute(QueryHelper.createQueryForExampleObject(obj, model));
        
        if (res.size() > 1) {
            throw new IllegalArgumentException("More than one object in the database has "
                                               + "this primary key");
        }
        if (res.size() == 1) {
            Object ret = ((ResultsRow) res.get(0)).get(0);
            return ret;
        }
        return null;
    }

    /**
     * Runs an EXPLAIN on the query without and LIMIT or OFFSET.
     *
     * @param q the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explining the query
     */
    public ExplainResult estimate(Query q) throws ObjectStoreException {
        return explain(q, 0, Integer.MAX_VALUE - 1);
    }

    /**
     * Runs an EXPLAIN for the given query with specified start and limit parameters.  This
     * gives estimated time for a single 'page' of the query.
     *
     * @param q the query to explain
     * @param start first row required, numbered from zero
     * @param limit the maximum number of rows to return
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explining the query
     */
    public ExplainResult estimate(Query q, int start, int limit) throws ObjectStoreException {
        return explain(q, start, limit);
    }

    private ExplainResult explain(Query q, int start, int limit) throws ObjectStoreException {
        PersistenceBrokerFlyMine pb = pbf.createPersistenceBroker(db, model);
        ExplainResult result = pb.explain(q, start, limit);
        pb.close();
        return result;
    }


    /**
     * Execute a COUNT(*) on a query, returns the number of row the query will produce
     *
     * @param q Flymine Query on which to run COUNT(*)
     * @return the number of row to be produced by query
     */
    public int count(Query q) {
        PersistenceBrokerFlyMine pb = pbf.createPersistenceBroker(db, model);
        int count = pb.count(q);
        pb.close();
        return count;
    }

}

