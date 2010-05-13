package org.intermine.metadata;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Iterator;

import org.intermine.model.testmodel.SimpleObject;

public class ModelTest extends TestCase
{
    private static final Set EMPTY_SET = Collections.EMPTY_SET;
    protected static final String ENDL = System.getProperty("line.separator");
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
        "<class name=\"Broke\" is-interface=\"true\"><attribute name=\"debt\" type=\"int\"/><reference name=\"bank\" referenced-type=\"Bank\" reverse-reference=\"debtors\"/></class>" + ENDL +
        "<class name=\"Thing\" is-interface=\"true\"></class>" + ENDL +
        "<class name=\"Employable\" extends=\"Thing\" is-interface=\"true\"><attribute name=\"name\" type=\"java.lang.String\"/></class>" + ENDL +
        "<class name=\"HasAddress\" is-interface=\"true\"><reference name=\"address\" referenced-type=\"Address\"/></class>" + ENDL +
        "<class name=\"HasSecretarys\" is-interface=\"true\"><collection name=\"secretarys\" referenced-type=\"Secretary\"/></class>" + ENDL +
        "<class name=\"Contractor\" extends=\"Employable ImportantPerson\" is-interface=\"false\"><reference name=\"personalAddress\" referenced-type=\"Address\"/><reference name=\"businessAddress\" referenced-type=\"Address\"/><collection name=\"companys\" referenced-type=\"Company\" reverse-reference=\"contractors\"/><collection name=\"oldComs\" referenced-type=\"Company\" reverse-reference=\"oldContracts\"/></class>" + ENDL +
        "<class name=\"Manager\" extends=\"Employee ImportantPerson\" is-interface=\"false\"><attribute name=\"title\" type=\"java.lang.String\"/></class>" + ENDL +
        "<class name=\"Employee\" extends=\"Employable HasAddress\" is-interface=\"false\"><attribute name=\"fullTime\" type=\"boolean\"/><attribute name=\"age\" type=\"int\"/><attribute name=\"end\" type=\"java.lang.String\"/><reference name=\"department\" referenced-type=\"Department\" reverse-reference=\"employees\"/><reference name=\"departmentThatRejectedMe\" referenced-type=\"Department\" reverse-reference=\"rejectedEmployee\"/><collection name=\"simpleObjects\" referenced-type=\"SimpleObject\" reverse-reference=\"employee\"/></class>" + ENDL +
        "<class name=\"Department\" extends=\"RandomInterface\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/><reference name=\"company\" referenced-type=\"Company\" reverse-reference=\"departments\"/><reference name=\"manager\" referenced-type=\"Manager\"/><collection name=\"employees\" referenced-type=\"Employee\" reverse-reference=\"department\"/><collection name=\"rejectedEmployee\" referenced-type=\"Employee\" reverse-reference=\"departmentThatRejectedMe\"/></class>" + ENDL +
        "<class name=\"Company\" extends=\"RandomInterface HasAddress HasSecretarys\" is-interface=\"true\"><attribute name=\"name\" type=\"java.lang.String\"/><attribute name=\"vatNumber\" type=\"int\"/><reference name=\"CEO\" referenced-type=\"CEO\" reverse-reference=\"company\"/><collection name=\"departments\" referenced-type=\"Department\" reverse-reference=\"company\"/><collection name=\"contractors\" referenced-type=\"Contractor\" reverse-reference=\"companys\"/><collection name=\"oldContracts\" referenced-type=\"Contractor\" reverse-reference=\"oldComs\"/></class>" + ENDL +
        "<class name=\"Address\" extends=\"Thing\" is-interface=\"false\"><attribute name=\"address\" type=\"java.lang.String\"/></class>" + ENDL +
        "<class name=\"RandomInterface\" is-interface=\"true\"></class>" + ENDL +
        "<class name=\"CEO\" extends=\"Manager HasSecretarys\" is-interface=\"false\"><attribute name=\"salary\" type=\"int\"/><reference name=\"company\" referenced-type=\"Company\" reverse-reference=\"CEO\"/></class>" + ENDL +
        "<class name=\"ImportantPerson\" is-interface=\"true\"><attribute name=\"seniority\" type=\"java.lang.Integer\"/></class>" + ENDL +
        "<class name=\"Secretary\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/></class>" + ENDL +
        "<class name=\"Types\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/><attribute name=\"booleanType\" type=\"boolean\"/><attribute name=\"floatType\" type=\"float\"/><attribute name=\"doubleType\" type=\"double\"/><attribute name=\"shortType\" type=\"short\"/><attribute name=\"intType\" type=\"int\"/><attribute name=\"longType\" type=\"long\"/><attribute name=\"booleanObjType\" type=\"java.lang.Boolean\"/><attribute name=\"floatObjType\" type=\"java.lang.Float\"/><attribute name=\"doubleObjType\" type=\"java.lang.Double\"/><attribute name=\"shortObjType\" type=\"java.lang.Short\"/><attribute name=\"intObjType\" type=\"java.lang.Integer\"/><attribute name=\"longObjType\" type=\"java.lang.Long\"/><attribute name=\"bigDecimalObjType\" type=\"java.math.BigDecimal\"/><attribute name=\"dateObjType\" type=\"java.util.Date\"/><attribute name=\"stringObjType\" type=\"java.lang.String\"/></class>" + ENDL +
        "<class name=\"Bank\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/><collection name=\"debtors\" referenced-type=\"Broke\" reverse-reference=\"bank\"/></class>" + ENDL +
        "<class name=\"SimpleObject\" extends=\"java.lang.Object\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/><reference name=\"employee\" referenced-type=\"Employee\" reverse-reference=\"simpleObjects\"/></class>" + ENDL +
        "<class name=\"Range\" is-interface=\"false\"><attribute name=\"rangeStart\" type=\"int\"/><attribute name=\"rangeEnd\" type=\"int\"/><attribute name=\"name\" type=\"java.lang.String\"/><reference name=\"parent\" referenced-type=\"Company\"/></class>" + ENDL
        + "</model>";
        assertEquals(modelString, model.toString());
    }
}
