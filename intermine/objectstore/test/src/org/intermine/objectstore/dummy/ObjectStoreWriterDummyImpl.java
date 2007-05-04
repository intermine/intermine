package org.intermine.objectstore.dummy;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.SingletonResults;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A dummy ObjectStoreWriter to help with testing.  Most methods throw an
 * UnsupportedOperationException.
 *
 * @author Kim Rutherford
 */

public class ObjectStoreWriterDummyImpl implements ObjectStoreWriter
{
    private Map storedObjects = new HashMap();
    // a holder for the old storedObjects Map when we are in a transaction
    private Map savedObjects = null;
    private ObjectStore os;

    // used to generate unique IDs
    private int idCounter = 0;

    /**
     * Create a new ObjectStoreWriterDummyImpl object.
     */
    public ObjectStoreWriterDummyImpl(ObjectStore os) {
        this.os = os;
    }

    public ObjectStore getObjectStore() {
        return os;
    }

    public void store(InterMineObject o) throws ObjectStoreException {
        if (o.getId() == null) {
            Integer newId = getSerial();
            o.setId(newId);
            storedObjects.put(newId, o);
        } else {
            storedObjects.put(o.getId(), o);
        }
    }

    public void addToCollection(Integer hasId, Class clazz, String fieldName, Integer hadId)
        throws ObjectStoreException {
        throw new ObjectStoreException("Not implemented");
    }

    public void delete(InterMineObject o) throws ObjectStoreException {
        storedObjects.remove(o.getId());
    }

    public Integer getSerial() throws ObjectStoreException {
        while (storedObjects.containsKey(new Integer(idCounter))) {
            idCounter++;
        }
        return new Integer(idCounter);
    }

    public boolean isInTransaction() throws ObjectStoreException {
       if (savedObjects == null) {
           return false;
       } else {
           return true;
       }
    }

    public void beginTransaction() throws ObjectStoreException {
        savedObjects = storedObjects;
        storedObjects = new HashMap(savedObjects);
    }

    public void commitTransaction() throws ObjectStoreException {
        savedObjects = null;
    }

    public void abortTransaction() throws ObjectStoreException {
        storedObjects = savedObjects;
        savedObjects = null;
    }

    public void close() throws ObjectStoreException {
       // do nothing
    }

    public Results execute(Query q) {
        throw new UnsupportedOperationException();
    }

    public SingletonResults executeSingleton(Query q) {
        throw new UnsupportedOperationException();
    }

    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            Map<Object, Integer> sequence) throws ObjectStoreException {
        throw new UnsupportedOperationException();
    }

    public InterMineObject getObjectById(Integer id) throws ObjectStoreException {
        return (InterMineObject) storedObjects.get(id);
    }

    public InterMineObject getObjectById(Integer id, Class clazz) throws ObjectStoreException {
        return getObjectById(id);
    }

    public List getObjectsByIds(Collection ids) throws ObjectStoreException {
        throw new UnsupportedOperationException();
    }

    public void prefetchObjectById(Integer id) {
        throw new UnsupportedOperationException();
    }

    public void invalidateObjectById(Integer id) {
        throw new UnsupportedOperationException();
    }

    public Object cacheObjectById(Integer id, InterMineObject obj) {
        throw new UnsupportedOperationException();
    }

    public void flushObjectById() {
        throw new UnsupportedOperationException();
    }

    public InterMineObject pilferObjectById(Integer id) {
        throw new UnsupportedOperationException();
    }

    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        throw new UnsupportedOperationException();
    }

    public int count(Query q, Map<Object, Integer> sequence) throws ObjectStoreException {
        throw new UnsupportedOperationException();
    }

    public Model getModel() {
        return os.getModel();
    }

    public InterMineObject getObjectByExample(InterMineObject o, Set fieldNames) {
        throw new UnsupportedOperationException();
    }

    public boolean isMultiConnection() {
        throw new UnsupportedOperationException();
    }

    public Map<Object, Integer> getSequence(Set<Object> tables) {
        throw new UnsupportedOperationException();
    }

    public int getMaxLimit() {
        throw new UnsupportedOperationException();
    }

    public int getMaxOffset() {
        throw new UnsupportedOperationException();
    }

    public long getMaxTime() {
        throw new UnsupportedOperationException();
    }

    public Map getStoredObjects() {
        return storedObjects;
    }
 
    /**
     * Returns a new empty ObjectStoreBag object that is valid for this ObjectStore.
     *
     * @return an ObjectStoreBag
     * @throws ObjectStoreException if an error occurs fetching a new ID
     */
    public ObjectStoreBag createObjectStoreBag() throws ObjectStoreException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Adds an element to an ObjectStoreBag.
     *
     * @param osb an ObjectStoreBag
     * @param element an Integer to add to the bag
     * @throws ObjectStoreException if an error occurs
     */
    public void addToBag(ObjectStoreBag osb, Integer element) throws ObjectStoreException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Adds a collection of elements to an ObjectStoreBag.
     *
     * @param osb an ObjectStoreBag
     * @param coll a Collection of Integers
     * @throws ObjectStoreException if an error occurs
     */
    public void addAllToBag(ObjectStoreBag osb, Collection coll) throws ObjectStoreException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Removes an element from an ObjectStoreBag.
     *
     * @param osb an ObjectStoreBag
     * @param element an Integer to add to the bag
     * @throws ObjectStoreException if an error occurs
     */
    public void removeFromBag(ObjectStoreBag osb, Integer element) throws ObjectStoreException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Removes a collection of elements from an ObjectStoreBag.
     *
     * @param osb an ObjectStoreBag
     * @param coll a Collection of Integers
     * @throws ObjectStoreException if an error occurs
     */
    public void removeAllFromBag(ObjectStoreBag osb, Collection coll) throws ObjectStoreException {
        throw new RuntimeException("Not implemented");
    }

    public void addToBagFromQuery(ObjectStoreBag osb, Query query) throws ObjectStoreException {
        throw new UnsupportedOperationException();
    }
}
