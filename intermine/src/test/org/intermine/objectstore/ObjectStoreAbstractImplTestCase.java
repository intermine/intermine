package org.flymine.objectstore;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.objectstore.proxy.LazyCollection;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.objectstore.proxy.LazyInitializer;

import org.flymine.model.testmodel.*;

public class ObjectStoreAbstractImplTestCase extends ObjectStoreTestCase
{
    protected static ObjectStoreAbstractImpl osai;
    
    public ObjectStoreAbstractImplTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        ObjectStoreTestCase.oneTimeSetUp();
        if (os instanceof ObjectStoreAbstractImpl) {
            osai = (ObjectStoreAbstractImpl) os;
        }
    }

    public void testCheckStartLimit() throws Exception {
        int oldOffset = osai.maxOffset;
        osai.maxOffset = 10;
        try {
            osai.checkStartLimit(11,0);
            fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        osai.maxOffset = oldOffset;

        int oldLimit = osai.maxLimit;
        osai.maxLimit = 10;
        try {
            osai.checkStartLimit(0,11);
            fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        osai.maxLimit = oldLimit;
    }

    public void testLimitTooHigh() throws Exception {
        // try to run query with limit higher than imposed maximum
        int before = osai.maxLimit;
        osai.maxLimit = 99;
        try{
            osai.execute((Query) queries.get("SelectSimpleObject"), 10, 100, true);
            fail("Expected: ObjectStoreException");
        } catch (IndexOutOfBoundsException e) {
        } finally {
            osai.maxLimit = before;
        }
    }

    public void testOffsetTooHigh() throws Exception {
        // try to run query with offset higher than imposed maximum
        int before = osai.maxOffset;
        osai.maxOffset = 99;
        try {
            osai.execute((Query) queries.get("SelectSimpleObject"), 100, 50, true);
            fail("Expected: ObjectStoreException");
        } catch (IndexOutOfBoundsException e) {
        } finally {
            osai.maxOffset = before;
        }
    }

    public void testTooMuchTime()  throws Exception {
        // try to run a query that takes longer than max amount of time
        long before = osai.maxTime;
        osai.maxTime = -1;
        try {
            osai.execute((Query) queries.get("SelectSimpleObject"), 0, 1, true);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        } finally {
            osai.maxTime = before;
        }
    }

    public void testPromoteLazyCollection() throws Exception {
        // Create a Department object with a LazyCollection
        Department dept = getDeptExampleObject();
        assertTrue(dept.getEmployees() instanceof LazyCollection);

        osai.promoteProxies(dept);

        // Employees should now have become a Results object
        Collection col = dept.getEmployees();
        if (!(col instanceof SingletonResults)) {
            fail("LazyCollection was not converted to a Results object");
        }
    }

    public void testPromoteLazyCollectionSet() throws Exception {
        // Create a Department object with a LazyCollection
        Example ex = getExampleObjectWithSet();
        assertTrue(ex.getSet() instanceof LazyCollection);

        osai.promoteProxies(ex);

        // Employees should now have become a Results object
        Collection col = ex.getSet();
        if (!(col instanceof SingletonResults)) {
            fail("LazyCollection was not converted to a Results object");
        }
    }

    public void testPromoteLazyReference() throws Exception {
        // Create a Department object with a LazyCollection
        Department dept = getDeptExampleObject();
        assertTrue(dept.getCompany() instanceof LazyReference);

        osai.promoteProxies(dept);

        // Company should now be materialized
        Object obj = dept.getCompany();
        if (!(obj instanceof Company)) {
            fail("LazyCollection was not converted to a Results object");
        }
    }

    // set up a Department object with an id and Employees as a LazyCollection
    // and a LazyReference
    private Department getDeptExampleObject() throws Exception {
        Department dept = new Department();
        Class deptClass = dept.getClass();
//         Field f = deptClass.getDeclaredField("id");
//         f.setAccessible(true);
//         f.set(dept, new Integer(1234));

        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        LazyCollection lazyCol = new LazyCollection(q1);
        dept.setEmployees((List)lazyCol);

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Company.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        LazyReference lazyRef = (LazyReference) LazyInitializer.getDynamicProxy(Company.class, q2, new Integer(0));
        dept.setCompany((Company)lazyRef);

        return dept;
    }

    // set up an Example object with field of type Set that is actually a LazyCollection
    private Example getExampleObjectWithSet() throws Exception {
        Example ex = new Example();
        Class exClass = ex.getClass();
        //Field f = deptClass.getDeclaredField("id");
        //f.setAccessible(true);
        //f.set(dept, new Integer(1234));

        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Example.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        LazyCollection lazyCol = new LazyCollection(q1);
        ex.setSet((Set)lazyCol);

        return ex;
    }

    // example class with a set, for testing promoteProxies()
    public static class Example {
        Set set = new HashSet();
        public void setSet(Set set) {
            this.set = set;
        }
        public Set getSet() {
            return this.set;
        }
    }
}
