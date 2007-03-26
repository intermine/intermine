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

import java.util.Collections;
import java.util.Set;

public class FieldDescriptorTest extends TestCase
{
    private static final Set EMPTY_SET = Collections.EMPTY_SET;

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

    public void testSetClassDescriptorValid() throws Exception {
        FieldDescriptor fd = new TestFieldDescriptor("name");
        ClassDescriptor cld = new ClassDescriptor("Class1", null, false,
                                                  EMPTY_SET, EMPTY_SET, EMPTY_SET);
        try {
            fd.setClassDescriptor(cld);
        } catch (IllegalStateException e) {
            fail("Unable to set ClassDescriptor");
        }
    }

    public void testSetClassDescriptorTwice() throws Exception {
        FieldDescriptor fd = new TestFieldDescriptor("name");
        ClassDescriptor cld = new ClassDescriptor("Class1", null, false,
                                                  EMPTY_SET, EMPTY_SET, EMPTY_SET);
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
    }
}
