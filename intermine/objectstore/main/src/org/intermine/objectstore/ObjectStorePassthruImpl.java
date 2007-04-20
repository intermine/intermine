package org.intermine.objectstore;

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
import java.util.List;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;

/**
 * A generic ObjectStore that passes through every request to an underlying ObjectStore. Extend
 * this to make useful ObjectStores.
 *
 * @author Matthew Wakeling
 */
public class ObjectStorePassthruImpl implements ObjectStore
{
    protected ObjectStore os;

    /**
     * Creates an instance, from another ObjectStore instance.
     *
     * @param os an ObjectStore object to use
     */
    public ObjectStorePassthruImpl(ObjectStore os) {
        this.os = os;
    }

    /**
     * @see ObjectStore#execute(Query)
     */
    public Results execute(Query q) throws ObjectStoreException {
        return os.execute(q);
    }

    /**
     * @see ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        return os.execute(q, start, limit, optimise, explain, sequence);
    }

    /**
     * @see ObjectStore#getObjectById(Integer)
     */
    public InterMineObject getObjectById(Integer id) throws ObjectStoreException {
        return os.getObjectById(id);
    }

    /**
     * @see ObjectStore#getObjectById(Integer, Class)
     */
    public InterMineObject getObjectById(Integer id, Class clazz) throws ObjectStoreException {
        return os.getObjectById(id, clazz);
    }

    /**
     * @see ObjectStore#getObjectsByIds(Integer)
     */
    public List getObjectsByIds(Collection ids) throws ObjectStoreException {
        return os.getObjectsByIds(ids);
    }

    /**
     * @see ObjectStore#prefetchObjectById
     */
    public void prefetchObjectById(Integer id) {
        os.prefetchObjectById(id);
    }

    /**
     * @see ObjectStore#invalidateObjectById
     */
    public void invalidateObjectById(Integer id) {
        os.invalidateObjectById(id);
    }

    /**
     * @see ObjectStore#cacheObjectById
     */
    public Object cacheObjectById(Integer id, InterMineObject obj2) {
        return os.cacheObjectById(id, obj2);
    }

    /**
     * @see ObjectStore#flushObjectById
     */
    public void flushObjectById() {
        os.flushObjectById();
    }

    /**
     * @see ObjectStore#pilferObjectById
     */
    public InterMineObject pilferObjectById(Integer id) {
        return os.pilferObjectById(id);
    }

    /**
     * @see ObjectStore#estimate
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return os.estimate(q);
    }

    /**
     * @see ObjectStore#count
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        return os.count(q, sequence);
    }

    /**
     * @see ObjectStore#getModel
     */
    public Model getModel() {
        return os.getModel();
    }

    /**
     * @see ObjectStore#getObjectByExample
     */
    public InterMineObject getObjectByExample(InterMineObject o, Set fieldNames)
            throws ObjectStoreException {
        return os.getObjectByExample(o, fieldNames);
    }

    /**
     * @see ObjectStore#isMultiConnection
     */
    public boolean isMultiConnection() {
        return os.isMultiConnection();
    }

    /**
     * @see ObjectStore#getSequence
     */
    public int getSequence() {
        return os.getSequence();
    }

    /**
     * @see ObjectStore#getMaxLimit
     */
    public int getMaxLimit() {
        return os.getMaxLimit();
    }

    /**
     * @see ObjectStore#getMaxOffset
     */
    public int getMaxOffset() {
        return os.getMaxOffset();
    }

    /**
     * @see ObjectStore#getMaxTime
     */
    public long getMaxTime() {
        return os.getMaxTime();
    }

    /**
     * @see ObjectStore#getSerial
     */
    public Integer getSerial() throws ObjectStoreException {
        return os.getSerial();
    }

    /**
     * @see ObjectStore#createObjectStoreBag
     */
    public ObjectStoreBag createObjectStoreBag() throws ObjectStoreException {
        return os.createObjectStoreBag();
    }
}
