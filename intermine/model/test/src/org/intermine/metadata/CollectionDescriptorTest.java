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
import java.util.Set;

import junit.framework.TestCase;


public class CollectionDescriptorTest extends TestCase
{
    private static final Set<AttributeDescriptor> noAttrs =
            Collections.unmodifiableSet(new HashSet<AttributeDescriptor>());
    private static final Set<ReferenceDescriptor> noRefs =
            Collections.unmodifiableSet(new HashSet<ReferenceDescriptor>());
    private static final Set<CollectionDescriptor> noColls =
            Collections.unmodifiableSet(new HashSet<CollectionDescriptor>());

    public CollectionDescriptorTest(String arg) {
        super(arg);
    }

    public void testNullConstructorFields() throws Exception {
        try {
            new CollectionDescriptor(null, "String", null);
            fail("Expected IllegalArgumentException for null name");
        } catch (IllegalArgumentException e) {
        }

        try {
            new CollectionDescriptor("", "String", null);
            fail("Expected: IllegalArgumentException for empty name");
        } catch (IllegalArgumentException e) {
        }

        try {
            new CollectionDescriptor("name", null, null);
            fail("Expected IllegalArgumentException for null referencedType");
        } catch (IllegalArgumentException e) {
        }

        try {
            new CollectionDescriptor("name", "", null);
            fail("Expected: IllegalArgumentException for empty referencedType");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSetClassDescriptor() throws Exception {

        ClassDescriptor cld = new ClassDescriptor("Class1", null, false, noAttrs, noRefs, noColls);

        CollectionDescriptor cod = new CollectionDescriptor("name", "String", null);
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
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "Class2", null);
        Set<CollectionDescriptor> collections = Collections.singleton(cod1);
        // cld1 has a CollectionDescriptor that contains objects of type Class2
        new ClassDescriptor("Class1", null, false, noAttrs, noRefs, collections);
        new ClassDescriptor("Class2", null, false, noAttrs, noRefs, noColls);
        try {
            cod1.getReferencedClassDescriptor();
            fail("Expected IllegalStateException, model has not yet been set");
        } catch (IllegalStateException e) {
        }
    }

    public void testGetReferencedClass() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "package.name.Class2", null);
        Set<CollectionDescriptor> collections = Collections.singleton(cod1);
        // cld1 has a ReferenceDescriptor that points to Class2
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, noAttrs, noRefs, collections);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, noAttrs, noRefs, noColls);
        new Model("model", "package.name", Arrays.asList(cld1, cld2));
        try {
            ClassDescriptor refCld = cod1.getReferencedClassDescriptor();
            assertTrue("ClassDescriptor was null", refCld != null);
            assertTrue("Expected ClassDescriptor to be Class2", refCld.getName().equals("package.name.Class2"));
        } catch (IllegalStateException e) {
            fail("Should have returned a ClassDescriptor");
        }
    }

    public void testReverseReferenceValid() throws Exception {
        // codd1 in Class1 points to Class2, cod2 in Class2 points to Class1
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "package.name.Class2", "cod2");
        CollectionDescriptor cod2 = new CollectionDescriptor("cod2", "package.name.Class1", "cod1");
        Set<CollectionDescriptor> cols1 = Collections.singleton(cod1);
        Set<CollectionDescriptor> cols2 = Collections.singleton(cod2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, noAttrs, noRefs, cols1);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, noAttrs, noRefs, cols2);
        new Model("model", "package.name", Arrays.asList(cld1, cld2));
        try {
            ReferenceDescriptor rfdReverse = cod1.getReverseReferenceDescriptor();
            assertEquals(cod2, rfdReverse);
            assertEquals(cld1, rfdReverse.getReferencedClassDescriptor());
        } catch (IllegalStateException e) {
            fail("Should have returned reverse ReferenceDescriptor");
        }
    }

    public void testReverseReferenceInvalid() throws Exception {
        // cod1 points to Class2 but has reverse reference (codDummy) that is not a field of Class1
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "package.name.Class2", "codDummy");
        CollectionDescriptor cod2 = new CollectionDescriptor("cod2", "package.name.Class1", "cod1");
        Set<CollectionDescriptor> cols1 = Collections.singleton(cod1);
        Set<CollectionDescriptor> cols2 = Collections.singleton(cod2);
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, noAttrs, noRefs, cols1);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, noAttrs, noRefs, cols2);

        try {
            new Model("model", "package.name", Arrays.asList(cld1, cld2));
            fail("Expected a MetaDataException to be thrown");
        } catch (MetaDataException e) {
        }
    }

    public void testRelationTypeOneToMany() throws Exception {
        CollectionDescriptor col = new CollectionDescriptor("col1", "package.name.Class1", "ref1");
        ReferenceDescriptor ref  = new ReferenceDescriptor("ref1", "package.name.Class1", null);
        Set<CollectionDescriptor> cols = Collections.singleton(col);
        Set<ReferenceDescriptor> refs = Collections.singleton(ref);
        ClassDescriptor cld = new ClassDescriptor("package.name.Class1", null, false, noAttrs, refs, cols);
        new Model("model1", "package.name", Collections.singleton(cld));
        assertEquals(FieldDescriptor.ONE_N_RELATION, col.relationType());
    }

    public void testRelationTypeManyToMany() throws Exception {
        CollectionDescriptor col1 = new CollectionDescriptor("col1", "package.name.Class1", "col2");
        CollectionDescriptor col2 = new CollectionDescriptor("col2", "package.name.Class1", null);
        Set<CollectionDescriptor> cols = new HashSet<CollectionDescriptor>(Arrays.asList(col1, col2));
        ClassDescriptor cld = new ClassDescriptor("package.name.Class1", null, false, noAttrs, noRefs, cols);
        new Model("model1", "package.name", Collections.singleton(cld));
        assertEquals(FieldDescriptor.M_N_RELATION, col1.relationType());
    }

    public void testRelationTypeUnidirectional() throws Exception {
        CollectionDescriptor col = new CollectionDescriptor("col1", "package.name.Class1", null);
        Set<CollectionDescriptor> cols = Collections.singleton(col);
        ClassDescriptor cld = new ClassDescriptor("package.name.Class1", null, false, noAttrs, noRefs, cols);
        new Model("model1", "package.name", Collections.singleton(cld));
        assertEquals(FieldDescriptor.M_N_RELATION, col.relationType());
    }

    public void testEquals() throws Exception {
        CollectionDescriptor cod1 = new CollectionDescriptor("cod1", "Class1", "cod1");
        CollectionDescriptor cod2 = new CollectionDescriptor("cod1", "Class1", "cod1");
        CollectionDescriptor cod3 = new CollectionDescriptor("cod2", "Class1", "cod1");
        CollectionDescriptor cod5 = new CollectionDescriptor("cod1", "Class2", "cod1");
        CollectionDescriptor cod6 = new CollectionDescriptor("cod1", "Class1", "cod2");
        assertEquals(cod1, cod2);
        assertEquals(cod1.hashCode(), cod2.hashCode());
        assertFalse(cod1.equals(cod3));
        assertFalse(cod1.equals(cod5));
        assertFalse(cod1.equals(cod6));
    }

    public void testToString() throws Exception {
        CollectionDescriptor col = new CollectionDescriptor("ref", "package.name.Class1", null);
        String expected = "<collection name=\"ref\" referenced-type=\"Class1\"/>";
        assertEquals(col.toString(), expected);
        col = new CollectionDescriptor("ref", "package.name.Class1", "reverseRef");
        expected = "<collection name=\"ref\" referenced-type=\"Class1\" reverse-reference=\"reverseRef\"/>";
        assertEquals(col.toString(), expected);
    }
}
