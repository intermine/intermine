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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.PathExpressionField;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QueryPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.sql.DatabaseUtil;
import org.intermine.sql.precompute.OptimiserCache;
import org.intermine.sql.precompute.PrecomputedTable;
import org.intermine.util.DynamicUtil;

/**
 * Provides a method to convert from SQL ResultSet data to InterMine object-based data.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public final class ResultsConverter
{
    private ResultsConverter() {
    }

    /**
     * Method to convert from SQL results to InterMine object-based results.
     * This method accepts an SQL ResultSet and a Query as an input. The ResultSet must contain a
     * column named the same as the aliases of the elements in the SELECT list of the Query,
     * each containing either the OBJECT column in the case of a business object, or the value of
     * any other value.
     * <br>
     * This method will return a List of ResultsRow objects.
     *
     * @param sqlResults the ResultSet
     * @param q the Query
     * @param os the ObjectStoreInterMineImpl with which to associate any new lazy objects
     * @param c a Connection with which to make extra requests
     * @param sequence an object representing the state of the database
     * @param optimise whether to use optimisation on path expression queries
     * @param extra object to record extra query execution time
     * @param goFasterTables a Set of PrecomputedTables that may help with extra queries
     * @param goFasterCache an OptimiserCache that may help with extra queries
     * @return a List of ResultsRow objects
     * @throws ObjectStoreException if the ResultSet does not match the Query in any way, or if a
     * SQL exception occurs
     */
    public static List<ResultsRow<Object>> convert(ResultSet sqlResults, Query q,
            ObjectStoreInterMineImpl os, Connection c, Map<Object, Integer> sequence,
            boolean optimise, ExtraQueryTime extra, Set<PrecomputedTable> goFasterTables,
            OptimiserCache goFasterCache) throws ObjectStoreException {
        Object currentColumn = null;
        HashSet<QuerySelectable> noObjectColumns = new HashSet<QuerySelectable>();
        HashSet<String> noObjectClassColumns = new HashSet<String>();
        boolean needPathExpressions = false;
        try {
            List<ResultsRow<Object>> retval = new ArrayList<ResultsRow<Object>>();
            HashSet<Integer> idsToFetch = new HashSet<Integer>();

            // populate aliases map once - ensure keys are Java object ids not the hashCode
            Map<QuerySelectable, String> aliases = new IdentityHashMap<QuerySelectable, String>();
            for (QuerySelectable node : q.getSelect()) {
                aliases.put(node, DatabaseUtil.generateSqlCompatibleName(q.getAliases().get(node)));
            }

            while (sqlResults.next()) {
                ResultsRow<Object> row = new ResultsRow<Object>();
                for (QuerySelectable node : q.getSelect()) {
                    String alias = aliases.get(node);
                    if (node instanceof QueryClass) {
                        Integer idField = null;
                        Object obj = null;
                        if (InterMineObject.class.isAssignableFrom(((QueryClass) node).getType())) {
                            idField = new Integer(sqlResults.getInt(alias + "id"));
                            obj = os.pilferObjectById(idField);
                        }
                        if (obj == null) {
                            String objectField = null;
                            if (noObjectColumns.contains(node)) {
                                if (obj == null) {
                                    obj = new ProxyReference(os, idField, InterMineObject.class);
                                    idsToFetch.add(idField);
                                }
                            } else {
                                if (os.getSchema().isFlatMode(((QueryClass) node).getType())) {
                                    obj = buildObject(sqlResults, alias, os,
                                            ((QueryClass) node).getType(), noObjectClassColumns);
                                    if (idField != null) {
                                        os.cacheObjectById(idField, (InterMineObject) obj);
                                    }
                                } else {
                                    try {
                                        objectField = sqlResults.getString(alias);
                                        if (objectField != null) {
                                            currentColumn = objectField;
                                            obj = NotXmlParser.parse(objectField, os);
                                            //if (objectField.length() < ObjectStoreInterMineImpl
                                            //        .CACHE_LARGEST_OBJECT) {
                                            os.cacheObjectById(((InterMineObject) obj).getId(),
                                                    (InterMineObject) obj);
                                            //} else {
                                            //    LOG.debug("Not cacheing large object "
                                            //            + obj.getId() + " on read" + " (size = "
                                            //            + (objectField.length() / 512) + " kB)");
                                            //}
                                        }
                                    } catch (SQLException e) {
                                        // Do nothing - it's just a notxml missing. However, to
                                        // avoid an Exception-storm, we should probably stop trying
                                        // this on future rows. We don't know how slow this
                                        // ResultSet is at throwing these exceptions.
                                        noObjectColumns.add(node);
                                        if (obj == null) {
                                            obj = new ProxyReference(os, idField,
                                                    InterMineObject.class);
                                            idsToFetch.add(idField);
                                        }
                                    }
                                }
                            }
                        }
                        row.add(obj);
                    } else if (node instanceof QueryPathExpression) {
                        row.add(null);
                        needPathExpressions = true;
                    } else {
                        currentColumn = sqlResults.getObject(alias);
                        if (currentColumn != null) {
                            if (Date.class.equals(node.getType())) {
                                currentColumn = new Date(((Long) currentColumn).longValue());
                            } else if (Class.class.equals(node.getType())) {
                                Set<Class<?>> classes = new HashSet<Class<?>>();
                                try {
                                    String[] b = ((String) currentColumn).split(" ");
                                    for (int i = 0; i < b.length; i++) {
                                        classes.add(Class.forName(b[i]));
                                    }
                                } catch (ClassNotFoundException e) {
                                    SQLException e2
                                        = new SQLException("Invalid entry in class column");
                                    e2.initCause(e);
                                    throw e2;
                                }
                                if (classes.size() == 1) {
                                    currentColumn = classes.iterator().next();
                                } else {
                                    currentColumn = DynamicUtil.composeClass(classes);
                                }
                            } else if (Short.class.equals(node.getType())
                                    && (currentColumn instanceof Integer)) {
                                int i = ((Integer) currentColumn).intValue();
                                currentColumn = new Short((short) i);
                            } else if (ClobAccess.class.equals(node.getType())) {
                                currentColumn = ClobAccess.decodeDbDescription(os,
                                        (String) currentColumn);
                            }
                        }
                        row.add(currentColumn);
                    }
                }
                retval.add(row);
            }
            if (!idsToFetch.isEmpty()) {
                Map<Integer, InterMineObject> fetched = fetchByIds(os, c, sequence,
                        InterMineObject.class, idsToFetch, extra);
                for (ResultsRow<Object> row : retval) {
                    for (int i = 0; i < row.size(); i++) {
                        Object obj = row.get(i);
                        if (obj instanceof ProxyReference) {
                            Integer id = ((ProxyReference) obj).getId();
                            obj = fetched.get(id);
                            if (obj == null) {
                                throw new ObjectStoreException("Error - could not fetch object"
                                        + " with ID of " + id + " for query " + q);
                            }
                            row.set(i, obj);
                        }
                    }
                }
            }
            if (needPathExpressions) {
                HashSet<QuerySelectable> done = new HashSet<QuerySelectable>();
                for (QuerySelectable node : q.getSelect()) {
                    if (node instanceof QueryObjectPathExpression) {
                        // TODO: Fetch the objects
                        if (!done.contains(node)) {
                            fetchObjectPathExpression(os, c, sequence, q,
                                    (QueryObjectPathExpression) node, retval, optimise, extra,
                                    goFasterTables, goFasterCache);
                            done.add(node);
                        }
                    } else if (node instanceof QueryCollectionPathExpression) {
                        fetchCollectionPathExpression(os, c, sequence, q,
                                (QueryCollectionPathExpression) node, retval, optimise, extra,
                                goFasterTables, goFasterCache);
                    } else if (node instanceof PathExpressionField) {
                        if (!done.contains(((PathExpressionField) node).getQope())) {
                            fetchObjectPathExpression(os, c, sequence, q,
                                    ((PathExpressionField) node).getQope(), retval, optimise,
                                    extra, goFasterTables, goFasterCache);
                            done.add(((PathExpressionField) node).getQope());
                        }
                    }
                }
            }
            return retval;
        } catch (SQLException e) {
            throw new ObjectStoreException("Error converting results: " + currentColumn, e);
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Unknown class mentioned in database OBJECT field"
                    + " while converting results: " + currentColumn, e);
        } catch (ClassCastException e) {
            throw new ObjectStoreException("Object is of wrong type while converting results: "
                    + currentColumn, e);
        }
    }

    //private static long timeSpentBuildObject = 0;
    //private static long timeSpentSql = 0;
    //private static int countBuildObject = 0;

    /**
     * Builds an object from separate fields in flat mode.
     *
     * @param sqlResults the SQL ResultSet
     * @param alias the name of the column being built
     * @param os the ObjectStore
     * @param type a Class matching the QueryClass that is this column
     * @param noObjectClassColumns a Set used internally
     * @return an InterMineObject
     * @throws SQLException if something goes wrong
     */
    protected static Object buildObject(ResultSet sqlResults, String alias,
            ObjectStoreInterMineImpl os, Class<?> type, Set<String> noObjectClassColumns)
        throws SQLException {
        //long time1 = System.currentTimeMillis();
        @SuppressWarnings("unchecked") Set<Class<?>> classes = (Set) Collections.singleton(type);
        if (!noObjectClassColumns.contains(alias)) {
            String objectClass = null;
            try {
                //long time3 = System.currentTimeMillis();
                objectClass = sqlResults.getString(alias + "objectclass");
                //timeSpentSql += System.currentTimeMillis() - time3;
            } catch (SQLException e) {
                noObjectClassColumns.add(alias);
            }
            if (objectClass != null) {
                classes = new HashSet<Class<?>>();
                try {
                    String[] b = objectClass.split(" ");
                    for (int i = 0; i < b.length; i++) {
                        classes.add(Class.forName(b[i]));
                    }
                } catch (ClassNotFoundException e) {
                    SQLException e2 = new SQLException("Invalid entry in objectclass column");
                    e2.initCause(e);
                    throw e2;
                }
            }
        }
        FastPathObject retval = DynamicUtil.createObject(classes);
        Map<String, FieldDescriptor> fields = os.getModel().getFieldDescriptorsForClass(retval
                .getClass());
        for (Map.Entry<String, FieldDescriptor> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            FieldDescriptor fd = entry.getValue();
            if (fd instanceof AttributeDescriptor) {
                //long time3 = System.currentTimeMillis();
                Object value = sqlResults.getObject(alias + DatabaseUtil.getColumnName(fd));
                //timeSpentSql += System.currentTimeMillis() - time3;
                Class<?> expectedType = retval.getFieldType(fieldName);
                if ((value instanceof Long) && Date.class.equals(expectedType)) {
                    value = new Date(((Long) value).longValue());
                } else if ((value instanceof Integer) && (Short.class.equals(expectedType)
                        || Short.TYPE.equals(expectedType))) {
                    value = new Short((short) ((Integer) value).intValue());
                }
                try {
                    retval.setFieldValue(fieldName, value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            } else if (fd instanceof CollectionDescriptor) {
                CollectionDescriptor cd = (CollectionDescriptor) fd;
                @SuppressWarnings("unchecked") Collection lazyColl = new ProxyCollection(os,
                        (InterMineObject) retval, cd.getName(), cd.getReferencedClassDescriptor()
                            .getType());
                retval.setFieldValue(cd.getName(), lazyColl);
            } else if (fd instanceof ReferenceDescriptor) {
                ReferenceDescriptor rd = (ReferenceDescriptor) fd;
                //long time3 = System.currentTimeMillis();
                Integer id = (Integer) sqlResults.getObject(alias + DatabaseUtil.getColumnName(fd));
                //timeSpentSql += System.currentTimeMillis() - time3;
                @SuppressWarnings("unchecked") Class<? extends InterMineObject> refType =
                    (Class) rd.getReferencedClassDescriptor().getType();
                if (id == null) {
                    retval.setFieldValue(fieldName, null);
                } else {
                    retval.setFieldValue(fieldName, new ProxyReference(os, id, refType));
                }
            }
        }
        //long time2 = System.currentTimeMillis();
        //timeSpentBuildObject += time2 - time1;
        //countBuildObject++;
        //if (countBuildObject % 100000 == 0) {
        //    LOG.info("Called buildObject " + countBuildObject + " times. Time spent: "
        //    + timeSpentBuildObject + ", Sql: " + timeSpentSql);
        //}
        return retval;
    }

    /**
     * Fetches the contents of a QueryObjectPathExpression for a query.
     *
     * @param os the ObjectStoreInterMineImpl
     * @param c the Connection
     * @param sequence an object representing the state of the database
     * @param q the Query
     * @param qope the QueryObjectPathExpression to fetch data for
     * @param retval the array of results that will be returned
     * @param optimise whether to optimise the query
     * @param extra object to record extra query execution time
     * @param goFasterTables a Set of PrecomputedTables that may help with extra queries
     * @param goFasterCache an OptimiserCache that may help with extra queries
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void fetchObjectPathExpression(ObjectStoreInterMineImpl os, Connection c,
            Map<Object, Integer> sequence, Query q, QueryObjectPathExpression qope,
            List<ResultsRow<Object>> retval, boolean optimise, ExtraQueryTime extra,
            Set<PrecomputedTable> goFasterTables,
            OptimiserCache goFasterCache) throws ObjectStoreException {
        int startingPoint;
        // This is a Map from the starting point ID to the ID of the referenced object
        Map<Integer, Integer> objectIds = new HashMap<Integer, Integer>();
        Set<Integer> idsToFetch = new HashSet<Integer>();
        QueryClass qc = qope.getQueryClass();
        // Search for starting point.
        startingPoint = q.getSelect().indexOf(qc);
        if (startingPoint == -1) {
            throw new ObjectStoreException("Path Expression " + qope + " needs QueryClass "
                    + qc + " to be in the SELECT list");
        }
        for (ResultsRow<Object> row : retval) {
            InterMineObject o = (InterMineObject) row.get(startingPoint);
            Integer refId = null;
            try {
                InterMineObject ref = (InterMineObject) o.getFieldProxy(qope.getFieldName());
                if (ref != null) {
                    refId = ref.getId();
                    idsToFetch.add(refId);
                    objectIds.put(o.getId(), refId);
                }
            } catch (Exception e) {
                throw new ObjectStoreException("Shouldn't ever happen", e);
            }
        }
        Query qopeQuery = qope.getQuery(idsToFetch, os.getSchema().isMissingNotXml());
        long startTime = System.currentTimeMillis();
        List<ResultsRow<Object>> res = os.executeWithConnection(c, qopeQuery, 0,
                Integer.MAX_VALUE, optimise, false, sequence, goFasterTables, goFasterCache);
        extra.addTime(System.currentTimeMillis() - startTime);
        Map<Integer, ResultsRow<Object>> fetched = new HashMap<Integer, ResultsRow<Object>>();
        for (ResultsRow<Object> row : res) {
            fetched.put((Integer) row.get(0), row);
        }
        int columnCount = q.getSelect().size();
        for (ResultsRow<Object> row : retval) {
            Integer startingId = ((InterMineObject) row.get(startingPoint)).getId();
            ResultsRow<Object> pathRow = fetched.get(objectIds.get(startingId));
            if (pathRow != null) {
                for (int column = 0; column < columnCount; column++) {
                    QuerySelectable qs = q.getSelect().get(column);
                    if (qs.equals(qope)) {
                        row.set(column, pathRow.get(1));
                    } else if (qs instanceof PathExpressionField) {
                        try {
                            if (((PathExpressionField) qs).getQope().equals(qope)) {
                                row.set(column, pathRow.get(((PathExpressionField) qs)
                                            .getFieldNumber() + 1));
                            }
                        } catch (IndexOutOfBoundsException e) {
                            throw new ObjectStoreException("PathExpressionField index "
                                    + ((PathExpressionField) qs).getFieldNumber()
                                    + " is out of range - it is numbered from zero, up to "
                                    + (pathRow.size() - 2), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Fetches the QueryCollectionPathExpression data for a query.
     *
     * @param os the ObjectStoreInterMineImpl
     * @param c the Connection
     * @param sequence an object representing the state of the database
     * @param q the Query
     * @param qcpe the QueryCollectionPathExpression to fetch data for
     * @param retval the array of results that will be returned
     * @param optimise whether to optimise the query
     * @param extra object to record extra query execution time
     * @param goFasterTables a Set of PrecomputedTables that may help with extra queries
     * @param goFasterCache an OptimiserCache that may help with extra queries
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void fetchCollectionPathExpression(ObjectStoreInterMineImpl os, Connection c,
            Map<Object, Integer> sequence, Query q, QueryCollectionPathExpression qcpe,
            List<ResultsRow<Object>> retval, boolean optimise, ExtraQueryTime extra,
            Set<PrecomputedTable> goFasterTables,
            OptimiserCache goFasterCache) throws ObjectStoreException {
        int startingPoint;
        Map<Integer, List<Object>> idsToFetch = new HashMap<Integer, List<Object>>();
        Set<InterMineObject> objectsToFetch = new HashSet<InterMineObject>();
        int columnToReplace = q.getSelect().indexOf(qcpe);
        QueryClass qc = qcpe.getQueryClass();
        startingPoint = q.getSelect().indexOf(qc);
        if (startingPoint == -1) {
            throw new ObjectStoreException("Path Expression " + qcpe + " needs QueryClass "
                    + qc + " to be in the SELECT list");
        }
        if (qcpe.isCollection()) {
            for (ResultsRow<Object> row : retval) {
                InterMineObject o = (InterMineObject) row.get(startingPoint);
                if (!idsToFetch.containsKey(o.getId())) {
                    idsToFetch.put(o.getId(), new ArrayList<Object>());
                    objectsToFetch.add(o);
                }
            }
            Query subQ = qcpe.getQuery(objectsToFetch);
            long startTime = System.currentTimeMillis();
            List<ResultsRow<Object>> results = os.executeWithConnection(c, subQ, 0,
                    Integer.MAX_VALUE, optimise, false, sequence, goFasterTables, goFasterCache);
            extra.addTime(System.currentTimeMillis() - startTime);
            boolean singleton = qcpe.isSingleton();
            for (ResultsRow<Object> row : results) {
                Integer id = (Integer) row.get(0);
                List<Object> list = idsToFetch.get(id);
                if (singleton) {
                    list.add(row.get(1));
                } else {
                    ResultsRow<Object> newRow = new ResultsRow<Object>();
                    Iterator<Object> iter = row.iterator();
                    iter.next();
                    while (iter.hasNext()) {
                        newRow.add(iter.next());
                    }
                    list.add(newRow);
                }
            }
            for (ResultsRow<Object> row : retval) {
                InterMineObject o = (InterMineObject) row.get(startingPoint);
                row.set(columnToReplace, idsToFetch.get(o.getId()));
            }
        } else {
            Map<Integer, Integer> objectIds = new HashMap<Integer, Integer>();
            for (ResultsRow<Object> row : retval) {
                InterMineObject o = (InterMineObject) row.get(startingPoint);
                Integer refId = null;
                try {
                    InterMineObject ref = (InterMineObject) o.getFieldProxy(qcpe.getFieldName());
                    if (ref != null) {
                        refId = ref.getId();
                        idsToFetch.put(refId, new ArrayList<Object>());
                        objectIds.put(o.getId(), refId);
                        objectsToFetch.add(ref);
                    }
                } catch (Exception e) {
                    throw new ObjectStoreException("Shouldn't ever happen", e);
                }
            }
            Query subQ = qcpe.getQuery(objectsToFetch);
            long startTime = System.currentTimeMillis();
            List<ResultsRow<Object>> results = os.executeWithConnection(c, subQ, 0,
                    Integer.MAX_VALUE, optimise, false, sequence, goFasterTables, goFasterCache);
            extra.addTime(System.currentTimeMillis() - startTime);
            boolean singleton = qcpe.isSingleton();
            for (ResultsRow<Object> row : results) {
                Integer id = (Integer) row.get(0);
                List<Object> list = idsToFetch.get(id);
                if (singleton) {
                    list.add(row.get(1));
                } else {
                    ResultsRow<Object> newRow = new ResultsRow<Object>();
                    Iterator<Object> iter = row.iterator();
                    iter.next();
                    while (iter.hasNext()) {
                        newRow.add(iter.next());
                    }
                    list.add(newRow);
                }
            }
            for (ResultsRow<Object> row : retval) {
                InterMineObject o = (InterMineObject) row.get(startingPoint);
                Integer referenceId = objectIds.get(o.getId());
                row.set(columnToReplace, idsToFetch.get(referenceId));
            }
        }
    }

    /**
     * Fetches a group of objects from the database by ID.
     *
     * @param os the ObjectStore
     * @param c the Connection
     * @param sequence an object representing the state of the database
     * @param clazz the class of the table to search in
     * @param idsToFetch a Collection of IDs to fetch
     * @param extra object to record extra query execution time
     * @return a Map from ID to object
     * @throws ObjectStoreException if an error occurs
     */
    protected static Map<Integer, InterMineObject> fetchByIds(ObjectStoreInterMineImpl os,
            Connection c, Map<Object, Integer> sequence, Class<?> clazz,
            Collection<Integer> idsToFetch, ExtraQueryTime extra) throws ObjectStoreException {
        try {
            if (idsToFetch.isEmpty()) {
                return Collections.emptyMap();
            }
            Query q = new Query();
            QueryClass qc = new QueryClass(clazz);
            q.addFrom(qc);
            q.addToSelect(qc);
            BagConstraint bc = new BagConstraint(new QueryField(qc, "id"), ConstraintOp.IN,
                        idsToFetch);
            q.setConstraint(bc);
            q.setDistinct(false);
            ObjectStoreInterMineImpl.BagTableToRemove bttr = null;
            if (idsToFetch.size() >= os.getMinBagTableSize()) {
                bttr = os.createTempBagTable(c, bc, false, null);
            }
            long startTime = System.currentTimeMillis();
            Iterator<ResultsRow<Object>> iter = os.executeWithConnection(c, q, 0, Integer.MAX_VALUE,
                    false, false, sequence).iterator();
            extra.addTime(System.currentTimeMillis() - startTime);
            if (bttr != null) {
                os.removeTempBagTable(c, bttr);
            }
            Map<Integer, InterMineObject> fetched = new HashMap<Integer, InterMineObject>();
            while (iter.hasNext()) {
                ResultsRow<Object> fetchedObjectRow = iter.next();
                InterMineObject fetchedObject = (InterMineObject) fetchedObjectRow.get(0);
                fetched.put(fetchedObject.getId(), fetchedObject);
            }
            return fetched;
        } catch (SQLException e) {
            throw new ObjectStoreException("Error fetching additional objects", e);
        }
    }
}
