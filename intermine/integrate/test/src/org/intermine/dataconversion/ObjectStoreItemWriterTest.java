package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;

public class ObjectStoreItemWriterTest extends ItemWriterTestCase {
    public ObjectStoreItemWriterTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        osw = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        itemWriter = new ObjectStoreItemWriter(osw);
        super.setUp();
    }

    public void tearDown() throws Exception {
        itemWriter.close();
        osw.close();
    }
}

