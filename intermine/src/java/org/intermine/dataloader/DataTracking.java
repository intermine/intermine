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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.model.datatracking.Field;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.util.CacheMap;

/**
 * Class providing API for datatracking
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 */
public class DataTracking
{
    private static WeakHashMap dataTrackerToCache = new WeakHashMap();
    private static WeakHashMap dataTrackerToPrecache = new WeakHashMap();

    /**
     * Retrieve the Source for a specified field of an object stored in the database.
     *
     * @param obj the object
     * @param field the name of the field
     * @param osw the ObjectStoreWriter used for datatracking
     * @return the Source
     * @throws ObjectStoreException if an error occurs
     */
    public static Source getSource(FlyMineBusinessObject obj, String field, ObjectStoreWriter osw)
        throws ObjectStoreException {
        Field fieldObj = getField(obj, field, osw);
        if (fieldObj != null) {
            return fieldObj.getSource();
        }
        return null;
    }

    /**
     * Retrieve the Field for a specified fieldname of an object stored in the database.
     *
     * @param obj the FlyMineBusinessObject
     * @param field the name of the field
     * @param osw the ObjectStoreWriter used for datatracking
     * @return the Field
     * @throws ObjectStoreException if an error occurs
     */
    public static Field getField(FlyMineBusinessObject obj, String field, ObjectStoreWriter osw)
            throws ObjectStoreException {
        if (obj.getId() != null) {
            Map cache = (Map) dataTrackerToCache.get(osw);
            if (cache == null) {
                cache = new CacheMap();
                dataTrackerToCache.put(osw, cache);
            }
            Map cachedObject = (Map) cache.get(obj.getId().toString());
            if (cachedObject == null) {
                //System//.out.println("Getting datatracking data for id=" + obj.getId()
                //        + " - not cached, field=" + field + ", cache = " + cache.keySet());
                cachedObject = new HashMap();
                Query q = new Query();
                QueryClass qc1 = new QueryClass(Field.class);
                QueryField qf = new QueryField(qc1, "objectId");
                SimpleConstraint sc = new SimpleConstraint(qf,
                        ConstraintOp.EQUALS, new QueryValue(obj.getId()));
                q.setConstraint(sc);
                q.addFrom(qc1);
                q.addToSelect(qc1);

                SingletonResults res = new SingletonResults(q, osw, osw.getSequence());
                Iterator resIter = res.iterator();
                while (resIter.hasNext()) {
                    Field fieldObj = (Field) resIter.next();
                    String fieldName = fieldObj.getName();
                    if (cachedObject.containsKey(fieldName)) {
                        throw new ObjectStoreException("Found more than one source for field '"
                                + fieldName + "' in object with id '" + obj.getId() + "'");
                    }
                    cachedObject.put(fieldName, fieldObj);
                }
                cache.put(obj.getId().toString(), cachedObject);
            //} else {
                //System//.out.println("Getting datatracking data for id=" + obj.getId()
                //        + " -     cached, field=" + field + ", cache = " + cache.keySet());
            }
            return (Field) cachedObject.get(field);
        } else {
            return null;
        }
    }

    /**
     * Set the Source for a field of an object stored in the database
     * 
     * @param obj the object
     * @param field the name of the field
     * @param source the Source of the field - this should be the version already present in the
     *                data tracker
     * @param osw the ObjectStoreWriter used for data tracking
     * @throws ObjectStoreException if an error occurs
     */
    public static void setSource(FlyMineBusinessObject obj, String field, Source source,
            ObjectStoreWriter osw) throws ObjectStoreException {
        if (obj.getId() == null) {
            throw new IllegalArgumentException("obj id is null");
        }
        if (source.getId() == null) {
            throw new IllegalArgumentException("source id is null");
        }
        Field fieldObj = getField(obj, field, osw);
        
        Field f = new Field();
        f.setObjectId(obj.getId());
        f.setName(field);
        if (fieldObj != null) {
            f.setId(fieldObj.getId());
        }
        f.setSource(source);
        osw.store(f);
        
        Map cache = (Map) dataTrackerToCache.get(osw);
        if (cache == null) {
            cache = new CacheMap();
            dataTrackerToCache.put(osw, cache);
        }
        Map cachedObject = (Map) cache.get(obj.getId().toString());
        if (cachedObject != null) {
            cachedObject.put(field, f);
            //System//.out.println("Storing and caching data for  id=" + obj.getId() + ", field="
            //        + field);
        //} else {
            //System//.out.println("Storing data for              id=" + obj.getId() + ", field="
            //        + field);
        }
    }

    /**
     * Clears the cache for a particular object, in preparation for writing all the data for that
     * object. This allows the data tracker to cache the writes that are about to happen.
     *
     * @param obj the object
     * @param osw the ObjectStoreWriter used for data tracking
     */
    public static void clearObj(FlyMineBusinessObject obj, ObjectStoreWriter osw) {
        if (obj.getId() == null) {
            throw new IllegalArgumentException("obj id is null");
        }

        Map cache = (Map) dataTrackerToCache.get(osw);
        if (cache == null) {
            cache = new CacheMap();
            dataTrackerToCache.put(osw, cache);
        }
        cache.put(obj.getId().toString(), new HashMap());
        //System//.out.println("Cleared cache for object      id=" + obj.getId());
    }

    /**
     * Pre-cache the Fields for a specified set of objects stored in the database. The pre-cached
     * data is forcably held in memory until such time as releasePrecached() is called, when it
     * is released to normal CacheMap behaviour. This method will release any previous pre-cached
     * data. The aim of this method is to cause the datatracking reads to be grouped together in a
     * single read at the start, allowing the writes to be batched into a single batch write at the
     * end.
     *
     * @param set a Set of FlyMineBusinessObjects to precache
     * @param osw the ObjectStoreWriter used for datatracking
     */
    public static void precacheObjects(Set set, ObjectStoreWriter osw) {
        try {
            Map cache = (Map) dataTrackerToCache.get(osw);
            if (cache == null) {
                cache = new CacheMap();
                dataTrackerToCache.put(osw, cache);
            }
            dataTrackerToPrecache.remove(osw);
            Set precache = new HashSet();
            Set bag = new HashSet();
            Iterator objIter = set.iterator();
            while (objIter.hasNext()) {
                FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
                if (obj.getId() != null) {
                    String cacheIndex = obj.getId().toString();
                    precache.add(cacheIndex);
                    // Look to see if the object is already cached. Note that the cache is a
                    // CacheMap, so you can't do containsKey() and then get().
                    Map cachedObject = (Map) cache.get(cacheIndex);
                    if (cachedObject != null) {
                        // Put the data *back in* to the cache, but with OUR cache index stored in
                        // precache, so it is held in memory.
                        cache.put(cacheIndex, cachedObject);
                    } else {
                        // Put the object's ID into the bag for our future query.
                        bag.add(obj.getId());
                        cache.put(cacheIndex, new HashMap());
                    }
                }
            }

            // Now, having secured all the data we already have, we need to run a single query to
            // get all the data we do not yet have.
            if (!bag.isEmpty()) {
                Query q = new Query();
                QueryClass qc1 = new QueryClass(Field.class);
                QueryField qf = new QueryField(qc1, "objectId");
                BagConstraint bc = new BagConstraint(qf, ConstraintOp.IN, bag);
                q.setConstraint(bc);
                q.addFrom(qc1);
                q.addToSelect(qc1);

                SingletonResults res = new SingletonResults(q, osw, osw.getSequence());
                Iterator resIter = res.iterator();
                while (resIter.hasNext()) {
                    Field fieldObj = (Field) resIter.next();
                    String fieldName = fieldObj.getName();
                    Integer objId = fieldObj.getObjectId();
                    // We can guarantee that this will not return null, because we asserted that
                    // earlier and we have held onto the keys so the CacheMap will not throw them
                    // away.
                    Map cachedObject = (Map) cache.get(objId.toString());
                    if (cachedObject.containsKey(fieldName)) {
                        throw new ObjectStoreException("Found more than one source for field '"
                                + fieldName + "' in object with id '" + objId + "'");
                    }
                    cachedObject.put(fieldName, fieldObj);
                }
            }
            dataTrackerToPrecache.put(osw, precache);
            //System//.out.println("Precached objects. cache = " + cache.keySet());
        } catch (ObjectStoreException e) {
            // Ignore
        }
    }

    /**
     * Release the precached data into normal CacheMap behaviour.
     *
     * @param osw the ObjectStoreWriter being used for data tracking
     */
    public static void releasePrecached(ObjectStoreWriter osw) {
        Map cache = (Map) dataTrackerToCache.get(osw);
        if (cache == null) {
            cache = new CacheMap();
            dataTrackerToCache.put(osw, cache);
        }
        dataTrackerToPrecache.remove(osw);
    }
}
