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
import java.util.Iterator;
import java.util.Map;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.model.datatracking.Field;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
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
    private static HashMap dataTrackerToCache = new HashMap();

    /**
     * Retrieve the Source for a specified field of an object stored in the database.
     *
     * @param obj the object
     * @param field the name of the field
     * @param os the ObjectStore used for datatracking
     * @return the Source
     * @throws ObjectStoreException if an error occurs
     */
    public static Source getSource(FlyMineBusinessObject obj, String field, ObjectStore os)
        throws ObjectStoreException {
        Field fieldObj = getField(obj, field, os);
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
     * @param os the ObjectStore used for datatracking
     * @return the Field
     * @throws ObjectStoreException if an error occurs
     */
    public static Field getField(FlyMineBusinessObject obj, String field, ObjectStore os)
            throws ObjectStoreException {
        if (obj.getId() != null) {
            Object cacheId = os;
            if (cacheId instanceof ObjectStoreWriter) {
                cacheId = ((ObjectStoreWriter) cacheId).getObjectStore();
            }
            Map cache = (Map) dataTrackerToCache.get(cacheId);
            if (cache == null) {
                cache = new CacheMap();
                dataTrackerToCache.put(cacheId, cache);
            }
            Map cachedObject = (Map) cache.get(obj.getId());
            if (cachedObject == null) {
                cachedObject = new HashMap();
                Query q = new Query();
                QueryClass qc1 = new QueryClass(Field.class);
                QueryField qf = new QueryField(qc1, "objectId");
                SimpleConstraint sc = new SimpleConstraint(qf,
                        ConstraintOp.EQUALS, new QueryValue(obj.getId()));
                q.setConstraint(sc);
                q.addFrom(qc1);
                q.addToSelect(qc1);

                SingletonResults res = new SingletonResults(q, os);
                Iterator resIter = res.iterator();
                while (resIter.hasNext()) {
                    Field fieldObj = (Field) resIter.next();
                    String fieldName = fieldObj.getName();
                    if (cachedObject.containsKey(fieldName)) {
                        throw new ObjectStoreException("Found more than one source for field '"
                                + field + "' in object with id '" + obj.getId() + "'");
                    }
                    cachedObject.put(fieldName, fieldObj);
                }
                cache.put(obj.getId(), cachedObject);
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
        
        Object cacheId = osw;
        if (cacheId instanceof ObjectStoreWriter) {
            cacheId = ((ObjectStoreWriter) cacheId).getObjectStore();
        }
        Map cache = (Map) dataTrackerToCache.get(cacheId);
        if (cache == null) {
            cache = new CacheMap();
            dataTrackerToCache.put(cacheId, cache);
        }
        Map cachedObject = (Map) cache.get(obj.getId());
        if (cachedObject != null) {
            cachedObject.put(field, f);
        }
    }
}
