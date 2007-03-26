package org.intermine.modelproduction.xmlschema;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.Reader;

import junit.framework.TestCase;

public class XmlMetaDataTest extends TestCase
{
    XmlMetaData xmlInfo;

    public XmlMetaDataTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        Reader xsdReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("xmlschematest.xsd"));
        xmlInfo = new XmlMetaData(xsdReader);
    }

    public void testReferenceElements() throws Exception {
        assertTrue(xmlInfo.isReferenceElement("company/department/employee/businessAddress"));
        assertTrue(xmlInfo.isReferenceElement("company/department/manager/businessAddress"));
        assertTrue(xmlInfo.isReferenceElement("company/addressBook/address"));

        assertEquals("ref", xmlInfo.getReferenceElementField("company/department/employee/businessAddress"));
        assertEquals("ref", xmlInfo.getReferenceElementField("company/department/manager/businessAddress"));
        assertEquals("ref", xmlInfo.getReferenceElementField("company/addressBook/address"));
    }

    public void testKeyRef() throws Exception {
        assertEquals("addressKey", xmlInfo.getReferencingKeyName("company/department/employee/businessAddress", "ref"));
    }

    public void testKey() throws Exception {
        assertEquals("company/address", xmlInfo.getKeyPath("addressKey"));
        assertEquals("id", xmlInfo.getKeyField("addressKey"));
    }

    public void testKeyFields() throws Exception {
        assertTrue(xmlInfo.getKeyFields("company/address").contains("id"));
    }

    public void testClassNames() throws Exception {
        // check correct class names returned for all paths
        assertEquals("Company", xmlInfo.getClsNameFromXPath("company"));
        assertEquals("Department_Company", xmlInfo.getClsNameFromXPath("company/department"));
        assertEquals("Employee", xmlInfo.getClsNameFromXPath("company/department/manager"));
        assertEquals("Employee", xmlInfo.getClsNameFromXPath("company/department/employee"));
        assertEquals("Address_Company", xmlInfo.getClsNameFromXPath("company/address"));
        assertEquals("Address_Company", xmlInfo.getClsNameFromXPath("company/department/manager/businessAddress"));
        assertEquals("Address_Company", xmlInfo.getClsNameFromXPath("company/department/employee/businessAddress"));
        assertEquals("Address_Company", xmlInfo.getClsNameFromXPath("company/addressBook/address"));
    }
}

