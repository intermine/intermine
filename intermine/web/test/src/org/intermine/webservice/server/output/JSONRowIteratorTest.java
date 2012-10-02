/**
 *
 */
package org.intermine.webservice.server.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.InterMineAPI;
import org.intermine.api.query.MainHelper;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.dummy.DummyResults;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IteratorIterable;
import org.json.JSONArray;

/**
 * A test for the JSONRowIterator
 * @author Alex Kalderimis
 *
 */
public class JSONRowIteratorTest extends TestCase {

    private ObjectStoreDummyImpl os;
    private final InterMineAPI apiWithRedirection = new DummyAPI();
    private final InterMineAPI apiWithoutRedirection = new DummyAPI(false);
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

    /**
     * Empty constructor
     */
    public JSONRowIteratorTest() {
        // Empty Constructor
    }

    /**
     * Constructor with name.
     * @param name
     */
    public JSONRowIteratorTest(String name) {
        super(name);
    }

    public void testSingleSimpleObject() throws Exception {
        os.setResultsSize(1);

        String jsonString = "[" +
                "{id: 3, class: 'Manager', value: 'David Brent', url: '/report.do?id=3'}," +
                "{id: 3, class: 'Manager', value: '39', url: '/report.do?id=3'}" +
                "]";
        JSONArray expected = new JSONArray(jsonString);

        ResultsRow row = new ResultsRow();
        row.add(david);

        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.age");

        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 5, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);

        ExportResultsIterator iter = new ExportResultsIterator(pq, q, results, pathToQueryNode);

        JSONRowIterator jsonIter = new JSONRowIterator(iter, apiWithoutRedirection);

        List<JSONArray> got = new ArrayList<JSONArray>();
        for (JSONArray gotRow : new IteratorIterable<JSONArray>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(1, got.size());

        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));

        jsonString = "[" +
            "{id: 3, class: 'Manager', value: 'David Brent', url: 'Link for:Manager [address=null, age=\"39\", department=null, departmentThatRejectedMe=null, end=\"null\", fullTime=\"false\", id=\"3\", name=\"David Brent\", seniority=\"null\", title=\"null\"]'}," +
            "{id: 3, class: 'Manager', value: '39', url: 'Link for:Manager [address=null, age=\"39\", department=null, departmentThatRejectedMe=null, end=\"null\", fullTime=\"false\", id=\"3\", name=\"David Brent\", seniority=\"null\", title=\"null\"]'}," +
        "]";
        expected = new JSONArray(jsonString);

        iter = new ExportResultsIterator(pq, q, results, pathToQueryNode);
        jsonIter = new JSONRowIterator(iter, apiWithRedirection);

        got = new ArrayList<JSONArray>();
        for (JSONArray gotRow : new IteratorIterable<JSONArray>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(1, got.size());

        assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(0)));
    }

    public void testResultsWithNulls() throws Exception {
        os.setResultsSize(2);

        List<String> jsonStrings = Arrays.asList(
                "[" +
                "{id: 3, class: 'Manager', value: 'David Brent', url: '/report.do?id=3'}," +
                "{id: 3, class: 'Manager', value: '39', url: '/report.do?id=3'}" +
                "]",
                "[" +
                "{value: null, url: null}," +
                "{value: null, url: null}" +
                "]");

        ResultsRow row = new ResultsRow();
        row.add(david);

        ResultsRow emptyRow = new ResultsRow();
        emptyRow.add(null);

        os.addRow(row);
        os.addRow(emptyRow);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.age");

        ExportResultsIterator iter = getIterator(pq);

        JSONRowIterator jsonIter = new JSONRowIterator(iter, apiWithoutRedirection);

        List<JSONArray> got = new ArrayList<JSONArray>();
        for (JSONArray gotRow : new IteratorIterable<JSONArray>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(2, got.size());

        for (int i = 0; i < got.size(); i++) {
            JSONArray expected = new JSONArray(jsonStrings.get(i));
            assertEquals(null, JSONObjTester.getProblemsComparing(expected, got.get(i)));
        }
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
        os.setResultsSize(5);

        List<String> jsonStrings = new ArrayList<String>();
        jsonStrings.add("[" +
                "{id: 5, class: 'Employee', url: '/report.do?id=5', value:30}," +
                "{id: 5, class: 'Employee', url: '/report.do?id=5', value:'Tim Canterbury'}" +
                "]");
        jsonStrings.add("[" +
                "{id: 6, class: 'Employee', url: '/report.do?id=6', value:32}," +
                "{id: 6, class: 'Employee', url: '/report.do?id=6', value:'Gareth Keenan'}" +
                "]");
        jsonStrings.add("[" +
                "{id: 7, class: 'Employee', url: '/report.do?id=7', value:26}," +
                "{id: 7, class: 'Employee', url: '/report.do?id=7', value:'Dawn Tinsley'}" +
                "]");
        jsonStrings.add("[" +
                "{id: 8, class: 'Employee', url: '/report.do?id=8', value:41}," +
                "{id: 8, class: 'Employee', url: '/report.do?id=8', value:'Keith Bishop'}" +
                "]");
        jsonStrings.add("[" +
                "{id: 9, class: 'Employee', url: '/report.do?id=9', value:28}," +
                "{id: 9, class: 'Employee', url: '/report.do?id=9', value:'Lee'}" +
                "]");

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

        os.addRow(row1);
        os.addRow(row2);
        os.addRow(row3);
        os.addRow(row4);
        os.addRow(row5);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Employee.age", "Employee.name");

        ExportResultsIterator iter = getIterator(pq);

        JSONRowIterator jsonIter = new JSONRowIterator(iter, apiWithoutRedirection);

        List<JSONArray> got = new ArrayList<JSONArray>();
        for (JSONArray gotRow : new IteratorIterable<JSONArray>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(5, got.size());
        for (int i = 0; i < jsonStrings.size(); i++) {
            JSONArray jo = new JSONArray(jsonStrings.get(i));
            assertEquals(null, JSONObjTester.getProblemsComparing(jo, got.get(i)));
        }

    }

    public void testSingleObjectWithNestedCollections() throws Exception {
        os.setResultsSize(4);

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
        pq.addViews("Company.name", "Company.vatNumber", "Company.departments.name", "Company.departments.employees.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);

        ExportResultsIterator iter = getIterator(pq);

        JSONRowIterator jsonIter = new JSONRowIterator(iter, apiWithoutRedirection);

        List<JSONArray> got = new ArrayList<JSONArray>();
        for (JSONArray gotRow : new IteratorIterable<JSONArray>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(got.size(), 4);
        List<String> jsonStrings = new ArrayList<String>();
        jsonStrings.add(
                "[" +
                "{id: 1, class: 'Company', value:'Wernham-Hogg',url:'/report.do?id=1'}," +
                "{id: 1, class: 'Company', value:101,url:'/report.do?id=1'}," +
                "{id: 11, class: 'Department', value:'Sales',url:'/report.do?id=11'}," +
                "{id: 5, class: 'Employee', value:'Tim Canterbury',url:'/report.do?id=5'}" +
                "]");
        jsonStrings.add(
                "[" +
                "{id: 1, class: 'Company', value:'Wernham-Hogg',url:'/report.do?id=1'}," +
                "{id: 1, class: 'Company', value:101,url:'/report.do?id=1'}," +
                "{id: 11, class: 'Department', value:'Sales',url:'/report.do?id=11'}," +
                "{id: 6, class: 'Employee', value:'Gareth Keenan',url:'/report.do?id=6'}" +
                "]");
        jsonStrings.add(
                "[" +
                "{id: 1, class: 'Company', value:'Wernham-Hogg',url:'/report.do?id=1'}," +
                "{id: 1, class: 'Company', value:101,url:'/report.do?id=1'}," +
                "{id: 13, class: 'Department', value:'Warehouse',url:'/report.do?id=13'}," +
                "{id: 9, class: 'Employee', value:'Lee',url:'/report.do?id=9'}" +
                "]");
        jsonStrings.add(
                "[" +
                "{id: 1, class: 'Company', value:'Wernham-Hogg',url:'/report.do?id=1'}," +
                "{id: 1, class: 'Company', value:101,url:'/report.do?id=1'}," +
                "{id: 13, class: 'Department', value:'Warehouse',url:'/report.do?id=13'}," +
                "{id: 10, class: 'Employee', value:'Alex',url:'/report.do?id=10'}" +
                "]");

        for (int index = 0; index < jsonStrings.size(); index++) {
            JSONArray ja = new JSONArray(jsonStrings.get(index));
            assertEquals(null, JSONObjTester.getProblemsComparing(ja, got.get(index)));
        }
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

        ExportResultsIterator iter = getIterator(pq);

        JSONRowIterator jsonIter = new JSONRowIterator(iter, apiWithoutRedirection);

        assert(!jsonIter.hasNext());

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
                    "Company.departments.employees.name",
                    "Company.departments.employees.age");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);

        ExportResultsIterator iter = getIterator(pq);

        JSONRowIterator jsonIter = new JSONRowIterator(iter, apiWithoutRedirection);

        List<JSONArray> got = new ArrayList<JSONArray>();
        for (JSONArray gotRow : new IteratorIterable<JSONArray>(jsonIter)) {
            got.add(gotRow);
        }

        assertEquals(got.size(), 4);
        List<String> jsonStrings = new ArrayList<String>();
        jsonStrings.add(
                "[" +
                "{id: 1, class: 'Company', value:'Wernham-Hogg',url:'/report.do?id=1'}," +
                "{id: 1, class: 'Company', value:101,url:'/report.do?id=1'}," +
                "{id: 11, class: 'Department', value:'Sales',url:'/report.do?id=11'}," +
                "{id: 5, class: 'Employee', value:'Tim Canterbury',url:'/report.do?id=5'}," +
                "{id: 5, class: 'Employee', value:'30',url:'/report.do?id=5'}" +
                "]");
        jsonStrings.add(
                "[" +
                "{id: 1, class: 'Company', value:'Wernham-Hogg',url:'/report.do?id=1'}," +
                "{id: 1, class: 'Company', value:101,url:'/report.do?id=1'}," +
                "{id: 11, class: 'Department', value:'Sales',url:'/report.do?id=11'}," +
                "{id: 6, class: 'Employee', value:'Gareth Keenan',url:'/report.do?id=6'}," +
                "{id: 6, class: 'Employee', value:'32',url:'/report.do?id=6'}" +
                "]");
        jsonStrings.add(
                "[" +
                "{id: 1, class: 'Company', value:'Wernham-Hogg',url:'/report.do?id=1'}," +
                "{id: 1, class: 'Company', value:101,url:'/report.do?id=1'}," +
                "{id: 13, class: 'Department', value:'Warehouse',url:'/report.do?id=13'}," +
                "{id: 9, class: 'Employee', value:'Lee',url:'/report.do?id=9'}," +
                "{id: 9, class: 'Employee', value:'28',url:'/report.do?id=9'}" +
                "]");
        jsonStrings.add(
                "[" +
                "{id: 1, class: 'Company', value:'Wernham-Hogg',url:'/report.do?id=1'}," +
                "{id: 1, class: 'Company', value:101,url:'/report.do?id=1'}," +
                "{id: 13, class: 'Department', value:'Warehouse',url:'/report.do?id=13'}," +
                "{id: 10, class: 'Employee', value:'Alex',url:'/report.do?id=10'}," +
                "{id: 10, class: 'Employee', value:'24',url:'/report.do?id=10'}" +
                "]");

        for (int index = 0; index < jsonStrings.size(); index++) {
            JSONArray ja = new JSONArray(jsonStrings.get(index));
            assertEquals(null, JSONObjTester.getProblemsComparing(ja, got.get(index)));
        }
    }

    public void testRemove() throws ObjectStoreException {
        ResultsRow row = new ResultsRow();
        row.add(wernhamHogg);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber");

        ExportResultsIterator iter = getIterator(pq);

        JSONRowIterator jsonIter = new JSONRowIterator(iter, apiWithoutRedirection);

        try {
            jsonIter.remove();
            fail("Expected an UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }
}
