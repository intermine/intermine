package org.intermine.web.uri;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPITestCase;
import org.intermine.objectstore.*;
import java.util.Map;

public class InterMineLUIConverterTest extends InterMineAPITestCase {
    private static ObjectStoreWriter osw;
    private static MockInterMineLUIConverter converter;

    public InterMineLUIConverterTest(String arg) {
        super(arg);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        try {
            //load data into test db
            osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
            Map data = ObjectStoreTestUtils.getTestData("testmodel", "testmodel_data.xml");
            ObjectStoreTestUtils.storeData(osw, data);

            //set mock methods
            converter = new MockInterMineLUIConverter(im.getProfileManager().getSuperuserProfile());
            converter.setInterMineAPI(im);
            converter.setObjectStore(os);

        } catch (Exception e) {
            System.err.println("Error connecting to DB");
            System.err.println(e);
            return;
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (osw != null) {
            osw.close();
        }
    }

    public void testGetLUIWithCorrectID() {
        InterMineLUI lui = converter.getInterMineLUI(new Integer(9));
        assertEquals("Employee", lui.getClassName());
        assertEquals("EmployeeA2", lui.getIdentifier());
    }

    public void testGetLUIWithWrongID() {
        InterMineLUI lui = converter.getInterMineLUI(new Integer(100));
        assertNull(lui);
    }

    public void testGetID() {
        try {
            InterMineLUI lui = new InterMineLUI("Employee", "EmployeeA2");
            Integer id = converter.getInterMineID(lui);
            assertEquals(9, id.intValue());
        } catch (ObjectStoreException ex) {
        }
    }

    public void testGetIDWithWrongType() {
        try {
            InterMineLUI lui = new InterMineLUI("WrongType", "EmployeeA1");
            Integer id = converter.getInterMineID(lui);
            assertEquals(-1, id.intValue());
        } catch (ObjectStoreException ex) {
        }
    }

    public void testGetIDWithWrongIdentifierValue() {
        try {
            InterMineLUI lui = new InterMineLUI("Company", "12345678");
            Integer id = converter.getInterMineID(lui);
            assertEquals(-1, id.intValue());
        } catch (ObjectStoreException ex) {
        }
    }
}
