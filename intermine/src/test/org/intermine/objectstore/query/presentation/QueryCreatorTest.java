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
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.flymine.objectstore.query.*;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.model.testmodel.Employee;
import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Company;


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
        ops.put("name", SimpleConstraint.EQUALS.getIndex().toString());
        ops.put("fullTime", SimpleConstraint.EQUALS.getIndex().toString());

        Query q = new Query();
        QueryCreator.addToQuery(q, new QueryClass(Employee.class), fields, ops, model);

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


    // Add a QueryClass and its constraints to a query then alter and add
    // the same again - i.e. editing existing QueryClass
    public void testAddToQueryExists() throws Exception {
        Map fields1 = new HashMap();
        fields1.put("name", "Dennis");
        fields1.put("fullTime", "true");

        Map ops1 = new HashMap();
        ops1.put("name", SimpleConstraint.EQUALS.getIndex().toString());
        ops1.put("fullTime", SimpleConstraint.EQUALS.getIndex().toString());

        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        QueryCreator.addToQuery(q, qc, fields1, ops1, model);
        assertEquals(1, q.getFrom().size());
        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());


        Map fields2 = new HashMap();
        fields2.put("name", "Gerald");
        fields2.put("fullTime", "true");
        fields2.put("age", "43");

        Map ops2 = new HashMap();
        ops2.put("name", SimpleConstraint.EQUALS.getIndex().toString());
        ops2.put("fullTime", SimpleConstraint.NOT_EQUALS.getIndex().toString());
        ops2.put("age", SimpleConstraint.EQUALS.getIndex().toString());

        QueryCreator.addToQuery(q, qc, fields2, ops2, model);
        assertEquals(1, q.getFrom().size());
        List list = new ArrayList(((ConstraintSet) q.getConstraint()).getConstraints());
        assertFalse(list.get(0) instanceof ConstraintSet);
        assertEquals(3, ((ConstraintSet) q.getConstraint()).getConstraints().size());

    }

    public void testAddToQueryNullParameters() throws Exception {
        Query q = new Query();

        try {
            QueryCreator.addToQuery(null, new QueryClass(Employee.class), new HashMap(), new HashMap(), model);
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryCreator.addToQuery(q, null, new HashMap(), new HashMap(), model);
            fail("Expected NullPointerException, clsName parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryCreator.addToQuery(q, new QueryClass(Employee.class), null, new HashMap(), model);
            fail("Expected NullPointerException, fields parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryCreator.addToQuery(q, new QueryClass(Employee.class), new HashMap(), null, model);
            fail("Expected NullPointerException, ops parameter null");
        } catch (NullPointerException e) {
        }
    }

    public void testGenerateConstraints() throws Exception {
        Map fields = new HashMap();
        fields.put("name", "Dennis");
        fields.put("fullTime", "true");

        Map ops = new HashMap();
        ops.put("name", SimpleConstraint.EQUALS.getIndex().toString());
        ops.put("fullTime", SimpleConstraint.EQUALS.getIndex().toString());

        QueryClass qc = new QueryClass(Employee.class);
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Employee");

        ConstraintSet c = QueryCreator.generateConstraints(qc, fields, ops, new HashMap(), model);
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
        } catch (RuntimeException e) {
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
        cs3.addConstraint(sc2);
        assertEquals(cs3, q.getConstraint());
    }


    public void testRemoveConstraintsSimple() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    SimpleConstraint.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    SimpleConstraint.EQUALS,
                                                    new QueryValue("department1"));
        ConstraintSet c = new ConstraintSet(ConstraintSet.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        q.setConstraint(c);

        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryCreator.removeConstraints(q, qc1);
        assertEquals(1, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryCreator.removeConstraints(q, qc2);
        assertEquals(0, ((ConstraintSet) q.getConstraint()).getConstraints().size());
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
