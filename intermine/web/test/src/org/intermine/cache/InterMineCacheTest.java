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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 * InterMineCacheTest class
 *
 * @author Kim Rutherford
 */

public class InterMineCacheTest extends TestCase
{
    private final ObjectCreator objectCreator1;
    private final ObjectCreator objectCreator2;
    private final ObjectCreator objectCreator3;
    private final String cacheTag;

    public InterMineCacheTest() {
        objectCreator1 = new ObjectCreator() {
            public Long create(Integer intArg) {
                return new Long(intArg.intValue());
            }
        };    
        objectCreator2 = new ObjectCreator() {
            public CacheTestClass2 create(Integer intArg, String stringArg) {
                return new CacheTestClass2(intArg, stringArg);
            }
        };

        objectCreator3 = new ObjectCreator() {
            public CacheTestClass3 create(Integer intArg, String stringArg, Float floatArg) {
                return new CacheTestClass3(intArg, stringArg, floatArg);
            }
        };

        cacheTag = "tag1";
    }
    
    public void testRegister() throws Exception {
        InterMineCache cache = new InterMineCache();

        cache.register(cacheTag, objectCreator2);

        try {
            cache.register(cacheTag, objectCreator2);
            fail("should get IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        cache.register("test_tag", objectCreator2);

        Set expectedTags = new HashSet();
        expectedTags.add("test_tag");
        expectedTags.add(cacheTag);
        assertEquals(expectedTags, cache.getTags());
    }
    
    public void testUnregister() throws Exception {
        InterMineCache cache = new InterMineCache();

        try {
            cache.unregister(cacheTag);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        cache.register(cacheTag, objectCreator2);

        try {
            cache.register(cacheTag, objectCreator2);
            fail("should get IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testCreate1() throws Exception {
        InterMineCache cache = new InterMineCache();

        String otherTag = "another_tag";
        
        cache.register(cacheTag, objectCreator1);
        cache.register(otherTag, objectCreator1);
        
        Object createdObject = cache.get(cacheTag, new Integer(42));
        Object otherCreatedObject = cache.get(cacheTag, new Integer(13));
        
        assertNotNull(createdObject);
        assertTrue(createdObject instanceof Long);
        
        Long cacheTestClass = (Long) createdObject;
        assertEquals(new Long(42), cacheTestClass);
        
        Object createdObjectRef2 = cache.get(cacheTag, new Integer(42));
        Object otherCreatedObjectRef2 = cache.get(cacheTag, new Integer(13));
        
        // make sure we get the same object both times
        assertEquals(createdObject, createdObjectRef2);
        assertTrue(createdObject == createdObjectRef2);
        assertEquals(otherCreatedObject, otherCreatedObjectRef2);
        assertTrue(otherCreatedObject == otherCreatedObjectRef2);
        
        // make sure we get a different object ref using the other tag
        Object otherTagCreatedObject = cache.get(otherTag, new Integer(42));
        assertEquals(createdObject, otherTagCreatedObject);
        assertTrue(createdObject != otherTagCreatedObject);
        
        try {
            Object testObject = cache.get(cacheTag, new Integer(42), "some_string");
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }
    }
    
    public void testCreate2() throws Exception {
        InterMineCache cache = new InterMineCache();

        String otherTag = "another_tag";
        
        cache.register(cacheTag, objectCreator2);
        cache.register(otherTag, objectCreator2);
        
        Object createdObject = cache.get(cacheTag, new Integer(42), "some_string");
        Object otherCreatedObject = cache.get(cacheTag, new Integer(13), "another_string");
        
        assertNotNull(createdObject);
        assertTrue(createdObject instanceof CacheTestClass2);
        
        CacheTestClass2 cacheTestClass = (CacheTestClass2) createdObject;
        assertEquals("some_string", cacheTestClass.stringArg);
        assertEquals(new Integer(42), cacheTestClass.intArg);
        
        Object createdObjectRef2 = cache.get(cacheTag, new Integer(42), "some_string");
        Object otherCreatedObjectRef2 = cache.get(cacheTag, new Integer(13), "another_string");
        
        // make sure we get the same object both times
        assertEquals(createdObject, createdObjectRef2);
        assertTrue(createdObject == createdObjectRef2);
        assertEquals(otherCreatedObject, otherCreatedObjectRef2);
        assertTrue(otherCreatedObject == otherCreatedObjectRef2);
        
        // make sure we get a different object ref using the other tag
        Object otherTagCreatedObject = cache.get(otherTag, new Integer(42), "some_string");
        assertEquals(createdObject, otherTagCreatedObject);
        assertTrue(createdObject != otherTagCreatedObject);
    }

    public void testCreate3() throws Exception {
        InterMineCache cache = new InterMineCache();

        String otherTag = "another_tag";
        
        cache.register(cacheTag, objectCreator3);
        cache.register(otherTag, objectCreator3);
        
        Object createdObject = cache.get(cacheTag, new Integer(42), "some_string", new Float(1.1));
        Object otherCreatedObject = cache.get(cacheTag, new Integer(13), "another_string", new Float(9.0));
        
        assertNotNull(createdObject);
        assertTrue(createdObject instanceof CacheTestClass3);
        
        CacheTestClass3 cacheTestClass = (CacheTestClass3) createdObject;
        assertEquals("some_string", cacheTestClass.stringArg);
        assertEquals(new Integer(42), cacheTestClass.intArg);
        assertEquals(new Float(1.1), cacheTestClass.floatArg);
        
        Object createdObjectRef2 = cache.get(cacheTag, new Integer(42), "some_string", new Float(1.1));
        Object otherCreatedObjectRef2 = cache.get(cacheTag, new Integer(13), "another_string", new Float(9.0));
        
        // make sure we get the same object both times
        assertEquals(createdObject, createdObjectRef2);
        assertTrue(createdObject == createdObjectRef2);
        assertEquals(otherCreatedObject, otherCreatedObjectRef2);
        assertTrue(otherCreatedObject == otherCreatedObjectRef2);
        
        // make sure we get a different object ref using the other tag
        Object otherTagCreatedObject = cache.get(otherTag, new Integer(42), "some_string", new Float(1.1));
        assertEquals(createdObject, otherTagCreatedObject);
        assertTrue(createdObject != otherTagCreatedObject);
    }

    public void testFlushByKey() throws Exception {
        InterMineCache cache = new InterMineCache();

        String otherTag = "another_tag";
        
        cache.register(cacheTag, objectCreator3);
        cache.register(otherTag, objectCreator3);
        
        Object createdObject1Tag1 = cache.get(cacheTag, new Integer(11), "some_string_11", new Float(1.1));
        Object createdObject2Tag1 = cache.get(cacheTag, new Integer(21), "some_string_21", new Float(1.1));
        Object createdObject1Tag2 = cache.get(otherTag, new Integer(12), "some_string_12", new Float(1.1));
        Object createdObject2Tag2 = cache.get(otherTag, new Integer(22), "some_string_22", new Float(1.1));
        Object createdObject2Tag3 = cache.get(otherTag, new Integer(32), "some_string_22", new Float(1.1));
        
        assertEquals(2, cache.getCachedObjectCount(cacheTag));
        assertEquals(3, cache.getCachedObjectCount(otherTag));
        
        cache.flushByKey(otherTag, new Object[] {null, "some_string_22", new Float(1.1)});
        
        assertEquals(2, cache.getCachedObjectCount(cacheTag));
        assertEquals(1, cache.getCachedObjectCount(otherTag));
        
        // make sure a new object is created
        assertTrue(createdObject2Tag3 != cache.get(otherTag, new Integer(32),
                                                             "some_string_22", new Float(1.1)));

        assertEquals(2, cache.getCachedObjectCount(otherTag));
        
        cache.flushByKey(otherTag, new Object[] { null, null, null });
        
        assertEquals(0, cache.getCachedObjectCount(otherTag));
    }

    public void testFlush() throws Exception {
        InterMineCache cache = new InterMineCache();

        String otherTag = "another_tag";
        
        cache.register(cacheTag, objectCreator2);
        cache.register(otherTag, objectCreator2);
        
        Object createdObject1Tag1 = cache.get(cacheTag, new Integer(11), "some_string_11");
        Object createdObject2Tag1 = cache.get(cacheTag, new Integer(21), "some_string_21");
        Object createdObject1Tag2 = cache.get(otherTag, new Integer(12), "some_string_12");
        Object createdObject2Tag2 = cache.get(otherTag, new Integer(22), "some_string_22");

        assertEquals(2, cache.getCachedObjectCount(cacheTag));
        assertEquals(2, cache.getCachedObjectCount(otherTag));
        
        try {
            cache.flush("unknown_tag");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        cache.flush(cacheTag);
        
        assertEquals(0, cache.getCachedObjectCount(cacheTag));
        assertEquals(2, cache.getCachedObjectCount(otherTag));
        
        // make sure a new object is created
        assertTrue(createdObject1Tag1 != cache.get(cacheTag, new Integer(11), "some_string_11"));

        assertEquals(1, cache.getCachedObjectCount(cacheTag));
        
        cache.flushAll();
        
        assertEquals(0, cache.getCachedObjectCount(cacheTag));
        assertEquals(0, cache.getCachedObjectCount(otherTag));
    }
}
