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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.util.ConsistentSet;
import org.flymine.util.TypeUtil;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.ReferenceDescriptor;

import org.apache.log4j.Logger;

/**
 * Loads information from a data source into the Flymine database.
 * This class defines the store method, which can be used by the process method of
 * subclasses.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public class DataLoader
{
    protected static final Logger LOG = Logger.getLogger(DataLoader.class);
    protected IntegrationWriter iw;
    
    /**
     * No-arg constructor for testing purposes
     */
    protected DataLoader() {
    }

    /**
     * Construct a DataLoader
     * 
     * @param iw an IntegrationWriter to write to
     */
    public DataLoader(IntegrationWriter iw) {
        this.iw = iw;
    }

    /**
     * Stores an object, with all of the objects referenced by it as skeletons.
     *
     * @param obj an object to store
     * @throws ObjectStoreException if something goes wrong
     */
    public void store(FlyMineBusinessObject obj) throws ObjectStoreException {
        store(obj, new ConsistentSet(), false);
    }

    /**
     * Stores an object, with all of the objects referenced by it as skeletons.
     *
     * @param obj an object to store
     * @param set a Set of objects that we are already dealing with
     * @param skeleton whether this object is a skeleton
     * @throws ObjectStoreException if something goes wrong
     */
    protected void store(FlyMineBusinessObject obj, Set set, boolean skeleton)
        throws ObjectStoreException {
        if (set.contains(obj)) {
            return;
        }
        try {
            set.add(obj);
            IntegrationDescriptor integ = iw.getByExample(obj);
            
            // if object was in database id needs to be set
            if (integ.containsKey("id")) {
                TypeUtil.setFieldValue(obj, "id", integ.get("id"));
            }
            
            Map infos = TypeUtil.getFieldInfos(obj.getClass());
            Iterator iter = infos.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String fieldname = (String) entry.getKey();
                if (!"id".equals(fieldname)) {
                    TypeUtil.FieldInfo info = (TypeUtil.FieldInfo) entry.getValue();
                    Method setter = info.getSetter();
                    Method getter = info.getGetter();
                    Object valueInObjectToStore = getter.invoke(obj, new Object[] {});
                    
                    String className = obj.getClass().getName();
                    ClassDescriptor cld = iw.getObjectStore().getModel()
                        .getClassDescriptorByName(className);
                    FieldDescriptor fd = cld.getFieldDescriptorByName(fieldname);
                    if (fd == null) {
                        throw new ObjectStoreException("Field does not exist that was there"
                                + " before: " + className + "." + fieldname);
                    } else if (fd instanceof CollectionDescriptor) {
                        Collection objs = (Collection) valueInObjectToStore;
                        if (objs != null) {  // if any collection members in new object store them
                            Iterator objIter = objs.iterator();
                            while (objIter.hasNext()) {
                                FlyMineBusinessObject subObj = (FlyMineBusinessObject) objIter.next();
                                store(subObj, set, true);
                            }
                        }
                    } else {
                        if (fd instanceof ReferenceDescriptor) {
                            if (valueInObjectToStore != null) {
                                store((FlyMineBusinessObject) valueInObjectToStore, set, true);
                            }
                        }
                        // A normal attribute, which should be set if the IntegrationDescriptor
                        // thinks so.
                        if (integ.containsKey(fieldname)) {
                            Object oldValue = integ.get(fieldname);
                            //if (fd.relationType() == FieldDescriptor.ONE_ONE_RELATION) {
                            // TODO: set the reverse reference to null
                            //}
                            setter.invoke(obj, new Object[] {oldValue});
                        }
                    }
                }
            }
            iw.store(obj, skeleton);
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException("IllegalAccessException was thrown", e);
        } catch (InvocationTargetException e) {
            throw new ObjectStoreException("Something weird in java", e);
        }
    }
}
