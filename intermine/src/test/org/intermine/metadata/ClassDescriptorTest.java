package org.flymine.metadata;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;


public class ClassDescriptorTest extends TestCase {

    public ClassDescriptorTest(String arg) {
        super(arg);
    }

    public void setUp() {
    }

    public void tearDown() {
    }


    public void testConstructNameNull() {
        try {
            ClassDescriptor cld = new ClassDescriptor("", null, null, false,
                                                      new HashSet(), new HashSet(), new HashSet());
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    public void testSetModel() throws Exception {
        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  new HashSet(), new HashSet(), new HashSet());
        try {
            Model model1 = new Model("model1", Collections.singleton(cld));
        } catch (IllegalStateException e) {
            fail("Model should have been set correctly");
        }

        try {
            Model model2 = new Model("model2", Collections.singleton(cld));
            fail("Model already set, expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }

    public void testInterfaceDescriptors() throws Exception {
        ClassDescriptor int1 = new ClassDescriptor("Interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor int2 = new ClassDescriptor("Interface2", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld = new ClassDescriptor("Class1", null, "Interface1 Interface2", false,
                                                  new HashSet(), new HashSet(), new HashSet());

        Set clds = new HashSet(Arrays.asList(new Object[] {int1, int2, cld}));
        Model model = new Model("test", clds);

        Set interfaces = new HashSet(Arrays.asList(new Object[] {int1, int2}));
        assertEquals(interfaces, cld.getInterfaceDescriptors());

    }

    public void testImplementsNotExists() throws Exception {
        // construct class implementing Interface3 which does not exist
        ClassDescriptor int1 = new ClassDescriptor("Interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor int2 = new ClassDescriptor("Interface2", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld = new ClassDescriptor("Class1", null, "Interface1 Interface3", false,
                                                  new HashSet(), new HashSet(), new HashSet());

        Set clds = new HashSet(Arrays.asList(new Object[] {int1, int2, cld}));
        try {
            Model model = new Model("test", clds);
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testImplementsNotAnInterface() throws Exception {
        // construct a class implementing Class2 which is not an interface
        ClassDescriptor int1 = new ClassDescriptor("Interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor int2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld = new ClassDescriptor("Class1", null, "Interface1 Class2", false,
                                                  new HashSet(), new HashSet(), new HashSet());

        Set clds = new HashSet(Arrays.asList(new Object[] {int1, int2, cld}));
        try {
            Model model = new Model("test", clds);
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testInterfaceNotImplemented() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", Collections.singleton(cld1));
        try {
            assertTrue(cld1.getImplementorDescriptors().size() == 0);
        } catch (IllegalStateException e) {
            fail("IllegalStateException when getting implementor descriptors");
        }
    }

    public void testSuperClassExists() throws Exception {
        ClassDescriptor superCld = new ClassDescriptor("superCld", null, null, false,
                                                       new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld = new ClassDescriptor("cld", "superCld", null, false,  new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("test", new HashSet(Arrays.asList(new Object[] {cld, superCld})));
        assertEquals(superCld, cld.getSuperclassDescriptor());
    }

    public void testSuperClassNotExists() throws Exception {
        ClassDescriptor superCld = new ClassDescriptor("superCld", null, null, false,
                                                       new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld = new ClassDescriptor("cld", "anotherCld", null, false,  new HashSet(), new HashSet(), new HashSet());
        try {
            Model model = new Model("test", new HashSet(Arrays.asList(new Object[] {cld, superCld})));
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }
    }


    public void testSuperClassWrongType() throws Exception {
        // test where superclass is an interface, subclass isn't
        ClassDescriptor int1 = new ClassDescriptor("interface1", null, null, true,
                                                       new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld = new ClassDescriptor("cld", "interface1", null, false,  new HashSet(), new HashSet(), new HashSet());
        try {
            Model model1 = new Model("test", new HashSet(Arrays.asList(new Object[] {cld, int1})));
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }

        // test where superclass is not an interface, subclass is
        ClassDescriptor superCld = new ClassDescriptor("superCld", null, null, false,
                                                       new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor interface1 = new ClassDescriptor("interface1", "superCld", null, true,  new HashSet(), new HashSet(), new HashSet());
        try {
            Model model2 = new Model("test", new HashSet(Arrays.asList(new Object[] {interface1, superCld})));
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }

    }


    public void testAttributeDescriptorByName() throws Exception {
        Set attributes = getAttributes();
        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  attributes, new HashSet(), new HashSet());
        assertNotNull(cld.getAttributeDescriptorByName("atd1"));
        assertNotNull(cld.getAttributeDescriptorByName("atd2"));
    }

    public void testReferenceDescriptorByName() throws Exception {
        Set refs = getReferences();
        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  new HashSet(), refs, new HashSet());
        assertNotNull(cld.getReferenceDescriptorByName("rfd1"));
        assertNotNull(cld.getReferenceDescriptorByName("rfd2"));
    }

    public void testCollectionDescriptorByName() throws Exception {
        Set cols = getCollections();
        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  new HashSet(), new HashSet(), cols);
        assertNotNull(cld.getCollectionDescriptorByName("cld1"));
        assertNotNull(cld.getCollectionDescriptorByName("cld2"));
    }



    public void testUltimateSuperClassOne() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model1 = new Model("test1", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        assertEquals(cld1, cld2.getUltimateSuperclassDescriptor());
    }

    public void testUltimateSuperClassMany() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Class3", "Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld4 = new ClassDescriptor("Class4", "Class3", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model2 = new Model("test2", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3, cld4})));

        assertEquals(cld1, cld4.getUltimateSuperclassDescriptor());
    }

    public void testUltimateSuperClassNone() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());

        Model model1 = new Model("test1", new HashSet(Arrays.asList(new Object[] {cld1})));

        assertTrue("ultimate superclass should have been null", cld1.getUltimateSuperclassDescriptor() == null);
    }


    public void testGetAllAttributeDescriptors() throws Exception {
        // three superclass levels with one attribute each, getAllAttributeDescriptors on cld3 should return all 3
        AttributeDescriptor atb1 = new AttributeDescriptor("att1", false, "String");
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, Collections.singleton(atb1), new HashSet(), new HashSet());
        AttributeDescriptor atb2 = new AttributeDescriptor("att2", false, "String");
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, Collections.singleton(atb2), new HashSet(), new HashSet());
        AttributeDescriptor atb3 = new AttributeDescriptor("att3", false, "String");
        ClassDescriptor cld3 = new ClassDescriptor("Class3", "Class2", null, false, Collections.singleton(atb3), new HashSet(), new HashSet());

        Set atts = new HashSet(Arrays.asList(new Object[] {atb3, atb2, atb1}));
        Model model = new Model("test", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        assertEquals(atts, cld3.getAllAttributeDescriptors());
    }


    public void testGetPkFieldDescriptors() throws Exception {
        AttributeDescriptor atb1 = new AttributeDescriptor("att1", false, "String");
        AttributeDescriptor atb2 = new AttributeDescriptor("att2", true, "String");
        Set atts = new HashSet(Arrays.asList(new Object[] {atb1, atb2}));
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", true, "Class2", null);
        Set refs = new HashSet(Arrays.asList(new Object[] {rfd1, rfd2}));
        CollectionDescriptor cod1 = new CollectionDescriptor("cld1", false, "Class2", null, false);
        CollectionDescriptor cod2 = new CollectionDescriptor("cld2", true, "Class2", null, false);
        Set cols = new HashSet(Arrays.asList(new Object[] {cod1, cod2}));


        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, atts, refs, cols);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("test", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));

        // six fields in class, three of them are pks
        Set pks = new HashSet(Arrays.asList(new Object[] {atb2, rfd2, cod2}));

        assertEquals(pks, cld1.getPkFieldDescriptors());
    }



    public void testGetPkFieldDescriptorsSuper() throws Exception {

        AttributeDescriptor atb1 = new AttributeDescriptor("att1", true, "String");
        AttributeDescriptor atb2 = new AttributeDescriptor("att2", false, "String");
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(Arrays.asList(new Object[] {atb1, atb2})),
                                                   new HashSet(), new HashSet());
        AttributeDescriptor atb3 = new AttributeDescriptor("att3", true, "String");
        AttributeDescriptor atb4 = new AttributeDescriptor("att4", false, "String");
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, new HashSet(Arrays.asList(new Object[] {atb3, atb4})),
                                                   new HashSet(), new HashSet());
        AttributeDescriptor atb5 = new AttributeDescriptor("att5", true, "String");
        AttributeDescriptor atb6 = new AttributeDescriptor("att6", false, "String");
        ClassDescriptor cld3 = new ClassDescriptor("Class3", "Class2", null, false, new HashSet(Arrays.asList(new Object[] {atb5, atb6})),
                                                   new HashSet(), new HashSet());

        Set pks = new HashSet(Arrays.asList(new Object[] {atb5, atb3, atb1}));
        Model model = new Model("test", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3})));

        assertEquals(pks, cld3.getPkFieldDescriptors());
    }

    public void testGetSubclassDescriptors() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Class3", "Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld4 = new ClassDescriptor("Class4", "Class2", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model2 = new Model("test2", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3, cld4})));

        // getSubclassDescriptrors should just return direct subclasses (cld2, cld3)
        Set subs = new HashSet(Arrays.asList(new Object[] {cld2, cld3}));

        assertEquals(subs, cld1.getSubclassDescriptors());
    }


    public void testGetImplementorDescriptors() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("interface1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, "interface1", false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 = new ClassDescriptor("Class3", null, "interface1", false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld4 = new ClassDescriptor("Class4", "Class2", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model2 = new Model("test2", new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3, cld4})));

        // getImplementorDescriptrors should just return direct implementations (cld2, cld3)
        Set impls = new HashSet(Arrays.asList(new Object[] {cld2, cld3}));

        assertEquals(impls, cld1.getImplementorDescriptors());
    }

    public void testEquals1() throws Exception {
        ClassDescriptor c1 = new ClassDescriptor("class1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor c2 = new ClassDescriptor("class1", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor c3 = new ClassDescriptor("class1", null, null, true, Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_SET);
        ClassDescriptor c4 = new ClassDescriptor("class2", null, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor c5 = new ClassDescriptor("class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor c6 = new ClassDescriptor("class1", "flibble", null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor c7 = new ClassDescriptor("class1", null, "flibble", true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor c8 = new ClassDescriptor("class1", null, null, false, Collections.singleton(new AttributeDescriptor("field", false, "java.lang.String")), new HashSet(), new HashSet());
        ClassDescriptor c9 = new ClassDescriptor("class1", null, null, false, Collections.singleton(new AttributeDescriptor("field", false, "java.lang.String")), new HashSet(), new HashSet());
        
        assertEquals(c1, c2);
        assertEquals(c1, c3);
        assertTrue(!c1.equals(c4));
        assertTrue(!c1.equals(c5));
        assertTrue(!c1.equals(c6));
        assertTrue(!c1.equals(c7));
        assertTrue(!c5.equals(c8));
        assertEquals(c8, c9);
    }

    public void testToString() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", "Class2", "Interface1", false, new HashSet(), new HashSet(), new HashSet());
        String expected = "<class name=\"Class1\" extends=\"Class2\" implements=\"Interface1\" is-interface=\"false\">"
            + "</class>";

        assertEquals(expected, cld1.toString());
    }



    // ============================================

    private Set getAttributes() {
        Set attributes = new HashSet();
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", false, "String");
        AttributeDescriptor atd2 = new AttributeDescriptor("atd2", true, "Integer");
        attributes.add(atd1);
        attributes.add(atd2);
        return attributes;
    }

    private Set getReferences() {
        Set references = new HashSet();
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "String", "reverse1");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", true, "Integer", "reverse2");
        references.add(rfd1);
        references.add(rfd2);
        return references;
    }

    private Set getCollections() {
        Set collections = new HashSet();
        CollectionDescriptor cld1 = new CollectionDescriptor("cld1", false, "String", "reverse1", false);
        CollectionDescriptor cld2 = new CollectionDescriptor("cld2", true, "Integer", "reverse2", false);
        collections.add(cld1);
        collections.add(cld2);
        return collections;
    }
}
