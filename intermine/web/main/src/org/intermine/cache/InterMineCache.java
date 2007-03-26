package org.intermine.cache;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.util.CacheMap;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.commons.collections.keyvalue.MultiKey;

/**
 * A caching class that creates new objects when there is a cache miss.  If the get() method fails
 * to find a matching object ObjectCreator.create() is called to make a new object.  The new object
 * is held is a CacheMap for later use.
 * @author Kim Rutherford
 */

public class InterMineCache
{
    private Map objectCreators = new HashMap();
    private Map objectCaches = new HashMap();
    
    /**
     * Create a new cache.
     */
    public InterMineCache () {
        // empty
    }

    /**
     * Register a new ObjectCreator.  The ObjectCreator is used by get() to create objects that are
     * missing from the cache.
     * @param cacheTag the tag to associate with the ObjectCreator
     * @param objectCreator the ObjectCreator
     */
    public void register(String cacheTag, ObjectCreator objectCreator) {
        if (objectCreators.containsKey(cacheTag)) {
            throw new IllegalArgumentException("called register(" + cacheTag + ", ...) twice");
        }

        objectCreators.put(cacheTag, objectCreator);
        
        Map objectCache = (Map) objectCaches.get(cacheTag);
        if (objectCache == null) {
            objectCache = new CacheMap();
            objectCaches.put(cacheTag, objectCache);
        }
    }

    /**
     * Unregister the ObjectCreator with the given cacheTag.
     * @param cacheTag the cache tag to unregister
     */
    public void unregister(String cacheTag) {
        if (!objectCreators.containsKey(cacheTag)) {
            throw new IllegalArgumentException("called unregister(" + cacheTag + ") but "
                                               + cacheTag + " isn't registered");
        }
        
        objectCreators.remove(cacheTag);
        objectCaches.remove(cacheTag);
    }
    
    /**
     * Return a new object associated with cacheTag.  If no object matching arg1 is found one will
     * be created, cached and returned.
     * @param cacheTag the tag specifying which part of the cache to look arg1 in
     * @param arg1 used to look up the object to return (and the argument passed to the constructor
     * of the new object if one is created)
     * @return the new object
     */
    public Serializable get(String cacheTag, Object arg1) {
        CacheMap objectCache = getObjectCache(cacheTag);

        Serializable object = (Serializable) objectCache.get(arg1);
        if (object == null) {
            object = create(cacheTag, new Object[] {arg1});
            objectCache.put(arg1, object);
        }
        
        return object;
    }

    /**
     * Return a new object associated with cacheTag.  If no object matching arg1 is found one will
     * be created, cached and returned.
     * @param cacheTag the tag specifying which part of the cache to look arg1 in
     * @param arg1 first argument used to look up the object to return (and the argument passed to
     * the constructor of the new object if one is created)
     * @param arg2 second argument
     * @return the new object
     */
    public Serializable get(String cacheTag, Object arg1, Object arg2) {
        CacheMap objectCache = getObjectCache(cacheTag);

        MultiKey mkey = new MultiKey(arg1, arg2);
        Serializable object = (Serializable) objectCache.get(mkey);
        if (object == null) {
            object = create(cacheTag, new Object[] {arg1, arg2});
            objectCache.put(mkey, object);
        }
        
        return object;
    }

    /**
     * Return a new object associated with cacheTag.  If no object matching arg1 is found one will
     * be created, cached and returned.
     * @param cacheTag the tag specifying which part of the cache to look arg1 in
     * @param arg1 first argument used to look up the object to return (and the argument passed to
     * the constructor of the new object if one is created)
     * @param arg2 second argument
     * @param arg3 third argument
     * @return the new object
     */
    public Serializable get(String cacheTag, Object arg1, Object arg2, Object arg3) {
        CacheMap objectCache = getObjectCache(cacheTag);

        MultiKey mkey = new MultiKey(arg1, arg2, arg3);
        Serializable object = (Serializable) objectCache.get(mkey);
        if (object == null) {
            object = create(cacheTag, new Object[] {arg1, arg2, arg3});
            objectCache.put(mkey, object);
        }
        
        return object;        
    }
    
    /**
     * Return a new object associated with cacheTag.  If no object matching arg1 is found one will
     * be created, cached and returned.
     * @param cacheTag the tag specifying which part of the cache to look arg1 in
     * @param arg1 first argument used to look up the object to return (and the argument passed to
     * the constructor of the new object if one is created)
     * @param arg2 second argument
     * @param arg3 third argument
     * @param arg4 fourth argument
     * @return the new object
     */
    public Serializable get(String cacheTag, Object arg1, Object arg2, Object arg3, Object arg4) {
        CacheMap objectCache = getObjectCache(cacheTag);

        MultiKey mkey = new MultiKey(arg1, arg2, arg3, arg4);
        Serializable object = (Serializable) objectCache.get(mkey);
        if (object == null) {
            object = create(cacheTag, new Object[] {arg1, arg2, arg3, arg4});
            objectCache.put(mkey, object);
        }
        
        return object;        
    }
    
    private CacheMap getObjectCache(String cacheTag) {
        CacheMap ret = (CacheMap) objectCaches.get(cacheTag);
        if (ret == null) {
            throw new IllegalArgumentException("unknown tag: " + cacheTag);
        }
        return ret;
    }

    /**
     * Create a new object using the ObjectCreator for cacheTag.
     */
    private Serializable create(String cacheTag, Object[] objects) {
        ObjectCreator creator = (ObjectCreator) objectCreators.get(cacheTag);
        Class creatorClass = creator.getClass();
        Class[] classes = new Class[objects.length];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = objects[i].getClass();
        }

        try {
            Method creatorMethod = creatorClass.getMethod("create", classes);
            creatorMethod.setAccessible(true);
            return (Serializable) creatorMethod.invoke(creator, objects);
        } catch (Exception e) {
            throw new RuntimeException("failed to invoke the create method for cache tag \""
                                       + cacheTag + "\"", e);
        }
    }

    /**
     * Remove objects from the cache that match the given keys.  The length of the keys array
     * argument should be the same as the number of keys used when storing the object.  Any nulls
     * in the keys array will be treated as wildcards.  Eg.  If the cache contains keys: 
     * ("foo", "123"), ("foo", "456"), ("bar", "456") then calling flushByKey with {null, "456"}
     * will delete two entries, leaving ("foo", "123").
     * @param cacheTag the cache tag
     * @param keyParts the array of key parts
     */
    public void flushByKey(String cacheTag, Object[] keyParts) {
        Map cache = getObjectCache(cacheTag);
        Set keysToRemove = new HashSet();
        Iterator iter = cache.keySet().iterator();
       ENTRIES:
        while (iter.hasNext()) {
            MultiKey mkey = (MultiKey) iter.next();
            Object[] entryKeyParts = mkey.getKeys();
            for (int i = 0; i < entryKeyParts.length; i++) {
                if (keyParts[i] != null && !entryKeyParts[i].equals(keyParts[i])) {
                    continue ENTRIES;
                }
            }
            // key matched
            keysToRemove.add(mkey);
        }

        iter = keysToRemove.iterator();
        while (iter.hasNext()) {
            cache.remove(iter.next());
        }
    }

    /**
     * Clear the cache of all objects associated with the given cache tag.
     * @param cacheTag the cache tag
     */
    public void flush(String cacheTag) {
        getObjectCache(cacheTag).clear();
    }
    
    /**
     * Remove all objects from the cache.
     */
    public void flushAll() {
        Iterator iter = getTags().iterator();
        while (iter.hasNext()) {
            String cacheTag = (String) iter.next();
            flush(cacheTag);
        }
    }

    /**
     * Return a Set containing the cache tag strings that are currently registered.
     * @return the cache tags
     */
    public Set getTags() {
        return objectCreators.keySet();
    }
    
    /**
     * Return the number of objects associated with the given cache tag.
     * @param cacheTag the cache tag
     * @return the object count
     */
    protected int getCachedObjectCount(String cacheTag) {
        return getObjectCache(cacheTag).keySet().size();
    }
}
