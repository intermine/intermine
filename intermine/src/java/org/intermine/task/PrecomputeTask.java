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
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.metadata.Model;
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

            Query query = (Query) entry.getValue();

            try {
                ((ObjectStoreInterMineImpl) os).precompute(query);
            } catch (ObjectStoreException e) {
                throw new BuildException("Exception while precomputing query: " + query, e);
            }

            if (testMode) {
                PrintStream outputStream = System.out;
                long start = System.currentTimeMillis();
                outputStream.println("Running tests after precomputing " + key + ":");
                runTestQueries(os);
                outputStream.println("tests took: " + (System.currentTimeMillis() - start) / 1000
                                     + " seconds");
            }
        }
    }

    private static final String TEST_QUERY_PREFIX = "testquery.";

    private Map getPrecomputeQueries() throws BuildException {
        Map returnList = new TreeMap();

        Iterator iter = precomputeProperties.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            String precomputeKey = (String) entry.getKey();

            if (precomputeKey.startsWith("precompute.")) {
                String iqlQueryString = (String) entry.getValue();
                Query query = parseQuery(iqlQueryString, precomputeKey);
                returnList.put(precomputeKey, query);
            } else {
                if (precomputeKey.startsWith("query.constructor.")) {
                    String[] queryBits = ((String) entry.getValue()).split("[ \t]");
                    if (queryBits.length == 3) {
                        String subjectClassName = queryBits[0];
                        String connectingField = queryBits[1];
                        String objectClassName = queryBits[2];

                        Query constructedQuery =
                            constructQuery(subjectClassName, connectingField, objectClassName);

                        returnList.put(precomputeKey, constructedQuery);
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

        return returnList;
    }

    private Query constructQuery(String subjectClassName, String connectingField,
                                 String objectClassName) {
        return null;
    }

    private Query parseQuery(String iqlQueryString, String key) throws BuildException {
        IqlQuery iqlQuery = new IqlQuery(iqlQueryString, model.getPackageName());

        try {
            return iqlQuery.toQuery();
        } catch (IllegalArgumentException e) {
            throw new BuildException("Exception while parsing query: " + key
                                     + " = " + iqlQueryString, e);
        }
    }

    private void runTestQueries(ObjectStore os) throws BuildException {
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
                outputStream.println("  got size in " + (System.currentTimeMillis() - start) / 1000
                                     + " seconds");
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

    private void setModel() {
        try {
            model = Model.getInstanceByName(modelName);
        } catch (MetaDataException e) {
            throw new BuildException("Failed to find model for " + modelName, e);
        }
    }

    private void readProperties() throws BuildException {
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

    private String getPropertiesFileName() {
        return modelName + "_precompute.properties";
    }
}
