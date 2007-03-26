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

import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriterFactory;

public class TruncatedObjectStoreInterMineImplTest extends ObjectStoreInterMineImplTest
{
    public static void oneTimeSetUp() throws Exception {
        storeDataWriter = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory
            .getObjectStoreWriter("osw.truncunittest");
        ObjectStoreInterMineImplTest.oneTimeSetUp();
        os = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.truncunittest");
    }

    public TruncatedObjectStoreInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(TruncatedObjectStoreInterMineImplTest.class);
    }
}
