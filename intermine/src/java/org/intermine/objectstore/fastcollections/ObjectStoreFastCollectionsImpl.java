package org.flymine.objectstore.fastcollections;

/*
 * Copyright (C) 2002-2003 FlyMine
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

import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStorePassthruImpl;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.util.TypeUtil;

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
     * @param props the properties
     * @param model - thrown away
     * @return the ObjectStore
     * @throws IllegalArgumentException if props are invalid
     * @throws ObjectStoreException if there is a problem with the instance
     */
    public static ObjectStoreFastCollectionsImpl getInstance(Properties props, Model model)
        throws ObjectStoreException {
        String osAlias = props.getProperty("os");
        if (osAlias == null) {
            throw new IllegalArgumentException("No 'os' property specified for FastCollections"
                    + " objectstore");
        }
        ObjectStore os;
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
        } catch (Exception e) {
            throw new IllegalArgumentException("ObjectStore '" + osAlias + "' not found in"
                    + " properties");
        }
        return new ObjectStoreFastCollectionsImpl(os);
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
                        int id = ((FlyMineBusinessObject) o).getId().intValue();
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
                            Query subQ = new Query();
                            QueryClass qc1 = new QueryClass(clazz);
                            QueryClass qc2 = new QueryClass(coll.getReferencedClassDescriptor()
                                    .getType());
                            subQ.addFrom(qc1);
                            subQ.addFrom(qc2);
                            subQ.addToSelect(qc1);
                            subQ.addToSelect(qc2);
                            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                            if (q.getConstraint() == null) {
                                QueryField idField = new QueryField(qc1, "id");
                                cs.addConstraint(new SimpleConstraint(idField,
                                            ConstraintOp.GREATER_THAN_EQUALS,
                                            new QueryValue(new Integer(lowestId))));
                                cs.addConstraint(new SimpleConstraint(idField,
                                            ConstraintOp.LESS_THAN_EQUALS,
                                            new QueryValue(new Integer(highestId))));
                            } else {
                                cs.addConstraint(new BagConstraint(qc1, ConstraintOp.IN, bag));
                            }
                            QueryCollectionReference qcr = new QueryCollectionReference(qc1,
                                    fieldName);
                            cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS,
                                        qc2));
                            subQ.setConstraint(cs);
                            Results l = new Results(subQ, os, os.getSequence());
                            if (!optimise) {
                                l.setNoOptimise();
                            }
                            if (!explain) {
                                l.setNoExplain();
                            }
                            l.setBatchSize(limit * 2);
                            Iterator lIter = l.iterator();
                            while (lIter.hasNext()) {
                                ResultsRow row = (ResultsRow) lIter.next();
                                FlyMineBusinessObject fromObj = (FlyMineBusinessObject)
                                    bagMap.get(row.get(0));
                                FlyMineBusinessObject toObj = (FlyMineBusinessObject) row.get(1);
                                Collection fromCollection = (Collection) TypeUtil.getFieldValue(
                                        fromObj, fieldName);
                                fromCollection.add(toObj);
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
}

