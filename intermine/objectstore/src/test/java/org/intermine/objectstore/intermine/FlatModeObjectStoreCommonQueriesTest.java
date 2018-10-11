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

import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.*;
import org.junit.BeforeClass;

public class FlatModeObjectStoreCommonQueriesTest extends ObjectStoreImplQueryTestCase {

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        oneTimeSetUp("os.flatmodeunittest", "osw.flatmodeunittest", "testmodel", "testmodel_data_flatmode.xml");

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
        results.put("SelectClassFromInterMineObject", NO_RESULT);
        Object[][] r = new Object[][] { { CEO.class, new Long(1) },
                { Employee.class, new Long(3) },
                { Manager.class, new Long(2) } };
        results.put("SelectClassFromEmployee", ObjectStoreTestUtils.toList(r));
        results.put("SelectClassFromBrokeEmployable", NO_RESULT);
        results.put("SubclassCollection2", NO_RESULT);
        results.put("ConstrainClass1", NO_RESULT);
        results.put("ConstrainClass2", NO_RESULT);
    }
}
