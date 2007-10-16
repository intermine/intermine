package org.intermine.objectstore.intermine;

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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.BAGVAL_COLUMN;
import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.BAGID_COLUMN;
import static org.intermine.objectstore.intermine.ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.ObjectStoreBagCombination;
import org.intermine.objectstore.query.ObjectStoreBagsForObject;
import org.intermine.objectstore.query.OrderDescending;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryClassBag;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFieldPathExpression;
import org.intermine.objectstore.query.QueryForeignKey;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryOrderable;
import org.intermine.objectstore.query.QueryPathExpression;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SubqueryConstraint;
import org.intermine.objectstore.query.SubqueryExistsConstraint;
import org.intermine.objectstore.query.UnknownTypeValue;
import org.intermine.objectstore.query.ConstraintHelper;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.AlwaysMap;
import org.intermine.util.CombinedIterator;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;
import org.apache.torque.engine.database.model.Domain;
import org.apache.torque.engine.database.model.SchemaType;
import org.apache.torque.engine.platform.Platform;
import org.apache.torque.engine.platform.PlatformFactory;

/**
 * Code to generate an sql statement from a Query object.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 * @author Richard Smith
 */
public class SqlGenerator
{
    private static final Logger LOG = Logger.getLogger(SqlGenerator.class);
    protected static final int QUERY_NORMAL = 0;
    protected static final int QUERY_SUBQUERY_FROM = 1;
    protected static final int QUERY_SUBQUERY_CONSTRAINT = 2;
    protected static final int ID_ONLY = 2;
    protected static final int NO_ALIASES_ALL_FIELDS = 3;
    protected static final int QUERY_FOR_PRECOMP = 4;
    protected static final int QUERY_SUBQUERY_EXISTS = 5;

    protected static Map sqlCache = new WeakHashMap();
    protected static Map tablenamesCache = new WeakHashMap();

    /**
     * Generates a query to retrieve a single object from the database, by id.
     *
     * @param id the id of the object to fetch
     * @param clazz a Class of the object - if unsure use InterMineObject
     * @param schema the DatabaseSchema
     * @return a String suitable for passing to an SQL server
     * @throws ObjectStoreException if the given class is not in the model
     */
    public static String generateQueryForId(Integer id, Class clazz,
            DatabaseSchema schema) throws ObjectStoreException {
        ClassDescriptor tableMaster;
//        if (schema.isFlatMode(clazz)) {
//            Query q = new Query();
//            QueryClass qc = new QueryClass(clazz);
//            q.addFrom(qc);
//            q.addToSelect(qc);
//            q.setConstraint(new SimpleConstraint(new QueryField(qc, "id"), ConstraintOp.EQUALS,
//                        new QueryValue(id)));
//            q.setDistinct(false);
//            return generate(q, 0, 2, schema, null, null);
//        }
        if (schema.isMissingNotXml()) {
            tableMaster = schema.getModel()
                .getClassDescriptorByName(InterMineObject.class.getName());
        } else {
            ClassDescriptor cld = schema.getModel().getClassDescriptorByName(clazz.getName());
            if (cld == null) {
                throw new ObjectStoreException(clazz.toString() + " is not in the model");
            }
            tableMaster = schema.getTableMaster(cld);
        }
        if (schema.isTruncated(tableMaster)) {
            return "SELECT a1_.OBJECT AS a1_ FROM "
                + DatabaseUtil.getTableName(tableMaster) + " AS a1_ WHERE a1_.id = " + id.toString()
                + " AND a1_.class = '" + clazz.getName() + "' LIMIT 2";
        } else {
            return "SELECT a1_.OBJECT AS a1_ FROM "
                + DatabaseUtil.getTableName(tableMaster) + " AS a1_ WHERE a1_.id = " + id.toString()
                + " LIMIT 2";
        }
    }

    /**
     * Returns the table name used by the ID fetch query.
     *
     * @param clazz the Class of the object
     * @param schema the DatabaseSchema
     * @return a table name
     * @throws ObjectStoreException if the given class is not in the model
     */
    public static String tableNameForId(Class clazz,
            DatabaseSchema schema) throws ObjectStoreException {
        ClassDescriptor tableMaster;
        if (schema.isMissingNotXml()) {
            tableMaster = schema.getModel()
                .getClassDescriptorByName(InterMineObject.class.getName());
        } else {
            ClassDescriptor cld = schema.getModel().getClassDescriptorByName(clazz.getName());
            if (cld == null) {
                throw new ObjectStoreException(clazz.toString() + " is not in the model");
            }
            tableMaster = schema.getTableMaster(cld);
        }
        return DatabaseUtil.getTableName(tableMaster);
    }

    /**
     * Registers an offset for a given query. This is used later on to speed up queries that use
     * big offsets.
     *
     * @param q the Query
     * @param start the offset
     * @param schema the DatabaseSchema in which to look up metadata
     * @param db the Database that the ObjectStore uses
     * @param value a value, such that adding a WHERE component first_order_field &gt; value with
     *        OFFSET 0 is equivalent to the original query with OFFSET offset
     * @param bagTableNames a Map from BagConstraints to table names, where the table contains the
     *        contents of the bag that are relevant for the BagConstraint
     */
    public static void registerOffset(Query q, int start, DatabaseSchema schema, Database db,
                                      Object value, Map bagTableNames) {
        LOG.debug("registerOffset() called with offset: " + start);

        try {
            if (value.getClass().equals(Boolean.class)) {
                return;
            }
            QueryOrderable firstOrderByO = null;
            firstOrderByO = (QueryOrderable) q.getEffectiveOrderBy().iterator().next();
            if ((firstOrderByO instanceof QueryClass) && (!InterMineObject.class
                        .isAssignableFrom(((QueryClass) firstOrderByO).getType()))) {
                return;
            }
            synchronized (q) {
                Map schemaCache = getCacheForSchema(schema);
                CacheEntry cacheEntry = (CacheEntry) schemaCache.get(q);
                if (cacheEntry != null) {
                    if ((cacheEntry.getLastOffset() - start >= 100000)
                            || (start - cacheEntry.getLastOffset() >= 10000)) {
                        QueryNode firstOrderBy = null;
                        firstOrderBy = (QueryNode) q.getEffectiveOrderBy().iterator().next();
                        if (firstOrderBy instanceof QueryFunction) {
                            return;
                        }
                        if (firstOrderBy instanceof QueryClass) {
                            firstOrderBy = new QueryField((QueryClass) firstOrderBy, "id");
                        }
                        // Now we need to work out if this field is a primitive type or a object
                        // type (that can accept null values).
                        Constraint c = getOffsetConstraint(q, firstOrderBy, value, schema);
                        String sql = generate(q, schema, db, c, QUERY_NORMAL, bagTableNames);
                        cacheEntry.setLast(start, sql);
                    }
                    SortedMap headMap = cacheEntry.getCached().headMap(new Integer(start + 1));
                    Integer lastKey = null;
                    try {
                        lastKey = (Integer) headMap.lastKey();
                    } catch (NoSuchElementException e) {
                        // ignore
                    }
                    if (lastKey != null) {
                        int offset = lastKey.intValue();
                        if (start - offset < 100000) {
                            return;
                        }
                    }
                }
                // Now we need to work out if this field is a primitive type or a object
                // type (that can accept null values).
                Constraint offsetConstraint = getOffsetConstraint(q, firstOrderByO, value, schema);
                String sql = generate(q, schema, db, offsetConstraint, QUERY_NORMAL, bagTableNames);
                if (cacheEntry == null) {
                    cacheEntry = new CacheEntry(start, sql);
                    schemaCache.put(q, cacheEntry);
                }
                cacheEntry.getCached().put(new Integer(start), sql);
                //LOG.info("Created cache entry for offset " + start + " (cache contains "
                //    + cacheEntry.getCached().keySet() + ") for query " + q + ", sql = " + sql);
            }
        } catch (ObjectStoreException e) {
            LOG.error("Error while registering offset for query " + q + ": " + e);
        } catch (IllegalArgumentException e) {
        }
    }


    /**
     * Create a constraint to add to the main query to deal with offset - this is based on
     * the first element in the order by (field) and a given value (x).  If the order by
     * element cannot have null values this is: 'field &gt; x'.  If field can have null values
     * *and* it has not already been constrained as 'NOT NULL' in the main query it is:
     * '(field &gt; x or field IS NULL'.
     *
     * @param q the Query
     * @param firstOrderBy the offset element of the query's order by list
     * @param value a value, such that adding a WHERE component first_order_field &gt; value with
     *        OFFSET 0 is equivalent to the original query with OFFSET offset
     * @param schema the DatabaseSchema in which to look up metadata
     * @return the constraint(s) to add to the main query
     */
    protected static Constraint getOffsetConstraint(Query q, QueryOrderable firstOrderBy,
            Object value, DatabaseSchema schema) {
        boolean reverse = false;
        if (firstOrderBy instanceof OrderDescending) {
            firstOrderBy = ((OrderDescending) firstOrderBy).getQueryOrderable();
            reverse = true;
        }
        if (firstOrderBy instanceof QueryClass) {
            firstOrderBy = new QueryField((QueryClass) firstOrderBy, "id");
        }
        boolean hasNulls = true;
        if ((firstOrderBy instanceof QueryField) && (!reverse)) {
            FromElement qc = ((QueryField) firstOrderBy).getFromElement();
            if (qc instanceof QueryClass) {
                if ("id".equals(((QueryField) firstOrderBy).getFieldName())) {
                    hasNulls = false;
                } else {
                    AttributeDescriptor desc = (AttributeDescriptor) schema
                        .getModel().getFieldDescriptorsForClass(((QueryClass) qc)
                        .getType()).get(((QueryField) firstOrderBy)
                                        .getFieldName());
                    if (desc.isPrimitive()) {
                        hasNulls = false;
                    }
                }
            }
        }
        if (reverse) {
            return new SimpleConstraint((QueryEvaluable) firstOrderBy,
                    ConstraintOp.LESS_THAN, new QueryValue(value));
        } else {
            SimpleConstraint sc = new SimpleConstraint((QueryEvaluable) firstOrderBy,
                    ConstraintOp.GREATER_THAN, new QueryValue(value));
            if (hasNulls) {
                // if the query aready constrains the first order by field to be
                // not null it doesn't make sense to add a costraint to null
                CheckForIsNotNullConstraint check = new CheckForIsNotNullConstraint((QueryNode)
                        firstOrderBy);
                ConstraintHelper.traverseConstraints(q.getConstraint(), check);
                if (!check.exists()) {
                    ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
                    cs.addConstraint(sc);
                    cs.addConstraint(new SimpleConstraint((QueryEvaluable) firstOrderBy,
                                ConstraintOp.IS_NULL));
                    return cs;
                }
            }
            return sc;
        }
    }

    /**
     * Converts a Query object into an SQL String. To produce an SQL query that does not have
     * OFFSET and LIMIT clauses, set start to 0, and limit to Integer.MAX_VALUE.
     *
     * @param q the Query to convert
     * @param start the number of the first row for the query to return, numbered from zero
     * @param limit the maximum number of rows for the query to return
     * @param schema the DatabaseSchema in which to look up metadata
     * @param db the Database that the ObjectStore uses
     * @param bagTableNames a Map from BagConstraints to table names, where the table contains the
     *        contents of the bag that are relevant for the BagConstraint
     * @return a String suitable for passing to an SQL server
     * @throws ObjectStoreException if something goes wrong
     */
    public static String generate(Query q, int start, int limit, DatabaseSchema schema, Database db,
            Map bagTableNames) throws ObjectStoreException {
        synchronized (q) {
            Map schemaCache = getCacheForSchema(schema);
            CacheEntry cacheEntry = (CacheEntry) schemaCache.get(q);
            if (cacheEntry != null) {
                SortedMap headMap = cacheEntry.getCached().headMap(new Integer(start + 1));
                Integer lastKey = null;
                try {
                    lastKey = (Integer) headMap.lastKey();
                } catch (NoSuchElementException e) {
                    // ignore
                }
                if (lastKey != null) {
                    int offset = lastKey.intValue();
                    if ((offset > cacheEntry.getLastOffset())
                            || (cacheEntry.getLastOffset() > start)) {
                        return cacheEntry.getCached().get(lastKey)
                            + ((limit == Integer.MAX_VALUE ? "" : " LIMIT " + limit)
                                + (start == offset ? "" : " OFFSET " + (start - offset)));
                    } else {
                        return cacheEntry.getLastSQL()
                            + ((limit == Integer.MAX_VALUE ? "" : " LIMIT " + limit)
                                + (start == cacheEntry.getLastOffset() ? "" : " OFFSET "
                                    + (start - cacheEntry.getLastOffset())));
                    }
                }
            }
            String sql = generate(q, schema, db, null, QUERY_NORMAL, bagTableNames);
            /*if (cached == null) {
                cached = new TreeMap();
                schemaCache.put(q, cached);
            }
            cached.put(new Integer(0), sql);
            */
            return sql + ((limit == Integer.MAX_VALUE ? "" : " LIMIT " + limit)
                        + (start == 0 ? "" : " OFFSET " + start));
        }
    }

    /**
     * Returns a cache specific to a particular DatabaseSchema.
     *
     * @param schema the DatabaseSchema
     * @return a Map
     */
    private static Map getCacheForSchema(DatabaseSchema schema) {
        synchronized (sqlCache) {
            Map retval = (Map) sqlCache.get(schema);
            if (retval == null) {
                retval = Collections.synchronizedMap(new WeakHashMap());
                sqlCache.put(schema, retval);
            }
            return retval;
        }
    }

    /**
     * Converts a Query object into an SQL String.
     *
     * @param q the Query to convert
     * @param schema the DatabaseSchema in which to look up metadata
     * @param db the Database that the ObjectStore uses
     * @param offsetCon an additional constraint for improving the speed of large offsets
     * @param kind Query type
     * @param bagTableNames a Map from BagConstraints to table names, where the table contains the
     *        contents of the bag that are relevant for the BagConstraint
     * @return a String suitable for passing to an SQL server
     * @throws ObjectStoreException if something goes wrong
     */
    public static String generate(Query q, DatabaseSchema schema, Database db,
                                     Constraint offsetCon, int kind,
                                     Map bagTableNames) throws ObjectStoreException {
        State state = new State();
        List selectList = q.getSelect();
        if ((selectList.size() == 1) && (selectList.get(0) instanceof ObjectStoreBag)) {
            // Special case - we are fetching the contents of an ObjectStoreBag.
            return "SELECT " + BAGVAL_COLUMN + " AS a1_ FROM " + INT_BAG_TABLE_NAME + " WHERE "
                + BAGID_COLUMN + " = " + ((ObjectStoreBag) selectList.get(0)).getBagId()
                + " ORDER BY " + BAGVAL_COLUMN;
        } else if ((selectList.size() == 1)
                && (selectList.get(0) instanceof ObjectStoreBagCombination)) {
            // Another special case.
            ObjectStoreBagCombination osbc = (ObjectStoreBagCombination) selectList.get(0);
            if (osbc.getOp() == ObjectStoreBagCombination.UNION) {
                StringBuffer retval = new StringBuffer("SELECT DISTINCT " + BAGVAL_COLUMN
                        + " AS a1_ FROM " + INT_BAG_TABLE_NAME + " WHERE " + BAGID_COLUMN
                        + " IN (");
                boolean needComma = false;
                for (ObjectStoreBag osb : osbc.getBags()) {
                    if (needComma) {
                        retval.append(", ");
                    }
                    needComma = true;
                    retval.append(osb.getBagId() + "");
                }
                retval.append(") ORDER BY " + BAGVAL_COLUMN);
                return retval.toString();
            } else if (osbc.getOp() == ObjectStoreBagCombination.ALLBUTINTERSECT) {
                StringBuffer retval = new StringBuffer("SELECT " + BAGVAL_COLUMN
                        + " AS a1_ FROM " + INT_BAG_TABLE_NAME + " WHERE " + BAGID_COLUMN
                        + " IN (");
                boolean needComma = false;
                for (ObjectStoreBag osb : osbc.getBags()) {
                    if (needComma) {
                        retval.append(", ");
                    }
                    needComma = true;
                    retval.append(osb.getBagId() + "");
                }
                retval.append(") GROUP BY " + BAGVAL_COLUMN + " HAVING COUNT(*) < "
                        + osbc.getBags().size() + " ORDER BY " + BAGVAL_COLUMN);
                return retval.toString();
            } else {
                StringBuffer retval = new StringBuffer();
                boolean needComma = false;
                for (ObjectStoreBag osb : osbc.getBags()) {
                    if (needComma) {
                        retval.append(osbc.getOp() == ObjectStoreBagCombination.INTERSECT
                                ? " INTERSECT " : " EXCEPT ");
                    }
                    needComma = true;
                    retval.append("SELECT " + BAGVAL_COLUMN + " AS a1_ FROM " + INT_BAG_TABLE_NAME
                            + " WHERE " + BAGID_COLUMN + " = " + osb.getBagId());
                }
                retval.append(" ORDER BY a1_");
                return retval.toString();
            }
        } else if ((selectList.size() == 1)
                && (selectList.get(0) instanceof ObjectStoreBagsForObject)) {
            // Another special case.
            ObjectStoreBagsForObject osbfo = (ObjectStoreBagsForObject) selectList.get(0);
            StringBuffer retval = new StringBuffer("SELECT " + BAGID_COLUMN + " AS a1_ FROM "
                    + INT_BAG_TABLE_NAME + " WHERE " + BAGVAL_COLUMN + " = " + osbfo.getValue());
            Collection<ObjectStoreBag> bags = osbfo.getBags();
            if ((bags != null) && (!bags.isEmpty())) {
                retval.append(" AND " + BAGID_COLUMN + " IN (");
                boolean needComma = false;
                for (ObjectStoreBag osb : osbfo.getBags()) {
                    if (needComma) {
                        retval.append(", ");
                    }
                    needComma = true;
                    retval.append("" + osb.getBagId());
                }
                retval.append(")");
            }
            retval.append(" ORDER BY " + BAGID_COLUMN);
            return retval.toString();
        }
        state.setDb(db);
        state.setBagTableNames(bagTableNames);
        buildFromComponent(state, q, schema, bagTableNames);
        buildWhereClause(state, q, q.getConstraint(), schema);
        buildWhereClause(state, q, offsetCon, schema);
        String orderBy = ((kind == QUERY_NORMAL) || (kind == QUERY_FOR_PRECOMP)
                ? buildOrderBy(state, q, schema) : "");
        StringBuffer retval = new StringBuffer("SELECT ")
            .append(needsDistinct(q) ? "DISTINCT " : "")
            .append(buildSelectComponent(state, q, schema, kind))
            .append(state.getFrom())
            .append(state.getWhere())
            .append(buildGroupBy(q, schema, state))
            .append(state.getHaving())
            .append(orderBy);
        if ((q.getLimit() != Integer.MAX_VALUE) && (kind == QUERY_SUBQUERY_FROM)) {
            retval.append(" LIMIT " + q.getLimit());
        }

        return retval.toString();
    }

    /**
     * Returns true if this query requires a DISTINCT keyword in the generated SQL.
     *
     * @param q the Query
     * @return a boolean
     */
    protected static boolean needsDistinct(Query q) {
        if (!q.isDistinct()) {
            return false;
        }

        Set selectClasses = new HashSet();
        Iterator selectIter = q.getSelect().iterator();
        while (selectIter.hasNext()) {
            QuerySelectable n = (QuerySelectable) selectIter.next();
            if (n instanceof QueryClass) {
                selectClasses.add(n);
            } else if (n instanceof QueryField) {
                QueryField f = (QueryField) n;
                if ("id".equals(f.getFieldName())) {
                    FromElement qc = f.getFromElement();
                    if (qc instanceof QueryClass) {
                        selectClasses.add(qc);
                    }
                }
            }
        }

        boolean allPresent = true;
        Iterator fromIter = q.getFrom().iterator();
        while (fromIter.hasNext() && allPresent) {
            FromElement qc = (FromElement) fromIter.next();
            allPresent = selectClasses.contains(qc);
        }

        return !allPresent;
    }

    /**
     * Builds a Set of all table names that are touched by a given query.
     *
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param individualOsbs if true, adds individual ObjectStoreBags to the Set, otherwise just
     * adds the table name instead
     * @return a Set of table names
     * @throws ObjectStoreException if something goes wrong
     */
    public static Set findTableNames(Query q, DatabaseSchema schema, boolean individualOsbs)
    throws ObjectStoreException {
        synchronized (q) {
            Map schemaCache = getTablenamesCacheForSchema(schema);
            Set tablenames = (Set) schemaCache.get(q);
            if (tablenames == null) {
                tablenames = new HashSet();
                findTableNames(tablenames, q, schema, true, individualOsbs);
                schemaCache.put(q, tablenames);
            }
            return tablenames;
        }
    }

    /**
     * Returns a cache for table names specific to a particular DatabaseSchema.
     *
     * @param schema the DatabaseSchema
     * @return a Map
     */
    private static Map getTablenamesCacheForSchema(DatabaseSchema schema) {
        synchronized (tablenamesCache) {
            Map retval = (Map) tablenamesCache.get(schema);
            if (retval == null) {
                retval = Collections.synchronizedMap(new WeakHashMap());
                tablenamesCache.put(schema, retval);
            }
            return retval;
        }
    }

    /**
     * Adds table names to a Set of table names, from a given Query.
     *
     * @param tablenames a Set of table names - new names will be added here
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param addInterMineObject true if this method should normally add the InterMineObject
     * table to the Set
     * @param individualOsbs if true, adds individual ObjectStoreBags to the Set, otherwise just
     * adds the table name instead
     * @throws ObjectStoreException if something goes wrong
     */
    private static void findTableNames(Set tablenames, Query q,
            DatabaseSchema schema, boolean addInterMineObject, boolean individualOsbs)
    throws ObjectStoreException {
        if (completelyFalse(q.getConstraint())) {
            return;
        }
        findTableNamesInConstraint(tablenames, q.getConstraint(), schema, individualOsbs);
        Set fromElements = q.getFrom();
        Iterator fromIter = fromElements.iterator();
        while (fromIter.hasNext()) {
            FromElement fromElement = (FromElement) fromIter.next();
            if (fromElement instanceof QueryClass) {
                QueryClass qc = (QueryClass) fromElement;
                Set classes = DynamicUtil.decomposeClass(qc.getType());
                Iterator classIter = classes.iterator();
                while (classIter.hasNext()) {
                    Class cls = (Class) classIter.next();
                    ClassDescriptor cld = schema.getModel().getClassDescriptorByName(cls.getName());
                    if (cld == null) {
                        throw new ObjectStoreException(cls.toString() + " is not in the model");
                    }
                    ClassDescriptor tableMaster = schema.getTableMaster(cld);
                    tablenames.add(DatabaseUtil.getTableName(tableMaster));
                }
            } else if (fromElement instanceof Query) {
                Query subQ = (Query) fromElement;
                findTableNames(tablenames, subQ, schema, false, individualOsbs);
            } else if (fromElement instanceof QueryClassBag) {
                // Do nothing
            } else {
                throw new ObjectStoreException("Unknown FromElement: " + fromElement.getClass());
            }
        }
        String interMineObject = DatabaseUtil.getTableName(schema.getModel()
                .getClassDescriptorByName(InterMineObject.class.getName()));
        Iterator selectIter = q.getSelect().iterator();
        while (selectIter.hasNext()) {
            QuerySelectable selectable = (QuerySelectable) selectIter.next();
            if (selectable instanceof QueryClass) {
                if (addInterMineObject && schema.isMissingNotXml()) {
                    tablenames.add(interMineObject);
                }
            } else if (selectable instanceof QueryEvaluable) {
                // Do nothing
            } else if ((selectable instanceof QueryFieldPathExpression)
                    && ("id".equals(((QueryFieldPathExpression) selectable).getFieldName()))) {
                // Do nothing
            } else if (selectable instanceof QueryForeignKey) {
                // Do nothing
            } else if (selectable instanceof ObjectStoreBag) {
                if (individualOsbs) {
                    tablenames.add(selectable);
                } else {
                    tablenames.add(ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME);
                }
            } else if (selectable instanceof ObjectStoreBagCombination) {
                if (individualOsbs) {
                    tablenames.addAll(((ObjectStoreBagCombination) selectable).getBags());
                } else {
                    tablenames.add(ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME);
                }
            } else if (selectable instanceof ObjectStoreBagsForObject) {
                tablenames.add(ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME);
            } else {
                throw new ObjectStoreException("Illegal entry in SELECT list: "
                        + selectable.getClass());
            }
        }
    }

    /**
     * Adds table names to a Set of table names, from a given constraint.
     *
     * @param tablenames a Set of table names - new names will be added here
     * @param c the Constraint
     * @param schema the DatabaseSchema in which to look up metadata
     * @param individualOsbs if true, adds individual ObjectStoreBags to the Set, otherwise just
     * adds the table name instead
     * @throws ObjectStoreException if something goes wrong
     */
    private static void findTableNamesInConstraint(Set tablenames, Constraint c,
            DatabaseSchema schema, boolean individualOsbs) throws ObjectStoreException {
        if (c instanceof ConstraintSet) {
            Iterator conIter = ((ConstraintSet) c).getConstraints().iterator();
            while (conIter.hasNext()) {
                Constraint subC = (Constraint) conIter.next();
                findTableNamesInConstraint(tablenames, subC, schema, individualOsbs);
            }
        } else if (c instanceof SubqueryConstraint) {
            findTableNames(tablenames, ((SubqueryConstraint) c).getQuery(), schema, false,
                    individualOsbs);
        } else if (c instanceof SubqueryExistsConstraint) {
            findTableNames(tablenames, ((SubqueryExistsConstraint) c).getQuery(), schema, false,
                    individualOsbs);
        } else if (c instanceof ContainsConstraint) {
            ContainsConstraint cc = (ContainsConstraint) c;
            QueryReference ref = cc.getReference();
            if (ref instanceof QueryCollectionReference) {
                ReferenceDescriptor refDesc = (ReferenceDescriptor) schema.getModel()
                    .getFieldDescriptorsForClass(ref.getQcType()).get(ref.getFieldName());
                if (refDesc.relationType() == FieldDescriptor.M_N_RELATION) {
                    tablenames.add(DatabaseUtil.getIndirectionTableName((CollectionDescriptor)
                                refDesc));
                } else if (cc.getQueryClass() == null) {
                    tablenames.add(DatabaseUtil.getTableName(schema.getTableMaster(
                                    refDesc.getReferencedClassDescriptor())));
                }
            }
        } else if (c instanceof BagConstraint) {
            if (((BagConstraint) c).getOsb() != null) {
                if (individualOsbs) {
                    tablenames.add(((BagConstraint) c).getOsb());
                } else {
                    tablenames.add(ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME);
                }
            }
        } else if (!((c == null) || (c instanceof SimpleConstraint)
                    || (c instanceof ClassConstraint))) {
            throw new ObjectStoreException("Unknown constraint type: " + c.getClass());
        }
    }

    /**
     * Builds the FROM list for the SQL query.
     *
     * @param state the current Sql Query state
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param bagTableNames a Map from BagConstraint to temporary table name
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void buildFromComponent(State state, Query q, DatabaseSchema schema,
                                             Map bagTableNames)
            throws ObjectStoreException {
        Set fromElements = q.getFrom();
        Iterator fromIter = fromElements.iterator();
        while (fromIter.hasNext()) {
            FromElement fromElement = (FromElement) fromIter.next();
            if (fromElement instanceof QueryClass) {
                QueryClass qc = (QueryClass) fromElement;
                String baseAlias = DatabaseUtil.generateSqlCompatibleName((String) q.getAliases()
                        .get(qc));
                Set classes = DynamicUtil.decomposeClass(qc.getType());
                Map aliases = new LinkedHashMap();
                int sequence = 0;
                String lastAlias = "";
                Iterator classIter = classes.iterator();
                while (classIter.hasNext()) {
                    Class cls = (Class) classIter.next();
                    ClassDescriptor cld = schema.getModel().getClassDescriptorByName(cls.getName());
                    if (cld == null) {
                        throw new ObjectStoreException(cls.toString() + " is not in the model");
                    }
                    ClassDescriptor tableMaster = schema.getTableMaster(cld);
                    if (sequence == 0) {
                        aliases.put(cld, baseAlias);
                        state.addToFrom(DatabaseUtil.getTableName(tableMaster) + " AS "
                                + baseAlias);
                        if (schema.isTruncated(tableMaster)) {
                            if (state.getWhereBuffer().length() > 0) {
                                state.addToWhere(" AND ");
                            }
                            state.addToWhere(baseAlias + ".class = '" + cls.getName() + "'");
                        }
                    } else {
                        aliases.put(cld, baseAlias + "_" + sequence);
                        state.addToFrom(DatabaseUtil.getTableName(tableMaster) + " AS " + baseAlias
                                + "_" + sequence);
                        if (state.getWhereBuffer().length() > 0) {
                            state.addToWhere(" AND ");
                        }
                        state.addToWhere(baseAlias + lastAlias + ".id = " + baseAlias
                                + "_" + sequence + ".id");
                        lastAlias = "_" + sequence;
                        if (schema.isTruncated(tableMaster)) {
                            state.addToWhere(" AND " + baseAlias + "_" + sequence + ".class = '"
                                    + cls.getName() + "'");
                        }
                    }
                    sequence++;
                }
                Map fields = schema.getModel().getFieldDescriptorsForClass(qc.getType());
                Map fieldToAlias = state.getFieldToAlias(qc);
                Iterator fieldIter = null;
                if (schema.isFlatMode(qc.getType())) {
                    List iterators = new ArrayList();
                    ClassDescriptor cld = schema.getTableMaster((ClassDescriptor) schema.getModel()
                        .getClassDescriptorsForClass(qc.getType()).iterator().next());
                    DatabaseSchema.Fields dbsFields = schema.getTableFields(schema
                            .getTableMaster(cld));
                    iterators.add(dbsFields.getAttributes().iterator());
                    iterators.add(dbsFields.getReferences().iterator());
                    fieldIter = new CombinedIterator(iterators);
                } else {
                    fieldIter = fields.values().iterator();
                }
                while (fieldIter.hasNext()) {
                    FieldDescriptor field = (FieldDescriptor) fieldIter.next();
                    String name = field.getName();
                    Iterator aliasIter = aliases.entrySet().iterator();
                    while (aliasIter.hasNext()) {
                        Map.Entry aliasEntry = (Map.Entry) aliasIter.next();
                        ClassDescriptor cld = (ClassDescriptor) aliasEntry.getKey();
                        String alias = (String) aliasEntry.getValue();
                        if (cld.getAllFieldDescriptors().contains(field) || schema.isFlatMode(qc
                                    .getType())) {
                            fieldToAlias.put(name, alias + "." + DatabaseUtil.getColumnName(field));
                            break;
                        }
                    }
                }
                // Deal with OBJECT column
                if (schema.isMissingNotXml()) {
                    Iterator aliasIter = aliases.entrySet().iterator();
                    while (aliasIter.hasNext()) {
                        Map.Entry aliasEntry = (Map.Entry) aliasIter.next();
                        ClassDescriptor cld = (ClassDescriptor) aliasEntry.getKey();
                        String alias = (String) aliasEntry.getValue();
                        ClassDescriptor tableMaster = schema.getTableMaster(cld);
                        if (InterMineObject.class.equals(tableMaster.getType())) {
                            fieldToAlias.put("OBJECT", alias + ".OBJECT");
                            break;
                        }
                    }
                } else if (schema.isFlatMode(qc.getType())) {
                    // We never want an OBJECT column in this case. However, we may want an
                    // objectclass column.
                    fieldToAlias.put("objectclass", baseAlias + ".objectclass");
                } else {
                    fieldToAlias.put("OBJECT", baseAlias + ".OBJECT");
                }
            } else if (fromElement instanceof Query) {
                state.addToFrom("(" + generate((Query) fromElement, schema,
                                               state.getDb(), null, QUERY_SUBQUERY_FROM,
                                               bagTableNames) + ") AS "
                        + DatabaseUtil.generateSqlCompatibleName((String) q.getAliases()
                            .get(fromElement)));
                state.setFieldToAlias(fromElement, new AlwaysMap(DatabaseUtil
                            .generateSqlCompatibleName((String) (q.getAliases()
                                    .get(fromElement)))));
            } else if (fromElement instanceof QueryClassBag) {
                // The problem here is:
                // We do not know the column name for the "id" field, because this will use a
                // table like an indirection table or other class table. We need to have this id
                // column name available for QueryFields and for extra tables added to the query
                // that need to be tied to the original copy via the id column. This id column
                // name must be filled in by the ContainsConstraint code.
                // Therefore, we do nothing here.
            } else {
                throw new ObjectStoreException("Unknown FromElement: " + fromElement.getClass());
            }
        }
    }

    /**
     * Builds the WHERE clause for the SQL query.
     *
     * @param state the current Sql Query state
     * @param q the Query
     * @param c the Constraint
     * @param schema the DatabaseSchema in which to look up metadata
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void buildWhereClause(State state, Query q, Constraint c,
            DatabaseSchema schema) throws ObjectStoreException {
        if (c != null) {
            if (completelyFalse(c)) {
                throw new CompletelyFalseException();
            }
            if (completelyTrue(c)) {
                return;
            }
            LinkedList<Constraint> constraints = new LinkedList<Constraint>();
            boolean needWhereComma = state.getWhereBuffer().length() > 0;
            boolean needHavingComma = state.getHavingBuffer().length() > 0;
            constraints.add(c);
            while (!constraints.isEmpty()) {
                Constraint con = constraints.removeFirst();
                if ((con instanceof ConstraintSet)
                        && ((ConstraintSet) con).getOp().equals(ConstraintOp.AND)) {
                    constraints.addAll(0, ((ConstraintSet) con).getConstraints());
                } else {
                    boolean whs[] = whereHavingSafe(con, q);
                    if (whs[0]) {
                        StringBuffer buffer = state.getWhereBuffer();
                        if (needWhereComma) {
                            buffer.append(" AND ");
                        }
                        needWhereComma = true;
                        constraintToString(state, buffer, con, q, schema, SAFENESS_SAFE, true);
                    } else if (whs[1]) {
                        StringBuffer buffer = state.getHavingBuffer();
                        if (needHavingComma) {
                            buffer.append(" AND ");
                        }
                        needHavingComma = true;
                        constraintToString(state, buffer, con, q, schema, SAFENESS_SAFE, true);
                    } else {
                        throw new ObjectStoreException("Constraint " + con + " mixes WHERE"
                                + " and HAVING components");
                    }
                }
            }
        }
    }

    /**
     * Returns an array containing two boolean values. The first is whether this object is suitable
     * for use in a WHERE clause, and the second is whether the object is suitable for use in a
     * HAVING clause.
     *
     * @param o an Object of some kind
     * @param q the current Query
     * @return an array of two booleans
     * @throws ObjectStoreException if the object type is not recognised
     */
    protected static boolean[] whereHavingSafe(Object o, Query q) throws ObjectStoreException {
        if (o instanceof QueryField) {
            return new boolean[] {true, q.getGroupBy().contains(o)
                || q.getGroupBy().contains(((QueryField) o).getFromElement())};
        } else if (o instanceof QueryClass) {
            return new boolean[] {true, q.getGroupBy().contains(o)};
        } else if (o instanceof QueryValue) {
            return new boolean[] {true, true};
        } else if (o instanceof QueryFunction) {
            return new boolean[] {false, true};
        } else if (o instanceof QueryCast) {
            return whereHavingSafe(((QueryCast) o).getValue(), q);
        } else if (o instanceof QueryExpression) {
            QueryExpression qe = (QueryExpression) o;
            QueryEvaluable arg1 = qe.getArg1();
            QueryEvaluable arg2 = qe.getArg2();
            QueryEvaluable arg3 = qe.getArg3();
            boolean s[] = whereHavingSafe(arg1, q);
            boolean whereSafe = s[0];
            boolean havingSafe = s[1];
            if (arg2 != null) {
                s = whereHavingSafe(arg2, q);
                whereSafe = whereSafe && s[0];
                havingSafe = havingSafe && s[1];
            }
            if (arg3 != null) {
                s = whereHavingSafe(arg3, q);
                whereSafe = whereSafe && s[0];
                havingSafe = havingSafe && s[1];
            }
            return new boolean[] {whereSafe, havingSafe};
        } else if (o instanceof QueryForeignKey) {
            return new boolean[] {true, q.getGroupBy().contains(o)
                || q.getGroupBy().contains(((QueryForeignKey) o).getQueryClass())};
        } else if (o instanceof QueryReference) {
            QueryClass qc = ((QueryReference) o).getQueryClass();
            if (qc == null) {
                return new boolean[] {true, true};
            } else {
                return whereHavingSafe(qc, q);
            }
        } else if (o instanceof SimpleConstraint) {
            SimpleConstraint c = (SimpleConstraint) o;
            QueryEvaluable arg1 = c.getArg1();
            QueryEvaluable arg2 = c.getArg2();
            if (arg2 == null) {
                return whereHavingSafe(arg1, q);
            } else {
                boolean s1[] = whereHavingSafe(arg1, q);
                boolean s2[] = whereHavingSafe(arg2, q);
                return new boolean[] {s1[0] && s2[0], s1[1] && s2[1]};
            }
        } else if (o instanceof ConstraintSet) {
            boolean whereSafe = true;
            boolean havingSafe = true;
            for (Constraint c : ((ConstraintSet) o).getConstraints()) {
                boolean s[] = whereHavingSafe(c, q);
                whereSafe = whereSafe && s[0];
                havingSafe = havingSafe && s[1];
            }
            return new boolean[] {whereSafe, havingSafe};
        } else if (o instanceof BagConstraint) {
            return whereHavingSafe(((BagConstraint) o).getQueryNode(), q);
        } else if (o instanceof ClassConstraint) {
            ClassConstraint cc = (ClassConstraint) o;
            QueryClass arg1 = cc.getArg1();
            QueryClass arg2 = cc.getArg2QueryClass();
            boolean s[] = whereHavingSafe(arg1, q);
            boolean whereSafe = s[0];
            boolean havingSafe = s[1];
            if (arg2 != null) {
                s = whereHavingSafe(arg2, q);
                whereSafe = whereSafe && s[0];
                havingSafe = havingSafe && s[1];
            }
            return new boolean[] {whereSafe, havingSafe};
        } else if (o instanceof ContainsConstraint) {
            ContainsConstraint c = (ContainsConstraint) o;
            QueryReference arg1 = c.getReference();
            QueryClass arg2 = c.getQueryClass();
            boolean s[] = whereHavingSafe(arg1, q);
            boolean whereSafe = s[0];
            boolean havingSafe = s[1];
            if (arg2 != null) {
                s = whereHavingSafe(arg2, q);
                whereSafe = whereSafe && s[0];
                havingSafe = havingSafe && s[1];
            }
            return new boolean[] {whereSafe, havingSafe};
        } else if (o instanceof SubqueryConstraint) {
            QueryClass qc = ((SubqueryConstraint) o).getQueryClass();
            QueryEvaluable qe = ((SubqueryConstraint) o).getQueryEvaluable();
            if (qc != null) {
                return whereHavingSafe(qc, q);
            } else {
                return whereHavingSafe(qe, q);
            }
        } else if (o instanceof SubqueryExistsConstraint) {
            return new boolean[] {true, true};
        } else {
            throw new ObjectStoreException("Unrecognised object " + o);
        }
    }

    /**
     * Returns true if this constraint is always true, regardless of row values.
     *
     * @param con a Constraint
     * @return a boolean
     */
    protected static boolean completelyTrue(Constraint con) {
        if (con instanceof ConstraintSet) {
            ConstraintSet cs = (ConstraintSet) con;
            if (cs.getOp() == ConstraintOp.AND) {
                boolean retval = true;
                Iterator<Constraint> csIter = cs.getConstraints().iterator();
                while (csIter.hasNext() && retval) {
                    Constraint c = csIter.next();
                    retval = retval && completelyTrue(c);
                }
                return retval;
            } else if (cs.getOp() == ConstraintOp.OR) {
                boolean retval = false;
                Iterator<Constraint> csIter = cs.getConstraints().iterator();
                while (csIter.hasNext() && (!retval)) {
                    Constraint c = csIter.next();
                    retval = retval || completelyTrue(c);
                }
                return retval;
            } else if (cs.getOp() == ConstraintOp.NOR) {
                boolean retval = true;
                Iterator<Constraint> csIter = cs.getConstraints().iterator();
                while (csIter.hasNext() && retval) {
                    Constraint c = csIter.next();
                    retval = retval && completelyFalse(c);
                }
                return retval;
            } else if (cs.getOp() == ConstraintOp.NAND) {
                boolean retval = false;
                Iterator<Constraint> csIter = cs.getConstraints().iterator();
                while (csIter.hasNext() && retval) {
                    Constraint c = csIter.next();
                    retval = retval || completelyFalse(c);
                }
                return retval;
            } else {
                throw new IllegalArgumentException("Invalid operation " + cs.getOp());
            }
        } else if (con instanceof BagConstraint) {
            BagConstraint bc = (BagConstraint) con;
            return (bc.getBag() != null) && bc.getBag().isEmpty()
                && (bc.getOp() == ConstraintOp.NOT_IN);
        }
        return false;
    }

    /**
     * Returns true if this constraint is always false, regardless of row values.
     *
     * @param con a Constraint
     * @return a boolean
     */
    protected static boolean completelyFalse(Constraint con) {
        if (con instanceof ConstraintSet) {
            ConstraintSet cs = (ConstraintSet) con;
            if (cs.getOp() == ConstraintOp.AND) {
                boolean retval = false;
                Iterator<Constraint> csIter = cs.getConstraints().iterator();
                while (csIter.hasNext() && (!retval)) {
                    Constraint c = csIter.next();
                    retval = retval || completelyFalse(c);
                }
                return retval;
            } else if (cs.getOp() == ConstraintOp.OR) {
                boolean retval = true;
                Iterator<Constraint> csIter = cs.getConstraints().iterator();
                while (csIter.hasNext() && retval) {
                    Constraint c = csIter.next();
                    retval = retval && completelyFalse(c);
                }
                return retval;
            } else if (cs.getOp() == ConstraintOp.NOR) {
                boolean retval = false;
                Iterator<Constraint> csIter = cs.getConstraints().iterator();
                while (csIter.hasNext() && (!retval)) {
                    Constraint c = csIter.next();
                    retval = retval || completelyTrue(c);
                }
                return retval;
            } else if (cs.getOp() == ConstraintOp.NAND) {
                boolean retval = true;
                Iterator<Constraint> csIter = cs.getConstraints().iterator();
                while (csIter.hasNext() && retval) {
                    Constraint c = csIter.next();
                    retval = retval && completelyTrue(c);
                }
                return retval;
            } else {
                throw new IllegalArgumentException("Invalid operation " + cs.getOp());
            }
        } else if (con instanceof BagConstraint) {
            BagConstraint bc = (BagConstraint) con;
            return (bc.getBag() != null) && bc.getBag().isEmpty()
                && (bc.getOp() == ConstraintOp.IN);
        }
        return false;
    }

    /** Safeness value indicating a situation safe for ContainsConstraint CONTAINS */
    public static final int SAFENESS_SAFE = 1;
    /** Safeness value indicating a situation safe for ContainsConstraint DOES NOT CONTAIN */
    public static final int SAFENESS_ANTISAFE = -1;
    /** Safeness value indicating a situation unsafe for ContainsConstraint */
    public static final int SAFENESS_UNSAFE = 0;

    /**
     * Converts a Constraint object into a String suitable for putting in an SQL query.
     *
     * @param state the current SqlGenerator state
     * @param buffer the StringBuffer to place text into
     * @param c the Constraint object
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param safeness the ContainsConstraint safeness parameter
     * @param loseBrackets true if an AND ConstraintSet can be represented safely without
     * surrounding parentheses
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void constraintToString(State state, StringBuffer buffer, Constraint c,
            Query q, DatabaseSchema schema, int safeness,
            boolean loseBrackets) throws ObjectStoreException {
        if ((safeness != SAFENESS_SAFE) && (safeness != SAFENESS_ANTISAFE)
                & (safeness != SAFENESS_UNSAFE)) {
            throw new ObjectStoreException("Unknown ContainsConstraint safeness: " + safeness);
        }
        if (c instanceof ConstraintSet) {
            constraintSetToString(state, buffer, (ConstraintSet) c, q, schema, safeness,
                    loseBrackets);
        } else if (c instanceof SimpleConstraint) {
            simpleConstraintToString(state, buffer, (SimpleConstraint) c, q);
        } else if (c instanceof SubqueryConstraint) {
            subqueryConstraintToString(state, buffer, (SubqueryConstraint) c, q, schema);
        } else if (c instanceof SubqueryExistsConstraint) {
            subqueryExistsConstraintToString(state, buffer, (SubqueryExistsConstraint) c, q,
                    schema);
        } else if (c instanceof ClassConstraint) {
            classConstraintToString(state, buffer, (ClassConstraint) c, q, schema);
        } else if (c instanceof ContainsConstraint) {
            containsConstraintToString(state, buffer, (ContainsConstraint) c, q, schema, safeness,
                    loseBrackets);
        } else if (c instanceof BagConstraint) {
            bagConstraintToString(state, buffer, (BagConstraint) c, q, schema, safeness);
        } else {
            throw (new ObjectStoreException("Unknown constraint type: " + c));
        }
    }

    /**
     * Converts a ConstraintSet object into a String suitable for putting in an SQL query.
     *
     * @param state the current SqlGenerator state
     * @param buffer the StringBuffer to place text into
     * @param c the ConstraintSet object
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param safeness the ContainsConstraint safeness parameter
     * @param loseBrackets true if an AND ConstraintSet can be represented safely without
     * surrounding parentheses
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void constraintSetToString(State state, StringBuffer buffer, ConstraintSet c,
            Query q, DatabaseSchema schema, int safeness,
            boolean loseBrackets) throws ObjectStoreException {
        if ((safeness != SAFENESS_SAFE) && (safeness != SAFENESS_ANTISAFE)
                & (safeness != SAFENESS_UNSAFE)) {
            throw new ObjectStoreException("Unknown ContainsConstraint safeness: " + safeness);
        }
        ConstraintOp op = c.getOp();
        boolean negate = (op == ConstraintOp.NAND) || (op == ConstraintOp.NOR);
        boolean disjunctive = (op == ConstraintOp.OR) || (op == ConstraintOp.NOR);
        boolean andOrNor = (op == ConstraintOp.AND) || (op == ConstraintOp.NOR);
        int newSafeness;
        if (safeness == SAFENESS_UNSAFE) {
            newSafeness = SAFENESS_UNSAFE;
        } else if (c.getConstraints().size() == 1) {
            newSafeness = negate ? -safeness : safeness;
        } else if (safeness == (andOrNor ? SAFENESS_SAFE : SAFENESS_ANTISAFE)) {
            newSafeness = negate ? -safeness : safeness;
        } else {
            newSafeness = SAFENESS_UNSAFE;
        }
        if (c.getConstraints().isEmpty()) {
            buffer.append((disjunctive ? negate : !negate) ? "true" : "false");
        } else {
            buffer.append(negate ? "(NOT (" : (loseBrackets && (!disjunctive) ? "" : "("));
            boolean needComma = false;
            Map subqueryConstraints = new HashMap();
            Set constraints = c.getConstraints();
            Iterator constraintIter = constraints.iterator();
            while (constraintIter.hasNext()) {
                Constraint subC = (Constraint) constraintIter.next();
                if (disjunctive && (subC instanceof SubqueryConstraint)) {
                    SubqueryConstraint subQC = (SubqueryConstraint) subC;
                    Query subQCQuery = subQC.getQuery();
                    QueryEvaluable subQCEval = subQC.getQueryEvaluable();
                    QueryClass subQCClass = subQC.getQueryClass();
                    StringBuffer left = new StringBuffer();
                    if (subQCEval != null) {
                        queryEvaluableToString(left, subQCEval, q, state);
                    } else {
                        queryClassToString(left, subQCClass, q, schema, QUERY_SUBQUERY_CONSTRAINT,
                                state);
                    }
                    left.append(" " + subQC.getOp().toString() + " (");
                    StringBuffer existing = (StringBuffer) subqueryConstraints.get(left.toString());
                    if (existing == null) {
                        existing = new StringBuffer();
                        subqueryConstraints.put(left.toString(), existing);
                    } else {
                        existing.append(" UNION ");
                    }
                    existing.append(generate(subQCQuery, schema, state.getDb(), null,
                                QUERY_SUBQUERY_CONSTRAINT, state.getBagTableNames()));
                } else {
                    if ((disjunctive && completelyFalse(subC))
                            || ((!disjunctive) && completelyTrue(subC))) {
                        // This query can be skipped
                    } else {
                        if (needComma) {
                            buffer.append(disjunctive ? " OR " : " AND ");
                        }
                        needComma = true;
                        constraintToString(state, buffer, subC, q, schema, newSafeness, (!negate)
                                && (!disjunctive));
                    }
                }
            }
            Iterator subqueryConstraintIter = subqueryConstraints.entrySet().iterator();
            while (subqueryConstraintIter.hasNext()) {
                Map.Entry entry = (Map.Entry) subqueryConstraintIter.next();
                String left = (String) entry.getKey();
                String right = ((StringBuffer) entry.getValue()).toString();
                if (needComma) {
                    buffer.append(" OR ");
                }
                needComma = true;
                buffer.append(left);
                buffer.append(right);
                buffer.append(")");
            }
            buffer.append(negate ? "))" : (loseBrackets && (!disjunctive) ? "" : ")"));
        }
    }

    /**
     * Converts a SimpleConstraint object into a String suitable for putting in an SQL query.
     *
     * @param state the current SqlGenerator state
     * @param buffer the StringBuffer to place text into
     * @param c the SimpleConstraint object
     * @param q the Query
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void simpleConstraintToString(State state, StringBuffer buffer,
            SimpleConstraint c, Query q) throws ObjectStoreException {
        queryEvaluableToString(buffer, c.getArg1(), q, state);
        buffer.append(" " + c.getOp().toString());
        if (c.getArg2() != null) {
            buffer.append(" ");
            queryEvaluableToString(buffer, c.getArg2(), q, state);
        }
    }

    /**
     * Converts a SubqueryConstraint object into a String suitable for putting in an SQL query.
     *
     * @param state the current SqlGenerator state
     * @param buffer the StringBuffer to place text into
     * @param c the SubqueryConstraint object
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void subqueryConstraintToString(State state, StringBuffer buffer,
            SubqueryConstraint c, Query q, DatabaseSchema schema) throws ObjectStoreException {
        Query subQ = c.getQuery();
        QueryEvaluable qe = c.getQueryEvaluable();
        QueryClass cls = c.getQueryClass();
        if (qe != null) {
            queryEvaluableToString(buffer, qe, q, state);
        } else {
            queryClassToString(buffer, cls, q, schema, QUERY_SUBQUERY_CONSTRAINT,
                    state);
        }
        buffer.append(" " + c.getOp().toString() + " ("
                + generate(subQ, schema, state.getDb(), null, QUERY_SUBQUERY_CONSTRAINT,
                    state.getBagTableNames()) + ")");
    }

    /**
     * Converts a SubqueryExistsConstraint object into a String suitable for putting in an SQL
     * query.
     *
     * @param state the current SqlGenerator state
     * @param buffer the StringBuffer to place text into
     * @param c the SubqueryExistsConstraint object
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void subqueryExistsConstraintToString(State state, StringBuffer buffer,
            SubqueryExistsConstraint c, Query q,
            DatabaseSchema schema) throws ObjectStoreException {
        Query subQ = c.getQuery();
        buffer.append((c.getOp() == ConstraintOp.EXISTS ? "EXISTS(" : "(NOT EXISTS(")
                         + generate(subQ, schema, state.getDb(), null, QUERY_SUBQUERY_EXISTS,
                                    state.getBagTableNames())
                         + (c.getOp() == ConstraintOp.EXISTS ? ")" : "))"));
    }

    /**
     * Converts a ClassConstraint object into a String suitable for putting in an SQL query.
     *
     * @param state the current SqlGenerator state
     * @param buffer the StringBuffer to place text into
     * @param c the ClassConstraint object
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void classConstraintToString(State state, StringBuffer buffer,
            ClassConstraint c, Query q, DatabaseSchema schema) throws ObjectStoreException {
        QueryClass arg1 = c.getArg1();
        QueryClass arg2QC = c.getArg2QueryClass();
        InterMineObject arg2O = c.getArg2Object();
        queryClassToString(buffer, arg1, q, schema, ID_ONLY, state);
        buffer.append(" " + c.getOp().toString() + " ");
        if (arg2QC != null) {
            queryClassToString(buffer, arg2QC, q, schema, ID_ONLY, state);
        } else if (arg2O.getId() != null) {
            objectToString(buffer, arg2O);
        } else {
            throw new ObjectStoreException("ClassConstraint cannot contain an InterMineObject"
                    + " without an ID set");
        }
    }

    /**
     * Converts a ContainsConstraint object into a String suitable for putting in an SQL query.
     *
     * @param state the current SqlGenerator state
     * @param buffer the StringBuffer to place text into
     * @param c the ContainsConstraint object
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param safeness the ContainsConstraint safeness parameter
     * @param loseBrackets true if an AND ConstraintSet can be represented safely without
     * surrounding parentheses
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void containsConstraintToString(State state, StringBuffer buffer,
            ContainsConstraint c, Query q, DatabaseSchema schema, int safeness,
            boolean loseBrackets) throws ObjectStoreException {
        if ((safeness != SAFENESS_SAFE) && (safeness != SAFENESS_ANTISAFE)
                & (safeness != SAFENESS_UNSAFE)) {
            throw new ObjectStoreException("Unknown ContainsConstraint safeness: " + safeness);
        }
        QueryReference arg1 = c.getReference();
        QueryClass arg2 = c.getQueryClass();
        InterMineObject arg2Obj = c.getObject();
        Map fieldNameToFieldDescriptor = schema.getModel().getFieldDescriptorsForClass(arg1
                .getQcType());
        ReferenceDescriptor arg1Desc = (ReferenceDescriptor)
            fieldNameToFieldDescriptor.get(arg1.getFieldName());
        if (arg1Desc == null) {
            throw new ObjectStoreException("Reference "
                    + IqlQuery.queryReferenceToString(q, arg1, new ArrayList())
                    + "." + arg1.getFieldName() + " is not in the model - fields available in "
                    + arg1.getQcType() + " are " + fieldNameToFieldDescriptor.keySet());
        }
        if (arg1 instanceof QueryObjectReference) {
            String arg1Alias = (String) state.getFieldToAlias(arg1.getQueryClass()).get(arg1Desc
                    .getName());
            if (c.getOp().equals(ConstraintOp.IS_NULL) || c.getOp().equals(ConstraintOp
                        .IS_NOT_NULL)) {
                buffer.append(arg1Alias + " " + c.getOp().toString());
            } else {
                buffer.append(arg1Alias + (c.getOp() == ConstraintOp.CONTAINS ? " = " : " != "));
                if (arg2 == null) {
                    objectToString(buffer, arg2Obj);
                } else {
                    queryClassToString(buffer, arg2, q, schema, ID_ONLY, state);
                }
            }
        } else if (arg1 instanceof QueryCollectionReference) {
            InterMineObject arg1Obj = ((QueryCollectionReference) arg1).getQcObject();
            QueryClass arg1Qc = arg1.getQueryClass();
            QueryClassBag arg1Qcb = ((QueryCollectionReference) arg1).getQcb();
            if ((arg1Qcb != null) && (safeness != (c.getOp().equals(ConstraintOp.CONTAINS)
                            ? SAFENESS_SAFE : SAFENESS_ANTISAFE))) {
                throw new ObjectStoreException(safeness == SAFENESS_UNSAFE
                        ? "Invalid constraint: QueryClassBag ContainsConstraint cannot be inside"
                        + " an OR ConstraintSet"
                        : "Invalid constraint: DOES NOT CONTAINS cannot be applied to a"
                        + " QueryClassBag");
            }
            if (arg1Desc.relationType() == FieldDescriptor.ONE_N_RELATION) {
                if (arg2 == null) {
                    ReferenceDescriptor reverse = arg1Desc.getReverseReferenceDescriptor();
                    String indirectTableAlias = state.getIndirectAlias(); // Not really indirection
                    Map fieldToAlias = state.getFieldToAlias(arg1Qcb);
                    String arg2Alias = indirectTableAlias + "."
                        + DatabaseUtil.getColumnName(reverse);
                    ClassDescriptor tableMaster = schema.getTableMaster(reverse
                            .getClassDescriptor());
                    state.addToFrom(DatabaseUtil.getTableName(tableMaster) + " AS "
                            + indirectTableAlias);
                    buffer.append(loseBrackets ? "" : "(");
                    if (schema.isTruncated(tableMaster)) {
                        buffer.append(indirectTableAlias + ".class = '"
                                + reverse.getClassDescriptor().getType().getName() + "' AND ");
                    }
                    if (arg1Qc != null) {
                        queryClassToString(buffer, arg1Qc, q, schema, ID_ONLY, state);
                        buffer.append((c.getOp() == ConstraintOp.CONTAINS ? " = " : " != ")
                                + arg2Alias);
                    } else if (arg1Qcb != null) {
                        if (fieldToAlias.containsKey("id")) {
                            buffer.append(arg2Alias + " = " + fieldToAlias.get("id"));
                        } else {
                            fieldToAlias.put("id", arg2Alias);
                            if (arg1Qcb.getIds() == null) {
                                bagConstraintToString(state, buffer, new BagConstraint(new
                                            QueryField(arg1Qcb), ConstraintOp.IN,
                                            arg1Qcb.getOsb()), q,
                                        schema, SAFENESS_UNSAFE); // TODO: Not really unsafe
                            } else {
                                bagConstraintToString(state, buffer, new BagConstraint(
                                            new QueryField(arg1Qcb), (c.getOp() == ConstraintOp
                                                .CONTAINS ? ConstraintOp.IN : ConstraintOp.NOT_IN),
                                            arg1Qcb.getIds()), q, schema,
                                        SAFENESS_UNSAFE); // TODO: Not really unsafe
                            }
                        }
                    } else {
                        buffer.append(arg1Obj.getId() + (c.getOp() == ConstraintOp.CONTAINS
                                    ? " = " : " != ") + arg2Alias);
                    }
                    buffer.append(" AND " + indirectTableAlias + ".id = " + arg2Obj.getId());
                    buffer.append(loseBrackets ? "" : ")");
                } else {
                    String arg2Alias = (String) state.getFieldToAlias(arg2)
                        .get(arg1Desc.getReverseReferenceDescriptor().getName());
                    if (arg1Qc != null) {
                        queryClassToString(buffer, arg1Qc, q, schema, ID_ONLY, state);
                        buffer.append((c.getOp() == ConstraintOp.CONTAINS ? " = " : " != ")
                                + arg2Alias);
                    } else if (arg1Qcb != null) {
                        Map fieldToAlias = state.getFieldToAlias(arg1Qcb);
                        if (fieldToAlias.containsKey("id")) {
                            buffer.append(arg2Alias + " = " + fieldToAlias.get("id"));
                        } else {
                            fieldToAlias.put("id", arg2Alias);
                            if (arg1Qcb.getIds() == null) {
                                bagConstraintToString(state, buffer, new BagConstraint(
                                            new QueryField(arg1Qcb), ConstraintOp.IN,
                                            arg1Qcb.getOsb()), q, schema,
                                        SAFENESS_UNSAFE); // TODO: Not really unsafe
                            } else {
                                bagConstraintToString(state, buffer, new BagConstraint(
                                            new QueryField(arg1Qcb), (c.getOp() == ConstraintOp
                                                .CONTAINS ? ConstraintOp.IN : ConstraintOp.NOT_IN),
                                            arg1Qcb.getIds()), q, schema,
                                        SAFENESS_UNSAFE); // TODO: Not really unsafe
                            }
                        }
                    } else {
                        buffer.append("" + arg1Obj.getId()
                                + (c.getOp() == ConstraintOp.CONTAINS ? " = " : " != ")
                                + arg2Alias);
                    }
                }
            } else {
                if (safeness != (c.getOp().equals(ConstraintOp.CONTAINS) ? SAFENESS_SAFE
                            : SAFENESS_ANTISAFE)) {
                    throw new ObjectStoreException(safeness == SAFENESS_UNSAFE
                            ? "Cannot represent a many-to-many collection inside an OR"
                            + " ConstraintSet in SQL"
                            : "Cannot represent many-to-many collection DOES NOT CONTAIN in SQL");
                }
                CollectionDescriptor arg1ColDesc = (CollectionDescriptor) arg1Desc;
                String indirectTableAlias = state.getIndirectAlias();
                String arg2Alias = indirectTableAlias + "."
                    + DatabaseUtil.getInwardIndirectionColumnName(arg1ColDesc);
                state.addToFrom(DatabaseUtil.getIndirectionTableName(arg1ColDesc) + " AS "
                        + indirectTableAlias);
                buffer.append(loseBrackets ? "" : "(");
                if (arg1Qc != null) {
                    queryClassToString(buffer, arg1Qc, q, schema, ID_ONLY, state);
                    buffer.append(" = " + arg2Alias);
                } else if (arg1Qcb != null) {
                    Map fieldToAlias = state.getFieldToAlias(arg1Qcb);
                    if (fieldToAlias.containsKey("id")) {
                        buffer.append(arg2Alias + " = " + fieldToAlias.get("id"));
                    } else {
                        fieldToAlias.put("id", arg2Alias);
                        if (arg1Qcb.getIds() == null) {
                            bagConstraintToString(state, buffer, new BagConstraint(
                                        new QueryField(arg1Qcb), ConstraintOp.IN, arg1Qcb
                                        .getOsb()), q, schema,
                                    SAFENESS_UNSAFE); // TODO: Not really unsafe
                        } else {
                            bagConstraintToString(state, buffer, new BagConstraint(
                                        new QueryField(arg1Qcb), (c.getOp() == ConstraintOp
                                            .CONTAINS ? ConstraintOp.IN : ConstraintOp.NOT_IN),
                                        arg1Qcb.getIds()), q, schema,
                                    SAFENESS_UNSAFE); // TODO: Not really unsafe
                        }
                    }
                } else {
                    buffer.append(arg1Obj.getId() + " = " + arg2Alias);
                }
                buffer.append(" AND " + indirectTableAlias + "."
                        + DatabaseUtil.getOutwardIndirectionColumnName(arg1ColDesc) + " = ");
                if (arg2 == null) {
                    buffer.append("" + arg2Obj.getId());
                } else {
                    queryClassToString(buffer, arg2, q, schema, ID_ONLY, state);
                }
                buffer.append(loseBrackets ? "" : ")");
            }
        }
    }


    /**
     * The maximum size a bag in a BagConstraint can be before we consider using a temporary table
     * instead.
     */
    public static final int MAX_BAG_INLINE_SIZE = 2;

    /**
     * Converts a BagConstraint object into a String suitable for putting on an SQL query.
     *
     * @param state the current SqlGenerator state
     * @param buffer the StringBuffer to place text into
     * @param c the BagConstraint object
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param safeness the constraint context safeness
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void bagConstraintToString(State state, StringBuffer buffer, BagConstraint c,
            Query q, DatabaseSchema schema, int safeness) throws ObjectStoreException {
        Class type = c.getQueryNode().getType();
        String leftHandSide;
        if (c.getQueryNode() instanceof QueryEvaluable) {
            StringBuffer lhsBuffer = new StringBuffer();
            queryEvaluableToString(lhsBuffer, (QueryEvaluable) c.getQueryNode(), q, state);
            leftHandSide = lhsBuffer.toString();
        } else {
            StringBuffer lhsBuffer = new StringBuffer();
            queryClassToString(lhsBuffer, (QueryClass) c.getQueryNode(), q, schema, ID_ONLY, state);
            leftHandSide = lhsBuffer.toString();
        }
        SortedSet filteredBag = new TreeSet();
        Collection bagColl = c.getBag();
        if (bagColl == null) {
            ObjectStoreBag osb = c.getOsb();
            if (c.getOp() == ConstraintOp.IN) {
                buffer.append(leftHandSide);
            } else {
                buffer.append("(NOT (");
                buffer.append(leftHandSide);
            }

            if (((safeness == SAFENESS_SAFE) && (c.getOp() == ConstraintOp.IN))
                    || ((safeness == SAFENESS_ANTISAFE)
                        && (c.getOp() == ConstraintOp.NOT_IN))) {
                // We can move the temporary bag table to the FROM list.
                String indirectTableAlias = state.getIndirectAlias(); // Not really indirection
                state.addToFrom(ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME + " AS "
                        + indirectTableAlias);
                buffer.append(" = " + indirectTableAlias + "."
                        + ObjectStoreInterMineImpl.BAGVAL_COLUMN);
                buffer.append(" AND " + indirectTableAlias + "."
                        + ObjectStoreInterMineImpl.BAGID_COLUMN + " = " + osb.getBagId());
            } else {
                buffer.append(" IN (SELECT " + ObjectStoreInterMineImpl.BAGVAL_COLUMN
                        + " FROM ");
                buffer.append(ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME);
                buffer.append(" WHERE " + ObjectStoreInterMineImpl.BAGID_COLUMN + " = "
                        + osb.getBagId() + ")");
            }
            if (c.getOp() == ConstraintOp.NOT_IN) {
                buffer.append("))");
            }
        } else {
            Iterator bagIter = bagColl.iterator();
            //int lowest = Integer.MAX_VALUE;
            //int highest = Integer.MIN_VALUE;
            while (bagIter.hasNext()) {
                Object bagItem = bagIter.next();
                if (type.isInstance(bagItem)) {
                    if (bagItem instanceof InterMineObject) {
                        Integer bagValue = ((InterMineObject) bagItem).getId();
                        filteredBag.add(bagValue);
                    //    lowest = Math.min(bagValue.intValue(), lowest);
                    //    highest = Math.max(bagValue.intValue(), highest);
                    } else {
                        filteredBag.add(bagItem);
                    //    if (bagItem instanceof Integer) {
                    //        lowest = Math.min(((Integer) bagItem).intValue(), lowest);
                    //        highest = Math.max(((Integer) bagItem).intValue(), highest);
                    //    }
                    }
                }
            }
            if (filteredBag.isEmpty()) {
                buffer.append(c.getOp() == ConstraintOp.IN ? "false" : "true");
            } else {
                String bagTableName = (String) state.getBagTableNames().get(c);
                if (filteredBag.size() < MAX_BAG_INLINE_SIZE || bagTableName == null) {
                    int needComma = 0;
                    buffer.append(c.getOp() == ConstraintOp.IN ? "" : "(NOT (");
                    boolean limitRange = false;
                    //boolean limitRange = (lowest < highest) && (filteredBag.size() > 10);
                    //if (limitRange) {
                  //    buffer.append("(" + leftHandSide + " >= " + lowest + " AND " + leftHandSide
                    //            + " <= " + highest + " AND ");
                    //}
                    boolean parenthesesForGroups = (filteredBag.size() > 9000)
                        && ((c.getOp() == ConstraintOp.IN) || limitRange);
                    Iterator orIter = filteredBag.iterator();
                    while (orIter.hasNext()) {
                        if (needComma == 0) {
                            buffer.append((parenthesesForGroups ? "(" : "") + leftHandSide
                                    + " IN (");
                        } else if (needComma % 9000 == 0) {
                            buffer.append(") OR " + leftHandSide + " IN (");
                        } else {
                            buffer.append(", ");
                        }
                        needComma++;
                        StringBuffer constraint = new StringBuffer();
                        objectToString(buffer, orIter.next());
                    }
                    buffer.append(")");
                    //if (limitRange) {
                    //    buffer.append(")");
                    //}
                    if (parenthesesForGroups) {
                        buffer.append(")");
                    }
                    if (c.getOp() != ConstraintOp.IN) {
                        buffer.append("))");
                    }
                } else {
                    if (c.getOp() == ConstraintOp.IN) {
                        buffer.append(leftHandSide);
                    } else {
                        buffer.append("(NOT (");
                        buffer.append(leftHandSide);
                    }

                    if (((safeness == SAFENESS_SAFE) && (c.getOp() == ConstraintOp.IN))
                            || ((safeness == SAFENESS_ANTISAFE)
                                && (c.getOp() == ConstraintOp.NOT_IN))) {
                        // We can move the temporary bag table to the FROM list.
                        String indirectTableAlias = state.getIndirectAlias(); // Not really
                                                                              // indirection
                        state.addToFrom(bagTableName + " AS " + indirectTableAlias);
                        buffer.append(" = " + indirectTableAlias + ".value");
                    } else {
                        buffer.append(" IN (SELECT value FROM ");

                        buffer.append(bagTableName);
                        buffer.append(")");
                    }
                    if (c.getOp() == ConstraintOp.NOT_IN) {
                        buffer.append("))");
                    }
                }
            }
        }
    }

    /**
     * Converts an Object to a String, in a form suitable for SQL.
     *
     * @param buffer a StringBuffer to add text to
     * @param value the Object to convert
     * @throws ObjectStoreException if something goes wrong
     */
    public static void objectToString(StringBuffer buffer, Object value)
            throws ObjectStoreException {
        if (value instanceof UnknownTypeValue) {
            buffer.append(value.toString());
        } else if (value instanceof InterMineObject) {
            Integer id = ((InterMineObject) value).getId();
            if (id == null) {
                throw new ObjectStoreException("InterMineObject found"
                        + " without an ID set");
            }
            buffer.append(id.toString());
        } else if (value instanceof Date) {
            buffer.append(DatabaseUtil.objectToString(new Long(((Date) value).getTime())));
        } else {
            buffer.append(DatabaseUtil.objectToString(value));
        }
    }

    /**
     * Converts a QueryClass to a String.
     *
     * @param buffer the StringBuffer to add text to
     * @param qc the QueryClass to convert
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param kind the type of the output requested
     * @param state a State object
     * @throws ObjectStoreException if the model is internally inconsistent
     */
    protected static void queryClassToString(StringBuffer buffer, QueryClass qc, Query q,
            DatabaseSchema schema, int kind, State state) throws ObjectStoreException {
        if ((kind == ID_ONLY) && (!InterMineObject.class.isAssignableFrom(qc.getType()))) {
            throw new ObjectStoreException("QueryClass for non-InterMineObject class does not"
                    + " have an ID");
        }
        String alias = (String) q.getAliases().get(qc);
        Map fieldToAlias = state.getFieldToAlias(qc);
        if (alias == null) {
            throw new NullPointerException("A QueryClass is referenced by elements of a query,"
                    + " but the QueryClass is not in the FROM list of that query. QueryClass: "
                    + qc + ", aliases: " + q.getAliases());
        }
        if (kind == QUERY_SUBQUERY_EXISTS) {
            if (InterMineObject.class.isAssignableFrom(qc.getType())) {
                queryClassToString(buffer, qc, q, schema, QUERY_SUBQUERY_CONSTRAINT, state);
            } else {
                queryClassToString(buffer, qc, q, schema, NO_ALIASES_ALL_FIELDS, state);
            }
        } else if (kind == QUERY_SUBQUERY_CONSTRAINT) {
            buffer.append(DatabaseUtil.generateSqlCompatibleName(alias))
                .append(".id");
        } else {
            boolean needComma = false;
            String objectAlias = (String) state.getFieldToAlias(qc).get("OBJECT");
            if ((kind != QUERY_SUBQUERY_FROM) && (objectAlias != null)) {
                buffer.append(objectAlias);
                if ((kind == QUERY_NORMAL) || (kind == QUERY_FOR_PRECOMP)) {
                    buffer.append(" AS ")
                        .append(alias.equals(alias.toLowerCase())
                                ? DatabaseUtil.generateSqlCompatibleName(alias)
                                : "\"" + DatabaseUtil.generateSqlCompatibleName(alias) + "\"");
                }
                needComma = true;
            }
            if ((kind == QUERY_SUBQUERY_FROM) || (kind == NO_ALIASES_ALL_FIELDS)
                    || ((kind == QUERY_NORMAL) && schema.isFlatMode(qc.getType()))
                    || (kind == QUERY_FOR_PRECOMP)) {
                Iterator fieldIter = null;
                ClassDescriptor cld = schema.getModel().getClassDescriptorByName(qc.getType()
                        .getName());
                if (schema.isFlatMode(qc.getType()) && (kind == QUERY_NORMAL)) {
                    List iterators = new ArrayList();
                    DatabaseSchema.Fields fields = schema.getTableFields(schema
                            .getTableMaster(cld));
                    iterators.add(fields.getAttributes().iterator());
                    iterators.add(fields.getReferences().iterator());
                    fieldIter = new CombinedIterator(iterators);
                } else {
                    fieldIter = cld.getAllFieldDescriptors().iterator();
                }
                Map fieldMap = new TreeMap();
                while (fieldIter.hasNext()) {
                    FieldDescriptor field = (FieldDescriptor) fieldIter.next();
                    String columnName = DatabaseUtil.getColumnName(field);
                    if (columnName != null) {
                        fieldMap.put(columnName, field);
                    }
                }
                Iterator fieldMapIter = fieldMap.entrySet().iterator();
                while (fieldMapIter.hasNext()) {
                    Map.Entry fieldEntry = (Map.Entry) fieldMapIter.next();
                    FieldDescriptor field = (FieldDescriptor) fieldEntry.getValue();
                    String columnName = DatabaseUtil.getColumnName(field);

                    if (needComma) {
                        buffer.append(", ");
                    }
                    needComma = true;
                    buffer.append((String) fieldToAlias.get(field.getName()));
                    if (kind == QUERY_SUBQUERY_FROM) {
                        buffer.append(" AS ")
                            .append(DatabaseUtil.generateSqlCompatibleName(alias) + columnName);
                    } else if ((kind == QUERY_NORMAL) || (kind == QUERY_FOR_PRECOMP)) {
                        buffer.append(" AS ")
                            .append(alias.equals(alias.toLowerCase())
                                    ? DatabaseUtil.generateSqlCompatibleName(alias) + columnName
                                    : "\"" + DatabaseUtil.generateSqlCompatibleName(alias)
                                    + columnName.toLowerCase() + "\"");
                    }
                }
                if (schema.isFlatMode(qc.getType())
                        && schema.isTruncated(schema.getTableMaster(cld))) {
                    buffer.append(", ")
                        .append((String) fieldToAlias.get("objectclass"))
                        .append(" AS ")
                        .append(alias.equals(alias.toLowerCase())
                                ? DatabaseUtil.generateSqlCompatibleName(alias) + "objectclass"
                                : "\"" + DatabaseUtil.generateSqlCompatibleName(alias)
                                + "objectclass\"");
                }
            } else {
                if (needComma) {
                    buffer.append(", ");
                }
                buffer.append(DatabaseUtil.generateSqlCompatibleName(alias))
                    .append(".id AS ")
                    .append(alias.equals(alias.toLowerCase())
                            ? DatabaseUtil.generateSqlCompatibleName(alias) + "id"
                            : "\"" + DatabaseUtil.generateSqlCompatibleName(alias) + "id" + "\"");
            }
        }
    }

    /**
     * Converts a QueryEvaluable into a String suitable for an SQL query String.
     *
     * @param buffer the StringBuffer to add text to
     * @param node the QueryEvaluable
     * @param q the Query
     * @param state a State object
     * @throws ObjectStoreException if something goes wrong
     */
    protected static void queryEvaluableToString(StringBuffer buffer, QueryEvaluable node,
            Query q, State state) throws ObjectStoreException {
        if (node instanceof QueryField) {
            QueryField nodeF = (QueryField) node;
            FromElement nodeClass = nodeF.getFromElement();
            Map aliasMap = state.getFieldToAlias(nodeClass);
            String classAlias = (String) aliasMap.get(nodeF.getFieldName());

            buffer.append(classAlias);
            if (aliasMap instanceof AlwaysMap) {
                buffer.append(".")
                    .append(DatabaseUtil.generateSqlCompatibleName(nodeF.getFieldName()))
                    .append(nodeF.getSecondFieldName() == null
                            ? "" : DatabaseUtil.generateSqlCompatibleName(nodeF
                                .getSecondFieldName()));
            }
        } else if (node instanceof QueryExpression) {
            QueryExpression nodeE = (QueryExpression) node;
            if (nodeE.getOperation() == QueryExpression.SUBSTRING) {
                QueryEvaluable arg1 = nodeE.getArg1();
                QueryEvaluable arg2 = nodeE.getArg2();
                QueryEvaluable arg3 = nodeE.getArg3();

                buffer.append("SUBSTR(");
                queryEvaluableToString(buffer, arg1, q, state);
                buffer.append(", ");
                queryEvaluableToString(buffer, arg2, q, state);
                if (arg3 != null) {
                    buffer.append(", ");
                    queryEvaluableToString(buffer, arg3, q, state);
                }
                buffer.append(")");
            } else if (nodeE.getOperation() == QueryExpression.INDEX_OF) {
                QueryEvaluable arg1 = nodeE.getArg1();
                QueryEvaluable arg2 = nodeE.getArg2();

                buffer.append("STRPOS(");
                queryEvaluableToString(buffer, arg1, q, state);
                buffer.append(", ");
                queryEvaluableToString(buffer, arg2, q, state);
                buffer.append(")");
            } else if (nodeE.getOperation() == QueryExpression.LOWER) {
                buffer.append("LOWER(");
                queryEvaluableToString(buffer, nodeE.getArg1(), q, state);
                buffer.append(")");
            } else if (nodeE.getOperation() == QueryExpression.UPPER) {
                buffer.append("UPPER(");
                queryEvaluableToString(buffer, nodeE.getArg1(), q, state);
                buffer.append(")");
            } else {
                QueryEvaluable arg1 = nodeE.getArg1();
                QueryEvaluable arg2 = nodeE.getArg2();
                String op = null;
                switch (nodeE.getOperation()) {
                    case QueryExpression.ADD:
                        op = " + ";
                        break;
                    case QueryExpression.SUBTRACT:
                        op = " - ";
                        break;
                    case QueryExpression.MULTIPLY:
                        op = " * ";
                        break;
                    case QueryExpression.DIVIDE:
                        op = " / ";
                        break;
                    default:
                        throw (new ObjectStoreException("Invalid QueryExpression operation: "
                                    + nodeE.getOperation()));
                }
                buffer.append("(");
                queryEvaluableToString(buffer, arg1, q, state);
                buffer.append(op);
                queryEvaluableToString(buffer, arg2, q, state);
                buffer.append(")");
            }
        } else if (node instanceof QueryFunction) {
            QueryFunction nodeF = (QueryFunction) node;
            switch (nodeF.getOperation()) {
            case QueryFunction.COUNT:
                buffer.append("COUNT(*)");
                break;
            case QueryFunction.SUM:
                buffer.append("SUM(");
                queryEvaluableToString(buffer, nodeF.getParam(), q, state);
                buffer.append(")");
                break;
            case QueryFunction.AVERAGE:
                buffer.append("AVG(");
                queryEvaluableToString(buffer, nodeF.getParam(), q, state);
                buffer.append(")");
                break;
            case QueryFunction.MIN:
                buffer.append("MIN(");
                queryEvaluableToString(buffer, nodeF.getParam(), q, state);
                buffer.append(")");
                break;
            case QueryFunction.MAX:
                buffer.append("MAX(");
                queryEvaluableToString(buffer, nodeF.getParam(), q, state);
                buffer.append(")");
                break;
            case QueryFunction.STDDEV:
                buffer.append("STDDEV(");
                queryEvaluableToString(buffer, nodeF.getParam(), q, state);
                buffer.append(")");
                break;
            default:
                throw (new ObjectStoreException("Invalid QueryFunction operation: "
                            + nodeF.getOperation()));
            }
        } else if (node instanceof QueryValue) {
            QueryValue nodeV = (QueryValue) node;
            Object value = nodeV.getValue();
            objectToString(buffer, value);
        } else if (node instanceof QueryCast) {
            buffer.append("(");
            queryEvaluableToString(buffer, ((QueryCast) node).getValue(), q, state);
            buffer.append(")::");
            String torqueTypeName = TorqueModelOutput.generateJdbcType(node.getType()
                    .getName());
            SchemaType torqueType = SchemaType.getEnum(torqueTypeName);
            Platform torquePlatform = PlatformFactory.getPlatformFor(state.getDb().getPlatform()
                    .toLowerCase());
            Domain torqueDomain = torquePlatform.getDomainForSchemaType(torqueType);
            buffer.append(torqueDomain.getSqlType());
        } else if (node instanceof QueryForeignKey) {
            QueryForeignKey qor = (QueryForeignKey) node;
            buffer.append((String) state.getFieldToAlias(qor.getQueryClass()).get(qor
                        .getFieldName()));
        } else {
            throw (new ObjectStoreException("Invalid QueryEvaluable: " + node));
        }
    }

    /**
     * Builds a String representing the SELECT component of the Sql query.
     *
     * @param state the current Sql Query state
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param kind the kind of output requested
     * @return a String
     * @throws ObjectStoreException if something goes wrong
     */
    protected static String buildSelectComponent(State state, Query q, DatabaseSchema schema,
            int kind) throws ObjectStoreException {
        boolean needComma = false;
        StringBuffer retval = new StringBuffer();
        Iterator iter = q.getSelect().iterator();
        while (iter.hasNext()) {
            QuerySelectable node = (QuerySelectable) iter.next();
            String alias = (String) q.getAliases().get(node);
            if (node instanceof QueryClass) {
                if (needComma) {
                    retval.append(", ");
                }
                needComma = true;
                queryClassToString(retval, (QueryClass) node, q, schema, kind, state);
            } else if (node instanceof QueryEvaluable) {
                if (needComma) {
                    retval.append(", ");
                }
                needComma = true;
                queryEvaluableToString(retval, (QueryEvaluable) node, q, state);
                if ((kind == QUERY_NORMAL) || (kind == QUERY_FOR_PRECOMP)) {
                    retval.append(" AS " + (alias.equals(alias.toLowerCase())
                            ? DatabaseUtil.generateSqlCompatibleName(alias)
                            : "\"" + DatabaseUtil.generateSqlCompatibleName(alias) + "\""));
                } else if (kind == QUERY_SUBQUERY_FROM) {
                    retval.append(" AS " + DatabaseUtil.generateSqlCompatibleName(alias));
                }
            } else if ((node instanceof QueryFieldPathExpression)
                    && ("id".equals(((QueryFieldPathExpression) node).getFieldName()))) {
                QueryFieldPathExpression pe = (QueryFieldPathExpression) node;
                if (needComma) {
                    retval.append(", ");
                }
                needComma = true;
                // This is effectively a foreign key. Because we don't add this SELECT item to the
                // artificial ORDER BY list, we must assert that the QueryClass or its ID already
                // exists on the SELECT list - a weaker assertion than that we make for all
                // other QueryFieldPathExpressions anyway.
                if (!q.getSelect().contains(pe.getQueryClass())) {
                    throw new ObjectStoreException("Foreign key specified by"
                            + " QueryFieldPathExpression on SELECT list, but parent QueryClass not"
                            + " present on SELECT list");
                }
                retval.append((String) state.getFieldToAlias(pe.getQueryClass())
                        .get(pe.getReferenceName()))
                    .append(" AS ")
                    .append(DatabaseUtil.generateSqlCompatibleName(alias));
            } else if (node instanceof QueryPathExpression) {
                // Do nothing
            } else {
                throw new ObjectStoreException("Unknown object in SELECT list: " + node.getClass());
            }
        }
        iter = state.getOrderBy().iterator();
        while (iter.hasNext()) {
            String orderByField = (String) iter.next();
            if (needComma) {
                retval.append(", ");
            }
            needComma = true;
            retval.append(orderByField);
        }
        return retval.toString();
    }

    /**
     * Builds a String representing the GROUP BY component of the Sql query.
     *
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @param state a State object
     * @return a String
     * @throws ObjectStoreException if something goes wrong
     */
    protected static String buildGroupBy(Query q, DatabaseSchema schema,
            State state) throws ObjectStoreException {
        StringBuffer retval = new StringBuffer();
        boolean needComma = false;
        Iterator groupByIter = q.getGroupBy().iterator();
        while (groupByIter.hasNext()) {
            QueryNode node = (QueryNode) groupByIter.next();
            retval.append(needComma ? ", " : " GROUP BY ");
            needComma = true;
            if (node instanceof QueryClass) {
                queryClassToString(retval, (QueryClass) node, q, schema, NO_ALIASES_ALL_FIELDS,
                        state);
            } else {
                queryEvaluableToString(retval, (QueryEvaluable) node, q, state);
            }
        }
        return retval.toString();
    }

    /**
     * Builds a String representing the ORDER BY component of the Sql query.
     *
     * @param state the current Sql Query state
     * @param q the Query
     * @param schema the DatabaseSchema in which to look up metadata
     * @return a String
     * @throws ObjectStoreException if something goes wrong
     */
    protected static String buildOrderBy(State state, Query q, DatabaseSchema schema)
            throws ObjectStoreException {
        StringBuffer retval = new StringBuffer();
        HashSet seen = new HashSet();
        boolean needComma = false;
        Iterator orderByIter = q.getEffectiveOrderBy().iterator();
        while (orderByIter.hasNext()) {
            Object node = orderByIter.next();
            boolean desc = false;
            if (node instanceof OrderDescending) {
                desc = true;
                node = ((OrderDescending) node).getQueryOrderable();
            }
            if (!((node instanceof QueryValue) || (node instanceof QueryPathExpression))) {
                StringBuffer buffer = new StringBuffer();
                if (node instanceof QueryClass) {
                    if (TypeUtil.getFieldInfo(((QueryClass) node).getType(), "id") != null) {
                        queryClassToString(buffer, (QueryClass) node, q, schema, ID_ONLY, state);
                    } else {
                        queryClassToString(buffer, (QueryClass) node, q, schema,
                                NO_ALIASES_ALL_FIELDS, state);
                    }
                    if (!seen.contains(buffer.toString())) {
                        retval.append(needComma ? ", " : " ORDER BY ");
                        needComma = true;
                        retval.append(buffer.toString());
                        seen.add(buffer.toString());
                    }
                } else if (node instanceof QueryObjectReference) {
                    QueryObjectReference ref = (QueryObjectReference) node;
                    buffer.append((String) state.getFieldToAlias(ref.getQueryClass())
                            .get(ref.getFieldName()));
                    if (!seen.contains(buffer.toString())) {
                        retval.append(needComma ? ", " : " ORDER BY ");
                        needComma = true;
                        retval.append(buffer.toString());
                        seen.add(buffer.toString());
                        if (q.isDistinct()) {
                            if (q.getSelect().contains(ref)) {
                                // Nothing required
                            } else if (q.getSelect().contains(ref.getQueryClass())) {
                                // This means that the field's QueryClass is present in the SELECT
                                // list, so adding the field artificially will not alter the number
                                // of rows of a DISTINCT query.
                                if (!schema.isFlatMode(ref.getQueryClass().getType())) {
                                    buffer.append(" AS ")
                                        .append(state.getOrderByAlias());
                                    state.addToOrderBy(buffer.toString());
                                }
                            } else {
                                throw new ObjectStoreException("Reference " + buffer.toString()
                                        + " in the ORDER BY list must be in the SELECT list, or the"
                                        + " whole QueryClass must be in the SELECT list, or the"
                                        + " query made non-distinct");
                            }
                        }
                    }
                } else {
                    queryEvaluableToString(buffer, (QueryEvaluable) node, q, state);
                    if (!seen.contains(buffer.toString())) {
                        retval.append(needComma ? ", " : " ORDER BY ");
                        needComma = true;
                        retval.append(buffer.toString());
                        seen.add(buffer.toString());
                        if ((!q.getSelect().contains(node)) && q.isDistinct()
                                && (node instanceof QueryField)) {
                            FromElement fe = ((QueryField) node).getFromElement();
                            if (q.getSelect().contains(fe)) {
                                // This means that this field is not in the SELECT list, but its
                                // FromElement is, therefore adding it artificially to the SELECT
                                // list will not alter the number of rows of a DISTINCT query.
                                if (!schema.isFlatMode(InterMineObject.class)) {
                                    buffer.append(" AS ")
                                        .append(state.getOrderByAlias());
                                    state.addToOrderBy(buffer.toString());
                                }
                            } else if (fe instanceof QueryClass) {
                                throw new ObjectStoreException("Field " + buffer.toString()
                                        + " in the ORDER BY list must be in the SELECT list, or the"
                                        + " whole QueryClass " + fe.toString() + " must be in the"
                                        + " SELECT list, or the query made non-distinct");
                            } else {
                                throw new ObjectStoreException("Field " + buffer.toString()
                                        + " in the ORDER BY list must be in the SELECT list, or the"
                                        + " query made non-distinct");
                            }
                        }
                    }
                }
                if (desc) {
                    retval.append(" DESC");
                }
            }
        }
        return retval.toString();
    }

    private static class State
    {
        private StringBuffer whereText = new StringBuffer();
        private StringBuffer havingText = new StringBuffer();
        private StringBuffer fromText = new StringBuffer();
        private Set orderBy = new LinkedHashSet();
        private int number = 0;
        private Map fromToFieldToAlias = new HashMap();
        private Database db;

        // a Map from BagConstraints to table names, where the table contains the contents of the
        // bag that are relevant for the BagConstraint
        private Map bagTableNames = new HashMap();

        public State() {
            // empty
        }

        public String getWhere() {
            // a hacky fix for #731:
            String where = whereText.toString();
            //if (where.startsWith("(") && where.endsWith(")")) {
            //    where = where.substring(1, where.length() - 1);
            //}
            
            return (where.length() == 0 ? "" : " WHERE " + where);
        }

        public StringBuffer getWhereBuffer() {
            return whereText;
        }

        public String getHaving() {
            String having = havingText.toString();
            return (having.length() == 0 ? "" : " HAVING " + having);
        }

        public StringBuffer getHavingBuffer() {
            return havingText;
        }

        public String getFrom() {
            return fromText.toString();
        }

        public void addToWhere(String text) {
            whereText.append(text);
        }

        public void addToFrom(String text) {
            if (fromText.length() == 0) {
                fromText.append(" FROM ").append(text);
            } else {
                fromText.append(", ").append(text);
            }
        }

        public String getIndirectAlias() {
            return "indirect" + (number++);
        }

        public String getOrderByAlias() {
            return "orderbyfield" + (number++);
        }

        public void addToOrderBy(String s) {
            orderBy.add(s);
        }

        public Set getOrderBy() {
            return orderBy;
        }

        public Map getFieldToAlias(FromElement from) {
            Map retval = (Map) fromToFieldToAlias.get(from);
            if (retval == null) {
                retval = new HashMap();
                fromToFieldToAlias.put(from, retval);
            }
            return retval;
        }

        public void setFieldToAlias(FromElement from, Map map) {
            fromToFieldToAlias.put(from, map);
        }

        public void setBagTableNames(Map bagTableNames) {
            if (bagTableNames != null) {
                this.bagTableNames = bagTableNames;
            }
        }

        public Map getBagTableNames() {
            return bagTableNames;
        }

        public void setDb(Database db) {
            this.db = db;
        }

        public Database getDb() {
            return db;
        }
    }

    private static class CacheEntry
    {
        private TreeMap cached = new TreeMap();
        private int lastOffset;
        private String lastSQL;

        public CacheEntry(int lastOffset, String lastSQL) {
            this.lastOffset = lastOffset;
            this.lastSQL = lastSQL;
        }

        public TreeMap getCached() {
            return cached;
        }

        public void setLast(int lastOffset, String lastSQL) {
            this.lastOffset = lastOffset;
            this.lastSQL = lastSQL;
        }

        public int getLastOffset() {
            return lastOffset;
        }

        public String getLastSQL() {
            return lastSQL;
        }
    }
}
