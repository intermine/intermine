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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStorePassthruImpl;
import org.flymine.objectstore.proxy.ProxyReference;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.util.IntPresentSet;
import org.flymine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Provides an implementation of an objectstore that explicitly materialises all the collections
 * in the results set it provides, written for the translating objectstore.
 *
 * Note that this ObjectStore <b>BREAKS</b> the ObjectStore interface, because it returns
 * collections that are not only solid (HashSet / ArrayList) implementations, but also contain
 * ProxyReferences in place of objects that it thinks the DataLoader will not need.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreFastCollectionsForTranslatorImpl extends ObjectStorePassthruImpl
{
    protected static final Logger LOG = Logger.getLogger(
            ObjectStoreFastCollectionsForTranslatorImpl.class);
    private IntPresentSet doneAlready = new IntPresentSet();

    /**
     * Creates an instance, from another ObjectStore instance.
     *
     * @param os an ObjectStore object to use
     */
    public ObjectStoreFastCollectionsForTranslatorImpl(ObjectStore os) {
        super(os);
    }

    /**
     * Gets an ObjectStoreFastCollectionsForTranslatorImpl instance for the given properties
     *
     * @param props the properties
     * @param model - thrown away
     * @return the ObjectStore
     * @throws IllegalArgumentException if props are invalid
     * @throws ObjectStoreException if there is a problem with the instance
     */
    public static ObjectStoreFastCollectionsForTranslatorImpl getInstance(Properties props,
            Model model) throws ObjectStoreException {
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
        return new ObjectStoreFastCollectionsForTranslatorImpl(os);
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
            synchronized (doneAlready) {
                if (retval.size() > 1) {
                    // The ItemToObjectTranslator creates collections by creating a query with a
                    // BagConstraint with all the IDs of all the objects that are in the collection.
                    // We should be able to read these queries and extract the IDs, and create a
                    // super-bag for use in a single query.
                    QueryNode node = (QueryNode) q.getSelect().get(0);
                    if (node instanceof QueryClass) {
                        Map froms = new HashMap();
                        Set toIds = new TreeSet();
                        Iterator rowIter = retval.iterator();
                        while (rowIter.hasNext()) {
                            FlyMineBusinessObject o = (FlyMineBusinessObject)
                                ((ResultsRow) rowIter.next()).get(0);
                            Map fromColls = new HashMap();
                            Map fieldDescriptors = getModel().getFieldDescriptorsForClass(o
                                    .getClass());
                            Iterator fieldIter = fieldDescriptors.entrySet().iterator();
                            while (fieldIter.hasNext()) {
                                Map.Entry fieldEntry = (Map.Entry) fieldIter.next();
                                String fieldName = (String) fieldEntry.getKey();
                                FieldDescriptor field = (FieldDescriptor) fieldEntry.getValue();
                                if (field.relationType() == FieldDescriptor.M_N_RELATION) {
                                    Object sr = TypeUtil.getFieldValue(o, fieldName);
                                    if (sr instanceof SingletonResults) {
                                        Query existingQ = ((SingletonResults) sr).getQuery();
                                        if ((existingQ.getFrom().size() == 1)
                                                && (existingQ.getConstraint()
                                                    instanceof BagConstraint)) {
                                            BagConstraint bc = (BagConstraint)
                                                existingQ.getConstraint();
                                            if ((bc.getQueryNode() instanceof QueryField)
                                                    && (bc.getOp() == ConstraintOp.IN)) {
                                                QueryField qf = (QueryField) bc.getQueryNode();
                                                if (qf.getFromElement().equals(existingQ.getFrom()
                                                            .iterator().next())
                                                        && qf.getFieldName().equals("id")) {
                                                    Set bag = bc.getBag();
                                                    fromColls.put(fieldName, bag);
                                                    toIds.addAll(bag);
                                                }
                                            }
                                        }
                                    }
                                } else if ((field.relationType() == FieldDescriptor
                                            .ONE_ONE_RELATION)
                                        || (field.relationType() == FieldDescriptor
                                            .N_ONE_RELATION)) {
                                    Object proxyObj = TypeUtil.getFieldProxy(o, fieldName);
                                    if (proxyObj instanceof ProxyReference) {
                                        Integer id = ((ProxyReference) proxyObj).getId();
                                        fromColls.put(fieldName, id);
                                        toIds.add(id);
                                    }
                                }
                            }
                            froms.put(o, fromColls);
                            doneAlready.add(o.getId());
                        }
                        // Now, froms is a Map from object to be populated to a Map from collection
                        // name to a Set of ids of objects that should be in the collection.
                        // toIds is a Set of all the IDs that we need to fetch in our query.

                        int toIdSize = toIds.size();

                        // Find all the objects we have in the cache.

                        Map idToObj = new HashMap();
                        Iterator toIdIter = toIds.iterator();
                        while (toIdIter.hasNext()) {
                            Integer toId = (Integer) toIdIter.next();
                            FlyMineBusinessObject toObj = os.pilferObjectById(toId);
                            if (toObj != null) {
                                idToObj.put(toId, toObj);
                                toIdIter.remove();
                            }
                        }
                        
                        int toIdSizeAfterCache = toIds.size();
                        
                        // Now, we don't need to load in objects that the dataloader has already
                        // handled. That includes basically everything we have seen ever.

                        toIdIter = toIds.iterator();
                        while (toIdIter.hasNext()) {
                            Integer toId = (Integer) toIdIter.next();
                            if (doneAlready.contains(toId)) {
                                toIdIter.remove();
                                //LOG.error("Avoiding loading object with id " + toId);
                            }
                        }

                        //if (toIdSize > 0) {
                        //    LOG.error("Fetching batch of collection items - size: " + toIdSize
                        //            + ", after cache: " + toIdSizeAfterCache
                        //            + ", after doneAlready: " + toIds.size());
                        //}

                        while (!toIds.isEmpty()) {
                            Set bag = new HashSet();
                            toIdIter = toIds.iterator();
                            for (int i = 0; (i < limit) && toIdIter.hasNext(); i++) {
                                Integer toId = (Integer) toIdIter.next();
                                bag.add(toId);
                                toIdIter.remove();
                            }

                            Query subQ = new Query();
                            subQ.setDistinct(false);
                            QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
                            subQ.addFrom(qc);
                            subQ.addToSelect(qc);
                            QueryField qf = new QueryField(qc, "id");
                            subQ.setConstraint(new BagConstraint(qf, ConstraintOp.IN, bag));

                            Results l = new SingletonResults(subQ, os, os.getSequence());
                            if (!optimise) {
                                l.setNoOptimise();
                            }
                            if (!explain) {
                                l.setNoExplain();
                            }
                            l.setBatchSize(limit * 2);
                            Iterator lIter = l.iterator();
                            while (lIter.hasNext()) {
                                FlyMineBusinessObject o = (FlyMineBusinessObject) lIter.next();
                                idToObj.put(o.getId(), o);
                                doneAlready.add(o.getId());
                            }
                        }
                        // Now we have fetched all the objects in from the database. We now need to
                        // populate every object in our froms Map

                        Iterator fromIter = froms.entrySet().iterator();
                        while (fromIter.hasNext()) {
                            Map.Entry fromEntry = (Map.Entry) fromIter.next();
                            FlyMineBusinessObject objToPopulate = (FlyMineBusinessObject) fromEntry
                                .getKey();
                            Map collectionsToPopulate = (Map) fromEntry.getValue();
                            Iterator collectionIter = collectionsToPopulate.entrySet().iterator();
                            while (collectionIter.hasNext()) {
                                Map.Entry collectionEntry = (Map.Entry) collectionIter.next();
                                String collectionName = (String) collectionEntry.getKey();
                                Object contents = collectionEntry.getValue();
                                if (contents instanceof Set) {
                                    Set collectionContents = (Set) collectionEntry.getValue();
                                    Collection substituteCollection = new HashSet();
                                    Iterator contentIter = collectionContents.iterator();
                                    while (contentIter.hasNext()) {
                                        Integer idToAdd = (Integer) contentIter.next();
                                        FlyMineBusinessObject objToAdd = (FlyMineBusinessObject)
                                            idToObj.get(idToAdd);
                                        if (objToAdd == null) {
                                            objToAdd = new ProxyReference(os, idToAdd);
                                        }
                                        substituteCollection.add(objToAdd);
                                    }
                                    TypeUtil.setFieldValue(objToPopulate, collectionName,
                                            substituteCollection);
                                } else {
                                    Integer id = (Integer) contents;
                                    FlyMineBusinessObject objToAdd = (FlyMineBusinessObject)
                                        idToObj.get(id);
                                    if (objToAdd != null) {
                                        TypeUtil.setFieldValue(objToPopulate, collectionName,
                                                objToAdd);
                                    }
                                }
                            }
                        }
                    }
                }
                return retval;
            }
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }
}

