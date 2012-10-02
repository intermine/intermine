package org.intermine.webservice.server.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.intermine.api.query.MainHelper;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.Types;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.dummy.DummyResults;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IteratorIterable;
import org.json.JSONObject;

/**
 * TODO: tests are failing!!!
 * Tests for the JSONResultsIterator class
 *
 * @author Alexis Kalderimis
 */

public class JSONResultsIteratorTest extends TestCase {

    private ObjectStoreDummyImpl os;
    private Company wernhamHogg;
    private CEO jennifer;
    private Manager david;
    private Manager taffy;
    private Employee tim;
    private Employee gareth;
    private Employee dawn;
    private Employee keith;
    private Employee lee;
    private Employee alex;
    private Department sales;
    private Department reception;
    private Department accounts;
    private Department distribution;
    private Address address;
    private Contractor rowan;
    private Contractor ray;
    private Contractor jude;
    private Department swindon;
    private Manager neil;
    private Employee rachel;
    private Employee trudy;
    private Company bms;
    private Address address2;


    @Override
    protected void setUp() {
        os = new ObjectStoreDummyImpl();

        wernhamHogg = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        wernhamHogg.setId(new Integer(1));
        wernhamHogg.setName("Wernham-Hogg");
        wernhamHogg.setVatNumber(101);

        jennifer = new CEO();
        jennifer.setId(new Integer(2));
        jennifer.setName("Jennifer Taylor-Clarke");
        jennifer.setAge(42);

        david = new Manager();
        david.setId(new Integer(3));
        david.setName("David Brent");
        david.setAge(39);

        taffy = new Manager();
        taffy.setId(new Integer(4));
        taffy.setName("Glynn");
        taffy.setAge(38);

        tim = new Employee();
        tim.setId(new Integer(5));
        tim.setName("Tim Canterbury");
        tim.setAge(30);

        gareth = new Employee();
        gareth.setId(new Integer(6));
        gareth.setName("Gareth Keenan");
        gareth.setAge(32);

        dawn = new Employee();
        dawn.setId(new Integer(7));
        dawn.setName("Dawn Tinsley");
        dawn.setAge(26);

        keith = new Employee();
        keith.setId(new Integer(8));
        keith.setName("Keith Bishop");
        keith.setAge(41);

        lee = new Employee();
        lee.setId(new Integer(9));
        lee.setName("Lee");
        lee.setAge(28);

        alex = new Employee();
        alex.setId(new Integer(10));
        alex.setName("Alex");
        alex.setAge(24);

        sales = new Department();
        sales.setId(new Integer(11));
        sales.setName("Sales");

        accounts = new Department();
        accounts.setId(new Integer(12));
        accounts.setName("Accounts");

        distribution = new Department();
        distribution.setId(new Integer(13));
        distribution.setName("Warehouse");

        reception = new Department();
        reception.setId(new Integer(14));
        reception.setName("Reception");

        address = new Address();
        address.setId(new Integer(15));
        address.setAddress("42 Friendly St, Betjeman Trading Estate, Slough");

        rowan = new Contractor();
        rowan.setId(new Integer(16));
        rowan.setName("Rowan");

        ray = new Contractor();
        ray.setId(new Integer(17));
        ray.setName("Ray");

        jude = new Contractor();
        jude.setId(new Integer(18));
        jude.setName("Jude");

        swindon = new Department();
        swindon.setId(new Integer(19));
        swindon.setName("Swindon");

        neil = new Manager();
        neil.setId(new Integer(20));
        neil.setName("Neil Godwin");
        neil.setAge(35);

        rachel = new Employee();
        rachel.setId(new Integer(21));
        rachel.setName("Rachel");
        rachel.setAge(34);

        trudy = new Employee();
        trudy.setId(new Integer(22));
        trudy.setName("Trudy");
        trudy.setAge(25);

        bms = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        bms.setId(new Integer(23));
        bms.setName("Business Management Seminars");
        bms.setVatNumber(102);

        address2 = new Address();
        address2.setId(new Integer(24));
        address2.setAddress("19 West Oxford St, Reading");

    }

    private final Model model = Model.getInstanceByName("testmodel");

    public JSONResultsIteratorTest(String arg) {
        super(arg);
    }

    public void testSingleSimpleObject() throws Exception {
        os.setResultsSize(1);

        String jsonString = "{ 'class': 'Manager', 'objectId': 3, 'age': 39, 'name': 'David Brent' }";
        JSONObject expected = new JSONObject(jsonString);

        ResultsRow row = new ResultsRow();
        row.add(david);

        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.age");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(1, got.size());

        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

    }
    
    private ExportResultsIterator getIterator(PathQuery pq) throws ObjectStoreException {
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 5, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);

        ExportResultsIterator iter = new ExportResultsIterator(pq, q, results, pathToQueryNode);
        return iter;
    }

    public void testMultipleSimpleObjects() throws Exception {
        os.setResultsSize(6);

        List<String> jsonStrings = new ArrayList<String>();
        jsonStrings.add("{ 'class':'Employee', 'objectId':5, 'age':30, 'name': 'Tim Canterbury' }");
        jsonStrings.add("{ 'class':'Employee', 'objectId':6, 'age':32, 'name': 'Gareth Keenan' }");
        jsonStrings.add("{ 'class':'Employee', 'objectId':7, 'age':26, 'name': 'Dawn Tinsley' }");
        jsonStrings.add("{ 'class':'Employee', 'objectId':8, 'age':41, 'name': 'Keith Bishop' }");
        jsonStrings.add("{ 'class':'Employee', 'objectId':9, 'age':28, 'name': 'Lee' }");
        jsonStrings.add("{ 'class':'Manager',  'objectId':3, 'age':39, 'name': 'David Brent' }");

        ResultsRow row1 = new ResultsRow();
        row1.add(tim);
        ResultsRow row2 = new ResultsRow();
        row2.add(gareth);
        ResultsRow row3 = new ResultsRow();
        row3.add(dawn);
        ResultsRow row4 = new ResultsRow();
        row4.add(keith);
        ResultsRow row5 = new ResultsRow();
        row5.add(lee);
        ResultsRow row6 = new ResultsRow();
        row6.add(david);

        os.addRow(row1);
        os.addRow(row2);
        os.addRow(row3);
        os.addRow(row4);
        os.addRow(row5);
        os.addRow(row6);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Employee.name", "Employee.age", "Employee.id");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(6, got.size());
        for (int i = 0; i < jsonStrings.size(); i++) {
            JSONObject jo = new JSONObject(jsonStrings.get(i));
            assertEquals(null, JSONObjTester.getProblemsComparing(jo, got.get(i)));
        }

    }

    public void testSingleObjectWithNestedCollections() throws Exception {
        os.setResultsSize(1);

        ResultsRow row = new ResultsRow();
        row.add(wernhamHogg);
        List sub1 = new ArrayList();
        ResultsRow subRow1 = new ResultsRow();
        subRow1.add(sales);
        List sub2 = new ArrayList();
        ResultsRow subRow2 = new ResultsRow();
        subRow2.add(tim);
        sub2.add(subRow2);
        subRow2 = new ResultsRow();
        subRow2.add(gareth);
        sub2.add(subRow2);
        subRow2 = new ResultsRow();
        subRow2.add(david);
        sub2.add(subRow2);
        subRow1.add(sub2);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(distribution);
        sub2 = new ArrayList();
        subRow2 = new ResultsRow();
        subRow2.add(lee);
        sub2.add(subRow2);
        subRow2 = new ResultsRow();
        subRow2.add(alex);
        sub2.add(subRow2);
        subRow1.add(sub2);
        sub1.add(subRow1);
        row.add(sub1);
        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber", "Company.departments.name", "Company.departments.employees.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(got.size(), 1);

        String jsonString = "{" +
        "    'objectId'    : 1," +
        "    'class'       : 'Company'," +
        "    'name'           : 'Wernham-Hogg'," +
        "    'vatNumber'   : 101," +
        "    'departments' : [" +
        "        {" +
        "            'objectId'  : 11," +
        "            'class'     : 'Department'," +
        "            'name'      : 'Sales'," +
        "            'employees' : [" +
        "                { " +
        "                    'objectId' : 5," +
        "                    'class'    : 'Employee'," +
        "                    'name'     : 'Tim Canterbury'" +
        "                }, " +
        "                { " +
        "                    'objectId' : 6," +
        "                    'class'       : 'Employee'," +
        "                    'name'        : 'Gareth Keenan'" +
        "                }," +
        "                {" +
        "                    'objectId': 3," +
        "                    'class':    'Manager'," +
        "                    'name':     'David Brent'" +
        "                }" +
        "            ]" +
        "        }," +
        "        {" +
        "            'objectId'  : 13," +
        "            'class'     : 'Department'," +
        "            'name'      : 'Warehouse', " +
        "            'employees' : [" +
        "                {" +
        "                    'objectId' : 9," +
        "                    'class'    : 'Employee'," +
        "                    'name'     : 'Lee'" +
        "                }," +
        "                {" +
        "                    'objectId' : 10," +
        "                    'class'    : 'Employee'," +
        "                    'name'     : 'Alex'" +
        "                }" +
        "            ]" +
        "        }" +
        "    ]" +
        "}";


        JSONObject expected = new JSONObject(jsonString);
        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

    }
    public void testSingleObjectWithNestedCollectionsAndMultipleAttributes() throws Exception {
        os.setResultsSize(1);

        ResultsRow row = new ResultsRow();
        row.add(wernhamHogg);
        List sub1 = new ArrayList();
        ResultsRow subRow1 = new ResultsRow();
        subRow1.add(sales);
        List sub2 = new ArrayList();
        ResultsRow subRow2 = new ResultsRow();
        subRow2.add(tim);
        sub2.add(subRow2);
        subRow2 = new ResultsRow();
        subRow2.add(gareth);
        sub2.add(subRow2);
        subRow1.add(sub2);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(distribution);
        sub2 = new ArrayList();
        subRow2 = new ResultsRow();
        subRow2.add(lee);
        sub2.add(subRow2);
        subRow2 = new ResultsRow();
        subRow2.add(alex);
        sub2.add(subRow2);
        subRow1.add(sub2);
        sub1.add(subRow1);
        row.add(sub1);
        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber",
                        "Company.departments.name",
                            "Company.departments.employees.name", "Company.departments.employees.age");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(got.size(), 1);

        String jsonString = "{" +
        "    'objectId'    : 1," +
        "    'class'       : 'Company'," +
        "    'name'           : 'Wernham-Hogg'," +
        "    'vatNumber'   : 101," +
        "    'departments' : [" +
        "        {" +
        "            'objectId'  : 11," +
        "            'class'     : 'Department'," +
        "            'name'      : 'Sales'," +
        "            'employees' : [" +
        "                { " +
        "                    'objectId' : 5," +
        "                    'class'    : 'Employee'," +
        "                    'name'     : 'Tim Canterbury'," +
        "                    'age'       : 30" +
        "                }, " +
        "                { " +
        "                    'objectId' : 6," +
        "                    'class'       : 'Employee'," +
        "                    'name'        : 'Gareth Keenan'," +
        "                    'age'      : 32" +
        "                }" +
        "            ]" +
        "        }," +
        "        {" +
        "            'objectId'  : 13," +
        "            'class'     : 'Department'," +
        "            'name'      : 'Warehouse', " +
        "            'employees' : [" +
        "                {" +
        "                    'objectId' : 9," +
        "                    'class'    : 'Employee'," +
        "                    'name'     : 'Lee'," +
        "                    'age'      : 28" +
        "                }," +
        "                {" +
        "                    'objectId' : 10," +
        "                    'class'    : 'Employee'," +
        "                    'name'     : 'Alex'," +
        "                    'age'      : 24" +
        "                }" +
        "            ]" +
        "        }" +
        "    ]" +
        "}";


        JSONObject expected = new JSONObject(jsonString);
        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

    }


    // Attributes on references should precede references on references
    // The dummy attribute "id" can be used without populating the object with
    // unwanted stuff.
    public void testSingleObjectWithTrailOfReferences() throws Exception {
        os.setResultsSize(1);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.company.id", "Department.company.CEO.name", "Department.company.CEO.address.address");

        String jsonString = "{" +
                "                'class'    : 'Department'," +
                "                'objectId' : 11," +
                "                'name'     : 'Sales'," +
                "                'company'  : {" +
                "                    'class'    : 'Company'," +
                "                    'objectId' : 1," +
                "                    'CEO'      : {" +
                "                        'class'    : 'CEO'," +
                "                        'objectId' : 2," +
                "                        'name'     : 'Jennifer Taylor-Clarke'," +
                "                        'address'  : {" +
                "                            'class'    : 'Address'," +
                "                            'objectId' : 15," +
                "                            'address'  : '42 Friendly St, Betjeman Trading Estate, Slough'" +
                "                        }" +
                "                    }" +
                "                }" +
                "            }";
        JSONObject expected = new JSONObject(jsonString);

        ResultsRow row = new ResultsRow();
        row.add(sales);
        row.add(wernhamHogg);
        row.add(jennifer);
        row.add(address);
        os.addRow(row);

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(got.size(), 1);

        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

    }

    // Attributes on references should precede references on references
    // Not doing so causes an error
    public void testBadReferenceTrail() throws Exception {
        os.setResultsSize(1);

        ResultsRow row = new ResultsRow();
        row.add(sales);
        row.add(jennifer);
        row.add(address);
        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.company.CEO.name", "Department.company.CEO.address.address");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        try {
            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
            fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals("This node is not fully initialised: it has no objectId", e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }

    }

    // Attributes on references should precede references on references
    public void testSingleObjectWithACollection() throws Exception {
        os.setResultsSize(2);

        ResultsRow row = new ResultsRow();
        row.add(david);
        row.add(sales);
        row.add(tim);
        os.addRow(row);
        ResultsRow row2 = new ResultsRow();
        row2.add(david);
        row2.add(sales);
        row2.add(gareth);
        os.addRow(row2);

        String jsonString =
        "{" +
        "    'objectId'    : 3," +
        "    'class'       : 'Manager'," +
        "    'name'           : 'David Brent'," +
        "    'age'         : 39," +
        "    'department' : {" +
        "            'objectId'  : 11," +
        "            'class'     : 'Department'," +
        "            'name'      : 'Sales'," +
        "            'employees' : [" +
        "                { " +
        "                    'objectId' : 5," +
        "                    'class'    : 'Employee'," +
        "                    'name'     : 'Tim Canterbury'," +
        "                    'age'       : 30" +
        "                }, " +
        "                { " +
        "                    'objectId' : 6," +
        "                    'class'       : 'Employee'," +
        "                    'name'        : 'Gareth Keenan'," +
        "                    'age'      : 32" +
        "                }" +
        "            ]" +
        "    }" +
        "}";

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.age", "Manager.department.name", "Manager.department.employees.name", "Manager.department.employees.age");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(got.size(), 1);

        JSONObject expected = new JSONObject(jsonString);
        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

    }

    // Attributes on references should precede references on references
    // Not doing so causes an error
    public void testBadCollectionTrail() throws Exception {
        os.setResultsSize(2);

        ResultsRow row = new ResultsRow();
        row.add(david);
        row.add(tim);
        os.addRow(row);
        ResultsRow row2 = new ResultsRow();
        row2.add(david);
        row2.add(gareth);
        os.addRow(row2);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.department.employees.name");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        try {
            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
            fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals("This node is not properly initialised (it doesn't have an objectId) - is the view in the right order?", e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }

    }

    public void testHeadlessQuery() throws Exception {
        os.setResultsSize(2);

        ResultsRow row = new ResultsRow();
        row.add(tim);
        os.addRow(row);
        ResultsRow row2 = new ResultsRow();
        row2.add(gareth);
        os.addRow(row2);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.department.employees.name", "Manager.department.employees.age");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        try {
            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
            fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals("This node is not properly initialised (it doesn't have an objectId) - is the view in the right order?", e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }

    }

    public void testParallelCollectionOuterJoin() throws Exception {
        os.setResultsSize(1);

        String jsonString =
                "{" +
                "    'class' : 'Company'," +
                "    'objectId' : 1," +
                "    'name'     : 'Wernham-Hogg'," +
                "    'vatNumber' : 101," +
                "    departments : [" +
                "        {"    +
                "           'class' : 'Department'," +
                "            'objectId' : 11," +
                "            'name'     : 'Sales'" +
                "        }," +
                "        {" +
                "           'class' : 'Department'," +
                "            'objectId' : 12," +
                "            'name' : 'Accounts'" +
                "        }" +
                "    ]," +
                "    contractors : [" +
                "        {"    +
                "           'class' : 'Contractor'," +
                "            'objectId' : 16," +
                "            'name'     : 'Rowan'" +
                "        }," +
                "        {" +
                "           'class' : 'Contractor'," +
                "            'objectId' : 17," +
                "            'name' : 'Ray'" +
                "        }," +
                "        {" +
                "           'class' : 'Contractor'," +
                "            'objectId' : 18," +
                "            'name' : 'Jude'" +
                "        }" +
                "    ]" +
                "}";

        ResultsRow row = new ResultsRow();
        row.add(wernhamHogg);
        List sub1 = new ArrayList();
        ResultsRow subRow1 = new ResultsRow();
        subRow1.add(sales);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(accounts);
        sub1.add(subRow1);
        row.add(sub1);
        sub1 = new ArrayList();
        subRow1 = new ResultsRow();
        subRow1.add(rowan);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(ray);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(jude);
        sub1.add(subRow1);
        row.add(sub1);
        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber",
                        "Company.departments.name",
                        "Company.contractors.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.contractors", OuterJoinStatus.OUTER);

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        JSONObject expected = new JSONObject(jsonString);

        assertEquals(got.size(), 1);

        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

    }

    // should handle inner joins as well as outer joins
    public void testParallelCollectionInnerJoin() throws Exception {
        os.setResultsSize(4);

        String jsonString =
                "{" +
                "    'class' : 'Company'," +
                "    'objectId' : 1," +
                "    'name'     : 'Wernham-Hogg'," +
                "    'vatNumber' : 101," +
                "    departments : [" +
                "        {"    +
                "           'class' : 'Department'," +
                "            'objectId' : 11," +
                "            'name'     : 'Sales'" +
                "        }," +
                "        {" +
                "           'class' : 'Department'," +
                "            'objectId' : 12," +
                "            'name' : 'Accounts'" +
                "        }" +
                "    ]," +
                "    contractors : [" +
                "        {"    +
                "           'class' : 'Contractor'," +
                "            'objectId' : 16," +
                "            'name'     : 'Rowan'" +
                "        }," +
                "        {" +
                "           'class' : 'Contractor'," +
                "            'objectId' : 17," +
                "            'name' : 'Ray'" +
                "        }," +
                "        {" +
                "           'class' : 'Contractor'," +
                "            'objectId' : 18," +
                "            'name' : 'Jude'" +
                "        }" +
                "    ]" +
                "}";

        ResultsRow row1 = new ResultsRow();
        row1.add(wernhamHogg);
        row1.add(sales);
        row1.add(null);
        os.addRow(row1);

        ResultsRow row2 = new ResultsRow();
        row2.add(wernhamHogg);
        row2.add(accounts);
        row2.add(rowan);
        os.addRow(row2);

        ResultsRow row3 = new ResultsRow();
        row3.add(wernhamHogg);
        row3.add(accounts);
        row3.add(ray);
        os.addRow(row3);

        ResultsRow row4 = new ResultsRow();
        row4.add(wernhamHogg);
        row4.add(accounts);
        row4.add(jude);
        os.addRow(row4);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber",
                        "Company.departments.name",
                        "Company.contractors.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.INNER);
        pq.setOuterJoinStatus("Company.contractors", OuterJoinStatus.INNER);

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        JSONObject expected = new JSONObject(jsonString);

        assertEquals(got.size(), 1);

        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

    }

    // Should be ok with a result set of size 0, and produce no objects
    public void testZeroResults() throws ObjectStoreException {

        PathQuery pq = new PathQuery(model);
        pq.addViews(
                "Employee.age", "Employee.name",
                "Employee.department.name",
                "Employee.department.manager.name",
                "Employee.department.manager.address.address",
                "Employee.department.manager.department.name",
                "Employee.department.company.id",
                "Employee.department.company.contractors.id",
                "Employee.department.company.contractors.companys.name",
                "Employee.department.company.contractors.companys.vatNumber",
                "Employee.department.manager.department.company.name",
                "Employee.department.company.contractors.companys.address.address");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        assert(!jsonIter.hasNext());

    }

    public void testReferencesOnCollection() throws Exception {
        os.setResultsSize(4);

        String jsonString =
                "{" +
                "    class      : 'Employee'," +
                "    objectId   : 5," +
                "    name       : 'Tim Canterbury'," +
                "    age        : 30," +
                "    department : {" +
                "        class : 'Department'," +
                "        objectId : 11," +
                "        name : 'Sales'," +
                "        manager : {" +
                "            class : 'Manager'," +
                "            objectId: 3," +
                "            name: 'David Brent'," +
                "            address : {" +
                "                objectId: 15," +
                "                class : 'Address'," +
                "                address: '42 Friendly St, Betjeman Trading Estate, Slough'" +
                "            }," +
                "            department : {" +
                "                class : 'Department'," +
                "                objectId : 11," +
                "                name : 'Sales'," +
                "                company: {" +
                "                    class: 'Company'," +
                "                    objectId: 1," +
                "                    name: 'Wernham-Hogg'" +
                "                }" +
                "            }" +
                "        }," +
                "        company : {" +
                "            class : 'Company'," +
                "            objectId: 1," +
                "            contractors : [" +
                "                {" +
                "                    class: 'Contractor'," +
                "                    objectId: 17," +
                "                    companys: [" +
                "                        {" +
                "                            class: 'Company'," +
                "                            objectId: 23," +
                "                            name: 'Business Management Seminars'," +
                "                            vatNumber: 102," +
                "                            address: {" +
                "                                class: 'Address'," +
                "                                objectId: 24," +
                "                                address: '19 West Oxford St, Reading'" +
                "                            }" +
                "                        }" +
                "                    ]" +
                "                }" +
                "            ]" +
                "        }" +
                "   }" +
                "}";

        ResultsRow row1 = new ResultsRow();
        row1.add(tim);
        row1.add(sales);
        row1.add(david);
        row1.add(address);
        row1.add(sales);
        row1.add(wernhamHogg);
        row1.add(ray);
        row1.add(bms);
        row1.add(wernhamHogg);
        row1.add(address2);
        os.addRow(row1);

        PathQuery pq = new PathQuery(model);
        pq.addViews(
                "Employee.age", "Employee.name",
                "Employee.department.name",
                "Employee.department.manager.name",
                "Employee.department.manager.address.address",
                "Employee.department.manager.department.name",
                "Employee.department.company.id",
                "Employee.department.company.contractors.id",
                "Employee.department.company.contractors.companys.name",
                "Employee.department.company.contractors.companys.vatNumber",
                "Employee.department.manager.department.company.name",
                "Employee.department.company.contractors.companys.address.address");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        JSONObject expected = new JSONObject(jsonString);

        assertEquals(1, got.size());

        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

    }


    public void testHeadInWrongPlace() throws Exception {
        os.setResultsSize(2);

        ResultsRow row1 = new ResultsRow();
        row1.add(tim);
        row1.add(sales);
        os.addRow(row1);
        ResultsRow row2 = new ResultsRow();
        row2.add(gareth);
        row2.add(sales);
        os.addRow(row2);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.employees.name", "Department.name");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        try {
            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
            fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                "This result element ( Sales 11 Department) " +
                "does not belong on this map " +
                "({objectId=5, class=Employee, employees=[{objectId=5, name=Tim Canterbury, class=Employee}]}) " +
                "- classes don't match (Department ! isa Employee)",
                e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }

    }
    public void testRefBeforeItsParentCollectionOrder() throws Exception {
        os.setResultsSize(2);

        ResultsRow row1 = new ResultsRow();
        row1.add(wernhamHogg);
        row1.add(tim);
        row1.add(sales);
        os.addRow(row1);
        ResultsRow row2 = new ResultsRow();
        row2.add(wernhamHogg);
        row2.add(gareth);
        row2.add(sales);
        os.addRow(row2);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.departments.employees.name", "Company.departments.name");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        try {
            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
            fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals("This array is empty - is the view in the wrong order?", e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }

    }
    public void testColBeforeItsParentRefOrder() throws Exception {
        os.setResultsSize(2);

        ResultsRow row1 = new ResultsRow();
        row1.add(david);
        row1.add(tim);
        row1.add(sales);
        os.addRow(row1);
        ResultsRow row2 = new ResultsRow();
        row2.add(david);
        row2.add(gareth);
        row2.add(sales);
        os.addRow(row2);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.department.employees.name", "Manager.department.name");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        try {
            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
            fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals("This node is not properly initialised (it doesn't have an objectId) - is the view in the right order?", e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }

    }

    public void testMultipleObjectsWithColls() throws Exception {
        os.setResultsSize(6);

        List<String> jsonStrings = new ArrayList<String>();

        jsonStrings.add(
                    "    {" +
                    "            'objectId'  : 11," +
                    "            'class'     : 'Department'," +
                    "            'name'      : 'Sales'," +
                    "            'employees' : [" +
                    "                { " +
                    "                    'objectId' : 5," +
                    "                    'class'    : 'Employee'," +
                    "                    'name'     : 'Tim Canterbury'," +
                    "                    'age'       : 30" +
                    "                }, " +
                    "                { " +
                    "                    'objectId' : 6," +
                    "                    'class'       : 'Employee'," +
                    "                    'name'        : 'Gareth Keenan'," +
                    "                    'age'      : 32" +
                    "                }" +
                    "            ]" +
                    "    }");
        jsonStrings.add(
                "    {" +
                "            'objectId'  : 12," +
                "            'class'     : 'Department'," +
                "            'name'      : 'Accounts'," +
                "            'employees' : [" +
                "                { " +
                "                    'objectId' : 8," +
                "                    'class'    : 'Employee'," +
                "                    'name'     : 'Keith Bishop'," +
                "                    'age'       : 41" +
                "                } " +
                "            ]" +
                "    }");

        jsonStrings.add(
                     "        {" +
                    "            'objectId'  : 13," +
                    "            'class'     : 'Department'," +
                    "            'name'      : 'Warehouse', " +
                    "            'employees' : [" +
                    "                {" +
                    "                    'objectId' : 9," +
                    "                    'class'    : 'Employee'," +
                    "                    'name'     : 'Lee'," +
                    "                    'age'      : 28" +
                    "                }," +
                    "                {" +
                    "                    'objectId' : 10," +
                    "                    'class'    : 'Employee'," +
                    "                    'name'     : 'Alex'," +
                    "                    'age'      : 24" +
                    "                }" +
                    "            ]" +
                    "        }");
        jsonStrings.add(
                "    {" +
                "            'objectId'  : 14," +
                "            'class'     : 'Department'," +
                "            'name'      : 'Reception'," +
                "            'employees' : [" +
                "                { " +
                "                    'objectId' : 7," +
                "                    'class'    : 'Employee'," +
                "                    'name'     : 'Dawn Tinsley'," +
                "                    'age'       : 26" +
                "                } " +
                "            ]" +
                "    }");

        ResultsRow row1 = new ResultsRow();
        row1.add(sales);
        row1.add(tim);
        ResultsRow row2 = new ResultsRow();
        row2.add(sales);
        row2.add(gareth);
        ResultsRow row3 = new ResultsRow();
        row3.add(accounts);
        row3.add(keith);
        ResultsRow row4 = new ResultsRow();
        row4.add(distribution);
        row4.add(lee);
        ResultsRow row5 = new ResultsRow();
        row5.add(distribution);
        row5.add(alex);
        ResultsRow row6 = new ResultsRow();
        row6.add(reception);
        row6.add(dawn);

        os.addRow(row1);
        os.addRow(row2);
        os.addRow(row3);
        os.addRow(row4);
        os.addRow(row5);
        os.addRow(row6);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.employees.name", "Department.employees.age");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            System.out.println(gotRow);
            got.add(gotRow);
        }

        assertEquals(4, got.size());
        for (int i = 0; i < jsonStrings.size(); i++) {
            JSONObject jo = new JSONObject(jsonStrings.get(i));
            assertEquals(null, JSONObjTester.getProblemsComparing(jo, got.get(i)));
        }
    }

    public void testMultipleObjectsWithRefs() throws Exception {
        os.setResultsSize(6);

        List<String> jsonStrings = new ArrayList<String>();

        jsonStrings.add(
                    "    {" +
                    "            objectId  : 11," +
                    "            class     : 'Department'," +
                    "            name      : 'Sales'," +
                    "            manager   : { class: 'Manager', objectId: 3, age: 39, name: 'David Brent' }," +
                    "            company      : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }" +
                    "    }");
        jsonStrings.add(
                "    {" +
                "            objectId  : 12," +
                "            class     : 'Department'," +
                "            name      : 'Accounts'," +
                "            manager   : { class: 'Manager', objectId: 3, age: 39, name: 'David Brent' }," +
                "            company      : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }" +
                "    }");

        jsonStrings.add(
                     "        {" +
                    "            objectId  : 13," +
                    "            class     : 'Department'," +
                    "            name      : 'Warehouse', " +
                    "            manager   : { class: 'Manager', objectId: 4, age: 38, name: 'Glynn' }," +
                    "            company      : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }" +
                    "        }");
        jsonStrings.add(
                 "        {" +
                "            objectId  : 19," +
                "            class     : 'Department'," +
                "            name      : 'Swindon', " +
                "            manager   : { class: 'Manager', objectId: 20, age: 35, name: 'Neil Godwin' }," +
                "            company   : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }" +
                "        }");

        ResultsRow row1 = new ResultsRow();
        row1.add(sales);
        row1.add(david);
        row1.add(wernhamHogg);
        ResultsRow row2 = new ResultsRow();
        row2.add(accounts);
        row2.add(david);
        row2.add(wernhamHogg);
        ResultsRow row3 = new ResultsRow();
        row3.add(distribution);
        row3.add(taffy);
        row3.add(wernhamHogg);
        ResultsRow row4 = new ResultsRow();
        row4.add(swindon);
        row4.add(neil);
        row4.add(wernhamHogg);

        os.addRow(row1);
        os.addRow(row2);
        os.addRow(row3);
        os.addRow(row4);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.manager.name", "Department.manager.age", "Department.company.name", "Department.company.vatNumber");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(4, got.size());
        for (int i = 0; i < jsonStrings.size(); i++) {
            JSONObject jo = new JSONObject(jsonStrings.get(i));
            assertEquals(null, JSONObjTester.getProblemsComparing(jo, got.get(i)));
        }
    }

    public void testMultipleObjectsWithRefsAndCols() throws Exception {
    os.setResultsSize(7);

        List<String> jsonStrings = new ArrayList<String>();

        jsonStrings.add(
                    "    {" +
                    "            objectId  : 11," +
                    "            class     : 'Department'," +
                    "            name      : 'Sales'," +
                    "            manager   : { class: 'Manager', objectId: 3, age: 39, name: 'David Brent' }," +
                    "            company      : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }," +
                    "            'employees' : [" +
                    "                { " +
                    "                    objectId : 5," +
                    "                    class    : 'Employee'," +
                    "                    name     : 'Tim Canterbury'," +
                    "                    age         : 30" +
                    "                }, " +
                    "                { " +
                    "                    objectId : 6," +
                    "                    class     : 'Employee'," +
                    "                    name      : 'Gareth Keenan'," +
                    "                    age      : 32" +
                    "                }" +
                    "            ]" +
                    "    }");
        jsonStrings.add(
                "    {" +
                "            objectId  : 12," +
                "            class     : 'Department'," +
                "            name      : 'Accounts'," +
                "            manager   : { class: 'Manager', objectId: 3, age: 39, name: 'David Brent' }," +
                "            company      : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }," +
                "            employees : [" +
                "                { " +
                "                    objectId : 8," +
                "                    class    : 'Employee'," +
                "                    name     : 'Keith Bishop'," +
                "                    age         : 41" +
                "                } " +
                "            ]" +
                "    }");

        jsonStrings.add(
                     "        {" +
                    "            objectId  : 13," +
                    "            class     : 'Department'," +
                    "            name      : 'Warehouse', " +
                    "            manager   : { class: 'Manager', objectId: 4, age: 38, name: 'Glynn' }," +
                    "            company      : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }," +
                    "            employees : [" +
                    "                {" +
                    "                    objectId : 9," +
                    "                    class    : 'Employee'," +
                    "                    name     : 'Lee'," +
                    "                    age      : 28" +
                    "                }," +
                    "                {" +
                    "                    objectId : 10," +
                    "                    class    : 'Employee'," +
                    "                    name     : 'Alex'," +
                    "                    age      : 24" +
                    "                }" +
                    "            ]" +
                    "        }");
        jsonStrings.add(
                 "        {" +
                "            objectId  : 19," +
                "            class     : 'Department'," +
                "            name      : 'Swindon', " +
                "            manager   : { class: 'Manager', objectId: 20, age: 35, name: 'Neil Godwin' }," +
                "            company   : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }," +
                "            employees : [" +
                "                {" +
                "                    objectId : 21," +
                "                    class    : 'Employee'," +
                "                    name     : 'Rachel'," +
                "                    age      : 34" +
                "                }," +
                "                {" +
                "                    objectId : 22," +
                "                    class    : 'Employee'," +
                "                    name     : 'Trudy'," +
                "                    age      : 25" +
                "                }" +
                "            ]" +
                "        }");

        ResultsRow row1a = new ResultsRow();
        row1a.add(sales);
        row1a.add(david);
        row1a.add(wernhamHogg);
        row1a.add(tim);
        ResultsRow row1b = new ResultsRow();
        row1b.add(sales);
        row1b.add(david);
        row1b.add(wernhamHogg);
        row1b.add(gareth);
        ResultsRow row2 = new ResultsRow();
        row2.add(accounts);
        row2.add(david);
        row2.add(wernhamHogg);
        row2.add(keith);
        ResultsRow row3a = new ResultsRow();
        row3a.add(distribution);
        row3a.add(taffy);
        row3a.add(wernhamHogg);
        row3a.add(lee);
        ResultsRow row3b = new ResultsRow();
        row3b.add(distribution);
        row3b.add(taffy);
        row3b.add(wernhamHogg);
        row3b.add(alex);
        ResultsRow row4a = new ResultsRow();
        row4a.add(swindon);
        row4a.add(neil);
        row4a.add(wernhamHogg);
        row4a.add(rachel);
        ResultsRow row4b = new ResultsRow();
        row4b.add(swindon);
        row4b.add(neil);
        row4b.add(wernhamHogg);
        row4b.add(trudy);


        os.addRow(row1a);
        os.addRow(row1b);
        os.addRow(row2);
        os.addRow(row3a);
        os.addRow(row3b);
        os.addRow(row4a);
        os.addRow(row4b);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.manager.name", "Department.manager.age",
                        "Department.company.name", "Department.company.vatNumber",
                        "Department.employees.name", "Department.employees.age");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(4, got.size());
        for (int i = 0; i < jsonStrings.size(); i++) {
            JSONObject jo = new JSONObject(jsonStrings.get(i));
            assertEquals(null, JSONObjTester.getProblemsComparing(jo, got.get(i)));
        }
    }

    public void testUnsupportedOperations() throws Exception {
        os.setResultsSize(1);

        ResultsRow row = new ResultsRow();
        row.add(wernhamHogg);

        os.addRow(row);
        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));


        try {
            jsonIter.remove();
            fail("No exception was thrown when calling the unsupported method remove");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (UnsupportedOperationException e) {
            // Test that this is what we thought would happen.
            assertEquals("Remove is not implemented in this class", e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }
    }

    public void testCurrentArrayIsEmpty() throws Exception {
        os.setResultsSize(1);

        ResultsRow row = new ResultsRow();
        row.add(wernhamHogg);
        row.add(david);

        os.addRow(row);
        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.contractors.oldComs.departments.manager.name");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));


        try {

            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
            fail("No exception was thrown when calling the unsupported method remove");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals("This array is empty - is the view in the wrong order?", e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }
    }

    public void testCurrentMapIsNull() throws Exception {
        os.setResultsSize(1);

        ResultsRow row = new ResultsRow();
        row.add(sales);
        row.add(address);

        os.addRow(row);
        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.company.contractors.personalAddress.address");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));


        try {

            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
            fail("No exception was thrown when calling the unsupported method remove");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals("This node is not properly initialised (it doesn't have an objectId) - is the view in the right order?", e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }
    }

    public void testDateConversion() throws Exception {
        os.setResultsSize(1);

        Types typeContainer = new Types();
        typeContainer.setId(new Integer(100));
        Calendar cal = Calendar.getInstance();
        cal.set(108 + 1900, 6, 6);
        typeContainer.setDateObjType(cal.getTime());

        ResultsRow row = new ResultsRow();
        row.add(typeContainer);

        os.addRow(row);
        PathQuery pq = new PathQuery(model);
        pq.addViews("Types.dateObjType");

        String jsonString = "{ class: 'Types', objectId: 100, dateObjType: '2008-07-06' }";

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(1, got.size());
        JSONObject expected = new JSONObject(jsonString);
        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

    }

    public void testIsCellValidForPath() throws Exception {
        Path manP = null;
        Path empsP = null;
        Path depP = null;
        try {
            manP = new Path(model, "Manager.name");
            depP = new Path(model, "Manager.department");
            empsP = new Path(model, "Manager.department.employees");
        } catch (PathException e) {
            e.printStackTrace();
        }

        ResultElement re = new ResultElement(david, manP, false);
        Map<String, Object> jsonMap = new TreeMap<String, Object>();


        ResultsRow row = new ResultsRow();
        row.add(david);
        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        assertTrue(jsonIter.isCellValidForPath(re, manP));
        assertTrue(jsonIter.isCellValidForPath(re, empsP));
        assertTrue(jsonIter.isCellValidForPath(null, manP));
        assertTrue(jsonIter.isCellValidForPath(new ResultElement(null, null, false), manP));
        assertTrue(! jsonIter.isCellValidForPath(re, depP));


    }

    public void testAIsaB() throws ObjectStoreException {

        ResultsRow row = new ResultsRow();
        row.add(david);
        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        assertTrue(jsonIter.aIsaB("Manager", "Employee"));
        assertTrue(! jsonIter.aIsaB("Employee", "Manager"));
        assertTrue(! jsonIter.aIsaB("Manager", "Department"));
        assertTrue(jsonIter.aIsaB(null, null));
        assertTrue(jsonIter.aIsaB("Manager", "Manager"));
        try {
            jsonIter.aIsaB("Manager", "Fool");
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (IllegalArgumentException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                "These names are not valid classes: a=Manager,b=Fool",
                e.getMessage());
        }
        try {
            jsonIter.aDescendsFromB("Fool", "Manager");
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                "Problem getting supers for Fool",
                e.getMessage());
        }
    }

    public void testThosePlacesOtherTestsCannotReach() throws Exception {
        // In normal circumstances these exceptions will never be thrown.
        // These tests are just to check that they will be
        // should abnormal circumstances ever occur.
        Path p = null;
        Path depP = null;
        Path empsP = null;
        Path badP = null;
        try {
            p = new Path(model, "Manager.name");
            depP = new Path(model, "Manager.department");
            empsP = new Path(model, "Manager.department.employees");
            badP = new BadPath(model, "Manager.name");
        } catch (PathException e) {
            e.printStackTrace();
        }
        ResultElement re = new ResultElement(david, p, false);
        Map<String, Object> jsonMap = new TreeMap<String, Object>();


        ResultsRow row = new ResultsRow();
        row.add(david);
        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name");

        JSONResultsIterator jsonIter = new JSONResultsIterator(getIterator(pq));

        jsonMap.put("objectId", 1000);
        try {
            jsonIter.setOrCheckClassAndId(re, p, jsonMap);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                    "This result element ( David Brent 3 Manager) " +
                    "does not belong on this map ({class=Manager, objectId=1000}) - " +
                    "objectIds don't match (1000 != 3)",
                    e.getMessage());
        }

        jsonMap.clear();
        try {
            jsonIter.setOrCheckClassAndId(re, depP, jsonMap);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                "This result element ( David Brent 3 Manager) " +
                "does not match its column because: " +
                "classes not compatible (Department is not a superclass of Manager)",
                e.getMessage());
        }

        jsonMap.clear();
        jsonMap.put("class", "Department");
        try {
            jsonIter.setOrCheckClassAndId(re, p, jsonMap);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                    "This result element ( David Brent 3 Manager) " +
                    "does not belong on this map ({class=Department}) " +
                    "- classes don't match (Manager ! isa Department)",
                    e.getMessage());
        }

        jsonMap.clear();
        jsonMap.put("name", null);
        try {
            jsonIter.addFieldToMap(re, p, jsonMap);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                    "Trying to set key name as David Brent in {class=Manager, name=null, objectId=3} " +
                    "but it already has the value null",
                    e.getMessage());
        }

        jsonMap.clear();
        jsonMap.put("name", "Neil");
        try {
            jsonIter.addFieldToMap(re, p, jsonMap);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                    "Trying to set key name as David Brent in {class=Manager, name=Neil, objectId=3} " +
                    "but it already has the value Neil",
                    e.getMessage());
        }

        jsonIter.currentArray = null;
        try {
            jsonIter.setCurrentMapFromCurrentArray(re);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                    "Nowhere to put this field",
                    e.getMessage());
        }

        jsonIter.currentArray = null;
        try {
            jsonIter.setCurrentMapFromCurrentArray();
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                "Nowhere to put this reference - the current array is null",
                e.getMessage());
        }

        jsonMap.put("department", "Clowning About");
        jsonIter.currentMap = jsonMap;
        try {
            jsonIter.addReferenceToCurrentNode(depP);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                    "Trying to set a reference on department, " +
                    "but this node " +
                    "{class=Manager, department=Clowning About, name=Neil, objectId=3} " +
                    "already has this key set, " +
                    "and to something other than a map " +
                    "(java.lang.String: Clowning About)",
                    e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }

        jsonIter.currentMap = null;
        jsonIter.currentArray = null;
        try {
            jsonIter.addReferenceToCurrentNode(depP);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                    "Nowhere to put this reference - the current array is null",
                    e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }

        jsonMap.put("employees", "Poor Fools");
        jsonIter.currentMap = jsonMap;
        try {
            jsonIter.addCollectionToCurrentNode(empsP);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                    "Trying to set a collection on employees, " +
                    "but this node " +
                    "{class=Manager, department=Clowning About, employees=Poor Fools, name=Neil, objectId=3} " +
                    "already has this key set to something other than a list " +
                    "(java.lang.String: Poor Fools)",
                    e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }

        try {
            jsonIter.addReferencedCellToJsonMap(re, badP, jsonMap);
            fail("No exception was thrown, although I tried very hard indeed to cause one");
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (JSONFormattingException e) {
            // Test that this is what we thought would happen.
            assertEquals(
                    "Bad path type: Manager.name",
                    e.getMessage());
        } catch (Throwable e){
            // All other exceptions are failures
            fail("Got unexpected error: " + e);
        }


    }

    private class BadPath extends Path {

        public BadPath(Model m, String s) throws PathException {
            super(m, s);
        }

        @Override
        public List<Path> decomposePath() {
            return Arrays.asList((Path) this);
        }
        @Override
        public boolean endIsAttribute() {
            return false;
        }
        @Override
        public boolean endIsReference() {
            return false;
        }
        @Override
        public boolean endIsCollection() {
            return false;
        }
        @Override
        public boolean isRootPath() {
            return false;
        }

    }


}
