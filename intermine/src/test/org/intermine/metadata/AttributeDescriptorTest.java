package org.flymine.metadata;

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
            new AttributeDescriptor(null, true, "int");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorEmptyName() throws Exception {
        try {
            new AttributeDescriptor("", true, "int");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorNullType() throws Exception {
        try {
            new AttributeDescriptor("name", true, null);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorEmptyType() throws Exception {
        try {
            new AttributeDescriptor("name", true, "");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testRelationType() throws Exception {
        AttributeDescriptor attr = new AttributeDescriptor("name", true, "int");
        assertEquals(FieldDescriptor.NOT_RELATION, attr.relationType());
    }

    public void testEquals() throws Exception {
        AttributeDescriptor attr1 = new AttributeDescriptor("name1", true, "int");
        AttributeDescriptor attr2 = new AttributeDescriptor("name1", true, "int");
        AttributeDescriptor attr3 = new AttributeDescriptor("name2", true, "int");
        AttributeDescriptor attr4 = new AttributeDescriptor("name1", false, "int");
        AttributeDescriptor attr5 = new AttributeDescriptor("name1", true, "float");
        assertEquals(attr1, attr2);
        assertEquals(attr1.hashCode(), attr2.hashCode());
        assertFalse(attr1.equals(attr3));
        assertFalse(attr1.equals(attr4));
        assertFalse(attr1.equals(attr5));
    }
    
    public void testToString() throws Exception {
        AttributeDescriptor attr = new AttributeDescriptor("attr", true, "int");
        String expected = "<attribute name=\"attr\" type=\"int\" primary-key=\"true\"/>";
        assertEquals(expected, attr.toString());
    }
}
