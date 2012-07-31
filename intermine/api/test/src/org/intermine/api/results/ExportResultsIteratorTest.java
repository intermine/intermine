package org.intermine.api.results;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.query.MainHelper;
import org.intermine.api.results.flatouterjoins.MultiRow;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.dummy.DummyResults;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IteratorIterable;

/**
 * Tests for the ExportResultsIterator class
 *
 * @author Matthew Wakeling
 */

public class ExportResultsIteratorTest extends TestCase
{
    private final Model model = Model.getInstanceByName("testmodel");
    private Map classKeys;

    public ExportResultsIteratorTest (String arg) {
        super(arg);
    }

    public void testNestedCollection() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setResultsSize(1);


        // Set up some known objects in the first 3 results rows
        Company company1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company1.setName("Company1");
        company1.setVatNumber(101);
        company1.setId(new Integer(1));

        Department department1 = new Department();
        department1.setName("Department1");
        department1.setId(new Integer(2));
        Department department2 = new Department();
        department2.setName("Department2");
        department2.setId(new Integer(3));

        Employee employee1 = new Employee();
        employee1.setName("Employee1");
        employee1.setId(new Integer(4));
        employee1.setAge(42);
        Employee employee2 = new Employee();
        employee2.setName("Employee2");
        employee2.setId(new Integer(5));
        employee2.setAge(43);
        Employee employee3 = new Employee();
        employee3.setName("Employee3");
        employee3.setId(new Integer(6));
        employee3.setAge(44);
        Employee employee4 = new Employee();
        employee4.setName("Employee4");
        employee4.setId(new Integer(7));
        employee4.setAge(45);

        ResultsRow row = new ResultsRow();
        row.add(company1);
        List sub1 = new ArrayList();
        ResultsRow subRow1 = new ResultsRow();
        subRow1.add(department1);
        List sub2 = new ArrayList();
        ResultsRow subRow2 = new ResultsRow();
        subRow2.add(employee1);
        sub2.add(subRow2);
        subRow2 = new ResultsRow();
        subRow2.add(employee2);
        sub2.add(subRow2);
        subRow1.add(sub2);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(department2);
        sub2 = new ArrayList();
        subRow2 = new ResultsRow();
        subRow2.add(employee3);
        sub2.add(subRow2);
        subRow2 = new ResultsRow();
        subRow2.add(employee4);
        sub2.add(subRow2);
        subRow1.add(sub2);
        sub1.add(subRow1);
        row.add(sub1);
        os.addRow(row);
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber", "Company.departments.name", "Company.departments.employees.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);
        
        Path p1 = pq.makePath("Company.name");
        Path p2 = pq.makePath("Company.vatNumber");
        Path p3 = pq.makePath("Company.departments.name");
        Path p4 = pq.makePath("Company.departments.employees.name");
        
        List expected = Arrays.asList(
        		Arrays.asList(
	        		new ResultElement(company1, p1, false), 
	        		new ResultElement(company1, p2, false), 
	        		new ResultElement(department1, p3, false), 
	        		new ResultElement(employee1, p4, false)
	        	),
                Arrays.asList(
                	new ResultElement(company1, p1, false), 
                	new ResultElement(company1, p2, false), 
                	new ResultElement(department1, p3, false), 
                	new ResultElement(employee2, p4, false)
                ),
                Arrays.asList(
            		new ResultElement(company1, p1, false), 
            		new ResultElement(company1, p2, false), 
            		new ResultElement(department2, p3, false), 
            		new ResultElement(employee3, p4, false)
                ),
                Arrays.asList(
            		new ResultElement(company1, p1, false), 
            		new ResultElement(company1, p2, false), 
            		new ResultElement(department2, p3, false), 
            		new ResultElement(employee4, p4, false)
                )
        );

        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
        
        List got = new ArrayList();
        for (List gotRow : new IteratorIterable<List<ResultElement>>(iter)) {
            got.add(gotRow);
        }

        assertEquals(expected, got);
    }

    public void testParallelCollection() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setResultsSize(1);

        // Set up some known objects in the first 3 results rows
        Company company1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company1.setName("Company1");
        company1.setVatNumber(101);
        company1.setId(new Integer(1));

        Department department1 = new Department();
        department1.setName("Department1");
        department1.setId(new Integer(2));
        Department department2 = new Department();
        department2.setName("Department2");
        department2.setId(new Integer(3));

        Contractor contractor1 = new Contractor();
        contractor1.setName("Contractor1");
        contractor1.setId(new Integer(4));
        Contractor contractor2 = new Contractor();
        contractor2.setName("Contractor2");
        contractor2.setId(new Integer(5));
        Contractor contractor3 = new Contractor();
        contractor3.setName("Contractor3");
        contractor3.setId(new Integer(6));

        ResultsRow row = new ResultsRow();
        row.add(company1);
        List sub1 = new ArrayList();
        ResultsRow subRow1 = new ResultsRow();
        subRow1.add(department1);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(department2);
        sub1.add(subRow1);
        row.add(sub1);
        sub1 = new ArrayList();
        subRow1 = new ResultsRow();
        subRow1.add(contractor1);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(contractor2);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(contractor3);
        sub1.add(subRow1);
        row.add(sub1);
        os.addRow(row);
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber", "Company.departments.name", "Company.contractors.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.contractors", OuterJoinStatus.OUTER);
        Path p1 = pq.makePath("Company.name");
        Path p2 = pq.makePath("Company.vatNumber");
        Path p3 = pq.makePath("Company.departments.name");
        Path p4 = pq.makePath("Company.contractors.name");
        
        List expected = Arrays.asList(
        		Arrays.asList(
        				new ResultElement(company1, p1, false), 
        				new ResultElement(company1, p2, false), 
        				new ResultElement(department1, p3, false), 
        				null
        		),
				Arrays.asList(new ResultElement(company1, p1, false), 
						new ResultElement(company1, p2, false), 
						new ResultElement(department2, p3, false), 
						null
				),
				Arrays.asList(
						new ResultElement(company1, p1, false), 
						new ResultElement(company1, p2, false), 
						null, 
						new ResultElement(contractor1, p4, false)
				),
				Arrays.asList(
						new ResultElement(company1, p1, false), 
						new ResultElement(company1, p2, false), 
						null, 
						new ResultElement(contractor2, p4, false)
				),
				Arrays.asList(
						new ResultElement(company1, p1, false), 
						new ResultElement(company1, p2, false), 
						null, 
						new ResultElement(contractor3, p4, false)
				)
        	);
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
        
        List got = new ArrayList();
        for (List gotRow : new IteratorIterable<List<ResultElement>>(iter)) {
            got.add(gotRow);
        }
        
        assertEquals(expected, got);
    }

    public void testReference() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setResultsSize(2);

        // Set up some known objects in the first 3 results rows
        Company company1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company1.setName("Company1");
        company1.setVatNumber(101);
        company1.setId(new Integer(1));

        Department department1 = new Department();
        department1.setName("Department1");
        department1.setId(new Integer(2));
        Department department2 = new Department();
        department2.setName("Department2");
        department2.setId(new Integer(3));

        ResultsRow row1 = new ResultsRow();
        row1.add(department1);
        row1.add(company1);
        os.addRow(row1);
        ResultsRow row2 = new ResultsRow();
        row2.add(department2);
        row2.add(company1);
        os.addRow(row2);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.company.name");
        pq.setOuterJoinStatus("Department.company", OuterJoinStatus.OUTER);
        Path p1 = pq.makePath("Department.name");
        Path p2 = pq.makePath("Department.company.name");
        
        List expected = Arrays.asList(
        		Arrays.asList(
        				new ResultElement(department1, p1, false), 
        				new ResultElement(company1, p2, false)
        		),
                Arrays.asList(
                		new ResultElement(department2, p1, false), 
                		new ResultElement(company1, p2, false)
                )
        );
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 2, true, true, new HashMap());
        System.err.println(resultList);
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);

        List got = new ArrayList();
        for (List gotRow : new IteratorIterable<List<ResultElement>>(iter)) {
            got.add(gotRow);
        }
        
        assertEquals(expected, got);
    }

    public void testReferenceReference() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setResultsSize(2);

        // Set up some known objects in the first 3 results rows
        Company company1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company1.setName("Company1");
        company1.setVatNumber(101);
        company1.setId(new Integer(1));

        Department department1 = new Department();
        department1.setName("Department1");
        department1.setId(new Integer(2));
        Department department2 = new Department();
        department2.setName("Department2");
        department2.setId(new Integer(3));

        Employee e1 = new Employee();
        e1.setName("Employee1");
        e1.setId(4);
        Employee e2 = new Employee();
        e2.setName("Employee2");
        e2.setId(5);

        ResultsRow row = new ResultsRow();
        row.add(e1);
        row.add(department1);
        row.add(company1);
        os.addRow(row);
        row = new ResultsRow();
        row.add(e2);
        row.add(department2);
        row.add(company1);
        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Employee.name", "Employee.department.name", "Employee.department.company.name");
        pq.setOuterJoinStatus("Employee.department", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Employee.department.company", OuterJoinStatus.OUTER);
        Path p1 = pq.makePath("Employee.name");
        Path p2 = pq.makePath("Employee.department.name");
        Path p3 = pq.makePath("Employee.department.company.name");
        
        List expected = Arrays.asList(
        		Arrays.asList(
        				new ResultElement(e1, p1, false), 
        				new ResultElement(department1, p2, false), 
        				new ResultElement(company1, p3, false)
        		),
                Arrays.asList(
                		new ResultElement(e2, p1, false), 
                		new ResultElement(department2, p2, false), 
                		new ResultElement(company1, p3, false)
                )
        );
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 2, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
        
        System.out.println("Columns: " + iter.getColumns());
        List got = new ArrayList();
        for (List gotRow : new IteratorIterable<List<ResultElement>>(iter)) {
            got.add(gotRow);
        }
        
        assertEquals(expected, got);
    }

    public void testMultipleCollectionsWithOneEntry() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setResultsSize(1);

        Company company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setName("Company1");
        company.setVatNumber(101);
        company.setId(1);

        Department department = new Department();
        department.setName("Department1");
        department.setId(2);

        Contractor contractor = new Contractor();
        contractor.setName("Fred");
        contractor.setId(3);

        ResultsRow row = new ResultsRow();
        row.add(company);
        List sub1 = new MultiRow();
        ResultsRow subRow1 = new ResultsRow();
        subRow1.add(department);
        sub1.add(subRow1);
        row.add(sub1);
        sub1 = new MultiRow();
        subRow1 = new ResultsRow();
        subRow1.add(contractor);
        sub1.add(subRow1);
        row.add(sub1);
        os.addRow(row);

        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.departments.name", "Company.contractors.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.contractors", OuterJoinStatus.OUTER);
        Path p1 = pq.makePath("Company.name");
        Path p2 = pq.makePath("Company.departments.name");
        Path p3 = pq.makePath("Company.contractors.name");
        
        List expected = Arrays.asList(
        		Arrays.asList(
        				new ResultElement(company, p1, false),
        				new ResultElement(department, p2, false),
        				new ResultElement(contractor, p3, false)
        		)
        );
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
        
        List got = new ArrayList();
        for (List gotRow : new IteratorIterable<List<ResultElement>>(iter)) {
            got.add(gotRow);
        }
        
        assertEquals(expected, got);
    }
}
