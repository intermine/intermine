package org.intermine.metadata;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.intermine.model.testmodel.SimpleObject;

public class ModelTest extends TestCase
{
    private static final Set EMPTY_SET = Collections.EMPTY_SET;
    protected static final String ENDL = System.getProperty("line.separator");
    protected static final String INDENT = ENDL + "\t";
    public ModelTest(String arg) {
        super(arg);
    }

    public void testGetInstanceByWrongName() throws Exception {
        try {
            Model.getInstanceByName("wrong_name");
            fail("Expected IllegalArgumentException, wrong model name");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetInstanceByNameSameInstance() throws Exception {
        Model model1 = Model.getInstanceByName("testmodel");
        Model model2 = Model.getInstanceByName("testmodel");
        assertTrue(model1 == model2);
    }

    public void testContructNullArguments() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false,
                new HashSet<AttributeDescriptor>(),
                new HashSet<ReferenceDescriptor>(),
                new HashSet<CollectionDescriptor>());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false,
                new HashSet<AttributeDescriptor>(),
                new HashSet<ReferenceDescriptor>(),
                new HashSet<CollectionDescriptor>());
        Set<ClassDescriptor> clds = new HashSet<ClassDescriptor>(Arrays.asList(new ClassDescriptor[] {cld1, cld2}));

        try {
            new Model(null, "package.name", clds);
            fail("Expected NullPointerException, name was null");
        } catch(NullPointerException e) {
        }

        try {
            new Model("", "package.name", clds);
            fail("Expected IllegalArgumentException, name was empty string");
        } catch(IllegalArgumentException e) {
        }

        try {
            new Model("model", "package.name", null);
            fail("Expected NullPointerException, name was null");
        } catch(NullPointerException e) {
        }
    }

    public void testGetDirectSubs() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        Set<ClassDescriptor> hasAddressCds =
            model.getClassDescriptorsForClass(org.intermine.model.testmodel.HasAddress.class);
        assertEquals(2, hasAddressCds.size());

        ClassDescriptor addressCld = (ClassDescriptor) hasAddressCds.iterator().next();

        if (addressCld.getName() == "org.intermine.model.InterMineObject") {
            // we want org.intermine.model.testmodel.HasAddress
            addressCld = (ClassDescriptor) hasAddressCds.iterator().next();
        }

        Set<ClassDescriptor> resultCds = model.getDirectSubs(addressCld);
        Set<String> expectedCdNames = new HashSet<String>();
        expectedCdNames.add("org.intermine.model.testmodel.Employee");
        expectedCdNames.add("org.intermine.model.testmodel.Company");
        Set<String> resultCdNames = new HashSet<String>();
        for (ClassDescriptor cld : resultCds) {
            resultCdNames.add(cld.getName());
        }
        assertEquals(expectedCdNames, resultCdNames);
    }

    public void testGetAllSubs() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        Set<ClassDescriptor> hasAddressCds =
            model.getClassDescriptorsForClass(org.intermine.model.testmodel.HasAddress.class);
        assertEquals(2, hasAddressCds.size());

        ClassDescriptor addressCld = (ClassDescriptor) hasAddressCds.iterator().next();

        if (addressCld.getName() == "org.intermine.model.InterMineObject") {
            // we want org.intermine.model.testmodel.HasAddress
            addressCld = (ClassDescriptor) hasAddressCds.iterator().next();
        }

        Set<ClassDescriptor> resultCds = model.getAllSubs(addressCld);
        Set<String> expectedCdNames = new HashSet<String>();
        expectedCdNames.add("org.intermine.model.testmodel.Company");
        expectedCdNames.add("org.intermine.model.testmodel.Employee");
        expectedCdNames.add("org.intermine.model.testmodel.Manager");
        expectedCdNames.add("org.intermine.model.testmodel.CEO");
        Set<String> resultCdNames = new HashSet<String>();
        for (ClassDescriptor cld : resultCds) {
            resultCdNames.add(cld.getName());
        }
        assertEquals(expectedCdNames, resultCdNames);
    }

    public void testGetClassDescriptorByName() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Set<ClassDescriptor> clds = new HashSet<ClassDescriptor>(Arrays.asList(new ClassDescriptor[] {cld1, cld2}));
        Model model = new Model("model", "package.name", clds);

        assertEquals(cld1, model.getClassDescriptorByName("Class1"));
        assertEquals(cld2, model.getClassDescriptorByName("Class2"));
    }

    public void testGetCDByNameQualified() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertNotNull(model.getClassDescriptorByName("org.intermine.model.testmodel.Company"));
    }

    public void testGetCDByNameUnqualified() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertNotNull(model.getClassDescriptorByName("Company"));
    }

    public void testGetClassDescriptorByWrongName() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Set<ClassDescriptor> clds = new HashSet<ClassDescriptor>(Arrays.asList(new ClassDescriptor[] {cld1, cld2}));
        Model model = new Model("model", "package.name", clds);

        assertTrue(null == model.getClassDescriptorByName("WrongName"));
    }

    public void testGetClassDescriptorsForClass() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        Set cds = model.getClassDescriptorsForClass(org.intermine.model.testmodel.CEO.class);
        Set expectedCdNames = new HashSet();
        expectedCdNames.add("org.intermine.model.testmodel.ImportantPerson");
        expectedCdNames.add("org.intermine.model.testmodel.Employable");
        expectedCdNames.add("org.intermine.model.testmodel.HasAddress");
        expectedCdNames.add("org.intermine.model.testmodel.CEO");
        expectedCdNames.add("org.intermine.model.testmodel.Employee");
        expectedCdNames.add("org.intermine.model.testmodel.Thing");
        expectedCdNames.add("org.intermine.model.InterMineObject");
        expectedCdNames.add("org.intermine.model.testmodel.HasSecretarys");
        expectedCdNames.add("org.intermine.model.testmodel.Manager");
        Set cdNames = new HashSet();
        for (Iterator iter = cds.iterator(); iter.hasNext(); ) {
            cdNames.add(((ClassDescriptor) iter.next()).getName());
        }
        assertEquals(expectedCdNames, cdNames);
    }

    public void testEquals() throws Exception {
        Model m1 = new Model("flibble", "package.name", EMPTY_SET);
        Model m2 = new Model("flibble", "package.name", EMPTY_SET);
        Model m3 = new Model("flobble", "package.name", EMPTY_SET);
        Model m4 = new Model("flibble", "package.name", Collections.singleton(new ClassDescriptor("package.name.Class1", null, true, EMPTY_SET, EMPTY_SET, EMPTY_SET)));

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertFalse(m1.equals(m3));
        assertTrue(!m1.equals(m3));
        assertTrue(!m1.equals(m4));
    }

    public void testToString() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Set clds = new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("model", "package.name", clds);

        String expected = "<model name=\"model\" package=\"package.name\">" + ENDL
            + "<class name=\"Class1\" is-interface=\"false\"></class>" + ENDL
            + "<class name=\"Class2\" is-interface=\"false\"></class>" + ENDL
            + "</model>";

        assertEquals(expected, model.toString());
    }

    // test that we end up with BaseClass < MidClass < SubClass from BaseClass < SubClass,
    // MidClass < SubClass and BaseClass < MidClass
    public void testRemoveRedundantClasses() throws Exception {
        String baseName = "org.intermine.model.testmodel.BaseClass";
        String subName = "org.intermine.model.testmodel.SubClass";
        String midName = "org.intermine.model.testmodel.MidClass";
        ClassDescriptor base = new ClassDescriptor(baseName, null, true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor sub = new ClassDescriptor(subName, "org.intermine.model.testmodel.BaseClass org.intermine.model.testmodel.MidClass", true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor mid = new ClassDescriptor(midName, baseName, true, new HashSet(), new HashSet(), new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] {base, mid, sub}));
        Model model = new Model("model", "org.intermine.model.testmodel", clds);

        ClassDescriptor subCD = model.getClassDescriptorByName(subName);

        assertEquals(1, subCD.getSuperclassNames().size());
        assertEquals(midName, subCD.getSuperclassNames().iterator().next());

    }

    public void testCircularDependencyFail() throws Exception {
        ClassDescriptor c1 = new ClassDescriptor("org.intermine.model.testmodel.Class1", "org.intermine.model.testmodel.Class2", true, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor c2 = new ClassDescriptor("org.intermine.model.testmodel.Class2", "org.intermine.model.testmodel.Class1", true, new HashSet(), new HashSet(), new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] {c1, c2}));
        try {
            Model model = new Model("model", "org.intermine.model.testmodel", clds);
            fail("expected exception");
        } catch (MetaDataException e) {
            // expected
        }
    }

    public void testFieldsInNonInterMineObject() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        ClassDescriptor cld = model.getClassDescriptorByName("SimpleObject");
        assertEquals(2, cld.getAllFieldDescriptors().size());
        assertEquals(new HashSet(Arrays.asList("employee", "name")), model.getFieldDescriptorsForClass(SimpleObject.class).keySet());
    }

    public void testGetQualifiedTypeName() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertEquals("org.intermine.model.testmodel.Employee",
                     model.getQualifiedTypeName("Employee"));
        assertEquals("java.lang.String",
                     model.getQualifiedTypeName("String"));
        assertEquals("int",
                     model.getQualifiedTypeName("int"));
        assertEquals("java.util.Date",
                     model.getQualifiedTypeName("Date"));
        assertEquals("java.math.BigDecimal",
                     model.getQualifiedTypeName("BigDecimal"));

        try {
            model.getQualifiedTypeName("SomeUnkownClass");
            fail("Expected ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected
        }

        try {
            model.getQualifiedTypeName("java.lang.String");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testToString3() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        InputStream is = getClass().getResourceAsStream("expected_model.xml");

        String modelString = IOUtils.toString(is).replaceAll("\n$", "");
        assertEquals(modelString, model.toString().replaceAll("\t", "    "));
    }

    public void testGetSimpleObjectClassDescriptors() throws Exception {
        Model simpleObjectModel = getSimpleObjectModel();

        ClassDescriptor simple = simpleObjectModel.getClassDescriptorByName("Simple");
        Set<ClassDescriptor> expected = new HashSet<ClassDescriptor>(Collections.singleton(simple));
        assertEquals(expected, simpleObjectModel.getSimpleObjectClassDescriptors());
    }

    public void testGetTopDownTraversal() throws Exception {
        Model model = Model.getInstanceByName("testmodel");

        /**
         *         A
         *       /   \
         *      B     G
         *     / \    /
         *    C   E  /
         *   /     \/
         *  D      F
         *
         * Expected level order: [A], [B, G], [C, E, F], [D]
         * F should be in third level which is it's most shallow position
         */

        Model smallModel = getSmallModel();

        ClassDescriptor im = model.getClassDescriptorByName("org.intermine.model.InterMineObject");
        // the order of nodes at any give level is undefined, so check by level
        List<ClassDescriptor> actual = smallModel.getTopDownLevelTraversal();

        // level zero
        assertEquals(im, actual.get(0));

        // level one
        assertEquals(smallModel.getClassDescriptorByName("A"), actual.get(1));

        // level two
        Set<ClassDescriptor> expected = new HashSet<ClassDescriptor>();
        expected.add(smallModel.getClassDescriptorByName("B"));
        expected.add(smallModel.getClassDescriptorByName("G"));
        assertEquals(expected, new HashSet<ClassDescriptor>(actual.subList(2, 4)));

        // level two
        expected = new HashSet<ClassDescriptor>();
        expected.add(smallModel.getClassDescriptorByName("C"));
        expected.add(smallModel.getClassDescriptorByName("E"));
        expected.add(smallModel.getClassDescriptorByName("F"));
        assertEquals(expected, new HashSet<ClassDescriptor>(actual.subList(4, 7)));

        // level four
        assertEquals(smallModel.getClassDescriptorByName("D"), actual.get(7));
    }

    public void testGetBottomUpTraversal() throws Exception {
        Model model = Model.getInstanceByName("testmodel");

        /**
         *         A
         *       /   \
         *      B     G
         *     / \    /
         *    C   E  /
         *   /     \/
         *  D      F
         *
         * Expected level order: [D], [C, E, F], [B, G], [A]
         * F should be in third level which is it's most shallow position
         */

        Model smallModel = getSmallModel();

        ClassDescriptor im = model.getClassDescriptorByName("org.intermine.model.InterMineObject");

        // the order of nodes at any give level is undefined, so check by level
        List<ClassDescriptor> actual = smallModel.getBottomUpLevelTraversal();

        // level zero
        assertEquals(smallModel.getClassDescriptorByName("D"), actual.get(0));

        // level one
        Set<ClassDescriptor> expected = new HashSet<ClassDescriptor>();
        expected.add(smallModel.getClassDescriptorByName("C"));
        expected.add(smallModel.getClassDescriptorByName("E"));
        expected.add(smallModel.getClassDescriptorByName("F"));
        assertEquals(expected, new HashSet<ClassDescriptor>(actual.subList(1, 4)));

        // level two
        expected = new HashSet<ClassDescriptor>();
        expected.add(smallModel.getClassDescriptorByName("B"));
        expected.add(smallModel.getClassDescriptorByName("G"));
        assertEquals(expected, new HashSet<ClassDescriptor>(actual.subList(4, 6)));

        // level three
        assertEquals(smallModel.getClassDescriptorByName("A"), actual.get(6));

        // level four
        assertEquals(im, actual.get(7));
    }


    // simple objects come before InterMineObject in traversal order, they have no inheritance
    public void testGetTopDownTraversalSimpleObjects() throws Exception {
        Model simpleObjectModel = getSimpleObjectModel();

        List<ClassDescriptor> expected = new ArrayList<ClassDescriptor>();
        expected.add(simpleObjectModel.getClassDescriptorByName("Simple"));
        expected.add(simpleObjectModel.getClassDescriptorByName("org.intermine.model.InterMineObject"));
        expected.add(simpleObjectModel.getClassDescriptorByName("A"));
        expected.add(simpleObjectModel.getClassDescriptorByName("B"));

        assertEquals(expected, simpleObjectModel.getTopDownLevelTraversal());
    }

    /**
     * Return a model with inheritance structure:
     *
     *  InterMineObject
     *         |
     *         A
     *       /   \
     *      B     G
     *     / \    /
     *    C   E  /
     *   /     \/
     *  D      F
     */
    private Model getSmallModel() throws MetaDataException {
        Set<ClassDescriptor> smallModelClds = new HashSet<ClassDescriptor>();
        ClassDescriptor a = new ClassDescriptor("A", null, true, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        smallModelClds.add(a);
        ClassDescriptor b = new ClassDescriptor("B", "A", true, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        smallModelClds.add(b);
        ClassDescriptor c = new ClassDescriptor("C", "B", true, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        smallModelClds.add(c);
        ClassDescriptor d = new ClassDescriptor("D", "C", true, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        smallModelClds.add(d);
        ClassDescriptor e = new ClassDescriptor("E", "B", true, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        smallModelClds.add(e);
        ClassDescriptor f = new ClassDescriptor("F", "G E", true, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        smallModelClds.add(f);
        ClassDescriptor g = new ClassDescriptor("G", "A", true, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        smallModelClds.add(g);

        Model smallModel = new Model("small", "", smallModelClds);
        return smallModel;
    }

    private Model getSimpleObjectModel() throws MetaDataException {
        Set<ClassDescriptor> clds = new HashSet<ClassDescriptor>();
        ClassDescriptor a = new ClassDescriptor("A", null, true, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        clds.add(a);
        ClassDescriptor b = new ClassDescriptor("B", "A", true, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        clds.add(b);
        ClassDescriptor s = new ClassDescriptor("Simple", "java.lang.Object", false, EMPTY_SET, EMPTY_SET, EMPTY_SET);
        clds.add(s);

        Model simpleObjectModel = new Model("simple", "", clds);
        return simpleObjectModel;
    }

}
