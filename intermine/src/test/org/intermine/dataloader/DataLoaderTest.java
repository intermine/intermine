package org.flymine.dataloader;

import junit.framework.TestCase;

import java.util.Iterator;
import java.util.ArrayList;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ojb.ObjectStoreWriterOjbImpl;
import org.flymine.objectstore.ojb.ObjectStoreOjbImpl;
import org.flymine.model.testmodel.*;
import org.flymine.metadata.Model;

public class DataLoaderTest extends TestCase
{
    protected DataLoader loader;
    protected ObjectStore os;
    protected ObjectStoreWriter writer;
    protected ArrayList toDelete = new ArrayList();

    public DataLoaderTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        writer = new ObjectStoreWriterOjbImpl((ObjectStoreOjbImpl) os);
        Model model = Model.getInstanceByName("testmodel");
        IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter("dataloader.unittest", "source1", os);
        loader = new DataLoader(iw);
    }

    public void tearDown() throws Exception {
        Iterator deleteIter = toDelete.iterator();
        while (deleteIter.hasNext()) {
            Object o = deleteIter.next();
            writer.delete(o);
        }
    }

    public void testSimpleStore() throws Exception {
        // store an address, has no obj/collection references
        Address a = new Address();
        a.setAddress("1 Unit Road, TestVille");
        toDelete.add(a);

        loader.store(a);

        Address a2 = (Address) os.getObjectByExample(a);

        assertNotNull("Expected addess to be retrieved from DB", a2);
        assertTrue("address id sould be set", (a2.getId().intValue() != 0));
    }

    public void testStoreWithObjectRef() throws Exception {
        Address a = new Address();
        a.setAddress("2 Unit Road, TestVille");
        toDelete.add(a);

        Company c = new Company();
        c.setAddress(a);
        c.setName("UnitTest Ltd");
        c.setVatNumber(100);
        toDelete.add(c);

        // storing Company should store associated Address as a skeleton
        loader.store(c);

        // check address was stored
        Address a2 = (Address) os.getObjectByExample(a);
        assertNotNull("Expected address to be retieved from DB", a2);
        assertTrue("address id should be set", (a2.getId().intValue() != 0));
        assertEquals(a, a2);

        // check company
        Company c2 = (Company) os.getObjectByExample(c);
        assertNotNull("Expected company to be retieved from DB", c2);
        assertTrue("company id should be set", (c2.getId().intValue() != 0));
        assertEquals(c, c2);
    }

    public void testStoreWithObjectRefNtoOne() throws Exception {
        Address a = new Address();
        a.setAddress("2 Unit Road, TestVille");
        toDelete.add(a);

        Company c = new Company();
        c.setName("Acme Testing");
        c.setAddress(a);
        toDelete.add(c);

        Department d = new Department();
        d.setName("Department1");
        d.setCompany(c);
        toDelete.add(d);

        // Store department, company and address should get stored as well
        loader.store(d);

        // check address was stored
        Address a2 = (Address) os.getObjectByExample(a);
        assertNotNull("Expected address to be retieved from DB", a2);
        assertTrue("address id should be set", (a2.getId().intValue() != 0));
        assertEquals(a, a2);

        // check company
        Company c2 = (Company) os.getObjectByExample(c);
        assertNotNull("Expected company to be retieved from DB", c2);
        assertTrue("company id should be set", (c2.getId().intValue() != 0));
        assertEquals(c, c2);

        // check department
        Department d2 = (Department) os.getObjectByExample(d);
        assertNotNull("Expected department to be retieved from DB", d2);
        assertTrue("department id should be set", (d2.getId().intValue() != 0));
        assertEquals(d, d2);
    }

    // store a new Company with an address that is already in DB
    public void testStoreWithObjectRefThatAlreadyExists() throws Exception {
        // store an address in DB
        Address aDb = new Address();
        aDb.setAddress("3 Unit Road, TestVille");
        writer.store(aDb);
        toDelete.add(aDb);

        Address a = new Address();
        a.setAddress("3 Unit Road, TestVille");
        toDelete.add(a);

        Company c = new Company();
        c.setAddress(a);
        c.setName("Testing Ltd");
        c.setVatNumber(1000);
        toDelete.add(c);

        // storing Company should store associated Address as a skeleton
        loader.store(c);

        // check address was stored, an exception is thrown by getByExample if same addess was stored twice
        Address a2 = (Address) os.getObjectByExample(a);
        assertNotNull("Expected address to be retieved from DB", a2);
        assertTrue("address id should be set", (a2.getId().intValue() != 0));
        assertEquals(a, a2);

        // check company
        Company c2 = (Company) os.getObjectByExample(c);
        assertNotNull("Expected company to be retieved from DB", c2);
        assertTrue("company id should be set", (c2.getId().intValue() != 0));
        assertEquals(c, c2);

    }

    // store a CEO and its company as a skeleton, try to add company
    // again as a a top-level object
    public void testSkeletonThenRealObject() throws Exception {

        Address a1 = new Address();
        a1.setAddress("1 Unit Road, TestVille");
        toDelete.add(a1);

        Address a2 = new Address();
        a2.setAddress("2 Unit Road, TestVille");
        toDelete.add(a2);

        // a skeleton company
        Company c1 = new Company();
        c1.setName("Acme Testing");
        c1.setAddress(a1);

        CEO ceo = new CEO();
        ceo.setName("ceo1");
        ceo.setFullTime(true);
        ceo.setAddress(a2);
        ceo.setCompany(c1);
        toDelete.add(ceo);

        // Now store the ceo, check that company is stored
        loader.store(ceo);

        Company ex1 = (Company) os.getObjectByExample(c1);
        assertNotNull("Expected company to be retieved from DB", ex1);
        assertTrue("company id should be set", (ex1.getId().intValue() != 0));
        assertEquals(c1, ex1);

        // store same company as a "real" object
        Company c2 = new Company();
        c2.setName("Acme Testing");
        c2.setAddress(a1);
        c2.setVatNumber(1000);
        toDelete.add(c2);

        loader.store(c2);

        // check that vatNumber got set in company in database
        Company ex2 = (Company) os.getObjectByExample(c2);
        assertNotNull("Expected company to be retieved from DB", ex2);
        assertTrue("company id should be set", (ex2.getId().intValue() != 0));
        assertTrue("Expect vatNumber to be set", (ex2.getVatNumber() == 1000));
        assertEquals(c2, ex2);
    }

    public void testStoreWithCollection() throws Exception {
        Company c = new Company();
        c.setName("UnitTest Ltd");
        c.setVatNumber(100);
        Address a = new Address();
        a.setAddress("2 Unit Road, TestVille");
        c.setAddress(a);
        Department d1 = new Department();
        d1.setName("Department 1");
        d1.setCompany(c);
        Department d2 = new Department();
        d2.setName("Department 2");
        d2.setCompany(c);
        c.getDepartments().add(d1);
        c.getDepartments().add(d2);

        toDelete.add(a);
        toDelete.add(c);
        toDelete.add(d1);
        toDelete.add(d2);

        // Storing Company should store associated address and departments as skeletons.
        loader.store(c);

        // Now check that the two departments were stored.
        Department gotD1 = (Department) os.getObjectByExample(d1);
        assertNotNull("Expected department to be retrieved from DB", gotD1);
        assertTrue("Department id should be set", gotD1.getId().intValue() != 0);
        assertEquals(d1, gotD1);

        Department gotD2 = (Department) os.getObjectByExample(d2);
        assertNotNull("Expected department to be retrieved from DB", gotD2);
        assertTrue("Department id should be set", gotD2.getId().intValue() != 0);
        assertEquals(d2, gotD2);

        // check company
        Company gotC = (Company) os.getObjectByExample(c);
        assertNotNull("Expected company to be retieved from DB", gotC);
        assertTrue("company id should be set", (gotC.getId().intValue() != 0));
        assertEquals(c, gotC);
    }

    public void testStoreWithManyToMany() throws Exception {
        Company c1 = new Company();
        c1.setName("UnitTest 1 Ltd");
        c1.setVatNumber(100);
        Address a1 = new Address();
        a1.setAddress("1 Unit Road, TestVille");
        c1.setAddress(a1);
        Company c2 = new Company();
        c2.setName("UnitTest 2 Ltd");
        c2.setVatNumber(200);
        Address a2 = new Address();
        a2.setAddress("2 Unit Road, TestVille");
        c2.setAddress(a2);

        Contractor d1 = new Contractor();
        d1.setName("Alice");
        Address a3 = new Address();
        a3.setAddress("Aliceville");
        d1.setPersonalAddress(a3);
        Address a4 = new Address();
        a4.setAddress("Alice and co");
        d1.setBusinessAddress(a4);
        Contractor d2 = new Contractor();
        d2.setName("Bob");
        Address a5 = new Address();
        a5.setAddress("Bobville");
        d2.setPersonalAddress(a5);
        Address a6 = new Address();
        a6.setAddress("Bob and co");
        d2.setBusinessAddress(a6);

        c1.getContractors().add(d1);
        c1.getContractors().add(d2);
        c2.getContractors().add(d1);
        d1.getCompanys().add(c1);
        d1.getCompanys().add(c2);
        d2.getCompanys().add(c1);

        toDelete.add(c1);
        toDelete.add(c2);
        toDelete.add(d1);
        toDelete.add(d2);
        toDelete.add(a1);
        toDelete.add(a2);
        toDelete.add(a3);
        toDelete.add(a4);
        toDelete.add(a5);
        toDelete.add(a6);

        loader.store(c1);

        // Check company 1.
        Company gotC1 = (Company) os.getObjectByExample(c1);
        assertNotNull("Expected company to be retrieved from DB", gotC1);
        assertTrue("Expected retrieved company to be a separate instance from the stored one", c1 != gotC1);
        assertTrue("Company id should be set", gotC1.getId().intValue() != 0);
        assertEquals(c1, gotC1);
        assertEquals(2, gotC1.getContractors().size());

        // Check company 2.
        Company gotC2 = (Company) os.getObjectByExample(c2);
        assertNotNull("Expected company to be retrieved from DB", gotC2);
        assertTrue("Expected retrieved company to be a separate instance from the stored one", c2 != gotC2);
        assertTrue("Company id should be set", gotC2.getId().intValue() != 0);
        assertEquals(c2, gotC2);
        assertEquals(1, gotC2.getContractors().size());

        // Check contractor 1.
        Contractor gotD1 = (Contractor) os.getObjectByExample(d1);
        assertNotNull("Expected contractor to be retrieved from DB", gotD1);
        assertTrue("Expected retrieved contractor to be a separate instance from the stored one", d1 != gotD1);
        assertTrue("Department id should be set", gotD1.getId().intValue() != 0);
        assertEquals(d1, gotD1);
        assertEquals(2, gotD1.getCompanys().size());

        // Check contractor 2.
        Contractor gotD2 = (Contractor) os.getObjectByExample(d2);
        assertNotNull("Expected contractor to be retrieved from DB", gotD2);
        assertTrue("Expected retrieved contractor to be a separate instance from the stored one", d2 != gotD2);
        assertTrue("Department id should be set", gotD2.getId().intValue() != 0);
        assertEquals(d2, gotD2);
        assertEquals(1, gotD2.getCompanys().size());
    }
}
