package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFieldPathExpression;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QueryPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Provides a method to convert from SQL ResultSet data to InterMine object-based data.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ResultsConverter
{
    private static final Logger LOG = Logger.getLogger(ResultsConverter.class);

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
     * @return a List of ResultsRow objects
     * @throws ObjectStoreException if the ResultSet does not match the Query in any way, or if a
     * SQL exception occurs
     */
    public static List<ResultsRow> convert(ResultSet sqlResults, Query q,
            ObjectStoreInterMineImpl os, Connection c,
            Map<Object, Integer> sequence) throws ObjectStoreException {
        Object currentColumn = null;
        HashSet noObjectColumns = new HashSet();
        HashSet noObjectClassColumns = new HashSet();
        boolean needPathExpressions = false;
        try {
            List<ResultsRow> retval = new ArrayList();
            HashSet idsToFetch = new HashSet();
            while (sqlResults.next()) {
                ResultsRow row = new ResultsRow();
                for (QuerySelectable node : q.getSelect()) {
                    String alias = DatabaseUtil.generateSqlCompatibleName((String) q.getAliases()
                            .get(node));
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
                    } else if ((node instanceof QueryFieldPathExpression)
                            && "id".equals(((QueryFieldPathExpression) node).getFieldName())
                            && (((QueryFieldPathExpression) node).getQueryClass() != null)) {
                        currentColumn = sqlResults.getObject(alias);
                        Integer foreignKey = (Integer) (currentColumn == null
                                ? ((QueryFieldPathExpression) node).getDefaultValue()
                                : currentColumn);
                        row.add(foreignKey);
                    } else if (node instanceof QueryPathExpression) {
                        row.add(node);
                        needPathExpressions = true;
                    } else {
                        currentColumn = sqlResults.getObject(alias);
                        if (Date.class.equals(node.getType())) {
                            currentColumn = new Date(((Long) currentColumn).longValue());
                        }
                        row.add(currentColumn);
                    }
                }
                retval.add(row);
            }
            if (!idsToFetch.isEmpty()) {
                Map<Integer, InterMineObject> fetched = fetchByIds(os, c, sequence,
                        InterMineObject.class, idsToFetch);
                for (ResultsRow row : retval) {
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
            Map<QueryObjectPathExpression, Integer> startingPoints = new HashMap();
            Map<QueryObjectPathExpression, Map<Integer, InterMineObject>> fetchedObjects
                = new HashMap();
            if (needPathExpressions) {
                for (QuerySelectable node : q.getSelect()) {
                    if (node instanceof QueryFieldPathExpression) {
                        QueryFieldPathExpression qfpe = (QueryFieldPathExpression) node;
                        if ("id".equals(qfpe.getFieldName()) && (qfpe.getQueryClass() != null)) {
                            // Do nothing - this field is put in the SELECT list.
                        } else {
                            // TODO: Fetch the fields
                            fetchFieldPathExpression(os, c, sequence, q, qfpe, startingPoints,
                                    retval, fetchedObjects);
                        }
                    } else if (node instanceof QueryObjectPathExpression) {
                        // TODO: Fetch the objects
                        fetchObjectPathExpression(os, c, sequence, q,
                                (QueryObjectPathExpression) node, startingPoints, retval,
                                fetchedObjects);
                    } else if (node instanceof QueryCollectionPathExpression) {
                        fetchCollectionPathExpression(os, c, sequence, q,
                                (QueryCollectionPathExpression) node, startingPoints, retval,
                                fetchedObjects);
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
            ObjectStoreInterMineImpl os, Class type, Set noObjectClassColumns) throws SQLException {
        //long time1 = System.currentTimeMillis();
        Set classes = Collections.singleton(type);
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
                classes = new HashSet();
                try {
                    String b[] = objectClass.split(" ");
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
        Object retval = DynamicUtil.createObject(classes);
        Map fields = os.getModel().getFieldDescriptorsForClass(retval.getClass());
        Iterator iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String fieldName = (String) entry.getKey();
            FieldDescriptor fd = (FieldDescriptor) entry.getValue();
            if (fd instanceof AttributeDescriptor) {
                TypeUtil.FieldInfo fieldInfo = TypeUtil.getFieldInfo(retval.getClass(), fieldName);
                //long time3 = System.currentTimeMillis();
                Object value = sqlResults.getObject(alias + DatabaseUtil.getColumnName(fd));
                //timeSpentSql += System.currentTimeMillis() - time3;
                if ((value instanceof Long) && Date.class.equals(fieldInfo.getType())) {
                    value = new Date(((Long) value).longValue());
                }
                try {
                    fieldInfo.getSetter().invoke(retval, value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            } else if (fd instanceof CollectionDescriptor) {
                CollectionDescriptor cd = (CollectionDescriptor) fd;
                Collection lazyColl = new ProxyCollection(os, (InterMineObject) retval,
                        cd.getName(), cd.getReferencedClassDescriptor().getType());
                TypeUtil.setFieldValue(retval, cd.getName(), lazyColl);
            } else if (fd instanceof ReferenceDescriptor) {
                ReferenceDescriptor rd = (ReferenceDescriptor) fd;
                //long time3 = System.currentTimeMillis();
                Integer id = (Integer) sqlResults.getObject(alias + DatabaseUtil.getColumnName(fd));
                //timeSpentSql += System.currentTimeMillis() - time3;
                Class refType = rd.getReferencedClassDescriptor().getType();
                if (id == null) {
                    TypeUtil.setFieldValue(retval, fieldName, null);
                } else {
                    TypeUtil.setFieldValue(retval, fieldName, new ProxyReference(os, id, refType));
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
     * Fetches the QueryObjectPathExpression data for a query.
     *
     * @param os the ObjectStoreInterMineImpl
     * @param c the Connection
     * @param sequence an object representing the state of the database
     * @param q the Query
     * @param qope the QueryObjectPathExpression to fetch data for
     * @param startingPoints a map for locating the data that the reference is from. A Map from
     * QueryObjectPathExpression to an Integer giving the column number
     * @param retval the array of results that will be returned
     * @param fetchedObjects a map containing the data that has been fetched, to reuse. A Map from
     * QueryObjectPathExpression to a Map from starting point object ID to result object
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void fetchObjectPathExpression(ObjectStoreInterMineImpl os, Connection c,
            Map<Object, Integer> sequence, Query q, QueryObjectPathExpression qope,
            Map<QueryObjectPathExpression, Integer> startingPoints, List<ResultsRow> retval,
            Map<QueryObjectPathExpression, Map<Integer, InterMineObject>> fetchedObjects)
    throws ObjectStoreException {
        if (fetchedObjects.get(qope) == null) {
            int startingPoint;
            // This is a Map from the starting point ID to the ID of the referenced object
            Map<Integer, Integer> objectIds = new HashMap();
            // This is a Map from the starting point ID to the referenced object
            Map<Integer, InterMineObject> objects = new HashMap();
            Set<Integer> idsToFetch = new HashSet();
            if (qope.getQueryClass() != null) {
                QueryClass qc = qope.getQueryClass();
                // Search for starting point.
                startingPoint = q.getSelect().indexOf(qc);
                if (startingPoint == -1) {
                    throw new ObjectStoreException("Path Expression " + qope + " needs QueryClass "
                            + qc + " to be in the SELECT list");
                }
                Method getter = TypeUtil.getProxyGetter(qc.getType(), qope.getFieldName());
                for (ResultsRow row : retval) {
                    InterMineObject o = (InterMineObject) row.get(startingPoint);
                    Integer refId = null;
                    try {
                        InterMineObject ref = (InterMineObject) getter.invoke(o);
                        if (ref != null) {
                            refId = ref.getId();
                            if (ref instanceof ProxyReference) {
                                idsToFetch.add(refId);
                                objectIds.put(o.getId(), refId);
                            } else {
                                objects.put(o.getId(), ref);
                            }
                        }
                    } catch (Exception e) {
                        // Shouldn't ever happen
                        throw new ObjectStoreException(e);
                    }
                }
            } else {
                QueryObjectPathExpression parent = qope.getQope();
                fetchObjectPathExpression(os, c, sequence, q, parent, startingPoints, retval,
                        fetchedObjects);
                startingPoint = startingPoints.get(parent).intValue();
                Map<Integer, InterMineObject> previousObjects = fetchedObjects.get(parent);
                Method getter = TypeUtil.getProxyGetter(parent.getType(), qope.getFieldName());
                for (Map.Entry<Integer, InterMineObject> entry : previousObjects.entrySet()) {
                    Integer refId = null;
                    try {
                        InterMineObject ref = (InterMineObject) getter.invoke(entry.getValue());
                        if (ref != null) {
                            refId = ref.getId();
                            if (ref instanceof ProxyReference) {
                                idsToFetch.add(refId);
                                objectIds.put(entry.getKey(), refId);
                            } else {
                                objects.put(entry.getKey(), ref);
                            }
                        }
                    } catch (Exception e) {
                        // Shouldn't ever happen
                    }
                }
            }
            startingPoints.put(qope, new Integer(startingPoint));
            Map<Integer, InterMineObject> fetched = fetchByIds(os, c, sequence, qope.getType(),
                    idsToFetch);
            for (Map.Entry<Integer, Integer> objectIdsEntry : objectIds.entrySet()) {
                InterMineObject ref = fetched.get(objectIdsEntry.getValue());
                if (ref == null) {
                    throw new ObjectStoreException("Error - could not fetch object with ID of "
                            + objectIdsEntry.getValue() + " for path expression " + qope
                            + " for starting point " + objectIdsEntry.getKey());
                }
                objects.put(objectIdsEntry.getKey(), ref);
            }
            fetchedObjects.put(qope, objects);
            int rowToReplace = q.getSelect().indexOf(qope);
            if (rowToReplace != -1) {
                for (ResultsRow row : retval) {
                    row.set(rowToReplace, objects.get(((InterMineObject) row.get(startingPoint))
                                .getId()));
                }
            }
        }
    }

    /**
     * Fetches the QueryFieldPathExpression data for a query.
     *
     * @param os the ObjectStoreInterMineImpl
     * @param c the Connection
     * @param sequence an object representing the state of the database
     * @param q the Query
     * @param qfpe the QueryFieldPathExpression to fetch data for
     * @param startingPoints a map for locating the data that the reference is from. A Map from
     * QueryObjectPathExpression to an Integer giving the column number
     * @param retval the array of results that will be returned
     * @param fetchedObjects a map containing the data that has been fetched, to reuse. A Map from
     * QueryObjectPathExpression to a Map from starting point object ID to result object
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void fetchFieldPathExpression(ObjectStoreInterMineImpl os, Connection c,
            Map<Object, Integer> sequence, Query q, QueryFieldPathExpression qfpe,
            Map<QueryObjectPathExpression, Integer> startingPoints, List<ResultsRow> retval,
            Map<QueryObjectPathExpression, Map<Integer, InterMineObject>> fetchedObjects)
    throws ObjectStoreException {
        QueryObjectPathExpression parent = qfpe.getParent();
        fetchObjectPathExpression(os, c, sequence, q, parent, startingPoints, retval,
                fetchedObjects);
        Map<Integer, InterMineObject> objects = fetchedObjects.get(parent);
        int startingPoint = startingPoints.get(parent).intValue();
        int rowToReplace = q.getSelect().indexOf(qfpe);
        try {
            for (ResultsRow row : retval) {
                InterMineObject obj = objects.get(((InterMineObject) row.get(startingPoint))
                        .getId());
                if (obj == null) {
                    row.set(rowToReplace, qfpe.getDefaultValue());
                } else {
                    row.set(rowToReplace, TypeUtil.getFieldValue(obj, qfpe.getFieldName()));
                }
            }
        } catch (Exception e) {
            // Shouldn't ever happen
            throw new ObjectStoreException(e);
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
     * @param startingPoints a map for locating the data that the reference is from. A Map from
     * QueryObjectPathExpression to an Integer giving the column number
     * @param retval the array of results that will be returned
     * @param fetchedObjects a map containing the data that has been fetched, to reuse. A Map from
     * QueryObjectPathExpression to a Map from starting point object ID to result object
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void fetchCollectionPathExpression(ObjectStoreInterMineImpl os, Connection c,
            Map<Object, Integer> sequence, Query q, QueryCollectionPathExpression qcpe,
            Map<QueryObjectPathExpression, Integer> startingPoints, List<ResultsRow> retval,
            Map<QueryObjectPathExpression, Map<Integer, InterMineObject>> fetchedObjects)
    throws ObjectStoreException {
        int startingPoint;
        Map<Integer, List> idsToFetch = new HashMap();
        Set<InterMineObject> objectsToFetch = new HashSet();
        int rowToReplace = q.getSelect().indexOf(qcpe);
        if (qcpe.getQueryClass() != null) {
            QueryClass qc = qcpe.getQueryClass();
            startingPoint = q.getSelect().indexOf(qc);
            if (startingPoint == -1) {
                throw new ObjectStoreException("Path Expression " + qcpe + " needs QueryClass "
                        + qc + " to be in the SELECT list");
            }
            for (ResultsRow row : retval) {
                InterMineObject o = (InterMineObject) row.get(startingPoint);
                if (!idsToFetch.containsKey(o.getId())) {
                    idsToFetch.put(o.getId(), new ArrayList());
                    objectsToFetch.add(o);
                }
            }
            Query subQ = qcpe.getQuery(objectsToFetch);
            List<ResultsRow> results = os.executeWithConnection(c, subQ, 0, Integer.MAX_VALUE,
                    false, false, sequence);
            boolean singleton = qcpe.isSingleton();
            for (ResultsRow row : results) {
                Integer id = (Integer) row.get(0);
                List list = idsToFetch.get(id);
                if (singleton) {
                    list.add(row.get(1));
                } else {
                    ResultsRow newRow = new ResultsRow();
                    Iterator iter = row.iterator();
                    iter.next();
                    while (iter.hasNext()) {
                        newRow.add(iter.next());
                    }
                    list.add(newRow);
                }
            }
            for (ResultsRow row : retval) {
                InterMineObject o = (InterMineObject) row.get(startingPoint);
                row.set(rowToReplace, idsToFetch.get(o.getId()));
            }
        } else {
            QueryObjectPathExpression parent = qcpe.getQope();
            fetchObjectPathExpression(os, c, sequence, q, parent, startingPoints, retval,
                    fetchedObjects);
            startingPoint = startingPoints.get(parent).intValue();
            Map<Integer, InterMineObject> previousObjects = fetchedObjects.get(parent);
            for (Map.Entry<Integer, InterMineObject> entry : previousObjects.entrySet()) {
                InterMineObject o = entry.getValue();
                if (!idsToFetch.containsKey(o.getId())) {
                    idsToFetch.put(o.getId(), new ArrayList());
                    objectsToFetch.add(o);
                }
            }
            Query subQ = qcpe.getQuery(objectsToFetch);
            List<ResultsRow> results = os.executeWithConnection(c, subQ, 0, Integer.MAX_VALUE,
                    false, false, sequence);
            boolean singleton = qcpe.isSingleton();
            for (ResultsRow row : results) {
                Integer id = (Integer) row.get(0);
                List list = idsToFetch.get(id);
                if (singleton) {
                    list.add(row.get(1));
                } else {
                    ResultsRow newRow = new ResultsRow();
                    Iterator iter = row.iterator();
                    iter.next();
                    while (iter.hasNext()) {
                        newRow.add(iter.next());
                    }
                    list.add(newRow);
                }
            }
            for (ResultsRow row : retval) {
                InterMineObject o = (InterMineObject) row.get(startingPoint);
                o = previousObjects.get(o.getId());
                if (o == null) {
                    row.set(rowToReplace, null);
                } else {
                    row.set(rowToReplace, idsToFetch.get(o.getId()));
                }
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
     * @return a Map from ID to object
     * @throws ObjectStoreException if an error occurs
     */
    protected static Map<Integer, InterMineObject> fetchByIds(ObjectStoreInterMineImpl os,
            Connection c, Map<Object, Integer> sequence, Class clazz,
            Collection<Integer> idsToFetch) throws ObjectStoreException {
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
            Iterator iter = os.executeWithConnection(c, q, 0, Integer.MAX_VALUE, false, false,
                    sequence).iterator();
            if (bttr != null) {
                os.removeTempBagTable(c, bttr);
            }
            Map<Integer, InterMineObject> fetched = new HashMap();
            while (iter.hasNext()) {
                ResultsRow fetchedObjectRow = (ResultsRow) iter.next();
                InterMineObject fetchedObject = (InterMineObject) fetchedObjectRow.get(0);
                fetched.put(fetchedObject.getId(), fetchedObject);
            }
            return fetched;
        } catch (SQLException e) {
            throw new ObjectStoreException("Error fetching additional objects", e);
        }
    }
}
