package org.flymine.objectstore.query.presentation;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.flymine.objectstore.query.*;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.model.testmodel.Employee;

public class QueryCreatorTest extends TestCase
{
    Model model;

    public QueryCreatorTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testAddToQuery() throws Exception {
        Map fields = new HashMap();
        fields.put("name", "Dennis");
        fields.put("fullTime", "true");

        Map ops = new HashMap();
        ops.put("name", String.valueOf(SimpleConstraint.EQUALS));
        ops.put("fullTime", String.valueOf(SimpleConstraint.EQUALS));

        Query q = new Query();
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Employee");
        QueryCreator.addToQuery(q, cld, fields, ops);

        ArrayList from = new ArrayList(q.getFrom());
        assertEquals(Employee.class, ((QueryClass) from.get(0)).getType());

        ArrayList list = new ArrayList(((ConstraintSet) q.getConstraint()).getConstraints());
        SimpleConstraint res1 = (SimpleConstraint) list.get(0);
        assertEquals("fullTime", ((QueryField) res1.getArg1()).getFieldName());
        assertEquals(Boolean.class, ((QueryField) res1.getArg1()).getType());
        assertEquals(new Boolean(true), ((QueryValue) res1.getArg2()).getValue());
        assertEquals(Boolean.class, ((QueryValue) res1.getArg2()).getType());

        SimpleConstraint res2 = (SimpleConstraint) list.get(1);
        assertEquals("name", ((QueryField) res2.getArg1()).getFieldName());
        assertEquals(String.class, ((QueryField) res2.getArg1()).getType());
        assertEquals("Dennis", ((QueryValue) res2.getArg2()).getValue());
        assertEquals(String.class, ((QueryValue) res2.getArg2()).getType());
    }

    public void testAddToQueryNullParameters() throws Exception {
        Query q = new Query();
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Employee");

        try {
            QueryCreator.addToQuery(null, cld, new HashMap(), new HashMap());
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryCreator.addToQuery(q, null, new HashMap(), new HashMap());
            fail("Expected NullPointerException, clsName parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryCreator.addToQuery(q, cld, null, new HashMap());
            fail("Expected NullPointerException, fields parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryCreator.addToQuery(q, cld, new HashMap(), null);
            fail("Expected NullPointerException, ops parameter null");
        } catch (NullPointerException e) {
        }
    }

    public void testGenerateConstraints() throws Exception {
        Map fields = new HashMap();
        fields.put("name", "Dennis");
        fields.put("fullTime", "true");

        Map ops = new HashMap();
        ops.put("name", String.valueOf(SimpleConstraint.EQUALS));
        ops.put("fullTime", String.valueOf(SimpleConstraint.EQUALS));

        QueryClass qc = new QueryClass(Employee.class);
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Employee");

        ConstraintSet c = QueryCreator.generateConstraints(qc, fields, ops, new HashMap(), cld);
        ArrayList list = new ArrayList(c.getConstraints());
        SimpleConstraint res1 = (SimpleConstraint) list.get(0);
        assertEquals("fullTime", ((QueryField) res1.getArg1()).getFieldName());
        assertEquals(Boolean.class, ((QueryField) res1.getArg1()).getType());
        assertEquals(new Boolean(true), ((QueryValue) res1.getArg2()).getValue());
        assertEquals(Boolean.class, ((QueryValue) res1.getArg2()).getType());

        SimpleConstraint res2 = (SimpleConstraint) list.get(1);
        assertEquals("name", ((QueryField) res2.getArg1()).getFieldName());
        assertEquals(String.class, ((QueryField) res2.getArg1()).getType());
        assertEquals("Dennis", ((QueryValue) res2.getArg2()).getValue());
        assertEquals(String.class, ((QueryValue) res2.getArg2()).getType());
    }

    public void testAddConstraintNull() throws Exception {
        try {
            QueryCreator.addConstraint(new Query(), null);
            fail("Expected NullPointerException");
        } catch (Exception e) {
        }
    }

    public void testAddConstraintEmpty() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"), SimpleConstraint.EQUALS, new QueryValue("Bob"));

        q.setConstraint(sc);
        QueryCreator.addConstraint(q, new ConstraintSet(ConstraintSet.AND));

        assertEquals(sc, q.getConstraint());
    }
    
    public void testAddConstraintToNull() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        Constraint sc = new SimpleConstraint(new QueryField(qc, "name"), SimpleConstraint.EQUALS, new QueryValue("Bob"));
        ConstraintSet cs = new ConstraintSet(ConstraintSet.AND);
        cs.addConstraint(sc);

        QueryCreator.addConstraint(q, cs);

        assertEquals(cs, q.getConstraint());
    }

    public void testAddConstraintToConstraint() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc, "name"), SimpleConstraint.EQUALS, new QueryValue("Bob"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc, "age"), SimpleConstraint.EQUALS, new QueryValue(new Integer(54)));
        ConstraintSet cs2 = new ConstraintSet(ConstraintSet.AND);
        cs2.addConstraint(sc2);

        q.setConstraint(sc1);
        QueryCreator.addConstraint(q, cs2);

        ConstraintSet cs3 = new ConstraintSet(ConstraintSet.AND);
        cs3.addConstraint(sc1);
        cs3.addConstraint(sc2);
        assertEquals(cs3, q.getConstraint());
    }

    public void testAddConstraintToConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc, "name"), SimpleConstraint.EQUALS, new QueryValue("Bob"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc, "age"), SimpleConstraint.EQUALS, new QueryValue(new Integer(54)));
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(sc1);
        ConstraintSet cs2 = new ConstraintSet(ConstraintSet.AND);
        cs2.addConstraint(sc2);

        q.setConstraint(cs1);
        QueryCreator.addConstraint(q, cs2);

        ConstraintSet cs3 = new ConstraintSet(ConstraintSet.AND);
        cs3.addConstraint(sc1);
        cs3.addConstraint(cs2);
        assertEquals(cs3, q.getConstraint());
    }

    public void testCreateQueryValue() throws Exception {
        String value = null;
        QueryValue qv = null;

        qv = QueryCreator.createQueryValue(Integer.class, "101");
        assertEquals(new Integer(101), (Integer) qv.getValue());
        qv = QueryCreator.createQueryValue(Float.class, "1.01");
        assertEquals(new Float(1.01), (Float) qv.getValue());
        qv = QueryCreator.createQueryValue(Double.class, "1.01");
        assertEquals(new Double(1.01), (Double) qv.getValue());
        qv = QueryCreator.createQueryValue(Boolean.class, "false");
        assertEquals(new Boolean(false), (Boolean) qv.getValue());
        qv = QueryCreator.createQueryValue(String.class, "test");
        assertEquals("test", qv.getValue());
        qv = QueryCreator.createQueryValue(Long.class, "101");
        assertEquals(new Long(101), (Long) qv.getValue());
        qv = QueryCreator.createQueryValue(Short.class, "101");
        assertEquals(new Short((short)101), (Short) qv.getValue());
        qv = QueryCreator.createQueryValue(Date.class, "30/08/76");
        assertEquals(new SimpleDateFormat(QueryCreator.DATE_FORMAT).parse("30/08/76"), qv.getValue());

        try {
            qv = QueryCreator.createQueryValue(java.util.Iterator.class, "test");
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
