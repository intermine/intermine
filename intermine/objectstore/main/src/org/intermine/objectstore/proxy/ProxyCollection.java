package org.intermine.objectstore.proxy;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.SingletonResults;

import org.apache.log4j.Logger;

/**
 * Class which holds a reference to a collection in the database
 *
 * @author Matthew Wakeling
 */
public class ProxyCollection extends AbstractSet implements LazyCollection
{
    private static final Logger LOG = Logger.getLogger(ProxyCollection.class);

    private ObjectStore os;
    private InterMineObject o;
    private String fieldName;
    private Class clazz;
    private boolean noOptimise;
    private boolean noExplain;
    private SoftReference collectionRef = null;
    private int batchSize = 0;

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
    public ProxyCollection(ObjectStore os, InterMineObject o, String fieldName, Class clazz) {
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
    public int size() {
        return getCollection().size();
    }

    /**
     * @see List#iterator
     */
    public Iterator iterator() {
        return getCollection().iterator();
    }

    /**
     * @see LazyCollection#getQuery
     */
    public Query getQuery() {
        try {
            return ((SingletonResults) getCollection()).getQuery();
        } catch (ClassCastException e) {
            return internalGetQuery();
        }
    }

    /**
     * @see LazyCollection#getInfo
     */
    public ResultsInfo getInfo() throws ObjectStoreException {
        Collection coll = getCollection();
        try {
            return ((SingletonResults) coll).getInfo();
        } catch (ClassCastException e) {
            return new ResultsInfo(0, 0, coll.size());
        }
    }

    /**
     * @see LazyCollection#setNoOptimise
     */
    public synchronized void setNoOptimise() {
        noOptimise = true;
        if (collectionRef != null) {
            Collection collection = (Collection) collectionRef.get();
            if ((collection != null) && (collection instanceof SingletonResults)) {
                ((SingletonResults) collection).setNoOptimise();
            }
        }
    }

    /**
     * @see LazyCollection#setNoExplain
     */
    public synchronized void setNoExplain() {
        noExplain = true;
        if (collectionRef != null) {
            Collection collection = (Collection) collectionRef.get();
            if ((collection != null) && (collection instanceof SingletonResults)) {
                ((SingletonResults) collection).setNoExplain();
            }
        }
    }

    /**
     * @see LazyCollection#setBatchSize
     */
    public void setBatchSize(int size) {
        batchSize = size;
        collectionRef = new SoftReference(null);
    }

    /**
     * Gets (or creates) a SingletonResults object to which requests are delegated.
     *
     * @return a SingletonResults object
     */
    private synchronized Collection getCollection() {
        Collection collection = null;
        if (collectionRef == null) {
            usedCount++;
            maybeLog();
        }
        // WARNING - read this following line very carefully.
        if ((collectionRef == null)
                || ((collection = ((Collection) collectionRef.get())) == null)) {
            evaluateCount++;
            maybeLog();
            // Now build a query - SELECT that FROM this, that WHERE this.coll CONTAINS that
            //                         AND this = <this>
            // Or if we have a one-to-many collection, then:
            //    SELECT that FROM that WHERE that.reverseColl CONTAINS <this>
            Query q = internalGetQuery();
            collection = new SingletonResults(q, os, os.getSequence());
            if (batchSize != 0) {
                ((SingletonResults) collection).setBatchSize(batchSize);
            }
            if (noOptimise) {
                ((SingletonResults) collection).setNoOptimise();
            }
            if (noExplain) {
                ((SingletonResults) collection).setNoExplain();
            }
            collectionRef = new SoftReference(collection);
        }
        return collection;
    }

    /**
     * @see LazyCollection#asList()
     */
    public List asList() {
        Collection collection = getCollection();
        if (collection instanceof List) {
            return (List) collection;
        } else {
            return new ArrayList(collection);
        }
    }

    /**
     * Gets the collection if it is a real materialised collection.
     *
     * @return a Collection
     */
    public synchronized Collection getMaterialisedCollection() {
        if (collectionRef != null) {
            Collection collection = (Collection) collectionRef.get();
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
    public synchronized void setMaterialisedCollection(Collection coll) {
        collectionRef = new SoftReference(coll);
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
        } else if ((createdCount + usedCount + evaluateCount) % 10000 == 0) {
            LOG.debug("Created: " + createdCount + ", Used: " + usedCount + ", Evaluated: "
                    + evaluateCount);
        }
    }

    /**
     * @see Object#toString
     *
     * We override this here in order to prevent possible infinite recursion.
     */
    public String toString() {
        return "ProxyCollection";
    }
}
