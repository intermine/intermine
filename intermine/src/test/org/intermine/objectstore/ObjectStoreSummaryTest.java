package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

public class ObjectStoreSummaryTest extends StoreDataTestCase
{
    public ObjectStoreSummaryTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        strictTestQueries = false;
    }
    
    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public void executeTest(String type) {
        
    }

    public static Test suite() {
        return buildSuite(ObjectStoreSummaryTest.class);
    }

    public void testGetCount() throws Exception {
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        assertNotNull(os);
        ObjectStoreSummary oss = new ObjectStoreSummary(os);
        assertEquals(2, oss.getClassCount("org.intermine.model.testmodel.Company"));
        // do it again to check the cache
        assertEquals(2, oss.getClassCount("org.intermine.model.testmodel.Company"));
    }
}
