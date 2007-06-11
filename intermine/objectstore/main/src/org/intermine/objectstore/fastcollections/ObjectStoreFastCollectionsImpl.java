package org.intermine.objectstore.fastcollections;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
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
import org.intermine.util.TypeUtil;

/**
 * Provides an implementation of an objectstore that explicitly materialises all the collections
 * in the results set it provides.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreFastCollectionsImpl extends ObjectStorePassthruImpl
{
    private boolean fetchAllFields = true;
    private Set fieldExceptions = Collections.EMPTY_SET;
    
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
    public void setFetchFields(boolean fetchAllFields, Set fieldExceptions) {
        this.fetchAllFields = fetchAllFields;
        this.fieldExceptions = fieldExceptions;
    }

    private boolean doThisField(FieldDescriptor field) {
        return fetchAllFields != fieldExceptions.contains(field);
    }

    /**
     * {@inheritDoc}
     */
    public Results execute(Query q) {
        return new Results(q, this, SEQUENCE_IGNORE);
    }

    /**
     * {@inheritDoc}
     */
    public SingletonResults executeSingleton(Query q) {
        return new SingletonResults(q, this, SEQUENCE_IGNORE);
    }

    /**
     * {@inheritDoc}
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            Map<Object, Integer> sequence) throws ObjectStoreException {
        try {
            List results = os.execute(q, start, limit, optimise, explain, sequence);
            CacheHoldingArrayList retval;
            if (results instanceof CacheHoldingArrayList) {
                retval = (CacheHoldingArrayList) results;
            } else {
                retval = new CacheHoldingArrayList(results);
            }
            if (retval.size() > 1) {
                QuerySelectable node = (QuerySelectable) q.getSelect().get(0);
                if (node instanceof QueryClass) {
                    Map bagMap = new HashMap();
                    int lowestId = Integer.MAX_VALUE;
                    int highestId = Integer.MIN_VALUE;
                    Iterator rowIter = retval.iterator();
                    while (rowIter.hasNext()) {
                        InterMineObject o = (InterMineObject) ((ResultsRow) rowIter.next()).get(0);
                        bagMap.put(o.getId(), o);
                        int id = ((InterMineObject) o).getId().intValue();
                        lowestId = (lowestId < id ? lowestId : id);
                        highestId = (highestId > id ? highestId : id);
                    }
                    Class clazz = ((QueryClass) node).getType();
                    Map fieldDescriptors = getModel().getFieldDescriptorsForClass(clazz);
                    Iterator fieldIter = fieldDescriptors.entrySet().iterator();
                    while (fieldIter.hasNext()) {
                        Map.Entry fieldEntry = (Map.Entry) fieldIter.next();
                        String fieldName = (String) fieldEntry.getKey();
                        FieldDescriptor field = (FieldDescriptor) fieldEntry.getValue();
                        Map collections = new HashMap();
                        if (doThisField(field) && (field instanceof CollectionDescriptor)) {
                            CollectionDescriptor coll = (CollectionDescriptor) field;
                            Iterator bagIter = bagMap.entrySet().iterator();
                            while (bagIter.hasNext()) {
                                Map.Entry entry = (Map.Entry) bagIter.next();
                                Integer id = (Integer) entry.getKey();
                                InterMineObject o = (InterMineObject) entry.getValue();
                                ProxyCollection pc = (ProxyCollection) TypeUtil
                                    .getFieldValue(o, fieldName);
                                Collection materialisedCollection = pc.getMaterialisedCollection();
                                if (!(materialisedCollection instanceof HashSet)) {
                                    materialisedCollection = new HashSet();
                                    collections.put(id, materialisedCollection);
                                }
                                retval.addToHolder(materialisedCollection);
                            }
                            Set bag = collections.keySet();
                            if ((q.getConstraint() == null) && q.getOrderBy().isEmpty()
                                    && q.getGroupBy().isEmpty()) {
                                // This is a shortcut query. We know that the original query is
                                // merely selecting everything from a certain class, ordered by id,
                                // so we can skip the BagConstraint and bound the id instead.
                                Query subQ = new Query();
                                subQ.setDistinct(false);
                                QueryClass qc1 = new QueryClass(clazz);
                                QueryClass qc2 = new QueryClass(coll.getReferencedClassDescriptor()
                                        .getType());
                                subQ.addFrom(qc1);
                                subQ.addFrom(qc2);
                                subQ.addToSelect(new QueryField(qc1, "id"));
                                subQ.addToSelect(qc2);
                                ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                                subQ.setConstraint(cs);
                                QueryCollectionReference qcr = new QueryCollectionReference(qc1,
                                        fieldName);
                                cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS,
                                            qc2));
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
                                Results l = new Results(subQ, os, sequence);
                                if (!optimise) {
                                    l.setNoOptimise();
                                }
                                if (!explain) {
                                    l.setNoExplain();
                                }
                                l.setBatchSize(limit * 20);
                                insertResults(collections, l, fieldName);
                            } else {
                                List bagList = new ArrayList(bag);
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
                                    if (!optimise) {
                                        l.setNoOptimise();
                                    }
                                    if (!explain) {
                                        l.setNoExplain();
                                    }
                                    l.setBatchSize(limit * 2);
                                    insertResults(collections, l, fieldName);
                                }
                            }
                            Iterator iter = collections.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry entry = (Map.Entry) iter.next();
                                Integer id = (Integer) entry.getKey();
                                Collection materialisedCollection = (Collection) entry.getValue();
                                InterMineObject fromObj = (InterMineObject) bagMap.get(id);;
                                ProxyCollection pc = (ProxyCollection) TypeUtil
                                    .getFieldValue(fromObj, fieldName);
                                pc.setMaterialisedCollection(materialisedCollection);
                            }
                        }
                    }
                }
            }
            return retval;
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }

    private void insertResults(Map collections, List l, String fieldName)
        throws IllegalAccessException {
        Iterator lIter = l.iterator();
        while (lIter.hasNext()) {
            ResultsRow row = (ResultsRow) lIter.next();
            Collection fromCollection = (Collection) collections.get(row.get(0));
            if (fromCollection != null) {
                InterMineObject toObj = (InterMineObject) row.get(1);
                fromCollection.add(toObj);
            }
        }
    }
}

