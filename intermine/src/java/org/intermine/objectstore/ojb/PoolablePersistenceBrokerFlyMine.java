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

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.singlevm.PoolablePersistenceBroker;

import org.flymine.objectstore.query.Query;
import org.flymine.sql.Database;
import org.flymine.sql.query.ExplainResult;

/**
 * Extension of PoolablePersistenceBroker to implement PersistenceBrokerFlyMine
 *
 * @author Mark Woodbridge
 */
class PoolablePersistenceBrokerFlyMine extends PoolablePersistenceBroker
    implements PersistenceBrokerFlyMine
{
    /**
     * @see PoolablePersistenceBroker#PoolablePersistenceBroker
     */
    public PoolablePersistenceBrokerFlyMine(PersistenceBroker broker, KeyedObjectPool pool) {
        super(broker, pool);
    }

    /**
     * @see PersistenceBrokerFlyMine#execute
     */
    public List execute(Query query, int start, int limit) {
        return ((PersistenceBrokerFlyMine) getBroker()).execute(query, start, limit);
    }

    /**
     *  @see PersistenceBrokerFlyMine#explain
     */
    public ExplainResult explain(Query query, int start, int limit) {
        return ((PersistenceBrokerFlyMine) getBroker()).explain(query, start, limit);
    }

    /**
     * @see PersistenceBrokerFlyMine#count
     */
    public int count(Query query) {
        return ((PersistenceBrokerFlyMine) getBroker()).count(query);
    }
    
    /**
     * @see PersistenceBrokerFlyMine#setDatabase
     */
    public void setDatabase(Database db) {
        ((PersistenceBrokerFlyMine) getBroker()).setDatabase(db);
    }
    
    /**
     * @see PersistenceBrokerFlyMine#getDatabase
     */
    public Database getDatabase() {
        return ((PersistenceBrokerFlyMine) getBroker()).getDatabase();
    }
}
