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


public class JavaModelOutputTest extends TestCase
{

    private String INDENT = ModelOutput.INDENT;
    private String ENDL = ModelOutput.ENDL;
    private Model model;
    private File file;
    private JavaModelOutput mo;


    public JavaModelOutputTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        model = new Model("model", new HashSet());
        file = new File("temp.xml");
        mo = new JavaModelOutput(model, file);
    }

    public void testProcess() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        File path = new File("./");
        JavaModelOutput mo = new JavaModelOutput(model, path);
        mo.process();

        File file = new File("./Class1.java");
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

            String expected = "public class Class1" + ENDL + "{" + ENDL
            + INDENT + "protected Integer id;" + ENDL
            + INDENT + "public Integer getId() { return id; }" + ENDL + ENDL + "}";

            assertEquals(expected, (new String(text)).trim());
        } finally {
            file.delete();
        }


    }


    public void testGenerateModel() throws Exception {
        assertNull(mo.generate(model));
    }

    public void testGenerateClassDescriptorIsClass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class1" + ENDL + "{" + ENDL
            + INDENT + "protected Integer id;" + ENDL
            + INDENT + "public Integer getId() { return id; }" + ENDL + ENDL + "}";

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorIsInterface() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = "package package.name;" + ENDL + ENDL
            + "public interface Interface1" + ENDL + "{" + ENDL
            + "}";

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorHasSuperclass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", "package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class2 extends Class1" + ENDL + "{" + ENDL
            + INDENT + "protected String ojbConcreteClass = \"package.name.Class2\";" + ENDL + ENDL
            + "}";

        assertEquals(expected, mo.generate(cld2));
    }

    public void testGenerateClassDescriptorHasSubclasses() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", "package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class1" + ENDL + "{" + ENDL
            + INDENT + "protected Integer id;" + ENDL
            + INDENT + "public Integer getId() { return id; }" + ENDL + ENDL
            + INDENT + "protected String ojbConcreteClass = \"package.name.Class1\";" + ENDL + ENDL + "}";

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorImplementsInterfaces() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Interface2", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("package.name.Class1", null, "Interface1 Interface2", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class1 implements Interface1, Interface2" + ENDL + "{" + ENDL
            + INDENT + "protected Integer id;" + ENDL
            + INDENT + "public Integer getId() { return id; }" + ENDL + ENDL + "}";

        assertEquals(expected, mo.generate(cld3));
    }

    public void testGenerateClassDescriptorHasFields() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", false, "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "package.name.Class2", null);
        Set refs = new HashSet(Collections.singleton(rfd1));
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "package.name.Class2", null, true);
        Set cols = new HashSet(Collections.singleton(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, atts, refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class1" + ENDL + "{" + ENDL
            + INDENT + "protected Integer id;" + ENDL
            + INDENT + "public Integer getId() { return id; }" + ENDL + ENDL
            + INDENT + "protected java.lang.String atd1;" + ENDL
            + INDENT + "public java.lang.String getAtd1() { return this.atd1; }" + ENDL
            + INDENT + "public void setAtd1(java.lang.String atd1) { this.atd1=atd1; }" + ENDL + ENDL
            + INDENT + "protected Class2 rfd1;" + ENDL
            + INDENT + "public package.name.Class2 getRfd1() { return this.rfd1; }" + ENDL
            + INDENT + "public void setRfd1(package.name.Class2 rfd1) { this.rfd1=rfd1; }" + ENDL + ENDL
            + INDENT + "protected java.util.List cod1 = new java.util.ArrayList();" + ENDL
            + INDENT + "public java.util.List getCod1() { return this.cod1; }" + ENDL
            + INDENT + "public void setCod1(java.util.List cod1) { this.cod1=cod1; }" + ENDL + ENDL
            + "}";

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateAttributeDescriptor() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "protected java.lang.String atd1;" + ENDL
            + INDENT + "public java.lang.String getAtd1() { return this.atd1; }" + ENDL
            + INDENT + "public void setAtd1(java.lang.String atd1) { this.atd1=atd1; }" + ENDL + ENDL;

        assertEquals(expected, mo.generate(atd1));
    }

    public void testGenerateReferenceDescriptorOJB() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        Set refs = new HashSet(Collections.singleton(rfd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), refs, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "protected Class2 rfd1;" + ENDL
            + INDENT + "public Class2 getRfd1() { return this.rfd1; }" + ENDL
            + INDENT + "public void setRfd1(Class2 rfd1) { this.rfd1=rfd1; }" + ENDL + ENDL;

        assertEquals(expected, mo.generate(rfd1));
    }

    public void testGenerateCollectionDescriptorUnordered() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, false);
        Set cols = new HashSet(Collections.singleton(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "protected java.util.Set cod1 = new java.util.HashSet();" + ENDL
            + INDENT + "public java.util.Set getCod1() { return this.cod1; }" + ENDL
            + INDENT + "public void setCod1(java.util.Set cod1) { this.cod1=cod1; }" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cod1));
    }

    public void testGenerateCollectionDescriptorOrdered() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, true);
        Set cols = new HashSet(Collections.singleton(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "protected java.util.List cod1 = new java.util.ArrayList();" + ENDL
            + INDENT + "public java.util.List getCod1() { return this.cod1; }" + ENDL
            + INDENT + "public void setCod1(java.util.List cod1) { this.cod1=cod1; }" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cod1));
    }

    public void testGenerateGetSet() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "public java.lang.String getAtd1() { return this.atd1; }" + ENDL
            + INDENT + "public void setAtd1(java.lang.String atd1) { this.atd1=atd1; }" + ENDL;

        assertEquals(expected, mo.generateGetSet(atd1));
    }


    public void testGenerateEquals() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "public boolean equals(Object o) {" + ENDL
            + INDENT + INDENT + "if (!(o instanceof Class1)) return false;" + ENDL
            + INDENT + INDENT + "return (id == null) ? equalsPK(o) : id.equals(((Class1)o).getId());" + ENDL
            + INDENT + "}" + ENDL + ENDL;

        assertEquals(expected, mo.generateEquals(cld1));

    }

    public void testGenerateEqualsPKObject() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "public boolean equalsPK(Object o) {" + ENDL
            + INDENT + INDENT + "if (!(o instanceof Class1)) return false;" + ENDL
            + INDENT + INDENT + "Class1 obj = (Class1) o;" + ENDL
            + INDENT + INDENT + "return obj.getId() == null && (obj.getAtd1() == null ? (atd1 == null) : obj.getAtd1().equals(atd1));" + ENDL
            + INDENT + "}" + ENDL + ENDL;

        assertEquals(expected, mo.generateEqualsPK(cld1));
    }

    public void testGenerateEqualsPKPrimitive() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "int");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "public boolean equalsPK(Object o) {" + ENDL
            + INDENT + INDENT + "if (!(o instanceof Class1)) return false;" + ENDL
            + INDENT + INDENT + "Class1 obj = (Class1) o;" + ENDL
            + INDENT + INDENT + "return obj.getId() == null && obj.getAtd1() == atd1;" + ENDL
            + INDENT + "}" + ENDL + ENDL;

        assertEquals(expected, mo.generateEqualsPK(cld1));
    }

    public void testGenerateHashCodeObject() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT  + "public int hashCode() {" + ENDL
            + INDENT + INDENT + "if (id != null) return id.hashCode();" + ENDL
            + INDENT + INDENT + "return (atd1 == null ? 0 : atd1.hashCode());" + ENDL
            + INDENT + "}" + ENDL + ENDL;

        assertEquals(expected, mo.generateHashCode(cld1));
    }

    public void testGenerateHashCodePrimitive() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "float");
        AttributeDescriptor atd2 = new AttributeDescriptor("atd2", true, "int");
        Set atts = new LinkedHashSet(Arrays.asList(new Object[] { atd1, atd2}));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT  + "public int hashCode() {" + ENDL
            + INDENT + INDENT + "if (id != null) return id.hashCode();" + ENDL
            + INDENT + INDENT + "return ((int) atd1) ^ ((int) atd2);" + ENDL
            + INDENT + "}" + ENDL + ENDL;

        assertEquals(expected, mo.generateHashCode(cld1));
    }

    public void testGenerateHashCodeBoolean() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "boolean");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT  + "public int hashCode() {" + ENDL
            + INDENT + INDENT + "if (id != null) return id.hashCode();" + ENDL
            + INDENT + INDENT + "return (atd1 ? 0 : 1);" + ENDL
            + INDENT + "}" + ENDL + ENDL;

        assertEquals(expected, mo.generateHashCode(cld1));
    }


    public void testGenerateToString() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "int");
        AttributeDescriptor atd2 = new AttributeDescriptor("atd2", true, "int");
        Set atts = new LinkedHashSet(Arrays.asList(new Object[] {atd1, atd2}));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new LinkedHashSet(Collections.singleton(cld1)));

        String expected = INDENT + "public String toString() { return \"Class1 [\"+id+\"] \"+atd1+\", \"+atd2; }" + ENDL;

        assertEquals(expected, mo.generateToString(cld1));
    }



    public void testGetType() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", true, "java.lang.String");
        assertEquals("java.lang.String", mo.getType(atd1));

        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, true);
        CollectionDescriptor cod2 = new CollectionDescriptor("cod2", false, "Class2", null, false);
        Set refs = new HashSet(Collections.singleton(rfd1));
        Set cols = new HashSet(Arrays.asList(new Object[] {cod1, cod2}));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        assertEquals("Class2", mo.getType(rfd1));
        assertEquals("java.util.List", mo.getType(cod1));
        assertEquals("java.util.Set", mo.getType(cod2));
    }
}

