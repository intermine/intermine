package org.intermine.codegen;

/*
 * Copyright (C) 2002-2007 FlyMine
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

    private String INDENT = ModelOutput.INDENT;
    private String ENDL = ModelOutput.ENDL;
    private File file;
    private JavaModelOutput mo;
    private String uri = "http://www.intermine.org/model/testmodel";

    public JavaModelOutputTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        file = new File("temp.xml");
        Model emptyModel = new Model("model", uri, new HashSet());
        mo = new JavaModelOutput(emptyModel, file);
    }

    public void testProcess() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model processModel = new Model("model", uri, new HashSet(Collections.singleton(cld1)));

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
                + INDENT + "public void setId(java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
                + INDENT + "public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : false; }" + ENDL
                + INDENT + "public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
                + INDENT + "public String toString() { return \"Class1 [Id=\\\"\" + id + \"\\\"]\"; }" + ENDL
                + "}" + ENDL;
            assertEquals(expected, buffer.toString());
        } finally {
            processFile.delete();
        }
    }

    public void testGenerateModel() throws Exception {
        Model emptyModel = new Model("model", uri, new HashSet());
        assertNull(mo.generate(emptyModel));
    }

    public void testGenerateClassDescriptorIsClass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Collections.singleton(cld1)));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class1 implements org.intermine.model.InterMineObject" + ENDL + "{" + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : false; }" + ENDL
            + INDENT + "public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "public String toString() { return \"Class1 [Id=\\\"\" + id + \"\\\"]\"; }" + ENDL
            + "}" + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorIsInterface() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Interface1", null, true, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Collections.singleton(cld1)));

        String expected = "package package.name;" + ENDL + ENDL
            + "public interface Interface1 extends org.intermine.model.InterMineObject" + ENDL + "{" + ENDL
            + "}" + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorHasSuperclass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", "package.name.Class1", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class2 extends package.name.Class1" + ENDL + "{" + ENDL
            + INDENT + "public boolean equals(Object o) { return (o instanceof Class2 && id != null) ? id.equals(((Class2)o).getId()) : false; }" + ENDL
            + INDENT + "public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "public String toString() { return \"Class2 [Id=\\\"\" + id + \"\\\"]\"; }" + ENDL
            + "}" + ENDL;

        assertEquals(expected, mo.generate(cld2));
    }

    public void testGenerateClassDescriptorHasSubclasses() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", "package.name.Class1", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class1 implements org.intermine.model.InterMineObject" + ENDL + "{" + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : false; }" + ENDL
            + INDENT + "public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "public String toString() { return \"Class1 [Id=\\\"\" + id + \"\\\"]\"; }" + ENDL
            + "}" + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorImplementsInterfaces() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Interface1", null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Interface2", null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("package.name.Class1", "package.name.Interface1 package.name.Interface2", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class1 implements package.name.Interface1, package.name.Interface2" + ENDL + "{" + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : false; }" + ENDL
            + INDENT + "public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "public String toString() { return \"Class1 [Id=\\\"\" + id + \"\\\"]\"; }" + ENDL
            + "}" + ENDL;

        assertEquals(expected, mo.generate(cld3));
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
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class1 implements org.intermine.model.InterMineObject" + ENDL + "{" + ENDL
            + INDENT + "// Attr: package.name.Class1.atd1" + ENDL
            + INDENT + "protected java.lang.String atd1;" + ENDL
            + INDENT + "public java.lang.String getAtd1() { return atd1; }" + ENDL
            + INDENT + "public void setAtd1(java.lang.String atd1) { this.atd1 = atd1; }" + ENDL + ENDL
            + INDENT + "// Ref: package.name.Class1.rfd1" + ENDL
            + INDENT + "protected org.intermine.model.InterMineObject rfd1;" + ENDL
            + INDENT + "public package.name.Class2 getRfd1() { if (rfd1 instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((package.name.Class2) ((org.intermine.objectstore.proxy.ProxyReference) rfd1).getObject()); }; return (package.name.Class2) rfd1; }" + ENDL
            + INDENT + "public void setRfd1(package.name.Class2 rfd1) { this.rfd1 = rfd1; }" + ENDL
            + INDENT + "public void proxyRfd1(org.intermine.objectstore.proxy.ProxyReference rfd1) { this.rfd1 = rfd1; }" + ENDL
            + INDENT + "public org.intermine.model.InterMineObject proxGetRfd1() { return rfd1; }" + ENDL + ENDL
            + INDENT + "// Col: package.name.Class1.cod1" + ENDL
            + INDENT + "protected java.util.Set cod1 = new java.util.HashSet();" + ENDL
            + INDENT + "public java.util.Set getCod1() { return cod1; }" + ENDL
            + INDENT + "public void setCod1(java.util.Set cod1) { this.cod1 = cod1; }" + ENDL
            + INDENT + "public void addCod1(package.name.Class2 arg) { cod1.add(arg); }" + ENDL + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : false; }" + ENDL
            + INDENT + "public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "public String toString() { return \"Class1 [Atd1=\\\"\" + atd1 + \"\\\", Cod1:Collection, Id=\\\"\" + id + \"\\\", Rfd1=\" + (rfd1 == null ? \"null\" : (rfd1.getId() == null ? \"no id\" : rfd1.getId().toString())) + \"]\"; }" + ENDL
            + "}" + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateAttributeDescriptor() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "// Attr: Class1.atd1" + ENDL
            + INDENT + "protected java.lang.String atd1;" + ENDL
            + INDENT + "public java.lang.String getAtd1() { return atd1; }" + ENDL
            + INDENT + "public void setAtd1(java.lang.String atd1) { this.atd1 = atd1; }" + ENDL + ENDL;

        assertEquals(expected, mo.generate(atd1, true));
    }

    public void testGenerateReferenceDescriptor() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "Class2", null);
        Set refs = new HashSet(Collections.singleton(rfd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), refs, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "// Ref: Class1.rfd1" + ENDL
            + INDENT + "protected org.intermine.model.InterMineObject rfd1;" + ENDL
            + INDENT + "public Class2 getRfd1() { if (rfd1 instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((Class2) ((org.intermine.objectstore.proxy.ProxyReference) rfd1).getObject()); }; return (Class2) rfd1; }" + ENDL
            + INDENT + "public void setRfd1(Class2 rfd1) { this.rfd1 = rfd1; }" + ENDL
            + INDENT + "public void proxyRfd1(org.intermine.objectstore.proxy.ProxyReference rfd1) { this.rfd1 = rfd1; }" + ENDL
            + INDENT + "public org.intermine.model.InterMineObject proxGetRfd1() { return rfd1; }" + ENDL + ENDL;

        assertEquals(mo.generate(rfd1, true) + "\n" + expected, expected, mo.generate(rfd1, true));
    }

    public void testGenerateCollectionDescriptor() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "Class2", null);
        Set cols = new HashSet(Collections.singleton(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "// Col: Class1.cod1" + ENDL
            + INDENT + "protected java.util.Set cod1 = new java.util.HashSet();" + ENDL
            + INDENT + "public java.util.Set getCod1() { return cod1; }" + ENDL
            + INDENT + "public void setCod1(java.util.Set cod1) { this.cod1 = cod1; }" + ENDL
            + INDENT + "public void addCod1(Class2 arg) { cod1.add(arg); }" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cod1, true));
    }

    public void testGenerateGetSet() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "public java.lang.String getAtd1() { return atd1; }" + ENDL
            + INDENT + "public void setAtd1(java.lang.String atd1) { this.atd1 = atd1; }" + ENDL;

        assertEquals(expected, mo.generateGetSet(atd1, true));
    }

    public void testGenerateEquals() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        Set atts = new HashSet(Collections.singleton(atd1));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Collections.singleton(cld1)));

        String expected =  INDENT + "public boolean equals(Object o) { return (o instanceof Class1 && id != null) ? id.equals(((Class1)o).getId()) : false; }" + ENDL;

        assertEquals(expected, mo.generateEquals(cld1));
    }

    public void testGenerateHashCode() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL;

        assertEquals(expected, mo.generateHashCode(cld1));
    }

    public void testGenerateToString() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "int");
        AttributeDescriptor atd2 = new AttributeDescriptor("atd2", "int");
        Set atts = new LinkedHashSet(Arrays.asList(new Object[] {atd1, atd2}));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", uri, new LinkedHashSet(Collections.singleton(cld1)));

        String expected = INDENT + "public String toString() { return \"Class1 [Atd1=\\\"\" + atd1 + \"\\\", Atd2=\\\"\" + atd2 + \"\\\", Id=\\\"\" + id + \"\\\"]\"; }" + ENDL;

        assertEquals(expected, mo.generateToString(cld1));
    }

    public void testGetType() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        assertEquals("java.lang.String", mo.getType(atd1));

        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "Class2", null);
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "Class2", null);
        Set refs = new HashSet(Collections.singleton(rfd1));
        Set cols = new HashSet(Collections.singleton(cod1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        assertEquals("Class2", mo.getType(rfd1));
        assertEquals("java.util.Set", mo.getType(cod1));
    }

    public void testGenerateMultiInheritanceLegal() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "int");
        Set atds1 = new HashSet(Collections.singleton(atd1));
        AttributeDescriptor atd2 = new AttributeDescriptor("atd1", "int");
        Set atds2 = new HashSet(Collections.singleton(atd2));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, true, atds1, new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, true, atds2, new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("package.name.Class3", "package.name.Class1 package.name.Class2", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        String expected = "package package.name;" + ENDL + ENDL
            + "public class Class3 implements package.name.Class1, package.name.Class2" + ENDL + "{" + ENDL
            + INDENT + "// Attr: package.name.Class1.atd1" + ENDL
            + INDENT + "protected int atd1;" + ENDL
            + INDENT + "public int getAtd1() { return atd1; }" + ENDL
            + INDENT + "public void setAtd1(int atd1) { this.atd1 = atd1; }" + ENDL + ENDL
            + INDENT + "// Attr: org.intermine.model.InterMineObject.id" + ENDL
            + INDENT + "protected java.lang.Integer id;" + ENDL
            + INDENT + "public java.lang.Integer getId() { return id; }" + ENDL
            + INDENT + "public void setId(java.lang.Integer id) { this.id = id; }" + ENDL + ENDL
            + INDENT + "public boolean equals(Object o) { return (o instanceof Class3 && id != null) ? id.equals(((Class3)o).getId()) : false; }" + ENDL
            + INDENT + "public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }" + ENDL
            + INDENT + "public String toString() { return \"Class3 [Atd1=\\\"\" + atd1 + \"\\\", Id=\\\"\" + id + \"\\\"]\"; }" + ENDL
            + "}" + ENDL;

        assertEquals(expected, mo.generate(cld3));
    }

}

