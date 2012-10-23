package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.Clob;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.IntPresentSet;
import org.intermine.util.IntToIntMap;

/**
 * Abstract implementation of ObjectStoreIntegrationWriter.  To retain
 * O/R mapping independence concrete subclasses should delegate writing to
 * a mapping tool specific implementation of ObjectStoreWriter.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */

public abstract class IntegrationWriterAbstractImpl implements IntegrationWriter
{
    private static final Logger LOG = Logger.getLogger(IntegrationWriterAbstractImpl.class);

    protected ObjectStoreWriter osw;
    protected static final int SKELETON = 0;
    protected static final int FROM_DB = 1;
    protected static final int SOURCE = 2;
    protected IntToIntMap idMap = new IntToIntMap();
    protected IntPresentSet dbIdsStored = new IntPresentSet();
    protected int idMapOps = 0;
    protected boolean ignoreDuplicates = false;
    protected HintingFetcher eof;
    protected BaseEquivalentObjectFetcher beof;
    protected Source lastSource = null;
    protected Set<String> seenBrokenOneToMany = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Constructs a new instance of an IntegrationWriter
     *
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     */
    public IntegrationWriterAbstractImpl(ObjectStoreWriter osw) {
        this.osw = osw;
        beof = new BaseEquivalentObjectFetcher(getModel(), idMap, osw);
        eof = new HintingFetcher(beof);
    }

    /**
     * Resets the IntegrationWriter, clearing the id map and the hints
     */
    public void reset() {
        idMap.clear();
        eof = new HintingFetcher(beof);
    }

    /**
     * Returns the base equivalent object fetcher.
     *
     * @return a BaseEquivalentObjectFetcher
     */
    public BaseEquivalentObjectFetcher getBaseEof() {
        return beof;
    }

    /**
     * Sets the equivalent object fetcher.
     *
     * @param eof a HintingFetcher
     */
    public void setEof(HintingFetcher eof) {
        this.eof = eof;
    }

    /**
     * {@inheritDoc}
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
     * Returns a Set of objects from the idMap or database that are equivalent to the given
     * object, according to the primary keys defined by the given Source.
     *
     * @param obj the Object to look for
     * @param source the data Source
     * @return a Set of InterMineObjects
     * @throws ObjectStoreException if an error occurs
     */
    public Set<InterMineObject> getEquivalentObjects(InterMineObject obj,
            Source source) throws ObjectStoreException {
        lastSource = source;
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
                        + " Source object ID: " + obj.toString()
                        + (idMap.size() < 100 ? ", idMap = " : ""));
            }
            if ((obj.getId() == null) || ignoreDuplicates) {
                return beof.queryEquivalentObjects(obj, source);
            } else {
                return eof.queryEquivalentObjects(obj, source);
            }
        } else {
            // was in idMap, no need to query database
            return Collections.singleton((InterMineObject) new ProxyReference(osw, destId,
                    InterMineObject.class));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void store(FastPathObject o, Source source,
            Source skelSource) throws ObjectStoreException {
        if (o == null) {
            throw new NullPointerException("Object o should not be null");
        }
        long time = (new Date()).getTime();
        store(o, source, skelSource, SOURCE);
        long now = (new Date()).getTime();
        if (now - time > 20000) {
            LOG.info("Stored object " + o.getClass().getName() + (o instanceof InterMineObject
                        ? ":" + ((InterMineObject) o).getId() : "") + " - took " + (now - time)
                    + " ms");
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
    protected abstract InterMineObject store(FastPathObject o, Source source,
            Source skelSource, int type) throws ObjectStoreException;

    protected long timeSpentRecursing = 0;

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
     * @throws ObjectStoreException if an error occurs in the underlying objectstore
     */
    protected void copyField(FastPathObject srcObj, FastPathObject dest,
            Source source, Source skelSource, FieldDescriptor field,
            int type) throws IllegalAccessException, ObjectStoreException {
        String fieldName = field.getName();
        if (!"id".equals(fieldName)) {
            switch (field.relationType()) {
                case FieldDescriptor.NOT_RELATION:
                    Object value = srcObj.getFieldValue(fieldName);
                    if (value instanceof ClobAccess) {
                        // Don't copy field if this is a skeleton - this avoids creating the Clob
                        // twice
                        if ((type == FROM_DB) || (type == SOURCE)) {
                            dest.setFieldValue(fieldName, value);
                        }
                    } else {
                        dest.setFieldValue(fieldName, value);
                    }
                    break;
                case FieldDescriptor.N_ONE_RELATION:
                    if ((type == FROM_DB) || (type == SOURCE)
                            || DataLoaderHelper.fieldIsPrimaryKey(getModel(),
                                dest.getClass(), fieldName, source)) {
                        if (type == FROM_DB) {
                            dest.setFieldValue(fieldName, srcObj.getFieldProxy(fieldName));
                        } else {
                            InterMineObject sourceTarget = (InterMineObject)
                                srcObj.getFieldProxy(fieldName);
                            if (sourceTarget instanceof ProxyReference) {
                                if (idMap.get(sourceTarget.getId()) == null) {
                                    if (type == SOURCE) {
                                        if (srcObj instanceof InterMineObject) {
                                            LOG.error("Having to fetch reference " + fieldName
                                                    + " for object with ID "
                                                    + ((InterMineObject) srcObj).getId()
                                                    + " referencing object with ID "
                                                    + sourceTarget.getId());
                                        }
                                    }
                                    sourceTarget = ((ProxyReference) sourceTarget).getObject();
                                }
                            }
                            long time1 = System.currentTimeMillis();
                            InterMineObject target = store(sourceTarget, source, skelSource,
                                    SKELETON);
                            long time2 = System.currentTimeMillis();
                            timeSpentRecursing += time2 - time1;
                            dest.setFieldValue(fieldName, target);
                        }
                    }
                    break;
                case FieldDescriptor.ONE_ONE_RELATION:
                    if ((type == FROM_DB) || (type == SOURCE)) {
                        InterMineObject loser = (InterMineObject) dest.getFieldValue(fieldName);
                        ReferenceDescriptor reverseRef = ((ReferenceDescriptor) field)
                            .getReverseReferenceDescriptor();
                        if (loser != null) {
                            invalidateObjectById(loser.getId());
                            try {
                                loser.setFieldValue(reverseRef.getName(), null);
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
                            target = (InterMineObject) srcObj.getFieldProxy(fieldName);
                            if ((target != null) && (target.getId() != null)
                                    && (idMap.get(target.getId()) == null)
                                    && (target instanceof ProxyReference)) {
                                target = ((ProxyReference) target).getObject();
                            }
                            long time1 = System.currentTimeMillis();
                            target = store(target, source, skelSource, SKELETON);
                            long time2 = System.currentTimeMillis();
                            timeSpentRecursing += time2 - time1;
                        } else {
                            target = (InterMineObject) srcObj.getFieldValue(fieldName);
                        }
                        /*if (target != null) {
                            if (target instanceof ProxyReference) {
                                LOG.debug("Reifying object for modification in place");
                                target = ((ProxyReference) target).getObject();
                            }
                            InterMineObject targetsReferent = (InterMineObject)
                                target.getFieldValue(reverseRef.getName());
                            if ((targetsReferent != null) && (!targetsReferent.equals(dest))) {
                                invalidateObjectById(targetsReferent.getId());
                                targetsReferent.setFieldValue(fieldName, null);
                                store(targetsReferent);
                            }
                            target.setFieldValue(reverseRef.getName(), dest);
                            store(target);
                        }*/
                        dest.setFieldValue(fieldName, target);
                    }
                    break;
                case FieldDescriptor.ONE_N_RELATION:
                    // Do nothing.
                    if (type == SOURCE) {
                        String brokenOneToMany = field.getClassDescriptor().getUnqualifiedName()
                            + "." + field.getName();
                        if (!seenBrokenOneToMany.contains(brokenOneToMany)) {
                            @SuppressWarnings("unchecked") Collection<InterMineObject> col =
                                (Collection) srcObj.getFieldValue(fieldName);
                            if (!col.isEmpty()) {
                                LOG.error("Data source should not contain data in the one to many "
                                        + "relation " + brokenOneToMany + ", as it is ignored. "
                                        + "Put the data into the reverse reference instead.");
                                seenBrokenOneToMany.add(brokenOneToMany);
                            }
                        }
                    }
                    break;
                case FieldDescriptor.M_N_RELATION:
                    if ((type == SOURCE) || ((type == FROM_DB)
                                && ((((InterMineObject) dest).getId() == null)
                                || (!((InterMineObject) dest).getId()
                                    .equals(((InterMineObject) srcObj).getId()))))) {
                        @SuppressWarnings("unchecked") Collection<InterMineObject> destCol =
                            (Collection) dest.getFieldValue(fieldName);
                        @SuppressWarnings("unchecked") Collection<InterMineObject> col =
                            (Collection) srcObj.getFieldValue(fieldName);
                        for (InterMineObject colObj : col) {
                            if (type == FROM_DB) {
                                destCol.add(colObj);
                            } else {
                                try {
                                    long time1 = System.currentTimeMillis();
                                    destCol.add(store(colObj, source, skelSource, SKELETON));
                                    long time2 = System.currentTimeMillis();
                                    timeSpentRecursing += time2 - time1;
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
                default:
                    throw new Error("Unrecognised relation type " + field.relationType());
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
    public InterMineObject getObjectById(Integer id, Class<? extends InterMineObject> clazz)
        throws ObjectStoreException {
        return osw.getObjectById(id, clazz);
    }

    /**
     * {@inheritDoc}
     */
    public List<InterMineObject> getObjectsByIds(Collection<Integer> ids)
        throws ObjectStoreException {
        return osw.getObjectsByIds(ids);
    }

    /**
     * Store an object in this ObjectStore, delegates to internal ObjectStoreWriter.
     *
     * @param o the object to store
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public void store(Object o) throws ObjectStoreException {
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
    public void addToCollection(Integer hasId, Class<?> clazz, String fieldName, Integer hadId)
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
     * {@inheritDoc}
     */
    public void delete(QueryClass qc, Constraint c) throws ObjectStoreException {
        osw.delete(qc, c);
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
    public void addAllToBag(ObjectStoreBag osb,
            Collection<Integer> coll) throws ObjectStoreException {
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
    public void removeAllFromBag(ObjectStoreBag osb,
            Collection<Integer> coll) throws ObjectStoreException {
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
     * Creates a new empty Clob that is valid for this ObjectStore.
     *
     * @return a Clob
     * @throws ObjectStoreException if an error occurs fetching a new ID
     */
    public Clob createClob() throws ObjectStoreException {
        return osw.createClob();
    }

    /**
     * {@inheritDoc}
     */
    public void replaceClob(Clob clob, String text) throws ObjectStoreException {
        osw.replaceClob(clob, text);
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
     * {@inheritDoc}
     */
    public void batchCommitTransaction() throws ObjectStoreException {
        osw.batchCommitTransaction();
    }

    /**
     * {@inheritDoc}
     */
    public ObjectStore getObjectStore() {
        return osw.getObjectStore();
    }

    /**
     * {@inheritDoc}
     */
    public Results execute(Query q) {
        return osw.execute(q);
    }

    /**
     * {@inheritDoc}
     */
    public Results execute(Query q, int batchSize, boolean optimise, boolean explain,
            boolean prefetch) {
        return osw.execute(q, batchSize, optimise, explain, prefetch);
    }

    /**
     * {@inheritDoc}
     */
    public SingletonResults executeSingleton(Query q) {
        return osw.executeSingleton(q);
    }

    /**
     * {@inheritDoc}
     */
    public SingletonResults executeSingleton(Query q, int batchSize, boolean optimise,
            boolean explain, boolean prefetch) {
        return osw.executeSingleton(q, batchSize, optimise, explain, prefetch);
    }

    /**
     * {@inheritDoc}
     */
    public List<ResultsRow<Object>> execute(Query q, int start, int limit, boolean optimise,
            boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {
        return osw.execute(q, start, limit, optimise, explain, sequence);
    }

    /**
     * {@inheritDoc}
     */
    public void prefetchObjectById(Integer id) {
        osw.prefetchObjectById(id);
    }

    /**
     * {@inheritDoc}
     */
    public void invalidateObjectById(Integer id) {
        osw.invalidateObjectById(id);
    }

    /**
     * {@inheritDoc}
     */
    public Object cacheObjectById(Integer id, InterMineObject obj) {
        return osw.cacheObjectById(id, obj);
    }

    /**
     * {@inheritDoc}
     */
    public void flushObjectById() {
        osw.flushObjectById();
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject pilferObjectById(Integer id) {
        return osw.pilferObjectById(id);
    }

    /**
     * {@inheritDoc}
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return osw.estimate(q);
    }

    /**
     * {@inheritDoc}
     */
    public int count(Query q, Map<Object, Integer> sequence) throws ObjectStoreException {
        return osw.count(q, sequence);
    }

    /**
     * {@inheritDoc}
     */
    public Model getModel() {
        return osw.getModel();
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject getObjectByExample(InterMineObject o,
            Set<String> fieldNames) throws ObjectStoreException {
        return osw.getObjectByExample(o, fieldNames);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws ObjectStoreException {
        osw.close();
        beof.close(lastSource);
        eof.close(lastSource);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiConnection() {
        return osw.isMultiConnection();
    }

    /**
     * {@inheritDoc}
     */
    public Set<Object> getComponentsForQuery(Query q) {
        return osw.getComponentsForQuery(q);
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Integer> getSequence(Set<Object> tables) {
        return osw.getSequence(tables);
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxLimit() {
        return osw.getMaxLimit();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxOffset() {
        return osw.getMaxOffset();
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxTime() {
        return osw.getMaxTime();
    }

    /**
     * {@inheritDoc}
     */
    public ObjectStoreWriter getNewWriter() {
        throw new UnsupportedOperationException("This IntegrationWriter can't be used to get a "
                + "new ObjectStoreWriter");
    }
}
