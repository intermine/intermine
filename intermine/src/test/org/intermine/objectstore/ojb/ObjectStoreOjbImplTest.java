package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.math.BigDecimal;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.QueryFunction;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.sql.query.ExplainResult;

import org.flymine.model.testmodel.*;

public class ObjectStoreOjbImplTest extends QueryTestCase
{
    public ObjectStoreOjbImplTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        //Thread.sleep(10000);
    }

    public void setUpResults() throws Exception {
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

        r = new Object[][] { { data.get("ContractorA"), data.get("CompanyA") },
                             { data.get("ContractorA"), data.get("CompanyB") } };
        results.put("ContainsMN", toList(r));

        r = new Object[][] { { data.get("CompanyA"), new Long(1) },
                             { data.get("CompanyB"), new Long(2) } };
        results.put("SimpleGroupBy", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1"), data.get("EmployeeA1"), ((Employee)data.get("EmployeeA1")).getAddress() } };
        results.put("MultiJoin", toList(r));

        r = new Object[][] { { new BigDecimal("3476.0000000000"), "DepartmentA1", data.get("DepartmentA1") },
                             { new BigDecimal("3476.0000000000"), "DepartmentB1", data.get("DepartmentB1") },
                             { new BigDecimal("3476.0000000000"), "DepartmentB2", data.get("DepartmentB2") } };
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
    }

    public void executeTest(String type) throws Exception {
        Results res = os.execute((Query)queries.get(type));
        assertEquals(type + " has failed", results.get(type), res);
    }

    private List toList(Object[][] o) {
        List rows = new ArrayList();
        for(int i=0;i<o.length;i++) {
            rows.add(new ResultsRow(Arrays.asList((Object[])o[i])));
        }
        return rows;
    }


    public void testCEOWhenSearchingForManager() throws Exception {
        QueryClass c1 = new QueryClass(Manager.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("EmployeeB1");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        q1.setConstraint(sc1);
        List l1 = os.execute(q1, 0, 10);
        //System.out.println(l1.toString());
        //System.out.println(l1.get(0).getClass());
        //System.out.println(((ResultsRow) l1.get(0)).get(0).getClass());
        CEO ceo = (CEO) (((ResultsRow) l1.get(0)).get(0));
        //System.out.println(ceo.getSalary());
        assertEquals(45000, ceo.getSalary());
    }

    public void testLimitTooHigh() throws Exception {
        // try to run query with limit higher than imposed maximum
        int before = os.maxLimit;
        os.maxLimit = 99;
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Manager.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        try{
            List l1 = os.execute(q1, 10, 120);
            fail("Expected: ObjectStoreException");
        }  catch (ObjectStoreException e) {
            os.maxLimit = before;
        }
    }

    public void testOffsetTooHigh() throws Exception {
        // try to run query with offset higher than imposed maximum
        int before = os.maxOffset;
        os.maxOffset = 99;
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Manager.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        try {
            List l1 = os.execute(q1, 100, 180);
            fail("Expected: ObjectStoreException");
        }  catch (ObjectStoreException e) {
            os.maxOffset = before;
        }
    }

    public void testTooManyRows() throws Exception {
        // try to run a query that returns more than max number of rows
        int  before = os.maxRows;
        os.maxRows = 0;
        Query q1 = new Query();
        try {
            List l1 = os.execute((Query)queries.get("WhereClassClass"), 0, 10);
            fail("Expected: ObjectStoreException");
        }  catch (ObjectStoreException e) {
            os.maxRows = before;
        }
    }


    public void testTooMuchTime()  throws Exception {
        // try to run a query that takes longer than max amount of time
        long before = os.maxTime;
        os.maxTime = 0;
        try {
            List l1 = os.execute((Query)queries.get("WhereClassClass"), 0, 50);
            fail("Expected: ObjectStoreException");
        }  catch (ObjectStoreException e) {
            os.maxTime = before;
        }
    }


    public void testEstimateQueryNotNull1() throws Exception {
        ExplainResult er = os.estimate((Query)queries.get("WhereClassClass"));
        if (er == null) {
            fail("a null ExplainResult was returned");
        }
    }

    public void testExtimateStartEndNotNull() throws Exception {
        ExplainResult er = os.estimate((Query)queries.get("WhereClassClass"), 0, 10);
        if (er == null) {
            fail("a null ExplainResult was returned");
        }
    }

    public void testResults() throws Exception {
        Object[][] r = new Object[][] { { data.get("CompanyA") },
                                        { data.get("CompanyB") } };
        List res = os.execute((Query) queries.get("SelectSimpleObject"));
        assertEquals(toList(r).size(), res.size());
        assertEquals(toList(r), res);
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
        assertEquals(a, ((Employee) data.get("EmployeeA1")).getAddress());
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
        assertEquals(contractors, expected1);

        List companies = ((Contractor) contractors.get(0)).getCompanys();
        assertTrue(companies instanceof Results);
        List expected2 = new ArrayList();
        expected2.add(data.get("CompanyA"));
        expected2.add(data.get("CompanyB"));
        assertEquals(companies, expected2);
    }


    public void testLazyReference() throws Exception {
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
        Company c = d.getCompany();
        assertTrue(c instanceof org.flymine.objectstore.proxy.LazyReference);
        assertTrue(c.equals(data.get("CompanyA")));
        assertTrue(data.get("CompanyA").equals(c));
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
        assertTrue(a instanceof org.flymine.objectstore.proxy.LazyReference);
        assertEquals(a, ((Employee) data.get("EmployeeA1")).getAddress());
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
        assertEquals(a, ((Company) data.get("CompanyA")).getAddress());
    }

}
