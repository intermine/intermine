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

//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.util.DynamicUtil;
import org.flymine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Simple implementation of IntegrationWriter - assumes that this is the only (or first) data source
 * to be written to the database.
 *
 * @author Matthew Wakeling
 */
public class IntegrationWriterSingleSourceImpl extends IntegrationWriterAbstractImpl
{
    protected static final Logger LOG = Logger.getLogger(IntegrationWriterSingleSourceImpl.class);
    private static final int SKELETON = 0;
    private static final int FROM_DB = 1;
    private static final int SOURCE = 2;

    /**
     * Constructs a new instance of IntegrationWriterSingleSourceImpl.
     *
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     */
    public IntegrationWriterSingleSourceImpl(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * @see IntegrationWriter#store
     */
    public void store(FlyMineBusinessObject o, Source source) throws ObjectStoreException {
        if (o == null) {
            throw new NullPointerException("Object o should not be null");
        }
        store(o, source, false);
    }

    /**
     * Stores an object in this ObjectStore - recurses.
     *
     * @param o the Object to store
     * @param source the data source that provided this object
     * @param skeleton true if this object is a skeleton
     * @throws ObjectStoreException if an error occurs during the storage of the object
     */
    private FlyMineBusinessObject store(FlyMineBusinessObject o, Source source,
            boolean skeleton) throws ObjectStoreException {
        if (o == null) {
            return null;
        }
        Set equivalentObjects = getEquivalentObjects(o, source);
        Integer newId = null;
        Iterator equivalentIter = equivalentObjects.iterator();
        if (equivalentIter.hasNext()) {
            newId = ((FlyMineBusinessObject) equivalentIter.next()).getId();
        }
        Set classes = new HashSet();
        classes.addAll(DynamicUtil.decomposeClass(o.getClass()));
        Iterator objIter = equivalentObjects.iterator();
        while (objIter.hasNext()) {
            FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
            classes.addAll(DynamicUtil.decomposeClass(obj.getClass()));
        }
        FlyMineBusinessObject newObj = (FlyMineBusinessObject) DynamicUtil.createObject(classes);
        newObj.setId(newId);

        if (skeleton) {
            copyFields(o, newObj, source, SKELETON);
        }
        objIter = equivalentObjects.iterator();
        while (objIter.hasNext()) {
            FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
            copyFields(obj, newObj, source, FROM_DB);
        }
        if (!skeleton) {
            copyFields(o, newObj, source, SOURCE);
        }
        store(newObj);
 
        while (equivalentIter.hasNext()) {
            FlyMineBusinessObject objToDelete = (FlyMineBusinessObject) equivalentIter.next();
            delete(objToDelete);
        }

        return newObj;
    }

    private void copyFields(FlyMineBusinessObject srcObj, FlyMineBusinessObject dest,
            Source source, int type) throws ObjectStoreException {
        try {
            Map fieldDescriptors = getModel().getFieldDescriptorsForClass(srcObj.getClass());
            Iterator fieldIter = fieldDescriptors.entrySet().iterator();
            while (fieldIter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) ((Map.Entry) fieldIter.next()).getValue();
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
                                            TypeUtil.getFieldValue(srcObj, fieldName));
                                } else {
                                    FlyMineBusinessObject target = store((FlyMineBusinessObject)
                                            TypeUtil.getFieldValue(srcObj, fieldName), source,
                                            true);
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
                                                + field.getName() + ", type is "
                                                + field.relationType());
                                    }
                                    store(loser);
                                }
                                FlyMineBusinessObject target = null;
                                if (type == SOURCE) {
                                    target = store((FlyMineBusinessObject)
                                            TypeUtil.getFieldValue(srcObj, fieldName), source,
                                            true);
                                } else {
                                    target = (FlyMineBusinessObject) TypeUtil.getFieldValue(srcObj,
                                            fieldName);
                                }
                                if (target != null) {
                                    FlyMineBusinessObject targetsReferent = (FlyMineBusinessObject)
                                        TypeUtil.getFieldValue(target, reverseRef.getName());
                                    if (targetsReferent != null) {
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
                                Collection col = (Collection) TypeUtil.getFieldValue(srcObj,
                                        fieldName);
                                Iterator colIter = col.iterator();
                                while (colIter.hasNext()) {
                                    FlyMineBusinessObject colObj = (FlyMineBusinessObject) colIter
                                        .next();
                                    invalidateObjectById(colObj.getId());
                                    ReferenceDescriptor reverseRef = ((CollectionDescriptor) field)
                                        .getReverseReferenceDescriptor();
                                    TypeUtil.setFieldValue(colObj, reverseRef.getName(), dest);
                                    store(colObj, source, true);
                                }
                            }
                            break;
                        case FieldDescriptor.M_N_RELATION:
                            if ((type == SOURCE) || ((type == FROM_DB) && ((dest.getId() == null)
                                        || (!dest.getId().equals(srcObj.getId()))))) {
                                Collection destCol = (Collection) TypeUtil.getFieldValue(dest,
                                        fieldName);
                                Collection col = (Collection) TypeUtil.getFieldValue(srcObj,
                                        fieldName);
                                Iterator colIter = col.iterator();
                                while (colIter.hasNext()) {
                                    FlyMineBusinessObject colObj = (FlyMineBusinessObject) colIter
                                        .next();
                                    invalidateObjectById(colObj.getId());
                                    ReferenceDescriptor reverseRef = ((CollectionDescriptor) field)
                                        .getReverseReferenceDescriptor();
                                    TypeUtil.setFieldValue(colObj, reverseRef.getName(),
                                            Collections.singletonList(dest));
                                    if (type == FROM_DB) {
                                        destCol.add(colObj);
                                    } else {
                                        destCol.add(store(colObj, source, true));
                                    }
                                }
                            }
                            break;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }
}
