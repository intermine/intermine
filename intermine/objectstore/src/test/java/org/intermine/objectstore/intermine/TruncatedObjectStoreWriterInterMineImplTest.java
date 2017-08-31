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

import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TruncatedObjectStoreWriterInterMineImplTest extends ObjectStoreWriterTests
{
    protected static ObjectStoreWriter writer;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        writer = ObjectStoreWriterFactory.getObjectStoreWriter("osw.truncunittest");
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        writer.close();
    }

    @Test
    public void testExceptionOutOfTransaction() throws Exception {
        Assert.assertFalse(writer.isInTransaction());
        // First, cause an exception outside a transaction
        try {
            writer.store(new Employee() {
                public Integer getId() {
                    throw new RuntimeException();
                }
                public void setId(Integer id) {
                    throw new RuntimeException();
                }
            });
        } catch (Exception e) {
        }
        Assert.assertFalse(writer.isInTransaction());
        // Now try and do something normal.
        Object o = writer.getObjectById(new Integer(2));
    }
}
