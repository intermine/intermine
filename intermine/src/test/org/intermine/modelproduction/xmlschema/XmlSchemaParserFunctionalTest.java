package org.intermine.modelproduction.xmlschema;

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

public class XmlSchemaParserFunctionalTest extends TestCase
{
    private static final String MODEL = "xmlschematest";
    private static final String PKG = "org.intermine.model." + MODEL + ".";
    private String nameSpace = "http://www.intermine.org/model";

    public XmlSchemaParserFunctionalTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        ModelParser parser1 = new XmlSchemaParser(nameSpace, MODEL);
        Reader reader1 = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(MODEL + ".xsd"));
        Model model1 = parser1.process(reader1);
        ModelParser parser2 = new InterMineModelParser();
        Reader reader2 = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(MODEL + ".xml"));
        Model model2 = parser2.process(reader2);
        assertEquals(model1, model2);
    }

}

