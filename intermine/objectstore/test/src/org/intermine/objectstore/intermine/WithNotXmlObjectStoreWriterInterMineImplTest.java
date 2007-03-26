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

import org.intermine.objectstore.ObjectStoreWriterTestCase;
import org.intermine.objectstore.ObjectStoreWriterFactory;

public class WithNotXmlObjectStoreWriterInterMineImplTest extends ObjectStoreWriterInterMineImplTest
{
    public static void oneTimeSetUp() throws Exception {
        writer = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.notxmlunittest");
        storeDataWriter = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.notxmlunittest");
        ObjectStoreWriterTestCase.oneTimeSetUp();
    }

    public WithNotXmlObjectStoreWriterInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(WithNotXmlObjectStoreWriterInterMineImplTest.class);
    }
}


