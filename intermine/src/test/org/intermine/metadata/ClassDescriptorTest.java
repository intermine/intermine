package org.flymine.metadata;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;


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
                                                      new ArrayList(), new ArrayList(), new ArrayList());
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    public void testSetModel() throws Exception {
        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  new ArrayList(), new ArrayList(), new ArrayList());
        try {
            Model model1 = new Model("model1", Arrays.asList(new Object[] {cld}));
        } catch (IllegalStateException e) {
            fail("Model should have been set correctly");
        }

        try {
            Model model2 = new Model("model2", Arrays.asList(new Object[] {cld}));
            fail("Model already set, expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }


    public void testInterfaceDescriptors() throws Exception {
        ClassDescriptor int1 = new ClassDescriptor("Interface1", null, null, true, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor int2 = new ClassDescriptor("Interface2", null, null, true, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld = new ClassDescriptor("Class1", null, "Interface1 Interface2", false,
                                                  new ArrayList(), new ArrayList(), new ArrayList());

        List clds = new ArrayList(Arrays.asList(new Object[] {int1, int2, cld}));
        Model model = new Model("test", clds);

        List interfaces = new ArrayList(Arrays.asList(new Object[] {int1, int2}));
        assertEquals(interfaces, cld.getInterfaceDescriptors());

    }

    public void testImplementsNotExists() throws Exception {
        // construct class implementing Interface3 which does not exist
        ClassDescriptor int1 = new ClassDescriptor("Interface1", null, null, true, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor int2 = new ClassDescriptor("Interface2", null, null, true, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld = new ClassDescriptor("Class1", null, "Interface1 Interface3", false,
                                                  new ArrayList(), new ArrayList(), new ArrayList());

        List clds = new ArrayList(Arrays.asList(new Object[] {int1, int2, cld}));
        try {
            Model model = new Model("test", clds);
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }
    }

    public void testImplementsNotAnInterface() throws Exception {
        // construct a class implementing Class2 which is not an interface
        ClassDescriptor int1 = new ClassDescriptor("Interface1", null, null, true, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor int2 = new ClassDescriptor("Class2", null, null, false, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld = new ClassDescriptor("Class1", null, "Interface1 Class2", false,
                                                  new ArrayList(), new ArrayList(), new ArrayList());

        List clds = new ArrayList(Arrays.asList(new Object[] {int1, int2, cld}));
        try {
            Model model = new Model("test", clds);
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }
    }


    public void testSuperClassExists() throws Exception {
        ClassDescriptor superCld = new ClassDescriptor("superCld", null, null, false,
                                                       new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld = new ClassDescriptor("cld", "superCld", null, false,  new ArrayList(), new ArrayList(), new ArrayList());
        Model model = new Model("test", Arrays.asList(new Object[] {cld, superCld}));
        assertEquals(superCld, cld.getSuperclassDescriptor());

    }


    public void testSuperClassNotExists() throws Exception {
        ClassDescriptor superCld = new ClassDescriptor("superCld", null, null, false,
                                                       new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld = new ClassDescriptor("cld", "anotherCld", null, false,  new ArrayList(), new ArrayList(), new ArrayList());
        try {
            Model model = new Model("test", Arrays.asList(new Object[] {cld, superCld}));
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }
    }


    public void testSuperClassWrongType() throws Exception {
        // test where superclass is an interface, subclass isn't
        ClassDescriptor int1 = new ClassDescriptor("interface1", null, null, true,
                                                       new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld = new ClassDescriptor("cld", "interface1", null, false,  new ArrayList(), new ArrayList(), new ArrayList());
        try {
            Model model1 = new Model("test", Arrays.asList(new Object[] {cld, int1}));
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }

        // test where superclass is not an interface, subclass is
        ClassDescriptor superCld = new ClassDescriptor("superCld", null, null, false,
                                                       new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor interface1 = new ClassDescriptor("interface1", "superCld", null, true,  new ArrayList(), new ArrayList(), new ArrayList());
        try {
            Model model2 = new Model("test", Arrays.asList(new Object[] {interface1, superCld}));
            fail("Expected MetaDataException");
        } catch (MetaDataException e) {
        }

    }


    public void testAttributeDescriptorByName() throws Exception {
        List attributes = getAttributes();
        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  attributes, new ArrayList(), new ArrayList());
        assertNotNull(cld.getAttributeDescriptorByName("atd1"));
        assertNotNull(cld.getAttributeDescriptorByName("atd2"));
    }

    public void testReferenceDescriptorByName() throws Exception {
        List refs = getReferences();
        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  new ArrayList(), refs, new ArrayList());
        assertNotNull(cld.getReferenceDescriptorByName("rfd1"));
        assertNotNull(cld.getReferenceDescriptorByName("rfd2"));
    }

    public void testCollectionDescriptorByName() throws Exception {
        List cols = getCollections();
        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  new ArrayList(), new ArrayList(), cols);
        assertNotNull(cld.getCollectionDescriptorByName("cld1"));
        assertNotNull(cld.getCollectionDescriptorByName("cld2"));
    }

    public void testPkFieldDescriptors() throws Exception {
        List atbs = getAttributes();
        List rfds = getReferences();
        List clds = getCollections();

        // six fields in class, three of them are pks
        List pks = new ArrayList();
        pks.add(atbs.get(1));
        pks.add(rfds.get(1));
        pks.add(clds.get(1));

        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  atbs, rfds, clds);

        assertEquals(pks, cld.getPkFieldDescriptors());
    }


    public void testUltimateSuperClassOne() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, new ArrayList(), new ArrayList(), new ArrayList());

        Model model1 = new Model("test1", Arrays.asList(new Object[] {cld1, cld2}));

        assertEquals(cld1, cld2.getUltimateSuperclassDescriptor());
    }

    public void testUltimateSuperClassMany() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", "Class1", null, false, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld3 = new ClassDescriptor("Class3", "Class2", null, false, new ArrayList(), new ArrayList(), new ArrayList());
        ClassDescriptor cld4 = new ClassDescriptor("Class4", "Class3", null, false, new ArrayList(), new ArrayList(), new ArrayList());

        Model model2 = new Model("test2", Arrays.asList(new Object[] {cld1, cld2, cld3, cld4}));

        assertEquals(cld1, cld4.getUltimateSuperclassDescriptor());
    }

    public void testUltimateSuperClassNone() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new ArrayList(), new ArrayList(), new ArrayList());

        Model model1 = new Model("test1", Arrays.asList(new Object[] {cld1}));

        assertTrue("ultimate superclass should have been null", cld1.getUltimateSuperclassDescriptor() == null);
    }


    // ------------

    private List getAttributes() {
        List attributes = new ArrayList();
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", false, "String");
        AttributeDescriptor atd2 = new AttributeDescriptor("atd2", true, "Integer");
        attributes.add(atd1);
        attributes.add(atd2);
        return attributes;
    }

    private List getReferences() {
        List references = new ArrayList();
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "String", "reverse1");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", true, "Integer", "reverse2");
        references.add(rfd1);
        references.add(rfd2);
        return references;
    }

    private List getCollections() {
        List collections = new ArrayList();
        CollectionDescriptor cld1 = new CollectionDescriptor("cld1", false, "String", "reverse1", false);
        CollectionDescriptor cld2 = new CollectionDescriptor("cld2", true, "Integer", "reverse2", false);
        collections.add(cld1);
        collections.add(cld2);
        return collections;
    }


}
