package org.intermine.api.xml;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreTestUtils;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.DynamicUtil;
import org.intermine.api.bag.PkQueryIdUpgrader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the InterMineBagHandler class.
 *
 * @author Kim Rutherford
 */

public class InterMineBagHandlerTest
{
    ObjectStoreWriter osw;
    private ObjectStore os;
    private int idCounter;

    @Before
    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();
        Map data = ObjectStoreTestUtils.getTestData("testmodel", "testmodel_data.xml");
        ObjectStoreTestUtils.storeData(osw, data);
    }

    @After
    public void teardown() throws Exception {
        osw.close();
    }

    @Test
    public void testNoNewObject() throws Exception {
        Company oldCompany = createCompanyWithId("Old company");

        // no new object so expect an empt set
        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        Assert.assertEquals(new HashSet<Integer>(), newIds);
    }

    @Test
    public void testFindCompanyByVatNumber() throws Exception {
        Company oldCompany = createCompanyWithId("Old company");
        oldCompany.setVatNumber(5678);

        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        Assert.assertEquals(1, newIds.size());
    }

    @Test
    public void testFindCompanyByName() throws Exception {
        Address oldAddress = createAddressWithId("Company Street, BVille");
        Company oldCompany = createCompanyWithId("CompanyB");
        oldCompany.setAddress(oldAddress);

        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);

        Assert.assertEquals(1, newIds.size());
    }

    @Test
    public void testFindCompanyByNameNoAddress() throws Exception {
        Company oldCompany = createCompanyWithId("CompanyB");
        // can't find a new object so expect empty set
        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        Assert.assertEquals(new HashSet<Integer>(), newIds);
    }

    @Test
    public void testFindCompanyByNameAndVat() throws Exception {
        Address oldAddress = createAddressWithId("Company Street, BVille");
        Company oldCompany = createCompanyWithId("CompanyB");
        oldCompany.setAddress(oldAddress);
        oldCompany.setVatNumber(5678);

        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        Assert.assertEquals(1, newIds.size());
    }

    /**
     * For the case when it returns two Objects
     * @throws Exception
     */
    @Test
    public void testFindCompanyByNameAndDifferentVat() throws Exception {
        Address oldAddress = createAddressWithId("Company Street, BVille");
        Company oldCompany = createCompanyWithId("CompanyB");
        oldCompany.setAddress(oldAddress);
        oldCompany.setVatNumber(1234);

        Set<Integer> newIds = new PkQueryIdUpgrader(os).getNewIds(oldCompany, os);
        Assert.assertEquals(2, newIds.size());
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
