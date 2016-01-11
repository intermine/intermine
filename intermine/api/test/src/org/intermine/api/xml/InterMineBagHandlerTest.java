package org.intermine.api.xml;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.util.DynamicUtil;
import org.intermine.web.bag.PkQueryIdUpgrader;

/**
 * Tests for the InterMineBagHandler class.
 *
 * @author Kim Rutherford
 */

public class InterMineBagHandlerTest extends StoreDataTestCase
{
    ObjectStoreWriter osw;
    private ObjectStore os;
    private int idCounter;

    public InterMineBagHandlerTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();
        idCounter = 0;
    }

    public void tearDown() throws Exception {
        osw.close();
    }

    public void executeTest(String type) {
    }

    public void testQueries() throws Throwable {
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public static Test suite() {
        return buildSuite(InterMineBagHandlerTest.class);
    }

    public void testNoNewObject() throws Exception {
        Company oldCompany = createCompanyWithId("Old company");

        // no new object so expect an empt set
        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        assertEquals(new HashSet<Integer>(), newIds);
    }

    public void testFindCompanyByVatNumber() throws Exception {
        Company oldCompany = createCompanyWithId("Old company");
        oldCompany.setVatNumber(5678);

        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        assertEquals(1, newIds.size());
    }

    public void testFindCompanyByName() throws Exception {
        Address oldAddress = createAddressWithId("Company Street, BVille");
        Company oldCompany = createCompanyWithId("CompanyB");
        oldCompany.setAddress(oldAddress);

        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);

        assertEquals(1, newIds.size());
    }

    public void testFindCompanyByNameNoAddress() throws Exception {
        Company oldCompany = createCompanyWithId("CompanyB");
        // can't find a new object so expect empty set
        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        assertEquals(new HashSet<Integer>(), newIds);
    }

    public void testFindCompanyByNameAndVat() throws Exception {
        Address oldAddress = createAddressWithId("Company Street, BVille");
        Company oldCompany = createCompanyWithId("CompanyB");
        oldCompany.setAddress(oldAddress);
        oldCompany.setVatNumber(5678);

        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        assertEquals(1, newIds.size());
    }

    /**
     * For the case when it returns two Objects
     * @throws Exception
     */
    public void testFindCompanyByNameAndDifferentVat() throws Exception {
        Address oldAddress = createAddressWithId("Company Street, BVille");
        Company oldCompany = createCompanyWithId("CompanyB");
        oldCompany.setAddress(oldAddress);
        oldCompany.setVatNumber(1234);

        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        assertEquals(2, newIds.size());
    }


    private Company createCompanyWithId(String companyName) {
        Company company =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setId(new Integer(idCounter++));
        company.setName(companyName);
        return company;
    }

    private Address createAddressWithId(String streetAddress) {
        Address address =  (Address) DynamicUtil.createObject(Collections.singleton(Address.class));
        address.setId(new Integer(idCounter++));
        address.setAddress(streetAddress);
        return address;
    }
}
