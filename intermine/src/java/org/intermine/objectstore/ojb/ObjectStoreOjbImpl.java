package org.flymine.objectstore.ojb;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Properties;

import org.apache.ojb.broker.*;
import org.apache.ojb.broker.metadata.*;

import org.flymine.sql.Database;
import org.flymine.sql.query.ExplainResult;
import org.flymine.objectstore.*;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.util.PropertiesUtil;

/**
 * Implementation of ObjectStore that uses OJB as its underlying store.
 *
 * @author Andrew Varley
 */
public class ObjectStoreOjbImpl implements ObjectStore
{
    protected static Map instances = new HashMap();
    protected PersistenceBrokerFlyMineImpl pb = null;
    protected long maxTime;
    protected int maxRows;
    protected int maxLimit;
    protected int maxOffset;

    /**
     * No argument constructor for testing purposes
     *
     */
    protected ObjectStoreOjbImpl() {
    }

    /**
     * Constructs an ObjectStoreOjbImpl interfacing with an OJB instance
     *
     * @param db the database in which the model resides
     * @throws ObjectStoreException if there is any problem with the underlying OJB instance
     * @throws NullPointerException if repository is null
     * @throws IllegalArgumentException if repository is invalid
     */
    protected ObjectStoreOjbImpl(Database db) throws ObjectStoreException {
        if (db == null) {
            throw new NullPointerException("db cannot be null");
        }

        PersistenceBroker pbTemp;
        ConnectionRepository cr = MetadataManager.getInstance().connectionRepository();
        JdbcConnectionDescriptor jcd = new JdbcConnectionDescriptor();

        jcd.setJcdAlias("ObjectStoreOjbImpl");
        jcd.setDefaultConnection(true);

        jcd.setDriver(db.getDriver());
        jcd.setDbms(db.getPlatform());

        String[] s = db.getURL().split(":");
        jcd.setProtocol(s[0]);
        jcd.setSubProtocol(s[1]);
        jcd.setDbAlias(s[2]);

        cr.addDescriptor(jcd);
        PBKey key = new PBKey("ObjectStoreOjbImpl", db.getUser(), db.getPassword());
        pbTemp = PersistenceBrokerFactory.createPersistenceBroker(key);

        if (pbTemp instanceof PersistenceBrokerFlyMineImpl) {
            pb = (PersistenceBrokerFlyMineImpl) pbTemp;
        } else {
            throw new IllegalArgumentException("ObjectStoreOjbImpl requires a "
                                               + "PersistenceBrokerFlyMineImpl to be "
                                               + "returned from PersistenceBrokerFactory");
        }

        Properties props = PropertiesUtil.getPropertiesStartingWith("os.query");
        props = PropertiesUtil.stripStart("os.query", props);
        maxRows = Integer.parseInt((String) props.get("max-rows"));
        maxTime = Long.parseLong((String) props.get("max-time"));
        maxLimit = Integer.parseInt((String) props.get("max-limit"));
        maxOffset = Integer.parseInt((String) props.get("max-offset"));
    }

    /**
     * Gets the PersistenceBroker used by this
     * ObjectStoreOjbImpl. Probably only useful for testing purposes.
     *
     * @return the PersistenceBroker this object is using
     */
    public PersistenceBroker getPersistenceBroker() {
        return pb;
    }

    /**
     * Gets a ObjectStoreOjbImpl instance for the given underlying repository
     *
     * @param db the database in which the model resides
     * @return the ObjectStoreOjbImpl for this repository
     * @throws IllegalArgumentException if repository is invalid
     * @throws ObjectStoreException if there is any problem with the underlying OJB instance
     */
    public static ObjectStoreOjbImpl getInstance(Database db) throws ObjectStoreException {
        synchronized (instances) {
            if (!(instances.containsKey(db))) {
                instances.put(db, new ObjectStoreOjbImpl(db));
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
     * @param end the number of the last row to return, numbered from zero
     * @return a List of ResultRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int end) throws ObjectStoreException {
        // check limit and offset are valid
        int limit = (end - start) + 1;
        if (start > maxOffset) {
            throw (new ObjectStoreException("start parameter (" + start
                                            + ") is greater than permitted maximum ("
                                            + maxOffset + ")"));
        }
        if (limit > maxLimit) {
            throw (new ObjectStoreException("number of rows required (" + limit
                                            + ") is greater than permitted maximum ("
                                            + maxLimit + ")"));
        }

        ExplainResult explain = pb.explain(q, start, end);

        if (explain.getTime() > maxTime) {
            throw (new ObjectStoreException("Estimated time to run query(" + explain.getTime()
                                            + ") greater than permitted maximum ("
                                            + maxTime + ")"));
        }
        if (explain.getRows() > maxRows) {
            throw (new ObjectStoreException("Estimated number of rows (" + explain.getRows()
                                            + ") greater than permitted maximum ("
                                            + maxRows + ")"));
        }

        List res = pb.execute(q, start, limit);
        for (int i = 0; i < res.size(); i++) {
            res.set(i, new ResultsRow(Arrays.asList((Object[]) res.get(i))));
        }
        return res;
    }

    /**
     * Runs an EXPLAIN on the query without ant LIMIT or OFFSET.
     *
     * @param q the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explining the query
     */
    public ExplainResult estimate(Query q) throws ObjectStoreException {
        return pb.explain(q, 0, 0);
    }

    /**
     * Runs an EXPLAIN for the given query with specified start and end parameters.  This
     * gives estimated time for a single 'page' of the query.
     *
     * @param q the query to explain
     * @param start first row required, numbered from zero
     * @param end the number of the last row required, numbered from zero
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explining the query
     */
    public ExplainResult estimate(Query q, int start, int end) throws ObjectStoreException {
        return pb.explain(q, start, end);
    }

}

