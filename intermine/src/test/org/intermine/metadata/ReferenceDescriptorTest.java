package org.flymine.metadata;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class ReferenceDescriptorTest extends TestCase {

    public ReferenceDescriptorTest(String arg) {
        super(arg);
    }

    public void setUp() {
    }

    public void tearDown() {
    }


    public void testNullConstructorFields() throws Exception {
        try {
            ReferenceDescriptor rfd = new ReferenceDescriptor(null, true, "String", "String");
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            ReferenceDescriptor rfd = new ReferenceDescriptor("", true, "String", "String");
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            ReferenceDescriptor rfd = new ReferenceDescriptor("name", true, null, "String");
            fail("Expected: IllegalArgumentException, referencedType parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            ReferenceDescriptor rfd = new ReferenceDescriptor("name", true, "", "String");
            fail("Expected: IllegalArgumentException, referencedType parameter");
        } catch (IllegalArgumentException e) {
        }

    }


    public void testSetClassDescriptor() throws Exception {

        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  new HashSet(), new HashSet(), new HashSet());

        ReferenceDescriptor rfd = new ReferenceDescriptor("name", true, "String", "String");
        try {
            rfd.setClassDescriptor(cld);
        } catch (IllegalStateException e) {
            fail("should have been able set ClassDescriptor");
        }

        try {
            rfd.setClassDescriptor(cld);
            fail("Expected: IllegalStateException, ClassDescriptor already set");
        } catch (IllegalStateException e) {
        }
    }


    public void testReferencedClassNotSet() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        Set references = Collections.singleton(rfd1);
        // cld1 has a ReferenceDescriptor that points to Class2
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), references, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        try {
            ClassDescriptor refCld = rfd1.getReferencedClassDescriptor();
            fail("Expected IllegalStateException, model has not yet been set");
        } catch (IllegalStateException e) {
        }
    }


    public void testGetReferencedClass() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", null);
        Set references = Collections.singleton(rfd1);
        // cld1 has a ReferenceDescriptor that points to Class2
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), references, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));
        try {
            ClassDescriptor refCld = rfd1.getReferencedClassDescriptor();
            assertTrue("ClassDescriptor was null", refCld != null);
            assertTrue("Expected ClassDescriptor to be Class2", refCld.getClassName() == "Class2");
        } catch (IllegalStateException e) {
            fail("Should have returned a ClassDescriptor");
        }
    }

    public void testReverseReferenceValid() throws Exception {
        // rfd1 in Class1 points to Class2, rfd2 in Class2 points to Class1
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", "rfd2");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", false, "Class1", "rfd1");
        Set refs1 = Collections.singleton(rfd1);
        Set refs2 = Collections.singleton(rfd2);
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), refs1, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), refs2, new HashSet());
        Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));
        try {
            ReferenceDescriptor rfdReverse = rfd1.getReverseReferenceDescriptor();
            assertEquals(rfd2, rfdReverse);
            assertEquals(cld1, rfdReverse.getReferencedClassDescriptor());

        } catch (IllegalStateException e) {
            fail("Should have returned reverse ReferenceDescriptor");
        }
    }

    public void testRevereseReferenceInvalid() throws Exception {
        // rfd1 points to Class2 but has reverse reference (rfdDummy) that is not a field of Class1
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", false, "Class2", "rfdDummy");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", false, "Class1", "rfd1");
        Set refs1 = Collections.singleton(rfd1);
        Set refs2 = Collections.singleton(rfd2);
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), refs1, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), refs2, new HashSet());

        try {
            Model model = new Model("model", new HashSet(Arrays.asList(new Object[] {cld1, cld2})));
            fail("Expected a MetaDataException to be thrown");
        } catch (MetaDataException e) {
        }
    }

}
