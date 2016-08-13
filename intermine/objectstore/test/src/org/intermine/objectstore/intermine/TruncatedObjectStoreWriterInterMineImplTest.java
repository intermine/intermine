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

import junit.framework.Test;

import org.intermine.objectstore.ObjectStoreWriterTestCase;
import org.intermine.objectstore.ObjectStoreWriterFactory;

public class TruncatedObjectStoreWriterInterMineImplTest extends ObjectStoreWriterInterMineImplTest
{
    public static void oneTimeSetUp() throws Exception {
        writer = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.truncunittest");
        storeDataWriter = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.truncunittest");
        ObjectStoreWriterTestCase.oneTimeSetUp();
    }

    public TruncatedObjectStoreWriterInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(TruncatedObjectStoreWriterInterMineImplTest.class);
    }
}


