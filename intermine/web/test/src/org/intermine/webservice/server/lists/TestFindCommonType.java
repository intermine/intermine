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

    private void withClass(String name) {
        classes.add(testModel.getClassDescriptorByName(name));
    }

    private void withClasses(String... names) {
        for (String name: names) {
            withClass(name);
        }
    }

    public void testSingleMember() {
        withClass("Employee");
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

    /**
     * Test that exceptions are thrown for unconnected types.
     * 
     * ie:
     * <pre>
     *     {BOOM}
     *     |    |
     *     A    B
     * </pre>
     */
    public void testIncompatibleTypes() {
        withClasses("Employee", "Department");
        try {
            String common = ListServiceUtils.findCommonSuperTypeOf(classes);
            fail("No exception thrown: " + common);
        } catch (BadRequestException e) {
            // Expected behaviour.
        }
    }

    /**
     * Test that the common type of parent and child is
     * the ancestor.
     * 
     * ie:
     * <pre>
     *     [A]
     *       \
     *        B
     * </pre>
     */
    public void testDirectSubclass() {
        withClasses("Employee", "Manager");
        String common = ListServiceUtils.findCommonSuperTypeOf(classes);
        assertEquals(common, "Employee");
    }

    /**
     * Test that the common type of grandparent and grand-child is
     * the ancestor.
     * 
     * ie:
     * <pre>
     *     [A]
     *       \
     *        ?
     *         \
     *          C
     * </pre>
     */
    public void testDistantSubclass() {
        withClasses("Employee", "CEO");
        String common = ListServiceUtils.findCommonSuperTypeOf(classes);
        assertEquals(common, "Employee");
    }

    /**
     * Test that the common type of cousins is the grandparent
     * 
     * ie:
     * <pre>
     *          [?]
     *         /   \
     *        B     C
     * </pre>
     */
    public void testCommonCousins() {
        withClasses("Department", "Company");
        String common = ListServiceUtils.findCommonSuperTypeOf(classes);
        assertEquals(common, "RandomInterface");
    }


    /**
     * Test that the most specific type of a lineage is the descendent.
     * 
     * ie:
     * <pre>
     *      A
     *       \
     *        B
     *         \
     *         [C]
     * </pre>
     */
    public void testMostSpecificType() {
        withClasses("Employee", "Manager", "CEO");
        String common = ListServiceUtils.findMostSpecificCommonTypeOf(classes);
        assertEquals(common, "CEO");
    }

    /**
     * Test that the most specific common type of an extended family
     * is the grandparent.
     * 
     * ie:
     * <pre>
     *          [A]
     *         /   \
     *        B     C
     *               \
     *                D
     * </pre>
     */
    public void testExtendedFamilySpecificType() {
        withClasses("HasAddress", "Employee", "Manager", "Company");
        String common = ListServiceUtils.findMostSpecificCommonTypeOf(classes);
        assertEquals("HasAddress", common);
    }

    /**
     * Test that the most specific common type of cousins is the grandparent
     * 
     * ie:
     * <pre>
     *          [?]
     *         /   \
     *        B     C
     *               \
     *                D
     * </pre>
     */
    public void testCousinsSpecificType() {
        withClasses("Employee", "Manager", "Company");
        String common = ListServiceUtils.findMostSpecificCommonTypeOf(classes);
        assertEquals("HasAddress", common);
    }

    /**
     * Test that the most specific common type of cousins is the grandparent
     * 
     * ie:
     * <pre>
     *           [?]
     *         /  |  \
     *        B   C   E
     *            |
     *            D
     * </pre>
     */
    public void testClansSpecificType() {
        withClasses("Contractor", "Manager", "CEO", "Address");
        String common = ListServiceUtils.findMostSpecificCommonTypeOf(classes);
        assertEquals("Thing", common);
    }

    /**
     * Test that the most specific common type of a branching tree is the root.
     * 
     * ie:
     * <pre>
     *           A
     *           |
     *          [B]
     *         /   \
     *        C     D
     * </pre>
     */
    public void testBranchingTreeSpecificType() {
        withClasses("Thing", "Employable", "Employee", "Contractor");
        String common = ListServiceUtils.findMostSpecificCommonTypeOf(classes);
        assertEquals("Employable", common);
    }

}
