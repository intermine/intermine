package org.flymine.objectstore.safe;

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

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStorePassthruImpl;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryCloner;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsInfo;

/**
 * Provides a safe implementation of an objectstore - that is, an implementation that works
 * correctly if passed a Query object that has been modified since being used previously.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreSafeImpl extends ObjectStorePassthruImpl
{
    /**
     * Creates an instance, from another ObjectStore instance.
     *
     * @param os an ObjectStore object to use
     */
    public ObjectStoreSafeImpl(ObjectStore os) {
        super(os);
    }

    /**
     * @see ObjectStore#execute(Query)
     */
    public Results execute(Query q) throws ObjectStoreException {
        return os.execute(QueryCloner.cloneQuery(q));
    }

    /**
     * @see ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        return os.execute(QueryCloner.cloneQuery(q), start, limit, optimise, explain, sequence);
    }

    /**
     * @see ObjectStore#estimate
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return os.estimate(QueryCloner.cloneQuery(q));
    }

    /**
     * @see ObjectStore#count
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        return os.count(QueryCloner.cloneQuery(q), sequence);
    }
}
