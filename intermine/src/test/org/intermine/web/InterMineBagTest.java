package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

import junit.framework.TestCase;

import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;

/**
 * Tests for InterMineBag.
 * 
 * @author tom
 */
public class InterMineBagTest extends TestCase
{
    ObjectStore os = new ObjectStoreDummyImpl();
    Department d1 = new Department();
    Employee e2 = new Employee();
    
    protected void setUp() throws Exception {
        super.setUp();
        d1.setId(new Integer(1));
        e2.setId(new Integer(2));
        os.cacheObjectById(new Integer(1), d1);
        os.cacheObjectById(new Integer(2), e2);
    }
    
    public void testGetSize() {
        InterMineBag bag = new InterMineBag(os);
        bag.add("Hello");
        bag.addId(new Integer(1));
        assertEquals(2, bag.getSize());
        assertEquals(2, bag.size());
    }

    public void testAddId() {
        InterMineBag bag = new InterMineBag(os);
        bag.addId(new Integer(1));
        assertEquals(1, bag.getSize());
    }

    /*
     * Class under test for boolean add(Object)
     */
    public void testAddObject() {
        InterMineBag bag = new InterMineBag(os);
        bag.add(new Integer(1));
        bag.add("asdfasdf");
        assertEquals(2, bag.getSize());
    }

    /*
     * Class under test for Iterator iterator()
     */
    public void testIterator() {
        InterMineBag bag = new InterMineBag(os);
        bag.add(new Integer(1));
        bag.add("hello");
        
        Iterator iter = bag.iterator();
        assertTrue(iter.hasNext());
        Object o = iter.next();
        assertEquals(new Integer(1), o);
        assertTrue(iter.hasNext());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());
        
        iter = bag.iterator();
        iter.next();
        iter.remove();
        assertTrue(iter.hasNext());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());
        
        assertEquals(1, bag.size());
    }

    public void testLazyIterator() {
        InterMineBag bag = new InterMineBag(os);
        bag.add(new Integer(1));
        bag.addId(new Integer(1));
        bag.add("hello");
        
        Iterator iter = bag.lazyIterator();
        assertTrue(iter.hasNext());
        Object o = iter.next();
        assertEquals(new Integer(1), o);
        assertTrue(iter.hasNext());
        o = iter.next();
        assertTrue("object not instance of ID", o instanceof InterMineBag.ID);
        assertEquals(1, ((InterMineBag.ID) o).getId());
        iter.remove();
        assertTrue(iter.hasNext());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());
    }

    /*
     * Class under test for Object get(int)
     */
    public void testGetint() {
        InterMineBag bag = new InterMineBag(os);
        bag.add(new Integer(1));
        bag.addId(new Integer(1));
        bag.add("hello");
        
        assertEquals(new Integer(1), bag.get(0));
        assertEquals(d1, bag.get(1));
        assertEquals("hello", bag.get(2));
    }

    public void testClear() {
        InterMineBag bag = new InterMineBag(os);
        bag.add(new Integer(1));
        bag.addId(new Integer(1));
        bag.add("hello");
        
        bag.clear();
        assertEquals(0, bag.size());
        Iterator iter = bag.iterator();
        assertFalse(iter.hasNext());
    }

    public void testAddAllCollection() {
        InterMineBag bag = new InterMineBag(os);
        bag.add(new Integer(1));
        bag.addId(new Integer(1));
        bag.add("hello");
        
        InterMineBag bag2 = new InterMineBag(os);
        bag2.add("world");
        bag2.addId(new Integer(2));
        
        assertEquals(3, bag.size());
        assertEquals(2, bag2.size());
        
        bag.addAll(bag2);
        System.out.println(bag.toString());
        assertEquals(5, bag.size());
        assertEquals(e2, bag.get(4));
        
    }

    /*
     * Class under test for boolean remove(java.lang.Object)
     */
    public void testRemoveObject() {
        InterMineBag bag = new InterMineBag(os);
        bag.add(new Integer(1));
        bag.addId(new Integer(1));
        bag.add("hello");
        
        assertEquals(3, bag.size());
        bag.remove(1);
        assertEquals(2, bag.size());
        
        assertEquals(new Integer(1), bag.get(0));
        assertEquals("hello", bag.get(1));
    }

    public void testRemoveAll() {
        InterMineBag bag = new InterMineBag(os);
        bag.add(new Integer(1));
        bag.addId(new Integer(1));
        bag.add("hello");
        
        InterMineBag bag2 = new InterMineBag(os);
        bag2.add("hello");
        bag2.add(d1);
        
        assertEquals(3, bag.size());
        assertEquals(2, bag2.size());
        
        bag.removeAll(bag2);
        
        assertEquals(1, bag.size());
        assertEquals(new Integer(1), bag.get(0));
    }
}
