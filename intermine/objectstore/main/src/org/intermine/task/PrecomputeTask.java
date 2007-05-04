package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.PathQueryUtil;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryOrderable;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.iql.IqlQuery;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * A Task that reads a list of queries from a properties file (eg. testmodel_precompute.properties)
 * and calls ObjectStoreInterMineImpl.precompute() using the Query.
 *
 * @author Kim Rutherford
 */

public class PrecomputeTask extends Task
{
    private static final Logger LOG = Logger.getLogger(PrecomputeTask.class);

    protected String alias;
    protected boolean testMode;
    protected int minRows = -1;
    // set by readProperties()
    protected Properties precomputeProperties = null;
    protected ObjectStore os = null;
    private static final String TEST_QUERY_PREFIX = "test.query.";
    // private boolean selectAllFields = true;   // put all available fields on the select list
    private boolean createAllOrders = false;  // create same table with all possible orders

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

        try {
            os = ObjectStoreFactory.getObjectStore(alias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }

        if (!(os instanceof ObjectStoreInterMineImpl)) {
            throw new BuildException(alias + " isn't an ObjectStoreInterMineImpl");
        }

        precomputeModel();
    }


    /**
     * Create precomputed tables for the given ObjectStore.  This method is also called from
     * PrecomputeTaskTest.
     */
    protected void precomputeModel() {
        readProperties();

        if (testMode) {
            PrintStream outputStream = System.out;
            outputStream.println("Starting tests");
            // run and ignore so that the results are cached for the next test
            runTestQueries();

            long start = System.currentTimeMillis();
            outputStream.println("Running tests before precomputing");
            runTestQueries();
            outputStream.println("tests took: " + (System.currentTimeMillis() - start) / 1000
                                 + " seconds");
        }

        Map pq = getPrecomputeQueries();
        LOG.info("pq.size(): " + pq.size());
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
                    LOG.info("precomputing " + key + " - " + query);
                    Collection indexes = new ArrayList();
                    Iterator qoListIter = query.getSelect().iterator();
                    while (qoListIter.hasNext()) {
                        QuerySelectable qo = (QuerySelectable) qoListIter.next();
                        if (qo instanceof QueryOrderable) {
                            indexes.add(qo);
                        }
                    }
                    precompute(query, indexes);

                    if (testMode) {
                        PrintStream outputStream = System.out;
                        long start = System.currentTimeMillis();
                        outputStream.println("Running tests after precomputing " + key + ": "
                                             + query);
                        runTestQueries();
                        outputStream.println("tests took: "
                                             + (System.currentTimeMillis() - start) / 1000
                                             + " seconds");
                    }
                }
            }
        }

        if (testMode) {
            PrintStream outputStream = System.out;
            long start = System.currentTimeMillis();
            outputStream.println("Running tests after all precomputes");
            runTestQueries();
            outputStream.println("tests took: "
                                 + (System.currentTimeMillis() - start) / 1000
                                 + " seconds");
        }
    }


    /**
     * Call ObjectStoreInterMineImpl.precompute() with the given Query.
     * @param query the query to precompute
     * @param indexes the index QueryNodes
     * @throws BuildException if the query cannot be precomputed.
     */
    protected void precompute(Query query, Collection indexes)
        throws BuildException {
        long start = System.currentTimeMillis();

        try {
            ((ObjectStoreInterMineImpl) os).precompute(query, indexes, true, "PrecomputeTask");
        } catch (ObjectStoreException e) {
            throw new BuildException("Exception while precomputing query: " + query
                    + " with indexes " + indexes, e);
        }

        LOG.info("precompute(indexes) of took "
                 + (System.currentTimeMillis() - start) / 1000
                 + " seconds for: " + query);
    }

    /**
     * Get a Map of keys (from the precomputeProperties file) to Query objects to precompute.
     * have no objects
     * @return a Map of keys to Query objects
     * @throws BuildException if the query cannot be constructed (for example when a class or the
     * collection doesn't exist
     */
    protected Map getPrecomputeQueries() throws BuildException {
        Map returnMap = new TreeMap();

        // TODO - read selectAllFields and createAllOrders from properties

        // TODO - property to not create empty tables

        Iterator iter = new TreeSet(precomputeProperties.keySet()).iterator();

        while (iter.hasNext()) {
            String precomputeKey = (String) iter.next();

            String value = (String) precomputeProperties.get(precomputeKey);

            if (precomputeKey.startsWith("precompute.query")) {
                String iqlQueryString = value;
                Query query = parseQuery(iqlQueryString, precomputeKey);
                List list = new ArrayList();
                list.add(query);
                returnMap.put(precomputeKey, list);
            } else {
                if (precomputeKey.startsWith("precompute.constructquery")) {
                    try {
                        List constructedQueries = constructQueries(value, createAllOrders);
                        returnMap.put(precomputeKey, constructedQueries);
                    } catch (Exception e) {
                        throw new BuildException(e);
                    }
                } else {
                    if (!precomputeKey.startsWith(TEST_QUERY_PREFIX)) {
                        throw new BuildException("unknown key: '" + precomputeKey
                                                 + "' in properties file "
                                                 + getPropertiesFileName());
                    }
                }
            }
        }
        return returnMap;
    }



    /**
     * Create queries for given path.  If path has a '+' next to any class then
     * expand to include all subclasses.
     * @param path the path to construct a query for
     * @param createAllOrdersFlag if true then create a query for all possible orders of QueryClass
     * objects on the from list of the query
     * @return a list of queries
     * @throws ClassNotFoundException if problem processing path
     * @throws IllegalArgumentException if problem processing path
     */
    protected List constructQueries(String path, boolean createAllOrdersFlag)
        throws ClassNotFoundException, IllegalArgumentException {

        List queries = new ArrayList();

        // expand '+' to all subclasses in path
        Set paths = PathQueryUtil.expandPath(os.getModel(), path);
        Iterator pathIter = paths.iterator();
        while (pathIter.hasNext()) {
            String nextPath = (String) pathIter.next();
            Query q = PathQueryUtil.constructQuery(os.getModel(), nextPath);
            if (createAllOrdersFlag) {
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
     * @param q the Query
     * @return clones of the Query with all permutations of orderBy
     */
    protected List getOrderedQueries(Query q) {
        List queryList = new ArrayList();

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
     * Add QueryFields for each of the field names in fieldNames to the given Query.
     * @param q the Query to add to
     * @param qc the QueryClass that the QueryFields should be created for
     * @param fieldNames the field names to create QueryFields for
     */
    protected void addFieldsToQuery(Query q, QueryClass qc, List fieldNames) {
        Iterator fieldNameIter = fieldNames.iterator();

        while (fieldNameIter.hasNext()) {
            String fieldName = (String) fieldNameIter.next();
            if (fieldName.equals("id")) {
                continue;
            }
            QueryField qf = new QueryField(qc, fieldName);
            q.addToSelect(qf);
        }
    }

    /**
     * For a given IQL query, return a Query object.
     * @param iqlQueryString the IQL String
     * @param key the key from the properties file
     * @return a Query object
     * @throws BuildException if the IQL String cannot be parsed.
     */
    protected Query parseQuery(String iqlQueryString, String key) throws BuildException {
        IqlQuery iqlQuery = new IqlQuery(iqlQueryString, os.getModel().getPackageName());

        try {
            return iqlQuery.toQuery();
        } catch (IllegalArgumentException e) {
            throw new BuildException("Exception while parsing query: " + key
                                     + " = " + iqlQueryString, e);
        }
    }

    /**
     * Run all the test queries specified in precomputeProperties.
     * @throws BuildException if there is an error while running the queries.
     */
    protected void runTestQueries() throws BuildException {
        TreeMap sortedPrecomputeProperties = new TreeMap(precomputeProperties);
        Iterator iter = sortedPrecomputeProperties.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            String testqueryKey = (String) entry.getKey();
            if (testqueryKey.startsWith(TEST_QUERY_PREFIX)) {
                String iqlQueryString = (String) entry.getValue();
                Query query = parseQuery(iqlQueryString, testqueryKey);

                long start = System.currentTimeMillis();
                PrintStream outputStream = System.out;
                outputStream.println("  running test " + testqueryKey + ":");
                Results results;
                results = os.execute(query);
                int resultsSize = results.size();
                outputStream.println("  got size " + resultsSize + " in "
                                   + (System.currentTimeMillis() - start) / 1000 + " seconds");
                if (resultsSize > 0) {
                    start = System.currentTimeMillis();
                    outputStream.println("  first row in "
                                         + (System.currentTimeMillis() - start) / 1000
                                         + " seconds");
                    start = System.currentTimeMillis();
                    outputStream.println("  last row in "
                                         + (System.currentTimeMillis() - start) / 1000
                                         + " seconds");
                }
            }
        }
    }


    /**
     * Set precomputeProperties by reading from propertiesFileName.
     * @throws BuildException if the file cannot be read.
     */
    protected void readProperties() throws BuildException {
        String propertiesFileName = getPropertiesFileName();

        try {
            InputStream is =
                PrecomputeTask.class.getClassLoader().getResourceAsStream(propertiesFileName);

            if (is == null) {
                throw new BuildException("Cannot find " + propertiesFileName
                                         + " in the class path");
            }

            precomputeProperties = new Properties();
            precomputeProperties.load(is);
        } catch (IOException e) {
            throw new BuildException("Exception while reading properties from "
                                     + propertiesFileName , e);
        }
    }

    /**
     * Return the name of the properties file that passed to the constructor.
     * @return the name of the properties file that passed to the constructor.
     */
    protected String getPropertiesFileName() {
        return os.getModel().getName() + "_precompute.properties";
    }


    /**
     * Given an integer number, n, return a Set of int arrays with all permutations
     * of numbers 0 to n.
     * @param n number of entities in ordered arrays
     * @return a set of int arrays
     */
    protected Set permutations(int n) {
        Set result = new LinkedHashSet();
        int[] array = new int[n];

        for (int i = 0; i < n; i++) {
            array[i] = i;
        }
        enumerate(result, array, n);
        return result;
    }

    private void swap(int[] array, int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    private void enumerate(Set result, int[] array, int n) {
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
}
