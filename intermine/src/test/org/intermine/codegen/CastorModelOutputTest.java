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

import java.util.*;
import java.io.*;

import org.flymine.util.*;
import org.flymine.metadata.*;


public class CastorModelOutputTest extends TestCase
{

    private String INDENT = ModelOutput.INDENT;
    private String ENDL = ModelOutput.ENDL;
    private Model model;
    private File file;
    private CastorModelOutput mo;


    public CastorModelOutputTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        model = new Model("model", new HashSet());
        file = new File("temp.xml");
        mo = new CastorModelOutput(model, file);
    }

    public void testProcess() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2})));

        File path = new File("./");
        CastorModelOutput mo = new CastorModelOutput(model, path);
        mo.process();

        File file = new File("./castor_xml_model.xml");
        FileReader reader = null;
        try {
            reader = new FileReader(file);

        } catch (FileNotFoundException e) {
            fail("file (" + file.getName() + ") not created successfully");
        } finally {
            file.delete();
        }

        try {
            char[] text = new char[1024];
            reader.read(text);

            String expected = "<!DOCTYPE databases PUBLIC \"-//EXOLAB/Castor Mapping DTD Version 1.0//EN\" "
            + "\"http://castor.exolab.org/mapping.dtd\">" + ENDL + "<mapping>" + ENDL
            + INDENT + "<include href=\"castor_xml_include.xml\"/>" + ENDL + ENDL
            + INDENT + "<class name=\"Class1\" auto-complete=\"true\" identity=\"id\">" + ENDL
            + INDENT + INDENT + "<field name=\"id\" type=\"java.lang.Integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + "</class>" + ENDL + ENDL
            + INDENT + "<class name=\"Class2\" auto-complete=\"true\" identity=\"id\">" + ENDL
            + INDENT + INDENT + "<field name=\"id\" type=\"java.lang.Integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + "</class>" + ENDL + ENDL
            + "</mapping>" + ENDL;

            // trim text then add a single ENDL
            assertEquals(expected, (new String(text)).trim() + ENDL);
        } finally {
            file.delete();
        }
    }

    public void testGenerateModel() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "<!DOCTYPE databases PUBLIC \"-//EXOLAB/Castor Mapping DTD Version 1.0//EN\" "
            + "\"http://castor.exolab.org/mapping.dtd\">" + ENDL + "<mapping>" + ENDL
            + INDENT + "<include href=\"castor_xml_include.xml\"/>" + ENDL + ENDL
            + INDENT + "<class name=\"Class1\" auto-complete=\"true\" identity=\"id\">" + ENDL
            + INDENT + INDENT + "<field name=\"id\" type=\"java.lang.Integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + "</class>" + ENDL + ENDL
            + INDENT + "<class name=\"Class2\" auto-complete=\"true\" identity=\"id\">" + ENDL
            + INDENT + INDENT + "<field name=\"id\" type=\"java.lang.Integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + "</class>" + ENDL + ENDL
            + "</mapping>" + ENDL;

        assertEquals(expected, mo.generate(model));
    }

    // should ignore interface
    public void testGenerateModelInterface() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Interface1", null, true, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        String expected = "<!DOCTYPE databases PUBLIC \"-//EXOLAB/Castor Mapping DTD Version 1.0//EN\" "
            + "\"http://castor.exolab.org/mapping.dtd\">" + ENDL + "<mapping>" + ENDL
            + INDENT + "<include href=\"castor_xml_include.xml\"/>" + ENDL + ENDL
            + INDENT + "<class name=\"Class1\" auto-complete=\"true\" identity=\"id\">" + ENDL
            + INDENT + INDENT + "<field name=\"id\" type=\"java.lang.Integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + "</class>" + ENDL + ENDL
            + INDENT + "<class name=\"Class2\" auto-complete=\"true\" identity=\"id\">" + ENDL
            + INDENT + INDENT + "<field name=\"id\" type=\"java.lang.Integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + "</class>" + ENDL + ENDL
            + "</mapping>" + ENDL;

        assertEquals(expected, mo.generate(model));
    }

    public void testGenerateClassDescriptorClass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "<class name=\"Class1\" auto-complete=\"true\" identity=\"id\">" + ENDL
            + INDENT + INDENT + "<field name=\"id\" type=\"java.lang.Integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + "</class>" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorFields() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "int");
        Set atts = new HashSet(Collections.singleton(atd1));

        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        Set refs = new HashSet(Collections.singleton(rfd1));

        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, true);
        Set cols = new HashSet(Collections.singleton(cod1));

        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, atts, refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "<class name=\"Class1\" auto-complete=\"true\" identity=\"id\">" + ENDL
            + INDENT + INDENT + "<field name=\"id\" type=\"java.lang.Integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"_xml-id\" type=\"string\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + INDENT + "<field name=\"atd1\" type=\"integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"atd1\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + INDENT + "<field name=\"rfd1\" type=\"Class2\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"rfd1\" reference=\"true\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + INDENT + "<field name=\"cod1\" type=\"Class2\" collection=\"collection\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"cod1\" reference=\"true\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL
            + INDENT + "</class>" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorSuperclass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "<class name=\"Class2\" extends=\"Class1\" auto-complete=\"true\" identity=\"id\">" + ENDL
            + INDENT + "</class>" + ENDL + ENDL;

        mo.seen.add(cld1);
        assertEquals(expected, mo.generate(cld2));
    }

    public void testGenerateAttributeDescriptorPrimitive() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "int");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + INDENT + "<field name=\"atd1\" type=\"integer\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"atd1\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL;

        assertEquals(expected, mo.generate(atd1));
    }

    public void testGenerateAttributeDescriptorString() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + INDENT + "<field name=\"atd1\" type=\"java.lang.String\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"atd1\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL;

        assertEquals(expected, mo.generate(atd1));
    }

    // Just test with an object that is not one of the basic types
    public void testGenerateAttributeDescriptorObject() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "java.util.Currency");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + INDENT + "<field name=\"atd1\" type=\"java.util.Currency\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"atd1\" node=\"element\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL;

        assertEquals(expected, mo.generate(atd1));
    }

    public void testGenerateReferenceDescriptor() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        Set refs = new HashSet(Collections.singleton(rfd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), refs, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + INDENT + "<field name=\"rfd1\" type=\"Class2\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"rfd1\" reference=\"true\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL;

        mo.references = new StringBuffer();
        mo.generate(rfd1);
        assertEquals(expected, mo.references.toString());
    }

    public void testGenerateCollectionDescriptor() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, true);
        Set cols = new HashSet(Collections.singleton(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + INDENT + "<field name=\"cod1\" type=\"Class2\" collection=\"collection\">" + ENDL
            + INDENT + INDENT + INDENT + "<bind-xml name=\"cod1\" reference=\"true\" node=\"attribute\"/>" + ENDL
            + INDENT + INDENT + "</field>" + ENDL;

        mo.collections = new StringBuffer();
        mo.generate(cod1);
        assertEquals(expected, mo.collections.toString());
    }

    public void testConvertType() throws Exception {
        assertEquals("integer", mo.convertType("int"));
        assertEquals("some_type", mo.convertType("some_type"));
        try {
            mo.convertType(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }
}
