package org.intermine.objectstore.intermine;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.util.SynchronisedIterator;

/**
 * An object that can perform a set of precomputes in parallel.
 *
 * @author Matthew Wakeling
 */
public class ParallelPrecomputer
{
    private static final Logger LOG = Logger.getLogger(ParallelPrecomputer.class);

    private int threadCount;
    private ObjectStoreInterMineImpl os;
    private int minRows = -1;

    /**
     * Constructor.
     *
     * @param os an ObjectStoreInterMineImpl to perform the precomputes on
     * @param threadCount the number of operations to perform in parallel
     */
    public ParallelPrecomputer(ObjectStoreInterMineImpl os, int threadCount) {
        this.os = os;
        this.threadCount = threadCount;
    }

    /**
     * Set the minimum row count for precomputed queries.  Queries that are estimated to have less
     * than this number of rows will not be precomputed.
     * @param minRows the minimum row count
     */
    public void setMinRows(int minRows) {
        this.minRows = minRows;
    }

    /**
     * Returns the ObjectStore that this ParallelPrecomputer uses.
     *
     * @return an ObjectStoreInterMineImpl
     */
    public ObjectStoreInterMineImpl getObjectStore() {
        return os;
    }

    /**
     * Perform a load of precompute operations in parallel. Jobs with fewer expected rows than the
     * minRows parameter are not processed. Jobs are processed in decreasing order of expected
     * time taken, which tends to reduce the total time taken.
     *
     * @param jobs a collection of jobs to precompute
     * @throws ObjectStoreException if an error occurs
     */
    public void precompute(Collection<Job> jobs) throws ObjectStoreException {
        TreeSet<Job> todo = new TreeSet<Job>();
        for (Job job : jobs) {
            job.prepare(this);
            if (job.getInfo().getRows() >= minRows) {
                todo.add(job);
            }
        }

        Iterator<Job> jobIter = new SynchronisedIterator<Job>(todo.iterator());
        Map<Integer, String> threads = new TreeMap<Integer, String>();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());

        synchronized (threads) {
            for (int i = 1; i < threadCount; i++) {
                Thread worker = new Thread(new Worker(threads, jobIter, i, exceptions));
                threads.put(new Integer(i), "");
                worker.setName("PrecomputeTask extra thread " + i);
                worker.start();
            }
        }

        try {
            while (jobIter.hasNext()) {
                Job job = jobIter.next();
                synchronized (threads) {
                    threads.put(new Integer(0), job.getKey());
                    LOG.info("Threads doing: " + threads);
                }
                executeJob(job, 0);
                if (!exceptions.isEmpty()) {
                    throw new ObjectStoreException("Exception while executing in worker thread",
                            exceptions.get(0));
                }
            }
        } catch (NoSuchElementException e) {
            // This is fine - just a consequence of concurrent access to the iterator. It means the
            // end of the iterator has been reached, so there is no more work to do.
        }
        LOG.info("Thread 0 finished");
        synchronized (threads) {
            threads.remove(new Integer(0));
            LOG.info("Threads doing: " + threads);
            while (threads.size() != 0) {
                LOG.info(threads.size() + " threads left");
                try {
                    threads.wait();
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
        }
        if (!exceptions.isEmpty()) {
            throw new ObjectStoreException("Exception while executing in worker thread",
                    exceptions.get(0));
        }
        LOG.info("All threads finished");
    }

    /**
     * Class representing a precomputing job to be performed.
     *
     * @author Matthew Wakeling
     */
    public static class Job implements Comparable<Job>
    {
        private String key;
        private Query query;
        private Collection<? extends QueryNode> indexes;
        private boolean allFields;
        private String category;
        private ResultsInfo info;

        /**
         * Constructor - takes the same arguments as ObjectStoreInterMineImpl.precompute().
         *
         * @param key a String that describes this job
         * @param query the Query for which to create the precomputed tables
         * @param indexes a Collection of QueryOrderables for which to create indexes
         * @param allFields true if all fields of QueryClasses in the SELECT list should be
         * included in the precomputed table's SELECT list. If the indexes parameter is null, then
         * indexes will be created for every field as well
         * @param category a String describing the category of the precomputed tables
         */
        public Job(String key, Query query, Collection<? extends QueryNode> indexes,
                boolean allFields, String category) {
            this.key = key;
            this.query = query;
            this.indexes = indexes;
            this.allFields = allFields;
            this.category = category;
            this.info = null;
        }

        /**
         * Prepares the Job by performing an EXPLAIN on the query.
         *
         * @param pp the ParallelPrecomputer that will execute this job
         * @throws ObjectStoreException if an error occurs
         */
        private void prepare(ParallelPrecomputer pp) throws ObjectStoreException {
            info = pp.getObjectStore().estimate(query);
        }

        /**
         * Returns the prepared EXPLAIN for this job.
         *
         * @return a ResultsInfo object
         */
        private ResultsInfo getInfo() {
            return info;
        }

        /**
         * Execute the job.
         *
         * @param pp the ParallelPrecomputer that is performing the operation
         * @param threadNo the number of the thread performing the operation, for logging
         * @throws ObjectStoreException if something goes wrong
         */
        private void execute(ParallelPrecomputer pp, int threadNo) throws ObjectStoreException {
            LOG.info("Job with key " + key + " has expected time " + info.getComplete());
            pp.precomputeQuery(key, query, indexes, allFields, category, threadNo);
        }

        /**
         * Returns the key describing this job.
         *
         * @return a String
         */
        private String getKey() {
            return key;
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo(Job job) {
            long thisTime = info.getComplete();
            long otherTime = job.info.getComplete();
            return (otherTime > thisTime ? 1 : (otherTime < thisTime ? -1
                        : query.toString().compareTo(job.query.toString())));
        }
    }

    private class Worker implements Runnable
    {
        private Map<Integer, String> threads;
        private Iterator<Job> jobIter;
        private int threadNo;
        private List<Exception> exceptions;

        public Worker(Map<Integer, String> threads, Iterator<Job> jobIter, int threadNo,
                List<Exception> exceptions) {
            this.threads = threads;
            this.jobIter = jobIter;
            this.threadNo = threadNo;
            this.exceptions = exceptions;
        }

        public void run() {
            try {
                while (jobIter.hasNext()) {
                    Job job = jobIter.next();
                    synchronized (threads) {
                        threads.put(new Integer(threadNo), job.getKey());
                        LOG.info("Threads doing: " + threads);
                    }
                    try {
                        executeJob(job, threadNo);
                    } catch (Exception e) {
                        // Something has gone wrong.
                        exceptions.add(e);
                    }
                }
            } catch (NoSuchElementException e) {
                // Empty
            } finally {
                LOG.info("Thread " + threadNo + " finished");
                synchronized (threads) {
                    threads.remove(new Integer(threadNo));
                    LOG.info("Threads doing: " + threads);
                    threads.notify();
                }
            }
        }
    }

    /**
     * Executes a job.
     *
     * @param job a Job to execute
     * @param threadNo the number of the executing thread
     * @throws ObjectStoreException if the query cannot be precomputed
     */
    private void executeJob(Job job, int threadNo) throws ObjectStoreException {
        job.execute(this, threadNo);
    }

    /**
     * Call ObjectStoreInterMineImpl.precompute() with the given Query.
     *
     * @param key the String describing the job, for logging
     * @param query the query to precompute
     * @param indexes the index QueryNodes
     * @param allFields whether to include all fields in the precomputed table
     * @param category the category of the precomputed table
     * @param threadNo the thread number, for logging
     * @throws ObjectStoreException if the query cannot be precomputed.
     */
    protected void precomputeQuery(String key, Query query, Collection<? extends QueryNode> indexes,
            boolean allFields, String category, int threadNo) throws ObjectStoreException {
        LOG.info("Thread " + threadNo + " precomputing " + key + " - " + query + " with indexes "
                + indexes);
        long start = System.currentTimeMillis();

        try {
            os.precompute(query, indexes, allFields, category);
        } catch (ObjectStoreException e) {
            LOG.error("Precompute failed for " + key, e);
            throw e;
        }

        LOG.info("Precompute took " + (System.currentTimeMillis() - start) + " ms for: " + key);
    }
}
