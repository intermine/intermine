package org.intermine.objectstore.flymine;

/*
 * Copyright (C) 2002-2003 FlyMine
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

public class ObjectStoreWriterFlyMineImplTest extends ObjectStoreWriterTestCase
{
    public static void oneTimeSetUp() throws Exception {
        writer = (ObjectStoreWriterFlyMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        ObjectStoreWriterTestCase.oneTimeSetUp();
    }

    public static void oneTimeTearDown() throws Exception {
        writer.close();
        ObjectStoreWriterTestCase.oneTimeTearDown();
    }

    public ObjectStoreWriterFlyMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreWriterFlyMineImplTest.class);
    }
}

