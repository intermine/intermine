package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.flymine.metadata.Model;
import org.flymine.model.testmodel.*;

public class DynamicUtilTest extends TestCase
{
    public DynamicUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
    }

    // NEED MORE TESTS FOR MULTIPLE INHERITED INTERFACES WHEN AVAILABLE
    // eg.    - B
    //       /
    //      A     - D
    //       \   /   \
    //        - C     - F
    //           \   /
    //            - E

    public void testCreateObjectOneInterface() throws Exception {
        Object obj = DynamicUtil.createObject(Collections.singleton(Company.class));
        assertTrue(obj instanceof Company);
        Company c = (Company) obj;
        c.setName("Flibble");
        assertEquals("Flibble", c.getName());
    }

    public void testCreateObjectOneInterfaceWithParents() throws Exception {
        Object obj = DynamicUtil.createObject(Collections.singleton(Employable.class));
        assertTrue(obj instanceof Employable);
        assertTrue(obj instanceof Thing);
    }

    public void testCreateObjectNoClassTwoInterfaces() throws Exception {
        Set intSet = new HashSet();
        intSet.add(Company.class);
        intSet.add(Broke.class);
        Object obj = DynamicUtil.createObject(intSet);
        assertTrue(obj instanceof Company);
        assertTrue(obj instanceof RandomInterface);
        assertTrue(obj instanceof Broke);
        assertTrue(obj instanceof HasSecretarys);
        assertTrue(obj instanceof HasAddress);

        ((Company) obj).setName("Wotsit");
        ((Broke) obj).setDebt(40);

        assertEquals("Wotsit", ((Company) obj).getName());
        assertEquals(40, ((Broke) obj).getDebt());
    }

    public void testCreateObjectClassOnly() throws Exception {
        Object obj = DynamicUtil.createObject(Collections.singleton(Employee.class));
        assertEquals(Employee.class, obj.getClass());
    }

    public void testCreateObjectClassInterfaces() throws Exception {
        Set intSet = new HashSet();
        intSet.add(Manager.class);
        intSet.add(Broke.class);
        Object obj = DynamicUtil.createObject(intSet);
        assertTrue(obj instanceof Manager);
        assertTrue(obj instanceof Employee);
        assertTrue(obj instanceof ImportantPerson);
        assertTrue(obj instanceof Employable);
        assertTrue(obj instanceof HasAddress);
        assertTrue(obj instanceof Broke);
        
        Manager m = (Manager) obj;
        m.setName("Frank");
        m.setTitle("Mr.");
        ((Broke) m).setDebt(30);
        assertEquals("Frank", m.getName());
        assertEquals("Mr.", m.getTitle());
        assertEquals(30, ((Broke) m).getDebt());
    }

    public void testCreateObjectTwoClasses() throws Exception {
        Set intSet = new HashSet();
        intSet.add(Manager.class);
        intSet.add(Department.class);
        try {
            Object obj = DynamicUtil.createObject(intSet);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testCreateObjectNothing() throws Exception {
        try {
            Object obj = DynamicUtil.createObject(Collections.EMPTY_SET);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
