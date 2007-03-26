package org.intermine.metadata;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ReferenceDescriptorTest extends TestCase
{
    private static final Set EMPTY_SET = Collections.EMPTY_SET;
    private String uri = "http://www.intermine.org/model/testmodel";

    public ReferenceDescriptorTest(String arg) {
        super(arg);
    }

    public void testNullConstructorFields() throws Exception {
        try {
            ReferenceDescriptor rfd = new ReferenceDescriptor(null, "String", "String");
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            ReferenceDescriptor rfd = new ReferenceDescriptor("", "String", "String");
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            ReferenceDescriptor rfd = new ReferenceDescriptor("name", null, "String");
            fail("Expected: IllegalArgumentException, referencedType parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            ReferenceDescriptor rfd = new ReferenceDescriptor("name", "", "String");
            fail("Expected: IllegalArgumentException, referencedType parameter");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSetClassDescriptor() throws Exception {
        ClassDescriptor cld = new ClassDescriptor("Class1", null, false,
                                                  new HashSet(), new HashSet(), new HashSet());
        ReferenceDescriptor rfd = new ReferenceDescriptor("name", "String", "String");
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
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "Class2", null);
        Set references = Collections.singleton(rfd1);
        // cld1 has a ReferenceDescriptor that points to Class2
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), references, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        try {
            ClassDescriptor refCld = rfd1.getReferencedClassDescriptor();
            fail("Expected IllegalStateException, model has not yet been set");
        } catch (IllegalStateException e) {
        }
    }

    public void testGetReferencedClass() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "Class2", null);
        Set references = Collections.singleton(rfd1);
        // cld1 has a ReferenceDescriptor that points to Class2
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), references, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2})));
        try {
            ClassDescriptor refCld = rfd1.getReferencedClassDescriptor();
            assertTrue("ClassDescriptor was null", refCld != null);
            assertTrue("Expected ClassDescriptor to be Class2", refCld.getName().equals("Class2"));
        } catch (IllegalStateException e) {
            fail("Should have returned a ClassDescriptor");
        }
    }

    public void testReverseReferenceValid() throws Exception {
        // rfd1 in Class1 points to Class2, rfd2 in Class2 points to Class1
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "Class2", "rfd2");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", "Class1", "rfd1");
        Set refs1 = Collections.singleton(rfd1);
        Set refs2 = Collections.singleton(rfd2);
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), refs1, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), refs2, new HashSet());
        Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2})));
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
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "Class2", "rfdDummy");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", "Class1", "rfd1");
        Set refs1 = Collections.singleton(rfd1);
        Set refs2 = Collections.singleton(rfd2);
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), refs1, new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), refs2, new HashSet());
        try {
            Model model = new Model("model", uri, new HashSet(Arrays.asList(new Object[] {cld1, cld2})));
            fail("Expected a MetaDataException to be thrown");
        } catch (MetaDataException e) {
        }
    }

    public void testRelationTypeOneToOne() throws Exception {
        ReferenceDescriptor ref1  = new ReferenceDescriptor("ref1", "Class1", "ref2");
        ReferenceDescriptor ref2  = new ReferenceDescriptor("ref2", "Class1", null);
        Set refs = new HashSet(Arrays.asList(new Object[] { ref1, ref2 }));
        ClassDescriptor cld = new ClassDescriptor("Class1", null, false, EMPTY_SET, refs, EMPTY_SET);
        Model model = new Model("model1", uri, Collections.singleton(cld));
        assertEquals(FieldDescriptor.ONE_ONE_RELATION, ref1.relationType());
    }

    public void testRelationTypeManyToOne() throws Exception {
        CollectionDescriptor col = new CollectionDescriptor("col1", "Class1", null);
        ReferenceDescriptor ref  = new ReferenceDescriptor("ref1", "Class1", "col1");
        Set cols = Collections.singleton(col);
        Set refs = Collections.singleton(ref);
        ClassDescriptor cld = new ClassDescriptor("Class1", null, false, EMPTY_SET, refs, cols);
        Model model = new Model("model1", uri, Collections.singleton(cld));
        assertEquals(FieldDescriptor.N_ONE_RELATION, ref.relationType());
    }

    public void testRelationTypeUnidirectional() throws Exception {
        ReferenceDescriptor ref = new ReferenceDescriptor("ref1", "Class1", null);
        Set refs = Collections.singleton(ref);
        ClassDescriptor cld = new ClassDescriptor("Class1", null, false, EMPTY_SET, refs, EMPTY_SET);
        Model model = new Model("model1", uri, Collections.singleton(cld));
        assertEquals(FieldDescriptor.N_ONE_RELATION, ref.relationType());
    }

    public void testEquals() throws Exception {
        ReferenceDescriptor ref1 = new ReferenceDescriptor("rfd1", "Class2", "rfd1");
        ReferenceDescriptor ref2 = new ReferenceDescriptor("rfd1", "Class2", "rfd1");
        ReferenceDescriptor ref3 = new ReferenceDescriptor("rfd2", "Class2", "rfd1");
        ReferenceDescriptor ref5 = new ReferenceDescriptor("rfd1", "Class3", "rfd1");
        ReferenceDescriptor ref6 = new ReferenceDescriptor("rfd1", "Class2", "rfd2");
        assertEquals(ref1, ref2);
        assertEquals(ref1.hashCode(), ref2.hashCode());
        assertFalse(ref1.equals(ref3));
        assertFalse(ref1.equals(ref5));
        assertFalse(ref1.equals(ref6));
    }

    public void testToString() throws Exception {
        ReferenceDescriptor ref = new ReferenceDescriptor("ref", "Class1", null);
        String expected = "<reference name=\"ref\" referenced-type=\"Class1\"/>";
        assertEquals(ref.toString(), expected);
        ref = new ReferenceDescriptor("ref", "Class1", "reverseRef");
        expected = "<reference name=\"ref\" referenced-type=\"Class1\" reverse-reference=\"reverseRef\"/>";
        assertEquals(ref.toString(), expected);
    }
}
