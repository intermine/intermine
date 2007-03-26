package org.intermine.util;

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

public class XmlUtilTest extends TestCase
{
    public void testXmlToJava() throws Exception {
        assertEquals("java.lang.String", XmlUtil.xmlToJavaType("string"));
        assertEquals("java.lang.String", XmlUtil.xmlToJavaType("normalizedString"));
        assertEquals("java.lang.String", XmlUtil.xmlToJavaType("language"));
        assertEquals("java.lang.String", XmlUtil.xmlToJavaType("Name"));
        assertEquals("java.lang.String", XmlUtil.xmlToJavaType("NCName"));
        assertEquals("java.lang.Integer", XmlUtil.xmlToJavaType("positiveInteger"));
        assertEquals("java.lang.Integer", XmlUtil.xmlToJavaType("negativeInteger"));
        assertEquals("java.lang.Integer", XmlUtil.xmlToJavaType("int"));
        assertEquals("java.lang.Integer", XmlUtil.xmlToJavaType("nonNegativeInteger"));
        assertEquals("java.lang.Integer", XmlUtil.xmlToJavaType("unsignedInt"));
        assertEquals("java.lang.Integer", XmlUtil.xmlToJavaType("integer"));
        assertEquals("java.lang.Integer", XmlUtil.xmlToJavaType("nonPositiveInteger"));
        assertEquals("java.lang.Short", XmlUtil.xmlToJavaType("short"));
        assertEquals("java.lang.Short", XmlUtil.xmlToJavaType("unsignedShort"));
        assertEquals("java.lang.Long", XmlUtil.xmlToJavaType("long"));
        assertEquals("java.lang.Long", XmlUtil.xmlToJavaType("unsignedLong"));
        assertEquals("java.lang.Byte", XmlUtil.xmlToJavaType("byte"));
        assertEquals("java.lang.Byte", XmlUtil.xmlToJavaType("unsignedByte"));
        assertEquals("java.lang.Float", XmlUtil.xmlToJavaType("float"));
        assertEquals("java.lang.Float", XmlUtil.xmlToJavaType("decimal"));
        assertEquals("java.lang.Double", XmlUtil.xmlToJavaType("double"));
        assertEquals("java.lang.Boolean", XmlUtil.xmlToJavaType("boolean"));
        assertEquals("java.net.URL", XmlUtil.xmlToJavaType("anyURI"));
        assertEquals("java.util.Date", XmlUtil.xmlToJavaType("dateTime"));

        try {
            XmlUtil.xmlToJavaType("rubbish");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testJavaToXml() throws Exception {
        assertEquals(XmlUtil.XSD_NAMESPACE + "string", XmlUtil.javaToXmlType("java.lang.String"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "integer", XmlUtil.javaToXmlType("java.lang.Integer"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "integer", XmlUtil.javaToXmlType("int"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "short", XmlUtil.javaToXmlType("java.lang.Short"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "short", XmlUtil.javaToXmlType("short"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "long", XmlUtil.javaToXmlType("java.lang.Long"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "long", XmlUtil.javaToXmlType("long"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "double", XmlUtil.javaToXmlType("java.lang.Double"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "double", XmlUtil.javaToXmlType("double"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "float", XmlUtil.javaToXmlType("java.lang.Float"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "float", XmlUtil.javaToXmlType("float"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "boolean", XmlUtil.javaToXmlType("java.lang.Boolean"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "boolean", XmlUtil.javaToXmlType("boolean"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "byte", XmlUtil.javaToXmlType("java.lang.Byte"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "byte", XmlUtil.javaToXmlType("byte"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "anyURI", XmlUtil.javaToXmlType("java.net.URL"));
        assertEquals(XmlUtil.XSD_NAMESPACE + "dateTime", XmlUtil.javaToXmlType("java.util.Date"));
        try {
            XmlUtil.javaToXmlType("rubbish");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testCorrectNamespace() throws Exception {
        assertEquals("http://www.intermine.org/test#",
                     XmlUtil.correctNamespace("http://www.intermine.org/test#junk"));
        assertEquals("http://www.intermine.org/test#",
                     XmlUtil.correctNamespace("http://www.intermine.org/test#junk#morejunk"));
        assertEquals("http://www.intermine.org/test#",
                     XmlUtil.correctNamespace("http://www.intermine.org/test/"));
        assertEquals("http://www.intermine.org/test#",
                     XmlUtil.correctNamespace("http://www.intermine.org/test"));
    }
    
    public void testIndentXmlSimple() throws Exception {
        String input = "<query name=\"\" model=\"testmodel\" view=\"Employee\"><node path=\"Employee\" " +
                "type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"=\" " +
                "value=\"10\"></constraint></node><node path=\"Employee.department\" type=\"Department\">" +
                "</node><node path=\"Employee.department.name\" type=\"String\"><constraint op=\"=\" " +
                "value=\"DepartmentA1\"></constraint></node></query>";
        String expected = "<query name=\"\" model=\"testmodel\" view=\"Employee\">\n" + 
                "  <node path=\"Employee\" type=\"Employee\">\n" + 
                "  </node>\n" + 
                "  <node path=\"Employee.age\" type=\"int\">\n" + 
                "    <constraint op=\"=\" value=\"10\">\n" + 
                "    </constraint>\n" + 
                "  </node>\n" + 
                "  <node path=\"Employee.department\" type=\"Department\">\n" + 
                "  </node>\n" + 
                "  <node path=\"Employee.department.name\" type=\"String\">\n" + 
                "    <constraint op=\"=\" value=\"DepartmentA1\">\n" + 
                "    </constraint>\n" + 
                "  </node>\n" + 
                "</query>";
        
        String output = XmlUtil.indentXmlSimple(input);
        System.out.println(output);
        assertEquals(output, expected);
    }
}