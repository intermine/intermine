package org.flymine.objectstore.ojb;

import java.util.Map;
import java.util.HashMap;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryDefaultImpl;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
//import org.apache.ojb.broker.metadata.DescriptorRepository;

import org.flymine.sql.Database;

/**
 * Extension of PersistenceBrokerFactoryDefaultImpl to return the right PersistenceBroker
 *
 * @author Mark Woodbridge
 */
public class PersistenceBrokerFactoryFlyMineImpl extends PersistenceBrokerFactoryDefaultImpl
{
    private Map pbKeys = new HashMap();

    /**
     * This version of createPersistenceBroker forces use of OJB.properties
     * @see PersistenceBrokerFactoryIF#createPersistenceBroker
     */
    public PersistenceBroker createPersistenceBroker(PBKey pbKey) throws PBFactoryException {
        return createNewBrokerInstance(pbKey);
    }

    /**
     * Create a FlyMine persistence broker from a database object
     *
     * @param db the database object
     * @return a FlyMine PersistenceBroker
     * @throws PBFactoryException if there is a problem creating the broker
     */
    public PersistenceBrokerFlyMineImpl createPersistenceBroker(Database db)
        throws PBFactoryException {
        if (db == null) {
            throw new PBFactoryException("Database is null");
        }

        PBKey key = (PBKey) pbKeys.get(db);
        if (key == null) {
            MetadataManager mdm = MetadataManager.getInstance();
            ConnectionRepository cr = mdm.connectionRepository();
            JdbcConnectionDescriptor jcd = new JdbcConnectionDescriptor();
            
            String alias = "jcdAlias";
            
            jcd.setJcdAlias(alias);
            
            jcd.setDriver(db.getDriver());
            jcd.setDbms(db.getPlatform());
            
            String[] s = db.getURL().split(":");
            jcd.setProtocol(s[0]);
            jcd.setSubProtocol(s[1]);
            jcd.setDbAlias(s[2]);
            
            cr.addDescriptor(jcd);

//         String repositoryFile = "repository_" + db.getModel() + ".xml";
//         DescriptorRepository dr = mdm.readDescriptorRepository(repositoryFile);
//         mdm.mergeDescriptorRepository(dr);

            key = new PBKey(alias, db.getUser(), db.getPassword());
            pbKeys.put(db, key);
        }

        PersistenceBroker pb = createPersistenceBroker(key);
        
        if (!(pb instanceof PersistenceBrokerFlyMineImpl)) {
            throw new IllegalArgumentException("PersistenceBrokerClass is not set to "
                                               + "PersistenceBrokerFlyMineImpl in OJB.properties");
        }
        ((PersistenceBrokerFlyMineImpl) pb).setDatabase(db);
        return (PersistenceBrokerFlyMineImpl) pb;
    }
}
