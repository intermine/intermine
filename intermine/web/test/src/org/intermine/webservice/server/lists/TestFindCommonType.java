package org.intermine.webservice.server.lists;

import java.util.HashSet;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.webservice.server.exceptions.BadRequestException;

import junit.framework.TestCase;

public class TestFindCommonType extends TestCase {

    private final static Model testModel = Model.getInstanceByName("testmodel");
    private Set<ClassDescriptor> classes;

    protected void setUp() throws Exception {
        super.setUp();
        classes = new HashSet<ClassDescriptor>();
    }
    
    public void testSingleMember() {
        classes.add(testModel.getClassDescriptorByName("Employee"));
        String common = ListServiceUtils.findCommonSuperTypeOf(classes);
        assertEquals(common, "Employee");
    }
    
    public void testNull() {
        classes = null;
        try {
            ListServiceUtils.findCommonSuperTypeOf(classes);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
            // Expected behaviour.
        }
    }
    
    public void testEmptySet() {
        try {
            ListServiceUtils.findCommonSuperTypeOf(classes);
            fail("No exception thrown");
        } catch (RuntimeException e) {
            // Expected behaviour.
        }
    }
    
    public void testIncompatibleTypes() {
        classes.add(testModel.getClassDescriptorByName("Employee"));
        classes.add(testModel.getClassDescriptorByName("Department"));
        try {
            String common = ListServiceUtils.findCommonSuperTypeOf(classes);
            fail("No exception thrown: " + common);
        } catch (BadRequestException e) {
            // Expected behaviour.
        }
    }
    
    public void testDirectSubclass() {
        classes.add(testModel.getClassDescriptorByName("Employee"));
        classes.add(testModel.getClassDescriptorByName("Manager"));
        String common = ListServiceUtils.findCommonSuperTypeOf(classes);
        assertEquals(common, "Employee");
    }
    
    public void testDistantSubclass() {
        classes.add(testModel.getClassDescriptorByName("Employee"));
        classes.add(testModel.getClassDescriptorByName("CEO"));
        String common = ListServiceUtils.findCommonSuperTypeOf(classes);
        assertEquals(common, "Employee");
    }
    
    public void testUnmentionedCommonType() {
        classes.add(testModel.getClassDescriptorByName("Department"));
        classes.add(testModel.getClassDescriptorByName("Company"));
        String common = ListServiceUtils.findCommonSuperTypeOf(classes);
        assertEquals(common, "RandomInterface");
    }
    
    public void testMostSpecificType() {
        classes.add(testModel.getClassDescriptorByName("Employee"));
        classes.add(testModel.getClassDescriptorByName("Manager"));
        classes.add(testModel.getClassDescriptorByName("CEO"));
        String common = ListServiceUtils.findMostSpecificCommonTypeOf(classes);
        assertEquals(common, "CEO");
    }
    
    public void testCousinsSpecificType() {
        classes.add(testModel.getClassDescriptorByName("HasAddress"));
        classes.add(testModel.getClassDescriptorByName("Employee"));
        classes.add(testModel.getClassDescriptorByName("Company"));
        String common = ListServiceUtils.findMostSpecificCommonTypeOf(classes);
        assertNull(common);
    }


}
