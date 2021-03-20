package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TruncatedObjectStoreWriterInterMineImplTest extends ObjectStoreWriterTestCase
{
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        oneTimeSetUp(ObjectStoreWriterFactory.getObjectStoreWriter("osw.truncunittest"));
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
