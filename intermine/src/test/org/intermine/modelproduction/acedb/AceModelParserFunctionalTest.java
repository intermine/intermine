package org.intermine.modelproduction.acedb;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.InputStreamReader;

import java.util.Set;
import java.util.LinkedHashSet;

import org.intermine.modelproduction.ModelParser;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

import junit.framework.TestCase;

public class AceModelParserFunctionalTest extends TestCase
{
    private static final String MODEL = "acedbtest";
    private static final String PKG = "org.intermine.model." + MODEL;
    private static final String NS = "http://www.intermine.org/model";
    private static final String uri = NS + "/testmodel";

    private ModelParser parser;
    private Reader reader;
    private Model model;

    public AceModelParserFunctionalTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        parser = new AceModelParser(MODEL, PKG, NS);
        reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(MODEL + ".wrm"));
        model = createModel();
    }

    public void testProcess() throws Exception {
        assertEquals(model, parser.process(reader));
    }

    private Model createModel() throws Exception {
        Set atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", "java.lang.String"));
        atts.add(new AttributeDescriptor("intValue", "java.lang.Integer"));
        atts.add(new AttributeDescriptor("stringValue", "java.lang.String"));
        atts.add(new AttributeDescriptor("stringValue_2", "java.lang.String"));
        atts.add(new AttributeDescriptor("onOrOff", "boolean"));
        atts.add(new AttributeDescriptor("dateValue", "java.util.Date"));
        Set refs = new LinkedHashSet();
        refs.add(new ReferenceDescriptor("reference", PKG + ".AceTestObject", null));
        refs.add(new ReferenceDescriptor("hashValue", PKG + ".AceTestObject", null));
        Set cols = new LinkedHashSet();
        cols.add(new CollectionDescriptor("stringValues", PKG + ".Text", null, false));
        cols.add(new CollectionDescriptor("references", PKG + ".AceTestObject", null, false));
        cols.add(new CollectionDescriptor("hashValues", PKG + ".AceTestObject", null, false));
        Set clds = new LinkedHashSet();
        ((AceModelParser) parser).addBuiltinClasses(clds);
        clds.add(new ClassDescriptor(PKG + ".AceTestObject", null, false, atts, refs, cols));
        return new Model(MODEL, uri, clds);
    }
}
