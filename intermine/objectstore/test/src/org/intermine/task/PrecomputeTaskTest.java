package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import junit.framework.Test;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.SqlGenerator;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryTestCase;

/**
 * Tests for PrecomputeTask.
 *
 * NOTE - this test depends on data being present in os.unittest which is
 * currently inserted before running the testmodel webapp tests.  If this
 * changes then this class will need to extend StoreDataTestCase.
 * @author Kim Rutherford
 */

public class PrecomputeTaskTest extends QueryTestCase
{
    ObjectStore os;

    public PrecomputeTaskTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();

        os = ObjectStoreFactory.getObjectStore("os.unittest");
    }

    public static Test suite() {
        return buildSuite(PrecomputeTaskTest.class);
    }

    /**
     * Test that PrecomputeTask creates the right pre-computed tables
     */
    public void testExecute() throws Exception {
        DummyPrecomputeTask task = new DummyPrecomputeTask();

        task.setAlias("os.unittest");
        task.setMinRows(new Integer(1));

        Properties summaryProperties;

        String configFile = "objectstoresummary.config.properties";

        InputStream is = PrecomputeTask.class.getClassLoader().getResourceAsStream(configFile);

        if (is == null) {
            throw new Exception("Cannot find " + configFile + " in the class path");
        }

        summaryProperties = new Properties();
        summaryProperties.load(is);

        task.execute();

        String[] expectedQueries = new String[] {
            "SELECT DISTINCT a1_, a2_, a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Employee AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a2_.employees CONTAINS a3_) ORDER BY a1_, a2_, a3_",
            "SELECT DISTINCT a1_, a2_, a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.CEO AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a2_.employees CONTAINS a3_) ORDER BY a1_, a2_, a3_",
            "SELECT DISTINCT a1_, a2_, a3_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Manager AS a3_ WHERE (a1_.departments CONTAINS a2_ AND a2_.employees CONTAINS a3_) ORDER BY a1_, a2_, a3_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE a1_.departments CONTAINS a2_ ORDER BY a1_, a2_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a2_ WHERE a1_.address CONTAINS a2_ ORDER BY a1_, a2_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a2_ WHERE a1_.employees CONTAINS a2_ ORDER BY a1_, a2_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a2_ WHERE a1_.employees CONTAINS a2_ ORDER BY a1_, a2_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a2_ WHERE a1_.employees CONTAINS a2_ ORDER BY a1_, a2_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.HasSecretarys AS a1_, org.intermine.model.testmodel.Secretary AS a2_ WHERE a1_.secretarys CONTAINS a2_ ORDER BY a1_, a2_",
            "SELECT DISTINCT emp, add FROM org.intermine.model.testmodel.Employee AS emp, org.intermine.model.testmodel.Address AS add WHERE emp.address CONTAINS add",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a2_ WHERE a1_.employees CONTAINS a2_"
        };
  
        String[] expectedSql = new String[] {
            "SELECT a1_.CEOId AS a1_CEOId, a1_.addressId AS a1_addressId, a1_.id AS a1_id, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.companyId AS a2_companyId, a2_.id AS a2_id, a2_.managerId AS a2_managerId, a2_.name AS a2_name, a3_.addressId AS a3_addressId, a3_.age AS a3_age, a3_.departmentId AS a3_departmentId, a3_.departmentThatRejectedMeId AS a3_departmentThatRejectedMeId, a3_.fullTime AS a3_fullTime, a3_.id AS a3_id, a3_.intermine_end AS a3_intermine_end, a3_.name AS a3_name FROM Company AS a1_, Department AS a2_, Employee AS a3_ WHERE a1_.id = a2_.companyId AND a2_.id = a3_.departmentId ORDER BY a1_.id, a2_.id, a3_.id",
            "SELECT a1_.CEOId AS a1_CEOId, a1_.addressId AS a1_addressId, a1_.id AS a1_id, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.companyId AS a2_companyId, a2_.id AS a2_id, a2_.managerId AS a2_managerId, a2_.name AS a2_name, a3_.addressId AS a3_addressId, a3_.age AS a3_age, a3_.companyId AS a3_companyId, a3_.departmentId AS a3_departmentId, a3_.departmentThatRejectedMeId AS a3_departmentThatRejectedMeId, a3_.fullTime AS a3_fullTime, a3_.id AS a3_id, a3_.intermine_end AS a3_intermine_end, a3_.name AS a3_name, a3_.salary AS a3_salary, a3_.seniority AS a3_seniority, a3_.title AS a3_title FROM Company AS a1_, Department AS a2_, CEO AS a3_ WHERE a1_.id = a2_.companyId AND a2_.id = a3_.departmentId ORDER BY a1_.id, a2_.id, a3_.id",
            "SELECT a1_.CEOId AS a1_CEOId, a1_.addressId AS a1_addressId, a1_.id AS a1_id, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.companyId AS a2_companyId, a2_.id AS a2_id, a2_.managerId AS a2_managerId, a2_.name AS a2_name, a3_.addressId AS a3_addressId, a3_.age AS a3_age, a3_.departmentId AS a3_departmentId, a3_.departmentThatRejectedMeId AS a3_departmentThatRejectedMeId, a3_.fullTime AS a3_fullTime, a3_.id AS a3_id, a3_.intermine_end AS a3_intermine_end, a3_.name AS a3_name, a3_.seniority AS a3_seniority, a3_.title AS a3_title FROM Company AS a1_, Department AS a2_, Manager AS a3_ WHERE a1_.id = a2_.companyId AND a2_.id = a3_.departmentId ORDER BY a1_.id, a2_.id, a3_.id",
            "SELECT a1_.CEOId AS a1_CEOId, a1_.addressId AS a1_addressId, a1_.id AS a1_id, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.companyId AS a2_companyId, a2_.id AS a2_id, a2_.managerId AS a2_managerId, a2_.name AS a2_name FROM Company AS a1_, Department AS a2_ WHERE a1_.id = a2_.companyId ORDER BY a1_.id, a2_.id",
            "SELECT a1_.CEOId AS a1_CEOId, a1_.addressId AS a1_addressId, a1_.id AS a1_id, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.address AS a2_address, a2_.id AS a2_id FROM Company AS a1_, Address AS a2_ WHERE a1_.addressId = a2_.id ORDER BY a1_.id, a2_.id",
            "SELECT a1_.companyId AS a1_companyId, a1_.id AS a1_id, a1_.managerId AS a1_managerId, a1_.name AS a1_name, a2_.addressId AS a2_addressId, a2_.age AS a2_age, a2_.departmentId AS a2_departmentId, a2_.departmentThatRejectedMeId AS a2_departmentThatRejectedMeId, a2_.fullTime AS a2_fullTime, a2_.id AS a2_id, a2_.intermine_end AS a2_intermine_end, a2_.name AS a2_name FROM Department AS a1_, Employee AS a2_ WHERE a1_.id = a2_.departmentId ORDER BY a1_.id, a2_.id",
            "SELECT a1_.companyId AS a1_companyId, a1_.id AS a1_id, a1_.managerId AS a1_managerId, a1_.name AS a1_name, a2_.addressId AS a2_addressId, a2_.age AS a2_age, a2_.companyId AS a2_companyId, a2_.departmentId AS a2_departmentId, a2_.departmentThatRejectedMeId AS a2_departmentThatRejectedMeId, a2_.fullTime AS a2_fullTime, a2_.id AS a2_id, a2_.intermine_end AS a2_intermine_end, a2_.name AS a2_name, a2_.salary AS a2_salary, a2_.seniority AS a2_seniority, a2_.title AS a2_title FROM Department AS a1_, CEO AS a2_ WHERE a1_.id = a2_.departmentId ORDER BY a1_.id, a2_.id",
            "SELECT a1_.companyId AS a1_companyId, a1_.id AS a1_id, a1_.managerId AS a1_managerId, a1_.name AS a1_name, a2_.addressId AS a2_addressId, a2_.age AS a2_age, a2_.departmentId AS a2_departmentId, a2_.departmentThatRejectedMeId AS a2_departmentThatRejectedMeId, a2_.fullTime AS a2_fullTime, a2_.id AS a2_id, a2_.intermine_end AS a2_intermine_end, a2_.name AS a2_name, a2_.seniority AS a2_seniority, a2_.title AS a2_title FROM Department AS a1_, Manager AS a2_ WHERE a1_.id = a2_.departmentId ORDER BY a1_.id, a2_.id",
            "SELECT a1_.id AS a1_id, a2_.id AS a2_id, a2_.name AS a2_name FROM HasSecretarys AS a1_, Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE a1_.id = indirect0.Secretarys AND indirect0.HasSecretarys = a2_.id ORDER BY a1_.id, a2_.id",
            "SELECT emp.addressId AS empaddressId, emp.age AS empage, emp.departmentId AS empdepartmentId, emp.departmentThatRejectedMeId AS empdepartmentThatRejectedMeId, emp.fullTime AS empfullTime, emp.id AS empid, emp.intermine_end AS empintermine_end, emp.name AS empname, intermine_add.address AS intermine_addaddress, intermine_add.id AS intermine_addid FROM Employee AS emp, Address AS intermine_add WHERE emp.addressId = intermine_add.id ORDER BY emp.id, intermine_add.id",
            "SELECT a1_.companyId AS a1_companyId, a1_.id AS a1_id, a1_.managerId AS a1_managerId, a1_.name AS a1_name, a2_.addressId AS a2_addressId, a2_.age AS a2_age, a2_.departmentId AS a2_departmentId, a2_.departmentThatRejectedMeId AS a2_departmentThatRejectedMeId, a2_.fullTime AS a2_fullTime, a2_.id AS a2_id, a2_.intermine_end AS a2_intermine_end, a2_.name AS a2_name FROM Department AS a1_, Employee AS a2_ WHERE a1_.id = a2_.departmentId ORDER BY a1_.id, a2_.id"
        };

        assertEquals(expectedQueries.length, task.queries.size());

        for (int i = 0; i < expectedQueries.length; i++) {
            Query generatedQuery = (Query) task.queries.get(i);
            assertEquals(expectedQueries[i], "" + generatedQuery);
            String generatedSql = SqlGenerator.generate(generatedQuery, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).getDatabase(), null, 4, Collections.EMPTY_MAP);
            assertEquals(expectedSql[i], generatedSql);
        }
    }
 
    public void testConstructQueries() throws Exception {
        PrecomputeTask pt = new PrecomputeTask();
        pt.os = os;
        List actual = pt.constructQueries("Employee department Department", true);


        QueryClass qcEmpl = new QueryClass(Employee.class);
        QueryClass qcDept = new QueryClass(Department.class);
        QueryObjectReference ref = new QueryObjectReference(qcEmpl, "department");
        ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcDept);

        Query q1 = new Query();
        q1.addToSelect(qcEmpl);
        q1.addFrom(qcEmpl);
        q1.addToSelect(qcDept);
        q1.addFrom(qcDept);
        q1.setConstraint(cc);
        q1.addToOrderBy(qcEmpl);
        q1.addToOrderBy(qcDept);

        Query q2 = new Query();
        q2.addToSelect(qcEmpl);
        q2.addFrom(qcEmpl);
        q2.addToSelect(qcDept);
        q2.addFrom(qcDept);
        q2.setConstraint(cc);
        q2.addToOrderBy(qcDept);
        q2.addToOrderBy(qcEmpl);

        // List of queries in both possible orders
        List expected = new ArrayList(Arrays.asList(new Query[] {q2, q1}));

        compareQueryLists(expected, actual);
    }


    public void testGetOrderedQueries() throws Exception {
        PrecomputeTask pt = new PrecomputeTask();
        pt.os = os;

        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        QueryClass qc2 = new QueryClass(Company.class);

        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc2);

        q1.addToOrderBy(qc1);
        q1.addToOrderBy(qc2);

        Query q2 = new Query();
        q2.addToSelect(qc1);
        q2.addFrom(qc1);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        q2.addToOrderBy(qc2);
        q2.addToOrderBy(qc1);

        assertEquals(2, pt.getOrderedQueries(q1).size());

        List queries = new ArrayList(Arrays.asList(new Object[] {q2, q1}));
        compareQueryLists(queries, pt.getOrderedQueries(q1));
    }

    
    protected void compareQueryLists(List a, List b) {
        assertEquals(a.size(), b.size());
        for (int i = 0; i< a.size(); i++) {
            assertEquals((Query) a.get(i), (Query) b.get(i));
        }
    }

    class DummyPrecomputeTask extends PrecomputeTask {
        protected List queries = new ArrayList();
        protected void precompute(Query query, Collection indexes) {
            queries.add(query);
        }
    }
}
