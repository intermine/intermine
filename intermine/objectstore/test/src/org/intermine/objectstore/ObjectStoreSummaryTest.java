package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.Test;

import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.Types;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;

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
        ObjectStoreSummary oss = new ObjectStoreSummary(os, new Properties());
        assertEquals(2, oss.getClassCount("org.intermine.model.testmodel.Company"));
    }

    public void testIgnore() throws Exception {
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        Properties config = new Properties();
        config.put("max.field.value", "10");
        config.put("ignore.counts", "org.intermine.model.testmodel.Employee.age");
        ObjectStoreSummary oss = new ObjectStoreSummary(os, config);
        assertNull(oss.getFieldValues("org.intermine.model.testmodel.Employee", "age"));
    }


    public void testGetFieldValues() throws Exception {
        Properties config = new Properties();
        config.put("max.field.value", "10");
        config.put("org.intermine.model.testmodel.Employee.fields", "age name");
        config.put("org.intermine.model.testmodel.Manager.fields", "title");
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreSummary oss = new ObjectStoreSummary(os, config);
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

    public void testEmptyAttributes() throws Exception {
        // delete names of existing employees so we have some empty attributes
        Query q = new Query();
        QueryClass qcEmployee = new QueryClass(Employee.class);
        q.addFrom(qcEmployee);
        q.addToSelect(qcEmployee);
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreWriter osw = os.getNewWriter();

        SingletonResults res = os.executeSingleton(q);
        osw.beginTransaction();
        for (Object o : res){
            Employee employee = (Employee) o;
            employee.setName(null);
            osw.store(employee);
        }
        osw.commitTransaction();

        ObjectStoreSummary oss = new ObjectStoreSummary(os, new Properties());

        Map<String, Set<String>> expectedEmptyAttributes = new HashMap<String, Set<String>>();
        HashSet<String> nameSet = new HashSet<String>(Arrays.asList(new String[] {"name"}));
        expectedEmptyAttributes.put(Employee.class.getName(), nameSet);
        expectedEmptyAttributes.put(Manager.class.getName(), nameSet);
        expectedEmptyAttributes.put(CEO.class.getName(), nameSet);
        // Types.clobObjType also empty for some reason
        expectedEmptyAttributes.put(Types.class.getName(), new HashSet<String>(Arrays.asList(new String[] {"clobObjType"})));
        assertEquals(expectedEmptyAttributes, oss.emptyAttributesMap);

        // the contents should be the same after a round-trip to the properties file
        Properties ossProps = oss.toProperties();
        ObjectStoreSummary ossFromProps = new ObjectStoreSummary(ossProps);
        assertEquals(expectedEmptyAttributes, ossFromProps.emptyAttributesMap);
    }

    public void testMaxValues() throws Exception {
        Properties config = new Properties();
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreSummary oss = new ObjectStoreSummary(os, config);

        // nothing in config
        assertEquals(ObjectStoreSummary.DEFAULT_MAX_VALUES, oss.getMaxValues());

        // config should overwrite
        config.put("max.field.values", "10");
        oss = new ObjectStoreSummary(os, config);
        assertEquals(10, oss.getMaxValues());

        // value should be written and read from properties
        ObjectStoreSummary ossFromProps = new ObjectStoreSummary(oss.toProperties());
        assertEquals(10, ossFromProps.getMaxValues());
    }

    public void testPropertiesRoundTrip() throws Exception {
        Properties config = new Properties();
        config.put("max.field.values", "10");
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreSummary oss = new ObjectStoreSummary(os, config);

        Properties out = oss.toProperties();
        ObjectStoreSummary ossFromProps = new ObjectStoreSummary(out);

        assertEquals(out, ossFromProps.toProperties());
        assertEquals(10, oss.maxValues);
        assertEquals(10, ossFromProps.maxValues);
    }
}
