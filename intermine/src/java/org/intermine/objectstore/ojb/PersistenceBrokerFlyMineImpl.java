package org.flymine.objectstore.ojb;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.singlevm.PersistenceBrokerImpl;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

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
     * @return a list of ResultsRows
     */
    public List execute(Query query, int start, int end) {
        List results = new ArrayList();
        //TODO pass start and end thru somehow
        Iterator iter = new MultiObjectRsIterator(query, this);
        while (iter.hasNext()) { // if iterator is of the right length...
            //TODO uncomment this
            //results.add(iter.next());
            results.add(new Object[] {});
        }
        return results;
    }
}
