package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.objectstore.flymine.ObjectStoreWriterFlyMineImpl;
import org.flymine.xml.full.FullParser;

public class BufferedItemWriterTest extends ItemWriterTestCase {
    public BufferedItemWriterTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        osw = (ObjectStoreWriterFlyMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        itemWriter = new ObjectStoreItemWriter(osw);
        itemWriter = new BufferedItemWriter(itemWriter);
        osw = (ObjectStoreWriterFlyMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        super.setUp();
    }
}

