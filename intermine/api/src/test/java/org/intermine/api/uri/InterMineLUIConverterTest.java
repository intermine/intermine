package org.intermine.api.uri;

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
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreTestUtils;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.DynamicUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Map;

public class InterMineLUIConverterTest {
    protected static ObjectStoreWriter storeDataWriter;
    private static Company company;

    @BeforeClass
    public static void setUp() throws Exception {
        storeDataWriter = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setId(1);
        company.setName("Company");
        company.setVatNumber(1234);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (storeDataWriter != null) {
            storeDataWriter.delete(company);
        }
    }

    @Test
    public void getLUIWithCorrectID() {
        InterMineLUIConverter converter = new InterMineLUIConverter();
        try {
            InterMineLUI lui = converter.getInterMineLUI(new Integer(1));
            assertEquals("Company", lui.getClassName());
            assertEquals("1234", lui.getIdentifier());
        } catch (ObjectStoreException ex) {
        }
    }

    @Test
    public void getLUIWithWrongID() {
        InterMineLUIConverter converter = new InterMineLUIConverter();
        try {
            InterMineLUI lui = converter.getInterMineLUI(new Integer(2));
            assertNull(lui);
        } catch (ObjectStoreException ex) {
        }
    }

    @Test
    public void getID() {
        InterMineLUIConverter converter = new InterMineLUIConverter();
        try {
            InterMineLUI lui = new InterMineLUI("Company", "1234");
            Integer id = converter.getInterMineID(lui);
            assertEquals(1, id.intValue());
        } catch (ObjectStoreException ex) {
        }
    }

    @Test
    public void getIDWithWrongType() {
        InterMineLUIConverter converter = new InterMineLUIConverter();
        try {
            InterMineLUI lui = new InterMineLUI("WrongType", "1234");
            Integer id = converter.getInterMineID(lui);
            assertEquals(-1, id.intValue());
        } catch (ObjectStoreException ex) {
        }
    }

    @Test
    public void getIDWithWrongIdentifierValue() {
        InterMineLUIConverter converter = new InterMineLUIConverter();
        try {
            InterMineLUI lui = new InterMineLUI("Company", "12345678");
            Integer id = converter.getInterMineID(lui);
            assertEquals(-1, id.intValue());
        } catch (ObjectStoreException ex) {
        }
    }
}
