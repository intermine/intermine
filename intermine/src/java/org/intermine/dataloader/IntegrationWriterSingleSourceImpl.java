package org.intermine.dataloader;

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
import java.util.Properties;
import java.util.Set;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.FlyMineBusinessObject;
import org.intermine.model.datatracking.Source;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;

import org.apache.log4j.Logger;

/**
 * Simple implementation of IntegrationWriter. Always overrides whatever is already in the
 * objectstore.
 *
 * @author Matthew Wakeling
 */
public class IntegrationWriterSingleSourceImpl extends IntegrationWriterAbstractImpl
{
    protected static final Logger LOG = Logger.getLogger(IntegrationWriterSingleSourceImpl.class);

    /**
     * Creates a new instance of this class, given the properties defining it.
     *
     * @param props the Properties
     * @return an instance of this class
     * @throws ObjectStoreException sometimes
     */
    public static IntegrationWriterSingleSourceImpl getInstance(Properties props) 
            throws ObjectStoreException {
        String writerAlias = props.getProperty("osw");
        if (writerAlias == null) {
            throw new ObjectStoreException(props.getProperty("alias") + " does not have an osw"
                    + " alias specified (check properties file)");
        }

        ObjectStoreWriter writer = ObjectStoreWriterFactory.getObjectStoreWriter(writerAlias);
        return new IntegrationWriterSingleSourceImpl(writer);
    }
    
    /**
     * Constructs a new instance of IntegrationWriterSingleSourceImpl.
     *
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     */
    public IntegrationWriterSingleSourceImpl(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * @see IntegrationWriter#getMainSource
     */
    public Source getMainSource(String name) throws ObjectStoreException {
        Source retval = new Source();
        retval.setName(name);
        retval.setSkeleton(false);
        return retval;
    }

    /**
     * @see IntegrationWriter#getSkeletonSource
     */
    public Source getSkeletonSource(String name) throws ObjectStoreException {
        Source retval = new Source();
        retval.setName(name);
        retval.setSkeleton(true);
        return retval;
    }

    /**
     * @see IntegrationWriterAbstractImpl#store(FlyMineBusinessObject, Source, Source, int)
     */
    protected FlyMineBusinessObject store(FlyMineBusinessObject o, Source source, Source skelSource,
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
            copyFields(o, newObj, source, skelSource, type);
        }
        objIter = equivalentObjects.iterator();
        while (objIter.hasNext()) {
            FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
            copyFields(obj, newObj, source, skelSource, FROM_DB);
        }
        if (type == SOURCE) {
            copyFields(o, newObj, source, skelSource, type);
        }
        store(newObj);
 
        while (equivalentIter.hasNext()) {
            FlyMineBusinessObject objToDelete = (FlyMineBusinessObject) equivalentIter.next();
            delete(objToDelete);
        }

        return newObj;
    }

    private void copyFields(FlyMineBusinessObject srcObj, FlyMineBusinessObject dest,
            Source source, Source skelSource, int type) throws ObjectStoreException {
        try {
            Map fieldDescriptors = getModel().getFieldDescriptorsForClass(srcObj.getClass());
            Iterator fieldIter = fieldDescriptors.entrySet().iterator();
            while (fieldIter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) ((Map.Entry) fieldIter.next()).getValue();
                copyField(srcObj, dest, source, skelSource, field, type);
            }
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }
}
