package org.intermine.xml.lite;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.math.BigDecimal;

import org.intermine.model.testmodel.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicBean;

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
        String s = "org.intermine.model.testmodel.Employee" + LiteRenderer.DELIM + "org.intermine.model.testmodel.Employable"
            + LiteRenderer.DELIM + "aid" + LiteRenderer.DELIM + "1234"
            + LiteRenderer.DELIM + "aname" + LiteRenderer.DELIM + "Employee1"
            + LiteRenderer.DELIM + "rdepartment" + LiteRenderer.DELIM + "5678";

        Employee obj1 = (Employee) LiteParser.parse(s, os);

        assertEquals("Employee1", obj1.getName());
        assertEquals(new Integer(1234), obj1.getId());
        Class c = Employee.class;
        java.lang.reflect.Field f = c.getDeclaredField("department");
        f.setAccessible(true);
        ProxyReference o = (ProxyReference) f.get(obj1);
        assertNotNull(o);
        assertEquals(new Integer(5678), o.getId());
    }

    public void testParseXmlSimple() throws Exception {
        InputStream is = new ByteArrayInputStream(new String("<object class=\"org.intermine.model.testmodel.Employee\" implements=\"org.intermine.model.testmodel.Employable\">"
                + "<field name=\"id\" value=\"1234\"/>"
                + "  <field name=\"name\" value=\"Employee1\"/>"
                + "  <reference name=\"department\" value=\"5678\"/>"
                + "</object>").getBytes());

        Employee obj1 = (Employee) LiteParser.parseXml(is, os);

        assertEquals("Employee1", obj1.getName());
        assertEquals(new Integer(1234), obj1.getId());
        Class c = Employee.class;
        java.lang.reflect.Field f = c.getDeclaredField("department");
        f.setAccessible(true);
        ProxyReference o = (ProxyReference) f.get(obj1);
        assertNotNull(o);
        assertEquals(new Integer(5678), o.getId());
    }


    public void testParseDynamic() throws Exception {

        String s = LiteRenderer.DELIM + "org.intermine.model.testmodel.Company net.sf.cglib.proxy.Factory"
            + LiteRenderer.DELIM + "raddress" + LiteRenderer.DELIM + "74328"
            + LiteRenderer.DELIM + "avatNumber" + LiteRenderer.DELIM + "100"
            + LiteRenderer.DELIM + "aname" + LiteRenderer.DELIM + "CompanyC"
            + LiteRenderer.DELIM + "aid" + LiteRenderer.DELIM + "74350";

        Company obj1 = (Company) LiteParser.parse(s, os);

        assertEquals("CompanyC", obj1.getName());
        assertEquals(100, obj1.getVatNumber());
        assertEquals(new Integer(74350), obj1.getId());
        Map fieldMap = ((DynamicBean) ((net.sf.cglib.proxy.Factory) obj1).getCallback(0)).getMap();
        ProxyReference addressRef = (ProxyReference) fieldMap.get("Address");
        assertNotNull(addressRef);
        assertEquals(new Integer(74328), addressRef.getId());
    }

    public void testParseXmlDynamic() throws Exception {
        InputStream is = new ByteArrayInputStream(
                ("<object class=\"\" implements=\"org.intermine.model.testmodel.Company net.sf.cglib.Factory\">"
                + "  <reference name=\"address\" value=\"74328\"/>"
                + "  <field name=\"vatNumber\" value=\"100\"/>"
                + "  <field name=\"name\" value=\"CompanyC\"/>"
                + "  <field name=\"id\" value=\"74350\"/>"
                + "</object>").getBytes());
        Company obj1 = (Company) LiteParser.parseXml(is, os);

        assertEquals("CompanyC", obj1.getName());
        assertEquals(100, obj1.getVatNumber());
        assertEquals(new Integer(74350), obj1.getId());
        Map fieldMap = ((DynamicBean) ((net.sf.cglib.proxy.Factory) obj1).getCallback(0)).getMap();
        ProxyReference addressRef = (ProxyReference) fieldMap.get("Address");
        assertNotNull(addressRef);
        assertEquals(new Integer(74328), addressRef.getId());
    }

    public void testParseTypesXml() throws Exception{
        InputStream is = new ByteArrayInputStream(("<object class=\"org.intermine.model.testmodel.Types\" implements=\"\">"
                + "  <field name=\"id\" value=\"1234\"/>"
                + "  <field name=\"name\" value=\"Types1\"/>"
                + "  <field name=\"floatType\" value=\"1.2\"/>"
                + "  <field name=\"doubleType\" value=\"1.3\"/>"
                + "  <field name=\"intType\" value=\"2\"/>"
                + "  <field name=\"booleanType\" value=\"true\"/>"
                + "  <field name=\"intObjType\" value=\"4\"/>"
                + "  <field name=\"booleanObjType\" value=\"true\"/>"
                + "  <field name=\"floatObjType\" value=\"2.2\"/>"
                + "  <field name=\"doubleObjType\" value=\"2.3\"/>"
                + "  <field name=\"dateObjType\" value=\"7777777777\"/>"
                + "  <field name=\"stringObjType\" value=\"A String\"/>"
                + "</object>").getBytes());
        Types obj1 = (Types) LiteParser.parseXml(is, os);

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


    public void testParseTypes() throws Exception{
        String s = "org.intermine.model.testmodel.Types"
            + LiteRenderer.DELIM
            + LiteRenderer.DELIM + "aid" + LiteRenderer.DELIM + "1234"
            + LiteRenderer.DELIM + "aname" + LiteRenderer.DELIM + "Types1"
            + LiteRenderer.DELIM + "afloatType" + LiteRenderer.DELIM + "1.2"
            + LiteRenderer.DELIM + "adoubleType" + LiteRenderer.DELIM + "1.3"
            + LiteRenderer.DELIM + "aintType" + LiteRenderer.DELIM + "2"
            + LiteRenderer.DELIM + "abooleanType" + LiteRenderer.DELIM + "true"
            + LiteRenderer.DELIM + "aintObjType" + LiteRenderer.DELIM + "4"
            + LiteRenderer.DELIM + "abooleanObjType" + LiteRenderer.DELIM + "true"
            + LiteRenderer.DELIM + "afloatObjType" + LiteRenderer.DELIM + "2.2"
            + LiteRenderer.DELIM + "adoubleObjType" + LiteRenderer.DELIM + "2.3"
            + LiteRenderer.DELIM + "adateObjType" + LiteRenderer.DELIM + "7777777777"
            + LiteRenderer.DELIM + "astringObjType" + LiteRenderer.DELIM + "A String"
            + LiteRenderer.DELIM + "ashortType" + LiteRenderer.DELIM + "231"
            + LiteRenderer.DELIM + "ashortObjType" + LiteRenderer.DELIM + "786"
            + LiteRenderer.DELIM + "alongType" + LiteRenderer.DELIM + "327641237623423"
            + LiteRenderer.DELIM + "alongObjType" + LiteRenderer.DELIM + "876328471234"
            + LiteRenderer.DELIM + "abigDecimalObjType" + LiteRenderer.DELIM + "9872876349183274123432.876128716235487621432";

        Types obj1 = (Types) LiteParser.parse(s, os);

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
        assertTrue((short) 231 == obj1.getShortType());
        assertTrue(327641237623423l == obj1.getLongType());
        assertEquals(new Short((short) 786), obj1.getShortObjType());
        assertEquals(new Long(876328471234l), obj1.getLongObjType());
        assertEquals(new BigDecimal("9872876349183274123432.876128716235487621432"), obj1.getBigDecimalObjType());
    }


    public void testParseNull() throws Exception{
        try {
            LiteParser.parse(null, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testParseNullXml() throws Exception{
        try {
            LiteParser.parseXml(null, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testParseEmptyLastString() throws Exception {
        Employee obj1 = (Employee) LiteParser.parse(
                "org.intermine.model.testmodel.Employee" + LiteRenderer.DELIM
                + "org.intermine.model.testmodel.Employable" + LiteRenderer.DELIM
                + "aid" + LiteRenderer.DELIM + "1234" + LiteRenderer.DELIM
                + "aname" + LiteRenderer.DELIM, os);

        assertEquals("", obj1.getName());
        assertEquals(new Integer(1234), obj1.getId());
    }
}
