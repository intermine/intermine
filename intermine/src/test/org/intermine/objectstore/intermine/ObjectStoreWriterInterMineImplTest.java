package org.intermine.objectstore.intermine;

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

public class ObjectStoreWriterInterMineImplTest extends ObjectStoreWriterTestCase
{
    public static void oneTimeSetUp() throws Exception {
        writer = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        ObjectStoreWriterTestCase.oneTimeSetUp();
    }

    public static void oneTimeTearDown() throws Exception {
        writer.close();
        ObjectStoreWriterTestCase.oneTimeTearDown();
    }

    public ObjectStoreWriterInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreWriterInterMineImplTest.class);
    }
}

