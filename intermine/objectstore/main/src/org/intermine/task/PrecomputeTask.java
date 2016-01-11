package org.intermine.task;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.intermine.objectstore.intermine.ParallelPrecomputer;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.querygen.QueryGenUtil;

/**
 * A Task that reads a list of queries from a properties file (eg. testmodel_precompute.properties)
 * and calls ObjectStoreInterMineImpl.precompute() using the Query.
 *
 * @author Kim Rutherford
 */
public class PrecomputeTask extends Task
{
    private static final Logger LOG = Logger.getLogger(PrecomputeTask.class);
    protected static final int THREAD_COUNT = 4;

    protected String alias;
    protected int minRows = -1;
    // set by readProperties()

    /**
     * Set the ObjectStore alias.
     *
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set the minimum row count for precomputed queries.  Queries that are estimated to have less
     * than this number of rows will not be precomputed.
     *
     * @param minRows the minimum row count
     */
    public void setMinRows(Integer minRows) {
        this.minRows = minRows.intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
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
     * Create precomputed tables for the given ObjectStore.
     *
     * @param createAllOrders if true construct all permutations of order by for the QueryClass
     *   objects on the from list
     * @param os The ObjectStore to add precomputed tables to
     * @param minRows don't create any precomputed tables with less than this many rows
     * @throws BuildException if something goes wrong
     */
    public void precompute(boolean createAllOrders, ObjectStore os, int minRows) {
        Properties properties = readProperties(os.getModel().getName());

        Map<String, List<Query>> pq = getPrecomputeQueries(createAllOrders, os, properties);
        LOG.info("pq.size(): " + pq.size());
        List<ParallelPrecomputer.Job> jobs = new ArrayList<ParallelPrecomputer.Job>();
        for (Map.Entry<String, List<Query>> entry : pq.entrySet()) {
            String key = entry.getKey();
            String value = properties.getProperty(key);

            List<Query> queries = entry.getValue();
            LOG.debug("queries: " + queries.size());
            for (Query query : queries) {
                LOG.info("key: " + key);

                jobs.add(new ParallelPrecomputer.Job(key, query, null,
                            !(value.contains(" ORDER BY ")), "PrecomputeTask"));
            }
        }

        ParallelPrecomputer pp = getPrecomputer((ObjectStoreInterMineImpl) os);
        pp.setMinRows(minRows);

        try {
            pp.precompute(jobs);
        } catch (ObjectStoreException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Create a ParallelPrecomputer.
     *
     * @param os an ObjectStoreInterMineImpl
     * @return a ParallelPrecomputer for the ObjectStore
     */
    protected ParallelPrecomputer getPrecomputer(ObjectStoreInterMineImpl os) {
        return new ParallelPrecomputer(os, THREAD_COUNT);
    }

    /**
     * Get a Map of keys (from the precomputeProperties file) to Query objects to precompute.
     *
     * @param createAllOrders if true construct all permutations of order by for the QueryClass
     *   objects on the from list
     * @param os the ObjectStore
     * @param precomputeProperties the properties specifying which queries to precompute
     * @return a Map of keys to Query objects
     * @throws BuildException if the query cannot be constructed (for example when a class or the
     * collection doesn't exist
     */
    private static Map<String, List<Query>> getPrecomputeQueries(boolean createAllOrders,
            ObjectStore os, Properties precomputeProperties) {
        Map<String, List<Query>> returnMap = new TreeMap<String, List<Query>>();

        // TODO - read selectAllFields and createAllOrders from properties

        // TODO - property to not create empty tables

        for (Object precomputeKeyObj : new TreeSet<Object>(precomputeProperties.keySet())) {
            String precomputeKey = (String) precomputeKeyObj;

            String value = (String) precomputeProperties.get(precomputeKey);

            if (precomputeKey.startsWith("precompute.query")) {
                String iqlQueryString = value;
                Query query = parseQuery(os.getModel(), iqlQueryString, precomputeKey);
                List<Query> list = new ArrayList<Query>();
                list.add(query);
                returnMap.put(precomputeKey, list);
            } else {
                if (precomputeKey.startsWith("precompute.constructquery")) {
                    try {
                        List<Query> constructedQueries =
                            constructQueries(createAllOrders, os, value, precomputeKey);
                        returnMap.put(precomputeKey, constructedQueries);
                    } catch (Exception e) {
                        throw new BuildException(e);
                    }
                } else {
                    throw new BuildException("unknown key: '" + precomputeKey
                            + "' in properties file "
                            + getPropertiesFileName(os.getModel().getName()));
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
     * @param os the ObjectStore
     * @param path the path to construct a query for
     * @param precomputeKey the key of the path in the config file, for logging
     * @return a list of queries
     * @throws ClassNotFoundException if problem processing path
     * @throws IllegalArgumentException if problem processing path
     * @throws ObjectStoreException if there is a problem running a query to process the path
     */
    protected static List<Query> constructQueries(boolean createAllOrders, ObjectStore os,
            String path, String precomputeKey) throws ClassNotFoundException, ObjectStoreException {
        List<Query> queries = new ArrayList<Query>();

        // expand '+' to all subclasses in path
        Set<String> paths = QueryGenUtil.expandPath(os, path);
        for (String nextPath : paths) {
            LOG.info("Expanded path for id " + precomputeKey + " to \"" + nextPath + "\"");
            Query q = QueryGenUtil.constructQuery(os.getModel(), nextPath);
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

        Set<int[]> permutations = permutations(q.getEffectiveOrderBy().size());
        for (int[] order : permutations) {
            Query newQuery = QueryCloner.cloneQuery(q);
            List<Object> orderBy = new ArrayList<Object>(newQuery.getEffectiveOrderBy());
            newQuery.clearOrderBy();

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
    private static Query parseQuery(Model model, String iqlQueryString, String key) {
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
    private static Properties readProperties(String modelName) {
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
     *
     * @param n number of entities in ordered arrays
     * @return a set of int arrays
     */
    private static Set<int[]> permutations(int n) {
        Set<int[]> result = new LinkedHashSet<int[]>();
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

    private static void enumerate(Set<int[]> result, int[] array, int n) {
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
