package org.flymine.objectstore;

import junit.framework.TestCase;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.*;
import java.beans.*;
import java.math.BigDecimal;

import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.query.ExplainResult;
import org.flymine.objectstore.query.*;
import org.flymine.model.testmodel.*;


/**
 * TestCase for all ObjectStores
 *
 */

public abstract class ObjectStoreTestCase extends SetupDataTestCase
{
    protected static ObjectStoreAbstractImpl os;

    /**
     * Constructor
     */
    public ObjectStoreTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();
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
    }

    protected static List toList(Object[][] o) {
        List rows = new ArrayList();
        for(int i=0;i<o.length;i++) {
            rows.add(new ResultsRow(Arrays.asList((Object[])o[i])));
        }
        return rows;
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

    // Extra tests for all ObjectStores

    public void testLimitTooHigh() throws Exception {
        // try to run query with limit higher than imposed maximum
        int before = os.maxLimit;
        os.maxLimit = 99;
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Manager.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        try{
            List l1 = os.execute(q1, 10, 100);
            fail("Expected: ObjectStoreException");
        }  catch (IndexOutOfBoundsException e) {
        } finally {
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
            List l1 = os.execute(q1, 100, 50);
            fail("Expected: ObjectStoreException");
        } catch (IndexOutOfBoundsException e) {
        } finally {
            os.maxOffset = before;
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

    public void testEstimateStartEndNotNull() throws Exception {
        ExplainResult er = os.estimate((Query)queries.get("WhereClassClass"), 0, 10);
        if (er == null) {
            fail("a null ExplainResult was returned");
        }
    }
}
