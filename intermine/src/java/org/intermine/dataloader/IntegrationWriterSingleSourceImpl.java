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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.flymine.metadata.FieldDescriptor;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.util.DynamicUtil;

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

    /**
     * Constructs a new instance of IntegrationWriterSingleSourceImpl.
     *
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     */
    public IntegrationWriterSingleSourceImpl(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * @see IntegrationWriter#store(FlymineBusinessObject, Source)
     */
    public void store(FlyMineBusinessObject o, Source source) throws ObjectStoreException {
        if (o == null) {
            throw new NullPointerException("Object o should not be null");
        }
        store(o, source, SOURCE);
    }

    /**
     * @see IntegrationWriterAbstractImpl#store(FlyMineBusinessObject, Source, int)
     */
    protected FlyMineBusinessObject store(FlyMineBusinessObject o, Source source,
            int type) throws ObjectStoreException {
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

        if (type == SKELETON) {
            copyFields(o, newObj, source, type);
        }
        objIter = equivalentObjects.iterator();
        while (objIter.hasNext()) {
            FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
            copyFields(obj, newObj, source, FROM_DB);
        }
        if (type == SOURCE) {
            copyFields(o, newObj, source, type);
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
                copyField(srcObj, dest, source, field, type);
            }
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }
}
