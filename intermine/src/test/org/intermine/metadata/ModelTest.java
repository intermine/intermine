package org.flymine.metadata;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


public class ModelTest extends TestCase
{
    Model m1, m2, m3, m4, m5;

    public ModelTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        m1 = new Model("flibble", new HashSet());
        m2 = new Model("flibble", new HashSet());
        m3 = new Model("flobble", new HashSet());
        m4 = new Model("flibble", Collections.singleton(new ClassDescriptor("class1", null, null, true, new HashSet(), new HashSet(), new HashSet())));
        m5 = new Model("flibble", Collections.singleton(new ClassDescriptor("class1", null, null, true, new HashSet(), new HashSet(), new HashSet())));
    }

    public void testEquals1() throws Exception {
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertTrue(!m1.equals(m3));
        assertTrue(m1.hashCode() != m3.hashCode());
    }

    public void testEquals2() throws Exception {
        assertTrue(!m1.equals(m4));
        assertTrue(m1.hashCode() != m4.hashCode());
        assertEquals(m4, m5);
        assertEquals(m4.hashCode(), m5.hashCode());
    }

    /*
     * TODO
    public void testEquals3() throws Exception {
        AceModelParser amp = new AceModelParser();
        Model lm1 = amp.readerToModel(new BufferedReader(new FileReader("models.wrm")));
        Model lm2 = amp.readerToModel(new BufferedReader(new FileReader("models.wrm")));
        assertTrue(lm1.equals(lm2));
    }
    */

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
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));

        try {
            Model model = new Model(null, clds);
            fail("Expected NullPointerException, name was null");
        } catch(NullPointerException e) {
        }

        try {
            Model model = new Model("", clds);
            fail("Expected IllegalArgumentException, name was empty string");
        } catch(IllegalArgumentException e) {
        }

        try {
            Model model = new Model("model", null);
            fail("Expected NullPointerException, name was null");
        } catch(NullPointerException e) {
        }
    }

    public void testGetClassDescriptorByName() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("model", clds);

        assertEquals(cld1, model.getClassDescriptorByName("Class1"));
        assertEquals(cld2, model.getClassDescriptorByName("Class2"));
    }

    public void testGetClassDescriptorByWrongName() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("model", clds);

        assertTrue(null == model.getClassDescriptorByName("WrongName"));
    }

    public void testGetClassNames() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Set clds = new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("model", clds);

        Set names = new HashSet(Arrays.asList(new Object[] {"Class1", "Class2"}));

        assertEquals(names, model.getClassNames());
    }

    public void testToString() throws Exception {
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, null, false, new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, null, false, new HashSet(), new HashSet(), new HashSet());
        Set clds = new LinkedHashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("model", clds);

        String expected = "<model name=\"model\">"
            + "<class name=\"Class1\" is-interface=\"false\"></class>"
            + "<class name=\"Class2\" is-interface=\"false\"></class>"
            + "</model>";

        assertEquals(expected, model.toString());
    }
}
