package org.flymine.metadata;

import junit.framework.TestCase;

import java.util.ArrayList;

public class AttributeDescriptorTest extends TestCase {

    public AttributeDescriptorTest(String arg) {
        super(arg);
    }

    public void setUp() {
    }

    public void tearDown() {
    }


    public void testNullConstructorFields() throws Exception {
        try {
            AttributeDescriptor atd = new AttributeDescriptor(null, true, "String");
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            AttributeDescriptor atd = new AttributeDescriptor("", true, "String");
            fail("Expected: IllegalArgumentException, name parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            AttributeDescriptor atd = new AttributeDescriptor("name", true, null);
            fail("Expected: IllegalArgumentException, type parameter");
        } catch (IllegalArgumentException e) {
        }

        try {
            AttributeDescriptor atd = new AttributeDescriptor("name", true, "");
            fail("Expected: IllegalArgumentException, type parameter");
        } catch (IllegalArgumentException e) {
        }

    }


    public void testSetClassDescriptor() throws Exception {

        ClassDescriptor cld = new ClassDescriptor("Class1", null, null, false,
                                                  new ArrayList(), new ArrayList(), new ArrayList());

        AttributeDescriptor atd = new AttributeDescriptor("name", true, "String");
        try {
            atd.setClassDescriptor(cld);
        } catch (IllegalStateException e) {
            fail("should have been able set ClassDescriptor");
        }

        try {
            atd.setClassDescriptor(cld);
            fail("Expected: IllegalStateException, ClassDescriptor already set");
        } catch (IllegalStateException e) {
        }
    }

}
