package org.flymine.objectstore.ojb;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.singlevm.PersistenceBrokerImpl;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.flymine.objectstore.query.Query;
import org.flymine.sql.query.ExplainResult;
import org.flymine.sql.Database;

/**
 * Extension of PersistenceBrokerImpl to allow execution of ObjectStore queries
 *
 * @author Mark Woodbridge
 */
public class PersistenceBrokerFlyMineImpl extends PersistenceBrokerImpl
{
    private Database database;
    
    /**
     * No argument constructor for testing purposes
     *
     */
    public PersistenceBrokerFlyMineImpl() {
    }

    /**
     * @see PersistenceBrokerImpl#PersistenceBrokerImpl
     */
    public PersistenceBrokerFlyMineImpl(PBKey key, PersistenceBrokerFactoryIF pbf)
    {
        super(key, pbf);
    }

    /**
     * Executes a query with start and limit result indices
     *
     * @param query the ObjectStore query
     * @param start start index
     * @param limit maximum number of rows to return
     * @return a list of ResultsRows
     */
    public List execute(Query query, int start, int limit) {
        List results = new ArrayList();
        Iterator iter = new MultiObjectRsIterator(query, this, start, limit);
        while (iter.hasNext()) { // if iterator is of the right length...
            results.add(iter.next());
        }
        return results;
    }

    /**
     * Runs EXPLAIN on the given query with start and limit result indices
     *
     * @param query the ObjectStore query
     * @param start start index
     * @param limit maximum number of rows to return
     * @return parsed results of the EXPLAIN
     */
    public ExplainResult explain(Query query, int start, int limit) {
        return ((JdbcAccessFlymineImpl) serviceJdbcAccess()).explainQuery(query, start, limit);
    }

    /**
     * Sets the database object that this PersistenceBroker object carries around.
     *
     * @param db the Database object
     */
    public void setDatabase(Database db) {
        database = db;
    }

    /**
     * Gets the database object from this PersistenceBroker object.
     *
     * @return the Database object
     */
    public Database getDatabase() {
        return database;
    }
}
