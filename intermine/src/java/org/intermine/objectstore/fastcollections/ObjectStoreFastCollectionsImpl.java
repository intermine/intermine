package org.intermine.objectstore.fastcollections;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
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
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.TypeUtil;

/**
 * Provides an implementation of an objectstore that explicitly materialises all the collections
 * in the results set it provides.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreFastCollectionsImpl extends ObjectStorePassthruImpl
{
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
     * @see ObjectStore#execute(Query)
     */
    public Results execute(Query q) throws ObjectStoreException {
        return new Results(q, this, getSequence());
    }

    /**
     * @see ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        try {
            List retval = os.execute(q, start, limit, optimise, explain, sequence);
            if (retval.size() > 1) {
                QueryNode node = (QueryNode) q.getSelect().get(0);
                if (node instanceof QueryClass) {
                    Map bagMap = new HashMap();
                    int lowestId = Integer.MAX_VALUE;
                    int highestId = Integer.MIN_VALUE;
                    Iterator rowIter = retval.iterator();
                    while (rowIter.hasNext()) {
                        Object o = ((ResultsRow) rowIter.next()).get(0);
                        bagMap.put(o, o);
                        int id = ((InterMineObject) o).getId().intValue();
                        lowestId = (lowestId < id ? lowestId : id);
                        highestId = (highestId > id ? highestId : id);
                    }
                    Set bag = bagMap.keySet();
                    Class clazz = ((QueryClass) node).getType();
                    Map fieldDescriptors = getModel().getFieldDescriptorsForClass(clazz);
                    Iterator fieldIter = fieldDescriptors.entrySet().iterator();
                    while (fieldIter.hasNext()) {
                        Map.Entry fieldEntry = (Map.Entry) fieldIter.next();
                        String fieldName = (String) fieldEntry.getKey();
                        FieldDescriptor field = (FieldDescriptor) fieldEntry.getValue();
                        if (field instanceof CollectionDescriptor) {
                            CollectionDescriptor coll = (CollectionDescriptor) field;
                            Iterator bagIter = bag.iterator();
                            if (HashSet.class.equals(coll.getCollectionClass())) {
                                while (bagIter.hasNext()) {
                                    TypeUtil.setFieldValue(bagIter.next(), fieldName,
                                            new HashSet());
                                }
                            } else {
                                while (bagIter.hasNext()) {
                                    TypeUtil.setFieldValue(bagIter.next(), fieldName,
                                            new ArrayList());
                                }
                            }
                            if ((q.getConstraint() == null) && q.getOrderBy().isEmpty()
                                    && q.getGroupBy().isEmpty()) {
                                Query subQ = new Query();
                                subQ.setDistinct(false);
                                QueryClass qc1 = new QueryClass(clazz);
                                QueryClass qc2 = new QueryClass(coll.getReferencedClassDescriptor()
                                        .getType());
                                subQ.addFrom(qc1);
                                subQ.addFrom(qc2);
                                subQ.addToSelect(qc1);
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
                                Results l = new Results(subQ, os, os.getSequence());
                                if (!optimise) {
                                    l.setNoOptimise();
                                }
                                if (!explain) {
                                    l.setNoExplain();
                                }
                                l.setBatchSize(limit * 2);
                                insertResults(bagMap, l, fieldName);
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
                                    subQ.addToSelect(qc1);
                                    subQ.addToSelect(qc2);
                                    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                                    subQ.setConstraint(cs);
                                    QueryCollectionReference qcr = new QueryCollectionReference(qc1,
                                            fieldName);
                                    cs.addConstraint(new ContainsConstraint(qcr,
                                                ConstraintOp.CONTAINS, qc2));
                                    cs.addConstraint(new BagConstraint(qc1, ConstraintOp.IN,
                                                bagList.subList(i, (i + 1000 < bagList.size()
                                                        ? i + 1000 : bagList.size()))));
                                    Results l = new Results(subQ, os, os.getSequence());
                                    if (!optimise) {
                                        l.setNoOptimise();
                                    }
                                    if (!explain) {
                                        l.setNoExplain();
                                    }
                                    l.setBatchSize(limit * 2);
                                    insertResults(bagMap, l, fieldName);
                                }
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

    private void insertResults(Map bagMap, List l, String fieldName) throws IllegalAccessException {
        Iterator lIter = l.iterator();
        while (lIter.hasNext()) {
            ResultsRow row = (ResultsRow) lIter.next();
            InterMineObject fromObj = (InterMineObject)
                bagMap.get(row.get(0));
            InterMineObject toObj = (InterMineObject) row.get(1);
            Collection fromCollection = (Collection) TypeUtil.getFieldValue(
                    fromObj, fieldName);
            fromCollection.add(toObj);
        }
    }
}

