package org.intermine.web.logic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Thing;
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

        Map<String, List<FieldDescriptor>> expected = new HashMap();
        ClassDescriptor cldEmp = model.getClassDescriptorByName(pkg + "Employee");
        ClassDescriptor cldMan = model.getClassDescriptorByName(pkg + "Manager");
        ClassDescriptor cldCEO = model.getClassDescriptorByName(pkg + "CEO");
        ClassDescriptor cldCom = model.getClassDescriptorByName(pkg + "Company");
        ClassDescriptor cldAdd = model.getClassDescriptorByName(pkg + "Address");
        ClassDescriptor cldCon = model.getClassDescriptorByName(pkg + "Contractor");
        ClassDescriptor cldEmb = model.getClassDescriptorByName(pkg + "Employable");
        ClassDescriptor cldDep = model.getClassDescriptorByName(pkg + "Department");

        ClassKeyHelper.addKey(expected, "Employee", cldEmp.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Manager", cldMan.getFieldDescriptorByName("title"));
        ClassKeyHelper.addKey(expected, "Contractor", cldCon.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Employable", cldEmb.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "CEO", cldCEO.getFieldDescriptorByName("title"));
        ClassKeyHelper.addKey(expected, "Manager", cldMan.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "CEO", cldCEO.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Company", cldCom.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Company", cldCom.getFieldDescriptorByName("vatNumber"));
        ClassKeyHelper.addKey(expected, "Address", cldAdd.getFieldDescriptorByName("address"));
        ClassKeyHelper.addKey(expected, "Department", cldDep.getFieldDescriptorByName("name"));
        assertEquals(expected, ClassKeyHelper.readKeys(model, props));
    }

    public void testIsKeyField() throws Exception {
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("class_keys.properties"));
        Map classKeys = ClassKeyHelper.readKeys(model, props);
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Employee", "name"));
        assertFalse(ClassKeyHelper.isKeyField(classKeys, "Employee", "age"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Manager", "title"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Company", "name"));
        assertFalse(ClassKeyHelper.isKeyField(classKeys, "Company", "address"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Company", "vatNumber"));
    }

 
    public void testHasKeyFields() throws Exception {
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("class_keys.properties"));
        Map classKeys = ClassKeyHelper.readKeys(model, props);
        assertTrue(ClassKeyHelper.hasKeyFields(classKeys, "Company"));
        assertTrue(ClassKeyHelper.hasKeyFields(classKeys, "Employee"));
        assertFalse(ClassKeyHelper.hasKeyFields(classKeys, "Bank"));
    }
}
