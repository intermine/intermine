package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FlyMineBusinessObject;
import org.intermine.model.datatracking.Source;
import org.intermine.model.testmodel.*;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryTestCase;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SubqueryConstraint;
import org.intermine.testing.OneTimeTestCase;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IntToIntMap;

import junit.framework.Test;

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

    public void testGetPrimaryKeysCld() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Map expected = new HashMap();
        expected.put("key1", new PrimaryKey("name, address"));
        expected.put("key2", new PrimaryKey("vatNumber"));        
        assertEquals(expected, DataLoaderHelper.getPrimaryKeys(cld));
    }

    public void testGetPrimaryKeysCldInherited() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Manager");
        Map expected = new HashMap();
        assertEquals(expected, DataLoaderHelper.getPrimaryKeys(cld));
    }

    public void testGetPrimaryKeysCldSource() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Source source = new Source();
        source.setName("testsource");
        assertEquals(Collections.singleton(new PrimaryKey("name, address")), DataLoaderHelper.getPrimaryKeys(cld, source));
    }

    public void testCreateQuery1() throws Exception {
        Source source = new Source();
        source.setName("testsource");
        Query q = new Query();
        QueryClass qc = new QueryClass(Employable.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("jkhsdfg")));
        q.setConstraint(cs);
        q.setDistinct(false);

        Employable e = (Employable) DynamicUtil.createObject(Collections.singleton(Employable.class));
        e.setName("jkhsdfg");

        assertEquals(q, DataLoaderHelper.createPKQuery(model, e, source, new IntToIntMap()));
    }

    public void testCreateQuery2() throws Exception {
        Source source = new Source();
        source.setName("testsource");
        Query q = new Query();
        QueryClass qc = new QueryClass(Employable.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.IS_NULL));
        q.setConstraint(cs);
        q.setDistinct(false);

        Employable e = (Employable) DynamicUtil.createObject(Collections.singleton(Employable.class));
        e.setName(null);

        assertEquals(q, DataLoaderHelper.createPKQuery(model, e, source, new IntToIntMap()));
    }
        
    public void testCreateQuery3() throws Exception {
        Source source = new Source();
        source.setName("testsource");
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("jkhsdfg")));
        QueryClass address = new QueryClass(Address.class);
        q.addFrom(address);
        Query subQ = new Query();
        QueryClass subQc = new QueryClass(Address.class);
        subQ.addFrom(subQc);
        subQ.addToSelect(subQc);
        ConstraintSet subCs = new ConstraintSet(ConstraintOp.AND);
        subCs.addConstraint(new SimpleConstraint(new QueryField(subQc, "address"), ConstraintOp.EQUALS, new QueryValue("10 Downing Street")));
        subQ.setConstraint(subCs);
        subQ.setDistinct(false);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc, "address"), ConstraintOp.CONTAINS, address));
        cs.addConstraint(new SubqueryConstraint(address, ConstraintOp.IN, subQ));
        q.setConstraint(cs);
        q.setDistinct(false);

        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setName("jkhsdfg");
        Address a = new Address();
        a.setAddress("10 Downing Street");
        c.setAddress(a);

        assertEquals(q, DataLoaderHelper.createPKQuery(model, c, source, new IntToIntMap()));
    }

    public void testCreateQuery4() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("jkhsdfg")));
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc, "address"), ConstraintOp.IS_NULL));
        q.setConstraint(cs);
        q.setDistinct(false);

        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setName("jkhsdfg");
        c.setAddress(null);

        assertEquals(q, DataLoaderHelper.createPKQuery(model, c, source, new IntToIntMap()));
    }

    public void testGetDescriptors() throws Exception {
        Map expected = new HashMap();
        expected.put("Department", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Company", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Address", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Employable", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("ImportantPerson", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Secretary", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Bank", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Types", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Employee.age", Arrays.asList(new Object[] {"storedata", "testsource2", "testsource"}));
        expected.put("Employee", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("Broke", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
        expected.put("HasAddress", Arrays.asList(new Object[] {"testsource2", "testsource", "storedata"}));
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
    }
}
