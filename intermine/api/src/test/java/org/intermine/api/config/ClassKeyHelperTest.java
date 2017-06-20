package org.intermine.api.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.util.DynamicUtil;

public class ClassKeyHelperTest extends TestCase {
    private Model model;
    private String pkg = "org.intermine.model.testmodel.";

    public ClassKeyHelperTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }

    public void testReadKeys() throws Exception {
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("class_keys.properties"));

        Map<String, List<FieldDescriptor>> expected = new HashMap<String, List<FieldDescriptor>>();
        ClassDescriptor cldEmp = model.getClassDescriptorByName(pkg + "Employee");
        ClassDescriptor cldMan = model.getClassDescriptorByName(pkg + "Manager");
        ClassDescriptor cldCEO = model.getClassDescriptorByName(pkg + "CEO");
        ClassDescriptor cldCom = model.getClassDescriptorByName(pkg + "Company");
        ClassDescriptor cldAdd = model.getClassDescriptorByName(pkg + "Address");
        ClassDescriptor cldCon = model.getClassDescriptorByName(pkg + "Contractor");
        ClassDescriptor cldEmb = model.getClassDescriptorByName(pkg + "Employable");
        ClassDescriptor cldDep = model.getClassDescriptorByName(pkg + "Department");
        ClassDescriptor cldSec = model.getClassDescriptorByName(pkg + "Secretary");
        ClassDescriptor cldSO = model.getClassDescriptorByName(pkg + "SimpleObject");
        ClassDescriptor cldBank = model.getClassDescriptorByName(pkg + "Bank");

        ClassKeyHelper.addKey(expected, "Employable", cldEmb.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Employee", cldEmp.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Manager", cldMan.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Manager", cldMan.getFieldDescriptorByName("title"));
        ClassKeyHelper.addKey(expected, "Contractor", cldCon.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "CEO", cldCEO.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "CEO", cldCEO.getFieldDescriptorByName("title"));
        ClassKeyHelper.addKey(expected, "Company", cldCom.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Company", cldCom.getFieldDescriptorByName("vatNumber"));
        ClassKeyHelper.addKey(expected, "Address", cldAdd.getFieldDescriptorByName("address"));
        ClassKeyHelper.addKey(expected, "Department", cldDep.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Secretary", cldSec.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "SimpleObject", cldSO.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Bank", cldBank.getFieldDescriptorByName("name"));
        assertEquals(expected, ClassKeyHelper.readKeys(model, props));
    }

    public void testIsKeyField() throws Exception {
        Map<String, List<FieldDescriptor>> classKeys = getClassKeys();
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Employee", "name"));
        assertFalse(ClassKeyHelper.isKeyField(classKeys, "Employee", "age"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Manager", "title"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Company", "name"));
        assertFalse(ClassKeyHelper.isKeyField(classKeys, "Company", "address"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Company", "vatNumber"));
    }


    public void testHasKeyFields() throws Exception {
        Map<String, List<FieldDescriptor>> classKeys = getClassKeys();
        assertTrue(ClassKeyHelper.hasKeyFields(classKeys, "Company"));
        assertTrue(ClassKeyHelper.hasKeyFields(classKeys, "Employee"));
        assertFalse(ClassKeyHelper.hasKeyFields(classKeys, "Test"));
    }

    public void testGetKeyFieldValue() throws Exception {
        Map<String, List<FieldDescriptor>> classKeys = getClassKeys();
        // class keys for Company: name, vatNumber
        FastPathObject obj =
            DynamicUtil.instantiateObject("org.intermine.model.testmodel.Company", null);
        obj.setFieldValue("vatNumber", 1234);
        assertEquals(1234, ClassKeyHelper.getKeyFieldValue(obj, classKeys));
        obj.setFieldValue("name", "CompanyA");
        assertEquals("CompanyA", ClassKeyHelper.getKeyFieldValue(obj, classKeys));

        FastPathObject manager =
            DynamicUtil.instantiateObject("org.intermine.model.testmodel.Manager", null);
        assertEquals(null, ClassKeyHelper.getKeyFieldValue(manager, classKeys));
        manager.setFieldValue("title", "Sir");
        assertEquals("Sir", ClassKeyHelper.getKeyFieldValue(manager, classKeys));
        manager.setFieldValue("name", "Geoff");
        assertEquals("Geoff", ClassKeyHelper.getKeyFieldValue(manager, classKeys));
    }

    private Map<String, List<FieldDescriptor>> getClassKeys() throws IOException {
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("class_keys.properties"));
        Map<String, List<FieldDescriptor>> classKeys = ClassKeyHelper.readKeys(model, props);
        return classKeys;
    }

}
