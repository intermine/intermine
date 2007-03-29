package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import junit.framework.TestCase;

import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.DynamicUtil;
import org.intermine.web.bag.PkQueryIdUpgrader;

/**
 * Tests for the InterMineBagHandler class.
 *
 * @author Kim Rutherford
 */

public class InterMineBagHandlerTest extends TestCase
{
    private ObjectStore os;

    public InterMineBagHandlerTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();
    }

    public void testNoNewObject() throws Exception {
        Company oldCompany =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));

        oldCompany.setName("Old company");

        // no new object so expect an empt set
        Set newIds = new PkQueryIdUpgrader().getNewIds(oldCompany, os);
        assertEquals(new HashSet(), newIds);
    }

    public void testFindCompanyByVatNumber() throws Exception {
        Company oldCompany =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));

        oldCompany.setName("Old company");
        oldCompany.setVatNumber(5678);

        Set newIds = new PkQueryIdUpgrader().getNewIds(oldCompany, os);

        assertEquals(1, newIds.size());
    }

    public void testFindCompanyByName() throws Exception {
        Address oldAddress =
            (Address) DynamicUtil.createObject(Collections.singleton(Address.class));
        oldAddress.setAddress("Company Street, BVille");

        Company oldCompany =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        oldCompany.setName("CompanyB");
        oldCompany.setAddress(oldAddress);

        Set newIds = new PkQueryIdUpgrader().getNewIds(oldCompany, os);

        assertEquals(1, newIds.size());
    }

    public void testFindCompanyByNameNoAddress() throws Exception {
        Company oldCompany =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        oldCompany.setName("CompanyB");

        // can't find a new object so expect empty set
        Set newIds = new PkQueryIdUpgrader().getNewIds(oldCompany, os);
        assertEquals(new HashSet(), newIds);
    }

    public void testFindCompanyByNameAndVat() throws Exception {
        Address oldAddress =
            (Address) DynamicUtil.createObject(Collections.singleton(Address.class));
        oldAddress.setAddress("Company Street, BVille");

        Company oldCompany =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        oldCompany.setName("CompanyB");
        oldCompany.setAddress(oldAddress);
        oldCompany.setVatNumber(5678);

        Set newIds = new PkQueryIdUpgrader().getNewIds(oldCompany, os);

        assertEquals(1, newIds.size());
    }

    public void testFindCompanyByNameAndDifferentVat() throws Exception {
        Address oldAddress =
            (Address) DynamicUtil.createObject(Collections.singleton(Address.class));
        oldAddress.setAddress("Company Street, BVille");

        Company oldCompany =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        oldCompany.setName("CompanyB");
        oldCompany.setAddress(oldAddress);
        oldCompany.setVatNumber(1234);

        try {
            Set newIds = new PkQueryIdUpgrader().getNewIds(oldCompany, os);
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }
    }

}
