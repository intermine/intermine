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

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Bank;
import org.intermine.model.testmodel.Broke;
import org.intermine.model.testmodel.SimpleObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.QueryClass;
import org.junit.*;

public class ObjectStoreWriterInterMineImplTest extends ObjectStoreWriterTestCase
{
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        oneTimeSetUp(ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest"));
    }

    @Test
    public void testDeleteNonInterMineObjectByQueryClass() throws Exception {
        // SimpleObject is just a FastPathObject, not an InterMineObject
        SimpleObject itemA = new SimpleObject();
        itemA.setName("simpleobject A");

        writer.store(itemA);

        QueryClass qc = new QueryClass(SimpleObject.class);
        writer.delete(qc, null);
    }

    @Test
    public void testDeleteInterMineObjectByQueryClass() throws Exception {
        Address addressA = new Address();
        addressA.setAddress("Address A");

        writer.store(addressA);

        boolean gotException = false;

        try {
            QueryClass addressQc = new QueryClass(Address.class);
            writer.delete(addressQc, null);
        } catch (ObjectStoreException e) {
            gotException = true;
        }

        Assert.assertTrue(gotException);
    }

    @Test
    public void testExceptionOutOfTransaction() throws Exception {
        Assert.assertFalse(writer.isInTransaction());
        // First, cause an exception outside a transaction
        try {
            writer.store(new RuntimeExceptionEmployee());
        } catch (Exception e) {}

        Assert.assertFalse(writer.isInTransaction());
        // Now try and do something normal.
        writer.getObjectById(new Integer(2));
    }
}

