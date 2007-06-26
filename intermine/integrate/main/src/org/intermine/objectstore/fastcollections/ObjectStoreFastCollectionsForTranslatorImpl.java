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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemToObjectTranslator;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.translating.ObjectStoreTranslatingImpl;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.IntPresentSet;
import org.intermine.util.TypeUtil;

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
    private static final Logger LOG = Logger.getLogger(
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
     * @param osAlias the alias of this objectstore
     * @param props the properties
     * @return the ObjectStore
     * @throws IllegalArgumentException if props are invalid
     * @throws ObjectStoreException if there is a problem with the instance
     */
    public static ObjectStoreFastCollectionsForTranslatorImpl getInstance(String osAlias,
                                                                          Properties props)
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
            // preserve ObjectStoreExceptions for more useful message
            Throwable t = e.getCause();
            if (t instanceof ObjectStoreException) {
                throw (ObjectStoreException) t;
            } else {
                throw new IllegalArgumentException("ObjectStore '" + underlyingOsAlias
                                                   + "' not found in properties");
            }
        }
        return new ObjectStoreFastCollectionsForTranslatorImpl(objectStore);
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
            List retval = os.execute(q, start, limit, optimise, explain, sequence);
            synchronized (doneAlready) {
                if (retval.size() > 1) {
                    // The ItemToObjectTranslator creates collections by creating a query with a
                    // BagConstraint with all the IDs of all the objects that are in the collection.
                    // We should be able to read these queries and extract the IDs, and create a
                    // super-bag for use in a single query.
                    QuerySelectable node = (QuerySelectable) q.getSelect().get(0);
                    if (node instanceof QueryClass) {
                        Map froms = new HashMap();
                        Set toIds = new TreeSet();
                        Iterator rowIter = retval.iterator();
                        while (rowIter.hasNext()) {
                            InterMineObject o = (InterMineObject)
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
                                                    Collection bag = bc.getBag();
                                                    fromColls.put(fieldName, new HashSet(bag));
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

                        // Find all the objects we have in the cache.

                        Map idToObj = new HashMap();
                        Iterator toIdIter = toIds.iterator();
                        while (toIdIter.hasNext()) {
                            Integer toId = (Integer) toIdIter.next();
                            InterMineObject toObj = os.pilferObjectById(toId);
                            if (toObj != null) {
                                idToObj.put(toId, toObj);
                                toIdIter.remove();
                            }
                        }

                        // Now, we don't need to load in objects that the dataloader has already
                        // handled. That includes basically everything we have seen ever.

                        HashSet idsToProxy = new HashSet();
                        toIdIter = toIds.iterator();
                        while (toIdIter.hasNext()) {
                            Integer toId = (Integer) toIdIter.next();
                            if (doneAlready.contains(toId)) {
                                toIdIter.remove();
                                idsToProxy.add(toId);
                            }
                        }

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
                            QueryClass qc = new QueryClass(InterMineObject.class);
                            subQ.addFrom(qc);
                            subQ.addToSelect(qc);
                            QueryField qf = new QueryField(qc, "id");
                            subQ.setConstraint(new BagConstraint(qf, ConstraintOp.IN, bag));

                            SingletonResults l = os.executeSingleton(subQ);
                            if (!optimise) {
                                l.setNoOptimise();
                            }
                            if (!explain) {
                                l.setNoExplain();
                            }
                            l.setBatchSize(limit * 2);
                            Iterator lIter = l.iterator();
                            while (lIter.hasNext()) {
                                InterMineObject o = (InterMineObject) lIter.next();
                                idToObj.put(o.getId(), o);
                                doneAlready.add(o.getId());
                            }
                        }
                        // Now we have fetched all the objects in from the database. We now need to
                        // populate every object in our froms Map

                        Iterator fromIter = froms.entrySet().iterator();
                        while (fromIter.hasNext()) {
                            Map.Entry fromEntry = (Map.Entry) fromIter.next();
                            InterMineObject objToPopulate = (InterMineObject) fromEntry
                                .getKey();
                            Map collectionsToPopulate = (Map) fromEntry.getValue();
                            Iterator collectionIter = collectionsToPopulate.entrySet().iterator();
                            while (collectionIter.hasNext()) {
                                Map.Entry collectionEntry = (Map.Entry) collectionIter.next();
                                String collectionName = (String) collectionEntry.getKey();
                                Object contents = collectionEntry.getValue();
                                if (contents instanceof Collection) {
                                    Collection collectionContents = (Collection)
                                        collectionEntry.getValue();
                                    Collection substituteCollection = (contents instanceof List
                                            ? (Collection) new ArrayList()
                                            : (Collection) new HashSet());
                                    Iterator contentIter = collectionContents.iterator();
                                    while (contentIter.hasNext()) {
                                        Integer idToAdd = (Integer) contentIter.next();
                                        InterMineObject objToAdd = (InterMineObject)
                                            idToObj.get(idToAdd);
                                        if (objToAdd == null) {
                                            if (idsToProxy.contains(idToAdd)) {
                                                objToAdd = new ProxyReference(os, idToAdd,
                                                        InterMineObject.class);
                                            } else {
                                                ObjectStoreTranslatingImpl osti =
                                                    (ObjectStoreTranslatingImpl) os;
                                                ItemToObjectTranslator itot =
                                                    (ItemToObjectTranslator) osti.getTranslator();
                                                String itemIdentifer =
                                                    itot.idToIdentifier(objToPopulate.getId());
                                                String idToAddIdentifer =
                                                    itot.idToIdentifier(idToAdd);
                                                String message = 
                                                    "Collection " + collectionName + " in object "
                                                    + objToPopulate.getId() + " ("  + itemIdentifer
                                                    + ") refers to object with id " + idToAdd + " ("
                                                    + idToAddIdentifer + ") which doesn't exist";
                                                throw new ObjectStoreException(message);
                                            }
                                        }
                                        substituteCollection.add(objToAdd);
                                    }
                                    TypeUtil.setFieldValue(objToPopulate, collectionName,
                                            substituteCollection);
                                } else {
                                    Integer id = (Integer) contents;
                                    InterMineObject objToAdd = (InterMineObject)
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

