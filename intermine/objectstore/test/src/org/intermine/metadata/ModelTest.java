package org.intermine.metadata;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

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
            Model model = Model.getInstanceByName("wrong_name");
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
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));

        try {
            Model model = new Model(null, "package.name", clds);
            fail("Expected NullPointerException, name was null");
        } catch(NullPointerException e) {
        }

        try {
            Model model = new Model("", "package.name", clds);
            fail("Expected IllegalArgumentException, name was empty string");
        } catch(IllegalArgumentException e) {
        }

        try {
            Model model = new Model("model", "package.name", null);
            fail("Expected NullPointerException, name was null");
        } catch(NullPointerException e) {
        }
    }

    public void testGetDirectSubs() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        Set hasAddressCds =
            model.getClassDescriptorsForClass(org.intermine.model.testmodel.HasAddress.class);
        assertEquals(2, hasAddressCds.size());

        ClassDescriptor addressCld = (ClassDescriptor) hasAddressCds.iterator().next();

        if (addressCld.getName() == "org.intermine.model.InterMineObject") {
            // we want org.intermine.model.testmodel.HasAddress
            addressCld = (ClassDescriptor) hasAddressCds.iterator().next();
        }

        Set resultCds = model.getDirectSubs(addressCld);
        Set expectedCdNames = new HashSet();
        expectedCdNames.add("org.intermine.model.testmodel.Employee");
        expectedCdNames.add("org.intermine.model.testmodel.Company");
        Set resultCdNames = new HashSet();
        for (Iterator iter = resultCds.iterator(); iter.hasNext(); ) {
            resultCdNames.add(((ClassDescriptor) iter.next()).getName());
        }
        assertEquals(expectedCdNames, resultCdNames);
    }

    public void testGetAllSubs() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        Set hasAddressCds =
            model.getClassDescriptorsForClass(org.intermine.model.testmodel.HasAddress.class);
        assertEquals(2, hasAddressCds.size());

        ClassDescriptor addressCld = (ClassDescriptor) hasAddressCds.iterator().next();

        if (addressCld.getName() == "org.intermine.model.InterMineObject") {
            // we want org.intermine.model.testmodel.HasAddress
            addressCld = (ClassDescriptor) hasAddressCds.iterator().next();
        }

        Set resultCds = model.getAllSubs(addressCld);
        Set expectedCdNames = new HashSet();
        expectedCdNames.add("org.intermine.model.testmodel.Company");
        expectedCdNames.add("org.intermine.model.testmodel.Employee");
        expectedCdNames.add("org.intermine.model.testmodel.Manager");
        expectedCdNames.add("org.intermine.model.testmodel.CEO");
        Set resultCdNames = new HashSet();
        for (Iterator iter = resultCds.iterator(); iter.hasNext(); ) {
            resultCdNames.add(((ClassDescriptor) iter.next()).getName());
        }
        assertEquals(expectedCdNames, resultCdNames);
    }

    public void testGetClassDescriptorByName() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), new HashSet(), new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
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
        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
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
        String modelString ="<model name=\"testmodel\" package=\"org.intermine.model.testmodel\">" + ENDL +
        "<class name=\"Broke\" is-interface=\"true\">" + INDENT + "<attribute name=\"debt\" type=\"int\"/>" + INDENT + "<reference name=\"bank\" referenced-type=\"Bank\" reverse-reference=\"debtors\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Thing\" is-interface=\"true\"></class>" + ENDL +
        "<class name=\"Employable\" extends=\"Thing\" is-interface=\"true\">" + INDENT + "<attribute name=\"name\" type=\"java.lang.String\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"HasAddress\" is-interface=\"true\">" + INDENT + "<reference name=\"address\" referenced-type=\"Address\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"HasSecretarys\" is-interface=\"true\">" + INDENT + "<collection name=\"secretarys\" referenced-type=\"Secretary\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Contractor\" extends=\"Employable ImportantPerson\" is-interface=\"false\">" + INDENT + "<reference name=\"personalAddress\" referenced-type=\"Address\"/>" + INDENT + "<reference name=\"businessAddress\" referenced-type=\"Address\"/>" + INDENT + "<collection name=\"companys\" referenced-type=\"Company\" reverse-reference=\"contractors\"/>" + INDENT + "<collection name=\"oldComs\" referenced-type=\"Company\" reverse-reference=\"oldContracts\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Manager\" extends=\"Employee ImportantPerson\" is-interface=\"false\">" + INDENT + "<attribute name=\"title\" type=\"java.lang.String\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Employee\" extends=\"Employable HasAddress\" is-interface=\"false\">" + INDENT + "<attribute name=\"fullTime\" type=\"boolean\"/>" + INDENT + "<attribute name=\"age\" type=\"int\"/>" + INDENT + "<attribute name=\"end\" type=\"java.lang.String\"/>" + INDENT + "<reference name=\"department\" referenced-type=\"Department\" reverse-reference=\"employees\"/>" + INDENT + "<reference name=\"departmentThatRejectedMe\" referenced-type=\"Department\" reverse-reference=\"rejectedEmployee\"/>" + INDENT + "<collection name=\"simpleObjects\" referenced-type=\"SimpleObject\" reverse-reference=\"employee\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Department\" extends=\"RandomInterface\" is-interface=\"false\">" + INDENT + "<attribute name=\"name\" type=\"java.lang.String\"/>" + INDENT + "<reference name=\"company\" referenced-type=\"Company\" reverse-reference=\"departments\"/>" + INDENT + "<reference name=\"manager\" referenced-type=\"Manager\"/>" + INDENT + "<collection name=\"employees\" referenced-type=\"Employee\" reverse-reference=\"department\"/>" + INDENT + "<collection name=\"rejectedEmployee\" referenced-type=\"Employee\" reverse-reference=\"departmentThatRejectedMe\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Company\" extends=\"RandomInterface HasAddress HasSecretarys\" is-interface=\"true\">" + INDENT + "<attribute name=\"name\" type=\"java.lang.String\"/>" + INDENT + "<attribute name=\"vatNumber\" type=\"int\"/>" + INDENT + "<reference name=\"CEO\" referenced-type=\"CEO\" reverse-reference=\"company\"/>" + INDENT + "<collection name=\"departments\" referenced-type=\"Department\" reverse-reference=\"company\"/>" + INDENT + "<collection name=\"contractors\" referenced-type=\"Contractor\" reverse-reference=\"companys\"/>" + INDENT + "<collection name=\"oldContracts\" referenced-type=\"Contractor\" reverse-reference=\"oldComs\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Address\" extends=\"Thing\" is-interface=\"false\">" + INDENT + "<attribute name=\"address\" type=\"java.lang.String\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"RandomInterface\" is-interface=\"true\"></class>" + ENDL +
        "<class name=\"CEO\" extends=\"Manager HasSecretarys\" is-interface=\"false\">" + INDENT + "<attribute name=\"salary\" type=\"int\"/>" + INDENT + "<reference name=\"company\" referenced-type=\"Company\" reverse-reference=\"CEO\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"ImportantPerson\" is-interface=\"true\">" + INDENT + "<attribute name=\"seniority\" type=\"java.lang.Integer\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Secretary\" is-interface=\"false\">" + INDENT + "<attribute name=\"name\" type=\"java.lang.String\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Types\" is-interface=\"false\">" + INDENT + "<attribute name=\"name\" type=\"java.lang.String\"/>" + INDENT + "<attribute name=\"booleanType\" type=\"boolean\"/>" + INDENT + "<attribute name=\"floatType\" type=\"float\"/>" + INDENT + "<attribute name=\"doubleType\" type=\"double\"/>" + INDENT + "<attribute name=\"shortType\" type=\"short\"/>" + INDENT + "<attribute name=\"intType\" type=\"int\"/>" + INDENT + "<attribute name=\"longType\" type=\"long\"/>" + INDENT + "<attribute name=\"booleanObjType\" type=\"java.lang.Boolean\"/>" + INDENT + "<attribute name=\"floatObjType\" type=\"java.lang.Float\"/>" + INDENT + "<attribute name=\"doubleObjType\" type=\"java.lang.Double\"/>" + INDENT + "<attribute name=\"shortObjType\" type=\"java.lang.Short\"/>" + INDENT + "<attribute name=\"intObjType\" type=\"java.lang.Integer\"/>" + INDENT + "<attribute name=\"longObjType\" type=\"java.lang.Long\"/>" + INDENT + "<attribute name=\"bigDecimalObjType\" type=\"java.math.BigDecimal\"/>" + INDENT + "<attribute name=\"dateObjType\" type=\"java.util.Date\"/>" + INDENT + "<attribute name=\"stringObjType\" type=\"java.lang.String\"/>" + INDENT + "<attribute name=\"clobObjType\" type=\"org.intermine.objectstore.query.ClobAccess\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Bank\" is-interface=\"false\">" + INDENT + "<attribute name=\"name\" type=\"java.lang.String\"/>" + INDENT + "<collection name=\"debtors\" referenced-type=\"Broke\" reverse-reference=\"bank\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"SimpleObject\" extends=\"java.lang.Object\" is-interface=\"false\">" + INDENT + "<attribute name=\"name\" type=\"java.lang.String\"/>" + INDENT + "<reference name=\"employee\" referenced-type=\"Employee\" reverse-reference=\"simpleObjects\"/>" + ENDL + "</class>" + ENDL +
        "<class name=\"Range\" is-interface=\"false\">" + INDENT + "<attribute name=\"rangeStart\" type=\"int\"/>" + INDENT + "<attribute name=\"rangeEnd\" type=\"int\"/>" + INDENT + "<attribute name=\"name\" type=\"java.lang.String\"/>" + INDENT + "<reference name=\"parent\" referenced-type=\"Company\"/>" + ENDL + "</class>" + ENDL
        + "</model>";
        assertEquals(modelString, model.toString());
    }

    public void testGetLevelOrderTraversal() throws Exception {
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

        // the order of nodes at any give level is undefined, so check by level
        List<ClassDescriptor> actual = smallModel.getLevelOrderTraversal();

        // level one
        assertEquals(a, actual.get(0));

        // level two
        Set<ClassDescriptor> expected = new HashSet<ClassDescriptor>();
        expected.add(b);
        expected.add(g);
        assertEquals(expected, new HashSet<ClassDescriptor>(actual.subList(1, 3)));

        // level two
        expected = new HashSet<ClassDescriptor>();
        expected.add(c);
        expected.add(e);
        expected.add(f);
        assertEquals(expected, new HashSet<ClassDescriptor>(actual.subList(3, 6)));

        // level four
        assertEquals(d, actual.get(6));
    }
}
