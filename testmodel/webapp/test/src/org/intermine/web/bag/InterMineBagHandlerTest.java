package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.DynamicUtil;
import org.intermine.util.XmlBinding;

import java.io.InputStream;

import junit.framework.TestCase;

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


        XmlBinding binding = new XmlBinding(osw.getModel());

        InputStream is =
            getClass().getClassLoader().getResourceAsStream("testmodel_data.xml");

        List objects = (List) binding.unmarshal(is);

        osw.beginTransaction();
        Iterator iter = objects.iterator();
        int i = 1;
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            o.setId(new Integer(i++));
        }
        iter = objects.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }
        osw.commitTransaction();
        osw.close();

    }

    public void tearDown() throws Exception {
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                                                    .getSequence());
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();
        osw.close();
    }
    
    public void testNoNewObject() throws Exception {   
        Company oldCompany =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));

        oldCompany.setName("Old company");

        try {
            Set newIds = new PkQueryIdUpgrader().getNewIds(oldCompany, os);
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }
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

        try {
            Set newIds = new PkQueryIdUpgrader().getNewIds(oldCompany, os);
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }
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
