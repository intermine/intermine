package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.IntToIntMap;
import org.intermine.util.IntPresentSet;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Abstract implementation of ObjectStoreIntegrationWriter.  To retain
 * O/R mapping independence concrete subclasses should delegate writing to
 * a mapping tool specific implementation of ObjectStoreWriter.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */

public abstract class IntegrationWriterAbstractImpl implements IntegrationWriter,
       EquivalentObjectFetcher
{
    private static final Logger LOG = Logger.getLogger(IntegrationWriterAbstractImpl.class);

    protected static final int MAX_MAPPINGS = 1000000;
    protected ObjectStoreWriter osw;
    protected static final int SKELETON = 0;
    protected static final int FROM_DB = 1;
    protected static final int SOURCE = 2;
    protected IntToIntMap idMap = new IntToIntMap();
    protected IntPresentSet dbIdsStored = new IntPresentSet();
    protected int idMapOps = 0;
    protected boolean ignoreDuplicates = false;
    protected EquivalentObjectFetcher eof;

    /**
     * Constructs a new instance of an IntegrationWriter
     *
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     */
    public IntegrationWriterAbstractImpl(ObjectStoreWriter osw) {
        this.osw = osw;
        eof = new HintingFetcher(osw.getObjectStore(), this);
    }

    /**
     * @see IntegrationWriter#setIgnoreDuplicates
     */
    public void setIgnoreDuplicates(boolean ignoreDuplicates) {
        this.ignoreDuplicates = ignoreDuplicates;
    }

    /**
     * Returns the underlying ObjectStoreWriter.
     *
     * @return osw
     */
    public ObjectStoreWriter getObjectStoreWriter() {
        return osw;
    }

    /**
     * Sets the EquivalentObjectFetcher that this IntegrationWriter will use to fetch equivalent
     * objects from the production database. If this method is not called, the IntegrationWriter
     * falls back to "this". This method is provided to permit a higher-performance system to be
     * used instead.
     *
     * @param eof an EquivalentObjectFinder
     */
    public void setEof(EquivalentObjectFetcher eof) {
        this.eof = eof;
    }

    /**
     * Returns a Set of objects from the idMap or database that are equivalent to the given
     * object, according to the primary keys defined by the given Source.
     *
     * @param obj the Object to look for
     * @param source the data Source
     * @return a Set of InterMineObjects
     * @throws ObjectStoreException if an error occurs
     */
    public Set getEquivalentObjects(InterMineObject obj, Source source)
            throws ObjectStoreException {
        if (obj == null) {
            throw new NullPointerException("obj should not be null");
        }
        Integer destId = null;
        if (obj.getId() != null) {
            destId = idMap.get(obj.getId());
        }
        if (destId == null) {
            // query database by primary key for equivalent objects
            if (obj instanceof ProxyReference) {
                LOG.error("IDMAP CONTENTS:" + idMap.toString());
                throw new IllegalArgumentException("Given a ProxyReference, but id not in ID Map."
                        + " Source object ID: " + obj.getId()
                        + (idMap.size() < 100 ? ", idMap = " : ""));
            }
            if ((obj.getId() == null) || ignoreDuplicates) {
                return queryEquivalentObjects(obj, source);
            } else {
                return eof.queryEquivalentObjects(obj, source);
            }
        } else {
            // was in idMap, no need to query database
            return Collections.singleton(new ProxyReference(osw, destId, InterMineObject.class));
        }
    }


    /**
     * Queries the database for equivalent objects for the given object, according
     * to the primary keys defined by the given Source.
     *
     * @param obj the Object to look for
     * @param source the data Source
     * @return a Set of InterMineObjects
     * @throws ObjectStoreException if an error occurs
     */
    public Set queryEquivalentObjects(InterMineObject obj, Source source)
        throws ObjectStoreException {
        Query q = null;
        try {
            q = DataLoaderHelper.createPKQuery(getModel(), obj, source, idMap, null, false);
        } catch (MetaDataException e) {
            throw new ObjectStoreException(e);
        }
        if (q != null) {
            SingletonResults result = new SingletonResults(q, this, getSequence());
            result.setNoOptimise();
            result.setNoExplain();
            return result;
        } else {
            return new HashSet();
        }
    }



    /**
     * @see IntegrationWriter#store
     */
    public void store(InterMineObject o, Source source, Source skelSource)
            throws ObjectStoreException {
        if (o == null) {
            throw new NullPointerException("Object o should not be null");
        }
        long time = (new Date()).getTime();
        store(o, source, skelSource, SOURCE);
        long now = (new Date()).getTime();
        if (now - time > 20000) {
            LOG.info("Stored object " + o.getClass().getName() + ":" + o.getId() + " - took "
                    + (now - time) + " ms");
        }
    }


    /**
     * Stores the given object in the objectstore. This method recurses into the object's fields
     * according to the type variable.
     *
     * @param o the object to store
     * @param source the data Source to which to attribute the data
     * @param skelSource the data Source to which to attribute skeleton data
     * @param type the type of action required, from SOURCE, SKELETON, or FROM_DB
     * @return the InterMineObject that was written to the database
     * @throws ObjectStoreException if an error occurs in the underlying objectstore
     */
    protected abstract InterMineObject store(InterMineObject o, Source source,
            Source skelSource, int type) throws ObjectStoreException;

    /**
     * Copies the value of the field given from the source object into the destination object.
     *
     * @param srcObj the source object
     * @param dest the destination object
     * @param source the data Source to which to attribute the data
     * @param skelSource the data Source to which to attribute skeleton data
     * @param field the FieldDescriptor describing the field to copy
     * @param type the type of copy required - SOURCE for a full copy, SKELETON for a minimal copy
     * that guarantees limited recursion, and FROM_DB to indicate that the source object originated
     * from the destination database.
     * @throws IllegalAccessException should never happen
     * @throws ObjectStoreException if an error ocurs in the underlying objectstore
     */
    protected void copyField(InterMineObject srcObj, InterMineObject dest,
            Source source, Source skelSource, FieldDescriptor field, int type)
            throws IllegalAccessException, ObjectStoreException {
        String fieldName = field.getName();
        if (!"id".equals(fieldName)) {
            switch (field.relationType()) {
                case FieldDescriptor.NOT_RELATION:
                    TypeUtil.setFieldValue(dest, fieldName, TypeUtil.getFieldValue(srcObj,
                                fieldName));
                    break;
                case FieldDescriptor.N_ONE_RELATION:
                    if ((type == FROM_DB) || (type == SOURCE)
                            || DataLoaderHelper.fieldIsPrimaryKey(getModel(),
                                dest.getClass(), fieldName, source)) {
                        if (type == FROM_DB) {
                            TypeUtil.setFieldValue(dest, fieldName,
                                    TypeUtil.getFieldProxy(srcObj, fieldName));
                        } else {
                            InterMineObject sourceTarget = (InterMineObject)
                                TypeUtil.getFieldProxy(srcObj, fieldName);
                            if (sourceTarget instanceof ProxyReference) {
                                if (idMap.get(sourceTarget.getId()) == null) {
                                    sourceTarget = ((ProxyReference) sourceTarget).getObject();
                                }
                            }
                            InterMineObject target = store(sourceTarget, source, skelSource,
                                    SKELETON);
                            TypeUtil.setFieldValue(dest, fieldName, target);
                        }
                    }
                    break;
                case FieldDescriptor.ONE_ONE_RELATION:
                    if ((type == FROM_DB) || (type == SOURCE)) {
                        InterMineObject loser = (InterMineObject)
                            TypeUtil.getFieldValue(dest, fieldName);
                        ReferenceDescriptor reverseRef = ((ReferenceDescriptor) field)
                            .getReverseReferenceDescriptor();
                        if (loser != null) {
                            invalidateObjectById(loser.getId());
                            try {
                                TypeUtil.setFieldValue(loser, reverseRef.getName(), null);
                            } catch (NullPointerException e) {
                                throw new NullPointerException("reverseRef must be null: "
                                        + reverseRef + ", forward ref is "
                                        + field.getClassDescriptor().getName() + "."
                                        + field.getName() + ", type is " + field.relationType());
                            }
                            store(loser);
                        }
                        InterMineObject target = null;
                        if (type == SOURCE) {
                            target = store((InterMineObject)
                                    TypeUtil.getFieldValue(srcObj, fieldName), source, skelSource,
                                    SKELETON);
                        } else {
                            target = (InterMineObject) TypeUtil.getFieldValue(srcObj,
                                    fieldName);
                        }
                        if (target != null) {
                            if (target instanceof ProxyReference) {
                                LOG.debug("Reifying object for modification in place");
                                target = ((ProxyReference) target).getObject();
                            }
                            InterMineObject targetsReferent = (InterMineObject)
                                TypeUtil.getFieldValue(target, reverseRef.getName());
                            if ((targetsReferent != null) && (!targetsReferent.equals(dest))) {
                                invalidateObjectById(targetsReferent.getId());
                                TypeUtil.setFieldValue(targetsReferent, fieldName, null);
                                store(targetsReferent);
                            }
                            TypeUtil.setFieldValue(target, reverseRef.getName(), dest);
                            store(target);
                        }
                        TypeUtil.setFieldValue(dest, fieldName, target);
                    }
                    break;
                case FieldDescriptor.ONE_N_RELATION:
                    if ((type == FROM_DB) && ((dest.getId() == null)
                                || (!dest.getId().equals(srcObj.getId())))) {
                        Collection col = (Collection) TypeUtil.getFieldValue(srcObj, fieldName);
                        Iterator colIter = col.iterator();
                        while (colIter.hasNext()) {
                            InterMineObject colObj = (InterMineObject) colIter.next();
                            invalidateObjectById(colObj.getId());
                            ReferenceDescriptor reverseRef = ((CollectionDescriptor) field)
                                .getReverseReferenceDescriptor();
                            TypeUtil.setFieldValue(colObj, reverseRef.getName(), dest);
                            if (type == FROM_DB) {
                                store(colObj);
                                // We don't need to call again on this object, this call may have
                                // been present to ensure the tracker is updated - but would get
                                // updated with the wrong source anyway.  See ticket #955.
                                //store(colObj, source, skelSource, FROM_DB);
                            } else {
                                store(colObj, source, skelSource, SKELETON);
                            }
                        }
                    }
                    break;
                case FieldDescriptor.M_N_RELATION:
                    if ((type == SOURCE) || ((type == FROM_DB) && ((dest.getId() == null)
                                || (!dest.getId().equals(srcObj.getId()))))) {
                        Collection destCol = (Collection) TypeUtil.getFieldValue(dest, fieldName);
                        Collection col = (Collection) TypeUtil.getFieldValue(srcObj, fieldName);
                        Iterator colIter = col.iterator();
                        while (colIter.hasNext()) {
                            InterMineObject colObj = (InterMineObject) colIter.next();
                            if (type == FROM_DB) {
                                destCol.add(colObj);
                            } else {
                                try {
                                    destCol.add(store(colObj, source, skelSource, SKELETON));
                                } catch (RuntimeException bob) {
                                    if (colObj instanceof ProxyReference) {
                                        LOG.warn("colObj: " + colObj + ", "
                                                 + ((ProxyReference) colObj).getObject());
                                    }
                                    LOG.warn("destCol = " + destCol);
                                    LOG.warn("col = " + col);
                                    LOG.warn("fieldName:" + field.getName()
                                             + " classDescriptionName:"
                                             + field.getClassDescriptor().getName(), bob);
                                    throw bob;
                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    /**
     * Puts a mapping into idMap.
     *
     * @param source the ID of the object from the source
     * @param dest the ID of the object from the destination
     * @throws ObjectStoreException if an attempt is made to change an existing mapping
     */
    public void assignMapping(Integer source, Integer dest) throws ObjectStoreException {
        if ((source != null) && (dest != null)) {
            Integer existingValue = idMap.get(source);
            if ((existingValue != null) && (!existingValue.equals(dest))) {
                throw new ObjectStoreException("Error: Attempt to put " + source + " -> "
                        + dest + " into ID Map, but " + source + " -> " + existingValue
                        + "exists already");
            }
            idMap.put(source, dest);
            dbIdsStored.add(dest);
            idMapOps++;
            if (idMapOps % 100000 == 0) {
                LOG.info("idMap size = " + idMap.size() + ", ops = " + idMapOps);
            }
        }
    }







    /* The following methods are implementing the ObjectStoreWriter interface */

    /**
     * Search database for object matching the given object id
     *
     * @param id the object ID
     * @return the retrieved object
     * @throws ObjectStoreException if an error occurs retieving the object
     */
    public InterMineObject getObjectById(Integer id) throws ObjectStoreException {
        return osw.getObjectById(id);
    }

    /**
     * Search database for object matching the given object id and class
     *
     * @param id the object ID
     * @param clazz a Class of the object
     * @return the retrieved object
     * @throws ObjectStoreException if an error occurs retrieving the object
     */
    public InterMineObject getObjectById(Integer id, Class clazz) throws ObjectStoreException {
        return osw.getObjectById(id, clazz);
    }

    /**
     * @see ObjectStore#getObjectsByIds(Collection)
     */
    public List getObjectsByIds(Collection ids) throws ObjectStoreException {
        return osw.getObjectsByIds(ids);
    }

    /**
     * Store an object in this ObjectStore, delegates to internal ObjectStoreWriter.
     *
     * @param o the object to store
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public void store(InterMineObject o) throws ObjectStoreException {
        osw.store(o);
    }

    /**
     * Add an object to another object's collection, delegate to internal ObjectStoreWriter.
     *
     * @param hasId the ID of the object that has the collection
     * @param clazz the class of the object
     * @param fieldName the name of the collection
     * @param hadId the ID of the object to be placed in the collection
     * @throws ObjectStoreException if something goes wrong
     */
    public void addToCollection(Integer hasId, Class clazz, String fieldName, Integer hadId)
        throws ObjectStoreException {
        osw.addToCollection(hasId, clazz, fieldName, hadId);
    }

    /**
     * Delete an object from this ObjectStore, delegate to internal ObjectStoreWriter.
     *
     * @param o the object to delete
     * @throws ObjectStoreException if an error occurs during deletion of the object
     */
    public void delete(InterMineObject o) throws ObjectStoreException {
        osw.delete(o);
    }

    /**
     * Returns a new empty ObjectStoreBag for this ObjectStore, delegate to internal
     * ObjectStoreWriter.
     *
     * @return an ObjectStoreBag
     * @throws ObjectStoreException if an error occurs fetching a new ID
     */
    public ObjectStoreBag createObjectStoreBag() throws ObjectStoreException {
        return osw.createObjectStoreBag();
    }

    /**
     * Adds an element to an ObjectStoreBag, delegate to internal ObjectStoreWriter.
     *
     * @param osb an ObjectStoreBag
     * @param element an Integer to add to the bag
     * @throws ObjectStoreException if an error occurs
     */
    public void addToBag(ObjectStoreBag osb, Integer element) throws ObjectStoreException {
        osw.addToBag(osb, element);
    }

    /**
     * Adds a collection of elements to an ObjectStoreBag, delegate to internal ObjectStoreWriter.
     *
     * @param osb an ObjectStoreBag
     * @param coll a Collection of Integers
     * @throws ObjectStoreException if an error occurs
     */
    public void addAllToBag(ObjectStoreBag osb, Collection coll) throws ObjectStoreException {
        osw.addAllToBag(osb, coll);
    }

    /**
     * Removes an element from an ObjectStoreBag, delegate to internal ObjectStoreWriter.
     *
     * @param osb an ObjectStoreBag
     * @param element an Integer to add to the bag
     * @throws ObjectStoreException if an error occurs
     */
    public void removeFromBag(ObjectStoreBag osb, Integer element) throws ObjectStoreException {
        osw.removeFromBag(osb, element);
    }

    /**
     * Removes a collection of elements from an ObjectStoreBag, delegate to internal
     * ObjectStoreWriter.
     *
     * @param osb an ObjectStoreBag
     * @param coll a Collection of Integers
     * @throws ObjectStoreException if an error occurs
     */
    public void removeAllFromBag(ObjectStoreBag osb, Collection coll) throws ObjectStoreException {
        osw.removeAllFromBag(osb, coll);
    }

    /**
     * Adds elements to an ObjectStoreBag, delegate to internal ObjectStoreWriter.
     *
     * @param osb an ObjectStoreBag
     * @param query a Query
     * @throws ObjectStoreException if an error occurs
     */
    public void addToBagFromQuery(ObjectStoreBag osb, Query query) throws ObjectStoreException {
        osw.addToBagFromQuery(osb, query);
    }

    /**
     * Gets an ID number which is unique in the database.
     *
     * @return an Integer
     * @throws ObjectStoreException if a problem occurs
     */
    public Integer getSerial() throws ObjectStoreException {
        return osw.getSerial();
    }

    /**
     * Check whether the ObjectStore is performing a transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @return true if in a transaction, false otherwise
     * @throws ObjectStoreException if an error occurs the check
     */
    public boolean isInTransaction() throws ObjectStoreException {
        return osw.isInTransaction();
    }

    /**
     * Request that the ObjectStore begins a transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @throws ObjectStoreException if a transaction is in progress, or is aborted
     */
    public void beginTransaction() throws ObjectStoreException {
        osw.beginTransaction();
    }

    /**
     * Request that the ObjectStore commits and closes the transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @throws ObjectStoreException if a transaction is not in progress, or is aborted
     */
    public void commitTransaction() throws ObjectStoreException {
        osw.commitTransaction();
    }

    /**
     * Request that the ObjectStore aborts and closes the transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @throws ObjectStoreException if a transaction is not in progress
     */
    public void abortTransaction() throws ObjectStoreException {
        osw.abortTransaction();
    }

    /**
     * @see org.intermine.objectstore.ObjectStoreWriter#getObjectStore
     */
    public ObjectStore getObjectStore() {
        return osw.getObjectStore();
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#execute
     */
    public Results execute(Query q) throws ObjectStoreException {
        return osw.execute(q);
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        return osw.execute(q, start, limit, optimise, explain, sequence);
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#prefetchObjectById
     */
    public void prefetchObjectById(Integer id) {
        osw.prefetchObjectById(id);
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#invalidateObjectById
     */
    public void invalidateObjectById(Integer id) {
        osw.invalidateObjectById(id);
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#cacheObjectById
     */
    public Object cacheObjectById(Integer id, InterMineObject obj) {
        return osw.cacheObjectById(id, obj);
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#flushObjectById
     */
    public void flushObjectById() {
        osw.flushObjectById();
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#pilferObjectById
     */
    public InterMineObject pilferObjectById(Integer id) {
        return osw.pilferObjectById(id);
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#estimate
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return osw.estimate(q);
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#count
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        return osw.count(q, sequence);
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#getModel
     */
    public Model getModel() {
        return osw.getModel();
    }

    /**
     * @see org.intermine.objectstore.ObjectStore#getObjectByExample
     */
    public InterMineObject getObjectByExample(InterMineObject o, Set fieldNames)
            throws ObjectStoreException {
        return osw.getObjectByExample(o, fieldNames);
    }

    /**
     * @see org.intermine.objectstore.ObjectStoreWriter#close
     */
    public void close() throws ObjectStoreException {
        osw.close();
    }

    /**
     * @see ObjectStore#isMultiConnection
     */
    public boolean isMultiConnection() {
        return osw.isMultiConnection();
    }

    /**
     * @see ObjectStore#getSequence
     */
    public int getSequence() {
        return osw.getSequence();
    }

    /**
     * @see ObjectStore#getMaxLimit
     */
    public int getMaxLimit() {
        return osw.getMaxLimit();
    }

    /**
     * @see ObjectStore#getMaxOffset
     */
    public int getMaxOffset() {
        return osw.getMaxOffset();
    }

    /**
     * @see ObjectStore#getMaxTime
     */
    public long getMaxTime() {
        return osw.getMaxTime();
    }
}
