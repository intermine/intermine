package org.intermine.util;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;
import java.util.*;

import junit.framework.TestCase;

/**
 * Tests for the CollectionUtil class.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */

public class CollectionUtilTest extends TestCase
{
    public CollectionUtilTest(String arg) {
        super(arg);
    }

    public void testLinkedHashMapAddEmpty1() throws Exception {
        LinkedHashMap map = new LinkedHashMap();

        Map newMap = CollectionUtil.linkedHashMapAdd(map, null, "newKey", "newValue");

        Set expectedKeys = new HashSet(Arrays.asList(new Object[] {"newKey"}));

        assertEquals(expectedKeys, newMap.keySet());

        Set expectedValues = new HashSet(Arrays.asList(new Object[] {"newValue"}));

        assertEquals(expectedValues, new HashSet(newMap.values()));
    }

    public void testLinkedHashMapAddEmpty2() throws Exception {
        LinkedHashMap map = new LinkedHashMap();

        try {
            Map newMap = CollectionUtil.linkedHashMapAdd(map, "nonexistentKey", "newKey", "newValue");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

    }

    public void testLinkedHashMapAddAfterNull() throws Exception {
        LinkedHashMap map = new LinkedHashMap();

        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        map.put("key4", "value4");

        Map newMap = CollectionUtil.linkedHashMapAdd(map, null, "newKey", "newValue");

        Set expectedKeys = new HashSet(Arrays.asList(new Object[] {
                                                         "newKey",
                                                         "key1", "key2", "key3", "key4"}));
                                                     
        assertEquals(expectedKeys, newMap.keySet());

        Set expectedValues = new HashSet(Arrays.asList(new Object[] {
                                                           "newValue",
                                                           "value1", "value2",
                                                           "value3", "value4"}));
                                                       
        assertEquals(expectedValues, new HashSet(newMap.values()));
    }

    public void testLinkedHashMapAddValue1() throws Exception {
        LinkedHashMap map = new LinkedHashMap();

        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        map.put("key4", "value4");

        Map newMap = CollectionUtil.linkedHashMapAdd(map, "key1", "newKey", "newValue");

        Set expectedKeys = new HashSet(Arrays.asList(new Object[] {
                                                         "key1", "newKey", "key2", "key3", "key4"
                                                         }));
                                                     
        assertEquals(expectedKeys, newMap.keySet());

        Set expectedValues = new HashSet(Arrays.asList(new Object[] {
                                                           "value1", "newValue", "value2", "value3",
                                                           "value4"}));
                                                       
        assertEquals(expectedValues, new HashSet(newMap.values()));
    }

    public void testLinkedHashMapAddValue2() throws Exception {
        LinkedHashMap map = new LinkedHashMap();

        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        map.put("key4", "value4");

        Map newMap = CollectionUtil.linkedHashMapAdd(map, "key4", "newKey", "newValue");

        Set expectedKeys = new HashSet(Arrays.asList(new Object[] {
                                                         "key1", "key2", "key3", "key4",
                                                         "newKey"}));
                                                     
        assertEquals(expectedKeys, newMap.keySet());

        Set expectedValues = new HashSet(Arrays.asList(new Object[] {
                                                           "value1", "value2", "value3",
                                                           "value4", "newValue"}));
                                                       
        assertEquals(expectedValues, new HashSet(newMap.values()));
    }
    
    public void testLinkedHashMapSameKey() throws Exception {
        LinkedHashMap map = new LinkedHashMap();

        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        map.put("key4", "value4");

        Map newMap = CollectionUtil.linkedHashMapAdd(map, "key2", "key2", "newValue");

        Set expectedKeys = new HashSet(Arrays.asList(new Object[] {
                                                         "key1", "key2", "key3", "key4"}));
                                                     
        assertEquals(expectedKeys, newMap.keySet());

        Set expectedValues = new HashSet(Arrays.asList(new Object[] {
                                                           "value1", "newValue", "value3",
                                                           "value4"}));
                                                       
        assertEquals(expectedValues, new HashSet(newMap.values()));
    }

    public void testGroupByClass() throws Exception {
        Collection c = new ArrayList();
        c.add(new Integer(5));
        c.add(new Integer(6));
        c.add(new Integer(7));
        c.add(new Float(3.5F));
        c.add("hello");

        Map expected = new HashMap();
        List lInts = new ArrayList();
        lInts.add(new Integer(5));
        lInts.add(new Integer(6));
        lInts.add(new Integer(7));
        List lFloat = new ArrayList();
        lFloat.add(new Float(3.5F));
        List lString = new ArrayList();
        lString.add("hello");
        List lNumbers = new ArrayList();
        lNumbers.addAll(lInts);
        lNumbers.addAll(lFloat);
        List lSerialisables = new ArrayList();
        lSerialisables.addAll(lNumbers);
        lSerialisables.addAll(lString);

        expected.put(Integer.class, lInts);
        expected.put(Float.class, lFloat);
        expected.put(String.class, lString);

        assertEquals(expected, CollectionUtil.groupByClass(c, false));

        expected.put(CharSequence.class, lString);
        expected.put(Number.class, lNumbers);
        expected.put(Serializable.class, lSerialisables);
        expected.put(Comparable.class, lSerialisables);

        assertEquals(expected, CollectionUtil.groupByClass(c, true));
    }
}
