package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.model.testmodel.Broke;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employable;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.HasAddress;
import org.intermine.model.testmodel.HasSecretarys;
import org.intermine.model.testmodel.ImportantPerson;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.RandomInterface;
import org.intermine.model.testmodel.Thing;

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

    public void testCreateObjectClassAndRedundantInterfaces() {
       Set intSet = new HashSet();
        intSet.add(Employee.class);
        intSet.add(Employable.class);
        Object obj = DynamicUtil.createObject(intSet);
        assertEquals(Employee.class, obj.getClass());
        assertTrue(obj instanceof Employee);
        assertTrue(obj instanceof Employable);
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

    public void testConstructors() throws Exception {
        Class c = DynamicUtil.createObject(Collections.singleton(Company.class)).getClass();
        Company obj = (Company) c.newInstance();
        ((net.sf.cglib.proxy.Factory) obj).setCallback(0, new DynamicBean());
        obj.setName("Fred");
        assertEquals("Fred", obj.getName());
    }

    public void testInstantiateObjectNullClassName() throws Exception {
        Object obj = DynamicUtil.instantiateObject(null, "org.intermine.model.testmodel.Broke");
        assertTrue(obj instanceof Broke);
    }

    public void testInstantiateObjectEmptyClassName() throws Exception {
        Object obj = DynamicUtil.instantiateObject("", "org.intermine.model.testmodel.Broke");
        assertTrue(obj instanceof Broke);
    }

    public void testInstantiateObjectNullImplementations() throws Exception {
        Object obj = DynamicUtil.instantiateObject("org.intermine.model.testmodel.Manager", null);
        assertTrue(obj instanceof Manager);
    }

    public void testInstantiateObjectEmptyImplementations() throws Exception {
        Object obj = DynamicUtil.instantiateObject("org.intermine.model.testmodel.Manager", "");
        assertTrue(obj instanceof Manager);
    }

    public void testInstantiateObject() throws Exception {
        Object obj = DynamicUtil.instantiateObject("org.intermine.model.testmodel.Manager", "org.intermine.model.testmodel.Broke");
        assertTrue(obj instanceof Manager);
        assertTrue(obj instanceof Broke);
    }

/*    public void testComposedClass() throws Exception {
        StringBuffer b = new StringBuffer();
        Class c = DynamicUtil.composeClass(Collections.singleton(Company.class));
        Set alreadySeen = new HashSet();
        while (c != null) {
            Method methods[] = c.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                if (! Modifier.isPrivate(methods[i].getModifiers())) {
                    MethodDescriptor md = new MethodDescriptor(m.getName(), m.getParameterTypes());
                    if (! alreadySeen.contains(md)) {
                        b.append(m.toString()).append("\n");
                        alreadySeen.add(md);
                    }
                }
            }
            c = c.getSuperclass();
        }
        throw new Exception(b.toString());
    }

    private static class MethodDescriptor
    {
        public String name;
        public Class[] parameters;

        public MethodDescriptor(String name, Class parameters[]) {
            this.name = name;
            this.parameters = parameters;
        }

        public boolean equals(Object o) {
            if (o instanceof MethodDescriptor) {
                if (name.equals(((MethodDescriptor) o).name)) {
                    Class oparameters[] = ((MethodDescriptor) o).parameters;
                    if (parameters.length == oparameters.length) {
                        for (int i = 0; i < parameters.length; i++) {
                            if (parameters[i] != oparameters[i]) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        public int hashCode() {
            return name.hashCode();
        }
    }*/

    public void testClassHeirarchyBrokeCEOIsBroke() throws Exception {
        Class brokeCEO = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {Broke.class, CEO.class})));
        assertTrue(Broke.class.isAssignableFrom(brokeCEO));
    }

    public void testClassHeirarchyBrokeCEOIsCEO() throws Exception {
        Class brokeCEO = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {Broke.class, CEO.class})));
        assertTrue(CEO.class.isAssignableFrom(brokeCEO));
    }

    /* See ticket #469
    public void testClassHeirarchyBrokeCEOIsBrokeManager() throws Exception {
        Class brokeCEO = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {Broke.class, CEO.class})));
        Class brokeManager = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {Broke.class, Manager.class})));
        assertTrue(brokeManager.isAssignableFrom(brokeCEO));
    }

    public void testClassHeirarchyBrokeCEOIsBrokeDynamic() throws Exception {
        Class brokeCEO = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {Broke.class, CEO.class})));
        Class broke = DynamicUtil.composeClass(Collections.singleton(Broke.class));
        assertTrue(broke.isAssignableFrom(brokeCEO));
    }

    public void testClassHeirarchyBrokeCEOIsBrokeHasSecretarysManager() throws Exception {
        Class brokeCEO = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {Broke.class, CEO.class})));
        Class brokeHasSecretarysManager = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {Broke.class, HasSecretarys.class, Manager.class})));
        assertTrue(brokeHasSecretarysManager.isAssignableFrom(brokeCEO));
    }

    public void testClassHeirarchyBrokeCEOIsImportantPersonEmployable() throws Exception {
        Class brokeCEO = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {Broke.class, CEO.class})));
        Class importantPersonEmployable = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {ImportantPerson.class, Employable.class})));
        assertTrue(importantPersonEmployable.isAssignableFrom(brokeCEO));
    }

    public void testClassHeirarchyContractorIsImportantPersonEmployable() throws Exception {
        Class importantPersonEmployable = DynamicUtil.composeClass(new HashSet(Arrays.asList(new Class[] {ImportantPerson.class, Employable.class})));
        assertTrue(importantPersonEmployable.isAssignableFrom(Contractor.class));
    }*/
}
