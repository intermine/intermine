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

import java.util.Map;
import java.util.HashMap;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryDefaultImpl;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;

import org.flymine.sql.Database;

import org.apache.log4j.Logger;

/**
 * Extension of PersistenceBrokerFactoryDefaultImpl to return the right PersistenceBroker
 *
 * @author Mark Woodbridge
 */
public class PersistenceBrokerFactoryFlyMineImpl extends PersistenceBrokerFactoryDefaultImpl
{
    protected static final Logger LOG = Logger
        .getLogger(PersistenceBrokerFactoryFlyMineImpl.class);

    private Map pbKeys = new HashMap();

    /**
     * Create a FlyMine persistence broker from a database object
     *
     * @param model the name of the model
     * @param db the database object
     * @return a FlyMine PersistenceBroker
     */
    public PersistenceBrokerFlyMine createPersistenceBroker(Database db, String model) {
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

            String repositoryFile = "repository_" + model + ".xml";
            DescriptorRepository dr = mdm.readDescriptorRepository(repositoryFile);
            mdm.mergeDescriptorRepository(dr);

            key = new PBKey(alias, db.getUser(), db.getPassword());
            pbKeys.put(db, key);
        }

        PersistenceBroker pb = createPersistenceBroker(key);
        
        if (!(pb instanceof PersistenceBrokerFlyMine)) {
            throw new IllegalArgumentException("PersistenceBrokerClass is not set to "
                                               + "PersistenceBrokerFlyMineImpl in OJB.properties");
        }
        ((PersistenceBrokerFlyMine) pb).setDatabase(db);

        LOG.debug(activePersistenceBroker() + " active brokers");

        return (PersistenceBrokerFlyMine) pb;
    }
}
