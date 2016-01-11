package org.intermine.objectstore.fastcollections;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryForeignKey;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.CacheHoldingArrayList;

/**
 * Provides an implementation of an objectstore that explicitly materialises all the collections
 * in the results set it provides.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreFastCollectionsImpl extends ObjectStorePassthruImpl
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreFastCollectionsImpl.class);

    private boolean fetchAllFields = true;
    private Set<FieldDescriptor> fieldExceptions = Collections.emptySet();

    /**
     * Creates an instance, from another ObjectStore instance.
     *
     * @param os an ObjectStore object to use
     */
    public ObjectStoreFastCollectionsImpl(ObjectStore os) {
        super(os);
    }

    /**
     * Gets an ObjectStoreFastCollectionsImpl instance for the given properties
     *
     * @param osAlias the alias of this objectstore
     * @param props the properties
     * @return the ObjectStore
     * @throws IllegalArgumentException if props are invalid
     * @throws ObjectStoreException if there is a problem with the instance
     */
    public static ObjectStoreFastCollectionsImpl getInstance(String osAlias, Properties props)
        throws ObjectStoreException {
        String underlyingOsAlias = props.getProperty("os");
        if (underlyingOsAlias == null) {
            throw new IllegalArgumentException("No 'os' property specified for FastCollections"
                    + " objectstore");
        }
        ObjectStore objectStore;
        try {
            objectStore = ObjectStoreFactory.getObjectStore(underlyingOsAlias);
        } catch (Exception e) {
            throw new IllegalArgumentException("ObjectStore '" + underlyingOsAlias
                                               + "' not found in properties");
        }
        return new ObjectStoreFastCollectionsImpl(objectStore);
    }

    /**
     * Sets variables which determines which fields are prefetched. Set fetchAllFields to true if
     * you want all fields except those mentioned in the fieldExceptions set to be fetched. Set
     * fetchAllFields to false if you only want those fields mentioned in the fieldExceptions set
     * to be fetched.
     *
     * @param fetchAllFields a boolean
     * @param fieldExceptions a Set of FieldDescriptors
     */
    public void setFetchFields(boolean fetchAllFields, Set<FieldDescriptor> fieldExceptions) {
        this.fetchAllFields = fetchAllFields;
        this.fieldExceptions = fieldExceptions;
    }

    private boolean doThisField(FieldDescriptor field) {
        return fetchAllFields != fieldExceptions.contains(field);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Results execute(Query q) {
        return new Results(q, this, SEQUENCE_IGNORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Results execute(Query q, int batchSize, boolean optimise, boolean explain,
            boolean prefetch) {
        Results retval = new Results(q, this, getSequence(getComponentsForQuery(q)));
        if (batchSize != 0) {
            retval.setBatchSize(batchSize);
        }
        if (!optimise) {
            retval.setNoOptimise();
        }
        if (!explain) {
            retval.setNoExplain();
        }
        if (!prefetch) {
            retval.setNoPrefetch();
        }
        retval.setImmutable();
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingletonResults executeSingleton(Query q) {
        return new SingletonResults(q, this, SEQUENCE_IGNORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingletonResults executeSingleton(Query q, int batchSize, boolean optimise,
            boolean explain, boolean prefetch) {
        SingletonResults retval = new SingletonResults(q, this,
                getSequence(getComponentsForQuery(q)));
        if (batchSize != 0) {
            retval.setBatchSize(batchSize);
        }
        if (!optimise) {
            retval.setNoOptimise();
        }
        if (!explain) {
            retval.setNoExplain();
        }
        if (!prefetch) {
            retval.setNoPrefetch();
        }
        retval.setImmutable();
        return retval;
    }

    private long timeSpentExecute = 0;
    private long timeSpentInspect = 0;
    private long timeSpentPrepare = 0;
    private long timeSpentQuery = 0;
    private long timeSpentSubExecute = 0;
    private long timeSpentProcess = 0;
    private int queryCount = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResultsRow<Object>> execute(Query q, int start, int limit, boolean optimise,
            boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {
        try {
            long time1 = System.currentTimeMillis();
            List<ResultsRow<Object>> results = os.execute(q, start, limit, optimise, explain,
                    sequence);
            CacheHoldingArrayList<ResultsRow<Object>> retval;
            if (results instanceof CacheHoldingArrayList<?>) {
                retval = (CacheHoldingArrayList<ResultsRow<Object>>) results;
            } else {
                retval = new CacheHoldingArrayList<ResultsRow<Object>>(results);
            }
            long time2 = System.currentTimeMillis();
            timeSpentExecute += time2 - time1;
            if (retval.size() > 1) {
                QuerySelectable node = q.getSelect().get(0);
                if (node instanceof QueryClass) {
                    Map<Integer, InterMineObject> bagMap = new HashMap<Integer, InterMineObject>();
                    int lowestId = Integer.MAX_VALUE;
                    int highestId = Integer.MIN_VALUE;
                    for (ResultsRow<Object> row : retval) {
                        InterMineObject o = (InterMineObject) row.get(0);
                        bagMap.put(o.getId(), o);
                        int id = o.getId().intValue();
                        lowestId = (lowestId < id ? lowestId : id);
                        highestId = (highestId > id ? highestId : id);
                    }
                    Class<?> clazz = ((QueryClass) node).getType();
                    Map<String, FieldDescriptor> fieldDescriptors = getModel()
                        .getFieldDescriptorsForClass(clazz);
                    time1 = System.currentTimeMillis();
                    timeSpentInspect += time1 - time2;
                    for (Map.Entry<String, FieldDescriptor> fieldEntry : fieldDescriptors
                            .entrySet()) {
                        String fieldName = fieldEntry.getKey();
                        FieldDescriptor field = fieldEntry.getValue();
                        Map<Integer, Collection<Object>> collections
                            = new HashMap<Integer, Collection<Object>>();
                        if (doThisField(field) && (field instanceof CollectionDescriptor)) {
                            CollectionDescriptor coll = (CollectionDescriptor) field;
                            time1 = System.currentTimeMillis();
                            for (Map.Entry<Integer, InterMineObject> entry : bagMap.entrySet()) {
                                Integer id = entry.getKey();
                                InterMineObject o = entry.getValue();
                                @SuppressWarnings("unchecked") ProxyCollection<Object> pc
                                    = (ProxyCollection<Object>) o.getFieldValue(fieldName);
                                Collection<Object> materialisedCollection = pc
                                    .getMaterialisedCollection();
                                if (!(materialisedCollection instanceof HashSet<?>)) {
                                    materialisedCollection = new HashSet<Object>();
                                    collections.put(id, materialisedCollection);
                                }
                                retval.addToHolder(materialisedCollection);
                            }
                            time2 = System.currentTimeMillis();
                            timeSpentPrepare += time2 - time1;
                            Set<Integer> bag = collections.keySet();
                            if ((q.getConstraint() == null) && q.getOrderBy().isEmpty()
                                    && q.getGroupBy().isEmpty()) {
                                // This is a shortcut query. We know that the original query is
                                // merely selecting everything from a certain class, ordered by id,
                                // so we can skip the BagConstraint and bound the id instead.
                                Query subQ = new Query();
                                subQ.setDistinct(false);
                                if (coll.relationType() == FieldDescriptor.ONE_N_RELATION) {
                                    QueryClass qc = new QueryClass(coll
                                            .getReferencedClassDescriptor().getType());
                                    subQ.addFrom(qc);
                                    QueryForeignKey qfk = new QueryForeignKey(qc,
                                            coll.getReverseReferenceFieldName());
                                    subQ.addToSelect(qfk);
                                    subQ.addToSelect(qc);
                                    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                                    subQ.setConstraint(cs);
                                    cs.addConstraint(new SimpleConstraint(qfk,
                                                ConstraintOp.GREATER_THAN_EQUALS,
                                                new QueryValue(new Integer(lowestId))));
                                    cs.addConstraint(new SimpleConstraint(qfk,
                                                ConstraintOp.LESS_THAN_EQUALS,
                                                new QueryValue(new Integer(highestId))));
                                } else {
                                    QueryClass qc1 = new QueryClass(clazz);
                                    QueryClass qc2 = new QueryClass(coll
                                            .getReferencedClassDescriptor().getType());
                                    subQ.addFrom(qc1);
                                    subQ.addFrom(qc2);
                                    subQ.addToSelect(new QueryField(qc1, "id"));
                                    subQ.addToSelect(qc2);
                                    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                                    subQ.setConstraint(cs);
                                    QueryCollectionReference qcr = new QueryCollectionReference(qc1,
                                            fieldName);
                                    cs.addConstraint(new ContainsConstraint(qcr,
                                                ConstraintOp.CONTAINS, qc2));
                                    QueryField idField = new QueryField(qc1, "id");
                                    cs.addConstraint(new SimpleConstraint(idField,
                                                ConstraintOp.GREATER_THAN_EQUALS,
                                                new QueryValue(new Integer(lowestId))));
                                    cs.addConstraint(new SimpleConstraint(idField,
                                                ConstraintOp.LESS_THAN_EQUALS,
                                                new QueryValue(new Integer(highestId))));
                                    if (coll.relationType() == FieldDescriptor.ONE_N_RELATION) {
                                        QueryForeignKey reverseIdField = new QueryForeignKey(qc2,
                                                coll.getReverseReferenceFieldName());
                                        cs.addConstraint(new SimpleConstraint(reverseIdField,
                                                    ConstraintOp.GREATER_THAN_EQUALS,
                                                    new QueryValue(new Integer(lowestId))));
                                        cs.addConstraint(new SimpleConstraint(reverseIdField,
                                                    ConstraintOp.LESS_THAN_EQUALS,
                                                    new QueryValue(new Integer(highestId))));
                                    }
                                }
                                Results l = new Results(subQ, os, sequence);
                                // TODO: This bypasses the objectstore results cache.
                                if (!optimise) {
                                    l.setNoOptimise();
                                }
                                if (!explain) {
                                    l.setNoExplain();
                                }
                                l.setBatchSize(limit * 20);
                                time1 = System.currentTimeMillis();
                                timeSpentQuery += time1 - time2;
                                insertResults(collections, l);
                                time2 = System.currentTimeMillis();
                                timeSpentSubExecute += time2 - time1;
                            } else {
                                List<Integer> bagList = new ArrayList<Integer>(bag);
                                for (int i = 0; i < bagList.size(); i += 1000) {
                                    Query subQ = new Query();
                                    subQ.setDistinct(false);
                                    QueryClass qc1 = new QueryClass(clazz);
                                    QueryClass qc2 = new QueryClass(coll
                                            .getReferencedClassDescriptor().getType());
                                    subQ.addFrom(qc1);
                                    subQ.addFrom(qc2);
                                    subQ.addToSelect(new QueryField(qc1, "id"));
                                    subQ.addToSelect(qc2);
                                    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                                    subQ.setConstraint(cs);
                                    QueryCollectionReference qcr = new QueryCollectionReference(qc1,
                                            fieldName);
                                    cs.addConstraint(new ContainsConstraint(qcr,
                                                ConstraintOp.CONTAINS, qc2));
                                    cs.addConstraint(new BagConstraint(new QueryField(qc1, "id"),
                                                ConstraintOp.IN,
                                                bagList.subList(i, (i + 1000 < bagList.size()
                                                        ? i + 1000 : bagList.size()))));
                                    Results l = new Results(subQ, os, sequence);
                                    // TODO: This bypasses the objectstore results cache.
                                    if (!optimise) {
                                        l.setNoOptimise();
                                    }
                                    if (!explain) {
                                        l.setNoExplain();
                                    }
                                    l.setBatchSize(limit * 20);
                                    time1 = System.currentTimeMillis();
                                    timeSpentQuery += time1 - time2;
                                    insertResults(collections, l);
                                    time2 = System.currentTimeMillis();
                                    timeSpentSubExecute += time2 - time1;
                                }
                            }
                            for (Map.Entry<Integer, Collection<Object>> entry : collections
                                    .entrySet()) {
                                Integer id = entry.getKey();
                                Collection<Object> materialisedCollection = entry.getValue();
                                InterMineObject fromObj = bagMap.get(id);
                                @SuppressWarnings("unchecked") ProxyCollection<Object> pc
                                    = (ProxyCollection<Object>) fromObj.getFieldValue(fieldName);
                                pc.setMaterialisedCollection(materialisedCollection);
                            }
                            time1 = System.currentTimeMillis();
                            timeSpentProcess += time1 - time2;
                        }
                    }
                }
            }
            queryCount++;
            if (queryCount % 10000 == 0) {
                LOG.info("Time spent: Execute: " + timeSpentExecute + ", Inspect: "
                        + timeSpentInspect + ", Prepare: " + timeSpentPrepare + ", Generate query: "
                        + timeSpentQuery + ", Execute query: " + timeSpentSubExecute
                        + ", Process: " + timeSpentProcess);
            }
            return retval;
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }

    private void insertResults(Map<Integer, Collection<Object>> collections, Results l)
        throws IllegalAccessException {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Collection<ResultsRow<Object>> res = (Collection) l;
        for (ResultsRow<Object> row : res) {
            Collection<Object> fromCollection = collections.get(row.get(0));
            if (fromCollection != null) {
                Object toObj = row.get(1);
                fromCollection.add(toObj);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "FastCollections(" + os + ")";
    }
}
