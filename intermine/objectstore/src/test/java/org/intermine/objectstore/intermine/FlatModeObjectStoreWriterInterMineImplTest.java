package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.BigDepartment;
import org.intermine.model.testmodel.Cleaner;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.*;
import org.intermine.util.DynamicUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FlatModeObjectStoreWriterInterMineImplTest extends ObjectStoreWriterTestCase
{
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        oneTimeSetUp(ObjectStoreWriterFactory.getObjectStoreWriter("osw.flatmodeunittest"));
    }

    @Test
    public void testWriteDynamicObject() throws Exception {
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {Company.class, Employee.class})));
        try {
            writer.store(o);
            Assert.fail("Expected: error");
        } catch (ObjectStoreException e) {
            Assert.assertEquals("Non-flat model heirarchy used in flat mode. Cannot store object with classes = [interface org.intermine.model.testmodel.Company, class org.intermine.model.testmodel.Employee]", e.getMessage());
        } finally {
            writer.delete(o);
        }
    }

    @Test
    public void testWriteInterMineObject() throws Exception {
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(Collections.singleton(InterMineObject.class));
        try {
            writer.store(o);
            Assert.fail("Expected: error");
        } catch (ObjectStoreException e) {
            Assert.assertEquals("Object [interface org.intermine.model.InterMineObject] does not map onto any database table.", e.getMessage());
        } finally {
            writer.delete(o);
        }
    }

    @Test
    public void testWriteCleaner() throws Exception {
        InterMineObject o = new Cleaner();
        try {
            writer.store(o);
            Assert.fail("Expected: error");
        } catch (ObjectStoreException e) {
            Assert.assertEquals("Cannot store object [class org.intermine.model.testmodel.Cleaner] - no column for field evenings in table Employee", e.getMessage());
        } finally {
            writer.delete(o);
        }
    }

    @Test
    public void testWriteBigDepartment() throws Exception {
        InterMineObject o = new BigDepartment();
        try {
            writer.store(o);
            Assert.fail("Expected: error");
        } catch (ObjectStoreException e) {
            Assert.assertEquals("Non-flat model heirarchy used in flat mode. Cannot store object with classes = [class org.intermine.model.testmodel.BigDepartment]", e.getMessage());
        } finally {
            writer.delete(o);
        }
    }
}
