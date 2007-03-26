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

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.modelproduction.ModelParser;
import org.intermine.modelproduction.xml.InterMineModelParser;

public class XmlSchemaParserFunctionalTest extends TestCase
{
    private static final String MODEL = "xmlschematest";
    private static final String PKG = "org.intermine.model." + MODEL;
    private static final String TGT_NS = "http://www.intermine.org/model/xmlschematest#";
    private String nameSpace = "http://www.intermine.org/model";

    public XmlSchemaParserFunctionalTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        ModelParser parser1 = new InterMineModelParser();
        Reader reader1 = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(MODEL + ".xml"));
        Model model1 = parser1.process(reader1);
        ModelParser parser2 = new XmlSchemaParser(MODEL, PKG, TGT_NS);
        Reader reader2 = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(MODEL + ".xsd"));
        Model model2 = parser2.process(reader2);
        assertEquals(model1, model2);
    }


    public void testReferences() throws Exception {
        ModelParser parser = new XmlSchemaParser(MODEL, PKG, TGT_NS);
        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(MODEL + ".xsd"));
        Model model = parser.process(reader);

        ClassDescriptor companyCld = model.getClassDescriptorByName("org.intermine.model.xmlschematest.Company");
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("address", "org.intermine.model.xmlschematest.Address_Company", null);
        assertEquals(rfd1, companyCld.getReferenceDescriptorByName("address"));
        CollectionDescriptor cod1 = new CollectionDescriptor("departments", "org.intermine.model.xmlschematest.Department_Company",
                                                             null);
        assertEquals(cod1, companyCld.getCollectionDescriptorByName("departments"));

        ClassDescriptor deptCld = model.getClassDescriptorByName("org.intermine.model.xmlschematest.Department_Company");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("manager", "org.intermine.model.xmlschematest.Employee", null);
        assertEquals(rfd2, deptCld.getReferenceDescriptorByName("manager"));
        CollectionDescriptor cod2 = new CollectionDescriptor("employees", "org.intermine.model.xmlschematest.Employee",
                                                             null);
        assertEquals(cod2, deptCld.getCollectionDescriptorByName("employees"));


        ClassDescriptor empCld = model.getClassDescriptorByName("org.intermine.model.xmlschematest.Employee");
        ReferenceDescriptor rfd3 = new ReferenceDescriptor("businessAddress", "org.intermine.model.xmlschematest.Address_Company", null);
        assertEquals(rfd3, empCld.getReferenceDescriptorByName("businessAddress"));
    }
}

