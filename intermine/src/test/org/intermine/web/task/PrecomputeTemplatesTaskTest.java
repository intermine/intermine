package org.intermine.web.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;

import junit.framework.*;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.io.InputStream;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.*;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.web.TemplateQuery;
import org.intermine.web.MainHelper;

import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Department;

/**
 * Tests for PrecomputeTemplatesTask.
 *
 * @author Kim Rutherford
 */

public class PrecomputeTemplatesTaskTest extends StoreDataTestCase
{
    public PrecomputeTemplatesTaskTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public void executeTest(String type) {

    }

    public static Test suite() {
        return buildSuite(PrecomputeTemplatesTaskTest.class);
    }

    // test that correct query and list of indexes generate for pre-computing
    public void testPrecomputeTemplate() throws Exception {
        PrecomputeTemplatesTask task = new PrecomputeTemplatesTask();
        Map templates = task.getPrecomputeTemplateQueries();
        TemplateQuery template = (TemplateQuery) templates.get("employeesOverACertainAgeFromDepartmentA");
        System.out.println(MainHelper.makeQuery(template.getQuery(), new HashMap(), new HashMap()));

        Query q = new Query();
        q.setDistinct(true);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryClass qcEmp = new QueryClass(Employee.class);
        QueryField qfAge = new QueryField(qcEmp, "age");
        q.addFrom(qcEmp);
        QueryClass qcDept = new QueryClass(Department.class);
        q.addFrom(qcDept);
        QueryObjectReference deptRef = new QueryObjectReference(qcEmp, "department");
        ContainsConstraint cc = new ContainsConstraint(deptRef, ConstraintOp.CONTAINS, qcDept);
        cs.addConstraint(cc);
        QueryField qfName = new QueryField(qcDept, "name");
        QueryFunction qf = new QueryFunction(qfName, QueryFunction.LOWER);
        SimpleConstraint sc =
            new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue("departmenta"));
        cs.addConstraint(sc);
        q.addToSelect(qcEmp);
        q.addToSelect(qfAge);
        q.setConstraint(cs);

        System.out.println("query: " + q);

        PrecomputeTemplatesTask.QueryAndIndexes expected = task.new QueryAndIndexes();
        expected.setQuery(q);
        expected.addIndex(qfAge);
        PrecomputeTemplatesTask.QueryAndIndexes qai = task.processTemplate(template);
        System.out.println("generate: " + qai.getQuery());
        assertEquals(expected.toString(), qai.toString());
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        System.out.println("generate: " + qai.getQuery());

        task.precompute(os, qai.getQuery(), qai.getIndexes());
    }

    public void testQueries() {

    }
}
