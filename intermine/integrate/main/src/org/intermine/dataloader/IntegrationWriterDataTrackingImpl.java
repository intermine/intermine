package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.lang.reflect.Constructor;

import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.sql.Database;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IntPresentSet;
import org.intermine.util.TypeUtil;

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
    private static final Logger LOG = Logger.getLogger(IntegrationWriterDataTrackingImpl.class);
    protected DataTracker dataTracker;
    protected IntPresentSet skeletons = new IntPresentSet();

    /**
     * Creates a new instance of this class, given the properties defining it.
     *
     * @param osAlias the alias of this objectstore
     * @param props the Properties
     * @return an instance of this class
     * @throws ObjectStoreException sometimes
     */
    public static IntegrationWriterDataTrackingImpl getInstance(String osAlias, Properties props)
        throws ObjectStoreException {
        return getInstance(osAlias, props, IntegrationWriterDataTrackingImpl.class,
                           DataTracker.class);
    }


    /**
     * Creates a new IntegrationWriter instance of the specified class and and with a specificed
     * DataTracker class plus properties.
     *
     * @param osAlias the alias of this objectstore
     * @param props the Properties
     * @param iwClass Class of IntegrationWriter to create - IntegrationWriterDatatrackingImpl
     *                or a subclass.
     * @param trackerClass Class of DataTracker to use with IntegrationWriter
     * @return an instance of this class
     * @throws ObjectStoreException sometimes
     */
    protected static IntegrationWriterDataTrackingImpl getInstance(String osAlias, Properties props,
                                                                  Class iwClass, Class trackerClass)
        throws ObjectStoreException {
        String writerAlias = props.getProperty("osw");
        if (writerAlias == null) {
            throw new ObjectStoreException(props.getProperty("alias") + " does not have an osw"
                    + " alias specified (check properties file)");
        }

        String trackerMaxSizeString = props.getProperty("datatrackerMaxSize");
        String trackerCommitSizeString = props.getProperty("datatrackerCommitSize");
        if (trackerMaxSizeString == null) {
            throw new ObjectStoreException(props.getProperty("alias") + " does not have a"
                    + " datatracker maximum size specified (check properties file)");
        }
        if (trackerCommitSizeString == null) {
            throw new ObjectStoreException(props.getProperty("alias") + " does not have a"
                    + " datatracker commit size specified (check properties file)");
        }

        ObjectStoreWriter writer = ObjectStoreWriterFactory.getObjectStoreWriter(writerAlias);
        try {
            int maxSize = Integer.parseInt(trackerMaxSizeString);
            int commitSize = Integer.parseInt(trackerCommitSizeString);
            Database db = ((ObjectStoreWriterInterMineImpl) writer).getDatabase();
            Constructor con = trackerClass.getConstructor(new Class[]
                {Database.class, Integer.TYPE, Integer.TYPE});
            DataTracker newDataTracker = (DataTracker) con.newInstance(new Object[]
                {db, new Integer(maxSize), new Integer(commitSize)});

            con = iwClass.getConstructor(new Class[] {ObjectStoreWriter.class, DataTracker.class});
            return (IntegrationWriterDataTrackingImpl) con.newInstance(new Object[]
                {writer, newDataTracker});
        } catch (Exception e) {
            IllegalArgumentException e2 = new IllegalArgumentException("Problem instantiating"
                    + " IntegrationWriterDataTrackingImpl " + props.getProperty("alias"));
            e2.initCause(e);
            throw e2;
        }
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
     * {@inheritDoc}
     */
    public Source getMainSource(String name) {
        return dataTracker.stringToSource(name);
    }

    /**
     * {@inheritDoc}
     */
    public Source getSkeletonSource(String name) {
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
     * {@inheritDoc}
     */
    protected InterMineObject store(InterMineObject o, Source source, Source skelSource,
                                    int type) throws ObjectStoreException {
        if (o == null) {
            return null;
        }
        try {
            Set equivObjects = getEquivalentObjects(o, source);
            if ((equivObjects.size() == 1) && (type == SKELETON)) {
                InterMineObject onlyEquivalent = (InterMineObject) equivObjects.iterator().next();
                if (onlyEquivalent instanceof ProxyReference) {
                    //LOG.debug("store() finished trivially for object " + oText);
                    // This onlyEquivalent object MUST have come from the ID map.
                    if (idMap.get(o.getId()) == null) {
                        LOG.error("Got a ProxyReference as the only equivalent object, but not from"
                                + " the ID map! o = " + o);
                    }
                    return onlyEquivalent;
                }
            }
            Integer newId = null;
            // if multiple equivalent objects in database just use id of first one
            Iterator equivalentIter = equivObjects.iterator();
            if (equivalentIter.hasNext()) {
                newId = ((InterMineObject) equivalentIter.next()).getId();
            }
            Set classes = new HashSet();
            classes.addAll(DynamicUtil.decomposeClass(o.getClass()));
            Iterator objIter = equivObjects.iterator();
            while (objIter.hasNext()) {
                InterMineObject obj = (InterMineObject) objIter.next();
                if (obj instanceof ProxyReference) {
                    obj = ((ProxyReference) obj).getObject();
                }
                try {
                    classes.addAll(DynamicUtil.decomposeClass(obj.getClass()));
                } catch (Exception e) {
                    LOG.error("Broken with: " + DynamicUtil.decomposeClass(o.getClass()));
                    throw new ObjectStoreException(e);
                }
            }
            if (newId == null) {
                newId = getSerial();
            }
            InterMineObject newObj = (InterMineObject) DynamicUtil.createObject(classes);
            newObj.setId(newId);
            if (type != FROM_DB) {
                assignMapping(o.getId(), newId);
            }

            Map trackingMap = new HashMap();
            Map fieldToEquivalentObjects = new HashMap();
            Model model = getModel();
            Map fieldDescriptors = model.getFieldDescriptorsForClass(newObj.getClass());
            Set modelFieldNames = fieldDescriptors.keySet();
            Set typeUtilFieldNames = TypeUtil.getFieldInfos(newObj.getClass()).keySet();
            if (!modelFieldNames.equals(typeUtilFieldNames)) {
                throw new ObjectStoreException("Failed to store data not in the model");
            }
            Iterator fieldIter = fieldDescriptors.entrySet().iterator();
            while (fieldIter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) ((Map.Entry) fieldIter.next()).getValue();
                String fieldName = field.getName();
                if (!"id".equals(fieldName)) {
                    Set sortedEquivalentObjects;

                    // always add to collections, resolve other clashes by priority
                    if (field instanceof CollectionDescriptor) {
                        sortedEquivalentObjects = new HashSet();
                    } else {
                        Comparator compare = new SourcePriorityComparator(dataTracker, field,
                             (type == SOURCE ? source : skelSource), o, dbIdsStored, this,
                             source, skelSource);
                        sortedEquivalentObjects = new TreeSet(compare);
                    }

                    if (model.getFieldDescriptorsForClass(o.getClass()).containsKey(fieldName)) {
                        sortedEquivalentObjects.add(o);
                    }
                    objIter = equivObjects.iterator();
                    while (objIter.hasNext()) {
                        InterMineObject obj = (InterMineObject) objIter.next();
                        Source fieldSource = dataTracker.getSource(obj.getId(), fieldName);
                        if ((equivObjects.size() == 1) && (fieldSource != null)
                            && (fieldSource.equals(source)
                            || (fieldSource.equals(skelSource) && (type != SOURCE)))) {
                            if (type == SOURCE) {
                                if (obj instanceof ProxyReference) {
                                    obj = ((ProxyReference) obj).getObject();
                                }
                                String errMessage;
                                if (dbIdsStored.contains(obj.getId())) {
                                    errMessage = "There is already an equivalent "
                                        + "in the database from this source (" + source
                                        + ") from *this* run; new object from source: \"" + o
                                        + "\", object from database (earlier in this run): \""
                                        + obj + "\"; noticed problem while merging field \""
                                        + field.getName() + "\" originally read from source: "
                                        + fieldSource;
                                } else {
                                    errMessage = "There is already an equivalent "
                                        + "in the database from this source (" + source
                                        + ") from a *previous* run; "
                                        + "object from source in this run: \""
                                        + o + "\", object from database: \"" + obj
                                        + "\"; noticed problem while merging field \""
                                        + field.getName() + "\" originally read from source: "
                                        + fieldSource;
                                }

                                if (!ignoreDuplicates) {
                                    LOG.error(errMessage);
                                    throw new IllegalArgumentException(errMessage);
                                }
                            }
                            //LOG.debug("store() finished simply for object " + oText);
                            return obj;
                        }
                        // materialise proxies before searching for this field
                        if (obj instanceof ProxyReference) {
                            ProxyReference newproxy = (ProxyReference) obj;
                            obj = ((ProxyReference) obj).getObject();
                            if (obj == null) {
                                LOG.error("obj is null o: " + o);
                                LOG.error("proxyId " + newproxy.getId());
                                LOG.error("proxy " + newproxy);
                                ObjectStore os = newproxy.getObjectStore();
                                os.invalidateObjectById(newproxy.getId());
                                obj = newproxy.getObject();
                                LOG.error("obj: " + obj);
                            }
                        }
                        try {
                            if (model.getFieldDescriptorsForClass(obj.getClass())
                                            .containsKey(fieldName)) {
                                sortedEquivalentObjects.add(obj);
                            }
                        } catch (RuntimeException e) {
                            LOG.error("fieldName: " + fieldName + " o: " + o + " id: "
                                      + obj.getId() + " obj: " + obj + " obj.getClass(): "
                                      + obj.getClass() + " description: " + model.
                                      getFieldDescriptorsForClass(obj.getClass()));
                            LOG.error("error " , e);
                            throw e;
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
                    InterMineObject obj = (InterMineObject) objIter.next();
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

            store(newObj);

            // We have called store() on an object, and we are about to write all of its data
            // tracking data. We should tell the data tracker, ONLY IF THE ID OF THE OBJECT IS NEW,
            // so that the data tracker can cache the writes without having to ask the db if
            // records for that objectid already exist - we know there aren't.
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
                InterMineObject objToDelete = (InterMineObject) equivalentIter.next();
                delete(objToDelete);
            }

            // keep track of skeletons that are stored and remove when replaced by real object
            if (type == SKELETON) {
                skeletons.add(newObj.getId());
            } else {
                if (skeletons.contains(newObj.getId().intValue())) {
                    skeletons.set(newObj.getId().intValue(), false);
                }
            }
            return newObj;
        } catch (RuntimeException e) {
            throw new RuntimeException("Exception while loading object " + o, e);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException("Exception while loading object " + o, e);
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * {@inheritDoc}
    public void commitTransaction() throws ObjectStoreException {
        osw.commitTransaction();
        dataTracker.flush();
    }
     */

    /**
     * {@inheritDoc}
     */
    public void close() throws ObjectStoreException {
        super.close();
        dataTracker.close();

        // There is a bug somewhere in this code that sometimes allows skeletons to
        // be stored without matching up with the real object object.  The problem
        // seems to be erratic, some runs complete without a problem.  Here we
        // throw an exception if any skeletons have been stored but never replace
        // by a real object to give early warning.
        if (!(skeletons.size() == 0)) {
            LOG.info("Some skeletons where not replaced by real "
                     + "objects: " + skeletons.toString());
            LOG.info("IDMAP CONTENTS:" + idMap.toString());
            throw new ObjectStoreException("Some skeletons where not replaced by real "
                                       + "objects: " + skeletons.size());
        }
    }
}
