package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
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

import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.MetaDataException;
import org.flymine.metadata.Model;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.proxy.ProxyReference;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.util.IntToIntMap;
import org.flymine.util.TypeUtil;

import org.apache.log4j.Logger;

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
    protected static final Logger LOG = Logger.getLogger(IntegrationWriterAbstractImpl.class);

    protected static final int MAX_MAPPINGS = 1000000;
    protected ObjectStoreWriter osw;
    protected static final int SKELETON = 0;
    protected static final int FROM_DB = 1;
    protected static final int SOURCE = 2;
    protected IntToIntMap idMap = new IntToIntMap();
    protected int idMapOps = 0;

    /**
     * Constructs a new instance of an IntegrationWriter
     *
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     */
    public IntegrationWriterAbstractImpl(ObjectStoreWriter osw) {
        this.osw = osw;
    }

    /**
     * Returns a Set of objects from the database that are equivalent to the given object, according
     * to the primary keys defined by the given Source.
     *
     * @param obj the Object to look for
     * @param source the data Source
     * @return a Set of FlyMineBusinessObjects
     * @throws ObjectStoreException if an error occurs
     */
    public Set getEquivalentObjects(FlyMineBusinessObject obj, Source source)
            throws ObjectStoreException {
        if (obj == null) {
            throw new NullPointerException("obj should not be null");
        }
        //String oText = obj.toString();
        //int oTextLength = oText.length();
        //System//.out.println(" --------------- getEquivalentObjects() called on "
        //        + oText.substring(0, oTextLength > 60 ? 60 : oTextLength));
        Integer destId = null;
        if (obj.getId() != null) {
            destId = idMap.get(obj.getId());
        }
        if (destId == null) {
            if (obj instanceof ProxyReference) {
                throw new IllegalArgumentException("Given a ProxyReference, but id not in ID Map");
            }
            Query q = null;
            try {
                q = DataLoaderHelper.createPKQuery(getModel(), obj, source, idMap);
            } catch (MetaDataException e) {
                throw new ObjectStoreException(e);
            }
            //System//.out.println(" --------------------------- " + q);
            SingletonResults retval = new SingletonResults(q, this, getSequence());
            retval.setNoOptimise();
            retval.setNoExplain();
            return retval;
        } else {
            return Collections.singleton(new ProxyReference(osw, destId));
        }
    }

    /**
     * @see IntegrationWriter#store
     */
    public void store(FlyMineBusinessObject o, Source source, Source skelSource)
            throws ObjectStoreException {
        if (o == null) {
            throw new NullPointerException("Object o should not be null");
        }
        long time = (new Date()).getTime();
        store(o, source, skelSource, SOURCE);
        long now = (new Date()).getTime();
        if (now - time > 20000) {
            LOG.error("Stored object " + o.getClass().getName() + ":" + o.getId() + " - took "
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
     * @return the FlyMineBusinessObject that was written to the database
     * @throws ObjectStoreException if an error occurs in the underlying objectstore
     */
    protected abstract FlyMineBusinessObject store(FlyMineBusinessObject o, Source source,
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
    protected void copyField(FlyMineBusinessObject srcObj, FlyMineBusinessObject dest,
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
                            FlyMineBusinessObject sourceTarget = (FlyMineBusinessObject)
                                TypeUtil.getFieldProxy(srcObj, fieldName);
                            if (sourceTarget instanceof ProxyReference) {
                                if (idMap.get(sourceTarget.getId()) == null) {
                                    sourceTarget = ((ProxyReference) sourceTarget).getObject();
                                }
                            }
                            FlyMineBusinessObject target = store(sourceTarget, source, skelSource,
                                    SKELETON);
                            TypeUtil.setFieldValue(dest, fieldName, target);
                        }
                    }
                    break;
                case FieldDescriptor.ONE_ONE_RELATION:
                    if ((type == FROM_DB) || (type == SOURCE)) {
                        FlyMineBusinessObject loser = (FlyMineBusinessObject)
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
                        FlyMineBusinessObject target = null;
                        if (type == SOURCE) {
                            target = store((FlyMineBusinessObject)
                                    TypeUtil.getFieldValue(srcObj, fieldName), source, skelSource,
                                    SKELETON);
                        } else {
                            target = (FlyMineBusinessObject) TypeUtil.getFieldValue(srcObj,
                                    fieldName);
                        }
                        if (target != null) {
                            if (target instanceof ProxyReference) {
                                LOG.error("Reifying object for modification in place");
                                target = ((ProxyReference) target).getObject();
                            }
                            FlyMineBusinessObject targetsReferent = (FlyMineBusinessObject)
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
                            FlyMineBusinessObject colObj = (FlyMineBusinessObject) colIter.next();
                            invalidateObjectById(colObj.getId());
                            ReferenceDescriptor reverseRef = ((CollectionDescriptor) field)
                                .getReverseReferenceDescriptor();
                            TypeUtil.setFieldValue(colObj, reverseRef.getName(), dest);
                            if (type == FROM_DB) {
                                store(colObj);
                                store(colObj, source, skelSource, FROM_DB);
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
                            FlyMineBusinessObject colObj = (FlyMineBusinessObject) colIter.next();
                            if (type == FROM_DB) {
                                destCol.add(colObj);
                            } else {
                                destCol.add(store(colObj, source, skelSource, SKELETON));
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
     */
    public void assignMapping(Integer source, Integer dest) {
        if ((source != null) && (dest != null)) {
            idMap.put(source, dest);
            idMapOps++;
            if (idMapOps % 100000 == 0) {
                LOG.error("idMap size = " + idMap.size() + ", ops = " + idMapOps);
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
    public FlyMineBusinessObject getObjectById(Integer id) throws ObjectStoreException {
        return osw.getObjectById(id);
    }

    /**
     * Store an object in this ObjectStore, delegates to internal ObjectStoreWriter.
     *
     * @param o the object to store
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public void store(FlyMineBusinessObject o) throws ObjectStoreException {
        osw.store(o);
    }

    /**
     * Delete an object from this ObjectStore, delegate to internal ObjectStoreWriter.
     *
     * @param o the object to delete
     * @throws ObjectStoreException if an error occurs during deletion of the object
     */
    public void delete(FlyMineBusinessObject o) throws ObjectStoreException {
        osw.delete(o);
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
     * @see org.flymine.objectstore.ObjectStoreWriter#getObjectStore
     */
    public ObjectStore getObjectStore() {
        return osw.getObjectStore();
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#execute
     */
    public Results execute(Query q) throws ObjectStoreException {
        return osw.execute(q);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        return osw.execute(q, start, limit, optimise, explain, sequence);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#prefetchObjectById
     */
    public void prefetchObjectById(Integer id) {
        osw.prefetchObjectById(id);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#invalidateObjectById
     */
    public void invalidateObjectById(Integer id) {
        osw.invalidateObjectById(id);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#cacheObjectById
     */
    public Object cacheObjectById(Integer id, FlyMineBusinessObject obj) {
        return osw.cacheObjectById(id, obj);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#flushObjectById
     */
    public void flushObjectById() {
        osw.flushObjectById();
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#pilferObjectById
     */
    public FlyMineBusinessObject pilferObjectById(Integer id) {
        return osw.pilferObjectById(id);
    }
    
    /**
     * @see org.flymine.objectstore.ObjectStore#estimate
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return osw.estimate(q);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#count
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        return osw.count(q, sequence);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#getModel
     */
    public Model getModel() {
        return osw.getModel();
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#getObjectByExample
     */
    public FlyMineBusinessObject getObjectByExample(FlyMineBusinessObject o, Set fieldNames)
            throws ObjectStoreException {
        return osw.getObjectByExample(o, fieldNames);
    }

    /**
     * @see org.flymine.objectstore.ObjectStoreWriter#close
     */
    public void close() {
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
}
