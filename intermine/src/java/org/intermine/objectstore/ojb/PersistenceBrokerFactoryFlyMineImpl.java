package org.flymine.objectstore.ojb;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryDefaultImpl;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PBFactoryException;

/**
 * Extension of PersistenceBrokerFactoryDefaultImpl to return the right PersistenceBroker
 *
 * @author Mark Woodbridge
 */
public class PersistenceBrokerFactoryFlyMineImpl extends PersistenceBrokerFactoryDefaultImpl
{
    /**
     * @see PersistenceBrokerFactoryIF#createPersistenceBroker
     */
    public PersistenceBroker createPersistenceBroker(PBKey pbKey) throws PBFactoryException {
        return createNewBrokerInstance(pbKey);
    }
}
