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

import junit.framework.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.flymine.testing.OneTimeTestCase;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.SetupDataTestCase;
import org.flymine.model.testmodel.*;
import org.flymine.util.TypeUtil;

public class IntegrationWriterSingleSourceImplTest extends SetupDataTestCase
{
    protected static ObjectStore os;
    protected static IntegrationWriterSingleSourceImpl iw;
    protected static ArrayList toDelete;

    public IntegrationWriterSingleSourceImplTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(IntegrationWriterSingleSourceImplTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        toDelete = new ArrayList();
    }

    public void tearDown() throws Exception {
        super.tearDown();

        Iterator deleteIter = toDelete.iterator();
        while (deleteIter.hasNext()) {
            Object o = deleteIter.next();
            writer.delete(o);
        }
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();
        iw = new IntegrationWriterSingleSourceImpl("test", writer);
        os = iw.getObjectStore();
    }

    // Not doing the Query tests here
    public void executeTest(String type) throws Exception {
    }

    public void testStoreObject() throws Exception {
        Company c = new Company();
        Address a = new Address();
        a.setAddress("Company Street, AVille");

        Address a2 = (Address) os.getObjectByExample(a);
        assertNotNull("address from db should not be null", a2);
        c.setAddress(a2);
        c.setName("CompanyC");
        c.setVatNumber(100);
        toDelete.add(c);

        iw.store(c);  // method we are testing

        Company example = (Company) os.getObjectByExample(c);
        assertNotNull("example from db should not be null", example);

        assertEquals(c.getAddress(), example.getAddress());
        assertEquals(c.getVatNumber(), example.getVatNumber());
        assertEquals(c.getName(), example.getName());
    }


    public void testObjectNotStoredByIntegrationWriter() throws Exception {
        Address address = new Address();
        address.setAddress("Company Street, AVille");
        Address a2 = (Address) os.getObjectByExample(address);

        assertNotNull("address from db should not be null", a2);

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
        Address a2 = (Address) os.getObjectByExample(address);

        Company company = new Company();
        company.setAddress(a2);
        company.setName("CompanyC");
        company.setVatNumber(100);
        toDelete.add(company);

        iw.store(company, true); // store as skeleton

        // check object stored correctly
        Company example = (Company) os.getObjectByExample(company);
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
        Address a2 = (Address) os.getObjectByExample(address);

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
        CEO ceo = (CEO) descriptor.get(Company.class.getDeclaredField("cEO"));
        assertNull("cEO should not have been filled in", ceo);

        List departments = (List) descriptor.get(Company.class.getDeclaredField("departments"));
        assertTrue("departments should have been an empty list", departments.size() == 0);
    }

}
