package org.flymine.metadata;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;


public class CollectionDescriptorTest extends TestCase {

    public CollectionDescriptorTest(String arg) {
        super(arg);
    }

    public void setUp() {
    }

    public void tearDown() {
    }


    public void testNullConstructorFields() throws Exception {
        try {
            CollectionDescriptor cod = new CollectionDescriptor(null, true, "String", null, true);
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            CollectionDescriptor cod = new CollectionDescriptor("", true, "String", null, true);
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            CollectionDescriptor cod = new CollectionDescriptor("name", true, null, null, true);
            fail("Expected: IllegalArgumentException, referencedType parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            CollectionDescriptor cod = new CollectionDescriptor("name", true, "", null, true);
            fail("Expected: IllegalArgumentException, referencedType parameter");
        } catch (IllegalArgumentException e) {
        }

    }


    public void testSetClassDescriptor() throws Exception {

        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  new ArrayList(), new ArrayList(), new ArrayList());

        CollectionDescriptor cod = new CollectionDescriptor("name", true, "String", null, true);
        try {
            cod.setClassDescriptor(cld);
        } catch (IllegalStateException e) {
            fail("should have been able set ClassDescriptor");
        }

        try {
            cod.setClassDescriptor(cld);
            fail("Expected: IllegalStateException, ClassDescriptor already set");
        } catch (IllegalStateException e) {
        }
    }


    public void testReferencedClassNotSet() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, true);
        List collections = Arrays.asList(new Object[] {cod1});
        // cld1 has a CollectionDescriptor that contains objects of type Class2
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new ArrayList(), new ArrayList(), collections);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new ArrayList(), new ArrayList(), new ArrayList());
        try {
            ClassDescriptor refCld = cod1.getReferencedClassDescriptor();
            fail("Expected IllegalStateException, model has not yet been set");
        } catch (IllegalStateException e) {
        }
    }


    public void testGetReferencedClass() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", null, true);
        List collections = Arrays.asList(new Object[] {cod1});
        // cld1 has a ReferenceDescriptor that points to Class2
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new ArrayList(), new ArrayList(), collections);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new ArrayList(), new ArrayList(), new ArrayList());
        Model model = new Model("model", Arrays.asList(new Object[] {cld1, cld2}));
        try {
            ClassDescriptor refCld = cod1.getReferencedClassDescriptor();
            assertTrue("ClassDescriptor was null", refCld != null);
            assertTrue("Expected ClassDescriptor to be Class2", refCld.getClassName() == "Class2");
        } catch (IllegalStateException e) {
            fail("Should have returned a ClassDescriptor");
        }
    }


    public void testReverseReferenceValid() throws Exception {
        // codd1 in Class1 points to Class2, cod2 in Class2 points to Class1
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", "cod2", true);
        CollectionDescriptor cod2 = new CollectionDescriptor("cod2", false, "Class1", "cod1", true);
        List cols1 = Arrays.asList(new Object[] {cod1});
        List cols2 = Arrays.asList(new Object[] {cod2});
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new ArrayList(), new ArrayList(), cols1);
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new ArrayList(), new ArrayList(), cols2);
        Model model = new Model("model", Arrays.asList(new Object[] {cld1, cld2}));
        try {
            ReferenceDescriptor rfdReverse = cod1.getReverseReferenceDescriptor();
            assertEquals(cod2, rfdReverse);
            assertEquals(cld1, rfdReverse.getReferencedClassDescriptor());
        } catch (IllegalStateException e) {
            fail("Should have returned reverse ReferenceDescriptor");
        }
    }

    public void testRevereseReferenceInvalid() throws Exception {
        // cod1 points to Class2 but has reverse reference (codDummy) that is not a field of Class1
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", false, "Class2", "codDummy", true);
        CollectionDescriptor cod2 = new CollectionDescriptor("cod2", false, "Class1", "cod1", true);
        List cols1 = Arrays.asList(new Object[] {cod1});
        List cols2 = Arrays.asList(new Object[] {cod2});
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new ArrayList(), cols1, new ArrayList());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new ArrayList(), cols2, new ArrayList());

        try {
            Model model = new Model("model", Arrays.asList(new Object[] {cld1, cld2}));
            fail("Expected a MetaDataException to be thrown");
        } catch (MetaDataException e) {
        }
    }

}
