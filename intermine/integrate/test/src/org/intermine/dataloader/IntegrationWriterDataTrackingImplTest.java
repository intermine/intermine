package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Bank;
import org.intermine.model.testmodel.Broke;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.SetupDataTestCase;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.testing.OneTimeTestCase;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IntToIntMap;

public class IntegrationWriterDataTrackingImplTest extends SetupDataTestCase
{
    protected static ObjectStoreWriter writer;
    protected static ObjectStore os;
    protected static IntegrationWriterDataTrackingImpl iw;
    protected boolean doIds;

    public IntegrationWriterDataTrackingImplTest(String arg) {
        super(arg);
        doIds = true;
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(IntegrationWriterDataTrackingImplTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        strictTestQueries = false;
        if (iw.isInTransaction()) {
            iw.abortTransaction();
        }
        storeData();
        iw.reset();
        iw.skeletons.clear();
        iw.beginTransaction();
        //iw.setEof(new HintingFetcher(iw.getObjectStoreWriter().getObjectStore(), iw));
    }

    public void tearDown() throws Exception {
        super.tearDown();
        iw.commitTransaction();
        iw.getDataTracker().clear();
        removeDataFromStore();
        iw.reset();
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();
        iw = (IntegrationWriterDataTrackingImpl) IntegrationWriterFactory.getIntegrationWriter("integration.unittestmulti");
        writer = (ObjectStoreWriterInterMineImpl) iw.getObjectStoreWriter();
        os = iw.getObjectStore();
    }

    public static void oneTimeTearDown() throws Exception {
        iw.close();
        SetupDataTestCase.oneTimeTearDown();
    }

    public static void storeData() throws Exception {
        if (iw == null) {
            throw new NullPointerException("iw must be set before trying to store data");
        }
        long start = new Date().getTime();

        try {
            iw.beginTransaction();

            Source source = iw.getMainSource("storedata");
            Source skelSource = iw.getSkeletonSource("storedata");

            //DataTracking.precacheObjects(new HashSet(data.values()), iw.getDataTracker());
            Iterator iter = data.entrySet().iterator();
            while (iter.hasNext()) {
                InterMineObject o = (InterMineObject) ((Map.Entry) iter.next())
                    .getValue();
                iw.store(o, source, skelSource);
            }
            //DataTracking.releasePrecached(iw.getDataTracker());

            iw.commitTransaction();
        } catch (Exception e) {
            iw.abortTransaction();
            throw new Exception(e);
        }

        System.out.println("Took " + (new Date().getTime() - start) + " ms to set up data and VACUUM ANALYZE");
    }

    public static void removeDataFromStore() throws Exception {
        removeDataFromStore(writer);
    }

    public static void removeDataFromStore(ObjectStoreWriter writer) throws Exception {
        System.out.println("Removing data from store");
        long start = new Date().getTime();
        if (writer == null) {
            throw new NullPointerException("writer must be set before trying to remove data");
        }
        try {
            writer.beginTransaction();
            Query q = new Query();
            QueryClass qc = new QueryClass(InterMineObject.class);
            q.addFrom(qc);
            q.addToSelect(qc);
            Set dataToRemove = writer.getObjectStore().executeSingleton(q);
            Iterator iter = dataToRemove.iterator();
            while (iter.hasNext()) {
                InterMineObject toDelete = (InterMineObject) iter.next();
                writer.delete(toDelete);
            }
            writer.commitTransaction();
        } catch (Exception e) {
            writer.abortTransaction();
            throw e;
        }
        System.out.println("Took " + (new Date().getTime() - start) + " ms to remove data from store");
    }




    // Not doing the Query tests here
    public void executeTest(String type) throws Exception {
    }

    public void testStoreObject() throws Exception {
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Address a = new Address();
        a.setAddress("Company Street, AVille");
        c.setAddress(a);
        c.setName("CompanyC");
        c.setVatNumber(100);

        if (doIds) {
            c.setId(new Integer(1));
            a.setId(new Integer(2));
        }
        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(c, source, skelSource);  // method we are testing

        Company example = (Company) iw.getObjectByExample(c, Collections.singleton("name"));
        assertNotNull("example from db should not be null", example);

        assertEquals(c.getVatNumber(), example.getVatNumber());
        assertEquals(c.getName(), example.getName());
        assertNotNull(example.getAddress());
        assertEquals(c.getAddress().getAddress(), example.getAddress().getAddress());

        Company c2 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c2.setName("CompanyA");
        Company example2 = (Company) iw.getObjectByExample(c2, Collections.singleton("name"));
        assertEquals(example.getAddress(), example2.getAddress());
    }

    public void testUpdateObjectField() throws Exception {
        Employee e = new Employee();
        Department d = new Department();
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        e.setName("EmployeeA2");
        e.setAge(32);
        e.setEnd("2");
        e.setFullTime(true);
        e.setDepartment(d);
        d.setName("DepartmentA1");
        d.setCompany(c);
        c.setVatNumber(1234);

        if (doIds) {
            e.setId(new Integer(1));
            d.setId(new Integer(2));
            c.setId(new Integer(3));
        }
        Source source = iw.getMainSource("testsource3");
        Source skelSource = iw.getSkeletonSource("testsource3");

        iw.store(e, source, skelSource);
        iw.store(d, source, skelSource);
        iw.store(c, source, skelSource);

        Employee re = (Employee) iw.getObjectByExample(e, Collections.singleton("name"));
        assertNotNull("Object from db should not be null", re);
        assertEquals(32, re.getAge());
        assertEquals("2", re.getEnd());
        assertTrue(re.getFullTime());
        assertNotNull(re.getAddress());
        assertNotNull(re.getDepartment());
    }

    public void testUpdateObjectOneToOne() throws Exception {
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Address a = new Address();
        Address a2 = new Address();
        CEO ceo = new CEO();
        a.setAddress("Company Street, AVille");
        c.setAddress(a);
        c.setName("CompanyA");
        c.setVatNumber(100);
        c.setcEO(ceo);
        a2.setAddress("Employee Street, BVille");
        ceo.setName("EmployeeB1");
        ceo.setSeniority(new Integer(76321));
        ceo.setFullTime(true);
        ceo.setSalary(45000);
        ceo.setAge(40);
        ceo.setCompany(c);
        ceo.setAddress(a2);

        if (doIds) {
            c.setId(new Integer(1));
            a.setId(new Integer(2));
            a2.setId(new Integer(3));
            ceo.setId(new Integer(4));
        }
        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(c, source, skelSource);  // method we are testing

        Company rc = (Company) iw.getObjectByExample(c, Collections.singleton("name"));
        assertNotNull("Object from db should not be null", rc);

        assertEquals(c.getVatNumber(), rc.getVatNumber());
        CEO rceo = rc.getcEO();
        assertNotNull(rceo);
        assertEquals(ceo.getName(), rceo.getName());
        assertNotNull(rceo.getCompany());
        assertEquals(rc, rceo.getCompany());

        Company exampleOC = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        exampleOC.setName("CompanyB");
        Company oc = (Company) iw.getObjectByExample(exampleOC, Collections.singleton("name"));

        assertNotNull(oc);
        assertNull(oc.getcEO());
    }

    public void testUpdateObjectOneToOne2() throws Exception {
        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        {
            Company companyA = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
            CEO ceoA = new CEO();
            Address companyAAddress = new Address();
            Address ceoAAddress = new Address();
            companyAAddress.setAddress("Company Street, AVille");
            companyA.setAddress(companyAAddress);
            companyA.setName("CompanyA");
            companyA.setVatNumber(1234);
            companyA.setcEO(ceoA);
            ceoAAddress.setAddress("Employee Street, AVille");
            ceoA.setAddress(ceoAAddress);
            ceoA.setSeniority(new Integer(876234));
            ceoA.setName("Fred");
            ceoA.setFullTime(false);
            ceoA.setSalary(1);
            ceoA.setAge(101);
            ceoA.setCompany(companyA);

            if (doIds) {
                companyA.setId(new Integer(1));
                ceoA.setId(new Integer(2));
                companyAAddress.setId(new Integer(3));
                ceoAAddress.setId(new Integer(4));
            }

            iw.store(companyA, source, skelSource);
            iw.store(ceoA, source, skelSource);
            iw.store(companyAAddress, source, skelSource);
            iw.store(ceoAAddress, source, skelSource);
        }

        Company exampleCompanyA = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        exampleCompanyA.setName("CompanyA");
        Company exampleCompanyB = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        exampleCompanyB.setName("CompanyB");
        CEO exampleCEOA = new CEO();
        exampleCEOA.setName("Fred");
        CEO exampleCEOB = new CEO();
        exampleCEOB.setName("EmployeeB1");

        Company rCompanyA = (Company) iw.getObjectByExample(exampleCompanyA, Collections.singleton("name"));
        Company rCompanyB = (Company) iw.getObjectByExample(exampleCompanyB, Collections.singleton("name"));
        CEO rCEOA = (CEO) iw.getObjectByExample(exampleCEOA, Collections.singleton("name"));
        CEO rCEOB = (CEO) iw.getObjectByExample(exampleCEOB, Collections.singleton("name"));

        assertNotNull(rCompanyA);
        assertNotNull(rCompanyB);
        assertNotNull(rCEOA);
        assertNotNull(rCEOB);
        assertEquals(rCompanyA, rCEOA.getCompany());
        assertEquals(rCompanyB, rCEOB.getCompany());
        assertEquals(rCEOA, rCompanyA.getcEO());
        assertEquals(rCEOB, rCompanyB.getcEO());

        Source source2 = iw.getMainSource("testsource2");
        Source skelSource2 = iw.getSkeletonSource("testsource2");
        {
            Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
            Address a = new Address();
            Address a2 = new Address();
            CEO ceo = new CEO();
            a.setAddress("Company Street, AVille");
            c.setAddress(a);
            c.setName("CompanyA");
            c.setVatNumber(100);
            c.setcEO(ceo);
            a2.setAddress("Employee Street, BVille");
            ceo.setName("EmployeeB1");
            ceo.setSeniority(new Integer(76321));
            ceo.setFullTime(true);
            ceo.setSalary(45000);
            ceo.setAge(40);
            ceo.setCompany(c);
            ceo.setAddress(a2);

            if (doIds) {
                c.setId(new Integer(1));
                a.setId(new Integer(2));
                a2.setId(new Integer(3));
                ceo.setId(new Integer(4));
            }

            iw.commitTransaction();
            iw.reset();
            iw.beginTransaction();
            iw.store(c, source2, skelSource2); // method we are testing
            //          CompanyA ------- CEOA            CompanyA --.   - CEOA
            // Change                             to                 \
            //          CompanyB ------- CEOB            CompanyB -   `-- CEOB
        }

        rCompanyA = (Company) iw.getObjectByExample(exampleCompanyA, Collections.singleton("name"));
        rCompanyB = (Company) iw.getObjectByExample(exampleCompanyB, Collections.singleton("name"));
        rCEOA = (CEO) iw.getObjectByExample(exampleCEOA, Collections.singleton("name"));
        rCEOB = (CEO) iw.getObjectByExample(exampleCEOB, Collections.singleton("name"));

        assertNotNull(rCompanyA);
        assertNotNull(rCompanyB);
        assertNotNull(rCEOA);
        assertNotNull(rCEOB);
        assertEquals(null, rCEOA.getCompany());
        assertEquals(rCompanyA, rCEOB.getCompany());
        assertEquals(rCEOB, rCompanyA.getcEO());
        assertEquals(null, rCompanyB.getcEO());
    }

    public void testUpdateObjectOneToOneNull() throws Exception {
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Address a = new Address();
        a.setAddress("Company Street, BVille");
        c.setAddress(a);
        c.setName("CompanyB");
        c.setVatNumber(100);

        if (doIds) {
            c.setId(new Integer(1));
            a.setId(new Integer(2));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(c, source, skelSource); // method we are testing

        Company result = (Company) iw.getObjectByExample(c, Collections.singleton("name"));
        assertNotNull("Object from db should not be null", result);

        assertEquals(c.getVatNumber(), result.getVatNumber());
        assertNull(result.getcEO());

        CEO ceo = new CEO();
        ceo.setName("EmployeeB1");
        CEO result2 = (CEO) iw.getObjectByExample(ceo, Collections.singleton("name"));
        assertNotNull(result2);
        assertNull(result2.getCompany());
    }

    public void testUpdateObjectManyToOne() throws Exception {
        Manager e = new Manager();
        Department d = new Department();
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Address a = new Address();
        Address a2 = new Address();
        a.setAddress("Company Street, BVille");
        c.setName("CompanyB");
        c.setAddress(a);
        c.addDepartments(d);
        d.setName("DepartmentB1");
        d.setCompany(c);
        e.setName("EmployeeA1");
        e.setDepartment(d);
        d.addEmployees(e);
        e.setAge(10);
        e.setFullTime(true);
        e.setSeniority(new Integer(876123));
        a2.setAddress("Employee Street, AVille");
        e.setAddress(a2);

        if (doIds) {
            e.setId(new Integer(1));
            d.setId(new Integer(2));
            c.setId(new Integer(3));
            a.setId(new Integer(4));
            a2.setId(new Integer(5));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(e, source, skelSource);  // method we are testing

        Employee re = (Employee) iw.getObjectByExample(e, Collections.singleton("name"));
        assertNotNull(re);

        Department rd = re.getDepartment();

        assertNotNull(rd);                          // Employee has a department
        assertEquals(d.getName(), rd.getName());    // Department is the right one

        assertTrue(rd.getEmployees().contains(re)); // And that department has the employee

        Department exampleOD = new Department();
        exampleOD.setName("DepartmentA1");
        Department od = (Department) iw.getObjectByExample(exampleOD, Collections.singleton("name"));

        assertNotNull(od);                          // The old department exists
        assertTrue(!od.getEmployees().contains(re));// And does not have the employee
    }

    public void testUpdateObjectManyToOneNull() throws Exception {
        Manager e = new Manager();
        Address a2 = new Address();
        e.setName("EmployeeA1");
        e.setDepartment(null);
        e.setAge(10);
        e.setFullTime(true);
        e.setSeniority(new Integer(876123));
        a2.setAddress("Employee Street, AVille");
        e.setAddress(a2);

        if (doIds) {
            e.setId(new Integer(1));
            a2.setId(new Integer(2));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(e, source, skelSource);  // method we are testing

        Employee re = (Employee) iw.getObjectByExample(e, Collections.singleton("name"));
        assertNotNull(re);

        assertNull(re.getDepartment());             // Employee no longer has a department

        Department exampleOD = new Department();
        exampleOD.setName("DepartmentA1");
        Department od = (Department) iw.getObjectByExample(exampleOD, Collections.singleton("name"));

        assertNotNull(od);                          // The old department exists
        assertTrue(!od.getEmployees().contains(re));// And does not have the employee
    }

    public void testUpdateObjectOneToMany() throws Exception {
        Manager e = new Manager();
        Department d = new Department();
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Address a = new Address();
        Address a2 = new Address();
        a.setAddress("Company Street, BVille");
        c.setName("CompanyB");
        c.setAddress(a);
        c.addDepartments(d);
        d.setName("DepartmentB1");
        d.setCompany(c);
        d.addEmployees(e);
        e.setDepartment(d);
        e.setName("EmployeeA1");
        e.setAge(10);
        e.setFullTime(true);
        e.setSeniority(new Integer(876123));
        a2.setAddress("Employee Street, AVille");
        e.setAddress(a2);

        if (doIds) {
            e.setId(new Integer(1));
            d.setId(new Integer(2));
            c.setId(new Integer(3));
            a.setId(new Integer(4));
            a2.setId(new Integer(5));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(d, source, skelSource);  // method we are testing

        Department rd = (Department) iw.getObjectByExample(d, Collections.singleton("name"));
        Employee re = (Employee) iw.getObjectByExample(e, Collections.singleton("name"));
        assertNotNull(rd);
        assertNotNull(re);

        //   ----------  NOTE - don't uncomment these tests. They don't work, and are not meant to,
        //   because the Integration Writer assumes that you will later on go and store the rest of
        //   the objects (and sort out the mess).

        //assertTrue(rd.getEmployees().contains(re)); // Department has the employee
        //assertEquals(re.getDepartment(), rd);       // And Employees department is the right one

        //Department exampleOD = new Department();
        //exampleOD.setName("DepartmentA1");
        //Department od = (Department) iw.getObjectByExample(exampleOD, Collections.singleton("name"));

        //assertNotNull(od);                          // The old department exists
        //assertTrue(!od.getEmployees().contains(re));// And does not have the employee
    }

    public void testUpdateObjectManyToMany() throws Exception {
        Contractor con = new Contractor();
        Address companyAAddress = new Address();
        Company companyA = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        companyAAddress.setAddress("Company Street, AVille");
        companyA.setAddress(companyAAddress);
        companyA.setName("CompanyA");
        con.setName("ContractorZ");
        con.addCompanys(companyA);
        companyA.addContractors(con);

        if (doIds) {
            con.setId(new Integer(1));
            companyAAddress.setId(new Integer(2));
            companyA.setId(new Integer(3));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(con, source, skelSource);  // method we are testing

        Company rca = (Company) iw.getObjectByExample(companyA, Collections.singleton("name"));
        Contractor rcon = (Contractor) iw.getObjectByExample(con, Collections.singleton("name"));
        assertNotNull(rca);
        assertNotNull(rcon);

        assertTrue(rca.getContractors().contains(rcon));
        assertTrue(rcon.getCompanys().contains(rca));

        Source source2 = iw.getMainSource("testsource2");
        Source skelSource2 = iw.getSkeletonSource("testsource2");

        Address companyBAddress = new Address();
        Company companyB = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        companyBAddress.setAddress("Company Street, BVille");
        companyB.setAddress(companyBAddress);
        companyB.setName("CompanyB");
        con.addCompanys(companyB);
        companyB.addContractors(con);

        if (doIds) {
            con.setId(new Integer(1));
            companyBAddress.setId(new Integer(4));
            companyB.setId(new Integer(5));
        }

        iw.commitTransaction();
        iw.reset();
        iw.beginTransaction();
        iw.store(con, source2, skelSource2);

        rca = (Company) iw.getObjectByExample(companyA, Collections.singleton("name"));
        Company rcb = (Company) iw.getObjectByExample(companyB, Collections.singleton("name"));
        rcon = (Contractor) iw.getObjectByExample(con, Collections.singleton("name"));
        assertNotNull(rca);
        assertNotNull(rcb);
        assertNotNull(rcon);

        assertTrue(rca.getContractors().contains(rcon));
        assertTrue(rcon.getCompanys().contains(rca));
        assertTrue(rcb.getContractors().contains(rcon));
        assertTrue(rcon.getCompanys().contains(rcb));
    }

    public void testUpdateObjectManyToManyWithMerge() throws Exception {
        // Add a duplicate CompanyA and ContractorA, plus a ContractorD only attached to the duplicate CompanyA, and a ContractorC only attached to the original CompanyA.
        Address exampleCAA = new Address();
        exampleCAA.setAddress("Company Street, AVille");
        Address dbCAA = (Address) iw.getObjectByExample(exampleCAA, Collections.singleton("address"));
        Company ca = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        ca.setAddress(dbCAA);
        ca.setName("CompanyA");
        Contractor exampleConA = new Contractor();
        exampleConA.setName("ContractorA");
        Contractor dbConA = (Contractor) iw.getObjectByExample(exampleConA, Collections.singleton("name"));
        ca.addContractors(dbConA);
        Contractor exampleConB = new Contractor();
        exampleConB.setName("ContractorB");
        Contractor dbConB = (Contractor) iw.getObjectByExample(exampleConB, Collections.singleton("name"));
        ca.addContractors(dbConB);

        Contractor conA = new Contractor();
        ca.addContractors(conA);
        conA.setName("ContractorA");
        conA.setSeniority(new Integer(128764));
        conA.addCompanys(ca);
        Company exampleCA = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        exampleCA.setName("CompanyA");
        Company dbCA = (Company) iw.getObjectByExample(exampleCA, Collections.singleton("name"));
        conA.addCompanys(dbCA);
        Company exampleCB = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        exampleCB.setName("CompanyB");
        Company dbCB = (Company) iw.getObjectByExample(exampleCB, Collections.singleton("name"));
        conA.addCompanys(dbCB);

        Contractor conC = new Contractor();
        conC.setName("ContractorC");
        conC.setSeniority(new Integer(2784112));
        conC.addCompanys(dbCA);

        Contractor conD = new Contractor();
        conD.setName("ContractorD");
        conD.setSeniority(new Integer(276423341));
        conD.addCompanys(ca);

        if (doIds) {
            ca.setId(new Integer(1));
            conA.setId(new Integer(2));
            conC.setId(new Integer(3));
            conD.setId(new Integer(4));
        }

        iw.store(ca);
        iw.store(conA);
        iw.store(conC);
        iw.store(conD);

        Source source = iw.getMainSource("testsource");

        DataTracker dataTracker = iw.getDataTracker();
        dataTracker.clearObj(ca.getId());
        dataTracker.setSource(ca.getId(), "name", source);
        dataTracker.setSource(ca.getId(), "address", source);
        dataTracker.setSource(ca.getId(), "vatNumber", source);
        dataTracker.setSource(ca.getId(), "CEO", source);
        dataTracker.clearObj(conA.getId());
        dataTracker.setSource(conA.getId(), "personalAddress", source);
        dataTracker.setSource(conA.getId(), "businessAddress", source);
        dataTracker.setSource(conA.getId(), "name", source);
        dataTracker.setSource(conA.getId(), "seniority", source);
        dataTracker.clearObj(conC.getId());
        dataTracker.setSource(conC.getId(), "personalAddress", source);
        dataTracker.setSource(conC.getId(), "businessAddress", source);
        dataTracker.setSource(conC.getId(), "name", source);
        dataTracker.setSource(conC.getId(), "seniority", source);
        dataTracker.clearObj(conD.getId());
        dataTracker.setSource(conD.getId(), "personalAddress", source);
        dataTracker.setSource(conD.getId(), "businessAddress", source);
        dataTracker.setSource(conD.getId(), "name", source);
        dataTracker.setSource(conD.getId(), "seniority", source);

        // Now set up a standard store operation that will set off a object merge.
        Contractor con = new Contractor();
        Address companyAAddress = new Address();
        Company companyA = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        companyAAddress.setAddress("Company Street, AVille");
        companyA.setAddress(companyAAddress);
        companyA.setName("CompanyA");
        con.setName("ContractorZ");
        con.addCompanys(companyA);
        companyA.addContractors(con);

        if (doIds) {
            con.setId(new Integer(5));
            companyAAddress.setId(new Integer(6));
            companyA.setId(new Integer(7));
        }
        Source source2 = iw.getMainSource("testsource2");
        Source skelSource2 = iw.getSkeletonSource("testsource2");

        // Make sure there are currently multiple copies of CompanyA and ContractorA.
        try {
            Company rca = (Company) iw.getObjectByExample(companyA, Collections.singleton("name"));
            fail("Expected an exception, because there are multiple objects matching this pattern");
        } catch (IllegalArgumentException e) {
        }

        try {
            Contractor rconA = (Contractor) iw.getObjectByExample(conA, Collections.singleton("name"));
            fail("Expected an exception, because there are multiple objects matching this pattern");
        } catch (IllegalArgumentException e) {
        }

        iw.commitTransaction();
        iw.reset();
        iw.beginTransaction();
        iw.store(con, source2, skelSource2); // method we are testing

        // Get objects (and test that there is only one copy of everything).
        Company rca = (Company) iw.getObjectByExample(companyA, Collections.singleton("name"));
        Contractor rconC = (Contractor) iw.getObjectByExample(conC, Collections.singleton("name"));
        Contractor rconD = (Contractor) iw.getObjectByExample(conD, Collections.singleton("name"));
        Contractor rconZ = (Contractor) iw.getObjectByExample(con, Collections.singleton("name"));
        assertNotNull(rca);
        assertNotNull(rconC);
        assertNotNull(rconD);
        assertNotNull(rconZ);

        // Test that everything is in the right collections.
        assertTrue(rca.getContractors().contains(rconC));
        assertTrue(rca.getContractors().contains(rconD));
        assertTrue(rca.getContractors().contains(rconZ));
        assertTrue(rconC.getCompanys().contains(rca));
        assertTrue(rconD.getCompanys().contains(rca));
        assertTrue(rconZ.getCompanys().contains(rca));

        conA.setCompanys(new HashSet());
        //Contractor sConA = (Contractor) iw.getObjectByExample(conA, Collections.singleton("name"));
        Query equivQuery = iw.beof.createPKQuery(conA, source2, false);
        Set equiv = iw.getEquivalentObjects(conA, source2);
        assertEquals(equiv.getClass().getName() + ": " + equiv + ", " + equivQuery, 2, equiv.size());
        iw.store(conA, source2, skelSource2);
        Contractor rconA = (Contractor) iw.getObjectByExample(conA, Collections.singleton("name"));
        assertNotNull(rconA);
        assertTrue(rca.getContractors().contains(rconA));
        assertTrue(rconA.getCompanys().contains(rca));
    }

    public void testAddClass() throws Exception {
        Employee e = (Employee) DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {Employee.class, Broke.class})));
        e.setName("EmployeeA1");
        ((Broke) e).setDebt(8762);

        if (doIds) {
            e.setId(new Integer(1));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(e, source, skelSource);  // method we are testing

        InterMineObject re = iw.getObjectByExample(e, Collections.singleton("name"));
        assertNotNull(re);
        assertTrue(re instanceof Broke);
        assertTrue(re instanceof Employee);
        assertTrue(re instanceof Manager);
        assertEquals(8762, ((Broke) re).getDebt());
        assertEquals(new Integer(876123), ((Manager) re).getSeniority());
    }

    public void testSourceWithMultipleCopies() throws Exception {
        Employee e1 = new Employee();
        e1.setName("EmployeeA1");
        Employee e2 = new Employee();
        e2.setName("EmployeeA1");

        if (doIds) {
            e1.setId(new Integer(1));
            e2.setId(new Integer(2));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(e1, source, skelSource);

        try {
            iw.store(e2, source, skelSource);
            fail("Expected: IllegalArgumentException");
        } catch (RuntimeException e) {
        }
    }

    public void testMergeWithSuperclass() throws Exception {
        Manager e1 = new Manager();
        e1.setName("EmployeeA2");
        e1.setTitle("Mr.");

        if (doIds) {
            e1.setId(new Integer(1));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(e1, source, skelSource);

        Employee e2 = new Employee();
        e2.setName("EmployeeA2");
        Manager e3 = (Manager) iw.getObjectByExample(e2, Collections.singleton("name"));
        assertEquals("Mr.", e3.getTitle());
        assertEquals("EmployeeA2", e3.getName());
    }

    public void testMergeWithDifferentFields() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("CompanyZ")));
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "vatNumber"), ConstraintOp.EQUALS, new QueryValue(new Integer(876213))));
        q.setConstraint(cs);

        Company c2 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Address a2 = new Address();
        Company c3 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Company c4 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Address a4 = new Address();

        c2.setName("CompanyZ");
        c2.setAddress(a2);
        a2.setAddress("Flibble");
        c3.setVatNumber(876213);
        c4.setName("CompanyZ");
        c4.setAddress(a4);
        a4.setAddress("Flibble");
        c4.setVatNumber(876213);

        if (doIds) {
            c2.setId(new Integer(1));
            a2.setId(new Integer(2));
            c3.setId(new Integer(1));
            c4.setId(new Integer(1));
            a4.setId(new Integer(2));
        }

        assertEquals(0, iw.executeSingleton(q).size());

        Source source2 = iw.getMainSource("testsource2");
        Source skelSource2 = iw.getSkeletonSource("testsource2");

        iw.store(c2, source2, skelSource2);
        iw.store(a2, source2, skelSource2);

        iw.commitTransaction();
        iw.reset();
        iw.beginTransaction();
        assertEquals(1, iw.executeSingleton(q).size());

        Source source3 = iw.getMainSource("testsource3");
        Source skelSource3 = iw.getSkeletonSource("testsource3");

        iw.store(c3, source3, skelSource3);

        iw.commitTransaction();
        iw.reset();
        iw.beginTransaction();
        assertEquals(2, iw.executeSingleton(q).size());

        Source source4 = iw.getMainSource("testsource4");
        Source skelSource4 = iw.getSkeletonSource("testsource4");

        iw.store(c4, source4, skelSource4);
        iw.store(a4, source4, skelSource4);

        iw.commitTransaction();
        iw.reset();
        iw.beginTransaction();
        //assertEquals(eof.createPKQuery(iw.getModel(), c4, source4, iw.idMap, null, false).toString(), 1, iw.executeSingleton(q).size());
        assertEquals(1, iw.executeSingleton(q).size());
    }

    // a bug existed whereby storing a skeleton then a real object retrieved and failed to materialise
    // a ProxyReference - failing a check that a field being processed was a member of the class
    public void testStoreObjectAfterSkeleton() throws Exception {
        // CompanyA is in db with source "storedata"
        // source "testsource3" has a lower priority than "storedata"

        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c.setVatNumber(1234);

        Department d = new Department();
        d.setCompany(c);
        d.setName("new_department");

        if (doIds) {
            c.setId(new Integer(1));
            d.setId(new Integer(3));
        }
        Source source = iw.getMainSource("testsource3");
        Source skelSource = iw.getSkeletonSource("testsource3");

        Company before = (Company) iw.getObjectByExample(c, Collections.singleton("vatNumber"));
        assertNotNull("before example from db should not be null", before);

        // storing the dept should store CompanyA as a skeleton
        iw.store(d, source, skelSource);  // method we are testing
        iw.store(c, source, skelSource);  // method we are testing

        // assert that storing a CompanyA with no vatNumber does not overwrite
        // the existing higher priority value
        Company after = (Company) iw.getObjectByExample(c, Collections.singleton("vatNumber"));
        assertNotNull("after example from db should not be null", after);

        assertEquals(before.getVatNumber(), after.getVatNumber());
        assertEquals(before.getName(), after.getName());
        assertEquals(before.getAddress().getAddress(), after.getAddress().getAddress());

    }

    public void testGetEquivalentObjects() throws Exception {

        Bank b = (Bank) DynamicUtil.createObject(Collections.singleton(Bank.class));
        b.setName("bank1");

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");
        Set objects = iw.getEquivalentObjects(b, source);
        System.out.println(objects);
        assertTrue(objects.isEmpty());
    }

    public void testSkeletonsNoException() throws Exception {
        Address a = (Address) DynamicUtil.createObject(Collections.singleton(Address.class));
        a.setAddress("address1");
        if (doIds) {
            a.setId(new Integer(1));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(a, source, skelSource, IntegrationWriterDataTrackingImpl.SKELETON);
        assertTrue(iw.skeletons.size() == 1);
        iw.store(a, source, skelSource, IntegrationWriterDataTrackingImpl.SOURCE);
        assertTrue(iw.skeletons.size() == 0);
    }

    public void testSkeletonsException() throws Exception {
        Address a = (Address) DynamicUtil.createObject(Collections.singleton(Address.class));
        a.setAddress("address1");
        if (doIds) {
            a.setId(new Integer(1));
        }

        IntegrationWriterDataTrackingImpl iw2 = (IntegrationWriterDataTrackingImpl) IntegrationWriterFactory.getIntegrationWriter("integration.unittestmulti");
        ObjectStoreWriter writer2 = (ObjectStoreWriterInterMineImpl) iw2.getObjectStoreWriter();
        Source source = iw2.getMainSource("testsource");
        Source skelSource = iw2.getSkeletonSource("testsource");

        iw2.store(a, source, skelSource, IntegrationWriterDataTrackingImpl.SKELETON);
        assertTrue(iw2.skeletons.size() == 1);

        try {
            iw2.close();
            fail("Expected exception because not all skeletons replaced by real objects");
        } catch (ObjectStoreException e) {
        } finally {
            iw2 = (IntegrationWriterDataTrackingImpl) IntegrationWriterFactory.getIntegrationWriter("integration.unittestmulti");
            try {
                writer2 = (ObjectStoreWriterInterMineImpl) iw2.getObjectStoreWriter();
                removeDataFromStore(writer2);
            } finally {
                iw2.close();
            }
        }
    }

    public void testCircularRecursionBug() throws Exception {
        Department d = new Department();
        Manager m = new Manager();
        d.setManager(m);
        m.setDepartment(d);
        d.setName("Bob");
        m.setName("Fred");

        if (doIds) {
            d.setId(new Integer(1));
            m.setId(new Integer(2));
        }

        Source source = iw.getMainSource("testsource");
        Source skelSource = iw.getSkeletonSource("testsource");

        iw.store(m, source, skelSource);

        Query q = new Query();
        QueryClass qc = new QueryClass(Manager.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("Fred")));
        SingletonResults r = iw.executeSingleton(q);
        assertEquals("Results: " + r, 1, r.size());

        Manager rm = (Manager) r.get(0);
        assertNotNull(rm);
        Department rd = rm.getDepartment();
        assertNotNull(rd);
        assertEquals(d.getName(), rd.getName());

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        q2.addFrom(qc2);
        q2.addToSelect(qc2);
        q2.setConstraint(new SimpleConstraint(new QueryField(qc2, "name"), ConstraintOp.EQUALS, new QueryValue("Bob")));
        SingletonResults r2 = iw.executeSingleton(q2);
        assertEquals("Results: " + r2, 1, r2.size());
    }
}
