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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class FieldDescriptorTest extends TestCase
{
    private static final Set<AttributeDescriptor> noAttrs =
            Collections.unmodifiableSet(new HashSet<AttributeDescriptor>());
    private static final Set<ReferenceDescriptor> noRefs =
            Collections.unmodifiableSet(new HashSet<ReferenceDescriptor>());
    private static final Set<CollectionDescriptor> noColls =
            Collections.unmodifiableSet(new HashSet<CollectionDescriptor>());

    public FieldDescriptorTest(String arg) {
        super(arg);
    }

    public void testConstructorNullName() throws Exception {
        try {
            new TestFieldDescriptor(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorEmptylName() throws Exception {
        try {
            new TestFieldDescriptor("");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSetClassDescriptorNull() throws Exception {
        FieldDescriptor fd = new TestFieldDescriptor("name");
        try {
            fd.setClassDescriptor(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    private ClassDescriptor makeClass(String name) {
        return new ClassDescriptor("Class1", null, false, noAttrs, noRefs, noColls);
    }

    public void testSetClassDescriptorValid() throws Exception {
        FieldDescriptor fd = new TestFieldDescriptor("name");
        ClassDescriptor cld = makeClass("Class1");
        try {
            fd.setClassDescriptor(cld);
        } catch (IllegalStateException e) {
            fail("Unable to set ClassDescriptor");
        }
    }

    public void testSetClassDescriptorTwice() throws Exception {
        FieldDescriptor fd = new TestFieldDescriptor("name");
        ClassDescriptor cld = makeClass("Class1");
        fd.setClassDescriptor(cld);
        try {
            fd.setClassDescriptor(cld);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }

    private class TestFieldDescriptor extends FieldDescriptor {
        public TestFieldDescriptor(String name) {
            super(name);
        }

        public int relationType() {
            return FieldDescriptor.NOT_RELATION;
        }

        @Override
        public String toJSONString() {
            return null;
        }
    }
}
