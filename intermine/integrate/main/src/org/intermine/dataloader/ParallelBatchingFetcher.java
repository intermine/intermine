package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.PrimaryKey;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.ObjectPipe;

import org.apache.log4j.Logger;

/**
 * Class providing EquivalentObjectFetcher functionality that fetches batches of equivalent objects
 * in parallel to improve performance.
 *
 * @author Matthew Wakeling
 */
public class ParallelBatchingFetcher extends BatchingFetcher
{
    private static final Logger LOG = Logger.getLogger(ParallelBatchingFetcher.class);
    private ObjectPipe<WorkUnit> jobs = new ObjectPipe<WorkUnit>();

    /**
     * Constructor
     *
     * @param fetcher another EquivalentObjectFetcher
     * @param dataTracker a DataTracker object to pass prefetch instructions to
     * @param source the data Source that is being loaded
     */
    public ParallelBatchingFetcher(BaseEquivalentObjectFetcher fetcher, DataTracker dataTracker,
            Source source) {
        super(fetcher, dataTracker, source);
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(new Worker(), "ParallelBatchingFetcher Worker " + (i + 1));
            t.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(Source source) {
        jobs.finish();
        LOG.info("Parallel Batching equivalent object query summary for source " + source + " :"
                + getSummary(source).toString() + "\nQueried " + batchQueried
                + " objects by batch, cache misses: " + cacheMisses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPks(Map<PrimaryKey, ClassDescriptor> pksToDo,
            Map<InterMineObject, Set<InterMineObject>> results,
            Map<ClassDescriptor, List<InterMineObject>> cldToObjectsForCld,
            long time1) throws ObjectStoreException {
        Map<PrimaryKey, ClassDescriptor> pksNotDone
            = new IdentityHashMap<PrimaryKey, ClassDescriptor>(pksToDo);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
        synchronized (pksNotDone) {
            while (!pksToDo.isEmpty()) {
                Iterator<PrimaryKey> pkIter = pksToDo.keySet().iterator();
                while (pkIter.hasNext()) {
                    PrimaryKey pk = pkIter.next();
                    ClassDescriptor cld = pksToDo.get(pk);
                    if (canDoPkNow(pk, cld, pksNotDone)) {
                        jobs.put(new WorkUnit(pk, cld, results, cldToObjectsForCld.get(cld),
                                    pksNotDone, exceptions));
                        pkIter.remove();
                    //} else {
                    //    LOG.error("Cannot do pk " + cld.getName() + "." + pk.getName() + " yet");
                    }
                }
                try {
                    pksNotDone.wait();
                } catch (InterruptedException e) {
                }
                if (!exceptions.isEmpty()) {
                    throw new ObjectStoreException("Error in worker thread", exceptions.iterator()
                            .next());
                }
            }
            while (!pksNotDone.isEmpty()) {
                try {
                    pksNotDone.wait();
                } catch (InterruptedException e) {
                }
            }
            if (!exceptions.isEmpty()) {
                throw new ObjectStoreException("Error in worker thread", exceptions.iterator()
                        .next());
            }
        }
        long time2 = System.currentTimeMillis();
        timeSpentPrefetchEquiv += time2 - time1;
    }

    private class WorkUnit
    {
        private PrimaryKey pk;
        private ClassDescriptor cld;
        private Map<InterMineObject, Set<InterMineObject>> results;
        private List<InterMineObject> objectsForCld;
        private Map<PrimaryKey, ClassDescriptor> pksNotDone;
        private List<Exception> exceptions;

        public WorkUnit(PrimaryKey pk, ClassDescriptor cld,
                Map<InterMineObject, Set<InterMineObject>> results,
                List<InterMineObject> objectsForCld,
                Map<PrimaryKey, ClassDescriptor> pksNotDone, List<Exception> exceptions) {
            this.pk = pk;
            this.cld = cld;
            this.results = results;
            this.objectsForCld = objectsForCld;
            this.pksNotDone = pksNotDone;
            this.exceptions = exceptions;
        }

        public void fetch() {
            try {
                Set<Integer> fetchedObjectIds = new HashSet<Integer>();
                doPk(pk, cld, results, objectsForCld, fetchedObjectIds);
                dataTracker.prefetchIds(fetchedObjectIds);
            } catch (Exception e) {
                exceptions.add(e);
            }
            synchronized (pksNotDone) {
                pksNotDone.remove(pk);
                pksNotDone.notify();
            }
        }
    }

    private class Worker implements Runnable
    {
        public Worker() {
        }

        public void run() {
            try {
                while (true) {
                    WorkUnit u = jobs.next();
                    u.fetch();
                }
            } catch (NoSuchElementException e) {
                // Fetcher has been closed
            }
        }
    }
}
