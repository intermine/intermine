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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import org.flymine.model.testmodel.*;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.proxy.ProxyReference;
import org.flymine.util.DynamicBean;

public class LiteParserTest extends TestCase
{
    ObjectStore os;
    
    public LiteParserTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        os = ObjectStoreFactory.getObjectStore("os.unittest");
    }
    
    public void testParse1() throws Exception {
        InputStream is = new ByteArrayInputStream(("<object class=\"org.flymine.model.testmodel.Employee\" implements=\"org.flymine.model.testmodel.Employable\">\n"
                + "  <field name=\"id\" value=\"1234\"/>\n"
                + "  <field name=\"name\" value=\"Employee1\"/>\n"
                + "  <reference name=\"department\" value=\"5678\"/>\n"
                + "</object>").getBytes());
                      
        Employee obj1 = (Employee) LiteParser.parse(is, os);

        assertEquals("Employee1", obj1.getName());
        assertEquals(new Integer(1234), obj1.getId());
        Class c = Employee.class;
        java.lang.reflect.Field f = c.getDeclaredField("department");
        f.setAccessible(true);
        ProxyReference o = (ProxyReference) f.get(obj1);
        assertNotNull(o);
        assertEquals(new Integer(5678), o.getId());
    }

    public void testParse2() throws Exception {
        InputStream is = new ByteArrayInputStream(
                ("<object class=\"\" implements=\"org.flymine.model.testmodel.Company net.sf.cglib.Factory\">\n"
                + "  <reference name=\"address\" value=\"74328\"/>\n"
                + "  <field name=\"vatNumber\" value=\"100\"/>\n"
                + "  <field name=\"name\" value=\"CompanyC\"/>\n"
                + "  <field name=\"id\" value=\"74350\"/>\n"
                + "</object>").getBytes());
        Company obj1 = (Company) LiteParser.parse(is, os);

        assertEquals("CompanyC", obj1.getName());
        assertEquals(100, obj1.getVatNumber());
        assertEquals(new Integer(74350), obj1.getId());
        Map fieldMap = ((DynamicBean) ((net.sf.cglib.Factory) obj1).interceptor()).getMap();
        ProxyReference addressRef = (ProxyReference) fieldMap.get("Address");
        assertNotNull(addressRef);
        assertEquals(new Integer(74328), addressRef.getId());
    }

    public void testParseTypes() throws Exception{
        InputStream is = new ByteArrayInputStream(("<object class=\"org.flymine.model.testmodel.Types\" implements=\"\">\n"
                + "  <field name=\"id\" value=\"1234\"/>\n"
                + "  <field name=\"name\" value=\"Types1\"/>\n"
                + "  <field name=\"floatType\" value=\"1.2\"/>\n"
                + "  <field name=\"doubleType\" value=\"1.3\"/>\n"
                + "  <field name=\"intType\" value=\"2\"/>\n"
                + "  <field name=\"booleanType\" value=\"true\"/>\n"
                + "  <field name=\"intObjType\" value=\"4\"/>\n"
                + "  <field name=\"booleanObjType\" value=\"true\"/>\n"
                + "  <field name=\"floatObjType\" value=\"2.2\"/>\n"
                + "  <field name=\"doubleObjType\" value=\"2.3\"/>\n"
                + "  <field name=\"dateObjType\" value=\"7777777777\"/>\n"
                + "  <field name=\"stringObjType\" value=\"A String\"/>\n"
                + "</object>").getBytes());
        Types obj1 = (Types) LiteParser.parse(is, os);

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
        //TODO: Other number types
    }

    public void testParseNull() throws Exception{
        try {
            LiteParser.parse(null, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

    }

}
