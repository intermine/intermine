package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreTestUtils;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.junit.BeforeClass;

import java.util.Collection;

public class WithNotXmlObjectStoreInterMineImplTest extends ObjectStoreInterMineCommonTests
{
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        os = (ObjectStoreInterMineImpl)ObjectStoreFactory.getObjectStore("os.notxmlunittest");
        Model model = Model.getInstanceByName("testmodel/testmodel");
        Collection items = ObjectStoreTestUtils.loadItemsFromXml(model, "testmodel_data.xml");
        ObjectStoreTestUtils.setIdsOnItems(items);
        data = ObjectStoreTestUtils.mapItemsToNames(items);
        System.out.println(data.size() + " entries in data mapItemsToNames");
        storeDataWriter = ObjectStoreWriterFactory.getObjectStoreWriter("osw.notxmlunittest");
        ObjectStoreTestUtils.storeData(storeDataWriter, data);
    }

    @org.junit.Test
    public void testFailFast2() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);

        Results r = os.execute(q);
        storeDataWriter.store(data.get("CompanyA"));
        r.iterator().hasNext();
    }
}
