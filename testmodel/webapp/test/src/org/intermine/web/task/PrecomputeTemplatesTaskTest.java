package org.intermine.web.task;

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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;

import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.web.logic.TemplateHelper;
import org.intermine.web.logic.TemplateQuery;

import java.io.InputStream;

import junit.framework.TestCase;

/**
 * Tests for PrecomputeTemplatesTask.
 *
 * @author Kim Rutherford
 */

//public class PrecomputeTemplatesTaskTest extends StoreDataTestCase
public class PrecomputeTemplatesTaskTest extends TestCase
{
    public PrecomputeTemplatesTaskTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    // test that correct query and list of indexes generate for pre-computing
    public void testPrecomputeTemplate() throws Exception {
        InputStream webProps = PrecomputeTemplatesTask.class
            .getClassLoader().getResourceAsStream("WEB-INF/web.properties");
        Properties properties = new Properties();
        properties.load(webProps);
        String user = properties.getProperty("superuser.account");

        PrecomputeTemplatesTask task = new PrecomputeTemplatesTask();
        task.setAlias("os.unittest");
        task.setUserProfileAlias("osw.userprofile-test");
        task.setUsername(user);

        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        Map templates = task.getPrecomputeTemplateQueries();
        TemplateQuery template = (TemplateQuery) templates.get("employeesOverACertainAgeFromDepartmentA");

        Query q = new Query();
        q.setDistinct(true);
        QueryClass qcEmp = new QueryClass(Employee.class);
        QueryField qfAge = new QueryField(qcEmp, "age");
        q.addFrom(qcEmp);
        QueryClass qcDept = new QueryClass(Department.class);
        q.addFrom(qcDept);
        QueryObjectReference deptRef = new QueryObjectReference(qcEmp, "department");
        ContainsConstraint cc = new ContainsConstraint(deptRef, ConstraintOp.CONTAINS, qcDept);
        QueryField qfName = new QueryField(qcDept, "name");
        q.addToSelect(qcEmp);
        q.addToSelect(qcDept);
        q.addToSelect(qfAge);
        q.addToSelect(qfName);
        q.setConstraint(cc);

        System.out.println("query: " + q);

        List indexes = new ArrayList();
        Query actualQ = TemplateHelper.getPrecomputeQuery(template, indexes, null);
        assertEquals(q.toString(), actualQ.toString());
        List expIndexes = new ArrayList(Arrays.asList(new Object[] {qfAge, qfName}));
        assertEquals(expIndexes.toString(), indexes.toString());

        task.precompute(os, actualQ, indexes, "template");
    }
}
