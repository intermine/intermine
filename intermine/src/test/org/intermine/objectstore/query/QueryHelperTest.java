package org.flymine.objectstore.query;

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

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.model.testmodel.Employee;
import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Company;


public class QueryHelperTest extends TestCase
{
    Model model;

    public QueryHelperTest(String arg) {
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
        ops.put("name", ConstraintOp.EQUALS.getIndex().toString());
        ops.put("fullTime", ConstraintOp.EQUALS.getIndex().toString());

        Query q = new Query();
        QueryHelper.addToQuery(q, new QueryClass(Employee.class), fields, ops, model);

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
        ops1.put("name", ConstraintOp.EQUALS.getIndex().toString());
        ops1.put("fullTime", ConstraintOp.EQUALS.getIndex().toString());

        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        QueryHelper.addToQuery(q, qc, fields1, ops1, model);
        assertEquals(1, q.getFrom().size());
        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());


        Map fields2 = new HashMap();
        fields2.put("name", "Gerald");
        fields2.put("fullTime", "true");
        fields2.put("age", "43");

        Map ops2 = new HashMap();
        ops2.put("name", ConstraintOp.EQUALS.getIndex().toString());
        ops2.put("fullTime", ConstraintOp.NOT_EQUALS.getIndex().toString());
        ops2.put("age", ConstraintOp.EQUALS.getIndex().toString());

        QueryHelper.addToQuery(q, qc, fields2, ops2, model);
        assertEquals(1, q.getFrom().size());
        List list = new ArrayList(((ConstraintSet) q.getConstraint()).getConstraints());
        assertFalse(list.get(0) instanceof ConstraintSet);
        assertEquals(3, ((ConstraintSet) q.getConstraint()).getConstraints().size());

    }

    public void testAddToQueryNullParameters() throws Exception {
        Query q = new Query();

        try {
            QueryHelper.addToQuery(null, new QueryClass(Employee.class), new HashMap(), new HashMap(), model);
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addToQuery(q, null, new HashMap(), new HashMap(), model);
            fail("Expected NullPointerException, clsName parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addToQuery(q, new QueryClass(Employee.class), null, new HashMap(), model);
            fail("Expected NullPointerException, fields parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addToQuery(q, new QueryClass(Employee.class), new HashMap(), null, model);
            fail("Expected NullPointerException, ops parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addToQuery(q, new QueryClass(Employee.class), new HashMap(), new HashMap(), null);
            fail("Expected NullPointerException, model parameter null");
        } catch (NullPointerException e) {
        }
    }


    public void testRemoveFromQuery() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        q.addToSelect(qc2);
        q.addFrom(qc2);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        q.setConstraint(c);

        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        assertEquals(2, q.getSelect().size());
        assertEquals(2, q.getFrom().size());
        QueryHelper.removeFromQuery(q, qc1);
        assertEquals(1, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        assertEquals(1, q.getSelect().size());
        assertEquals(1, q.getFrom().size());
        QueryHelper.removeFromQuery(q, qc2);
        assertEquals(0, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        assertEquals(0, q.getSelect().size());
        assertEquals(0, q.getFrom().size());
    }


    public void testRemoveFromQueryNotExists() throws Exception {
                Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        q.addToSelect(qc2);
        q.addFrom(qc2);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        q.setConstraint(c);

        QueryClass qc3 = new QueryClass(Employee.class);

        try {
            QueryHelper.removeFromQuery(q, qc2);
        } catch (Exception e) {
            fail("Expected no Exception to be thrown but was: " + e.getClass());
        }

    }

    public void testRemoveFromQueryNullArguments() throws Exception {
        try {
            QueryHelper.removeFromQuery(null, new QueryClass(Employee.class));
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.removeFromQuery(new Query(), null);
            fail("Expected NullPointerException, qc parameter null");
        } catch (NullPointerException e) {
        }
    }

    public void testGenerateConstraintsAttribute() throws Exception {
        Map fields = new HashMap();
        fields.put("name", "Dennis");
        fields.put("fullTime", "true");

        Map ops = new HashMap();
        ops.put("name", ConstraintOp.EQUALS.getIndex().toString());
        ops.put("fullTime", ConstraintOp.EQUALS.getIndex().toString());

        QueryClass qc = new QueryClass(Employee.class);
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Employee");

        ConstraintSet c = QueryHelper.generateConstraints(qc, fields, ops, new HashMap(), model);
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

    public void testGenerateConstraintsReference() throws Exception {
        Map fields = new HashMap();
        fields.put("department", "a1_");

        Map ops = new HashMap();
        ops.put("department", ConstraintOp.CONTAINS.getIndex().toString());

        QueryClass qc1 = new QueryClass(Employee.class);
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Employee");

        Map aliases = new HashMap();
        QueryClass qc2 = new QueryClass(Department.class);
        aliases.put("a1_", qc2);

        ConstraintSet cs = QueryHelper.generateConstraints(qc1, fields, ops, aliases, model);
        ContainsConstraint cc = (ContainsConstraint) cs.getConstraints().iterator().next();
        assertTrue(cc.getReference() instanceof QueryObjectReference);
        assertEquals(qc1.getType(), cc.getReference().getQueryClass().getType());
        assertEquals(qc2.getType(), cc.getReference().getType());
        assertEquals("department", cc.getReference().getFieldName());
        assertEquals(qc2.getType(), cc.getQueryClass().getType());
    }

    public void testGenerateConstraintsCollection() throws Exception {
        Map fields = new HashMap();
        fields.put("employees", "a1_");

        Map ops = new HashMap();
        ops.put("employees", ConstraintOp.CONTAINS.getIndex().toString());

        QueryClass qc1 = new QueryClass(Department.class);
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        Map aliases = new HashMap();
        QueryClass qc2 = new QueryClass(Employee.class);
        aliases.put("a1_", qc2);

        ConstraintSet cs = QueryHelper.generateConstraints(qc1, fields, ops, aliases, model);
        ContainsConstraint cc = (ContainsConstraint) cs.getConstraints().iterator().next();
        assertTrue(cc.getReference() instanceof QueryCollectionReference);
        assertEquals(qc1.getType(), cc.getReference().getQueryClass().getType());
        //assertEquals(qc2.getType(), cc.getReference().getType());
        assertEquals("employees", cc.getReference().getFieldName());
        assertEquals(qc2.getType(), cc.getQueryClass().getType());
    }

    public void testAddConstraintNull() throws Exception {
       try {
            QueryHelper.addConstraint(null, new ConstraintSet(ConstraintOp.AND));
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(new Query(), null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testAddConstraintEmpty() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("Bob"));

        q.setConstraint(sc);
        QueryHelper.addConstraint(q, new ConstraintSet(ConstraintOp.AND));

        assertEquals(sc, q.getConstraint());
    }

    public void testAddConstraintToNull() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        Constraint sc = new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("Bob"));
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(sc);

        QueryHelper.addConstraint(q, cs);

        assertEquals(cs, q.getConstraint());
    }

    public void testAddConstraintToConstraint() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("Bob"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc, "age"), ConstraintOp.EQUALS, new QueryValue(new Integer(54)));
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(sc2);

        q.setConstraint(sc1);
        QueryHelper.addConstraint(q, cs2);

        ConstraintSet cs3 = new ConstraintSet(ConstraintOp.AND);
        cs3.addConstraint(sc1);
        cs3.addConstraint(sc2);
        assertEquals(cs3, q.getConstraint());
    }


    public void testAddConstraintToConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("Bob"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc, "age"), ConstraintOp.EQUALS, new QueryValue(new Integer(54)));
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(sc1);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(sc2);

        q.setConstraint(cs1);
        QueryHelper.addConstraint(q, cs2);

        ConstraintSet cs3 = new ConstraintSet(ConstraintOp.AND);
        cs3.addConstraint(sc1);
        cs3.addConstraint(sc2);
        assertEquals(cs3, q.getConstraint());
    }


    public void testRemoveConstraintsAssociated() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        SimpleConstraint sc3 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryField(qc2, "name"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        c.addConstraint(sc3);
        q.setConstraint(c);

        // cross-reference (sc3) should not get removed
        assertEquals(3, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryHelper.removeConstraints(q, qc1, false);
        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryHelper.removeConstraints(q, qc2, false);
        assertEquals(1, ((ConstraintSet) q.getConstraint()).getConstraints().size());
    }

    public void testRemoveConstraintsRelated() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryField(qc2, "name"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        q.setConstraint(c);

        // remove qc1, leaves sc2
        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryHelper.removeConstraints(q, qc1, true);
        assertEquals(1, ((ConstraintSet) q.getConstraint()).getConstraints().size());

        // removing qc2 gets rid of all constraints
        c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        q.setConstraint(c);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryHelper.removeConstraints(q, qc2, true);
        assertEquals(0, ((ConstraintSet) q.getConstraint()).getConstraints().size());
    }


    public void testCreateQueryValue() throws Exception {
        QueryValue qv = null;

        qv = QueryHelper.createQueryValue(Integer.class, "101");
        assertEquals(new Integer(101), (Integer) qv.getValue());
        qv = QueryHelper.createQueryValue(Float.class, "1.01");
        assertEquals(new Float(1.01), (Float) qv.getValue());
        qv = QueryHelper.createQueryValue(Double.class, "1.01");
        assertEquals(new Double(1.01), (Double) qv.getValue());
        qv = QueryHelper.createQueryValue(Boolean.class, "false");
        assertEquals(new Boolean(false), (Boolean) qv.getValue());
        qv = QueryHelper.createQueryValue(String.class, "test");
        assertEquals("test", qv.getValue());
        qv = QueryHelper.createQueryValue(Long.class, "101");
        assertEquals(new Long(101), (Long) qv.getValue());
        qv = QueryHelper.createQueryValue(Short.class, "101");
        assertEquals(new Short((short)101), (Short) qv.getValue());
        qv = QueryHelper.createQueryValue(Date.class, "30/08/76");
        assertEquals(new SimpleDateFormat(QueryHelper.DATE_FORMAT).parse("30/08/76"), qv.getValue());

        try {
            qv = QueryHelper.createQueryValue(java.util.Iterator.class, "test");
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
