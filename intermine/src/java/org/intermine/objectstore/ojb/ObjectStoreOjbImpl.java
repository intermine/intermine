package org.flymine.objectstore.ojb;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.ojb.broker.*;
import org.apache.ojb.broker.metadata.*;

import org.flymine.sql.Database;
import org.flymine.objectstore.*;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;

/**
 * Implementation of ObjectStore that uses OJB as its underlying store.
 *
 * @author Andrew Varley
 */
public class ObjectStoreOjbImpl implements ObjectStore
{
    protected static Map instances = new HashMap();
    protected PersistenceBrokerFlyMineImpl pb = null;

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
    }

    /**
     * Gets the PersistenceBroker used by this
     * ObjectStoreOjbImpl. Probably only useful for testing purposes.
     *
     * @return the PersistenceBroker this object is using
     */
    protected PersistenceBroker getPersistenceBroker() {
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
     * @param start the start row
     * @param end the end row
     * @return a List of ResultRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int end) throws ObjectStoreException {
        return execute(q, start, end);
    }

}
