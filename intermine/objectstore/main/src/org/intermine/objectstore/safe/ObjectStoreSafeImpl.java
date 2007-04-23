package org.intermine.objectstore.safe;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;

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
     * {@inheritDoc}
     */
    public Results execute(Query q) throws ObjectStoreException {
        return os.execute(QueryCloner.cloneQuery(q));
    }

    /**
     * {@inheritDoc}
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        return os.execute(QueryCloner.cloneQuery(q), start, limit, optimise, explain, sequence);
    }

    /**
     * {@inheritDoc}
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return os.estimate(QueryCloner.cloneQuery(q));
    }

    /**
     * {@inheritDoc}
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        return os.count(QueryCloner.cloneQuery(q), sequence);
    }
}
