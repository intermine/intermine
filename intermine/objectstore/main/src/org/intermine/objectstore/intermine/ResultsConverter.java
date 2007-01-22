package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFieldPathExpression;
import org.intermine.objectstore.query.QueryPathExpression;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DatabaseUtil;
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
     * @return a List of ResultsRow objects
     * @throws ObjectStoreException if the ResultSet does not match the Query in any way, or if a
     * SQL exception occurs
     */
    public static List convert(ResultSet sqlResults, Query q, ObjectStoreInterMineImpl os,
            Connection c) throws ObjectStoreException {
        Object currentColumn = null;
        HashSet noObjectColumns = new HashSet();
        HashSet noObjectClassColumns = new HashSet();
        try {
            ArrayList retval = new ArrayList();
            HashSet idsToFetch = new HashSet();
            while (sqlResults.next()) {
                ResultsRow row = new ResultsRow();
                Iterator selectIter = q.getSelect().iterator();
                while (selectIter.hasNext()) {
                    QuerySelectable node = (QuerySelectable) selectIter.next();
                    String alias = DatabaseUtil.generateSqlCompatibleName((String) q.getAliases()
                            .get(node));
                    if (node instanceof QueryClass) {
                        Integer idField = new Integer(sqlResults.getInt(alias + "id"));
                        InterMineObject obj = os.pilferObjectById(idField);
                        if (obj == null) {
                            String objectField = null;
                            if (noObjectColumns.contains(node)) {
                                if (obj == null) {
                                    obj = new ProxyReference(os, idField, InterMineObject.class);
                                    idsToFetch.add(idField);
                                }
                            } else {
                                if (os.getSchema().isFlatMode()) {
                                    obj = buildObject(sqlResults, alias, os,
                                            ((QueryClass) node).getType(), noObjectClassColumns);
                                    os.cacheObjectById(obj.getId(), obj);
                                } else {
                                    try {
                                        objectField = sqlResults.getString(alias);
                                        if (objectField != null) {
                                            currentColumn = objectField;
                                            obj = NotXmlParser.parse(objectField, os);
                                            //if (objectField.length() < ObjectStoreInterMineImpl
                                            //        .CACHE_LARGEST_OBJECT) {
                                                os.cacheObjectById(obj.getId(), obj);
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
                            && "id".equals(((QueryFieldPathExpression) node).getFieldName())) {
                        currentColumn = sqlResults.getObject(alias);
                        Integer foreignKey = (Integer) (currentColumn == null
                                ? ((QueryFieldPathExpression) node).getDefaultValue()
                                : currentColumn);
                        row.add(foreignKey);
                    } else if (node instanceof QueryPathExpression) {
                        throw new ObjectStoreException("Path expressions are not implemented yet");
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
                Query q2 = new Query();
                QueryClass qc = new QueryClass(InterMineObject.class);
                q2.addFrom(qc);
                q2.addToSelect(qc);
                BagConstraint bc = new BagConstraint(new QueryField(qc, "id"), ConstraintOp.IN,
                            idsToFetch);
                q2.setConstraint(bc);
                q2.setDistinct(false);
                ObjectStoreInterMineImpl.BagTableToRemove bttr = null;
                if (idsToFetch.size() >= os.getMinBagTableSize()) {
                    bttr = os.createTempBagTable(c, bc, false);
                }
                Iterator iter = os.executeWithConnection(c, q2, 0, Integer.MAX_VALUE, false, false,
                        os.getSequence()).iterator();
                if (bttr != null) {
                    os.removeTempBagTable(c, bttr);
                }
                HashMap fetched = new HashMap();
                while (iter.hasNext()) {
                    ResultsRow fetchedObjectRow = (ResultsRow) iter.next();
                    InterMineObject fetchedObject = (InterMineObject) fetchedObjectRow.get(0);
                    fetched.put(fetchedObject.getId(), fetchedObject);
                }
                iter = retval.iterator();
                while (iter.hasNext()) {
                    ResultsRow row = (ResultsRow) iter.next();
                    for (int i = 0; i < row.size(); i++) {
                        Object obj = row.get(i);
                        if (obj instanceof ProxyReference) {
                            Integer id = ((ProxyReference) obj).getId();
                            obj = fetched.get(id);
                            if (obj == null) {
                                throw new ObjectStoreException("Error - could not fetch object"
                                        + " with ID of " + id);
                            }
                            row.set(i, obj);
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
    protected static InterMineObject buildObject(ResultSet sqlResults, String alias,
            ObjectStoreInterMineImpl os, Class type, Set noObjectClassColumns) throws SQLException {
        Set classes = Collections.singleton(type);
        if (!noObjectClassColumns.contains(alias)) {
            String objectClass = null;
            try {
                objectClass = sqlResults.getString(alias + "objectclass");
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
        InterMineObject retval = (InterMineObject) DynamicUtil.createObject(classes);
        Map fields = os.getModel().getFieldDescriptorsForClass(retval.getClass());
        Iterator iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String fieldName = (String) entry.getKey();
            FieldDescriptor fd = (FieldDescriptor) entry.getValue();
            if (fd instanceof AttributeDescriptor) {
                Object value = sqlResults.getObject(alias + DatabaseUtil.getColumnName(fd));
                if ((value instanceof Long) && Date.class.equals(TypeUtil.getFieldInfo(type,
                                fieldName).getType())) {
                    value = new Date(((Long) value).longValue());
                }
                TypeUtil.setFieldValue(retval, fieldName, value);
            } else if (fd instanceof CollectionDescriptor) {
                CollectionDescriptor cd = (CollectionDescriptor) fd;
                Collection lazyColl = new ProxyCollection(os, retval, cd.getName(),
                        cd.getReferencedClassDescriptor().getType());
                TypeUtil.setFieldValue(retval, cd.getName(), lazyColl);
            } else if (fd instanceof ReferenceDescriptor) {
                ReferenceDescriptor rd = (ReferenceDescriptor) fd;
                Integer id = new Integer(sqlResults.getInt(alias + DatabaseUtil.getColumnName(fd)));
                Class refType = rd.getReferencedClassDescriptor().getType();
                TypeUtil.setFieldValue(retval, fieldName, new ProxyReference(os, id, refType));
            }
        }
        return retval;
    }
}
