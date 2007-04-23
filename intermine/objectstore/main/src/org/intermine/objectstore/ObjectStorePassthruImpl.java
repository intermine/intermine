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
     * {@inheritDoc}
     */
    public Results execute(Query q) throws ObjectStoreException {
        return os.execute(q);
    }

    /**
     * {@inheritDoc}
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        return os.execute(q, start, limit, optimise, explain, sequence);
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject getObjectById(Integer id) throws ObjectStoreException {
        return os.getObjectById(id);
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject getObjectById(Integer id, Class clazz) throws ObjectStoreException {
        return os.getObjectById(id, clazz);
    }

    /**
     * {@inheritDoc}
     */
    public List getObjectsByIds(Collection ids) throws ObjectStoreException {
        return os.getObjectsByIds(ids);
    }

    /**
     * {@inheritDoc}
     */
    public void prefetchObjectById(Integer id) {
        os.prefetchObjectById(id);
    }

    /**
     * {@inheritDoc}
     */
    public void invalidateObjectById(Integer id) {
        os.invalidateObjectById(id);
    }

    /**
     * {@inheritDoc}
     */
    public Object cacheObjectById(Integer id, InterMineObject obj2) {
        return os.cacheObjectById(id, obj2);
    }

    /**
     * {@inheritDoc}
     */
    public void flushObjectById() {
        os.flushObjectById();
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject pilferObjectById(Integer id) {
        return os.pilferObjectById(id);
    }

    /**
     * {@inheritDoc}
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return os.estimate(q);
    }

    /**
     * {@inheritDoc}
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        return os.count(q, sequence);
    }

    /**
     * {@inheritDoc}
     */
    public Model getModel() {
        return os.getModel();
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject getObjectByExample(InterMineObject o, Set fieldNames)
            throws ObjectStoreException {
        return os.getObjectByExample(o, fieldNames);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiConnection() {
        return os.isMultiConnection();
    }

    /**
     * {@inheritDoc}
     */
    public int getSequence() {
        return os.getSequence();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxLimit() {
        return os.getMaxLimit();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxOffset() {
        return os.getMaxOffset();
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxTime() {
        return os.getMaxTime();
    }

    /**
     * {@inheritDoc}
     */
    public Integer getSerial() throws ObjectStoreException {
        return os.getSerial();
    }

    /**
     * {@inheritDoc}
     */
    public ObjectStoreBag createObjectStoreBag() throws ObjectStoreException {
        return os.createObjectStoreBag();
    }
}
