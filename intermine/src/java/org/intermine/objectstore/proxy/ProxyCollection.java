package org.intermine.objectstore.proxy;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.ref.SoftReference;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.SingletonResults;

import org.apache.log4j.Logger;

/**
 * Class which holds a reference to a collection in the database
 *
 * @author Matthew Wakeling
 */
public class ProxyCollection extends AbstractList implements Set, LazyCollection
{
    private static final Logger LOG = Logger.getLogger(ProxyCollection.class);

    private ObjectStore os;
    private InterMineObject o;
    private String fieldName;
    private Class clazz;
    private int flags; // 1 = useReverseRelation, 2 = noOptimise, 4 = noExplain
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
     * @param fieldName the name of the collection, or if useReverseRelation is true, the name of
     * the reverse field
     * @param clazz the Class of the objects in the collection
     * @param useReverseRelation if the collection query should use the reverse relationship
     */
    public ProxyCollection(ObjectStore os, InterMineObject o, String fieldName, Class clazz,
            boolean useReverseRelation) {
        this.os = os;
        this.o = o;
        this.fieldName = fieldName;
        this.clazz = clazz;
        this.flags = (useReverseRelation ? 1 : 0);
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
     * Gets the element in the given numbered position.
     *
     * @param index the element number
     * @return the relevant entry
     */
    public Object get(int index) {
        return getCollection().get(index);
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
     * @see List#subList
     */
    public List subList(int start, int end) {
        return getCollection().subList(start, end);
    }

    /**
     * @see List#iterator
     */
    public Iterator iterator() {
        return getCollection().iterator();
    }

    /**
     * @see List#isEmpty
     */
    public boolean isEmpty() {
        return getCollection().isEmpty();
    }

    /**
     * @see LazyCollection#getQuery
     */
    public Query getQuery() {
        return getCollection().getQuery();
    }

    /**
     * @see LazyCollection#getInfo
     */
    public ResultsInfo getInfo() throws ObjectStoreException {
        return getCollection().getInfo();
    }

    /**
     * @see LazyCollection#setNoOptimise
     */
    public void setNoOptimise() {
        flags = flags | 2;
        if (collectionRef != null) {
            SingletonResults collection = (SingletonResults) collectionRef.get();
            if (collection != null) {
                collection.setNoOptimise();
            }
        }
    }

    /**
     * @see LazyCollection#setNoExplain
     */
    public void setNoExplain() {
        flags = flags | 4;
        if (collectionRef != null) {
            SingletonResults collection = (SingletonResults) collectionRef.get();
            if (collection != null) {
                collection.setNoExplain();
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
    private SingletonResults getCollection() {
        SingletonResults collection = null;
        if (collectionRef == null) {
            usedCount++;
            maybeLog();
        }
        // WARNING - read this following line very carefully.
        if ((collectionRef == null)
                || ((collection = ((SingletonResults) collectionRef.get())) == null)) {
            evaluateCount++;
            maybeLog();
            // Now build a query - SELECT that FROM this, that WHERE this.coll CONTAINS that
            //                         AND this = <this>
            // Or if we have a one-to-many collection, then:
            //    SELECT that FROM that WHERE that.reverseColl CONTAINS <this>
            Query q = new Query();
            if ((flags % 2) == 1) {
                QueryClass qc1 = new QueryClass(clazz);
                q.addFrom(qc1);
                q.addToSelect(qc1);
                QueryObjectReference qor = new QueryObjectReference(qc1, fieldName);
                ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS, o);
                q.setConstraint(cc);
                q.addToOrderBy(qor);
                q.setDistinct(false);
            } else {
                QueryClass qc1 = new QueryClass(o.getClass());
                QueryClass qc2 = new QueryClass(clazz);
                q.addFrom(qc1);
                q.addFrom(qc2);
                q.addToSelect(qc2);
                ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1,
                                fieldName), ConstraintOp.CONTAINS, qc2));
                cs.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, o));
                q.setConstraint(cs);
                q.setDistinct(false);
            }
            collection = new SingletonResults(q, os, os.getSequence());
            if (batchSize != 0) {
                collection.setBatchSize(batchSize);
            }
            if (((flags / 2) % 2) == 1) {
                collection.setNoOptimise();
            }
            if ((flags / 4) == 1) {
                collection.setNoExplain();
            }
            collectionRef = new SoftReference(collection);
        }
        return collection;
    }

    private static void maybeLog() {
        if ((createdCount + usedCount + evaluateCount) % 10000 == 0) {
            LOG.info("Created: " + createdCount + ", Used: " + usedCount + ", Evaluated: "
                    + evaluateCount);
        } else if ((createdCount + usedCount + evaluateCount) % 100 == 0) {
            LOG.debug("Created: " + createdCount + ", Used: " + usedCount + ", Evaluated: "
                    + evaluateCount);
        }
    }
}
