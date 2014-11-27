package org.intermine.metadata;

/*
 * Copyright (C) 2002-2014 FlyMine
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
import java.util.Set;

import junit.framework.TestCase;

public class ReferenceDescriptorTest extends TestCase
{
    private ClassDescriptorFactory cldFac = new ClassDescriptorFactory("package.name");

    public ReferenceDescriptorTest(String arg) {
        super(arg);
    }

    public void testNullConstructorFields() throws Exception {
        try {
            new ReferenceDescriptor(null, "String", "String");
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            new ReferenceDescriptor("", "String", "String");
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            new ReferenceDescriptor("name", null, "String");
            fail("Expected: IllegalArgumentException, referencedType parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            new ReferenceDescriptor("name", "", "String");
            fail("Expected: IllegalArgumentException, referencedType parameter");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSetClassDescriptor() throws Exception {
        ClassDescriptor cld = cldFac.makeClass("Class1");
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
        Set<ReferenceDescriptor> references = Collections.singleton(rfd1);
        // cld1 has a ReferenceDescriptor that points to Class2
        new ClassDescriptor("Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, references, ClassDescriptorFactory.NO_COLLS);
        new ClassDescriptor("Class2", null, false,
                ClassDescriptorFactory.NO_ATTRS, ClassDescriptorFactory.NO_REFS, ClassDescriptorFactory.NO_COLLS);
        try {
            rfd1.getReferencedClassDescriptor();
            fail("Expected IllegalStateException, model has not yet been set");
        } catch (IllegalStateException e) {
        }
    }

    public void testGetReferencedClass() throws Exception {
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "package.name.Class2", null);
        Set<ReferenceDescriptor> references = Collections.singleton(rfd1);        
        // cld1 has a ReferenceDescriptor that points to Class2
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, references, ClassDescriptorFactory.NO_COLLS);
        ClassDescriptor cld2 = cldFac.makeClass("Class2");
        new Model("model", "package.name", Arrays.asList(cld1, cld2));
        try {
            ClassDescriptor refCld = rfd1.getReferencedClassDescriptor();
            assertTrue("ClassDescriptor was null", refCld != null);
            assertTrue("Expected ClassDescriptor to be Class2", refCld.getName().equals("package.name.Class2"));
        } catch (IllegalStateException e) {
            fail("Should have returned a ClassDescriptor");
        }
    }

    public void testReverseReferenceValid() throws Exception {
        // rfd1 in Class1 points to Class2, rfd2 in Class2 points to Class1
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "package.name.Class2", "rfd2");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", "package.name.Class1", "rfd1");
        Set<ReferenceDescriptor> refs1 = Collections.singleton(rfd1);
        Set<ReferenceDescriptor> refs2 = Collections.singleton(rfd2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs1, ClassDescriptorFactory.NO_COLLS);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs2, ClassDescriptorFactory.NO_COLLS);
        new Model("model", "package.name", Arrays.asList(cld1, cld2));
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
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "package.name.Class2", "rfdDummy");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", "package.name.Class1", "rfd1");
        Set<ReferenceDescriptor> refs1 = Collections.singleton(rfd1);
        Set<ReferenceDescriptor> refs2 = Collections.singleton(rfd2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs1, ClassDescriptorFactory.NO_COLLS);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs2, ClassDescriptorFactory.NO_COLLS);
        try {
            new Model("model", "package.name", Arrays.asList(cld1, cld2));
            fail("Expected a MetaDataException to be thrown");
        } catch (MetaDataException e) {
        }
    }

    // test reverse references that don't point to one another
    public void testRevereseReferenceNotReciprocal() throws Exception {
        // rfd1 points to Class2 but has reverse-reference that points to another field  in Class1
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "package.name.Class2", "rfd2");
        ReferenceDescriptor rfdOther1 = new ReferenceDescriptor("rfdOther1", "package.name.Class2", "rfd2");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", "package.name.Class1", "rfdOther1");
        Set<ReferenceDescriptor> refs1 = new HashSet<ReferenceDescriptor>(Arrays.asList(rfd1, rfdOther1));
        Set<ReferenceDescriptor> refs2 = Collections.singleton(rfd2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs1, ClassDescriptorFactory.NO_COLLS);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs2, ClassDescriptorFactory.NO_COLLS);
        Model model = new Model("model", "package.name", Arrays.asList(cld1, cld2));
        // this no longer throws an exception , instead creates the model but adds a problem
        assertEquals(1, model.getProblems().size());
    }

    // test reverse reference points to an attribute
    public void testRevereseReferenceIsAttribute() throws Exception {
        // rfd1 points to Class2.atd2 which is an attribute
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "package.name.Class2", "atd2");
        AttributeDescriptor atd2 = new AttributeDescriptor("atd2", "java.lang.String");
        Set<ReferenceDescriptor> refs1 = Collections.singleton(rfd1);
        Set<AttributeDescriptor> atts = Collections.singleton(atd2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs1, ClassDescriptorFactory.NO_COLLS);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, atts,
                ClassDescriptorFactory.NO_REFS, ClassDescriptorFactory.NO_COLLS);
        try {
            new Model("model", "package.name", Arrays.asList(cld1, cld2));
            fail("Expected a MetaDataException to be thrown");
        } catch (MetaDataException e) {
        }
    }


    // test reverse reference of the wrong referenced type
    public void testRevereseReferenceWrongType() throws Exception {
        // rfd1 points to Class2 but has reverse-reference rfd2 which exists but is a refernece to Class3
        ReferenceDescriptor rfd1 = new ReferenceDescriptor("rfd1", "package.name.Class2", "rfd2");
        ReferenceDescriptor rfd2 = new ReferenceDescriptor("rfd2", "package.name.Class3", "rfd1");
        ReferenceDescriptor rfd3 = new ReferenceDescriptor("rfd1", "package.name.Class2", "rfd2");
        //ReferenceDescriptor rfdOther2 = new ReferenceDescriptor("rfdOther2", "package.name.Class1", "rfd2");
        Set<ReferenceDescriptor> refs1 = Collections.singleton(rfd1);
        Set<ReferenceDescriptor> refs2 = Collections.singleton(rfd2);
        Set<ReferenceDescriptor> refs3 = Collections.singleton(rfd3);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs1, ClassDescriptorFactory.NO_COLLS);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs2, ClassDescriptorFactory.NO_COLLS);
        ClassDescriptor cld3 = new ClassDescriptor("package.name.Class3", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs3, ClassDescriptorFactory.NO_COLLS);

        Model model = new Model("model", "package.name", Arrays.asList(cld1, cld2, cld3));
        assertEquals(1, model.getProblems().size());
    }

    public void testRelationTypeOneToOne() throws Exception {
        ReferenceDescriptor ref1  = new ReferenceDescriptor("ref1", "package.name.Class1", "ref2");
        ReferenceDescriptor ref2  = new ReferenceDescriptor("ref2", "package.name.Class1", null);
        ClassDescriptor cld = new ClassDescriptor("package.name.Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, Arrays.asList(ref1, ref2), ClassDescriptorFactory.NO_COLLS);
        new Model("model1", "package.name", Collections.singleton(cld));
        assertEquals(FieldDescriptor.ONE_ONE_RELATION, ref1.relationType());
    }

    public void testRelationTypeManyToOne() throws Exception {
        CollectionDescriptor col = new CollectionDescriptor("col1", "package.name.Class1", null);
        ReferenceDescriptor ref  = new ReferenceDescriptor("ref1", "package.name.Class1", "col1");
        Set<CollectionDescriptor> cols = Collections.singleton(col);
        Set<ReferenceDescriptor> refs = Collections.singleton(ref);
        ClassDescriptor cld = new ClassDescriptor("package.name.Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, refs, cols);
        new Model("model1", "package.name", Collections.singleton(cld));
        assertEquals(FieldDescriptor.N_ONE_RELATION, ref.relationType());
    }

    public void testRelationTypeUnidirectional() throws Exception {
        ReferenceDescriptor ref = new ReferenceDescriptor("ref1", "package.name.Class1", null);
        ClassDescriptor cld = new ClassDescriptor("package.name.Class1", null, false,
                ClassDescriptorFactory.NO_ATTRS, Collections.singleton(ref), ClassDescriptorFactory.NO_COLLS);
        new Model("model1", "package.name", Collections.singleton(cld));
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
        ReferenceDescriptor ref = new ReferenceDescriptor("ref", "package.name.Class1", null);
        String expected = "<reference name=\"ref\" referenced-type=\"Class1\"/>";
        assertEquals(ref.toString(), expected);
        ref = new ReferenceDescriptor("ref", "package.name.Class1", "reverseRef");
        expected = "<reference name=\"ref\" referenced-type=\"Class1\" reverse-reference=\"reverseRef\"/>";
        assertEquals(ref.toString(), expected);
    }
}
