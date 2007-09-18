package org.intermine.web.logic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.intermine.web.logic.ClassKeyHelper;

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
        props.load(getClass().getClassLoader().getResourceAsStream("WEB-INF/class_keys.properties"));

        
        
        Map<String, Set<FieldDescriptor>> expected = new HashMap();
        ClassDescriptor cldEmp = model.getClassDescriptorByName(pkg + "Employee");
        ClassDescriptor cldMan = model.getClassDescriptorByName(pkg + "Manager");
        ClassDescriptor cldCEO = model.getClassDescriptorByName(pkg + "CEO");
        ClassDescriptor cldCom = model.getClassDescriptorByName(pkg + "Company");
        ClassDescriptor cldAdd = model.getClassDescriptorByName(pkg + "Address");
        ClassDescriptor cldCon = model.getClassDescriptorByName(pkg + "Contractor");
        ClassDescriptor cldEmb = model.getClassDescriptorByName(pkg + "Employable");
        
        ClassKeyHelper.addKey(expected, "Employee", cldEmp.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Employable", cldEmb.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Contractor", cldCon.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Manager", cldMan.getFieldDescriptorByName("name"));                
        ClassKeyHelper.addKey(expected, "CEO", cldCEO.getFieldDescriptorByName("name"));          
        ClassKeyHelper.addKey(expected, "Manager", cldMan.getFieldDescriptorByName("title")); 
        ClassKeyHelper.addKey(expected, "CEO", cldCEO.getFieldDescriptorByName("title"));
        ClassKeyHelper.addKey(expected, "Company", cldCom.getFieldDescriptorByName("name"));
        ClassKeyHelper.addKey(expected, "Company", cldCom.getFieldDescriptorByName("vatNumber"));
        ClassKeyHelper.addKey(expected, "Address", cldAdd.getFieldDescriptorByName("address"));
        assertEquals(expected, ClassKeyHelper.readKeys(model, props));
    }
    
    public void testIsKeyField() throws Exception {
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("WEB-INF/class_keys.properties"));
        Map classKeys = ClassKeyHelper.readKeys(model, props);
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Employee", "name"));
        assertFalse(ClassKeyHelper.isKeyField(classKeys, "Employee", "age"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Manager", "title"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Company", "name"));
        assertFalse(ClassKeyHelper.isKeyField(classKeys, "Company", "address"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, "Company", "vatNumber"));
    }
    
    public void testIsKeyFieldFromObject() throws Exception {
        Set classNames = new HashSet();
        classNames.add(Employee.class);
        classNames.add(Thing.class);
        classNames.add(Company.class);
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(classNames);
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("WEB-INF/class_keys.properties"));
        Map classKeys = ClassKeyHelper.readKeys(model, props);
        assertTrue(ClassKeyHelper.isKeyField(classKeys, o, "name"));
        assertFalse(ClassKeyHelper.isKeyField(classKeys, o, "age"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, o, "name"));
        assertFalse(ClassKeyHelper.isKeyField(classKeys, o, "address"));
        assertTrue(ClassKeyHelper.isKeyField(classKeys, o, "vatNumber"));
    }
    
    public void testGetKeyFieldClass() throws Exception {
        Set classNames = new HashSet();
        classNames.add(Employee.class);
        classNames.add(Thing.class);
        classNames.add(Company.class);
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(classNames);
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("WEB-INF/class_keys.properties"));
        Map classKeys = ClassKeyHelper.readKeys(model, props);
        assertEquals(Employee.class, ClassKeyHelper.getKeyFieldClass(classKeys, o, "name"));
        assertNull(ClassKeyHelper.getKeyFieldClass(classKeys, o, "age"));

    }
}
