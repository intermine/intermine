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

import java.io.File;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Collections;
import java.io.FileReader;
import java.io.FileNotFoundException;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.CollectionDescriptor;

public class OJBModelOutputTest extends TestCase
{
    private String INDENT = ModelOutput.INDENT;
    private String ENDL = ModelOutput.ENDL;
    private Model model;
    private File file;
    private OJBModelOutput mo;

    public OJBModelOutputTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        model = new Model("model", new HashSet());
        file = new File("temp.xml");
        mo = new OJBModelOutput(model, file);
    }

    public void testProcess() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2})));

        File path = new File("./");
        OJBModelOutput mo = new OJBModelOutput(model, path);
        mo.process();

        File file = new File("./repository_model.xml");
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

            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ENDL
            + "<!DOCTYPE descriptor-repository SYSTEM \"repository.dtd\" [" + ENDL
            + "<!ENTITY internal SYSTEM \"repository_internal.xml\">" + ENDL
            + "]>" + ENDL + ENDL + "<descriptor-repository version=\"1.0\" isolation-level=\"read-uncommitted\">" + ENDL
            + INDENT + "<class-descriptor class=\"Class1\" table=\"Class1\">" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>"
            + ENDL + INDENT + "</class-descriptor>" + ENDL + ENDL
            + INDENT + "<class-descriptor class=\"Class2\" table=\"Class2\">" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>"
            + ENDL + INDENT + "</class-descriptor>" + ENDL + ENDL
            + "&internal;" + ENDL + "</descriptor-repository>" + ENDL;

            // trim text then add a single ENDL
            assertEquals(expected, (new String(text)).trim() + ENDL);
        } finally {
            file.delete();
        }
    }

    public void testGenerateModel() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ENDL
            + "<!DOCTYPE descriptor-repository SYSTEM \"repository.dtd\" [" + ENDL
            + "<!ENTITY internal SYSTEM \"repository_internal.xml\">" + ENDL
            + "]>" + ENDL + ENDL + "<descriptor-repository version=\"1.0\" isolation-level=\"read-uncommitted\">" + ENDL
            + INDENT + "<class-descriptor class=\"Class1\" table=\"Class1\">" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>"
            + ENDL + INDENT + "</class-descriptor>" + ENDL + ENDL
            + INDENT + "<class-descriptor class=\"Class2\" table=\"Class2\">" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>"
            + ENDL + INDENT + "</class-descriptor>" + ENDL + ENDL
            + "&internal;" + ENDL + "</descriptor-repository>" + ENDL;

        assertEquals(expected, mo.generate(model));
    }

    public void testGenerateClassDescriptorClass() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "<class-descriptor class=\"Class1\" table=\"Class1\">" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>"
            + ENDL + INDENT + "</class-descriptor>" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorHasSuper() throws Exception {
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld1 = new ClassDescriptor("Class1", "Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "<class-descriptor class=\"Class1\" extends=\"Class2\" table=\"Class2\">" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"ojbConcreteClass\" column=\"CLASS\" jdbc-type=\"VARCHAR\"/>" + ENDL
            + INDENT + "</class-descriptor>" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorHasSubclasses() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Class3", "Class1", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model = new Model("model", new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        String expected = INDENT + "<class-descriptor class=\"Class1\" table=\"Class1\">" + ENDL
            + INDENT + INDENT + "<extent-class class-ref=\"Class2\"/>" + ENDL
            + INDENT + INDENT + "<extent-class class-ref=\"Class3\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"ojbConcreteClass\" column=\"CLASS\" jdbc-type=\"VARCHAR\"/>" + ENDL
            + INDENT + "</class-descriptor>" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

   public void testGenerateClassDescriptorIsInterface() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + "<class-descriptor class=\"Interface1\">"
            + ENDL + INDENT + "</class-descriptor>" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorImplementsInterfaces() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Interface2", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Class1", null, "Interface1 Interface2", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));


        String expected = INDENT + "<class-descriptor class=\"Class1\" table=\"Class1\">" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>"
            + ENDL + INDENT + "</class-descriptor>" + ENDL + ENDL;;

        assertEquals(expected, mo.generate(cld3));
    }

    public void testGenerateClassDescriptorIsImplemented() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class1", null, "Interface1", false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "<class-descriptor class=\"Interface1\">" + ENDL
            + INDENT + INDENT + "<extent-class class-ref=\"Class1\"/>" + ENDL
            + INDENT + "</class-descriptor>" + ENDL + ENDL;;

        assertEquals(expected, mo.generate(cld1));
    }

    public void testGenerateClassDescriptorFields() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("name1", true, "java.lang.String");
        Set atts = new LinkedHashSet(Collections.singleton(atd1));

        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        Set refs = new LinkedHashSet(Collections.singleton(rfd1));

        // only need simple unidirectionals cods
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, true);
        Set cols = new LinkedHashSet(Collections.singleton(cod1));

        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, atts, refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + "<class-descriptor class=\"Class1\" table=\"Class1\">" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"name1\" column=\"name1\" jdbc-type=\"LONGVARCHAR\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"rfd1Id\" column=\"rfd1Id\" access=\"anonymous\" jdbc-type=\"INTEGER\"/>" + ENDL
            + INDENT + INDENT + "<reference-descriptor name=\"rfd1\" class-ref=\"Class2\" proxy=\"true\">" + ENDL
            + INDENT + INDENT + INDENT + "<foreignkey field-ref=\"rfd1Id\"/>" + ENDL
            + INDENT + INDENT + "</reference-descriptor>" + ENDL
            + INDENT + INDENT + "<collection-descriptor name=\"cod1\" element-class-ref=\"Class2\""
            + " collection-class=\"java.util.ArrayList\" proxy=\"true\" indirection-table=\"Class1Cod1\">" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-this-class column=\"Class1Id\"/>" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-element-class column=\"Class2Id\"/>" + ENDL
            + INDENT + INDENT + "</collection-descriptor>" + ENDL
            + INDENT + "</class-descriptor>" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cld1));
    }

    // a ref and attribute descriptor in the superclass
    public void testGenerateClassDescriptorSuperFields() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("name1", true, "java.lang.String");
        Set atts1 = new LinkedHashSet(Collections.singleton(atd1));
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        Set refs1 = new LinkedHashSet(Collections.singleton(rfd1));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, atts1, refs1, new HashSet());

        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        mo.collections = new StringBuffer();
        mo.references = new StringBuffer();

        String expected = INDENT + "<class-descriptor class=\"Class2\" extends=\"Class1\" table=\"Class1\">" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"id\" column=\"ID\" jdbc-type=\"INTEGER\" primarykey=\"true\" autoincrement=\"true\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"ojbConcreteClass\" column=\"CLASS\" jdbc-type=\"VARCHAR\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"name1\" column=\"name1\" jdbc-type=\"LONGVARCHAR\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"rfd1Id\" column=\"rfd1Id\" access=\"anonymous\" jdbc-type=\"INTEGER\"/>" + ENDL
            + INDENT + INDENT + "<reference-descriptor name=\"rfd1\" class-ref=\"Class2\" proxy=\"true\">" + ENDL
            + INDENT + INDENT + INDENT + "<foreignkey field-ref=\"rfd1Id\"/>" + ENDL
            + INDENT + INDENT + "</reference-descriptor>" + ENDL
            + INDENT + "</class-descriptor>" + ENDL + ENDL;

        assertEquals(expected, mo.generate(cld2));
    }

    public void testDoAttributes() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("name1", true, "java.lang.String");
        AttributeDescriptor atd2 = new AttributeDescriptor("name2", true, "java.lang.String");
        Set atts = new LinkedHashSet(Arrays.asList(new Object[] {atd1, atd2}));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, atts, new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Collections.singleton(cld1)));

        String expected = INDENT + INDENT + "<field-descriptor name=\"name1\" column=\"name1\" jdbc-type=\"LONGVARCHAR\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"name2\" column=\"name2\" jdbc-type=\"LONGVARCHAR\"/>" + ENDL;

        StringBuffer sb = new StringBuffer();
        mo.doAttributes(cld1, sb);
        assertEquals(expected, sb.toString());
    }

    public void testDoAssociations() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", false, "Class2", null);
        Set refs = new LinkedHashSet(Arrays.asList(new Object[] {rfd1, rfd2}));

        // only need simple unidirectional cods
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, true);
        CollectionDescriptor cod2 = new CollectionDescriptor("cod2", false, "Class2", null, true);
        Set cols = new LinkedHashSet(Arrays.asList(new Object[] {cod1, cod2}));

        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        String expected = INDENT + INDENT + "<field-descriptor name=\"rfd1Id\" column=\"rfd1Id\" access=\"anonymous\" jdbc-type=\"INTEGER\"/>" + ENDL
            + INDENT + INDENT + "<field-descriptor name=\"rfd2Id\" column=\"rfd2Id\" access=\"anonymous\" jdbc-type=\"INTEGER\"/>" + ENDL;
        String references = INDENT + INDENT + "<reference-descriptor name=\"rfd1\" class-ref=\"Class2\" proxy=\"true\">" + ENDL
            + INDENT + INDENT + INDENT + "<foreignkey field-ref=\"rfd1Id\"/>" + ENDL
            + INDENT + INDENT + "</reference-descriptor>" + ENDL
            + INDENT + INDENT + "<reference-descriptor name=\"rfd2\" class-ref=\"Class2\" proxy=\"true\">" + ENDL
            + INDENT + INDENT + INDENT + "<foreignkey field-ref=\"rfd2Id\"/>" + ENDL
            + INDENT + INDENT + "</reference-descriptor>" + ENDL;
        String collections = INDENT + INDENT + "<collection-descriptor name=\"cod1\" element-class-ref=\"Class2\""
            + " collection-class=\"java.util.ArrayList\" proxy=\"true\" indirection-table=\"Class1Cod1\">" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-this-class column=\"Class1Id\"/>" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-element-class column=\"Class2Id\"/>" + ENDL
            + INDENT + INDENT + "</collection-descriptor>" + ENDL
            + INDENT + INDENT + "<collection-descriptor name=\"cod2\" element-class-ref=\"Class2\""
            + " collection-class=\"java.util.ArrayList\" proxy=\"true\" indirection-table=\"Class1Cod2\">" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-this-class column=\"Class1Id\"/>" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-element-class column=\"Class2Id\"/>" + ENDL
            + INDENT + INDENT + "</collection-descriptor>" + ENDL;

        mo.collections = new StringBuffer();
        mo.references = new StringBuffer();

        StringBuffer sb = new StringBuffer();
        mo.doAssociations(cld1, sb);
        assertEquals(expected, sb.toString());
        assertEquals(references, mo.references.toString());
        assertEquals(collections, mo.collections.toString());
    }

    public void testGenerateCollectionDescriptorUnidirctional() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, true);
        Set cols1 = Collections.singleton(cod1);
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), cols1);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        mo.collections = new StringBuffer();
        mo.generate(cod1);

        String collections = INDENT + INDENT + "<collection-descriptor name=\"cod1\" element-class-ref=\"Class2\""
            + " collection-class=\"java.util.ArrayList\" proxy=\"true\" indirection-table=\"Class1Cod1\">" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-this-class column=\"Class1Id\"/>" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-element-class column=\"Class2Id\"/>" + ENDL
            + INDENT + INDENT + "</collection-descriptor>" + ENDL;
        assertEquals(collections, mo.collections.toString());
    }

    public void testGenerateCollectionDescriptorNtoM() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", "cod2", true);
        CollectionDescriptor cod2 = new CollectionDescriptor("cod2", false, "Class1", "cod1", true);
        Set cols1 = Collections.singleton(cod1);
        Set cols2 = Collections.singleton(cod2);
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), cols1);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), cols2);
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        mo.collections = new StringBuffer();
        mo.generate(cod1);

        String collections = INDENT + INDENT + "<collection-descriptor name=\"cod1\" element-class-ref=\"Class2\""
            + " collection-class=\"java.util.ArrayList\" proxy=\"true\" indirection-table=\"Cod1Cod2\">" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-this-class column=\"Class1Id\"/>" + ENDL
            + INDENT + INDENT + INDENT + "<fk-pointing-to-element-class column=\"Class2Id\"/>" + ENDL
            + INDENT + INDENT + "</collection-descriptor>" + ENDL;

        assertEquals(collections, mo.collections.toString());
    }

    public void testGenerateCollectionDescriptor1toN() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", "rfd1", true);
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class1", null);
        Set cols1 = Collections.singleton(cod1);
        Set refs1 = Collections.singleton(rfd1);
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), cols1);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), refs1, new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        mo.collections = new StringBuffer();
        mo.generate(cod1);

        String collections = INDENT + INDENT + "<collection-descriptor name=\"cod1\" element-class-ref=\"Class2\""
            + " collection-class=\"java.util.ArrayList\" proxy=\"true\">" + ENDL
            + INDENT + INDENT + INDENT + "<inverse-foreignkey field-ref=\"rfd1Id\"/>" + ENDL
            + INDENT + INDENT + "</collection-descriptor>" + ENDL;
        assertEquals(collections, mo.collections.toString());
    }

    public void testGenerateReferenceDescriptor() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        Set refs1 = Collections.singleton(rfd1);
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), refs1, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));


        mo.references = new StringBuffer();
        String expected = INDENT + INDENT + "<field-descriptor name=\"rfd1Id\" column=\"rfd1Id\" access=\"anonymous\" jdbc-type=\"INTEGER\"/>" + ENDL;
        assertEquals(expected, mo.generate(rfd1));

        String references = INDENT + INDENT + "<reference-descriptor name=\"rfd1\" class-ref=\"Class2\" proxy=\"true\">"
            + ENDL + INDENT + INDENT + INDENT + "<foreignkey field-ref=\"rfd1Id\"/>" + ENDL
            + INDENT + INDENT + "</reference-descriptor>" + ENDL;
        assertEquals(references, mo.references.toString());
    }

    public void testGenerateAttributeDescriptor() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("name", true, "java.lang.String");
        String expected = INDENT + INDENT + "<field-descriptor name=\"name\" column=\"name\" jdbc-type=\"LONGVARCHAR\"/>" + ENDL;
        assertEquals(expected, mo.generate(atd1));
    }

    public void testGetParents() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Class3", "Class2", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model = new Model("test", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        List parents = Arrays.asList(new Object[] {cld1, cld2});
        assertEquals(parents, mo.getParents(cld3));
    }

    public void testGenerateSqlCompatibleName() throws Exception {
        assertEquals("finish", mo.generateSqlCompatibleName("end"));
        assertEquals("identifier", mo.generateSqlCompatibleName("id"));
        assertEquals("indx", mo.generateSqlCompatibleName("index"));
        assertEquals("ordr", mo.generateSqlCompatibleName("order"));
        assertEquals("complete", mo.generateSqlCompatibleName("full"));
        assertEquals("offst", mo.generateSqlCompatibleName("offset"));
        assertEquals("some_string", mo.generateSqlCompatibleName("some_string"));

        try {
            mo.generateOJBSqlType(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testGenerateOJBSqlType() throws Exception {
        assertEquals("INTEGER", mo.generateOJBSqlType("int"));
        assertEquals("INTEGER", mo.generateOJBSqlType("java.lang.Integer"));
        assertEquals("LONGVARCHAR", mo.generateOJBSqlType("java.lang.String"));
        assertEquals("INTEGER\" conversion=\"org.apache.ojb.broker.accesslayer.conversions.Boolean2IntFieldConversion", mo.generateOJBSqlType("boolean"));
        assertEquals("FLOAT", mo.generateOJBSqlType("java.lang.Float"));
        assertEquals("FLOAT", mo.generateOJBSqlType("float"));
        assertEquals("DATE\" conversion=\"org.apache.ojb.broker.accesslayer.conversions.JavaDate2SqlDateFieldConversion",
                     mo.generateOJBSqlType("java.util.Date"));
        assertEquals("some_type", mo.generateOJBSqlType("some_type"));
        try {
            mo.generateOJBSqlType(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }
}
