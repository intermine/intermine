package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;

import org.intermine.modelproduction.ModelParser;
import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

import junit.framework.TestCase;

public class XmlMetaDataTest extends TestCase
{

    public XmlMetaDataTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        Reader xsdReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("xmlschematest.xsd"));
        XmlMetaData xmlInfo = new XmlMetaData(xsdReader);

        System.out.println("idFields: " + xmlInfo.idFields);
        System.out.println("refFields: " + xmlInfo.refFields);

        assertTrue(xmlInfo.isReference("company/department/employee/businessAddress"));
        assertTrue(xmlInfo.isReference("company/department/manager/businessAddress"));
        assertTrue(xmlInfo.isReference("company/addressBook/address"));
        assertTrue(xmlInfo.isId("company/address"));
        assertEquals("ref", xmlInfo.getReferenceField("company/department/employee/businessAddress"));
        assertEquals("ref", xmlInfo.getReferenceField("company/department/manager/businessAddress"));
        assertEquals("ref", xmlInfo.getReferenceField("company/addressBook/address"));
        assertEquals("id", xmlInfo.getIdField("company/address"));
        assertEquals("company/address", xmlInfo.getIdPath("company/department/employee/businessAddress"));

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

