package org.intermine.metadata;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;

public class ClassDescriptorTest extends TestCase
{
    private static final String ENDL = System.getProperty("line.separator");
    private static final Set<AttributeDescriptor> noAttrs =
            Collections.unmodifiableSet(new HashSet<AttributeDescriptor>());
    private static final Set<ReferenceDescriptor> noRefs =
            Collections.unmodifiableSet(new HashSet<ReferenceDescriptor>());
    private static final Set<CollectionDescriptor> noColls =
            Collections.unmodifiableSet(new HashSet<CollectionDescriptor>());

    public ClassDescriptorTest(String arg) {
        super(arg);
    }

    public void testConstructNameNull() {
        try {
            new ClassDescriptor("", null, false, noAttrs, noRefs, noColls);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    // --- Private factory methods for making classes and interfaces.
    private ClassDescriptor makeClass(String name) {
        return makeClassDescriptor(name, null, false);
    }
    private ClassDescriptor makeClass(String name, String supers) {
        return makeClassDescriptor(name, supers, false);
    }
    private ClassDescriptor makeInterface(String name) {
        return makeClassDescriptor(name, null, true);
    }
    private ClassDescriptor makeInterface(String name, String supers) {
        return makeClassDescriptor(name, supers, true);
    }
    private ClassDescriptor makeClassDescriptor(String name, String supers, boolean isInterface) {
        return new ClassDescriptor(name, supers, isInterface, noAttrs, noRefs, noColls);
    }

    public void testSetModel() throws Exception {
        ClassDescriptor cld = makeClass("package.name.Class1");
        try {
            new Model("model1", "package.name", Collections.singleton(cld));
        } catch (IllegalStateException e) {
            fail("Model should have been set correctly");
        }

        try {
            new Model("model2", "package.name", Collections.singleton(cld));
            fail("Model already set, expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }

    public void testInterfaceDescriptors() throws Exception {
        ClassDescriptor int1 = makeInterface("package.name.Interface1");
        ClassDescriptor int2 = makeInterface("package.name.Interface2");
        ClassDescriptor cld =
                makeClassDescriptor("package.name.Class1", "package.name.Interface1 package.name.Interface2", false);

        new Model("test", "package.name", Arrays.asList(int1, int2, cld));

        Set<ClassDescriptor> interfaces = new HashSet<ClassDescriptor>(Arrays.asList(int1, int2));
        assertEquals(interfaces, cld.getSuperDescriptors());

    }

    public void testImplementsNotExists() throws MetaDataException {
        // construct class implementing Interface3 which does not exist
        ClassDescriptor int1 = makeInterface("package.name.Interface1");
        ClassDescriptor int2 = makeInterface("package.name.Interface2");
        ClassDescriptor cld = makeClassDescriptor(
                "package.name.Class1",
                "package.name.Interface1 package.name.Interface2 package.name.Interface3",
                false);

        try {
            new Model("test", "package.name", Arrays.asList(int1, int2, cld));
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testInterfaceNotImplemented() throws MetaDataException {
        ClassDescriptor cld1 = makeInterface("package.name.Interface1");
        Model model = new Model("model", "package.name", Collections.singleton(cld1));
        assertTrue(model.getClassDescriptorByName(cld1.getName()).getSubDescriptors().size() == 0);
    }

    public void testSuperClassExists() throws MetaDataException  {
        ClassDescriptor superCld = makeClass("package.name.superCld");
        ClassDescriptor cld = makeClass("package.name.cld", "package.name.superCld");
        new Model("test", "package.name", Arrays.asList(cld, superCld));
        assertEquals(superCld, cld.getSuperclassDescriptor());
    }

    public void testSuperClassNotExists() throws MetaDataException  {
        ClassDescriptor superCld = makeClass("package.name.superCld");
        ClassDescriptor cld = makeClass("package.name.cld", "package.name.anotherCld");
        try {
            new Model("test", "package.name", Arrays.asList(cld, superCld));
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testSuperClassWrongType() throws Exception {
        // test where superclass is not an interface, subclass is
        ClassDescriptor superCld = makeClass("package.name.superCld");
        ClassDescriptor interface1 = makeInterface("package.name.interface1", "package.name.superCld");
        try {
            new Model("test", "package.name", Arrays.asList(interface1, superCld));
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testMultipleSuperClass() throws Exception {
        ClassDescriptor superCld1 = makeClass("package.name.superCld1");
        ClassDescriptor superCld2 = makeClass("package.name.superCld2");
        ClassDescriptor cld = makeClass("package.name.cld", "package.name.superCld1 package.name.superCld2");
        try {
            new Model("test", "package.name", Arrays.asList(cld, superCld1, superCld2));
            fail("Expected: MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testFieldDescriptorByName() throws Exception {
        ClassDescriptor cld = new ClassDescriptor(
                "package.name.Class1", null, false,
                getAttributes(), getReferences(), getCollections());
        cld.setAllFieldDescriptors();
        assertNotNull(cld.getFieldDescriptorByName("atd1"));
        assertNotNull(cld.getFieldDescriptorByName("atd2"));
        assertNotNull(cld.getFieldDescriptorByName("rfd1"));
        assertNotNull(cld.getFieldDescriptorByName("rfd2"));
        assertNotNull(cld.getFieldDescriptorByName("cld1"));
        assertNotNull(cld.getFieldDescriptorByName("cld2"));
    }

    public void testGetAllAttributeDescriptors() throws Exception {
        // three superclass levels with one attribute each, getAllAttributeDescriptors on cld3 should return all 3
        AttributeDescriptor atb1 = new AttributeDescriptor("att1", "java.lang.String");
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, Collections.singleton(atb1), noRefs, noColls);
        AttributeDescriptor atb2 = new AttributeDescriptor("att2", "java.lang.String");
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", "package.name.Class1", false, Collections.singleton(atb2), noRefs, noColls);
        AttributeDescriptor atb3 = new AttributeDescriptor("att3", "java.lang.String");
        ClassDescriptor cld3 = new ClassDescriptor("package.name.Class3", "package.name.Class2", false, Collections.singleton(atb3), noRefs, noColls);

        new Model("test", "package.name", Arrays.asList(cld1, cld2, cld3));

        FieldDescriptor id = cld3.getFieldDescriptorByName("id");
        Set<FieldDescriptor> atts = new HashSet<FieldDescriptor>(Arrays.asList(atb3, atb2, atb1, id));

        assertEquals(atts, cld3.getAllAttributeDescriptors());
    }

    public void testGetSubDescriptors() throws Exception {
        ClassDescriptor cld1 = makeClass("package.name.Class1");
        ClassDescriptor cld2 = makeClass("package.name.Class2", "package.name.Class1");
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class1");
        ClassDescriptor cld4 = makeClass("package.name.Class4", "package.name.Class2");

        new Model("test2", "package.name", Arrays.asList(cld1, cld2, cld3, cld4));

        // getSubDescriptrors should just return direct subclasses (cld2, cld3)
        Set<ClassDescriptor> subs = new HashSet<ClassDescriptor>(Arrays.asList(cld2, cld3));

        assertEquals(subs, cld1.getSubDescriptors());
    }

    public void testGetAllSuperDescriptors() throws Exception {
        ClassDescriptor cld1 = makeClass("package.name.Class1");
        ClassDescriptor cld2 = makeClass("package.name.Class2", "package.name.Class1");
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class1");
        ClassDescriptor cld4 = makeClass("package.name.Class4", "package.name.Class2");

        new Model("test2", "package.name", Arrays.asList(cld1, cld2, cld3, cld4));

        // getAllSuperDescriptors of Class4 should be Class2 and Class1
        Set<ClassDescriptor> supers = new HashSet<ClassDescriptor>(Arrays.asList(cld2, cld1));
        assertEquals(supers, cld4.getAllSuperDescriptors());

        supers = new HashSet<ClassDescriptor>();
        assertEquals(supers, cld1.getAllSuperDescriptors());

        supers = Collections.singleton(cld1);
        assertEquals(supers, cld2.getAllSuperDescriptors());
    }

    public void testGetImplementorDescriptors() throws Exception {
        ClassDescriptor cld1 = makeInterface("package.name.Interface1");
        ClassDescriptor cld2 = makeClass("package.name.Class2", "package.name.Interface1");
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Interface1");
        ClassDescriptor cld4 = makeClass("package.name.Class4", "package.name.Class2");

        new Model("test2", "package.name", Arrays.asList(cld1, cld2, cld3, cld4));

        // getImplementorDescriptrors should just return direct implementations (cld2, cld3)
        Set<ClassDescriptor> impls = new HashSet<ClassDescriptor>(Arrays.asList(cld2, cld3));

        assertEquals(impls, cld1.getSubDescriptors());
    }

    public void testEquals() throws Exception {
        ClassDescriptor col1 = makeClass("class1");
        ClassDescriptor col2 = makeClass("class1");
        ClassDescriptor col3 = makeClass("class1", "Super");
        ClassDescriptor col4 = makeClass("class1", "Super");
        ClassDescriptor col5 = makeClass("class1", "Interface");
        ClassDescriptor col6 = makeClass("class1");
        ClassDescriptor col7 = new ClassDescriptor("class1", null, true,
                Collections.singleton(new AttributeDescriptor("field", "int")), noRefs, noColls);

        assertEquals(col1, col2);
        assertEquals(col1.hashCode(), col2.hashCode());
        assertFalse(col1.equals(col3));
        assertEquals(col3, col4);
        assertEquals(col3.hashCode(), col4.hashCode());
        assertFalse(col1.equals(col5));
        assertTrue(col1.equals(col6));
        assertFalse(col1.equals(col7));
    }

    public void testToString() throws Exception {
        ClassDescriptor cld1 = makeInterface("package.name.Interface1");
        ClassDescriptor cld2 = makeClass("package.name.Class2");
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class2 package.name.Interface1");
        String expected =
                "<class name=\"Class3\" extends=\"Class2 Interface1\" is-interface=\"false\"></class>" + ENDL;
        new Model("test", "package.name", Arrays.asList(cld1, cld2, cld3));
        assertEquals(expected, cld3.toString());
    }

    public void testToString2() throws Exception {
        ClassDescriptor cld1 = makeInterface("package.name.Interface1");
        ClassDescriptor cld2 = makeClass("package.name.Class2", "package.name.Interface1");
        String expected =
                "<class name=\"Class2\" extends=\"Interface1\" is-interface=\"false\"></class>" + ENDL;
        new Model("test", "package.name", Arrays.asList(cld1, cld2));
        assertEquals(expected, cld2.toString());
    }
    // ============================================

    private Set<AttributeDescriptor> getAttributes() {
        Set<AttributeDescriptor> attributes = new HashSet<AttributeDescriptor>();
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        AttributeDescriptor atd2 = new AttributeDescriptor("atd2", "java.lang.Integer");
        attributes.add(atd1);
        attributes.add(atd2);
        return attributes;
    }

    private Set<ReferenceDescriptor> getReferences() {
        Set<ReferenceDescriptor> references = new HashSet<ReferenceDescriptor>();
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "String", "reverse1");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", "Integer", "reverse2");
        references.add(rfd1);
        references.add(rfd2);
        return references;
    }

    private Set<CollectionDescriptor> getCollections() {
        Set<CollectionDescriptor> collections = new HashSet<CollectionDescriptor>();
        CollectionDescriptor cld1 = new CollectionDescriptor("cld1", "String", "reverse1");
        CollectionDescriptor cld2 = new CollectionDescriptor("cld2", "Integer", "reverse2");
        collections.add(cld1);
        collections.add(cld2);
        return collections;
    }

    public void testMultiInheritanceLegal() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "int");
        AttributeDescriptor atd2 = new AttributeDescriptor("atd1", "int");
        Set<AttributeDescriptor> atds1 = Collections.singleton(atd1);
        Set<AttributeDescriptor> atds2 = Collections.singleton(atd2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, true, atds1, noRefs, noColls);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, true, atds2, noRefs, noColls);
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class1 package.name.Class2");
        new Model("model", "package.name", Arrays.asList(cld1, cld2, cld3));
    }

    public void testMultiInheritanceLegalRef() throws Exception {
        ReferenceDescriptor ref1 = new ReferenceDescriptor("atd1", "package.name.Class2", null);
        ReferenceDescriptor ref2 = new ReferenceDescriptor("atd1", "package.name.Class2", null);
        Set<ReferenceDescriptor> refs1 = Collections.singleton(ref1);
        Set<ReferenceDescriptor> refs2 = Collections.singleton(ref2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, true, noAttrs, refs1, noColls);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, true, noAttrs, refs2, noColls);
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class1 package.name.Class2");
        new Model("model", "package.name", Arrays.asList(cld1, cld2, cld3));
        ReferenceDescriptor rd = cld3.getReferenceDescriptorByName("atd1", true);
        assertEquals("package.name.Class2", rd.getReferencedClassName());
    }

    public void testMultiInheritanceLegalCol() throws Exception {
        CollectionDescriptor coll1 = new CollectionDescriptor("atd1", "package.name.Class2", null);
        CollectionDescriptor coll2 = new CollectionDescriptor("atd1", "package.name.Class2", null);
        Set<CollectionDescriptor> colls1 = Collections.singleton(coll1);
        Set<CollectionDescriptor> colls2 = Collections.singleton(coll2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, true, noAttrs, noRefs, colls1);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, true, noAttrs, noRefs, colls2);
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class1 package.name.Class2");
        new Model("model", "package.name", Arrays.asList(cld1, cld2, cld3));
        ReferenceDescriptor rd = cld3.getCollectionDescriptorByName("atd1", true);
        assertEquals("package.name.Class2", rd.getReferencedClassName());
    }

    public void testMultiInheritanceIllegalDueToAttributeTypeMismatch() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("collision", "int");
        AttributeDescriptor atd2 = new AttributeDescriptor("collision", "float");
        Set<AttributeDescriptor> atds1 = Collections.singleton(atd1);
        Set<AttributeDescriptor> atds2 = Collections.singleton(atd2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, true, atds1, noRefs, noColls);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, true, atds2, noRefs, noColls);
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class1 package.name.Class2");
        try {
            new Model("model", "package.name", Arrays.asList(cld1, cld2, cld3));
            fail("Expected: MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testMultiInheritanceIllegalAttRef() throws Exception {
        AttributeDescriptor attr = new AttributeDescriptor("collision", "float");
        ReferenceDescriptor ref = new ReferenceDescriptor("collision", "package.name.Class2", null);
        Set<AttributeDescriptor> attrs = Collections.singleton(attr);
        Set<ReferenceDescriptor> refs = Collections.singleton(ref);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, true, noAttrs, refs, noColls);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, true, attrs, noRefs, noColls);
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class1 package.name.Class2");
        try {
            new Model("model", "package.name", Arrays.asList(cld1, cld2, cld3));
            fail("Expected: MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testMultiInheritanceIllegalAttCol() throws Exception {
        CollectionDescriptor coll = new CollectionDescriptor("collision", "package.name.Class2", null);
        AttributeDescriptor attr = new AttributeDescriptor("collision", "float");
        Set<CollectionDescriptor> colls = Collections.singleton(coll);
        Set<AttributeDescriptor> attrs = Collections.singleton(attr);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, true, noAttrs, noRefs, colls);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, true, attrs, noRefs, noColls);
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class1 package.name.Class2");
        try {
            new Model("model", "package.name", Arrays.asList(cld1, cld2, cld3));
            fail("Expected: MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testMultiInheritanceIllegalRefCol() throws Exception {
        CollectionDescriptor coll = new CollectionDescriptor("atd1", "package.name.Class2", null);
        ReferenceDescriptor ref = new ReferenceDescriptor("atd1", "package.name.Class2", null);
        Set<CollectionDescriptor> colls = Collections.singleton(coll);
        Set<ReferenceDescriptor> refs = Collections.singleton(ref);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, true, noAttrs, noRefs, colls);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, true, noAttrs, refs, noColls);
        ClassDescriptor cld3 = makeClass("package.name.Class3", "package.name.Class1 package.name.Class2");
        try {
            new Model("model", "package.name", Arrays.asList(cld1, cld2, cld3));
            fail("Expected: MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testFindSuperClassNames() throws Exception {
        String class1Name = "org.intermine.model.testmodel.Class1";
        String class3Name = "org.intermine.model.testmodel.Class3";
        String class2Name = "org.intermine.model.testmodel.Class2";
        String class4Name = "org.intermine.model.testmodel.Class4";
        ClassDescriptor c1 = makeInterface(class1Name, class3Name);
        ClassDescriptor c2 = makeInterface(class2Name, class3Name);
        ClassDescriptor c3 = makeInterface(class3Name, class4Name);
        ClassDescriptor c4 = makeInterface(class4Name);
        Model model = new Model("model", "org.intermine.model.testmodel", Arrays.asList(c1, c2, c3, c4));

        Set<String> supers = new LinkedHashSet<String>();
        ClassDescriptor.findSuperClassNames(model, class1Name, supers);
        assertEquals(2, supers.size());
        Set<String> expected = new LinkedHashSet<String>();
        expected.add(class3Name);
        expected.add(class4Name);
        assertEquals(expected, supers);

        supers = new LinkedHashSet<String>();
        ClassDescriptor.findSuperClassNames(model, class2Name, supers);
        assertEquals(2, supers.size());

        supers = new LinkedHashSet<String>();
        ClassDescriptor.findSuperClassNames(model, class3Name, supers);
        assertEquals(1, supers.size());

        supers = new LinkedHashSet<String>();
        ClassDescriptor.findSuperClassNames(model, class4Name, supers);
        assertEquals(0, supers.size());
    }

    public void testClassInheritanceCompare() throws Exception {
        String class1Name = "org.intermine.model.testmodel.Class1";
        String class2Name = "org.intermine.model.testmodel.Class2";
        String class3Name = "org.intermine.model.testmodel.Class3";
        String class4Name = "org.intermine.model.testmodel.Class4";
        ClassDescriptor c1 = makeInterface(class1Name, class2Name + " " + class3Name);
        ClassDescriptor c2 = makeInterface(class2Name, class4Name);
        ClassDescriptor c3 = makeInterface(class3Name, class4Name);
        ClassDescriptor c4 = makeInterface(class4Name);
        Model model = new Model("model", "org.intermine.model.testmodel", Arrays.asList(c1, c2, c3, c4));

        int comp = ClassDescriptor.classInheritanceCompare(model, class1Name, class2Name);
        assertEquals(1, comp);

        comp = ClassDescriptor.classInheritanceCompare(model, class2Name, class1Name);
        assertEquals(-1, comp);

        comp = ClassDescriptor.classInheritanceCompare(model, class1Name, class3Name);
        assertEquals(1, comp);

        comp = ClassDescriptor.classInheritanceCompare(model, class1Name, class4Name);
        assertEquals(1, comp);

        comp = ClassDescriptor.classInheritanceCompare(model, class1Name, class2Name);
        assertEquals(1, comp);

        comp = ClassDescriptor.classInheritanceCompare(model, class2Name, class3Name);
        assertEquals(0, comp);
    }

    public void testClassInheritanceCompare2() throws Exception {
        String class1Name = "org.intermine.model.testmodel.Class1";
        String class2Name = "org.intermine.model.testmodel.Class2";
        String class3Name = "org.intermine.model.testmodel.Class3";
        ClassDescriptor c1 = makeInterface(class1Name, class2Name + " " + class3Name);
        ClassDescriptor c2 = makeInterface(class2Name, class3Name);
        ClassDescriptor c3 = makeInterface(class3Name);
        Model model = new Model("model", "org.intermine.model.testmodel", Arrays.asList(c1, c2, c3));

        int comp = ClassDescriptor.classInheritanceCompare(model, class1Name, class2Name);
        assertEquals(1, comp);

        comp = ClassDescriptor.classInheritanceCompare(model, class2Name, class1Name);
        assertEquals(-1, comp);

        comp = ClassDescriptor.classInheritanceCompare(model, class1Name, class3Name);
        assertEquals(1, comp);

        comp = ClassDescriptor.classInheritanceCompare(model, class2Name, class3Name);
        assertEquals(1, comp);
    }

    // SimpleObjects should inherit from java.lang.Object, normal classes don't inherit
    public void testSimpleObjectClassDescriptors() throws Exception {
        ClassDescriptor simpleObjectCld = makeClass("package.name.Simple", "java.lang.Object");
        new Model("test", "package.name", Collections.singleton(simpleObjectCld));
        Set<String> expected = Collections.singleton("java.lang.Object");
        assertEquals(expected, simpleObjectCld.getSuperclassNames());
    }

}
