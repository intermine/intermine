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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.objectstore.Failure;
import org.intermine.objectstore.ObjectStoreAbstractImplTestCase;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

public class ResultsFlatOuterJoinsImplTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        osai = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        os = osai;
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
        setUpResults();
    }

    public ResultsFlatOuterJoinsImplTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ResultsFlatOuterJoinsImplTest.class);
    }

    public static void setUpResults() throws Exception {
        ObjectStoreAbstractImplTestCase.setUpResults();
        Map newResults = new LinkedHashMap();
        for (Map.Entry<String, Object> resultsEntry : results.entrySet()) {
            String testName = (String) resultsEntry.getKey();
            Object testValue = resultsEntry.getValue();
            if (testValue instanceof List) {
                List<List> testResults = (List<List>) testValue;
                List<List<List<MultiRowFirstValue>>> newTestResults = new ArrayList();
                for (List row : testResults) {
                    List<MultiRowFirstValue> newRow = new ArrayList();
                    for (Object o : row) {
                        newRow.add(new MultiRowFirstValue(o, 1));
                    }
                    newTestResults.add(Collections.singletonList(newRow));
                }
                newResults.put(testName, newTestResults);
            } else {
                newResults.put(testName, testValue);
            }
        }
        results = newResults;

        MultiRowFirstValue v1 = new MultiRowFirstValue(data.get("DepartmentA1"), 3);
        MultiRowFirstValue v2 = new MultiRowFirstValue(data.get("DepartmentB1"), 2);
        results.put("CollectionPathExpression", Arrays.asList(Arrays.asList(
                        Arrays.asList(v1, new MultiRowFirstValue(data.get("EmployeeA1"), 1)),
                        Arrays.asList(v1.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA2"), 1)),
                        Arrays.asList(v1.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA3"), 1))),
                    Arrays.asList(
                        Arrays.asList(v2, new MultiRowFirstValue(data.get("EmployeeB1"), 1)),
                        Arrays.asList(v2.getMrlv(), new MultiRowFirstValue(data.get("EmployeeB2"), 1))),
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("DepartmentB2"), 1), new MultiRowFirstValue(data.get("EmployeeB3"), 1)))));
        v1 = new MultiRowFirstValue(data.get("EmployeeA1"), 3);
        v2 = new MultiRowFirstValue(data.get("EmployeeA2"), 3);
        MultiRowFirstValue v3 = new MultiRowFirstValue(data.get("EmployeeA3"), 3);
        MultiRowFirstValue v4 = new MultiRowFirstValue(data.get("EmployeeB1"), 2);
        MultiRowFirstValue v5 = new MultiRowFirstValue(data.get("EmployeeB2"), 2);
        results.put("CollectionPathExpression2", Arrays.asList(Arrays.asList(
                        Arrays.asList(v1, new MultiRowFirstValue(data.get("EmployeeA1"), 1)),
                        Arrays.asList(v1.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA2"), 1)),
                        Arrays.asList(v1.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA3"), 1))),
                    Arrays.asList(
                        Arrays.asList(v2, new MultiRowFirstValue(data.get("EmployeeA1"), 1)),
                        Arrays.asList(v2.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA2"), 1)),
                        Arrays.asList(v2.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA3"), 1))),
                    Arrays.asList(
                        Arrays.asList(v3, new MultiRowFirstValue(data.get("EmployeeA1"), 1)),
                        Arrays.asList(v3.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA2"), 1)),
                        Arrays.asList(v3.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA3"), 1))),
                    Arrays.asList(
                        Arrays.asList(v4, new MultiRowFirstValue(data.get("EmployeeB1"), 1)),
                        Arrays.asList(v4.getMrlv(), new MultiRowFirstValue(data.get("EmployeeB2"), 1))),
                    Arrays.asList(
                        Arrays.asList(v5, new MultiRowFirstValue(data.get("EmployeeB1"), 1)),
                        Arrays.asList(v5.getMrlv(), new MultiRowFirstValue(data.get("EmployeeB2"), 1))),
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("EmployeeB3"), 1), new MultiRowFirstValue(data.get("EmployeeB3"), 1)))));

        v1 = new MultiRowFirstValue(data.get("CompanyA"), 3);
        v2 = new MultiRowFirstValue(data.get("DepartmentA1"), 3);
        v3 = new MultiRowFirstValue(data.get("CompanyB"), 3);
        v4 = new MultiRowFirstValue(data.get("DepartmentB1"), 2);
        results.put("CollectionPathExpression3", Arrays.asList(Arrays.asList(
                        Arrays.asList(v1, v2, new MultiRowFirstValue(data.get("EmployeeA1"), 1)),
                        Arrays.asList(v1.getMrlv(), v2.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA2"), 1)),
                        Arrays.asList(v1.getMrlv(), v2.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA3"), 1))),
                    Arrays.asList(
                        Arrays.asList(v3, v4, new MultiRowFirstValue(data.get("EmployeeB1"), 1)),
                        Arrays.asList(v3.getMrlv(), v4.getMrlv(), new MultiRowFirstValue(data.get("EmployeeB2"), 1)),
                        Arrays.asList(v3.getMrlv(), new MultiRowFirstValue(data.get("DepartmentB2"), 1), new MultiRowFirstValue(data.get("EmployeeB3"), 1)))));
        results.put("CollectionPathExpression4", Arrays.asList(Arrays.asList(
                        Arrays.asList(v1, new MultiRowFirstValue(data.get("EmployeeA1"), 1)),
                        Arrays.asList(v1.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA2"), 1)),
                        Arrays.asList(v1.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA3"), 1))),
                    Arrays.asList(
                        Arrays.asList(v3, new MultiRowFirstValue(data.get("EmployeeB1"), 1)),
                        Arrays.asList(v3.getMrlv(), new MultiRowFirstValue(data.get("EmployeeB2"), 1)),
                        Arrays.asList(v3.getMrlv(), new MultiRowFirstValue(data.get("EmployeeB3"), 1)))));
        results.put("CollectionPathExpression5", Arrays.asList(
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("CompanyA"), 1), new MultiRowFirstValue(data.get("DepartmentA1"), 1))),
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("CompanyB"), 1), new MultiRowFirstValue(data.get("DepartmentB1"), 1)))));
        v1 = new MultiRowFirstValue(data.get("DepartmentB1"), 2);
        v2 = new MultiRowFirstValue(data.get("DepartmentB2"), 2);
        v3 = new MultiRowFirstValue(data.get("CompanyB"), 2);
        results.put("CollectionPathExpression6", Arrays.asList(
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("DepartmentA1"), 1), new MultiRowFirstValue(data.get("CompanyA"), 1), new MultiRowFirstValue(data.get("DepartmentA1"), 1))),
                    Arrays.asList(
                        Arrays.asList(v1, v3, new MultiRowFirstValue(data.get("DepartmentB1"), 1)),
                        Arrays.asList(v1.getMrlv(), v3.getMrlv(), new MultiRowFirstValue(data.get("DepartmentB2"), 1))),
                    Arrays.asList(
                        Arrays.asList(v2, v3, new MultiRowFirstValue(data.get("DepartmentB1"), 1)),
                        Arrays.asList(v2.getMrlv(), v3.getMrlv(), new MultiRowFirstValue(data.get("DepartmentB2"), 1)))));
        results.put("CollectionPathExpression7", Arrays.asList(
                    Arrays.asList(Arrays.asList(new MultiRowFirstValue(data.get("EmployeeA1"), 1), new MultiRowFirstValue(data.get("DepartmentA1"), 1), new MultiRowFirstValue(data.get("CompanyA"), 1))),
                    Arrays.asList(Arrays.asList(new MultiRowFirstValue(data.get("EmployeeA2"), 1), new MultiRowFirstValue(data.get("DepartmentA1"), 1), new MultiRowFirstValue(data.get("CompanyA"), 1))),
                    Arrays.asList(Arrays.asList(new MultiRowFirstValue(data.get("EmployeeA3"), 1), new MultiRowFirstValue(data.get("DepartmentA1"), 1), new MultiRowFirstValue(data.get("CompanyA"), 1))),
                    Arrays.asList(Arrays.asList(new MultiRowFirstValue(data.get("EmployeeB1"), 1), new MultiRowFirstValue(data.get("DepartmentB1"), 1), new MultiRowFirstValue(data.get("CompanyB"), 1))),
                    Arrays.asList(Arrays.asList(new MultiRowFirstValue(data.get("EmployeeB2"), 1), new MultiRowFirstValue(data.get("DepartmentB1"), 1), new MultiRowFirstValue(data.get("CompanyB"), 1))),
                    Arrays.asList(Arrays.asList(new MultiRowFirstValue(data.get("EmployeeB3"), 1), new MultiRowFirstValue(data.get("DepartmentB2"), 1), new MultiRowFirstValue(data.get("CompanyB"), 1)))));
        results.put("SubclassCollection", Arrays.asList(
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("DepartmentA1"), 1), new MultiRowFirstValue(data.get("EmployeeA1"), 1))),
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("DepartmentB1"), 1), new MultiRowFirstValue(data.get("EmployeeB1"), 1))),
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("DepartmentB2"), 1), new MultiRowFirstValue(data.get("EmployeeB3"), 1)))));
        results.put("SubclassCollection2", Arrays.asList(
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("DepartmentA1"), 1), null)),
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("DepartmentB1"), 1), new MultiRowFirstValue(data.get("EmployeeB1"), 1))),
                    Arrays.asList(
                        Arrays.asList(new MultiRowFirstValue(data.get("DepartmentB2"), 1), null))));
        results.put("ObjectStoreBagsForObject", NO_RESULT);
        v1 = new MultiRowFirstValue(data.get("CompanyA"), 2);
        v2 = new MultiRowFirstValue(data.get("DepartmentA1"), 2);
        v3 = new MultiRowFirstValue(data.get("CompanyB"), 4);
        v4 = new MultiRowFirstValue(data.get("DepartmentB1"), 2);
        v5 = new MultiRowFirstValue(data.get("DepartmentB2"), 2);
        MultiRowFirstValue v6 = new MultiRowFirstValue(data.get("CompanyB"), 2);
        results.put("MultiColumnObjectInCollection", Arrays.asList(
                    Arrays.asList(
                        Arrays.asList(v1, v2, v1, new MultiRowFirstValue(data.get("ContractorA"), 1)),
                        Arrays.asList(v1.getMrlv(), v2.getMrlv(), v1.getMrlv(), new MultiRowFirstValue(data.get("ContractorB"), 1))),
                    Arrays.asList(
                        Arrays.asList(v3, v4, v6, new MultiRowFirstValue(data.get("ContractorA"), 1)),
                        Arrays.asList(v3.getMrlv(), v4.getMrlv(), v6.getMrlv(), new MultiRowFirstValue(data.get("ContractorB"), 1)),
                        Arrays.asList(v3.getMrlv(), v5, v6, new MultiRowFirstValue(data.get("ContractorA"), 1)),
                        Arrays.asList(v3.getMrlv(), v5.getMrlv(), v6.getMrlv(), new MultiRowFirstValue(data.get("ContractorB"), 1)))));

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
            List newRes = new ResultsFlatOuterJoinsImpl((List<ResultsRow>) ((List) res), (Query) queries.get(type));
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

    public Object objectToName(Object o) throws Exception {
        if (o instanceof MultiRowFirstValue) {
            MultiRowFirstValue mrfv = (MultiRowFirstValue) o;
            return "MRFV(" + objectToName(mrfv.getValue()) + ", " + mrfv.getRowspan() + ")";
        } else if (o instanceof MultiRowLaterValue) {
            return "MRLV(" + objectToName(((MultiRowLaterValue) o).getValue()) + ")";
        } else {
            return super.objectToName(o);
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
