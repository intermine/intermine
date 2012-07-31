package org.intermine.objectstore.fastcollections;

/*
 * Copyright (C) 2002-2012 FlyMine
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

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemToObjectTranslator;
import org.intermine.dataloader.Source;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.translating.ObjectStoreTranslatingImpl;
import org.intermine.objectstore.translating.Translator;
import org.intermine.util.IntPresentSet;

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
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(
            ObjectStoreFastCollectionsForTranslatorImpl.class);
    private IntPresentSet doneAlready = new IntPresentSet();
    @SuppressWarnings("unused")
    private Source source = null;

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
     * @param osAlias the alias of this objectstore
     * @param props the properties
     * @return the ObjectStore
     * @throws IllegalArgumentException if props are invalid
     * @throws ObjectStoreException if there is a problem with the instance
     */
    public static ObjectStoreFastCollectionsForTranslatorImpl getInstance(
            @SuppressWarnings("unused") String osAlias,
            Properties props) throws ObjectStoreException {
        String underlyingOsAlias = props.getProperty("os");
        if (underlyingOsAlias == null) {
            throw new IllegalArgumentException("No 'os' property specified for FastCollections"
                                               + " objectstore");
        }
        ObjectStore objectStore;
        try {
            objectStore = ObjectStoreFactory.getObjectStore(underlyingOsAlias);
        } catch (Exception e) {
            // preserve ObjectStoreExceptions for more useful message
            Throwable t = e.getCause();
            if (t instanceof ObjectStoreException) {
                throw (ObjectStoreException) t;
            } else {
                throw new IllegalArgumentException("ObjectStore '" + underlyingOsAlias
                                                   + "' not found in properties", e);
            }
        }
        return new ObjectStoreFastCollectionsForTranslatorImpl(objectStore);
    }

    /**
     * Sets the source of the data, so we know what the primary keys are, and can fetch that extra
     * data.
     *
     * @param source a Source object
     */
    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * Returns the doneAlready Set, for logging purposes
     *
     * @return an IntPresentSet
     */
    public IntPresentSet getDoneAlready() {
        return doneAlready;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResultsRow<Object>> execute(Query q, int start, int limit, boolean optimise,
            boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {
        try {
            List<ResultsRow<Object>> retval = os.execute(q, start, limit, optimise, explain,
                    sequence);
            synchronized (doneAlready) {
                if (retval.size() > 1) {
                    // The ItemToObjectTranslator creates collections by creating a query with a
                    // BagConstraint with all the IDs of all the objects that are in the collection.
                    // We should be able to read these queries and extract the IDs, and create a
                    // super-bag for use in a single query.
                    QuerySelectable node = q.getSelect().get(0);
                    if (node instanceof QueryClass) {
                        Map<FastPathObject, Map<String, Object>> froms =
                            new HashMap<FastPathObject, Map<String, Object>>();
                        Set<Integer> toIds = new TreeSet<Integer>();
                        Set<Integer> toAddToDoneAlready = new HashSet<Integer>();
                        Map<Integer, FastPathObject> idToObj =
                            new HashMap<Integer, FastPathObject>();
                        for (ResultsRow<Object> row : retval) {
                            FastPathObject o = (FastPathObject) row.get(0);
                            Map<String, Object> fromColls =
                                new HashMap<String, Object>();
                            Map<String, FieldDescriptor> fieldDescriptors = getModel()
                                .getFieldDescriptorsForClass(o.getClass());
                            for (Map.Entry<String, FieldDescriptor> fieldEntry
                                    : fieldDescriptors.entrySet()) {
                                String fieldName = fieldEntry.getKey();
                                FieldDescriptor field = fieldEntry.getValue();
                                if (field.relationType() == FieldDescriptor.M_N_RELATION) {
                                    Object sr = o.getFieldValue(fieldName);
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
                                                        && "id".equals(qf.getFieldName())) {
                                                    // We know that the bag is of integers. We can't
                                                    // persuade Java of this, so we need to copy.
                                                    Collection<?> bag = bc.getBag();
                                                    fromColls.put(fieldName, bag);
                                                    for (Object bagItem : bag) {
                                                        toIds.add((Integer) bagItem);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if ((field.relationType() == FieldDescriptor
                                            .ONE_ONE_RELATION)
                                        || (field.relationType() == FieldDescriptor
                                            .N_ONE_RELATION)) {
                                    Object proxyObj = o.getFieldProxy(fieldName);
                                    if (proxyObj instanceof ProxyReference) {
                                        Integer id = ((ProxyReference) proxyObj).getId();
                                        fromColls.put(fieldName, id);
                                        toIds.add(id);
                                    }
                                }
                            }
                            froms.put(o, fromColls);
                            if (o instanceof InterMineObject) {
                                toAddToDoneAlready.add(((InterMineObject) o).getId());
                                idToObj.put(((InterMineObject) o).getId(), o);
                            }
                        }
                        // Now, froms is a Map from object to be populated to a Map from collection
                        // name to a Set of ids of objects that should be in the collection.
                        // toIds is a Set of all the IDs that we need to fetch in our query.

                        // Now, we don't need to load in objects that the dataloader has already
                        // handled. That includes basically everything we have seen ever.
                        // This section of code was commented out for a long period of time, which
                        // would have resulted in more objects being fetched than strictly
                        // necessary, leading to lower performance. I am uncommenting them.
                        // Hopefully this will not break anything. Tested on a small build.

                        HashSet<Integer> idsToProxy = new HashSet<Integer>();
                        Iterator<Integer> toIdIter = toIds.iterator();
                        while (toIdIter.hasNext()) {
                            Integer toId = toIdIter.next();
                            if (doneAlready.contains(toId)) {
                                toIdIter.remove();
                                idsToProxy.add(toId);
                            }
                        }

                        // Find all the objects we have in the cache.

                        toIdIter = toIds.iterator();
                        while (toIdIter.hasNext()) {
                            Integer toId = toIdIter.next();
                            if (idToObj.containsKey(toId)) {
                                toIdIter.remove();
                            } else {
                                InterMineObject toObj = os.pilferObjectById(toId);
                                if (toObj != null) {
                                    idToObj.put(toId, toObj);
                                    toIdIter.remove();
                                }
                            }
                        }

                        while (!toIds.isEmpty()) {
                            Set<Integer> bag = new HashSet<Integer>();
                            toIdIter = toIds.iterator();
                            for (int i = 0; (i < limit) && toIdIter.hasNext(); i++) {
                                Integer toId = toIdIter.next();
                                bag.add(toId);
                                toIdIter.remove();
                            }

                            Query subQ = new Query();
                            subQ.setDistinct(false);
                            QueryClass qc = new QueryClass(InterMineObject.class);
                            subQ.addFrom(qc);
                            subQ.addToSelect(qc);
                            QueryField qf = new QueryField(qc, "id");
                            subQ.setConstraint(new BagConstraint(qf, ConstraintOp.IN, bag));

                            SingletonResults l = os.executeSingleton(subQ, limit * 2, optimise,
                                    explain, true);
                            for (Object result : l) {
                                InterMineObject o = (InterMineObject) result;
                                idToObj.put(o.getId(), o);
                                doneAlready.add(o.getId());
                            }
                        }
                        for (Integer toAdd : toAddToDoneAlready) {
                            doneAlready.add(toAdd);
                        }
                        // Now we have fetched all the objects in from the database. We now need to
                        // populate every object in our froms Map

                        for (Map.Entry<FastPathObject, Map<String, Object>> fromEntry
                                : froms.entrySet()) {
                            FastPathObject objToPopulate = fromEntry.getKey();
                            //String objDescription;
                            //if (objToPopulate instanceof InterMineObject) {
                            //    objDescription = "object with ID "
                            //        + ((InterMineObject) objToPopulate).getId();
                            //} else {
                            //    objDescription = "FastPathObject";
                            //}
                            Map<String, Object> collectionsToPopulate = fromEntry.getValue();
                            for (Map.Entry<String, Object> collectionEntry
                                    : collectionsToPopulate.entrySet()) {
                                String collectionName = collectionEntry.getKey();
                                Object contents = collectionEntry.getValue();
                                if (contents instanceof Collection<?>) {
                                    Collection<?> collectionContents = (Collection<?>)
                                        collectionEntry.getValue();
                                    Collection<InterMineObject> substituteCollection =
                                        new HashSet<InterMineObject>();
                                    for (Object idToAddObj : collectionContents) {
                                        Integer idToAdd = (Integer) idToAddObj;
                                        InterMineObject objToAdd = (InterMineObject)
                                            idToObj.get(idToAdd);
                                        if (objToAdd == null) {
                                            if (idsToProxy.contains(idToAdd)) {
                                                objToAdd = new ProxyReference(os, idToAdd,
                                                        InterMineObject.class);
                                                //LOG.warn("Did not fetch object with ID " + idToAdd
                                                //        + " for " + objDescription
                                                //        + " for collection " + collectionName);
                                            } else {
                                                ObjectStoreTranslatingImpl osti =
                                                    (ObjectStoreTranslatingImpl) os;
                                                ItemToObjectTranslator itot =
                                                    (ItemToObjectTranslator) osti.getTranslator();
                                                String itemIdentifer =
                                                    (objToPopulate
                                                     instanceof
                                                     InterMineObject ? itot.idToIdentifier(
                                                            ((InterMineObject) objToPopulate)
                                                            .getId()) : null);
                                                String idToAddIdentifer =
                                                    itot.idToIdentifier(idToAdd);
                                                String message =
                                                    "Collection " + collectionName + " in object "
                                                    + (objToPopulate instanceof InterMineObject
                                                            ? ((InterMineObject) objToPopulate)
                                                            .getId() : null) + " ("
                                                    + itemIdentifer + ") refers to object with id "
                                                    + idToAdd + " (" + idToAddIdentifer
                                                    + ") which doesn't exist";
                                                throw new ObjectStoreException(message);
                                            }
                                        //} else {
                                            //LOG.warn("Fetched object with ID " + idToAdd
                                            //        + " for " + objDescription
                                            //        + " for collection " + collectionName);
                                        }
                                        substituteCollection.add(objToAdd);
                                    }
                                    objToPopulate.setFieldValue(collectionName,
                                            substituteCollection);
                                } else {
                                    Integer id = (Integer) contents;
                                    InterMineObject objToAdd = (InterMineObject)
                                        idToObj.get(id);
                                    if (objToAdd != null) {
                                        objToPopulate.setFieldValue(collectionName, objToAdd);
                                        //LOG.warn("Fetched object with ID " + id + " for "
                                        //        + objDescription + " for reference "
                                        //        + collectionName);
                                    //} else {
                                        //LOG.warn("Did not fetch object with ID " + id + " for "
                                        //        + objDescription + " for reference "
                                        //        + collectionName);
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

    /**
     * Returns the Translator associated with the underlying ObjectStoreTranslatingImpl.
     *
     * @return a Translator
     */
    public Translator getTranslator() {
        return ((ObjectStoreTranslatingImpl) os).getTranslator();
    }
}
