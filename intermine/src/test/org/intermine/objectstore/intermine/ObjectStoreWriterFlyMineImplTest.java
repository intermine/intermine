package org.flymine.objectstore.flymine;

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

import org.flymine.objectstore.ObjectStoreWriterTestCase;
import org.flymine.objectstore.ObjectStoreWriterFactory;

public class ObjectStoreWriterFlyMineImplTest extends ObjectStoreWriterTestCase
{
    public static void oneTimeSetUp() throws Exception {
        writer = (ObjectStoreWriterFlyMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        ObjectStoreWriterTestCase.oneTimeSetUp();
    }

    public ObjectStoreWriterFlyMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreWriterFlyMineImplTest.class);
    }
}


