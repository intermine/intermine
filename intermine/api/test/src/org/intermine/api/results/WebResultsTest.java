package org.intermine.api.results;

/*
 * Copyright (C) 2002-2016 FlyMine
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

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.query.MainHelper;
import org.intermine.api.results.flatouterjoins.MultiRow;
import org.intermine.api.results.flatouterjoins.MultiRowFirstValue;
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
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the WebResults class
 *
 * @author Kim Rutherford
 * @author Xavier Watkins
 */

public class WebResultsTest extends InterMineAPITestCase
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
    private ObjectStoreDummyImpl osd;
    private final Model model = Model.getInstanceByName("testmodel");
    private Map classKeys;


    public WebResultsTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osd = new ObjectStoreDummyImpl();
        osd.setResultsSize(15);

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
        osd.addRow(row);
        row = new ResultsRow();
        row.add(department2);
        row.add(company2);
        row.add(man2);
        osd.addRow(row);
        row = new ResultsRow();
        row.add(department3);
        row.add(company3);
        row.add(man3);
        osd.addRow(row);
        classKeys = new HashMap();
        FieldDescriptor fd = model.getClassDescriptorByName("org.intermine.model.testmodel.Company").getFieldDescriptorByName("name");
        ArrayList<FieldDescriptor> keys = new ArrayList();
        keys.add(fd);
        classKeys.put("Company", keys);
    }

    // create a query with MainHelper.makeQuery() that contains both QueryClasses and QueryFields
     public void testGetPathToIndex() throws Exception {
         PathQuery pathQuery = new PathQuery(model);
         pathQuery.addViews("Department.name", "Department.manager.name", "Department.employees.name");
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

         Query query = MainHelper.makeQuery(pathQuery , new HashMap(), pathToQueryNode, null, null);
         WebResults webResults = new WebResults(im, pathQuery, osd.execute(query), pathToQueryNode, new HashMap());
         LinkedHashMap<String, Integer> actual = webResults.pathToIndex;
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
        pq.addViews("Company.name", "Company.vatNumber", "Company.CEO.name");
        pq.setDescription("Company", "description 1");
        Map<String, QuerySelectable> pathToQueryNode = new HashMap();
        Query query = MainHelper.makeQuery(pq , new HashMap(), pathToQueryNode, null, null);
        Results results = osd.execute(query);
        WebResults webResults = new WebResults(im, pq, results, pathToQueryNode, classKeys);
        List<Column> expectedColumns = new ArrayList<Column>();
        Column col1 = new Column("description 1 > name",0 ,Company.class);
        Column col2 = new Column("description 1 > vatNumber",1 ,Company.class);
        Column col3 = new Column("description 1 > CEO > name",2 ,CEO.class);
        expectedColumns.add(col1);
        expectedColumns.add(col2);
        expectedColumns.add(col3);
        assertEquals(expectedColumns.get(0), webResults.getColumns().get(0));
        assertEquals(expectedColumns.get(1), webResults.getColumns().get(1));
        assertEquals(expectedColumns.get(2), webResults.getColumns().get(2));
    }

    // Test with a PathQuery and some dummy results, call method with a made up row,
    // create expected ResultElements.  Doesn't need too much testing as Path.resolve() is tested.
    public void testTranslateRow() throws Exception {
        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.company.name", "Department.manager.name");
        Map<String, QuerySelectable> pathToQueryNode = new HashMap();
        Query query = MainHelper.makeQuery(pq , new HashMap(), pathToQueryNode, null, null);
        Results results = osd.execute(query);
        WebResults webResults = new WebResults(im, pq, results, pathToQueryNode, classKeys);
        List row1 = webResults.getResultElements(0);

        Department dept1 = new Department();
        dept1.setId(new Integer(4));
        dept1.setName("Department1");
        ResultElement res1 = new ResultElement(dept1, new Path(model, "Department.name"), false);

        Company c1 = (Company) DynamicUtil.instantiateObject("org.intermine.model.testmodel.Company", null);
        c1.setId(new Integer(1));
        c1.setName("Company1");
        ResultElement res2 = new ResultElement(c1, new Path(model, "Department.company.name"), false);

        Manager m1 = new Manager();
        m1.setId(new Integer(1));
        m1.setSeniority(new Integer(100));
        m1.setName("Manager1");
        ResultElement res3 = new ResultElement(m1, new Path(model, "Department.manager.name"), false);

        ResultsRow expected = new ResultsRow();
        expected.add(new MultiRowFirstValue(res1, 1));
        expected.add(new MultiRowFirstValue(res2, 1));
        expected.add(new MultiRowFirstValue(res3, 1));
        assertEquals(new MultiRow(Collections.singletonList(expected)), row1);
    }

    public void test() throws Exception {
        IqlQuery fq =
            new IqlQuery("SELECT DISTINCT a1_, a3_, a4_ FROM org.intermine.model.testmodel.Department AS a1_," +
                    " org.intermine.model.testmodel.CEO AS a2_, org.intermine.model.testmodel.Company " +
                    "AS a3_, org.intermine.model.testmodel.Manager AS a4_ " +
                    "WHERE (a1_.manager CONTAINS a2_ AND a2_.company CONTAINS a3_ " +
                    "AND a1_.employees CONTAINS a4_)",
                    "org.intermine.model.testmodel");
        Query query = fq.toQuery();
        Results results = osd.execute(query);

        PathQuery pathQuery = new PathQuery(model);
        pathQuery.addViews("Department.name", "Department.manager.company.name", "Department.manager.company.vatNumber", "Department.employees.seniority");
        pathQuery.addConstraint(new PathConstraintSubclass("Department.manager", "CEO"));
        pathQuery.addConstraint(new PathConstraintSubclass("Department.employees", "Manager"));
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
        pathToQueryNode.put("Department.employees", manQC);
        QueryField manSeniority = new QueryField(manQC, "seniority");
        pathToQueryNode.put("Department.employees.seniority", manSeniority);
        WebResults webResults = new WebResults(im, pathQuery, results, pathToQueryNode, null);

        assertEquals("Department1", webResults.get(0).get(0).get(0).getValue().getField());
        assertEquals("Company1", webResults.get(0).get(0).get(1).getValue().getField());
        assertEquals(new Integer(101), webResults.get(0).get(0).get(2).getValue().getField());
        assertEquals("Department2", webResults.get(1).get(0).get(0).getValue().getField());
        assertEquals("Company2", webResults.get(1).get(0).get(1).getValue().getField());
        assertEquals(new Integer(102), webResults.get(1).get(0).get(2).getValue().getField());
        assertEquals("Department3", webResults.get(2).get(0).get(0).getValue().getField());
        assertEquals("Company3", webResults.get(2).get(0).get(1).getValue().getField());
        assertEquals(new Integer(103), webResults.get(2).get(0).get(2).getValue().getField());
    }
}
