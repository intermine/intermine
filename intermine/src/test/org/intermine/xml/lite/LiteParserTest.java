package org.flymine.xml.lite;

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
import java.util.Date;

import org.flymine.model.testmodel.*;

public class LiteParserTest extends TestCase
{

    public LiteParserTest(String arg) {
        super(arg);
    }

    public void testParse1() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/LiteParserTest.xml");
        Employee obj1 = (Employee) LiteParser.parse(is);

        Employee e1 = new Employee();
        e1.setName("Employee1");

        assertEquals(e1.getName(), obj1.getName());
        assertEquals(new Integer(5678), obj1.getDepartment().getId());
    }

    public void testParseTypes() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/LiteParserTestTypes.xml");
        Types obj1 = (Types) LiteParser.parse(is);

        assertEquals("Types1", obj1.getName());
        assertTrue(1.2f == obj1.getFloatType());
        assertTrue(1.3d == obj1.getDoubleType());
        assertEquals(2, obj1.getIntType());
        assertTrue(obj1.getBooleanType());
        assertEquals(new Float(2.2f), obj1.getFloatObjType());
        assertEquals(new Double(2.3d), obj1.getDoubleObjType());
        assertEquals(new Integer(4), obj1.getIntObjType());
        assertEquals(Boolean.TRUE, obj1.getBooleanObjType());
        assertEquals(new Date(7777777777l), obj1.getDateObjType());
        assertEquals("A String", obj1.getStringObjType());

    }

    public void testParseNull() throws Exception{
        try {
            LiteParser.parse(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

    }

}
