package org.intermine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.Collection;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;

import org.intermine.web.TemplateQuery;
import org.intermine.web.TemplateQueryBinding;
import org.intermine.web.MainHelper;
import org.intermine.web.PathNode;

import org.apache.log4j.Logger;

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
    protected String summaryPropertiesFile;
    protected boolean testMode;
    protected int minRows = -1;
    // set by readProperties()
    protected Properties precomputeProperties = null;
    protected ObjectStoreSummary oss = null;
    protected ObjectStore os = null;
    private static final String TEST_QUERY_PREFIX = "test.query.";
    private String operation;

    /**
     * Set the ObjectStore alias
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
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
     * Set the name of the ObjectStore summary properties file.
     * @param summaryPropertiesFile the new summaryPropertiesFile
     */
    public void setSummaryPropertiesFile(String summaryPropertiesFile) {
        this.summaryPropertiesFile = summaryPropertiesFile;
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
     * Set the type of pre-compute:
     * model - read configuration file for model and pre-compute appropriate tables.
     * template - create pre-computed tables for all template queries defined for model.
     * all - both template and model
     * @param operation 'model', 'template' or 'all'
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        if (summaryPropertiesFile == null) {
            throw new BuildException("summaryPropertiesFile attribute is not set");
        }

        if (minRows == -1) {
            throw new BuildException("minRows attribute is not set");
        }

        if (operation == null) {
            throw new BuildException("operation attribte was not set");
        }

        ObjectStore objectStore;

        try {
            objectStore = ObjectStoreFactory.getObjectStore(alias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }

        if (!(objectStore instanceof ObjectStoreInterMineImpl)) {
            throw new BuildException(alias + " isn't an ObjectStoreInterMineImpl");
        }

        oss = createObjectStoreSummary();

        if (operation.equals("all")) {
            precomputeModel(objectStore, oss);
            precomputeTemplates(objectStore, oss);
        } else if (operation.equals("model")) {
            precomputeModel(objectStore, oss);
        } else if (operation.equals("template")) {
            precomputeTemplates(objectStore, oss);
        } else {
            throw new BuildException("invalied operation specified: " + operation);
        }
    }

    /**
     * Create precomputed tables for the given ObjectStore.  This method is also called from
     * PrecomputeTaskTest.
     * @param os the ObjectStore to precompute in
     * @param oss the ObjectStoreSummary for os
     */
    protected void precomputeModel(ObjectStore os, ObjectStoreSummary oss) {
        this.oss = oss;
        this.os = os;

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
        //Iterator iter = getPrecomputeQueries().entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();

            List queries = (List) entry.getValue();
            LOG.error("queries: " + queries.size());
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
                    LOG.info("precomputing " + key);
                    precompute(os, query);

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
     * Create precomputed tables for all template queries in the given ObjectStore.
     * @param os the ObjectStore to precompute in
     * @param oss the ObjectStoreSummary for os
     */
    protected void precomputeTemplates(ObjectStore os, ObjectStoreSummary oss) {
        Iterator iter = getPrecomputeTemplateQueries().entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            HashMap pathToQueryNode = new HashMap();
            TemplateQuery template = (TemplateQuery) entry.getValue();

            QueryAndIndexes qai = processTemplate(template);

            ResultsInfo resultsInfo;
            try {
                resultsInfo = os.estimate(qai.getQuery());
            } catch (ObjectStoreException e) {
                throw new BuildException("Exception while calling ObjectStore.estimate()", e);
            }

            if (resultsInfo.getRows() >= minRows) {
                LOG.info("precomputing template " + entry.getKey());
                precompute(os, qai.getQuery(), qai.getIndexes());
            }
        }
    }


    /**
     * For a template alter the query ready to pre-compute: remove editable constraints
     * and add editable fields to the select list.  Generate list of additional indexes
     * to create.
     * @param template the template query to alter
     * @return altered query and a list of indexes
     */
    protected QueryAndIndexes processTemplate(TemplateQuery template) {
        QueryAndIndexes qai = new QueryAndIndexes();
        HashMap pathToQueryNode = new HashMap();
        Query tmp = MainHelper.makeQuery(template.getQuery(), new HashMap(), pathToQueryNode);

        // find nodes with editable constraints to index and possibly add to select list
        Iterator niter = template.getNodes().iterator();
        while (niter.hasNext()) {
            PathNode node = (PathNode) niter.next();
            List ecs = template.getConstraints(node);
            if (ecs != null && ecs.size() > 0) {
                // node has editable constraints
                QueryNode qn = (QueryNode) pathToQueryNode.get(node.getPath());
                if (qn == null) {
                    throw new BuildException("no QueryNode for path " + node.getPath());
                }
                template.getQuery().getView().add(node.getPath());
                qai.addIndex(qn);
            }
        }

        // now generate query with editable constraints removed
        template.removeEditableConstraints();
        Query query = MainHelper.makeQuery(template.getQuery(), new HashMap(), new HashMap());
        qai.setQuery(query);

        return qai;
    }


    /**
     * Class to associate a query with a list of query nodes that will need to have
     * indexes created for them
     */
    protected class QueryAndIndexes
    {
        Query query = null;
        List indexes = new ArrayList();

        /**
         * add to the list of indexes
         * @param qn to add to list of indexes
         */
        public void addIndex(QueryNode qn) {
            this.indexes.add(qn);
        }

        /**
         * Return the list if indexes
         * @return list of indexes
         */
        public List getIndexes() {
            return this.indexes;
        }

        /**
         * set the query object
         * @param q the query object
         */
        public void setQuery(Query q) {
            this.query = q;
        }

        /**
         * get the query object
         * @return the query object
         */
        public Query getQuery() {
            return this.query;
        }

        /**
         * @see Object#equals
         */
        public boolean equals(Object o) {
            if (o instanceof QueryAndIndexes) {
                return query.equals(((QueryAndIndexes) o).query)
                    && indexes.equals(((QueryAndIndexes) o).indexes);
            }
            return false;
        }

        /**
         * @see Object#hashCode
         */
        public int hashCode() {
            return 3 * query.hashCode() + 7 * indexes.hashCode();
        }

        /**
         * @see Object#toString
         */
        public String toString() {
            return "query: " + query.toString() + ", indexes: " + indexes.toString();
        }
    }


    /**
     * Call ObjectStoreInterMineImpl.precompute() with the given Query.
     * @param os the ObjectStore to call precompute() on
     * @param query the query to precompute
     * @param indexes the index QueryNodes
     * @throws BuildException if the query cannot be precomputed.
     */
    protected void precompute(ObjectStore os, Query query, Collection indexes)
        throws BuildException {
        long start = System.currentTimeMillis();

        try {
            ((ObjectStoreInterMineImpl) os).precompute(query, indexes);
        } catch (ObjectStoreException e) {
            throw new BuildException("Exception while precomputing query: " + query
                    + " with indexes " + indexes, e);
        }

        LOG.info("precompute(indexes) of took "
                 + (System.currentTimeMillis() - start) / 1000
                 + " seconds for: " + query);
    }

    /**
     * Call ObjectStoreInterMineImpl.precompute() with the given Query.
     * @param os the ObjectStore to call precompute() on
     * @param query the query to precompute
     * @throws BuildException if the query cannot be precomputed.
     */
    protected void precompute(ObjectStore os, Query query) throws BuildException {
        long start = System.currentTimeMillis();

        try {
            ((ObjectStoreInterMineImpl) os).precompute(query);
        } catch (ObjectStoreException e) {
            throw new BuildException("Exception while precomputing query: " + query, e);
        }

        LOG.info("precompute() of took "
                 + (System.currentTimeMillis() - start) / 1000
                 + " seconds for: " + query);
    }

    /**
     * Get the built-in template queries.
     * @return Map from template name to TemplateQuery
     * @throws BuildException if an IO error occurs loading the template queries
     */
    protected Map getPrecomputeTemplateQueries() throws BuildException {
        TemplateQueryBinding binding = new TemplateQueryBinding();
        InputStream sin
                = PrecomputeTask.class.getClassLoader().getResourceAsStream("template-queries.xml");
        Map templates = binding.unmarshal(new InputStreamReader(sin));
        return templates;
    }

    /**
     * Get a Map of keys (from the precomputeProperties file) to Query objects to precompute.
     * @return a Map of keys to Query objects
     * @throws BuildException if the query cannot be constructed (for example when a class or the
     * collection doesn't exist
     */
    protected Map getPrecomputeQueries() throws BuildException {
        Map returnMap = new TreeMap();

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
                    String[] queryBits = value.split("[ \t]");

                    if (queryBits.length == 3) {
                        String objectClassName = queryBits[0];
                        String connectingField = queryBits[1];
                        String subjectClassName = queryBits[2];

                        List constructedQueries =
                            constructQueries(objectClassName, connectingField, subjectClassName);

                        returnMap.put(precomputeKey, constructedQueries);
                    } else {
                        if (queryBits.length == 5) {
                            String object1ClassName = queryBits[0];
                            String connectingField1 = queryBits[1];
                            String object2ClassName = queryBits[2];
                            String connectingField2 = queryBits[3];
                            String object3ClassName = queryBits[4];

                            List constructedQueries =
                                constructQueries(object1ClassName, connectingField1,
                                                 object2ClassName, connectingField2,
                                                 object3ClassName);


                            returnMap.put(precomputeKey, constructedQueries);
                        } else {
                            throw new BuildException(precomputeKey + " should have three or five "
                                                     + "fields (ie. class fieldname class)");
                        }
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

        Set allObjectCDs = getClassDecriptors(objectClassName);
        Set allSubjectCDs = getClassDecriptors(subjectClassName);

        List queryList = new ArrayList();

        Iterator allObjectCDsIter = allObjectCDs.iterator();

        while (allObjectCDsIter.hasNext()) {
            ClassDescriptor thisObjectCD = (ClassDescriptor) allObjectCDsIter.next();

            if (oss.getClassCount(thisObjectCD.getName()) == 0) {
                continue;
            }

            Iterator allSubjectCDsIter = allSubjectCDs.iterator();

            while (allSubjectCDsIter.hasNext()) {
                ClassDescriptor thisSubjectCD = (ClassDescriptor) allSubjectCDsIter.next();

                if (oss.getClassCount(thisSubjectCD.getName()) == 0) {
                    continue;
                }

                queryList.addAll(constructQuery(thisObjectCD.getType(), connectingFieldname,
                                                thisSubjectCD.getType(), true));
                queryList.addAll(constructQuery(thisObjectCD.getType(), connectingFieldname,
                                                thisSubjectCD.getType(), false));
            }
        }

        return queryList;
    }

    /**
     * Take three class names and two connecting collection or reference field names and create a
     * new Query.  Eg. for
     * "Company", "departments", "Department", "employees", "Employee" create:
     *   SELECT DISTINCT a1_, a2_, a3_ FROM org.intermine.model.testmodel.Company AS a1_,
     *   org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS
     *   a3_ WHERE (a1_.departments CONTAINS a2_ AND a2_.employees CONTAINS a3_)
     * Queries will also be created for all combinations of sub-classes of objectClassName and
     * subjectClassName if they are tagged with a "+" as the first character.
     * @param object1ClassName the name of the first object class
     * @param connectingFieldname1 the field name to use to reference the Class for object 2
     * @param object2ClassName the name of the second object class
     * @param connectingFieldname2 the field name to use to reference the Class for object 3
     * @param object3ClassName the name of the third object class
     * @return a List of Query objects
     * @throws BuildException if a query cannot be constructed (for example when a class or the
     * collection doesn't exist)
     */
    protected List constructQueries(String object1ClassName,
                                    String connectingFieldname1,
                                    String object2ClassName,
                                    String connectingFieldname2,
                                    String object3ClassName)
        throws BuildException {

        Set allObject1CDs = getClassDecriptors(object1ClassName);
        Set allObject2CDs = getClassDecriptors(object2ClassName);
        Set allObject3CDs = getClassDecriptors(object3ClassName);

        List queryList = new ArrayList();

        Iterator allObject1CDsIter = allObject1CDs.iterator();

        while (allObject1CDsIter.hasNext()) {
            ClassDescriptor thisObject1CD = (ClassDescriptor) allObject1CDsIter.next();

            if (oss.getClassCount(thisObject1CD.getName()) == 0) {
                continue;
            }

            Iterator allObject2CDsIter = allObject2CDs.iterator();

            while (allObject2CDsIter.hasNext()) {
                ClassDescriptor thisObject2CD = (ClassDescriptor) allObject2CDsIter.next();

                if (oss.getClassCount(thisObject2CD.getName()) == 0) {
                    continue;
                }

                Iterator allObject3CDsIter = allObject3CDs.iterator();

                while (allObject3CDsIter.hasNext()) {
                    ClassDescriptor thisObject3CD = (ClassDescriptor) allObject3CDsIter.next();

                    if (oss.getClassCount(thisObject3CD.getName()) == 0) {
                        continue;
                    }

                    queryList.addAll(constructQuery(thisObject1CD.getType(), connectingFieldname1,
                                                    thisObject2CD.getType(), connectingFieldname2,
                                                    thisObject3CD.getType(), true));

                    queryList.addAll(constructQuery(thisObject1CD.getType(), connectingFieldname1,
                                                    thisObject2CD.getType(), connectingFieldname2,
                                                    thisObject3CD.getType(), false));
                }
            }
        }

        return queryList;
    }

    /**
     * Return a Set of ClassDescriptors for the given className.  If className is a simple class
     * name (with or without the full package), then return it's ClassDescriptor.  If className
     * begins with a "+", remove the "+" and return Set containing the ClassDescriptor for the class
     * and ClassDescriptors for all subclasses.
     * @param className the class name
     * @return a Set of ClassDescriptors for the given className.
     */
    protected Set getClassDecriptors(String className) {
        boolean useSubClasses = false;
        if (className.startsWith("+")) {
            className = className.substring(1);
            useSubClasses = true;
        }
        if (className.indexOf(".") == -1) {
            className = os.getModel().getPackageName() + "." + className;
        }
        ClassDescriptor classDesc = os.getModel().getClassDescriptorByName(className);
        if (classDesc == null) {
            throw new BuildException("cannot find ClassDescriptor for " + className
                                     + " (read name from "
                                     + getPropertiesFileName() + ")");
        }

        Set returnSet;

        if (useSubClasses) {
            returnSet = os.getModel().getAllSubs(classDesc);
        } else {
            returnSet = new LinkedHashSet();
        }

        returnSet.add(classDesc);

        return returnSet;
    }

    /**
     * Take two class object and a connecting collection or reference field name and create a new
     * Query with objectClass.connectingFieldname = subjectClass
     * @param objectClass the object class
     * @param connectingFieldname the field name to use to reference the subject class
     * @param subjectClass the subject class
     * @param selectAllFields if true add all of the fields of objectClass and subjectClass to the
     * select list of the Query to precompute()
     * @return the new Queries
     * @throws BuildException if the query cannot be constructed (for example when a class or the
     * collection doesn't exist)
     */
    protected List constructQuery(Class objectClass, String connectingFieldname,
                                  Class subjectClass, boolean selectAllFields)
        throws BuildException {
        Query q = new Query();
        q.setDistinct(true);

        QueryClass qcObj = new QueryClass(objectClass);
        q.addFrom(qcObj);
        q.addToSelect(qcObj);

        if (selectAllFields) {
            List fieldNames = getAttributeFieldNames(objectClass);
            addFieldsToQuery(q, qcObj, fieldNames);
        }

        QueryClass qcSub = new QueryClass(subjectClass);
        q.addFrom(qcSub);
        q.addToSelect(qcSub);

        // fields should be added straight after the corresponding class for performance reasons
        if (selectAllFields) {
            List fieldNames = getAttributeFieldNames(subjectClass);
            addFieldsToQuery(q, qcSub, fieldNames);
        }

        ClassDescriptor objectClassDesc =
            os.getModel().getClassDescriptorByName(objectClass.getName());
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

        return getOrderedQueries(q);
    }

    /**
     * Take three class objects and two connecting collection or reference field name and create a
     * new Query.
     * @param object1Class an object class
     * @param connectingFieldname1 the field name to use to reference objectClass2 from objectClass1
     * @param object2Class an object class
     * @param connectingFieldname2 the field name to use to reference objectClass3 from objectClass2
     * @param object3Class an object class
     * @param selectAllFields if true add all of the fields of objectClass and subjectClass to the
     * select list of the Query to precompute()
     * @return the new Queries
     * @throws BuildException if the query cannot be constructed (for example when a class or the
     * collection doesn't exist)
     */
    protected List constructQuery(Class object1Class, String connectingFieldname1,
                                  Class object2Class, String connectingFieldname2,
                                  Class object3Class, boolean selectAllFields)
        throws BuildException {
        Query q = new Query();
        q.setDistinct(true);

        QueryClass qcObj1 = new QueryClass(object1Class);
        q.addFrom(qcObj1);
        q.addToSelect(qcObj1);

        if (selectAllFields) {
            List fieldNames = getAttributeFieldNames(object1Class);
            addFieldsToQuery(q, qcObj1, fieldNames);
        }

        QueryClass qcObj2 = new QueryClass(object2Class);
        q.addFrom(qcObj2);
        q.addToSelect(qcObj2);

        if (selectAllFields) {
            List fieldNames = getAttributeFieldNames(object2Class);
            addFieldsToQuery(q, qcObj2, fieldNames);
        }

        QueryClass qcObj3 = new QueryClass(object3Class);
        q.addFrom(qcObj3);
        q.addToSelect(qcObj3);

        if (selectAllFields) {
            List fieldNames = getAttributeFieldNames(object3Class);
            addFieldsToQuery(q, qcObj3, fieldNames);
        }

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        ClassDescriptor object1ClassDesc =
            os.getModel().getClassDescriptorByName(object1Class.getName());
        FieldDescriptor fd1 = object1ClassDesc.getFieldDescriptorByName(connectingFieldname1);
        if (fd1 == null) {
            throw new BuildException("cannot find FieldDescriptor for " + connectingFieldname1
                                     + " in " + object1Class.getName());
        }

        QueryReference ref1;
        if (fd1.isReference()) {
            ref1 = new QueryObjectReference(qcObj1, connectingFieldname1);
        } else {
            ref1 = new QueryCollectionReference(qcObj1, connectingFieldname1);
        }
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj2);
        cs.addConstraint(cc1);

        ClassDescriptor object2ClassDesc =
            os.getModel().getClassDescriptorByName(object2Class.getName());
        FieldDescriptor fd2 = object2ClassDesc.getFieldDescriptorByName(connectingFieldname2);
        if (fd2 == null) {
            throw new BuildException("cannot find FieldDescriptor for " + connectingFieldname2
                                     + " in " + object2Class.getName());
        }

        QueryReference ref2;
        if (fd2.isReference()) {
            ref2 = new QueryObjectReference(qcObj2, connectingFieldname2);
        } else {
            ref2 = new QueryCollectionReference(qcObj2, connectingFieldname2);
        }
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcObj3);
        cs.addConstraint(cc2);

        q.setConstraint(cs);
        return getOrderedQueries(q);
    }

    /**
     * Return a List containing clones of the given Query, but with each ordered by a different
     * field of the Query.
     * @param q the Query
     * @return clones of the Query order by each of the fields
     */
    protected List getOrderedQueries(Query q) {
        List queryList = new ArrayList();

        for (int i = 0; i < q.getSelect().size(); i++) {
            // clone() first, then call addToOrderBy() with a QueryNode from the cloned Query
            Query queryClone = QueryCloner.cloneQuery(q);

            if (i == 0) {
                // by default, the Query is ordered by the first selected QueryNode, so just use the
                // clone to avoid adding extra unnecessary order by elements
                queryList.add(queryClone);
            } else {
                QueryNode selectNode = (QueryNode) queryClone.getSelect().get(i);

                if (selectNode instanceof QueryClass) {
                    QueryClass queryClass = (QueryClass) selectNode;
                    queryClone.addToOrderBy(queryClass);
                } else {
                    QueryEvaluable queryEvaluable = (QueryEvaluable) selectNode;
                    queryClone.addToOrderBy(queryEvaluable);
                }
                queryList.add(queryClone);
            }
        }

        return queryList;
    }

    /**
     * Return the names of the attribute fields of a class.
     * @param c the Class
     * @return the names of the attribute fields of a class.
     */
    protected List getAttributeFieldNames(Class c) {
        List returnList = new ArrayList();

        ClassDescriptor classDesc = os.getModel().getClassDescriptorByName(c.getName());
        Set attributeFDs = classDesc.getAllAttributeDescriptors();
        Iterator attributeFDIter = attributeFDs.iterator();

        while (attributeFDIter.hasNext()) {
            FieldDescriptor fd = (FieldDescriptor) attributeFDIter.next();
            returnList.add(fd.getName());
        }

        return returnList;
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
                try {
                    results = os.execute(query);
                } catch (ObjectStoreException e) {
                    throw new BuildException("problem executing " + testqueryKey + " test", e);
                }
                int resultsSize = results.size();
                outputStream.println("  got size " + resultsSize + " in "
                                     + (System.currentTimeMillis() - start) / 1000 + " seconds");
                if (resultsSize > 0) {
                    start = System.currentTimeMillis();
                    List resultsRow1 = (List) results.get(0);
                    outputStream.println("  first row in "
                                         + (System.currentTimeMillis() - start) / 1000
                                         + " seconds");
                    start = System.currentTimeMillis();
                    List resultsRow2 = (List) results.get(resultsSize - 1);
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
     * Create a ObjectStoreSummary from the summaryPropertiesFile.
     * @return a new ObjectStoreSummary
     */
    protected ObjectStoreSummary createObjectStoreSummary() {
        try {
            InputStream summaryPropertiesStream =
                PrecomputeTask.class.getClassLoader().getResourceAsStream(summaryPropertiesFile);

            if (summaryPropertiesStream == null) {
                throw new BuildException("Cannot find " + summaryPropertiesFile);
            }

            Properties summaryProperties = new Properties();
            summaryProperties.load(summaryPropertiesStream);

            return new ObjectStoreSummary(summaryProperties);
        } catch (IOException e) {
            throw new BuildException("Exception while reading properties from "
                                     + summaryPropertiesFile , e);
        }
    }

    /**
     * Return the name of the properties file that passed to the constructor.
     * @return the name of the properties file that passed to the constructor.
     */
    protected String getPropertiesFileName() {
        return os.getModel().getName() + "_precompute.properties";
    }
}
