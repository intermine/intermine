package org.intermine.web.results;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.iql.IqlQuery;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.path.Path;
import org.intermine.util.DynamicUtil;

import junit.framework.TestCase;

/**
 * Tests for the WebResults class
 *
 * @author Kim Rutherford
 */

public class WebResultsTest extends TestCase
{
    private Results results;
    private Company company1;
    private Company company2;
    private Company company3;
    private Department department1;
    private Department department2;
    private Department department3;
    private Manager man1;
    private Manager man2;
    private Manager man3;
    private WebResults webResults;
    
    public WebResultsTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setResultsSize(15);
        IqlQuery fq =
            new IqlQuery("SELECT DISTINCT a1_, a3_, a4_ FROM org.intermine.model.testmodel.Department AS a1_," +
                    " org.intermine.model.testmodel.CEO AS a2_, org.intermine.model.testmodel.Company " +
                    "AS a3_, org.intermine.model.testmodel.Manager AS a4_ " +
                    "WHERE (a1_.manager CONTAINS a2_ AND a2_.company CONTAINS a3_ " +
                    "AND a1_.employees CONTAINS a4_)", 
                    "org.intermine.model.testmodel");
/*        "SELECT DISTINCT a3_, a4_ FROM org.intermine.model.testmodel.Department AS a1_, " +
        "org.intermine.model.testmodel.CEO AS a2_, org.intermine.model.testmodel.Company AS a3_, " +
        "org.intermine.model.testmodel.Manager AS a4_ " +
        "WHERE (a1_.manager CONTAINS a2_ AND a2_.company CONTAINS a3_ AND a1_.employees CONTAINS a4_)";
*/
        Query query = fq.toQuery();
        results = os.execute(query);

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
        man3 = (Manager) DynamicUtil.createObject(Collections.singleton(Manager.class));
        man3.setName("Manager3");
        man3.setSeniority(new Integer(300));
        man3.setId(new Integer(3));

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

        final Model model = Model.getInstanceByName("testmodel");
        List view = new ArrayList() {{ // see: http://www.c2.com/cgi/wiki?DoubleBraceInitialization
            add(new Path(model, "Department.name"));
            add(new Path(model, "Department.manager[CEO].company.name"));
            add(new Path(model, "Department.manager[CEO].company.vatNumber"));
            add(new Path(model, "Department.employees[Manager].seniority"));
        }};
        Map pathToQueryNode = new HashMap();
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
        
        Map classKeys = new HashMap();
        FieldDescriptor fd = model.getClassDescriptorByName("org.intermine.model.testmodel.Company").getFieldDescriptorByName("name");
        HashSet keys = new HashSet();
        keys.add(new HashSet(Arrays.asList(new Object[]{fd})));
        classKeys.put("Company", keys);
        webResults = new WebResults(view, results, model, pathToQueryNode, classKeys);
    }
    
    public void test() {
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





























