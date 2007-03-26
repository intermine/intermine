package org.intermine.objectstore.webservice.ser;

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

import java.util.ArrayList;

import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;


public class GenerateWSDDTaskTest extends TestCase
{
    private String INDENT = GenerateWSDDTask.INDENT;
    private String ENDL = GenerateWSDDTask.ENDL;

    private GenerateWSDDTask task;
    private TypeMapping tm;

    public GenerateWSDDTaskTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        tm = ((Call) new Service().createCall()).getTypeMapping();
        MappingUtil.registerDefaultMappings(tm);
        task = new GenerateWSDDTask();
    }

    public void testGenerateArrayList() {
        String expected = "<typeMapping qname=\"xsd:list\""
            + " xmlns:xsd=\"http://soapinterop.org/xsd\""
            + " type=\"java:java.util.ArrayList\""
            + " serializer=\"org.intermine.objectstore.webservice.ser.ListSerializerFactory\""
            + " deserializer=\"org.intermine.objectstore.webservice.ser.ListDeserializerFactory\""
            + " encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"/>";
        assertEquals(task.generateTypeMapping(ArrayList.class, tm) + " " + expected, task.generateTypeMapping(ArrayList.class, tm), expected);
    }
}
