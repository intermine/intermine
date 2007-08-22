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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employable;
import org.intermine.objectstore.query.QueryTestCase;
import org.intermine.testing.OneTimeTestCase;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IntToIntMap;

public class DataLoaderHelperTest extends QueryTestCase
{
    Model model;

    public DataLoaderHelperTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(DataLoaderHelperTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }

    public void testGetPrimaryKeysCldSource() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Source source = new Source();
        source.setName("testsource");
        assertEquals(Collections.singleton(new PrimaryKey("key1", "name, address", cld)), DataLoaderHelper.getPrimaryKeys(cld, source));

        source = new Source();
        source.setName("testsource5");
        try {
            DataLoaderHelper.getPrimaryKeys(cld, source);
            fail("Was expecting an exception");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetPrimaryKeysCldSource2() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Source source = new Source();
        source.setName("testsource4");
        Set expected = new HashSet();
        expected.add(new PrimaryKey("key1", "name,address", cld));
        expected.add(new PrimaryKey("key2", "vatNumber", cld));
        assertEquals(expected, DataLoaderHelper.getPrimaryKeys(cld, source));
    }

    public void testObjectPrimaryKeyIsNull1() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Employable e =
            (Employable) DynamicUtil.createObject(Collections.singleton(Employable.class));
        e.setName("jkhsdfg");
        ClassDescriptor cld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Employable");
        Set primaryKeys = new HashSet(PrimaryKeyUtil.getPrimaryKeys(cld).values());
        PrimaryKey pk = (PrimaryKey) primaryKeys.iterator().next();

        assertTrue(DataLoaderHelper.objectPrimaryKeyNotNull(model, e, cld, pk, source, new IntToIntMap()));
    }

    public void testObjectPrimaryKeyIsNullNullField() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Employable e =
            (Employable) DynamicUtil.createObject(Collections.singleton(Employable.class));
        e.setName(null);
        ClassDescriptor cld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Employable");
        Set primaryKeys = new HashSet(PrimaryKeyUtil.getPrimaryKeys(cld).values());
        PrimaryKey pk = (PrimaryKey) primaryKeys.iterator().next();

        assertFalse(DataLoaderHelper.objectPrimaryKeyNotNull(model, e, cld, pk, source, new IntToIntMap()));
    }

    public void testObjectPrimaryKeyIsNull2() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setName("jkhsdfg");
        Address a = new Address();
        a.setAddress("10 Downing Street");
        c.setAddress(a);
        c.setVatNumber(765213);

        ClassDescriptor cld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Set primaryKeys = new HashSet(PrimaryKeyUtil.getPrimaryKeys(cld).values());
        PrimaryKey pk = (PrimaryKey) primaryKeys.iterator().next();

        assertTrue(DataLoaderHelper.objectPrimaryKeyNotNull(model, c, cld, pk, source, new IntToIntMap()));
    }

    public void testObjectPrimaryKeyIsNullNullField2() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setName("jkhsdfg");
        Address a = new Address();
        a.setAddress(null);
        c.setAddress(a);
        c.setVatNumber(765213);

        ClassDescriptor cld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Set primaryKeys = new HashSet(PrimaryKeyUtil.getPrimaryKeys(cld).values());
        Iterator pkIter = primaryKeys.iterator();
        PrimaryKey pk1 = (PrimaryKey) pkIter.next();

        // Company.key2=vatNumber
        assertTrue(DataLoaderHelper.objectPrimaryKeyNotNull(model, c, cld, pk1, source, new IntToIntMap()));

        PrimaryKey pk2 = (PrimaryKey) pkIter.next();

        // Company.key1=name, address
        assertFalse(DataLoaderHelper.objectPrimaryKeyNotNull(model, c, cld, pk2, source, new IntToIntMap()));
    }

    public void testObjectPrimaryKeyIsNullNullField3() throws Exception {
        Department d = (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        d.setName("jkhsdfg");
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setAddress(null);
        c.setVatNumber(765213);
        d.setCompany(c);
        ClassDescriptor cld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Department");
        PrimaryKey pk = (PrimaryKey) PrimaryKeyUtil.getPrimaryKeys(cld).get("key1");

        assertTrue(DataLoaderHelper.objectPrimaryKeyNotNull(model, d, cld, pk, null, new IntToIntMap()));
    }

    public void testGetDescriptors() throws Exception {
        Map expected = new HashMap();
        expected.put("Department", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata", "testsource3"}));
        expected.put("Company", Arrays.asList(new Object[] {"testsource4", "testsource2", "testsource", "storedata", "testsource3"}));
        expected.put("Address", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata", "testsource4"}));
        expected.put("Employable", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("ImportantPerson", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Secretary", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Bank", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Types", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Employee.age", Arrays.asList(new Object[] {"testsource3", "storedata", "testsource2", "testsource"}));
        expected.put("Employee", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Broke", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("HasAddress", Arrays.asList(new Object[] {"testsource4", "testsource2", "testsource", "storedata", "testsource3"}));
        expected.put("Manager", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("CEO", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Contractor", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        assertEquals(expected, DataLoaderHelper.getDescriptors(model));
    }

    public void testComparePriorityField() throws Exception {
        Source sourceA = new Source();
        sourceA.setName("storedata");
        Source sourceB = new Source();
        sourceB.setName("testsource");
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        FieldDescriptor fd = cld.getFieldDescriptorByName("age");
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB) > 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceB, sourceA) < 0);
        assertEquals(0, DataLoaderHelper.comparePriority(fd, sourceA, sourceA));
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, Boolean.TRUE, null, null, null, null, false, false) > 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, null, Boolean.FALSE, null, null, null, false, false) > 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, null, null, null, null, null, false, false) > 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, Boolean.TRUE, Boolean.TRUE, null, null, null, false, false) > 0);
    }

    public void testComparePriorityClass() throws Exception {
        Source sourceA = new Source();
        sourceA.setName("storedata");
        Source sourceB = new Source();
        sourceB.setName("testsource");
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        FieldDescriptor fd = cld.getFieldDescriptorByName("fullTime");
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceB, sourceA) > 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB) < 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, Boolean.TRUE, null, null, null, null, false, false) < 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, null, Boolean.FALSE, null, null, null, false, false) < 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, null, null, null, null, null, false, false) < 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, Boolean.TRUE, Boolean.TRUE, null, null, null, false, false) < 0);
    }

    public void testComparePriorityMissing() throws Exception {
        Source sourceA = new Source();
        sourceA.setName("SourceA");
        Source sourceB = new Source();
        sourceB.setName("SourceB");
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Department");
        FieldDescriptor fd = cld.getFieldDescriptorByName("name");
        try {
            DataLoaderHelper.comparePriority(fd, sourceA, sourceB);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, Boolean.TRUE, null, null, null, null, false, false) > 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, null, Boolean.FALSE, null, null, null, false, false) < 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, null, null, null, null, null, false, false) < 0);
        assertTrue(DataLoaderHelper.comparePriority(fd, sourceA, sourceB, Boolean.TRUE, Boolean.TRUE, null, null, null, false, false) < 0);
    }
}
