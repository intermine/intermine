package org.flymine.modelproduction.uml;

/*
 * Copyright (C) 2002-2003 FlyMine
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

import org.flymine.modelproduction.ModelParser;
import org.flymine.modelproduction.xml.FlyMineModelParser;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.Model;

import junit.framework.TestCase;

public class XmiParserFunctionalTest extends TestCase
{
    private static final String MODEL = "xmitest";
    private static final String PKG = "org.flymine.model." + MODEL + ".";

    public XmiParserFunctionalTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        ModelParser parser1 = new XmiParser(MODEL);
        Reader reader1 = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(MODEL + ".xmi"));
        Model model1 = parser1.process(reader1);
        ModelParser parser2 = new FlyMineModelParser();
        Reader reader2 = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(MODEL + ".xml"));
        Model model2 = parser2.process(reader2);
        assertEquals(model1, model2);
    }

}
    
