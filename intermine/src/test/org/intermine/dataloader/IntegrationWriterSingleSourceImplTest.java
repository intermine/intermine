package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import junit.framework.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.testing.OneTimeTestCase;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.StoreDataTestCase;
import org.flymine.model.testmodel.*;
import org.flymine.util.DynamicUtil;
import org.flymine.util.TypeUtil;

public class IntegrationWriterSingleSourceImplTest extends StoreDataTestCase
{
    protected static ObjectStore os;
    protected static IntegrationWriterSingleSourceImpl iw;

    public IntegrationWriterSingleSourceImplTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(IntegrationWriterSingleSourceImplTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        strictTestQueries = false;
        removeDataFromStore();
        storeData();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
        iw = new IntegrationWriterSingleSourceImpl(writer);
        os = iw.getObjectStore();
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

        Source source = new Source();
        source.setName("testsource");

        iw.store(c, source);  // method we are testing

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

    public void testUpdateObjectOneToOne() throws Exception {
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Address a = new Address();
        Address a2 = new Address();
        CEO ceo = new CEO();
        a.setAddress("Company Street, AVille");
        c.setAddress(a);
        c.setName("CompanyA");
        c.setVatNumber(100);
        c.setCEO(ceo);
        a2.setAddress("Employee Street, BVille");
        ceo.setName("EmployeeB1");
        ceo.setSeniority(new Integer(76321));
        ceo.setFullTime(true);
        ceo.setSalary(45000);
        ceo.setAge(40);
        ceo.setCompany(c);
        ceo.setAddress(a2);

        Source source = new Source();
        source.setName("testsource");

        iw.store(c, source); // method we are testing

        Company rc = (Company) iw.getObjectByExample(c, Collections.singleton("name"));
        assertNotNull("Object from db should not be null", rc);

        assertEquals(c.getVatNumber(), rc.getVatNumber());
        CEO rceo = rc.getCEO();
        assertNotNull(rceo);
        assertEquals(ceo.getName(), rceo.getName());
        assertNotNull(rceo.getCompany());
        assertEquals(rc, rceo.getCompany());

        Company exampleOC = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        exampleOC.setName("CompanyB");
        Company oc = (Company) iw.getObjectByExample(exampleOC, Collections.singleton("name"));

        assertNotNull(oc);
        assertNull(oc.getCEO());
    }

    public void testUpdateObjectOneToOne2() throws Exception {
        Source source = new Source();
        source.setName("testsource");

        {
            Company companyA = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
            CEO ceoA = new CEO();
            Address companyAAddress = new Address();
            Address ceoAAddress = new Address();
            companyAAddress.setAddress("Company Street, AVille");
            companyA.setAddress(companyAAddress);
            companyA.setName("CompanyA");
            companyA.setVatNumber(1234);
            companyA.setCEO(ceoA);
            ceoAAddress.setAddress("Employee Street, AVille");
            ceoA.setAddress(ceoAAddress);
            ceoA.setSeniority(new Integer(876234));
            ceoA.setName("Fred");
            ceoA.setFullTime(false);
            ceoA.setSalary(1);
            ceoA.setAge(101);
            ceoA.setCompany(companyA);

            iw.store(companyA, source);
            iw.store(ceoA, source);
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
        assertEquals(rCEOA, rCompanyA.getCEO());
        assertEquals(rCEOB, rCompanyB.getCEO());
        
        {
            Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
            Address a = new Address();
            Address a2 = new Address();
            CEO ceo = new CEO();
            a.setAddress("Company Street, AVille");
            c.setAddress(a);
            c.setName("CompanyA");
            c.setVatNumber(100);
            c.setCEO(ceo);
            a2.setAddress("Employee Street, BVille");
            ceo.setName("EmployeeB1");
            ceo.setSeniority(new Integer(76321));
            ceo.setFullTime(true);
            ceo.setSalary(45000);
            ceo.setAge(40);
            ceo.setCompany(c);
            ceo.setAddress(a2);


            iw.store(c, source); // method we are testing
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
        assertEquals(rCEOB, rCompanyA.getCEO());
        assertEquals(null, rCompanyB.getCEO());
    }

    public void testUpdateObjectOneToOneNull() throws Exception {
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Address a = new Address();
        a.setAddress("Company Street, BVille");
        c.setAddress(a);
        c.setName("CompanyB");
        c.setVatNumber(100);

        Source source = new Source();
        source.setName("testsource");

        iw.store(c, source); // method we are testing

        Company result = (Company) iw.getObjectByExample(c, Collections.singleton("name"));
        assertNotNull("Object from db should not be null", result);

        assertEquals(c.getVatNumber(), result.getVatNumber());
        assertNull(result.getCEO());

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

        Source source = new Source();
        source.setName("testsource");

        iw.store(e, source); // method we are testing

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

        Source source = new Source();
        source.setName("testsource");

        iw.store(e, source); // method we are testing

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

        Source source = new Source();
        source.setName("testsource");

        iw.store(d, source); // method we are testing

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

        Source source = new Source();
        source.setName("testsource");

        iw.store(con, source); // method we are testing

        Company rca = (Company) iw.getObjectByExample(companyA, Collections.singleton("name"));
        Contractor rcon = (Contractor) iw.getObjectByExample(con, Collections.singleton("name"));
        assertNotNull(rca);
        assertNotNull(rcon);

        assertTrue(rca.getContractors().contains(rcon));
        assertTrue(rcon.getCompanys().contains(rca));


        Address companyBAddress = new Address();
        Company companyB = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        companyBAddress.setAddress("Company Street, BVille");
        companyB.setAddress(companyBAddress);
        companyB.setName("CompanyB");
        con.addCompanys(companyB);
        companyB.addContractors(con);

        iw.store(con, source);

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
        
        iw.store(ca);
        iw.store(conA);
        iw.store(conC);
        iw.store(conD);

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

        Source source = new Source();
        source.setName("testsource");

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
        
        iw.store(con, source); // method we are testing

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

        conA.setCompanys(new ArrayList());
        iw.store(conA, source);
        Contractor rconA = (Contractor) iw.getObjectByExample(conA, Collections.singleton("name"));
        assertNotNull(rconA);
        assertTrue(rca.getContractors().contains(rconA));
        assertTrue(rconA.getCompanys().contains(rca));
    }

    public void testAddClass() throws Exception {
        Employee e = (Employee) DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {Employee.class, Broke.class})));
        e.setName("EmployeeA1");
        ((Broke) e).setDebt(8762);

        Source source = new Source();
        source.setName("testsource");
        
        iw.store(e, source);
        
        FlyMineBusinessObject re = iw.getObjectByExample(e, Collections.singleton("name"));
        assertNotNull(re);
        assertTrue(re instanceof Broke);
        assertTrue(re instanceof Employee);
        assertTrue(re instanceof Manager);
        assertEquals(8762, ((Broke) re).getDebt());
        assertEquals(new Integer(876123), ((Manager) re).getSeniority());
    }






/*
    public void testStoredObjectAsSkeleton() throws Exception {
        Address address = new Address();
        address.setAddress("Company Street, AVille");
        Address a2 = (Address) iw.getObjectByExample(address, Collections.singleton("address"));

        Company company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setAddress(a2);
        company.setName("CompanyC");
        company.setVatNumber(100);
        toDelete.add(company);

        iw.store(company, true); // store as skeleton

        // check object stored correctly
        Company example = (Company) iw.getObjectByExample(company, Collections.singleton("name"));
        assertNotNull("Expected to retrieve object by example", example);

        // this object has not been stored as a skeleton, we should be able to write over everything
        // so IntegrationDesciptor should be empty apart from id
        IntegrationDescriptor descriptor = iw.getByExample(company);

        assertNotNull("Expected return from getByExample to be not null", descriptor);
        assertNotNull("Expected Id to be not null", descriptor.get("id"));

        // rest should be empty
        Map infos = TypeUtil.getFieldInfos(Company.class);
        Iterator iter = infos.keySet().iterator();
        while (iter.hasNext()) {
            String fieldname = (String) iter.next();
            if (!"id".equals(fieldname)) {
                assertNull(descriptor.get(fieldname));
            }
        }
    }

    public void testStoredObjectAsNonSkeleton() throws Exception {
        Address address = new Address();
        address.setAddress("Company Street, AVille");
        Address a2 = (Address) iw.getObjectByExample(address, Collections.singleton("address"));

        Company company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setAddress(a2);
        company.setName("CompanyC");
        company.setVatNumber(100);
        toDelete.add(company);

        iw.store(company, false);

        // object has already been stored by this integration writer so we cannot overwrite
        // fields - IntegrationDescriptor should contain fields we stored
        IntegrationDescriptor descriptor = iw.getByExample(company);

        assertNotNull("Expected return from getByExample to be not null", descriptor);

        assertNotNull("Expected Id to be not null", descriptor.get("id"));

        // assert that the fields we set are filled in
        Address a = (Address) descriptor.get("address");
        assertEquals(company.getAddress(), a);
        Integer vat = (Integer) descriptor.get("vatNumber");
        assertEquals(company.getVatNumber(), vat.intValue());
        String name = (String) descriptor.get("name");
        assertEquals(company.getName(), name);

        // rest should be empty
        CEO ceo = (CEO) descriptor.get("cEO");
        assertNull("cEO should not have been filled in", ceo);

        List departments = (List) descriptor.get("departments");
        assertTrue("departments should have been an empty list", departments.size() == 0);
    }
*/
}
