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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.objectstore.query.SimpleConstraint;

import org.flymine.model.testmodel.*;

/**
 * TestCase for all ObjectStores
 *
 */
public abstract class ObjectStoreTestCase extends SetupDataTestCase
{
    protected static ObjectStore os;

    public ObjectStoreTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();
        setUpResults();
    }

    /**
     * Set up all the results expected for a given subset of queries
     *
     * @throws Exception if an error occurs
     */
    public static void setUpResults() throws Exception {
        Object[][] r;

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") } };
        results.put("SelectSimpleObject", toList(r));

        r = new Object[][] { { "CompanyA", new Integer(5) },
                             { "CompanyB", new Integer(5) } };
        results.put("SubQuery", toList(r));

        r = new Object[][] { { "CompanyA" } };
        results.put("WhereSimpleEquals", toList(r));

        r = new Object[][] { { "CompanyB" } };
        results.put("WhereSimpleNotEquals", toList(r));
        results.put("WhereSimpleNegEquals", toList(r));

        r = new Object[][] { { "CompanyA" },
                             { "CompanyB" } };
        results.put("WhereSimpleLike", toList(r));

        r = new Object[][] { { "CompanyA" } };
        results.put("WhereEqualsString", toList(r));

        r = new Object[][] { { "CompanyB" } };
        results.put("WhereAndSet", toList(r));

        r = new Object[][] { { "CompanyA" },
                             { "CompanyB" } };
        results.put("WhereOrSet", toList(r));

        r = new Object[][] { { "CompanyA" } };
        results.put("WhereNotSet", toList(r));

        r = new Object[][] { { data.get("DepartmentA1") },
                             { data.get("DepartmentB1") },
                             { data.get("DepartmentB2") } };
        results.put("WhereSubQueryField", toList(r));

        r = new Object[][] { { data.get("CompanyA") } };
        results.put("WhereSubQueryClass", toList(r));

        r = new Object[][] { { data.get("CompanyB") } };
        results.put("WhereNotSubQueryClass", toList(r));

        r = new Object[][] { { data.get("CompanyB") } };
        results.put("WhereNegSubQueryClass", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("CompanyA") },
                             { data.get("CompanyB"), data.get("CompanyB") } };
        results.put("WhereClassClass", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("CompanyB") },
                             { data.get("CompanyB"), data.get("CompanyA") } };
        results.put("WhereNotClassClass", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("CompanyB") },
                             { data.get("CompanyB"), data.get("CompanyA") } };
        results.put("WhereNegClassClass", toList(r));

        r = new Object[][] { { data.get("CompanyA") } };
        results.put("WhereClassObject", toList(r));

        r = new Object[][] { { data.get("DepartmentA1"), data.get("EmployeeA1") } };
        results.put("Contains11", toList(r));

        r = new Object[][] { { data.get("DepartmentA1"), data.get("EmployeeB1") },
                             { data.get("DepartmentA1"), data.get("EmployeeB3") } };
        results.put("ContainsNot11", toList(r));
        results.put("ContainsNeg11", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1") } };
        results.put("Contains1N", toList(r));

        r = new Object[][] { { data.get("DepartmentA1"), data.get("CompanyA") } };
        results.put("ContainsN1", toList(r));

        r = new Object[][] { { data.get("ContractorA"), data.get("CompanyA") },
                             { data.get("ContractorA"), data.get("CompanyB") } };
        results.put("ContainsMN", toList(r));
        
        r = new Object[][] { { data.get("ContractorA"), data.get("CompanyA") },
                             { data.get("ContractorA"), data.get("CompanyB") },
                             { data.get("ContractorB"), data.get("CompanyA") },
                             { data.get("ContractorB"), data.get("CompanyB") } };
        results.put("ContainsDuplicatesMN", toList(r));

        r = new Object[][] { { data.get("CompanyA"), new Long(1) },
                             { data.get("CompanyB"), new Long(2) } };
        results.put("SimpleGroupBy", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1"), data.get("EmployeeA1"), ((Employee)data.get("EmployeeA1")).getAddress() } };
        results.put("MultiJoin", toList(r));

        r = new Object[][] { { new BigDecimal("3476.0000000000000"), "DepartmentA1", data.get("DepartmentA1") },
                             { new BigDecimal("3476.0000000000000"), "DepartmentB1", data.get("DepartmentB1") },
                             { new BigDecimal("3476.0000000000000"), "DepartmentB2", data.get("DepartmentB2") } };
        results.put("SelectComplex", toList(r));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") } };
        results.put("SelectClassAndSubClasses", toList(r));

        r = new Object[][] { { data.get("ContractorA") },
                             { data.get("ContractorB") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") },
                             { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") } };
        results.put("SelectInterfaceAndSubClasses", toList(r));

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") },
                             { data.get("DepartmentB1") },
                             { data.get("DepartmentB2") },
                             { data.get("DepartmentA1") } };
        results.put("SelectInterfaceAndSubClasses2", toList(r));

        r = new Object[][] { { data.get("ContractorA") },
                             { data.get("ContractorB") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB3") },
                             { data.get("EmployeeA1") } };
        results.put("SelectInterfaceAndSubClasses3", toList(r));

        r = new Object[][] { { new Integer(5), "CompanyA" },
                             { new Integer(5), "CompanyB" } };
        results.put("OrderByAnomaly", toList(r));

        r = new Object[][] { { data.get("Secretary1") },
                             { data.get("Secretary2") },
                             { data.get("Secretary3") } };
        results.put("SelectUnidirectionalCollection", toList(r));

        r = new Object[][] { { data.get("CompanyA") } };
        results.put("SelectClassObjectSubquery", toList(r));

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") } };
        results.put("EmptyAndConstraintSet", toList(r));

        results.put("EmptyOrConstraintSet", Collections.EMPTY_LIST);

        results.put("EmptyNandConstraintSet", Collections.EMPTY_LIST);

        results.put("EmptyNorConstraintSet", toList(r));

        results.put("BagConstraint", Collections.singletonList(Collections.singletonList(data.get("CompanyA"))));

        results.put("BagConstraint2", Collections.singletonList(Collections.singletonList(data.get("CompanyA"))));
    }

    /**
     * Execute a test for a query. This should run the query and
     * contain an assert call to assert that the returned results are
     * those expected.
     *
     * @param type the type of query we are testing (ie. the key in the queries Map)
     * @throws Exception if type does not appear in the queries map
     */
    public void executeTest(String type) throws Exception {
        Results res = os.execute((Query)queries.get(type));
        assertEquals(type + " has failed", results.get(type), res);
    }

    public void testResults() throws Exception {
        Object[][] r = new Object[][] { { data.get("CompanyA") },
                                        { data.get("CompanyB") } };
        List res = os.execute((Query) queries.get("SelectSimpleObject"));
        assertEquals(toList(r).size(), res.size());
        assertEquals(toList(r), res);
    }

    // estimate tests

    public void testEstimateQueryNotNull() throws Exception {
        ResultsInfo er = os.estimate((Query)queries.get("WhereClassClass"));
        if (er == null) {
            fail("a null ResultsInfo was returned");
        }
    }

    // reference and collection proxy tests

    public void testCEOWhenSearchingForManager() throws Exception {
        // select manager where manager.name="EmployeeB1" (actually a CEO)
        QueryClass c1 = new QueryClass(Manager.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("EmployeeB1");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        q1.setConstraint(sc1);
        List l1 = os.execute(q1);
        CEO ceo = (CEO) (((ResultsRow) l1.get(0)).get(0));
        assertEquals(45000, ceo.getSalary());
    }

    public void testLazyCollection() throws Exception {
        List r = os.execute((Query) queries.get("ContainsN1"));
        Department d = (Department) ((ResultsRow) r.get(0)).get(0);
        List e = d.getEmployees();
        assertTrue(e instanceof Results);

        List expected = new ArrayList();
        expected.add(data.get("EmployeeA1"));
        expected.add(data.get("EmployeeA2"));
        expected.add(data.get("EmployeeA3"));
        assertEquals(expected, e);

        // navigate from a lazy collection to a field that is a lazy reference
        Address a = ((Employee) e.get(0)).getAddress();
        assertTrue(a instanceof LazyReference);
        assertEquals(((Employee) data.get("EmployeeA1")).getAddress(), a);
    }

    public void testLazyCollectionMtoN() throws Exception {
        // query for company and check contractors
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        q1.setConstraint(sc1);
        Results r  = os.execute(q1);
        ResultsRow rr = (ResultsRow) r.get(0);
        Company c = (Company) rr.get(0);
        List contractors = c.getContractors();
        assertTrue(contractors instanceof Results);
        List expected1 = new ArrayList();
        expected1.add(data.get("ContractorA"));
        expected1.add(data.get("ContractorB"));
        assertEquals(expected1, contractors);

        List companies = ((Contractor) contractors.get(0)).getCompanys();
        assertTrue(companies instanceof Results);
        List expected2 = new ArrayList();
        expected2.add(data.get("CompanyA"));
        expected2.add(data.get("CompanyB"));
        assertEquals(expected2, companies);
    }

    public void testLazyReference() throws Exception {
        // select department where department.name="DepartmentA1"
        QueryClass qc1 = new QueryClass(Department.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        QueryField f1 = new QueryField(qc1, "name");
        QueryValue v1 = new QueryValue("DepartmentA1");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        q1.setConstraint(sc1);
        Results r  = os.execute(q1);
        ResultsRow rr = (ResultsRow) r.get(0);
        Department d = (Department) rr.get(0);
        Company c1 = d.getCompany();
        Company c2 = (Company) data.get("CompanyA");
        assertTrue(c1 instanceof LazyReference);
        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
        assertEquals(c2.getId(), c1.getId());
        assertEquals(c2.hashCode(), c1.hashCode());
        assertFalse(((LazyReference) c1).isMaterialised());
        assertEquals(c2.getName(), c1.getName());
        assertTrue(((LazyReference) c1).isMaterialised());
    }

    public void testLazyCollectionRef() throws Exception {
        QueryClass c1 = new QueryClass(Department.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("DepartmentA1");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        q1.setConstraint(sc1);
        Results r  = os.execute(q1);
        ResultsRow rr = (ResultsRow) r.get(0);
        Department d = (Department) rr.get(0);
        List e = d.getEmployees();
        assertTrue(e instanceof Results);

        List expected = new ArrayList();
        expected.add(data.get("EmployeeA1"));
        expected.add(data.get("EmployeeA2"));
        expected.add(data.get("EmployeeA3"));

        // test that we can navigate from member of lazy collection to one of its lazy references
        assertEquals(expected, e);
        Employee e1 = (Employee) e.get(0);
        Address a = e1.getAddress();
        assertTrue(a instanceof LazyReference);
        assertEquals(((Employee) data.get("EmployeeA1")).getAddress(), a);
    }

    public void testLazyReferenceRef() throws Exception {
        QueryClass c1 = new QueryClass(Department.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("DepartmentA1");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        q1.setConstraint(sc1);
        List r  = os.execute(q1);
        Department d = (Department) ((ResultsRow) r.get(0)).get(0);

        Company c = d.getCompany();
        assertTrue(c instanceof LazyReference);
        assertEquals(data.get("CompanyA"), c);

        // navigate from a lazy reference to a field that is a lazy reference
        Address a = c.getAddress();
        assertTrue(a instanceof LazyReference);
        assertEquals(((Company) data.get("CompanyA")).getAddress(), a);
    }

    // setDistinct tests

    public void testCountNoGroupByNotDistinct() throws Exception {
        Query q = (Query) queries.get("ContainsDuplicatesMN");
        q.setDistinct(false);
        int count = os.count(q);
        assertEquals(8, count);
    }

    public void testCountNoGroupByDistinct() throws Exception {
        Query q = (Query) queries.get("ContainsDuplicatesMN");
        q.setDistinct(true);
        int count = os.count(q);
        assertEquals(4, os.execute(q).size());
    }

   public void testCountGroupByNotDistinct() throws Exception {
        Query q = (Query) queries.get("SimpleGroupBy");
        q.setDistinct(false);
        int count = os.count(q);
        assertEquals(2, count);
    }

    public void testCountGroupByDistinct() throws Exception {
    // distinct doesn't actually do anything to group by reuslt
        Query q = (Query) queries.get("SimpleGroupBy");
        q.setDistinct(true);
        int count = os.count(q);
        assertEquals(2, count);
    }

    // getObjectByExample tests

    public void testGetObjectByExampleNull() throws Exception {
        try {
            os.getObjectByExample(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testGetObjectByExampleNonExistent() throws Exception {
        Address a = new Address();
        a.setAddress("10 Downing Street");
        assertNull(os.getObjectByExample(a));
    }

    public void testGetObjectByExampleIncomplete() throws Exception {
        Employee e = new Employee();
        e.setName("EmployeeA1");
        e.setAge(10);
        //address not set
        try {
            os.getObjectByExample(e);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }
    }

    public void testGetObjectByExampleAttribute() throws Exception {
        Address a1 = ((Employee) data.get("EmployeeA1")).getAddress();
        Address a = new Address();
        a.setAddress(a1.getAddress());
        assertEquals(a1, os.getObjectByExample(a1));
    }

    public void testGetObjectByExampleFields() throws Exception {
        Employee e1 = (Employee) data.get("EmployeeA1");
        Employee e = new Employee();
        e.setName(e1.getName());
        e.setAge(e1.getAge());
        e.setAddress(e1.getAddress());
        assertEquals(e1, os.getObjectByExample(e));
    }

    public void testDataTypes() throws Exception {
        Types d1 = (Types) data.get("Types1");
        //Types d2 = new Types();
        //d2.setName(d1.getName());
        Query q = new Query();
        QueryClass c = new QueryClass(Types.class);
        q.addFrom(c);
        q.addToSelect(c);
        ConstraintSet cs = new ConstraintSet(ConstraintSet.AND);
        cs.addConstraint(new SimpleConstraint(new QueryField(c, "floatType"), ConstraintOp.EQUALS, new QueryValue(new Float(0.6))));
        cs.addConstraint(new SimpleConstraint(new QueryField(c, "doubleType"), ConstraintOp.EQUALS, new QueryValue(new Double(0.88))));
        cs.addConstraint(new SimpleConstraint(new QueryField(c, "floatObjType"), ConstraintOp.EQUALS, new QueryValue(new Float(1.6))));
        cs.addConstraint(new SimpleConstraint(new QueryField(c, "doubleObjType"), ConstraintOp.EQUALS, new QueryValue(new Double(1.88))));
        q.setConstraint(cs);
        Results res = os.execute(q);
        Types d = (Types) ((List) res.get(0)).get(0);

        //Types d = (Types) (os.getObjectByExample(d2));

        // Go through each attribute to check that it has been set correctly
        assertEquals(d1.getIntType(), d.getIntType());
        assertEquals(d1.getFloatType(), d.getFloatType(), 0.0);
        assertEquals(d1.getDoubleType(), d.getDoubleType(), 0.0);
        assertEquals(d1.getBooleanType(), d.getBooleanType());
        assertEquals(d1.getIntObjType(), d.getIntObjType());
        assertEquals(d1.getFloatObjType(), d.getFloatObjType());
        assertEquals(d1.getDoubleObjType(), d.getDoubleObjType());
        // Boolean objects not properly supported in OJB
        // assertEquals(d1.getBooleanObjType(), d.getBooleanObjType());
        assertEquals(d1.getStringObjType(), d.getStringObjType());
        assertEquals(d1.getDateObjType(), d.getDateObjType());
    }


    // helper method

    protected static List toList(Object[][] o) {
        List rows = new ArrayList();
        for(int i=0;i<o.length;i++) {
            rows.add(new ResultsRow(Arrays.asList((Object[])o[i])));
        }
        return rows;
    }
}
