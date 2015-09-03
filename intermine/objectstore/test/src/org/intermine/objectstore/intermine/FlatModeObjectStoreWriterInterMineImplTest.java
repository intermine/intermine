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

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.BigDepartment;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Cleaner;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreWriterTestCase;
import org.intermine.util.DynamicUtil;

public class FlatModeObjectStoreWriterInterMineImplTest extends ObjectStoreWriterInterMineImplTest
{
    public static void oneTimeSetUp() throws Exception {
        alternateDataFile = "testmodel_data_flatmode.xml";
        writer = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.flatmodeunittest");
        storeDataWriter = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.flatmodeunittest");
        ObjectStoreWriterTestCase.oneTimeSetUp();
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
        ObjectStoreWriterInterMineImplTest.oneTimeTearDown();
        alternateDataFile = null;
    }

    public FlatModeObjectStoreWriterInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(FlatModeObjectStoreWriterInterMineImplTest.class);
    }

    public void testWriteInterMineObject() throws Exception {
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(InterMineObject.class);
        try {
            writer.store(o);
            fail("Expected: error");
        } catch (ObjectStoreException e) {
            assertEquals("Object interface org.intermine.model.InterMineObject does not map onto any database table.", e.getMessage());
        } finally {
            writer.delete(o);
        }
    }

    public void testWriteCleaner() throws Exception {
        InterMineObject o = new Cleaner();
        try {
            writer.store(o);
            fail("Expected: error");
        } catch (ObjectStoreException e) {
            assertEquals("Cannot store object class org.intermine.model.testmodel.Cleaner - no column for field evenings in table Employee", e.getMessage());
        } finally {
            writer.delete(o);
        }
    }

    public void testWriteBigDepartment() throws Exception {
        InterMineObject o = new BigDepartment();
        try {
            writer.store(o);
            fail("Expected: error");
        } catch (ObjectStoreException e) {
            assertEquals("Non-flat model heirarchy used in flat mode. Cannot store object with class = class org.intermine.model.testmodel.BigDepartment", e.getMessage());
        } finally {
            writer.delete(o);
        }
    }

    public void testExceptionOutOfTransaction() throws Exception {
    }
}
