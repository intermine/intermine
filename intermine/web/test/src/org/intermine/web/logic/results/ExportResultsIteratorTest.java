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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
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
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IteratorIterable;
import org.intermine.web.logic.query.MainHelper;

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
        final Path p1 = new Path(model, "Company.name");
        final Path p2 = new Path(model, "Company.vatNumber");
        final Path p3 = new Path(model, "Company:departments.name");
        final Path p4 = new Path(model, "Company:departments:employees.name");
        List view = new ArrayList() {{
            add(p1);
            add(p2);
            add(p3);
            add(p4);
        }};
        pq.setViewPaths(view);
        ExportResultsIterator iter = new ExportResultsIterator(os, pq, new HashMap(), null);
        List got = new ArrayList();
        for (ResultsRow gotRow : new IteratorIterable<ResultsRow>(iter)) {
            got.add(gotRow);
        }
        List expected = Arrays.asList(Arrays.asList(new ResultElement(company1, p1, false), new ResultElement(company1, p2, false), new ResultElement(department1, p3, false), new ResultElement(employee1, p4, false)),
                Arrays.asList(new ResultElement(company1, p1, false), new ResultElement(company1, p2, false), new ResultElement(department1, p3, false), new ResultElement(employee2, p4, false)),
                Arrays.asList(new ResultElement(company1, p1, false), new ResultElement(company1, p2, false), new ResultElement(department2, p3, false), new ResultElement(employee3, p4, false)),
                Arrays.asList(new ResultElement(company1, p1, false), new ResultElement(company1, p2, false), new ResultElement(department2, p3, false), new ResultElement(employee4, p4, false)));
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
        final Path p1 = new Path(model, "Company.name");
        final Path p2 = new Path(model, "Company.vatNumber");
        final Path p3 = new Path(model, "Company:departments.name");
        final Path p4 = new Path(model, "Company:contractors.name");
        List view = new ArrayList() {{
            add(p1);
            add(p2);
            add(p3);
            add(p4);
        }};
        pq.setViewPaths(view);
        ExportResultsIterator iter = new ExportResultsIterator(os, pq, new HashMap(), null);
        List got = new ArrayList();
        for (ResultsRow gotRow : new IteratorIterable<ResultsRow>(iter)) {
            got.add(gotRow);
        }
        List expected = Arrays.asList(Arrays.asList(new ResultElement(company1, p1, false), new ResultElement(company1, p2, false), new ResultElement(department1, p3, false), null),
                Arrays.asList(new ResultElement(company1, p1, false), new ResultElement(company1, p2, false), new ResultElement(department2, p3, false), null),
                Arrays.asList(new ResultElement(company1, p1, false), new ResultElement(company1, p2, false), null, new ResultElement(contractor1, p4, false)),
                Arrays.asList(new ResultElement(company1, p1, false), new ResultElement(company1, p2, false), null, new ResultElement(contractor2, p4, false)),
                Arrays.asList(new ResultElement(company1, p1, false), new ResultElement(company1, p2, false), null, new ResultElement(contractor3, p4, false)));
        assertEquals(expected, got);
    }
}
