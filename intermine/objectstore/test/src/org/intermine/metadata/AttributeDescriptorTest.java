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

public class AttributeDescriptorTest extends TestCase
{
    private static final Set EMPTY_SET = Collections.EMPTY_SET;
    
    public AttributeDescriptorTest(String arg) {
        super(arg);
    }

    public void testConstructorNullName() throws Exception {
        try {
            new AttributeDescriptor(null, "int");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorEmptyName() throws Exception {
        try {
            new AttributeDescriptor("", "int");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorNullType() throws Exception {
        try {
            new AttributeDescriptor("name", null);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorEmptyType() throws Exception {
        try {
            new AttributeDescriptor("name", "");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testRelationType() throws Exception {
        AttributeDescriptor attr = new AttributeDescriptor("name", "int");
        assertEquals(FieldDescriptor.NOT_RELATION, attr.relationType());
    }

    public void testEquals() throws Exception {
        AttributeDescriptor attr1 = new AttributeDescriptor("name1", "int");
        AttributeDescriptor attr2 = new AttributeDescriptor("name1", "int");
        AttributeDescriptor attr3 = new AttributeDescriptor("name2", "int");
        AttributeDescriptor attr4 = new AttributeDescriptor("name1", "float");
        assertEquals(attr1, attr2);
        assertEquals(attr1.hashCode(), attr2.hashCode());
        assertFalse(attr1.equals(attr3));
        assertFalse(attr1.equals(attr4));
    }
    
    public void testToString() throws Exception {
        AttributeDescriptor attr = new AttributeDescriptor("attr", "int");
        String expected = "<attribute name=\"attr\" type=\"int\"/>";
        assertEquals(expected, attr.toString());
    }
}
