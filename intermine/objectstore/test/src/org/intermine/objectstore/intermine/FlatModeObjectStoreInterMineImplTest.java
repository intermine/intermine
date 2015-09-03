package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;

public class FlatModeObjectStoreInterMineImplTest extends ObjectStoreInterMineImplTest
{
    public static void oneTimeSetUp() throws Exception {
        alternateDataFile = "testmodel_data_flatmode.xml";
        storeDataWriter = ObjectStoreWriterFactory
            .getObjectStoreWriter("osw.flatmodeunittest");
        ObjectStoreInterMineImplTest.oneTimeSetUp();
        os = ObjectStoreFactory.getObjectStore("os.flatmodeunittest");
        results.put("SelectInterfaceAndSubClasses", NO_RESULT);
        results.put("SelectInterfaceAndSubClasses2", NO_RESULT);
        results.put("InterfaceField", NO_RESULT);
        results.put("InterfaceReference", NO_RESULT);
        results.put("InterfaceCollection", NO_RESULT);
        results.put("OrSubquery", NO_RESULT);
        results.put("SelectClassFromInterMineObject", NO_RESULT);
        Object[][] r = new Object[][] { { CEO.class, new Long(1) },
                                        { Employee.class, new Long(3) },
                                        { Manager.class, new Long(2) } };
        results.put("SelectClassFromEmployee", toList(r));
        results.put("ConstrainClass1", NO_RESULT);
        results.put("ConstrainClass2", NO_RESULT);
    }

    public static void oneTimeTearDown() throws Exception {
        ObjectStoreInterMineImplTest.oneTimeTearDown();
        alternateDataFile = null;
    }

    public FlatModeObjectStoreInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(FlatModeObjectStoreInterMineImplTest.class);
    }

    public void testLazyCollectionMtoN() throws Exception {
    }

    public void testFailFast2() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);

        Results r = os.execute(q);
        storeDataWriter.store((Company) data.get("CompanyA"));
        r.iterator().hasNext();
    }
}
