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

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.model.testmodel.*;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryObjectReference;
import org.flymine.objectstore.query.QueryTestCase;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.testing.OneTimeTestCase;
import org.flymine.util.DynamicUtil;

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
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
        Map expected = new HashMap();
        expected.put("key1", new PrimaryKey("name, address"));
        expected.put("key2", new PrimaryKey("vatNumber"));        
        assertEquals(expected, DataLoaderHelper.getPrimaryKeys(cld));
    }

    public void testGetPrimaryKeysCldInherited() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Manager");
        Map expected = new HashMap();
        assertEquals(expected, DataLoaderHelper.getPrimaryKeys(cld));
    }

    public void testGetPrimaryKeysCldSource() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
        Source source = new Source();
        source.setName("testsource");
        assertEquals(Collections.singleton(new PrimaryKey("name, address")), DataLoaderHelper.getPrimaryKeys(cld, source));
    }

    public void testCreateQuery1() throws Exception {
        Source source = new Source();
        source.setName("testsource");
        Query q = new Query();
        QueryClass qcFMBO = new QueryClass(FlyMineBusinessObject.class);
        q.addFrom(qcFMBO);
        q.addToSelect(qcFMBO);
        ConstraintSet where = new ConstraintSet(ConstraintOp.OR);
        Query subQ = new Query();
        QueryClass qc = new QueryClass(Employable.class);
        subQ.addFrom(qc);
        subQ.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("jkhsdfg")));
        subQ.setConstraint(cs);
        where.addConstraint(new SubqueryConstraint(qcFMBO, ConstraintOp.IN, subQ));
        q.setConstraint(where);

        Employable e = (Employable) DynamicUtil.createObject(Collections.singleton(Employable.class));
        e.setName("jkhsdfg");

        assertEquals(q, DataLoaderHelper.createPKQuery(model, e, source));
    }

    public void testCreateQuery2() throws Exception {
        Source source = new Source();
        source.setName("testsource");
        Query q = new Query();
        QueryClass qcFMBO = new QueryClass(FlyMineBusinessObject.class);
        q.addFrom(qcFMBO);
        q.addToSelect(qcFMBO);
        ConstraintSet where = new ConstraintSet(ConstraintOp.OR);
        Query subQ = new Query();
        QueryClass qc = new QueryClass(Employable.class);
        subQ.addFrom(qc);
        subQ.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.IS_NULL));
        subQ.setConstraint(cs);
        where.addConstraint(new SubqueryConstraint(qcFMBO, ConstraintOp.IN, subQ));
        q.setConstraint(where);

        Employable e = (Employable) DynamicUtil.createObject(Collections.singleton(Employable.class));
        e.setName(null);

        assertEquals(q, DataLoaderHelper.createPKQuery(model, e, source));
    }
        
    public void testCreateQuery3() throws Exception {
        Source source = new Source();
        source.setName("testsource");
        Query q = new Query();
        QueryClass qcFMBO = new QueryClass(FlyMineBusinessObject.class);
        q.addFrom(qcFMBO);
        q.addToSelect(qcFMBO);
        ConstraintSet where = new ConstraintSet(ConstraintOp.OR);
        Query subQ = new Query();
        QueryClass qc = new QueryClass(Company.class);
        subQ.addFrom(qc);
        subQ.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("jkhsdfg")));
        QueryClass address = new QueryClass(Address.class);
        subQ.addFrom(address);
        Query subSubQ = new Query();
        QueryClass subQcFMBO = new QueryClass(FlyMineBusinessObject.class);
        subSubQ.addFrom(subQcFMBO);
        subSubQ.addToSelect(subQcFMBO);
        ConstraintSet subWhere = new ConstraintSet(ConstraintOp.OR);
        Query subSubSubQ = new Query();
        QueryClass subQc = new QueryClass(Address.class);
        subSubSubQ.addFrom(subQc);
        subSubSubQ.addToSelect(subQc);
        ConstraintSet subCs = new ConstraintSet(ConstraintOp.AND);
        subCs.addConstraint(new SimpleConstraint(new QueryField(subQc, "address"), ConstraintOp.EQUALS, new QueryValue("10 Downing Street")));
        subSubSubQ.setConstraint(subCs);
        subWhere.addConstraint(new SubqueryConstraint(subQcFMBO, ConstraintOp.IN, subSubSubQ));
        subSubQ.setConstraint(subWhere);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc, "address"), ConstraintOp.CONTAINS, address));
        cs.addConstraint(new SubqueryConstraint(address, ConstraintOp.IN, subSubQ));
        subQ.setConstraint(cs);
        where.addConstraint(new SubqueryConstraint(qcFMBO, ConstraintOp.IN, subQ));
        q.setConstraint(where);

        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setName("jkhsdfg");
        Address a = new Address();
        a.setAddress("10 Downing Street");
        c.setAddress(a);

        assertEquals(q, DataLoaderHelper.createPKQuery(model, c, source));
    }

    public void testCreateQuery4() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Query q = new Query();
        QueryClass qcFMBO = new QueryClass(FlyMineBusinessObject.class);
        q.addFrom(qcFMBO);
        q.addToSelect(qcFMBO);
        ConstraintSet where = new ConstraintSet(ConstraintOp.OR);
        Query subQ = new Query();
        QueryClass qc = new QueryClass(Company.class);
        subQ.addFrom(qc);
        subQ.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("jkhsdfg")));
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc, "address"), ConstraintOp.IS_NULL));
        subQ.setConstraint(cs);
        where.addConstraint(new SubqueryConstraint(qcFMBO, ConstraintOp.IN, subQ));
        q.setConstraint(where);

        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setName("jkhsdfg");
        c.setAddress(null);

        assertEquals(q, DataLoaderHelper.createPKQuery(model, c, source));
    }

    public void testGetDescriptors() throws Exception {
        Map expected = new HashMap();
        expected.put("Employee.age", Arrays.asList(new Object[] {"SourceA", "SourceB"}));
        expected.put("Employee", Arrays.asList(new Object[] {"SourceB", "SourceA"}));
        expected.put("Address", Arrays.asList(new Object[] {"SourceB", "SourceA"}));
        assertEquals(expected, DataLoaderHelper.getDescriptors(model));
    }

    public void testComparePriorityField() throws Exception {
        Source sourceA = new Source();
        sourceA.setName("SourceA");
        Source sourceB = new Source();
        sourceB.setName("SourceB");
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Employee");
        FieldDescriptor fd = cld.getFieldDescriptorByName("age");
        assertEquals(new Boolean(true), DataLoaderHelper.comparePriority(fd, sourceA, sourceB));
        assertEquals(new Boolean(false), DataLoaderHelper.comparePriority(fd, sourceB, sourceA));
    }

    public void testComparePriorityClass() throws Exception {
        Source sourceA = new Source();
        sourceA.setName("SourceA");
        Source sourceB = new Source();
        sourceB.setName("SourceB");
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Employee");
        FieldDescriptor fd = cld.getFieldDescriptorByName("fullTime");
        assertEquals(new Boolean(true), DataLoaderHelper.comparePriority(fd, sourceB, sourceA));
        assertEquals(new Boolean(false), DataLoaderHelper.comparePriority(fd, sourceA, sourceB));
    }

    public void testComparePriorityMissing() throws Exception {
        Source sourceA = new Source();
        sourceA.setName("SourceA");
        Source sourceB = new Source();
        sourceB.setName("SourceB");
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");
        FieldDescriptor fd = cld.getFieldDescriptorByName("name");
        assertNull(DataLoaderHelper.comparePriority(fd, sourceA, sourceB));
    }
}
