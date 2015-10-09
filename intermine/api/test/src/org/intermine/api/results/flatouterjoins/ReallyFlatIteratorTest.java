package org.intermine.api.results.flatouterjoins;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.intermine.objectstore.Failure;
import org.intermine.objectstore.ObjectStoreAbstractImplTestCase;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

public class ReallyFlatIteratorTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        osai = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        os = osai;
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
        setUpResults();
    }

    public ReallyFlatIteratorTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ReallyFlatIteratorTest.class);
    }

    public static void setUpResults() throws Exception {
        ObjectStoreAbstractImplTestCase.setUpResults();

        results.put("CollectionPathExpression", Arrays.asList(
                    Arrays.asList(data.get("DepartmentA1"), data.get("EmployeeA1")),
                    Arrays.asList(data.get("DepartmentA1"), data.get("EmployeeA2")),
                    Arrays.asList(data.get("DepartmentA1"), data.get("EmployeeA3")),
                    Arrays.asList(data.get("DepartmentB1"), data.get("EmployeeB1")),
                    Arrays.asList(data.get("DepartmentB1"), data.get("EmployeeB2")),
                    Arrays.asList(data.get("DepartmentB2"), data.get("EmployeeB3"))));
        results.put("CollectionPathExpression2", Arrays.asList(
                    Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA1")),
                    Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2")),
                    Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA3")),
                    Arrays.asList(data.get("EmployeeA2"), data.get("EmployeeA1")),
                    Arrays.asList(data.get("EmployeeA2"), data.get("EmployeeA2")),
                    Arrays.asList(data.get("EmployeeA2"), data.get("EmployeeA3")),
                    Arrays.asList(data.get("EmployeeA3"), data.get("EmployeeA1")),
                    Arrays.asList(data.get("EmployeeA3"), data.get("EmployeeA2")),
                    Arrays.asList(data.get("EmployeeA3"), data.get("EmployeeA3")),
                    Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB1")),
                    Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2")),
                    Arrays.asList(data.get("EmployeeB2"), data.get("EmployeeB1")),
                    Arrays.asList(data.get("EmployeeB2"), data.get("EmployeeB2")),
                    Arrays.asList(data.get("EmployeeB3"), data.get("EmployeeB3"))));
        results.put("CollectionPathExpression3", Arrays.asList(
                    Arrays.asList(data.get("CompanyA"), data.get("DepartmentA1"), data.get("EmployeeA1")),
                    Arrays.asList(data.get("CompanyA"), data.get("DepartmentA1"), data.get("EmployeeA2")),
                    Arrays.asList(data.get("CompanyA"), data.get("DepartmentA1"), data.get("EmployeeA3")),
                    Arrays.asList(data.get("CompanyB"), data.get("DepartmentB1"), data.get("EmployeeB1")),
                    Arrays.asList(data.get("CompanyB"), data.get("DepartmentB1"), data.get("EmployeeB2")),
                    Arrays.asList(data.get("CompanyB"), data.get("DepartmentB2"), data.get("EmployeeB3"))));
        results.put("CollectionPathExpression4", Arrays.asList(
                    Arrays.asList(data.get("CompanyA"), data.get("EmployeeA1")),
                    Arrays.asList(data.get("CompanyA"), data.get("EmployeeA2")),
                    Arrays.asList(data.get("CompanyA"), data.get("EmployeeA3")),
                    Arrays.asList(data.get("CompanyB"), data.get("EmployeeB1")),
                    Arrays.asList(data.get("CompanyB"), data.get("EmployeeB2")),
                    Arrays.asList(data.get("CompanyB"), data.get("EmployeeB3"))));
        results.put("CollectionPathExpression5", Arrays.asList(
                    Arrays.asList(data.get("CompanyA"), data.get("DepartmentA1")),
                    Arrays.asList(data.get("CompanyB"), data.get("DepartmentB1"))));
        results.put("CollectionPathExpression6", Arrays.asList(
                    Arrays.asList(data.get("DepartmentA1"), data.get("CompanyA"), data.get("DepartmentA1")),
                    Arrays.asList(data.get("DepartmentB1"), data.get("CompanyB"), data.get("DepartmentB1")),
                    Arrays.asList(data.get("DepartmentB1"), data.get("CompanyB"), data.get("DepartmentB2")),
                    Arrays.asList(data.get("DepartmentB2"), data.get("CompanyB"), data.get("DepartmentB1")),
                    Arrays.asList(data.get("DepartmentB2"), data.get("CompanyB"), data.get("DepartmentB2"))));
        results.put("CollectionPathExpression7", Arrays.asList(
                    Arrays.asList(data.get("EmployeeA1"), data.get("DepartmentA1"), data.get("CompanyA")),
                    Arrays.asList(data.get("EmployeeA2"), data.get("DepartmentA1"), data.get("CompanyA")),
                    Arrays.asList(data.get("EmployeeA3"), data.get("DepartmentA1"), data.get("CompanyA")),
                    Arrays.asList(data.get("EmployeeB1"), data.get("DepartmentB1"), data.get("CompanyB")),
                    Arrays.asList(data.get("EmployeeB2"), data.get("DepartmentB1"), data.get("CompanyB")),
                    Arrays.asList(data.get("EmployeeB3"), data.get("DepartmentB2"), data.get("CompanyB"))));
        results.put("SubclassCollection", Arrays.asList(
                    Arrays.asList(data.get("DepartmentA1"), data.get("EmployeeA1")),
                    Arrays.asList(data.get("DepartmentB1"), data.get("EmployeeB1")),
                    Arrays.asList(data.get("DepartmentB2"), data.get("EmployeeB3"))));
        results.put("SubclassCollection2", Arrays.asList(
                    Arrays.asList(data.get("DepartmentA1"), null),
                    Arrays.asList(data.get("DepartmentB1"), data.get("EmployeeB1")),
                    Arrays.asList(data.get("DepartmentB2"), null)));
        results.put("ObjectStoreBagsForObject", NO_RESULT);
        results.put("MultiColumnObjectInCollection", Arrays.asList(
                    Arrays.asList(data.get("CompanyA"), data.get("DepartmentA1"), data.get("CompanyA"), data.get("ContractorA")),
                    Arrays.asList(data.get("CompanyA"), data.get("DepartmentA1"), data.get("CompanyA"), data.get("ContractorB")),
                    Arrays.asList(data.get("CompanyB"), data.get("DepartmentB1"), data.get("CompanyB"), data.get("ContractorA")),
                    Arrays.asList(data.get("CompanyB"), data.get("DepartmentB1"), data.get("CompanyB"), data.get("ContractorB")),
                    Arrays.asList(data.get("CompanyB"), data.get("DepartmentB2"), data.get("CompanyB"), data.get("ContractorA")),
                    Arrays.asList(data.get("CompanyB"), data.get("DepartmentB2"), data.get("CompanyB"), data.get("ContractorB"))));
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
        if (results.get(type) instanceof Failure) {
            try {
                Results res = os.execute((Query) queries.get(type), 2, true, true, true);
                Iterator iter = res.iterator();
                while (iter.hasNext()) {
                    iter.next();
                }
                fail(type + " was expected to fail");
            } catch (Exception e) {
                assertEquals(type + " was expected to produce a particular exception", results.get(type), new Failure(e));
            }
        } else {
            Results res = os.execute((Query)queries.get(type), 2, true, true, true);
            Iterator resIter = new ReallyFlatIterator(new ResultsFlatOuterJoinsImpl((List<ResultsRow>) ((List) res), (Query) queries.get(type)).iterator());
            List newRes = new ArrayList();
            while (resIter.hasNext()) {
                newRes.add(resIter.next());
            }
            List expected = (List) results.get(type);
            if ((expected != null) && (!expected.equals(newRes))) {
                Set a = new HashSet(expected);
                Set b = new HashSet(newRes);
                List la = resToNames(expected);
                List lb = resToNames(newRes);
                if (a.equals(b)) {
                    assertEquals(type + " has failed - wrong order", la, lb);
                }
                fail(type + " has failed. Expected " + la + " but was " + lb);
            }
            //assertEquals(type + " has failed", results.get(type), newRes);
        }
    }


    public void testResults() throws Exception {
        // Don't
    }

    public void testCEOWhenSearchingForManager() throws Exception {
        // Don't
    }

    public void testLazyCollection() throws Exception {
        // Don't
    }

    public void testLazyCollectionMtoN() throws Exception {
        // Don't
    }

    public void testDataTypes() throws Exception {
        // Don't
    }

    public void testGetObjectMultipleTimes() throws Exception {
        // Don't
    }

    public void testSimpleObjects() throws Exception {
        // Don't
    }
}
