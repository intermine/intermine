package org.flymine.objectstore.ojb;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.singlevm.PersistenceBrokerImpl;
import java.util.Collection;

import org.flymine.objectstore.query.Query;

/**
 * Extension of PersistenceBrokerImpl to allow execution of ObjectStore queries
 *
 * @author Mark Woodbridge
 */
public class PersistenceBrokerFlyMineImpl extends PersistenceBrokerImpl
{
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
     * Executes a query with start and end result indices
     *
     * @param query the ObjectStore query
     * @param start start index
     * @param end end index
     * @return a collection of ResultsRows
     */
    public Collection getCollectionByQuery(Query query, int start, int end) {
        return (Collection) new java.util.ArrayList();
    }
}
