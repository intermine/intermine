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

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
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

public class EquivalentObjectFetcherTest extends QueryTestCase
{
    Model model;
    EquivalentObjectFetcher eof;

    public EquivalentObjectFetcherTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(EquivalentObjectFetcherTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
        eof = new BaseEquivalentObjectFetcher(model, new IntToIntMap(), null);
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

        assertEquals(q, eof.createPKQuery(e, source, false));
    }

    public void testCreateQueryNullFields() throws Exception {
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

        Employable e =
            (Employable) DynamicUtil.createObject(Collections.singleton(Employable.class));
        e.setName(null);

        assertEquals(q, eof.createPKQuery(e, source, true));
    }

    // null key field in object (attribute)
    public void testCreateQueryDisableNullFields1() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Employable e =
            (Employable) DynamicUtil.createObject(Collections.singleton(Employable.class));
        e.setName(null);

        try {
            Query q = eof.createPKQuery(e, source, false);
            fail("" + q);
        } catch (IllegalArgumentException ex) {
        }
    }

    // null key field in object (reference)
    public void testCreateQueryDisableNullFields2() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Company c =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setName("company1");
        c.setAddress(null);

        try {
            Query q = eof.createPKQuery(c, source, false);
            fail("" + q);
        } catch (IllegalArgumentException ex) {
        }
    }

    // null key fields in referenced object
    public void testCreateQueryDisableNullFields3() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Company c =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setName("company1");
        Address a = (Address) DynamicUtil.createObject(Collections.singleton(Address.class));
        a.setAddress(null);
        c.setAddress(a);

        try {
            Query q = eof.createPKQuery(c, source, false);
            fail("" + q);
        } catch (IllegalArgumentException ex) {
        }
    }

    // one key has null values, expect just the other key
    public void testCreateQueryDisableNullFields4() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        Query q = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("dept1")));
        QueryClass qc1 = new QueryClass(Manager.class);
        q.addFrom(qc1);
        Query subQ = new Query();
        QueryClass subQc = new QueryClass(Employable.class);
        subQ.addFrom(subQc);
        subQ.addToSelect(subQc);
        ConstraintSet subCs = new ConstraintSet(ConstraintOp.AND);
        subCs.addConstraint(new SimpleConstraint(new QueryField(subQc, "name"), ConstraintOp.EQUALS, new QueryValue("manager1")));
        subQ.setConstraint(subCs);
        subQ.setDistinct(false);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc, "manager"), ConstraintOp.CONTAINS, qc1));
        cs.addConstraint(new SubqueryConstraint(qc1, ConstraintOp.IN, subQ));

        q.setConstraint(cs);
        q.setDistinct(false);

        Department d =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        d.setName("dept1");
        Manager m = (Manager) DynamicUtil.createObject(Collections.singleton(Manager.class));
        m.setName("manager1");
        d.setManager(m);

        assertEquals(q, eof.createPKQuery(d, source, false));
    }

    // one key has null values, expect just the other key
    public void testCreateQueryDisableNullFields5() throws Exception {
        Source source = new Source();
        source.setName("testsource4");

        Query q = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("dept1")));
        QueryClass qc1 = new QueryClass(Company.class);
        q.addFrom(qc1);
        Query subQ = new Query();
        QueryClass subQc = new QueryClass(Company.class);
        subQ.addFrom(subQc);
        subQ.addToSelect(subQc);
        ConstraintSet subCs = new ConstraintSet(ConstraintOp.AND);
        subCs.addConstraint(new SimpleConstraint(new QueryField(subQc, "vatNumber"), ConstraintOp.EQUALS, new QueryValue(new Integer(1234))));
        subQ.setConstraint(subCs);
        subQ.setDistinct(false);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc, "company"), ConstraintOp.CONTAINS, qc1));
        cs.addConstraint(new SubqueryConstraint(qc1, ConstraintOp.IN, subQ));

        q.setConstraint(cs);
        q.setDistinct(false);

        Department d =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        d.setName("dept1");
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setVatNumber(1234);
        d.setCompany(c);

        assertEquals(q, eof.createPKQuery(d, source, false));
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

        assertEquals(q, eof.createPKQuery(c, source, false));
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

        assertEquals(q, eof.createPKQuery(c, source, true));
    }

    public void testCreateQuery5() throws Exception {
        Source source = new Source();
        source.setName("testsource4");

        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);

        Query qB = new Query();
        QueryClass qcB = new QueryClass(Company.class);
        qB.addFrom(qcB);
        qB.addToSelect(qcB);
        ConstraintSet csB = new ConstraintSet(ConstraintOp.AND);
        csB.addConstraint(new SimpleConstraint(new QueryField(qcB, "name"), ConstraintOp.EQUALS, new QueryValue("jkhsdfg")));
        QueryClass qcB2 = new QueryClass(Address.class);
        qB.addFrom(qcB2);
        Query qC = new Query();
        QueryClass qcC = new QueryClass(Address.class);
        qC.addFrom(qcC);
        qC.addToSelect(qcC);
        ConstraintSet csC = new ConstraintSet(ConstraintOp.AND);
        csC.addConstraint(new SimpleConstraint(new QueryField(qcC, "address"), ConstraintOp.EQUALS, new QueryValue("10 Downing Street")));
        qC.setConstraint(csC);
        qC.setDistinct(false);
        csB.addConstraint(new ContainsConstraint(new QueryObjectReference(qcB, "address"), ConstraintOp.CONTAINS, qcB2));
        csB.addConstraint(new SubqueryConstraint(qcB2, ConstraintOp.IN, qC));
        qB.setConstraint(csB);
        qB.setDistinct(false);
        cs.addConstraint(new SubqueryConstraint(qc, ConstraintOp.IN, qB));

        Query qD = new Query();
        QueryClass qcD = new QueryClass(Company.class);
        qD.addFrom(qcD);
        qD.addToSelect(qcD);
        ConstraintSet csD = new ConstraintSet(ConstraintOp.AND);
        csD.addConstraint(new SimpleConstraint(new QueryField(qcD, "vatNumber"), ConstraintOp.EQUALS, new QueryValue(new Integer(765213))));
        qD.setConstraint(csD);
        qD.setDistinct(false);
        cs.addConstraint(new SubqueryConstraint(qc, ConstraintOp.IN, qD));

        q.setConstraint(cs);
        q.setDistinct(false);

        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setName("jkhsdfg");
        Address a = new Address();
        a.setAddress("10 Downing Street");
        c.setAddress(a);
        c.setVatNumber(765213);

        assertEquals(q, eof.createPKQuery(c, source, false));
    }
}
