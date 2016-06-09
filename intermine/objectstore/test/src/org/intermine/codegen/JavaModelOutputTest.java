package org.intermine.codegen;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;


public class JavaModelOutputTest extends TestCase
{

    private String INDENT = JavaModelOutput.INDENT;
    private String ENDL = JavaModelOutput.ENDL;
    private File file;
    private JavaModelOutput mo;

    public JavaModelOutputTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        file = new File("temp.xml");
        Model emptyModel = new Model("model", "hello", new HashSet());
        mo = new JavaModelOutput(emptyModel, file);
    }

    public void testProcess() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model processModel = new Model("model", "", new HashSet(Collections.singleton(cld1)));

        new JavaModelOutput(processModel, new File("./")).process();

        File processFile = new File("./Class1.java");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(processFile));
            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + ENDL);
            }

            String expected = "public class Class1 implements org.intermine.model.InterMineObject" + ENDL + "{" + ENDL
                + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
                + INDENT + "protected java.lang.Integer id;" + ENDL
                + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
                + INDENT + "public void setId(final java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
                + INDENT + "@Override public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : this == o; }" + ENDL
                + INDENT + "@Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
                + INDENT + "@Override public String toString() { return \"Class1 [id=\" + id + \"]\"; }" + ENDL;
            assertTrue(buffer.toString(), buffer.toString().contains(expected));
        } finally {
            processFile.delete();
        }
    }

    public void testGenerateClassDescriptorIsClass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Collections.singleton(cld1)));

        String expected = "public class Class1 implements org.intermine.model.InterMineObject" + ENDL + "{" + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(final java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "@Override public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : this == o; }" + ENDL
            + INDENT + "@Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "@Override public String toString() { return \"Class1 [id=\" + id + \"]\"; }" + ENDL;

        assertTrue(expected, mo.generate(cld1, false).contains(expected));
    }

    public void testGenerateClassDescriptorIsInterface() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Interface1", null, true, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Collections.singleton(cld1)));

        String expected = "package package.name;" + ENDL + ENDL
            + "public interface Interface1 extends org.intermine.model.InterMineObject" + ENDL + "{" + ENDL
            + "}" + ENDL;

        assertEquals(expected, mo.generate(cld1, false));
    }

    public void testGenerateClassDescriptorHasSuperclass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", "package.name.Class1", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "public class Class2 extends package.name.Class1" + ENDL + "{" + ENDL
            + INDENT + "@Override public boolean equals(Object o) { return (o instanceof Class2 && id != null) ? id.equals(((Class2)o).getId()) : this == o; }" + ENDL
            + INDENT + "@Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "@Override public String toString() { return \"Class2 [id=\" + id + \"]\"; }" + ENDL;

        assertTrue(expected, mo.generate(cld2, false).contains(expected));
    }

    public void testGenerateClassDescriptorHasSubclasses() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", "package.name.Class1", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "public class Class1 implements org.intermine.model.InterMineObject" + ENDL + "{" + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(final java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "@Override public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : this == o; }" + ENDL
            + INDENT + "@Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "@Override public String toString() { return \"Class1 [id=\" + id + \"]\"; }" + ENDL;

        assertTrue(expected, mo.generate(cld1, false).contains(expected));
    }

    public void testGenerateClassDescriptorImplementsInterfaces() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Interface1", null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Interface2", null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("package.name.Class1", "package.name.Interface1 package.name.Interface2", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        String expected = "public class Class1 implements package.name.Interface1, package.name.Interface2" + ENDL + "{" + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(final java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "@Override public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : this == o; }" + ENDL
            + INDENT + "@Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "@Override public String toString() { return \"Class1 [id=\" + id + \"]\"; }" + ENDL;

        assertTrue(expected, mo.generate(cld3, false).contains(expected));
    }

    public void testGenerateClassDescriptorHasFields() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "package.name.Class2", null);
        Set refs = new HashSet(Collections.singleton(rfd1));
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "package.name.Class2", null);
        Set cols = new HashSet(Collections.singleton(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, atts, refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "public class Class1 implements org.intermine.model.InterMineObject" + ENDL + "{" + ENDL
            + INDENT + "// Attr: package.name.Class1.atd1" + ENDL
            + INDENT + "protected java.lang.String atd1;" + ENDL
            + INDENT + "public java.lang.String getAtd1() { return atd1; }" + ENDL
            + INDENT + "public void setAtd1(final java.lang.String atd1) { this.atd1 = atd1; }" + ENDL + ENDL
            + INDENT + "// Ref: package.name.Class1.rfd1" + ENDL
            + INDENT + "protected org.intermine.model.InterMineObject rfd1;" + ENDL
            + INDENT + "public package.name.Class2 getRfd1() { if (rfd1 instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((package.name.Class2) ((org.intermine.objectstore.proxy.ProxyReference) rfd1).getObject()); }; return (package.name.Class2) rfd1; }" + ENDL
            + INDENT + "public void setRfd1(final package.name.Class2 rfd1) { this.rfd1 = rfd1; }" + ENDL
            + INDENT + "public void proxyRfd1(final org.intermine.objectstore.proxy.ProxyReference rfd1) { this.rfd1 = rfd1; }" + ENDL
            + INDENT + "public org.intermine.model.InterMineObject proxGetRfd1() { return rfd1; }" + ENDL + ENDL
            + INDENT + "// Col: package.name.Class1.cod1" + ENDL
            + INDENT + "protected java.util.Set<package.name.Class2> cod1 = new java.util.HashSet<package.name.Class2>();" + ENDL
            + INDENT + "public java.util.Set<package.name.Class2> getCod1() { return cod1; }" + ENDL
            + INDENT + "public void setCod1(final java.util.Set<package.name.Class2> cod1) { this.cod1 = cod1; }" + ENDL
            + INDENT + "public void addCod1(final package.name.Class2 arg) { cod1.add(arg); }" + ENDL + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(final java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "@Override public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : this == o; }" + ENDL
            + INDENT + "@Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "@Override public String toString() { return \"Class1 [atd1=\" + (atd1 == null ? \"null\" : \"\\\"\" + atd1 + \"\\\"\") + \", id=\" + id + \", rfd1=\" + (rfd1 == null ? \"null\" : (rfd1.getId() == null ? \"no id\" : rfd1.getId().toString())) + \"]\"; }" + ENDL;

        String actual = mo.generate(cld1, false);
        assertTrue(actual, actual.contains(expected));
    }

    public void testGenerateAttributeDescriptor() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", "", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "// Attr: Class1.atd1" + ENDL
            + INDENT + "protected java.lang.String atd1;" + ENDL
            + INDENT + "public java.lang.String getAtd1() { return atd1; }" + ENDL
            + INDENT + "public void setAtd1(final java.lang.String atd1) { this.atd1 = atd1; }" + ENDL + ENDL;

        assertEquals(expected, mo.generate(atd1, true));
    }

    public void testGenerateReferenceDescriptor() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "Class2", null);
        Set refs = new HashSet(Collections.singleton(rfd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), refs, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "// Ref: Class1.rfd1" + ENDL
            + INDENT + "protected org.intermine.model.InterMineObject rfd1;" + ENDL
            + INDENT + "public Class2 getRfd1() { if (rfd1 instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((Class2) ((org.intermine.objectstore.proxy.ProxyReference) rfd1).getObject()); }; return (Class2) rfd1; }" + ENDL
            + INDENT + "public void setRfd1(final Class2 rfd1) { this.rfd1 = rfd1; }" + ENDL
            + INDENT + "public void proxyRfd1(final org.intermine.objectstore.proxy.ProxyReference rfd1) { this.rfd1 = rfd1; }" + ENDL
            + INDENT + "public org.intermine.model.InterMineObject proxGetRfd1() { return rfd1; }" + ENDL + ENDL;

        assertEquals(mo.generate(rfd1, true) + "\n" + expected, expected, mo.generate(rfd1, true));
    }

    public void testGenerateCollectionDescriptor() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "Class2", null);
        Set cols = new HashSet(Collections.singleton(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "// Col: Class1.cod1" + ENDL
            + INDENT + "protected java.util.Set<Class2> cod1 = new java.util.HashSet<Class2>();" + ENDL
            + INDENT + "public java.util.Set<Class2> getCod1() { return cod1; }" + ENDL
            + INDENT + "public void setCod1(final java.util.Set<Class2> cod1) { this.cod1 = cod1; }" + ENDL
            + INDENT + "public void addCod1(final Class2 arg) { cod1.add(arg); }" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cod1, true));
    }

    public void testGenerateGetSet() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", "", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "public java.lang.String getAtd1() { return atd1; }" + ENDL
            + INDENT + "public void setAtd1(final java.lang.String atd1) { this.atd1 = atd1; }" + ENDL;

        assertEquals(expected, mo.generateGetSet(atd1, true));
    }

    public void testGenerateEquals() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Collections.singleton(cld1)));

        String expected =  INDENT + "@Override public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : this == o; }" + ENDL;

        assertEquals(expected, mo.generateEquals(cld1));
    }

    public void testGenerateHashCode() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "@Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL;

        assertEquals(expected, mo.generateHashCode(cld1));
    }

    public void testGenerateToString() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("inty", "int");
        AttributeDescriptor atd2 = new AttributeDescriptor("str", "java.lang.String");
        AttributeDescriptor atd3 = new AttributeDescriptor("integery", "java.lang.Integer");
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "package.name.Class2", null);
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "package.name.Class2", null);
        Set<AttributeDescriptor> atts = new LinkedHashSet(Arrays.asList(atd1, atd2, atd3));
        Set<ReferenceDescriptor> refs = new LinkedHashSet(Arrays.asList(rfd1));
        Set<CollectionDescriptor> cols = new LinkedHashSet(Arrays.asList(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, atts, refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new LinkedHashSet(Arrays.asList(cld1, cld2)));

        String expected = INDENT + "@Override public String toString() { return \"Class1 [id=\" + id + \", integery=\" + integery + \", inty=\" + inty + \", rfd1=\" + (rfd1 == null ? \"null\" : (rfd1.getId() == null ? \"no id\" : rfd1.getId().toString())) + \", str=\" + (str == null ? \"null\" : \"\\\"\" + str + \"\\\"\") + \"]\"; }" + ENDL;

        assertEquals(expected, mo.generateToString(cld1));
    }

    public void testGetType() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        assertEquals("java.lang.String", mo.getType(atd1));

        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "package.name.Class2", null);
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "package.name.Class2", null);
        Set refs = new HashSet(Collections.singleton(rfd1));
        Set cols = new HashSet(Collections.singleton(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        assertEquals("package.name.Class2", mo.getType(rfd1));
        assertEquals("java.util.Set<package.name.Class2>", mo.getType(cod1));
    }

    public void testGenerateMultiInheritanceLegal() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "int");
        Set atds1 = new HashSet(Collections.singleton(atd1));
        AttributeDescriptor atd2 = new AttributeDescriptor("atd1", "int");
        Set atds2 = new HashSet(Collections.singleton(atd2));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, true, atds1, new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, true, atds2, new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("package.name.Class3", "package.name.Class1 package.name.Class2", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", "package.name", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        String expected = "public class Class3 implements package.name.Class1, package.name.Class2" + ENDL + "{" + ENDL
            + INDENT + "// Attr: package.name.Class1.atd1" + ENDL
            + INDENT + "protected int atd1;" + ENDL
            + INDENT + "public int getAtd1() { return atd1; }" + ENDL
            + INDENT + "public void setAtd1(final int atd1) { this.atd1 = atd1; }" + ENDL + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(final java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "@Override public boolean equals(Object o) { return (o instanceof Class3 && id != null) ? id.equals(((Class3)o).getId()) : this == o; }" + ENDL
            + INDENT + "@Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "@Override public String toString() { return \"Class3 [atd1=\" + atd1 + \", id=\" + id + \"]\"; }" + ENDL;

        String actual = mo.generate(cld3, false);
        assertTrue(actual, actual.contains(expected));
    }
}

