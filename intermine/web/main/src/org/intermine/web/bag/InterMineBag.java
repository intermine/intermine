package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;

/**
 * A LinkedHashSet with a getSize() method.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */

public abstract class InterMineBag extends AbstractSet
{
    protected static final Logger LOG = Logger.getLogger(InterMineBag.class);

    /** User id, used for storing and retrieving. */
    private Integer userId;
    /** Bag name, used for storing and retrieving. */
    private String name;
    /** Bag size, used to provide this info without having to fetch the entire bag contents. */
    private int size;
    /** ObjectStore used for storing and retrieving. */
    protected ObjectStore os;
    /** SoftReference to a materialised version of the bag. If this holds a reference to a valid
     * LinkedHashSet, then the Set is cached data that does not need to be stored, and set will
     * be null. */
    private SoftReference setReference;
    /** Strong reference to a materialised version of the bag. If this holds a LinkedHashSet, then
     * the Set is data that needs to be stored, and setReference will be null. */
    private LinkedHashSet set;

    /**
     * Constructs a new InterMineBag to be lazily-loaded from the userprofile database.
     *
     * @param userId the id of the user, matching the userprofile database
     * @param name the name of the bag, matching the userprofile database
     * @param size the size of the bag
     * @param os the ObjectStore to use to retrieve the contents of the bag
     */
    public InterMineBag(Integer userId, String name, int size, ObjectStore os) {
        setReference = null;
        set = null;
        this.userId = userId;
        this.name = name;
        this.size = size;
        this.os = os;
    }

    /**
     * Constructs a new InterMineBag with certain contents.
     *
     * @param userId the id of the user, to be saved in the userprofile database
     * @param name the name of the bag, to be saved in the userprofile database
     * @param os the ObjectStore to use to store the contents of the bag
     * @param c the new bag contents
     */
    public InterMineBag(Integer userId, String name, ObjectStore os, Collection c) {
        setReference = null;
        set = new LinkedHashSet(c);
        this.userId = userId;
        this.name = name;
        this.size = -1;
        this.os = os;
    }

    /**
     * @see AbstractSet#size
     */
    public synchronized int size() {
        if (set != null) {
            return set.size();
        } else {
            return size;
        }
    }

    /**
     * Bean method to get size
     *
     * @return the size
     */
    public int getSize() {
        return size();
    }

    /**
     * Get the real set of Bag Elements
     * @return a LinkedHashSet containing the bag elements
     */
    protected synchronized LinkedHashSet getRealSet() {
        if (set != null) {
            return set;
        }
        if (setReference != null) {
            LinkedHashSet retval = (LinkedHashSet) setReference.get();
            if (retval != null) {
                return retval;
            }
        }
        if (size == 0) {
            return new LinkedHashSet();
        }
        try {
            Query q = new Query();
            QueryClass qc = new QueryClass(SavedBag.class);
            q.addFrom(qc);
            q.addToSelect(new QueryField(qc, "bag"));
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"),
                        ConstraintOp.EQUALS, new QueryValue(name)));
            cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc, "userProfile"),
                        ConstraintOp.CONTAINS, new ProxyReference(null, userId,
                                                                  UserProfile.class)));
            q.setConstraint(cs);
            Results res = os.execute(q);
            String bagText = (String) ((List) res.get(0)).get(0);
            Map unmarshalled = InterMineBagBinding.unmarshal(new StringReader(bagText), os,
                    IdUpgrader.ERROR_UPGRADER, userId);
            InterMineBag un = (InterMineBag) unmarshalled.get(name);
            setReference = new SoftReference(un.set);
            return un.set;
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the real set for writting to it (add, remove, clear)
     * @return a LinkedHasSet
     */
    private synchronized LinkedHashSet getRealSetForWrite() {
        if (set == null) {
            set = getRealSet();
        }
        return set;
    }

    /**
     * Tells if the bag needs to written to the database
     * @return a boolean
     */
    public synchronized boolean needsWrite() {
        return set != null;
    }

    /**
     * Resets to the database
     */
    public synchronized void resetToDatabase() {
        setReference = new SoftReference(set);
        size = set.size();
        set = null;
    }

    /**
     * @see AbstractSet#add
     */
    public boolean add(Object o) {
        return getRealSetForWrite().add(o);
    }

    /**
     * @see AbstractSet#clear
     */
    public void clear() {
        getRealSetForWrite().clear();
    }

    /**
     * @see AbstractSet#contains
     */
    public boolean contains(Object o) {
        return getRealSet().contains(o);
    }

    /**
     * @see AbstractSet#iterator
     */
    public Iterator iterator() {
        // Need a special iterator here, so we can record when a modification occurs
        return new BagIterator();
    }

    /**
     * @see AbstractSet#remove
     */
    public boolean remove(Object o) {
        return getRealSetForWrite().remove(o);
    }

    /**
     * Return a collection of actual objects represented by this bag rather than any
     * intermediate form (such as intermine object id numbers).
     * @return collection of objects
     */
    public abstract Collection toObjectCollection();

    private class BagIterator implements Iterator
    {
        private LinkedHashSet iterSet;
        private Iterator iter;

        public BagIterator() {
            iterSet = getRealSet();
            iter = iterSet.iterator();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Object next() {
            return iter.next();
        }

        public void remove() {
            if (set == null) {
                set = iterSet;
            }
            iter.remove();
        }
    }
}
