package org.flymine.dataloader;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.flymine.objectstore.ObjectStoreQueriesTestCase;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ojb.ObjectStoreOjbImpl;
import org.flymine.objectstore.ojb.ObjectStoreWriterOjbImpl;
import org.flymine.sql.DatabaseFactory;
import org.flymine.model.testmodel.*;
import org.flymine.util.RelationType;
import org.flymine.util.TypeUtil;


public class IntegrationWriterSingleSourceImplTest extends ObjectStoreQueriesTestCase
{
    public IntegrationWriterSingleSourceImplTest(String arg) {
        super(arg);
    }

    protected IntegrationWriterSingleSourceImpl iw;
    protected ArrayList toDelete;

    public void setUp() throws Exception {
        super.setUp();

        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        writer = new ObjectStoreWriterOjbImpl((ObjectStoreOjbImpl) os);
        iw = new IntegrationWriterSingleSourceImpl("test", writer);
        db = DatabaseFactory.getDatabase("db.unittest");
        storeData();
        toDelete = new ArrayList();
    }

    public void tearDown() throws Exception {
        removeDataFromStore();

        Iterator deleteIter = toDelete.iterator();
        while (deleteIter.hasNext()) {
            Object o = deleteIter.next();
            writer.delete(o);
        }
    }

    // Not doing the Query tests here
    public void executeTest(String type) throws Exception {
    }

    public void testStoreObject() throws Exception {
        Company c = new Company();
        Address a = new Address();
        a.setAddress("Company Street, AVille");

        Address a2 = (Address) writer.getObjectByExample(a);
        assertNotNull("address from db should not be null", a2);
        c.setAddress(a2);
        c.setName("CompanyC");
        c.setVatNumber(100);
        toDelete.add(c);

        iw.store(c);  // method we are testing

        Company example = (Company) writer.getObjectByExample(c);
        assertNotNull("example from db should not be null", example);

        assertEquals(c.getAddress(), example.getAddress());
        assertEquals(c.getVatNumber(), example.getVatNumber());
        assertEquals(c.getName(), example.getName());
    }


    public void testObjectNotStoredByIntegrationWriter() throws Exception {
        Address address = new Address();
        address.setAddress("Company Street, AVille");
        Address a2 = (Address) writer.getObjectByExample(address);

        Company company = new Company();
        company.setAddress(a2);
        company.setName("CompanyA");

        // company Id starts off as null
        assertEquals(null, company.getId());

        IntegrationDescriptor descriptor = iw.getByExample(company);

        // this object has not been stored by the integration writer so expect IntegrationDescriptor
        // to be empty apart from id
        assertNotNull("Expected return from getByExample to be not null", descriptor);
        Field f = Company.class.getDeclaredField("id");
        assertNotNull("Expected Id to be not null", descriptor.get(f));

        // rest should be empty
        Map fieldToGetter = TypeUtil.getFieldToGetter(Company.class);
        Iterator iter = fieldToGetter.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Field field = (Field) entry.getKey();
            assertNull(descriptor.get(field));
        }
    }

    public void testStoredObjectAsSkeleton() throws Exception {
        Address address = new Address();
        address.setAddress("Company Street, AVille");
        Address a2 = (Address) writer.getObjectByExample(address);

        Company company = new Company();
        company.setAddress(a2);
        company.setName("CompanyC");
        company.setVatNumber(100);
        toDelete.add(company);

        iw.store(company, true); // store as skeleton

        // check object stored correctly
        Company example = (Company) writer.getObjectByExample(company);
        assertNotNull("Expected to retrieve object by example", example);

        // this object has not been stored as a skeleton, we should be able to write over everything
        // so IntegrationDesciptor should be empty apart from id
        IntegrationDescriptor descriptor = iw.getByExample(company);

        assertNotNull("Expected return from getByExample to be not null", descriptor);
        Field f = Company.class.getDeclaredField("id");
        assertNotNull("Expected Id to be not null", descriptor.get(f));

        // rest should be empty
        Map fieldToGetter = TypeUtil.getFieldToGetter(Company.class);
        Iterator iter = fieldToGetter.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Field field = (Field) entry.getKey();
            assertNull(descriptor.get(field));
        }
    }

    public void testStoredObjectAsNonSkeleton() throws Exception {
        Address address = new Address();
        address.setAddress("Company Street, AVille");
        Address a2 = (Address) writer.getObjectByExample(address);

        Company company = new Company();
        company.setAddress(a2);
        company.setName("CompanyC");
        company.setVatNumber(100);
        toDelete.add(company);

        iw.store(company, false);

        // object has already been stored by this integration writer so we cannot overwrite
        // fields - IntegrationDescriptor should contain fields we stored
        IntegrationDescriptor descriptor = iw.getByExample(company);

        assertNotNull("Expected return from getByExample to be not null", descriptor);

        Field f = Company.class.getDeclaredField("id");
        assertNotNull("Expected Id to be not null", descriptor.get(f));

        // assert that the fields we set are filled in
        Address a = (Address) descriptor.get(Company.class.getDeclaredField("address"));
        assertEquals(company.getAddress(), a);
        Integer vat = (Integer) descriptor.get(Company.class.getDeclaredField("vatNumber"));
        assertEquals(company.getVatNumber(), vat.intValue());
        String name = (String) descriptor.get(Company.class.getDeclaredField("name"));
        assertEquals(company.getName(), name);

        // rest should be empty
        CEO ceo = (CEO) descriptor.get(Company.class.getDeclaredField("CEO"));
        assertNull("CEO should not have been filled in", ceo);

        List departments = (List) descriptor.get(Company.class.getDeclaredField("departments"));
        assertTrue("departments should have been an empty list", departments.size() == 0);
    }

}
