package org.intermine.objectstore.safe;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;

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
    @Override
    public Results execute(Query q) {
        return os.execute(QueryCloner.cloneQuery(q));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Results execute(Query q, int batchSize, boolean optimise, boolean explain,
            boolean prefetch) {
        Results retval = new Results(q, this, getSequence(getComponentsForQuery(q)));
        if (batchSize != 0) {
            retval.setBatchSize(batchSize);
        }
        if (!optimise) {
            retval.setNoOptimise();
        }
        if (!explain) {
            retval.setNoExplain();
        }
        if (!prefetch) {
            retval.setNoPrefetch();
        }
        retval.setImmutable();
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingletonResults executeSingleton(Query q) {
        return os.executeSingleton(QueryCloner.cloneQuery(q));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingletonResults executeSingleton(Query q, int batchSize, boolean optimise,
            boolean explain, boolean prefetch) {
        SingletonResults retval = new SingletonResults(q, this,
                getSequence(getComponentsForQuery(q)));
        if (batchSize != 0) {
            retval.setBatchSize(batchSize);
        }
        if (!optimise) {
            retval.setNoOptimise();
        }
        if (!explain) {
            retval.setNoExplain();
        }
        if (!prefetch) {
            retval.setNoPrefetch();
        }
        retval.setImmutable();
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResultsRow<Object>> execute(Query q, int start, int limit, boolean optimise,
            boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {
        return os.execute(QueryCloner.cloneQuery(q), start, limit, optimise, explain, sequence);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return os.estimate(QueryCloner.cloneQuery(q));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int count(Query q, Map<Object, Integer> sequence) throws ObjectStoreException {
        return os.count(QueryCloner.cloneQuery(q), sequence);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Safe(" + os + ")";
    }
}
