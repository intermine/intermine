package org.intermine.metadata;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class DescriptorUtilsTest {

    private final static Model testModel = Model.getInstanceByName("testmodel");
    private Set<ClassDescriptor> classes;

    @Before
    public void setUp() throws Exception {
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

    @Test
    public void testSingleMember() throws MetaDataException {
        withClass("Employee");
        String common = DescriptorUtils.findSumType(classes).getUnqualifiedName();
        assertEquals(common, "Employee");
    }

    @Test
    public void testNull() throws MetaDataException {
        classes = null;
        try {
            DescriptorUtils.findSumType(classes);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
            // Expected behaviour.
        }
    }

    @Test
    public void testEmptySet() {
        try {
            DescriptorUtils.findSumType(classes);
            fail("No exception thrown");
        } catch (MetaDataException e) {
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
    @Test
    public void testIncompatibleTypes() {
        withClasses("Employee", "Department");
        try {
            String common = DescriptorUtils.findSumType(classes).getUnqualifiedName();
            fail("No exception thrown: " + common);
        } catch (MetaDataException e) {
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
    @Test
    public void testDirectSubclass() throws MetaDataException {
        withClasses("Employee", "Manager");
        String common = DescriptorUtils.findSumType(classes).getUnqualifiedName();
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
    @Test
    public void testDistantSubclass() throws MetaDataException {
        withClasses("Employee", "CEO");
        String common = DescriptorUtils.findSumType(classes).getUnqualifiedName();
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
    @Test
    public void testCommonCousins() throws MetaDataException {
        withClasses("Department", "Company");
        String common = DescriptorUtils.findSumType(classes).getUnqualifiedName();
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
    @Test
    public void testMostSpecificType() throws MetaDataException {
        withClasses("Employee", "Manager", "CEO");
        String common = DescriptorUtils.findIntersectionType(classes).getUnqualifiedName();
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
    @Test
    public void testExtendedFamilySpecificType() throws MetaDataException {
        withClasses("HasAddress", "Employee", "Manager", "Company");
        String common = DescriptorUtils.findIntersectionType(classes).getUnqualifiedName();
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
    @Test
    public void testCousinsSpecificType() throws MetaDataException {
        withClasses("Employee", "Manager", "Company");
        String common = DescriptorUtils.findIntersectionType(classes).getUnqualifiedName();
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
    @Test
    public void testClansSpecificType() throws MetaDataException {
        withClasses("Contractor", "Manager", "CEO", "Address");
        String common = DescriptorUtils.findIntersectionType(classes).getUnqualifiedName();
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
    @Test
    public void testBranchingTreeSpecificType() throws MetaDataException {
        withClasses("Thing", "Employable", "Employee", "Contractor");
        String common = DescriptorUtils.findIntersectionType(classes).getUnqualifiedName();
        assertEquals("Employable", common);
    }
}
