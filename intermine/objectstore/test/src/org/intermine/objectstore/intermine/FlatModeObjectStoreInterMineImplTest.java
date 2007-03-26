package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import java.util.List;

import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;

import org.intermine.model.testmodel.Employee;

public class FlatModeObjectStoreInterMineImplTest extends ObjectStoreInterMineImplTest
{
    public static void oneTimeSetUp() throws Exception {
        alternateDataFile = "testmodel_data_flatmode.xml";
        storeDataWriter = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory
            .getObjectStoreWriter("osw.flatmodeunittest");
        ObjectStoreInterMineImplTest.oneTimeSetUp();
        os = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.flatmodeunittest");
        results.put("SelectInterfaceAndSubClasses", NO_RESULT);
        results.put("SelectInterfaceAndSubClasses2", NO_RESULT);
        results.put("InterfaceField", NO_RESULT);
        results.put("InterfaceReference", NO_RESULT);
        results.put("InterfaceCollection", NO_RESULT);
        results.put("DynamicInterfacesAttribute", NO_RESULT);
        results.put("DynamicClassInterface", NO_RESULT);
        results.put("DynamicClassRef1", NO_RESULT);
        results.put("DynamicClassRef2", NO_RESULT);
        results.put("DynamicClassRef3", NO_RESULT);
        results.put("DynamicClassRef4", NO_RESULT);
        results.put("DynamicClassConstraint", NO_RESULT);
        results.put("DynamicBagConstraint2", NO_RESULT);
        results.put("OrSubquery", NO_RESULT);
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
}
