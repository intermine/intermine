package org.flymine.codegen;

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

import java.util.ArrayList;

import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

import org.flymine.metadata.Model;
import org.flymine.objectstore.webservice.ser.SerializationUtil;

import org.flymine.model.testmodel.Employee;

public class AxisModelOutputTest extends TestCase
{
    private String INDENT = ModelOutput.INDENT;
    private String ENDL = ModelOutput.ENDL;
    private Model model;
    private AxisModelOutput mo;
    private TypeMapping tm;

    public AxisModelOutputTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
        tm = ((Call) new Service().createCall()).getTypeMapping();
        SerializationUtil.registerDefaultMappings(tm);
        SerializationUtil.registerMappings(tm, model);
        mo = new AxisModelOutput(model, null);
    }

    public void testGenerateArrayList() {
        String expected = "<typeMapping qname=\"xsd:list\""
            + " xmlns:xsd=\"http://soapinterop.org/xsd\""
            + " type=\"java:java.util.ArrayList\""
            + " serializer=\"org.flymine.objectstore.webservice.ser.ListSerializerFactory\""
            + " deserializer=\"org.flymine.objectstore.webservice.ser.ListDeserializerFactory\""
            + " encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"/>";
        assertEquals(mo.generateTypeMapping(ArrayList.class, tm) + " " + expected, mo.generateTypeMapping(ArrayList.class, tm), expected);
    }

    public void testGenerateEmployee() {
        String expected = "<typeMapping qname=\"Employee\""
            + " type=\"java:org.flymine.model.testmodel.Employee\""
            + " serializer=\"org.flymine.objectstore.webservice.ser.DefaultSerializerFactory\""
            + " deserializer=\"org.flymine.objectstore.webservice.ser.DefaultDeserializerFactory\""
            + " encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"/>";
        assertEquals(mo.generateTypeMapping(Employee.class, tm), expected);
    }
}
