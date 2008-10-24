package org.intermine.objectstore.flatouterjoins;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.objectstore.ObjectStoreAbstractImplTestCase;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.Lazy;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

public class ObjectStoreFlatOuterJoinsImplTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        osai = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        os = new ObjectStoreFlatOuterJoinsImpl(osai);
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
        setUpResults();
    }

    public ObjectStoreFlatOuterJoinsImplTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreFlatOuterJoinsImplTest.class);
    }

    public static void setUpResults() throws Exception {
        ObjectStoreAbstractImplTestCase.setUpResults();
/*        Map newResults = new LinkedHashMap();
        for (Map.Entry resultsEntry : ((Set<Map.Entry>) results.entrySet())) {
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
*/
        MultiRowFirstValue v1 = new MultiRowFirstValue(data.get("DepartmentA1"), 3);
        MultiRowFirstValue v2 = new MultiRowFirstValue(data.get("DepartmentB1"), 2);
        results.put("CollectionPathExpression", Arrays.asList(Arrays.asList(
                        Arrays.asList(v1, new MultiRowFirstValue(data.get("EmployeeA1"), 1)),
                        Arrays.asList(v1.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA2"), 1)),
                        Arrays.asList(v1.getMrlv(), new MultiRowFirstValue(data.get("EmployeeA3"), 1))),
                    Arrays.asList(
                        Arrays.asList(v2, new MultiRowFirstValue(data.get("EmployeeB1"), 1)),
                        Arrays.asList(v2.getMrlv(), new MultiRowFirstValue(data.get("EmployeeB2"), 1))),
                    Arrays.asList(data.get("DepartmentB2"), data.get("EmployeeB3"))));
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
                    Arrays.asList(data.get("EmployeeB3"), data.get("EmployeeB3"))));

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
                    Arrays.asList(data.get("CompanyA"), data.get("DepartmentA1")),
                    Arrays.asList(data.get("CompanyB"), data.get("DepartmentB1"))));
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
