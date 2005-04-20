package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

/**
 * A bag of objects.
 *
 * @author Kim Rutherford
 * @author Thomas Riley
 */
public class InterMineBag extends AbstractList
{
    private static final Logger LOG = Logger.getLogger(InterMineBag.class);
    
    /** Upgrade messages. */
    protected ArrayList upgradeMessages;
    /** Objects contained in the bag or instances of ID. */
    private ArrayList objects = new ArrayList(256);
    /** ObjectStore. */
    private ObjectStore os;

    /** 
     * Constructs a new, empty InterMineBag.
     * @param os object store to read objects from
     */
    public InterMineBag(ObjectStore os) {
        super();
        this.os = os;
    }

    /**
     * @see AbstractList#size
     */
    public int getSize() {
        return size();
    }
    
    /**
     * Add an object by intermine-id.
     * @param id the intermine object id
     */
    public void addId(Integer id) {
        add(new ID(id.intValue()));
    }
    
    /**
     * Add an object to the bag.
     * @param object the object
     * @return true
     * @see AbstractList#add(java.lang.Object)
     */
    public boolean add(Object object) {
        return objects.add(object);
    }
    
    /**
     * Get upgrade messages.
     * @return upgrade messages
     */
    public UpgradeMessage[] getUpgradeMessages() {
        return (UpgradeMessage[]) upgradeMessages.toArray(new UpgradeMessage[0]);
    }
    
    /**
     * Add an upgrade message
     * @param message an upgrade message
     */
    void addUpgradeMessage(UpgradeMessage message) {
        upgradeMessages.add(message);
    }
    
    /**
     * Iterate over elements in the bag, loading objects from the database if necessary.
     * @return iterator that iterates over bag contents
     */
    public Iterator iterator() {
        return new BagIterator();
    }
    
    /**
     * Iterate over bag elements but return instances of InterMineBag.ID for InterMineObjects
     * that haven't yet been loaded from the database. This allows routines that only care about
     * objects ids to function without the overhead of lazily loading InterMineObjects during
     * iteration.
     * 
     * @return iterator over primative wrappers, InterMineObjects and InterMineBag.IDs
     */
    public Iterator lazyIterator() {
        return objects.iterator();
    }

    /**
     * Get the size of the bag.
     * @return size of the bag
     */
    public int size() {
        return objects.size();
    }

    /**
     * Get an object from the bag.
     * @param index index into the bag
     * @return object at location index
     */
    public Object get(int index) {
        Object next = objects.get(index);
        if (next instanceof ID) {
            try {
                objects.set(index, os.getObjectById(new Integer(((ID) next).getId())));
                return objects.get(index);
            } catch (ObjectStoreException err) {
                LOG.error(err);
                throw new RuntimeException(err);
            }
        } else {
            return next;
        }
    }
    
    /**
     * @see java.util.Collection#clear()
     */
    public void clear() {
        objects.clear();
    }
    
    /**
     * @see java.util.List#remove(int)
     */
    public Object remove(int index) {
        return objects.remove(index);
    }
    
    /**
     * Stand-in for not-yet-loaded InterMineObject in objects ArrayList.
     */
    public static class ID
    {
        private int id;
        
        private ID(int id) {
            this.id = id;
        }
        
        /**
         * Get the InterMineObject id.
         * @return InterMineObject id
         */
        public int getId() {
            return id;
        }
    }
    
    /**
     * Some kind of informational message associated with the process of
     * upgrading this bag to a new model/data release. The message will be
     * displayed to the user.
     */
    public interface UpgradeMessage
    {
        /**
         * Human readable message.
         * @return human readable message
         */
        public String getMessage();
    }
    
    /**
     * UpgradeMessage object that represents an element in a bag that
     * couldn't be matched with an object in the new model.
     */
    static class MissingElement implements UpgradeMessage
    {
        private String description;

        /**
         * Create new instance fo MissingElement
         * @param description description of missing element
         */
        public MissingElement(String description) {
            this.description = description;
        }
        
        /**
         * Get description of missing element.
         * @return description of missing element
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * @see UpgradeMessage#getMessage()
         */
        public String getMessage() {
            return "Hello world";
        }
    }
    
    /**
     * Bag element iterator. Calls InterMineBag.get to lazily load bag elements.
     */
    private class BagIterator implements Iterator
    {
        private int index = 0;
        
        /**
         * @see Iterator#hasNext()
         */
        public boolean hasNext() {
            return objects.size() > index;
        }

        /**
         * @see Iterator#next()
         */
        public Object next() {
            return get(index++);
        }

        /**
         * Throws new UnsupportedOperationException.
         * @see Iterator#remove()
         */
        public void remove() {
            objects.remove(index - 1);
            index--;
        }
    }
}
