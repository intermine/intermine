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

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.proxy.ProxyReference;
import org.flymine.util.DynamicUtil;

import org.apache.log4j.Logger;

/**
 * Priority-based implementation of IntegrationWriter. Allows field values to be chosen according
 * to the relative priorities of the data sources that originated them.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class IntegrationWriterDataTrackingImpl extends IntegrationWriterAbstractImpl
{
    protected static final Logger LOG = Logger.getLogger(IntegrationWriterDataTrackingImpl.class);
    protected DataTracker dataTracker;

    /**
     * Creates a new instance of this class, given the properties defining it.
     *
     * @param props the Properties
     * @return an instance of this class
     * @throws ObjectStoreException sometimes
     */
    public static IntegrationWriterDataTrackingImpl getInstance(Properties props) 
            throws ObjectStoreException {
        String writerAlias = props.getProperty("osw");
        if (writerAlias == null) {
            throw new ObjectStoreException(props.getProperty("alias") + " does not have an osw"
                    + " alias specified (check properties file)");
        }

        String trackerAlias = props.getProperty("datatracker");
        if (trackerAlias == null) {
            throw new ObjectStoreException(props.getProperty("alias") + " does not have a"
                    + " datatracker alias specified (check properties file)");
        }

        ObjectStoreWriter writer = ObjectStoreWriterFactory.getObjectStoreWriter(writerAlias);
        DataTracker dataTracker = DataTrackerFactory.getDataTracker(trackerAlias);
        return new IntegrationWriterDataTrackingImpl(writer, dataTracker);
    }

    /**
     * Constructs a new instance of IntegrationWriterDataTrackingImpl.
     *
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     * @param dataTracker an instance of DataTracker, which we can use to store data tracking
     * information
     */
    public IntegrationWriterDataTrackingImpl(ObjectStoreWriter osw, DataTracker dataTracker) {
        super(osw);
        this.dataTracker = dataTracker;
    }

    /**
     * @see IntegrationWriter#getMainSource
     */
    public Source getMainSource(String name) throws ObjectStoreException {
        return dataTracker.stringToSource(name);
    }

    /**
     * @see IntegrationWriter#getSkeletonSource
     */
    public Source getSkeletonSource(String name) throws ObjectStoreException {
        return dataTracker.stringToSource("skel_" + name);
    }

    /**
     * Returns the data tracker being used.
     *
     * @return dataTracker
     */
    protected DataTracker getDataTracker() {
        return dataTracker;
    }
    
    /**
     * @see IntegrationWriterAbstractImpl#store(FlyMineBusinessObject, Source, Source, int)
     */
    protected FlyMineBusinessObject store(FlyMineBusinessObject o, Source source, Source skelSource,
            int type) throws ObjectStoreException {
        if (o == null) {
            return null;
        }
        String oText = o.toString();
        int oTextLength = oText.length();
        //System//.out.println(" --------------- Store called on "
        //        + o.substring(oTextLength > 60 ? 60 : oTextLength));
        Set equivalentObjects = getEquivalentObjects(o, source);
        if ((equivalentObjects.size() == 1) && (type == SKELETON)) {
            FlyMineBusinessObject onlyEquivalent = (FlyMineBusinessObject)
                equivalentObjects.iterator().next();
            if (onlyEquivalent instanceof ProxyReference) {
                return onlyEquivalent;
            }
        }
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
            if (obj instanceof ProxyReference) {
                obj = ((ProxyReference) obj).getObject();
            }
            classes.addAll(DynamicUtil.decomposeClass(obj.getClass()));
        }
        FlyMineBusinessObject newObj = (FlyMineBusinessObject) DynamicUtil.createObject(classes);
        newObj.setId(newId);

        Map trackingMap = new HashMap();
        try {
            Map fieldToEquivalentObjects = new HashMap();
            Map fieldDescriptors = getModel().getFieldDescriptorsForClass(newObj.getClass());
            Iterator fieldIter = fieldDescriptors.entrySet().iterator();
            while (fieldIter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) ((Map.Entry) fieldIter.next()).getValue();
                String fieldName = field.getName();
                if (!"id".equals(fieldName)) {
                    Set sortedEquivalentObjects;
                    
                    if (field instanceof CollectionDescriptor) {
                        sortedEquivalentObjects = new HashSet();
                    } else {
                        Comparator compare = new SourcePriorityComparator(dataTracker, field,
                                (type == SOURCE ? source : skelSource), o);
                        sortedEquivalentObjects = new TreeSet(compare);
                    }

                    if (getModel().getFieldDescriptorsForClass(o.getClass())
                            .containsKey(fieldName)) {
                        sortedEquivalentObjects.add(o);
                    }
                    objIter = equivalentObjects.iterator();
                    while (objIter.hasNext()) {
                        FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
                        Source fieldSource = dataTracker.getSource(obj.getId(), fieldName);
                        if ((equivalentObjects.size() == 1) && (fieldSource != null)
                                && (fieldSource.equals(source) || (fieldSource.equals(skelSource)
                                        && (type != SOURCE)))) {
                            if (type != FROM_DB) {
                                assignMapping(o.getId(), obj.getId());
                            }
                            return obj;
                        }
                        if (getModel().getFieldDescriptorsForClass(obj.getClass())
                                .containsKey(fieldName)) {
                            sortedEquivalentObjects.add(obj);
                        }
                    }
                    fieldToEquivalentObjects.put(field, sortedEquivalentObjects);
                }
            }

            Iterator fieldToEquivIter = fieldToEquivalentObjects.entrySet().iterator();
            while (fieldToEquivIter.hasNext()) {
                Source lastSource = null;
                Map.Entry fieldToEquivEntry = (Map.Entry) fieldToEquivIter.next();
                FieldDescriptor field = (FieldDescriptor) fieldToEquivEntry.getKey();
                Set sortedEquivalentObjects = (Set) fieldToEquivEntry.getValue();
                String fieldName = field.getName();
                    
                objIter = sortedEquivalentObjects.iterator();
                while (objIter.hasNext()) {
                    FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
                    if (obj == o) {
                        copyField(obj, newObj, source, skelSource, field, type);
                        lastSource = (type == SOURCE ? source : skelSource);
                    } else {
                        Source fieldSource = dataTracker.getSource(obj.getId(), fieldName);
                        copyField(obj, newObj, fieldSource, fieldSource, field, FROM_DB);
                        lastSource = fieldSource;
                    }
                }
                trackingMap.put(fieldName, lastSource);
            }
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
        store(newObj);
        
        // We have called store() on an object, and we are about to write all of its data tracking
        // data. We should tell the data tracker, ONLY IF THE ID OF THE OBJECT IS NEW, so that
        // the data tracker can cache the writes without having to ask the db if records for that
        // objectid already exist - we know there aren't.
        if (newId == null) {
            dataTracker.clearObj(newObj.getId());
        }

        Iterator trackIter = trackingMap.entrySet().iterator();
        while (trackIter.hasNext()) {
            Map.Entry trackEntry = (Map.Entry) trackIter.next();
            String fieldName = (String) trackEntry.getKey();
            Source lastSource = (Source) trackEntry.getValue();
            dataTracker.setSource(newObj.getId(), fieldName, lastSource);
        }

        while (equivalentIter.hasNext()) {
            FlyMineBusinessObject objToDelete = (FlyMineBusinessObject) equivalentIter.next();
            delete(objToDelete);
        }

        if (type != FROM_DB) {
            assignMapping(o.getId(), newObj.getId());
        }
        return newObj;
    }

    /**
     * @see IntegrationWriterAbstractImpl#commitTransaction
    public void commitTransaction() throws ObjectStoreException {
        osw.commitTransaction();
        dataTracker.flush();
    }
     */

    /**
     * @see IntegrationWriterAbstractImpl#close
     */
    public void close() {
        osw.close();
        dataTracker.flush();
    }
}
