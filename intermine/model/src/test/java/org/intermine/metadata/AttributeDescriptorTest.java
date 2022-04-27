package org.intermine.metadata;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

public class AttributeDescriptorTest extends TestCase {
    public AttributeDescriptorTest(String arg) {
        super(arg);
    }

    public void testConstructorNullName() throws Exception {
        try {
            new AttributeDescriptor(null, "int", null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorEmptyName() throws Exception {
        try {
            new AttributeDescriptor("", "int", null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorNullType() throws Exception {
        try {
            new AttributeDescriptor("name", null, null);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorEmptyType() throws Exception {
        try {
            new AttributeDescriptor("name", "", null);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testRelationType() throws Exception {
        AttributeDescriptor attr = new AttributeDescriptor("name", "int", null);
        assertEquals(FieldDescriptor.NOT_RELATION, attr.relationType());
    }

    public void testEquals() throws Exception {
        AttributeDescriptor attr1 = new AttributeDescriptor("name1", "int", null);
        AttributeDescriptor attr2 = new AttributeDescriptor("name1", "int", null);
        AttributeDescriptor attr3 = new AttributeDescriptor("name2", "int", null);
        AttributeDescriptor attr4 = new AttributeDescriptor("name1", "float", null);
        assertEquals(attr1, attr2);
        assertEquals(attr1.hashCode(), attr2.hashCode());
        assertFalse(attr1.equals(attr3));
        assertFalse(attr1.equals(attr4));
    }

    public void testToString() throws Exception {
        AttributeDescriptor attr = new AttributeDescriptor("attr", "int", null);
        String expected = "<attribute name=\"attr\" type=\"int\"/>";
        assertEquals(expected, attr.toString());

        attr = new AttributeDescriptor("myAttribute", "java.lang.String", "myTerm");
        expected = "<attribute name=\"myAttribute\" type=\"java.lang.String\" term=\"myTerm\"/>";
        assertEquals(expected, attr.toString());
    }
}