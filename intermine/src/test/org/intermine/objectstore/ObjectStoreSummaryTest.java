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

import java.util.Properties;
import java.util.Arrays;
import java.io.InputStream;

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
        Properties objectStoreSummaryProperties = new Properties();
        ClassLoader classLoader = ObjectStoreSummaryTest.class.getClassLoader();
        InputStream objectStoreSummaryPropertiesStream =
            classLoader.getResourceAsStream("objectstoresummary.properties");
        objectStoreSummaryProperties.load(objectStoreSummaryPropertiesStream);
        ObjectStoreSummary oss = new ObjectStoreSummary(objectStoreSummaryProperties);
        assertEquals(2, oss.getClassCount("org.intermine.model.testmodel.Company"));
    }

    public void testGetFieldValues() throws Exception {
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        assertNotNull(os);
        Properties objectStoreSummaryProperties = new Properties();
        ClassLoader classLoader = ObjectStoreSummaryTest.class.getClassLoader();
        InputStream objectStoreSummaryPropertiesStream =
            classLoader.getResourceAsStream("objectstoresummary.properties");
        objectStoreSummaryProperties.load(objectStoreSummaryPropertiesStream);
        ObjectStoreSummary oss = new ObjectStoreSummary(objectStoreSummaryProperties);

        assertEquals(Arrays.asList(new Object [] {"10", "20", "30", "40", "50", "60"}),
                     oss.getFieldValues("org.intermine.model.testmodel.Employee", "age"));

        assertEquals(Arrays.asList(new Object [] {"Mr.", null}),
                     oss.getFieldValues("org.intermine.model.testmodel.Manager", "title"));

        // null because Bank.name isn't in the objectstoresummary.config.properties file
        assertNull(oss.getFieldValues("org.intermine.model.testmodel.Bank", "name"));

        // null because max.field.values exceeded
        assertNull(oss.getFieldValues("org.intermine.model.testmodel.Thing", "id"));
        assertNull(oss.getFieldValues("org.intermine.model.InterMineObject", "id"));
    }
}
