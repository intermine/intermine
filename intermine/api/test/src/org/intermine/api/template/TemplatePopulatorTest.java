package org.intermine.api.template;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;

public class TemplatePopulatorTest extends TestCase {


    private TemplateQuery simple;
    private TemplateQuery twoConstraints;
    private Map<String, List<TemplateValue>> values = new HashMap<String, List<TemplateValue>>();

    public void setUp() throws Exception {
        super.setUp();
        Model model = Model.getInstanceByName("testmodel");

        simple = new TemplateQuery("simple", "simple", "", new PathQuery(model));
        simple.addViews("Employee.name", "Employee.age");
        PathConstraint nameCon = Constraints.eq("Employee.name", "Marmaduke");
        simple.addConstraint(nameCon);
        simple.setEditable(nameCon, true);

        twoConstraints = new TemplateQuery("twoConstraints", "twoConstraints", "", new PathQuery(model));
        twoConstraints.addViews("Employee.name", "Employee.age");
        PathConstraint ageCon = Constraints.greaterThan("Employee.age", "30");
        twoConstraints.addConstraint(ageCon);
        twoConstraints.setEditable(ageCon, true);
        PathConstraint depCon = Constraints.greaterThan("Employee.departments.name", "Finance");
        twoConstraints.addConstraint(depCon);
        twoConstraints.setEditable(depCon, true);
    }


    private Profile setUpProfile() throws Exception {
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");

        ObjectStoreWriter uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        ProfileManager pm = new ProfileManager(os, uosw);

        Profile profile = new Profile(pm, "testUser", null, "password", new HashMap(), 
                new HashMap(), new HashMap());
        pm.createProfile(profile);
        return profile;
    }


    public void testNoValuesForNode() throws Exception {
        PathConstraint age = new PathConstraintAttribute("Employee.age", ConstraintOp.EQUALS, "30");
        TemplateValue value = new TemplateValue(age, ConstraintOp.EQUALS, "21", 
                TemplateValue.ValueType.SIMPLE_VALUE, SwitchOffAbility.LOCKED);
        values.put("Employee.age", Arrays.asList(new TemplateValue[] {value}));
        try {
            TemplatePopulator.getPopulatedTemplate(simple, values);
            fail("Expected a TemplatePopulationException.");
        } catch (TemplatePopulatorException e) {
        }
    }

    public void testTooManyValuesForNode() throws Exception {
        PathConstraint nameCon = simple.getEditableConstraints("Employee.name").get(0);
        TemplateValue value1 = new TemplateValue(nameCon, ConstraintOp.EQUALS, "name", 
                TemplateValue.ValueType.SIMPLE_VALUE, SwitchOffAbility.LOCKED);
        TemplateValue value2 = new TemplateValue(nameCon, ConstraintOp.EQUALS, "other name", 
                TemplateValue.ValueType.SIMPLE_VALUE, SwitchOffAbility.LOCKED);
        values.put("Employee.name", Arrays.asList(new TemplateValue[] {value1, value2}));
        try {
            TemplatePopulator.getPopulatedTemplate(simple, values);
            fail("Expected a TemplatePopulationException.");
        } catch (TemplatePopulatorException e) {
        }
    }

    public void testTooFewValuesForNode() throws Exception {
        PathConstraint ageCon = twoConstraints.getEditableConstraints("Employee.age").get(0);
        TemplateValue value = new TemplateValue(ageCon, ConstraintOp.EQUALS, "21",
                TemplateValue.ValueType.SIMPLE_VALUE, SwitchOffAbility.LOCKED);
        values.put("Employee.age", Arrays.asList(new TemplateValue[] {value}));
        try {
            TemplatePopulator.getPopulatedTemplate(twoConstraints, values);
            fail("Expected a TemplatePopulationException.");
        } catch (TemplatePopulatorException e) {
        }
    }

    public void testOneConstraint() throws Exception {
        PathConstraint nameCon = simple.getEditableConstraints("Employee.name").get(0);
        TemplateValue value = new TemplateValue(nameCon, ConstraintOp.NOT_EQUALS,
                "Kevin Bacon", TemplateValue.ValueType.SIMPLE_VALUE, SwitchOffAbility.LOCKED);
        values.put("Employee.name", Arrays.asList(new TemplateValue[] {value}));
        TemplateQuery res = TemplatePopulator.getPopulatedTemplate(simple, values);
        assertEquals(1, res.getEditableConstraints().size());
        PathConstraint resCon = res.getEditableConstraints().get(0);
        assertEquals(ConstraintOp.NOT_EQUALS, resCon.getOp());
        assertEquals("Kevin Bacon", ((PathConstraintAttribute) resCon).getValue());
    }

    public void testOneBagConstraint() throws Exception {
        PathConstraint nameCon = simple.getEditableConstraints("Employee.name").get(0);
        TemplateValue value = new TemplateValue(nameCon, ConstraintOp.IN, "bag1",
                TemplateValue.ValueType.BAG_VALUE, SwitchOffAbility.LOCKED);
        values.put("Employee.name", Arrays.asList(new TemplateValue[] {value}));
        TemplateQuery res = TemplatePopulator.getPopulatedTemplate(simple, values);
        assertEquals(1, res.getEditableConstraints().size());
        // constraint should now be on parent node
        assertEquals(1, res.getEditableConstraints("Employee").size());
        assertEquals(0, res.getEditableConstraints("Employee.name").size());
        
        PathConstraint resCon = res.getEditableConstraints().get(0);
        assertEquals(ConstraintOp.IN, resCon.getOp());
        
        assertEquals("bag1", ((PathConstraintBag) resCon).getBag());
    }


    public void testPopulateTemplateWithBagNotOneConstraint() throws Exception {
        Profile profile = setUpProfile();
        InterMineBag bag = profile.createBag("bag1", "Company", "");
        try {
            TemplatePopulator.populateTemplateWithBag(twoConstraints, bag);
            fail("Expected a TemplatePopulatorException.");
        } catch (TemplatePopulatorException e) {
        }
    }

    public void testPopulateTemplateWithBagWrongType() throws Exception {
        Profile profile = setUpProfile();
        InterMineBag bag = profile.createBag("bag1", "Company", "");
        try {
            TemplatePopulator.populateTemplateWithBag(simple, bag);
            fail("Expected a TemplatePopulatorException.");
        } catch (TemplatePopulatorException e) {
        }
    }

    public void testPopulateTemplateWithBag() throws Exception {
        Profile profile = setUpProfile();
        InterMineBag bag = profile.createBag("bag1", "Employee", "");
        TemplateQuery res = TemplatePopulator.populateTemplateWithBag(simple, bag);
        assertEquals(1, res.getEditableConstraints().size());
        // constraint should now be on parent node
        assertEquals(1, res.getEditableConstraints("Employee").size());
        assertEquals(0, res.getEditableConstraints("Employee.name").size());

        PathConstraint resCon = res.getEditableConstraints().get(0);
        assertEquals(ConstraintOp.IN, resCon.getOp());
        assertEquals("bag1", ((PathConstraintBag) resCon).getBag());
    }
    
    public void testPopulateTemplateWithObject() throws Exception {
        InterMineObject obj = 
            (InterMineObject) DynamicUtil.createObject(Employee.class);
        obj.setId(101);
        TemplateQuery res = TemplatePopulator.populateTemplateWithObject(simple, obj);
        assertEquals(0, res.getEditableConstraints("Employee").size());
        assertEquals(0, res.getEditableConstraints("Employee.name").size());
        assertEquals(1, res.getEditableConstraints("Employee.id").size());

        PathConstraint resCon = res.getEditableConstraints().get(0);
        assertEquals(ConstraintOp.EQUALS, resCon.getOp());
        assertEquals("101", ((PathConstraintAttribute) resCon).getValue());
    }
    
    public void testPopulateTemplateWithObjectNotOneConstraint() throws Exception {
        InterMineObject obj = 
            (InterMineObject) DynamicUtil.createObject(Employee.class);
        obj.setId(101);
        try {
            TemplatePopulator.populateTemplateWithObject(twoConstraints, obj);
            fail("Expected a TemplatePopulatorException");
        } catch (TemplatePopulatorException e) {            
        }
    }
    
    public void testPopulateTemplateWithObjectWrongType() throws Exception {
        InterMineObject obj = 
            (InterMineObject) DynamicUtil.createObject(Department.class);
        obj.setId(101);
        try {
            TemplatePopulator.populateTemplateWithObject(simple, obj);
            fail("Expected a TemplatePopulatorException");
        } catch (TemplatePopulatorException e) {
        }
    }

    public void testSetConstraintWrongBagOp() throws Exception {
        PathConstraint nameCon = simple.getEditableConstraints("Employee.name").get(0);
        TemplateValue value = new TemplateValue(nameCon, ConstraintOp.GREATER_THAN, "bag1",
                TemplateValue.ValueType.BAG_VALUE, SwitchOffAbility.LOCKED);
        values.put("Employee.name", Arrays.asList(new TemplateValue[] {value}));
        try {
            TemplatePopulator.getPopulatedTemplate(simple, values);
            fail("Expected an exception.");
        } catch (IllegalArgumentException e) {    		
        }
    }
}
