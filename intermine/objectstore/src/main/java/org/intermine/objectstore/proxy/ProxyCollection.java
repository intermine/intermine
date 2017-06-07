package org.intermine.objectstore.proxy;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.ref.SoftReference;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.ResultsBatches;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.SingletonResults;

/**
 * Class which holds a reference to a collection in the database
 *
 * @author Matthew Wakeling
 * @param <E> The element type
 */
public class ProxyCollection<E> extends AbstractSet<E> implements LazyCollection<E>
{
    private static final Logger LOG = Logger.getLogger(ProxyCollection.class);

    private ObjectStore os;
    private InterMineObject o;
    private String fieldName;
    private Class<?> clazz;
    private boolean noOptimise;
    private boolean noExplain;
    private SoftReference<Collection<E>> collectionRef = null;
    private int batchSize = ResultsBatches.DEFAULT_BATCH_SIZE;

    private static int createdCount = 0;
    private static int usedCount = 0;
    private static int evaluateCount = 0;

    /**
     * Construct a ProxyCollection object.
     *
     * @param os the ObjectStore from which to retrieve the collection
     * @param o the object containing the collection
     * @param fieldName the name of the collection
     * @param clazz the Class of the objects in the collection
     */
    public ProxyCollection(ObjectStore os, InterMineObject o, String fieldName,
            Class<? extends E> clazz) {
        this.os = os;
        this.o = o;
        this.fieldName = fieldName;
        this.clazz = clazz;
        noOptimise = true;
        noExplain = true;
        createdCount++;
        maybeLog();
    }

    /**
     * Returns the ObjectStore that this proxy will use
     *
     * @return an ObjectStore
     */
    public ObjectStore getObjectStore() {
        return os;
    }

    /**
     * Gets the number of elements in this collection
     *
     * @return the number of elements
     */
    @Override
    public int size() {
        return getCollection().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator() {
        return getCollection().iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery() {
        try {
            return ((SingletonResults) getCollection()).getQuery();
        } catch (ClassCastException e) {
            return internalGetQuery();
        }
    }

    /**
     * {@inheritDoc}
     */
    public ResultsInfo getInfo() throws ObjectStoreException {
        Collection<E> coll = getCollection();
        try {
            return ((SingletonResults) coll).getInfo();
        } catch (ClassCastException e) {
            return new ResultsInfo(0, 0, coll.size());
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setNoOptimise() {
        noOptimise = true;
        if (collectionRef != null) {
            Collection<E> collection = collectionRef.get();
            if ((collection != null) && (collection instanceof SingletonResults)) {
                ((SingletonResults) collection).setNoOptimise();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setNoExplain() {
        noExplain = true;
        if (collectionRef != null) {
            Collection<E> collection = collectionRef.get();
            if ((collection != null) && (collection instanceof SingletonResults)) {
                ((SingletonResults) collection).setNoExplain();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBatchSize(int size) {
        batchSize = size;
        collectionRef = new SoftReference<Collection<E>>(null);
    }

    /**
     * Gets (or creates) a SingletonResults object to which requests are delegated.
     *
     * @return a SingletonResults object
     */
    private synchronized Collection<E> getCollection() {
        Collection<E> collection = null;
        if (collectionRef == null) {
            usedCount++;
            maybeLog();
        }
        if (collectionRef != null) {
            collection = collectionRef.get();
        }
        if (collection == null) {
            evaluateCount++;
            maybeLog();
            // Now build a query - SELECT that FROM this, that WHERE this.coll CONTAINS that
            //                         AND this = <this>
            // Or if we have a one-to-many collection, then:
            //    SELECT that FROM that WHERE that.reverseColl CONTAINS <this>
            Query q = internalGetQuery();
            collection = executeCollection(q);
            collectionRef = new SoftReference<Collection<E>>(collection);
        }
        return collection;
    }

    @SuppressWarnings("unchecked")
    private Collection<E> executeCollection(Query q) {
        return (Collection) os.executeSingleton(q, batchSize, !noOptimise, !noExplain, true);
    }

    /**
     * {@inheritDoc}
     */
    public List<E> asList() {
        Collection<E> collection = getCollection();
        if (collection instanceof List<?>) {
            return (List<E>) collection;
        } else {
            return new ArrayList<E>(collection);
        }
    }

    /**
     * Gets the collection if it is a real materialised collection.
     *
     * @return a Collection
     */
    public synchronized Collection<E> getMaterialisedCollection() {
        if (collectionRef != null) {
            Collection<E> collection = collectionRef.get();
            if ((collection != null) && (!(collection instanceof SingletonResults))) {
                return collection;
            }
        }
        return null;
    }

    /**
     * Sets the collection with a new materialised collection.
     *
     * @param coll the new Collection
     */
    public synchronized void setMaterialisedCollection(Collection<E> coll) {
        collectionRef = new SoftReference<Collection<E>>(coll);
    }

    private Query internalGetQuery() {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(clazz);
        q.addFrom(qc1);
        q.addToSelect(qc1);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(o, fieldName),
                        ConstraintOp.CONTAINS, qc1));
        q.setDistinct(false);
        return q;
    }

    private static void maybeLog() {
        if ((createdCount + usedCount + evaluateCount) % 1000000 == 0) {
            LOG.info("Created: " + createdCount + ", Used: " + usedCount + ", Evaluated: "
                    + evaluateCount);
        }
    }

    /**
     * {@inheritDoc}
     *
     * We override this here in order to prevent possible infinite recursion.
     */
    @Override
    public String toString() {
        return "ProxyCollection";
    }
}
