package org.intermine.task;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;

/**
 * A Task that reads a list of queries from a properties file (eg. testmodel_precompute.properties)
 * and calls ObjectStoreInterMineImpl.precompute() using the Query.
 *
 * @author Kim Rutherford
 */

public class PrecomputeTask extends Task
{
    String alias;
    String modelName;
    boolean testMode;
    int minRows;
    // read by readProperties()
    Properties precomputeProperties = null;
    // set by setModel()
    Model model = null;

    /**
     * Set the ObjectStore alias
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set the model name - used to create the name of the properties file to search for.
     * @param modelName the properties file
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Set the mode of operation - if true run and time a set of test queries after each
     * call to precompute().
     * @param testMode set test mode on if and only if this is true
     */
    public void setTestMode(Boolean testMode) {
        this.testMode = testMode.booleanValue();
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
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        if (modelName == null) {
            throw new BuildException("modelName attribute is not set");
        }

        setModel();
        readProperties();

        ObjectStore os = null;

        try {
            os = ObjectStoreFactory.getObjectStore(alias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }

        if (!(os instanceof ObjectStoreInterMineImpl)) {
            throw new BuildException(alias + " isn't an ObjectStoreInterMineImpl");
        }

        if (testMode) {
            PrintStream outputStream = System.out;
            long start = System.currentTimeMillis();
            outputStream.println("Running tests before precomputing:");
            runTestQueries(os);
            outputStream.println("tests took: " + (System.currentTimeMillis() - start) / 1000
                                 + " seconds");
        }

        Iterator iter = getPrecomputeQueries().entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();

            List queries = (List) entry.getValue();

            Iterator queriesIter = queries.iterator();

            while (queriesIter.hasNext()) {
                Query query = (Query) queriesIter.next();

                ResultsInfo resultsInfo;

                try {
                    resultsInfo = os.estimate(query);
                } catch (ObjectStoreException e) {
                    throw new BuildException("Exception while calling ObjectStore.estimate()", e);
                }

                if (resultsInfo.getRows() >= minRows) {
                    precompute(os, query);
                }

                if (testMode) {
                    PrintStream outputStream = System.out;
                    long start = System.currentTimeMillis();
                    outputStream.println("Running tests after precomputing " + key + ":");
                    runTestQueries(os);
                    outputStream.println("tests took: "
                                         + (System.currentTimeMillis() - start) / 1000
                                         + " seconds");
                }
            }
        }
    }

    private static final String TEST_QUERY_PREFIX = "test.query.";

    /**
     * Call ObjectStoreInterMineImpl.precompute() with the given Query.
     * @param os the ObjectStore to call precompute() on
     * @param query the query to precompute
     * @throws BuildException if the query cannot be precomputed.
     */
    protected void precompute(ObjectStore os, Query query) throws BuildException {
        try {
            ((ObjectStoreInterMineImpl) os).precompute(query);
        } catch (ObjectStoreException e) {
            throw new BuildException("Exception while precomputing query: " + query, e);
        }
    }

    /**
     * Get a Map of keys (from the precomputeProperties file) to Query objects to precompute.
     * @return a Map of keys to Query objects
     * @throws BuildException if the query cannot be constructed (for example when a class or the
     * collection doesn't exist
     */
    protected Map getPrecomputeQueries() throws BuildException {
        Map returnMap = new TreeMap();

        Iterator iter = precomputeProperties.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            String precomputeKey = (String) entry.getKey();

            if (precomputeKey.startsWith("precompute.query")) {
                String iqlQueryString = (String) entry.getValue();
                Query query = parseQuery(iqlQueryString, precomputeKey);
                List list = new ArrayList();
                list.add(query);
                returnMap.put(precomputeKey, list);
            } else {
                if (precomputeKey.startsWith("precompute.constructquery")) {
                    String[] queryBits = ((String) entry.getValue()).split("[ \t]");
                    if (queryBits.length == 3) {
                        String objectClassName = queryBits[0];
                        String connectingField = queryBits[1];
                        String subjectClassName = queryBits[2];

                        List constructedQueries =
                            constructQueries(objectClassName, connectingField, subjectClassName);

                        returnMap.put(precomputeKey, constructedQueries);
                    } else {
                        throw new BuildException(precomputeKey + " should have three fields "
                                                 + "(ie. class fieldname class)");
                    }
                } else {
                    if (!precomputeKey.startsWith(TEST_QUERY_PREFIX)) {
                        throw new BuildException("unknown key in properties file "
                                                 + getPropertiesFileName());
                    }
                }
            }
        }

        return returnMap;
    }

    /**
     * Take two class names and a connecting collection or reference field name and create a new
     * Query.  Eg. for
     * "Company", "departments", "Department", create:
     *   SELECT DISTINCT a1_, a2_ FROM org.intermine.model.test model.Company AS a1_,
     *   org.intermine.model.testmodel.Department AS a2_ WHERE a1_.departments CONTAINS a2_ ORDER BY
     *   a1_
     * Queries will also be created for all combinations of sub-classes of objectClassName and
     * subjectClassName.
     * @param objectClassName the name of the object class
     * @param connectingFieldname the field name to use to reference the subject class
     * @param subjectClassName the name of the subject class
     * @return a List of Query objects
     * @throws BuildException if a query cannot be constructed (for example when a class or the
     * collection doesn't exist)
     */
    protected List constructQueries(String objectClassName,
                                    String connectingFieldname,
                                    String subjectClassName)
        throws BuildException {
        if (objectClassName.indexOf(".") == -1) {
            objectClassName = model.getPackageName() + "." + objectClassName;
        }
        if (subjectClassName.indexOf(".") == -1) {
            subjectClassName = model.getPackageName() + "." + subjectClassName;
        }

        ClassDescriptor objectClassDesc = model.getClassDescriptorByName(objectClassName);
        if (objectClassDesc == null) {
            throw new BuildException("cannot find ClassDescriptor for " + objectClassName
                                     + " (read name from "
                                     + getPropertiesFileName() + ")");
        }

        ClassDescriptor subjectClassDesc = model.getClassDescriptorByName(subjectClassName);
        if (subjectClassDesc == null) {
            throw new BuildException("cannot find ClassDescriptor for " + subjectClassName
                                     + " (read name from "
                                     + getPropertiesFileName() + ")");
        }

        Set allObjectCDs = model.getAllSubs(objectClassDesc);
        allObjectCDs.add(objectClassDesc);

        Set allSubjectCDs = model.getAllSubs(subjectClassDesc);
        allSubjectCDs.add(subjectClassDesc);

        List queryList = new ArrayList();

        Iterator allObjectCDsIter = allObjectCDs.iterator();

        while (allObjectCDsIter.hasNext()) {
            ClassDescriptor thisObjectCD = (ClassDescriptor) allObjectCDsIter.next();

            Iterator allSubjectCDsIter = allSubjectCDs.iterator();

            while (allSubjectCDsIter.hasNext()) {
                ClassDescriptor thisSubjectCD = (ClassDescriptor) allSubjectCDsIter.next();

                queryList.add(constructQuery(thisObjectCD.getType(), connectingFieldname,
                                             thisSubjectCD.getType()));
            }
        }

        return queryList;
    }

    /**
     * Take two class object and a connecting collection or reference field name and create a new
     * Query.  Eg. for
     * @param objectClass the object class
     * @param connectingFieldname the field name to use to reference the subject class
     * @param subjectClass the subject class
     * @return the new Query
     * @throws BuildException if the query cannot be constructed (for example when a class or the
     * collection doesn't exist)
     */
    protected Query constructQuery(Class objectClass, String connectingFieldname,
                                   Class subjectClass)
        throws BuildException {
        Query q = new Query();
        q.setDistinct(true);

        QueryClass qcObj = new QueryClass(objectClass);
        q.addFrom(qcObj);
        q.addToSelect(qcObj);

        QueryClass qcSub = new QueryClass(subjectClass);
        q.addFrom(qcSub);
        q.addToSelect(qcSub);
        q.addToOrderBy(qcObj);

        ClassDescriptor objectClassDesc = model.getClassDescriptorByName(objectClass.getName());
        FieldDescriptor fd = objectClassDesc.getFieldDescriptorByName(connectingFieldname);
        if (fd == null) {
            throw new BuildException("cannot find FieldDescriptor for " + connectingFieldname
                                     + " in " + objectClass.getName());
        }

        QueryReference ref;

        if (fd.isReference()) {
            ref = new QueryObjectReference(qcObj, connectingFieldname);
        } else {
            ref = new QueryCollectionReference(qcObj, connectingFieldname);
        }

        ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcSub);
        q.setConstraint(cc);

        return q;
    }


    /**
     * For a given IQL query, return a Query object.
     * @param iqlQueryString the IQL String
     * @param key the key from the properties file
     * @return a Query object
     * @throws BuildException if the IQL String cannot be parsed.
     */
    protected Query parseQuery(String iqlQueryString, String key) throws BuildException {
        IqlQuery iqlQuery = new IqlQuery(iqlQueryString, model.getPackageName());

        try {
            return iqlQuery.toQuery();
        } catch (IllegalArgumentException e) {
            throw new BuildException("Exception while parsing query: " + key
                                     + " = " + iqlQueryString, e);
        }
    }

    /**
     * Run all the test queries specified in precomputeProperties.
     * @param os the ObjectStore to run the queries against.
     * @throws BuildException if there is an error while running the queries.
     */
    protected void runTestQueries(ObjectStore os) throws BuildException {
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
                try {
                    results = os.execute(query);
                } catch (ObjectStoreException e) {
                    throw new BuildException("problem executing " + testqueryKey + " test", e);
                }
                int resultsSize = results.size();
                outputStream.println("  got size " + resultsSize + " in "
                                     + (System.currentTimeMillis() - start) / 1000 + " seconds");
                if (resultsSize > 0) {
                    List resultsRow1 = (List) results.get(0);
                    outputStream.println("  first row in "
                                         + (System.currentTimeMillis() - start) / 1000
                                         + " seconds");
                    List resultsRow2 = (List) results.get(resultsSize - 1);
                    outputStream.println("  last row in "
                                         + (System.currentTimeMillis() - start) / 1000
                                         + " seconds");
                }
            }
        }
    }

    /**
     * Set model using modelName.
     */
    protected void setModel() {
        try {
            model = Model.getInstanceByName(modelName);
        } catch (MetaDataException e) {
            throw new BuildException("Failed to find model for " + modelName, e);
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
            throw new BuildException("Exception while creating reading properties from "
                                     + propertiesFileName , e);
        }
    }

    /**
     * Return the name of the properties file that passed to the constructor.
     * @return the name of the properties file that passed to the constructor.
     */
    protected String getPropertiesFileName() {
        return modelName + "_precompute.properties";
    }
}
