package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.path.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryHandler;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.MainHelper;

/**
 * Tests for the WebResults class
 *
 * @author Kim Rutherford
 * @author Xavier Watkins
 */

public class WebResultsTest extends TestCase
{
    private Company company1;
    private Company company2;
    private Company company3;
    private Department department1;
    private Department department2;
    private Department department3;
    private Manager man1;
    private Manager man2;
    private CEO man3;
    private ObjectStoreDummyImpl os;
    private final Model model = Model.getInstanceByName("testmodel");
    private Map classKeys;

    public WebResultsTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        os = new ObjectStoreDummyImpl();
        os.setResultsSize(15);

        // Set up some known objects in the first 3 results rows
        department1 = new Department();
        department1.setName("Department1");
        department1.setId(new Integer(4));
        department2 = new Department();
        department2.setName("Department2");
        department2.setId(new Integer(5));
        department3 = new Department();
        department3.setName("Department3");
        department3.setId(new Integer(6));

        company1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company1.setName("Company1");
        company1.setVatNumber(101);
        company1.setId(new Integer(1));
        company2 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company2.setName("Company2");
        company2.setVatNumber(102);
        company2.setId(new Integer(2));
        company3 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company3.setName("Company3");
        company3.setVatNumber(103);
        company3.setId(new Integer(3));

        man1 = (Manager) DynamicUtil.createObject(Collections.singleton(Manager.class));
        man1.setName("Manager1");
        man1.setSeniority(new Integer(100));
        man1.setId(new Integer(1));
        man2 = (Manager) DynamicUtil.createObject(Collections.singleton(Manager.class));
        man2.setName("Manager2");
        man2.setSeniority(new Integer(200));
        man2.setId(new Integer(2));
        man3 = (CEO) DynamicUtil.createObject(Collections.singleton(CEO.class));
        man3.setName("Manager3");
        man3.setSeniority(new Integer(300));
        man3.setId(new Integer(3));
        man3.setCompany(company3);

        ResultsRow row = new ResultsRow();
        row.add(department1);
        row.add(company1);
        row.add(man1);
        os.addRow(row);
        row = new ResultsRow();
        row.add(department2);
        row.add(company2);
        row.add(man2);
        os.addRow(row);
        row = new ResultsRow();
        row.add(department3);
        row.add(company3);
        row.add(man3);
        os.addRow(row);
        classKeys = new HashMap();
        FieldDescriptor fd = model.getClassDescriptorByName("org.intermine.model.testmodel.Company").getFieldDescriptorByName("name");
        ArrayList<FieldDescriptor> keys = new ArrayList();
        keys.add(fd);
        classKeys.put("Company", keys);
    }

    // create a query with MainHelper.makeQuery() that contains both QueryClasses and QueryFields
     public void testGetPathToIndex() throws Exception {
         List<Path> view = new ArrayList<Path>() {{ // see: http://www.c2.com/cgi/wiki?DoubleBraceInitialization
             add(new Path(model, "Department.name"));
             add(new Path(model, "Department.manager.name"));
             add(new Path(model, "Department.employees.name"));
         }};
         PathQuery pathQuery = new PathQuery(model);
         pathQuery.setViewPaths(view);
         QueryClass dept1 = new QueryClass(Department.class);
         QueryField depName = new QueryField(dept1, "name");

         QueryClass man1 = new QueryClass(Manager.class);
         QueryField manName = new QueryField(man1, "name");

         QueryClass emp1 = new QueryClass(Employee.class);
         QueryField empName = new QueryField(emp1, "name");


         Map<String, QuerySelectable> pathToQueryNode = new HashMap();
         pathToQueryNode.put("Department", dept1);
         pathToQueryNode.put("Department.name", depName);
         pathToQueryNode.put("Department.manager", man1);
         pathToQueryNode.put("Department.manager.name", manName);
         pathToQueryNode.put("Department.employees", emp1);
         pathToQueryNode.put("Department.employees.name", empName);

         Query query = MainHelper.makeQuery(pathQuery , new HashMap(), pathToQueryNode, null, null, true);
         LinkedHashMap<String, Integer> actual = WebResults.getPathToIndex(query, pathToQueryNode);
         LinkedHashMap<String, Integer> expected = new LinkedHashMap<String, Integer>();
         expected.put("Department.employees.name", 2);
         expected.put("Department.name", 0);
         expected.put("Department.manager.name", 1);
         expected.put("Department.employees", 2);
         expected.put("Department.manager", 1);
         expected.put("Department", 0);
         assertEquals(expected, actual);
    }

    // create a PathQuery, create expected column objects and compare.  Include:
    //   - some paths with descriptions
    //   - select fields that are/aren't class keys
    public void testSetColumns() throws Exception {
        PathQuery pq = new PathQuery(model);
        List view = new ArrayList() {{ // see: http://www.c2.com/cgi/wiki?DoubleBraceInitialization
            add(new Path(model, "Company.name"));
            add(new Path(model, "Company.vatNumber"));
            add(new Path(model, "Company.CEO.name"));
            add(new Path(model, "Employee.name"));
        }};
        pq.addPathStringDescription("Company", "description 1");
        pq.setViewPaths(view);
        Map<String, QuerySelectable> pathToQueryNode = new HashMap();
        Query query = MainHelper.makeQuery(pq , new HashMap(), pathToQueryNode, null, null, true);
        Results results = os.execute(query);
        WebResults webResults = new WebResults(pq, results, model, pathToQueryNode, classKeys, null);
        List<Column> expectedColumns = new ArrayList<Column>();
        Path path = new Path(model, "Company.CEO.name");
        Column col1 = new Column("description 1.name",0 ,Company.class);
        Column col2 = new Column("description 1.vatNumber",1 ,Company.class);
        Column col3 = new Column("description 1.CEO.name",2 ,CEO.class);
        Column col4 = new Column("Employee.name",3 ,Employee.class);
        expectedColumns.add(col1);
        expectedColumns.add(col2);
        expectedColumns.add(col3);
        expectedColumns.add(col4);
        assertEquals(expectedColumns.get(0), webResults.getColumns().get(0));
        assertEquals(expectedColumns.get(1), webResults.getColumns().get(1));
        assertEquals(expectedColumns.get(2), webResults.getColumns().get(2));
        assertEquals(expectedColumns.get(3), webResults.getColumns().get(3));
    }

    // Test with a PathQuery and some dummy results, call method with a made up row,
    // create expected ResultElements.  Doesn't need too much testing as Path.resolve() is tested.
     public void testTranslateRow() throws Exception {
         PathQuery pq = new PathQuery(model);
         List view = new ArrayList() {{ // see: http://www.c2.com/cgi/wiki?DoubleBraceInitialization
             add(new Path(model, "Department.name"));
             add(new Path(model, "Department.company.name"));
         }};
         pq.setViewPaths(view);
         Map<String, QuerySelectable> pathToQueryNode = new HashMap();
         Query query = MainHelper.makeQuery(pq , new HashMap(), pathToQueryNode, null, null, true);
         Results results = os.execute(query);
         WebResults webResults = new WebResults(pq, results, model, pathToQueryNode, classKeys, null);
         List row1 = webResults.getResultElements(0);
         ResultElement res1 = new ResultElement(os, "Department1", new Integer(4), Department.class, new Path(model, "Department.name"), false);
         ResultElement res2 = new ResultElement(os, "Company1", new Integer(1), Company.class, new Path(model, "Department.company.name"), false);
         List expected = new ArrayList();
         expected.add(res1);
         expected.add(res2);
         assertEquals(expected, row1);
     }

    public void test() {
        IqlQuery fq =
            new IqlQuery("SELECT DISTINCT a1_, a3_, a4_ FROM org.intermine.model.testmodel.Department AS a1_," +
                    " org.intermine.model.testmodel.CEO AS a2_, org.intermine.model.testmodel.Company " +
                    "AS a3_, org.intermine.model.testmodel.Manager AS a4_ " +
                    "WHERE (a1_.manager CONTAINS a2_ AND a2_.company CONTAINS a3_ " +
                    "AND a1_.employees CONTAINS a4_)",
                    "org.intermine.model.testmodel");
        Query query = fq.toQuery();
        Results results = os.execute(query);

        List view = new ArrayList() {{ // see: http://www.c2.com/cgi/wiki?DoubleBraceInitialization
            add(new Path(model, "Department.name"));
            add(new Path(model, "Department.manager[CEO].company.name"));
            add(new Path(model, "Department.manager[CEO].company.vatNumber"));
            add(new Path(model, "Department.employees[Manager].seniority"));
        }};
        PathQuery pathQuery = new PathQuery(model);
        pathQuery.setViewPaths(view);
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        QueryClass deptQC = (QueryClass) query.getSelect().get(0);
        pathToQueryNode.put("Department", deptQC);
        QueryField deptNameQF = new QueryField(deptQC, "name");
        pathToQueryNode.put("Department.name", deptNameQF);

        QueryClass compQC = (QueryClass) query.getSelect().get(1);
        pathToQueryNode.put("Department.manager.company", compQC);
        QueryField compNameQF = new QueryField(compQC, "name");
        pathToQueryNode.put("Department.manager.company.name", compNameQF );
        QueryField compVatNumQF = new QueryField(compQC, "vatNumber");
        pathToQueryNode.put("Department.manager.company.vatNumber", compVatNumQF);

        QueryClass manQC = (QueryClass) query.getSelect().get(2);
        QueryField manSeniority = new QueryField(manQC, "seniority");
        pathToQueryNode.put("Department.employees.seniority", manSeniority);
        WebResults webResults =
            new WebResults(pathQuery, results, model, pathToQueryNode, classKeys, null);

        assertEquals("Department1", ((List) webResults.get(0)).get(0));
        assertEquals("Company1", ((List) webResults.get(0)).get(1));
        assertEquals(new Integer(101), ((List) webResults.get(0)).get(2));
        assertEquals("Department2", ((List) webResults.get(1)).get(0));
        assertEquals("Company2", ((List) webResults.get(1)).get(1));
        assertEquals(new Integer(102), ((List) webResults.get(1)).get(2));
        assertEquals("Department3", ((List) webResults.get(2)).get(0));
        assertEquals("Company3", ((List) webResults.get(2)).get(1));
        assertEquals(new Integer(103), ((List) webResults.get(2)).get(2));
    }
}





























