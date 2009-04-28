package org.intermine.task;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.querygen.QueryGenUtil;
import org.intermine.util.SynchronisedIterator;

/**
 * A Task that reads a list of queries from a properties file (eg. testmodel_precompute.properties)
 * and calls ObjectStoreInterMineImpl.precompute() using the Query.
 *
 * @author Kim Rutherford
 */

public class PrecomputeTask extends Task
{
    private static final Logger LOG = Logger.getLogger(PrecomputeTask.class);
    private static final int THREAD_COUNT = 4;

    protected String alias;
    protected int minRows = -1;
    // set by readProperties()

    // used only by PrecomputeTaskTest
    protected static List<Query> testQueries = new ArrayList<Query>();
    private static boolean testMode = false;

    /**
     * Set the ObjectStore alias
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set the minimum row count for precomputed queries.  Queries that are estimated to have less
     * than this number of rows will not be precomputed.
     * @param minRows the minimum row count
     */
    public void setMinRows(Integer minRows) {
        this.minRows = minRows.intValue();
    }

    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        if (minRows == -1) {
            throw new BuildException("minRows attribute is not set");
        }

        ObjectStore os;

        try {
            os = ObjectStoreFactory.getObjectStore(alias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }

        if (!(os instanceof ObjectStoreInterMineImpl)) {
            throw new BuildException(alias + " isn't an ObjectStoreInterMineImpl");
        }

        precompute(false, os, minRows);
    }


    /**
     * Create precomputed tables for the given ObjectStore.  This method is also called from
     * PrecomputeTaskTest.
     * @param createAllOrders if true construct all permutations of order by for the QueryClass
     *   objects on the from list
     * @param os The ObjectStore to add precomputed tables to
     * @param minRows don't create any precomputed tables with less than this many rows
     */
    public static void precompute(boolean createAllOrders, ObjectStore os, int minRows) {
        Properties properties = readProperties(os.getModel().getName());

        Map pq = getPrecomputeQueries(createAllOrders, os.getModel(), properties);
        LOG.info("pq.size(): " + pq.size());
        Set<Job> jobs = new TreeSet<Job>();
        Iterator iter = pq.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();

            List queries = (List) entry.getValue();
            LOG.debug("queries: " + queries.size());
            Iterator queriesIter = queries.iterator();
            while (queriesIter.hasNext()) {
                Query query = (Query) queriesIter.next();

                LOG.info("key: " + key);

                ResultsInfo resultsInfo;

                try {
                    resultsInfo = os.estimate(query);
                } catch (ObjectStoreException e) {
                    throw new BuildException("Exception while calling ObjectStore.estimate()", e);
                }

                if (resultsInfo.getRows() >= minRows) {
                    LOG.info("Will precompute " + key + " - " + query);
                    jobs.add(new Job(os, key, query, resultsInfo.getComplete()));
                }
            }
        }

        Iterator<Job> jobIter = new SynchronisedIterator<Job>(jobs.iterator());
        Map<Integer, String> threads = new TreeMap<Integer, String>();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());

        synchronized (threads) {
            for (int i = 1; i < THREAD_COUNT; i++) {
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
                job.execute(0);
                if (!exceptions.isEmpty()) {
                    throw new BuildException("Exception while executing in worker thread",
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
            throw new BuildException("Exception while executing in worker thread",
                    exceptions.get(0));
        }
        LOG.info("All threads finished");
    }

    private static class Job implements Comparable<Job>
    {
        private ObjectStore os;
        private String key;
        private Query query;
        private long time;

        public Job(ObjectStore os, String key, Query query, long time) {
            this.os = os;
            this.key = key;
            this.query = query;
            this.time = time;
        }

        public void execute(int threadNo) throws BuildException {
            LOG.info("Thread " + threadNo + " precomputing " + key + " - " + query);
            precomputeQuery(os, query);
        }

        public String getKey() {
            return key;
        }

        public int compareTo(Job job) {
            return (job.time > time ? 1 : (job.time < time ? -1
                        : query.toString().compareTo(job.query.toString())));
        }
    }

    private static class Worker implements Runnable
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
                    job.execute(threadNo);
                }
            } catch (NoSuchElementException e) {
                // Empty
            } catch (Exception e) {
                // Something has gone wrong.
                exceptions.add(e);
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
     * Call ObjectStoreInterMineImpl.precompute() with the given Query.
     *
     * @param os the ObjectStore to precompute
     * @param query the query to precompute
     * @param indexes the index QueryNodes
     * @throws BuildException if the query cannot be precomputed.
     */
    private static void precomputeQuery(ObjectStore os, Query query) throws BuildException {
        if (testMode) {
            testQueries.add(query);
        }

        long start = System.currentTimeMillis();

        try {
            ((ObjectStoreInterMineImpl) os).precompute(query, null, true, "PrecomputeTask");
        } catch (ObjectStoreException e) {
            throw new BuildException("Exception while precomputing query: " + query, e);
        }

        LOG.info("Precompute took "
                 + (System.currentTimeMillis() - start) / 1000
                 + " seconds for: " + query);
    }

    /**
     * Get a Map of keys (from the precomputeProperties file) to Query objects to precompute.
     *
     * @param createAllOrders if true construct all permutations of order by for the QueryClass
     *   objects on the from list
     * @param model the Model
     * @param precomputeProperties the properties specifying which queries to precompute
     * @return a Map of keys to Query objects
     * @throws BuildException if the query cannot be constructed (for example when a class or the
     * collection doesn't exist
     */
    private static Map<String, List<Query>> getPrecomputeQueries(boolean createAllOrders,
            Model model, Properties precomputeProperties) throws BuildException {
        Map<String, List<Query>> returnMap = new TreeMap<String, List<Query>>();

        // TODO - read selectAllFields and createAllOrders from properties

        // TODO - property to not create empty tables

        Iterator iter = new TreeSet(precomputeProperties.keySet()).iterator();

        while (iter.hasNext()) {
            String precomputeKey = (String) iter.next();

            String value = (String) precomputeProperties.get(precomputeKey);

            if (precomputeKey.startsWith("precompute.query")) {
                String iqlQueryString = value;
                Query query = parseQuery(model, iqlQueryString, precomputeKey);
                List<Query> list = new ArrayList<Query>();
                list.add(query);
                returnMap.put(precomputeKey, list);
            } else {
                if (precomputeKey.startsWith("precompute.constructquery")) {
                    try {
                        List<Query> constructedQueries =
                            constructQueries(createAllOrders, model, value);
                        returnMap.put(precomputeKey, constructedQueries);
                    } catch (Exception e) {
                        throw new BuildException(e);
                    }
                } else {
                    throw new BuildException("unknown key: '" + precomputeKey
                            + "' in properties file " + getPropertiesFileName(model.getName()));
                }
            }
        }
        return returnMap;
    }

    /**
     * Create queries for given path.  If path has a '+' next to any class then
     * expand to include all subclasses.
     *
     * @param createAllOrders if true construct all permutations of order by for the QueryClass
     *   objects on the from list
     * @param model the Model
     * @param path the path to construct a query for
     * @return a list of queries
     * @throws ClassNotFoundException if problem processing path
     * @throws IllegalArgumentException if problem processing path
     */
    protected static List<Query> constructQueries(boolean createAllOrders, Model model, String path)
        throws ClassNotFoundException, IllegalArgumentException {

        List<Query> queries = new ArrayList<Query>();

        // expand '+' to all subclasses in path
        Set paths = QueryGenUtil.expandPath(model, path);
        Iterator pathIter = paths.iterator();
        while (pathIter.hasNext()) {
            String nextPath = (String) pathIter.next();
            Query q = QueryGenUtil.constructQuery(model, nextPath);
            if (createAllOrders) {
                queries.addAll(getOrderedQueries(q));
            } else {
                queries.add(q);
            }
        }
        return queries;
    }


    /**
     * Return a List containing clones of the given Query, but with all permutations
     * of order by for the QueryClass objects on the from list.
     *
     * @param q the Query
     * @return clones of the Query with all permutations of orderBy
     */
    private static List<Query> getOrderedQueries(Query q) {
        List<Query> queryList = new ArrayList<Query>();

        Set permutations = permutations(q.getOrderBy().size());
        Iterator iter = permutations.iterator();
        while (iter.hasNext()) {
            Query newQuery = QueryCloner.cloneQuery(q);
            List orderBy = new ArrayList(newQuery.getOrderBy());
            newQuery.clearOrderBy();

            int[] order = (int[]) iter.next();
            for (int i = 0; i < order.length; i++) {
                newQuery.addToOrderBy((QueryClass) orderBy.get(order[i]));
            }

            queryList.add(newQuery);
        }
        return queryList;
    }


    /**
     * For a given IQL query, return a Query object.
     * @param iqlQueryString the IQL String
     * @param key the key from the properties file
     * @param model the Model
     * @return a Query object
     * @throws BuildException if the IQL String cannot be parsed.
     */
    private static Query parseQuery(Model model, String iqlQueryString, String key)
        throws BuildException {
        IqlQuery iqlQuery = new IqlQuery(iqlQueryString, model.getPackageName());

        try {
            return iqlQuery.toQuery();
        } catch (IllegalArgumentException e) {
            throw new BuildException("Exception while parsing query: " + key
                                     + " = " + iqlQueryString, e);
        }
    }



    /**
     * Set precomputeProperties by reading from propertiesFileName.
     * @param modelName the model name
     * @return the Properties
     * @throws BuildException if the file cannot be read.
     */
    private static Properties readProperties(String modelName) throws BuildException {
        String propertiesFileName = getPropertiesFileName(modelName);

        try {
            InputStream is =
                PrecomputeTask.class.getClassLoader().getResourceAsStream(propertiesFileName);

            if (is == null) {
                throw new BuildException("Cannot find " + propertiesFileName
                                         + " in the class path");
            }

            Properties precomputeProperties = new Properties();
            precomputeProperties.load(is);
            return precomputeProperties;
        } catch (IOException e) {
            throw new BuildException("Exception while reading properties from "
                                     + propertiesFileName , e);
        }
    }

    /**
     * Return the name of the properties file we will read.
     * @param modelName the model name
     * @return the properties file name
     */
    protected static String getPropertiesFileName(String modelName) {
        return modelName + "_precompute.properties";
    }


    /**
     * Given an integer number, n, return a Set of int arrays with all permutations
     * of numbers 0 to n.
     * @param n number of entities in ordered arrays
     * @return a set of int arrays
     * Put the class in test mode.  Used by PrecomputeTaskTest.
     */
    private static Set permutations(int n) {
        Set result = new LinkedHashSet();
        int[] array = new int[n];

        for (int i = 0; i < n; i++) {
            array[i] = i;
        }
        enumerate(result, array, n);
        return result;
    }

    private static void swap(int[] array, int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    private static void enumerate(Set result, int[] array, int n) {
        if (n == 1) {
            int[] copy = new int[array.length];
            System.arraycopy(array, 0, copy, 0, array.length);
            result.add(copy);
            return;
        }
        for (int i = 0; i < n; i++) {
            swap(array, i, n - 1);
            enumerate(result, array, n - 1);
            swap(array, i, n - 1);
        }
    }

    /**
     * Put the class in test mode.  Used by PrecomputeTaskTest.
     */
    static void setTestMode() {
        testMode  = true;
    }
}
