package org.intermine.api.template;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathQuery;
import org.intermine.sql.DatabaseUtil;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplatePopulatorException;
import org.intermine.template.TemplateQuery;
import org.intermine.template.TemplateValue;
import org.intermine.util.DynamicUtil;

public class TemplatePopulatorTest extends TestCase
{
    private TemplateQuery simple;
    private TemplateQuery simpleWithOptionalCon;
    private TemplateQuery twoConstraints;
    private TemplateQuery threeConstraints;
    private Map<String, List<TemplateValue>> values = new HashMap<String, List<TemplateValue>>();
    private ObjectStoreWriter uosw;
    Map<String, List<FieldDescriptor>>  classKeys;
    private ProfileManager pm;
    private ObjectStore os;

    public void setUp() throws Exception {
        super.setUp();
        Model model = Model.getInstanceByName("testmodel");

        simple = new TemplateQuery("simple", "simple", "", new PathQuery(model));
        simple.addViews("Employee.name", "Employee.age");
        PathConstraint nameCon = Constraints.eq("Employee.name", "Marmaduke");
        simple.addConstraint(nameCon);
        simple.setEditable(nameCon, true);

        simpleWithOptionalCon = new TemplateQuery("simpleWithOtionalCon",
            "Simple, But with an optional rather than required constraint", "",
             new PathQuery(model));
        simpleWithOptionalCon.addViews("Employee.name", "Employee.age");
        simpleWithOptionalCon.addConstraint(nameCon);
        simpleWithOptionalCon.setEditable(nameCon, true);
        simpleWithOptionalCon.setSwitchOffAbility(nameCon, SwitchOffAbility.ON);

        twoConstraints = new TemplateQuery("twoConstraints", "twoConstraints", "",
                                           new PathQuery(model));
        twoConstraints.addViews("Employee.name", "Employee.age");
        PathConstraint ageCon = Constraints.greaterThan("Employee.age", "30");
        twoConstraints.addConstraint(ageCon);
        twoConstraints.setEditable(ageCon, true);
        PathConstraint depCon = Constraints.greaterThan("Employee.departments.name", "Finance");
        twoConstraints.addConstraint(depCon);
        twoConstraints.setEditable(depCon, true);
        twoConstraints.setSwitchOffAbility(depCon, SwitchOffAbility.ON);

        threeConstraints = new TemplateQuery("twoConstraints", "twoConstraints", "",
                                             new PathQuery(model));
        threeConstraints.addViews("Employee.name", "Employee.age");
        threeConstraints.addConstraint(ageCon);
        threeConstraints.setEditable(ageCon, true);
        threeConstraints.addConstraint(depCon);
        threeConstraints.setEditable(depCon, true);
        threeConstraints.setSwitchOffAbility(depCon, SwitchOffAbility.ON);
        threeConstraints.addConstraint(nameCon);
        threeConstraints.setEditable(nameCon, true);

        Properties classKeyProps = new Properties();
        classKeyProps.load(getClass().getClassLoader().getResourceAsStream("class_keys.properties"));
        classKeys = ClassKeyHelper.readKeys(model, classKeyProps);

        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        Connection con = ((ObjectStoreWriterInterMineImpl) uosw).getDatabase().getConnection();
        if (!DatabaseUtil.tableExists(con, "bagvalues")) {
            DatabaseUtil.createBagValuesTables(con);
        }
        if (con != null) {
            con.close();
        }

        os = ObjectStoreFactory.getObjectStore("os.unittest");
        pm = new ProfileManager(os, uosw);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        pm.close();
    }

    private Profile setUpProfile() throws Exception {
//        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
//        ProfileManager pm = new ProfileManager(os, uosw);

        Profile profile = new Profile(pm, "testUser", null, "password", new HashMap(),
                new HashMap(), new HashMap(), true, false);
        pm.createProfile(profile);
        return profile;
    }

    public void testNoValuesForRequiredNode() throws Exception {
        try {
            TemplatePopulator.getPopulatedTemplate(simple, values);
            fail("Expected a TemplatePopulationException.");
        } catch (TemplatePopulatorException e) {
            assertEquals("No value provided for required constraint Employee.name = Marmaduke",
                         e.getMessage());
        }
    }

    public void testNoValuesForOptionalNode() throws Exception {
        TemplateQuery tq = TemplatePopulator.getPopulatedTemplate(simpleWithOptionalCon, values);
        assertEquals(Collections.EMPTY_SET, tq.getConstraints().keySet());
        assertEquals(Collections.EMPTY_SET, tq.getConstraintCodes());

        TemplateValue value = new TemplateValue(
                twoConstraints.getEditableConstraints().get(0), ConstraintOp.EQUALS, "21",
                TemplateValue.ValueType.SIMPLE_VALUE, SwitchOffAbility.LOCKED);
        values.put("Employee.age", Arrays.asList(new TemplateValue[] {value}));
        TemplateQuery tq2 = TemplatePopulator.getPopulatedTemplate(twoConstraints, values);
        assertEquals(new HashSet<String>(Arrays.asList("A")),
                     new HashSet<String>(tq2.getConstraints().values()));
        assertEquals(new HashSet<String>(Arrays.asList("A")), tq2.getConstraintCodes());
    }

    public void testValueForNonExistentNode() throws Exception {
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
        PathConstraint ageCon = threeConstraints.getEditableConstraints("Employee.age").get(0);
        TemplateValue value = new TemplateValue(ageCon, ConstraintOp.EQUALS, "21",
                TemplateValue.ValueType.SIMPLE_VALUE, SwitchOffAbility.LOCKED);
        values.put("Employee.age", Arrays.asList(new TemplateValue[] {value}));
        try {
            TemplatePopulator.getPopulatedTemplate(threeConstraints, values);
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
        InterMineBag bag = profile.createBag("bag1", "Company", "", classKeys);
        try {
            TemplatePopulator.populateTemplateWithBag(twoConstraints, bag);
            fail("Expected a TemplatePopulatorException.");
        } catch (TemplatePopulatorException e) {
        } finally {
            profile.deleteBag("bag1");
            removeUserProfile(profile.getUsername());
        }
    }

    public void testPopulateTemplateWithBagWrongType() throws Exception {
        Profile profile = setUpProfile();
        InterMineBag bag = profile.createBag("bag1", "Company", "", classKeys);
        try {
            TemplatePopulator.populateTemplateWithBag(simple, bag);
            fail("Expected a TemplatePopulatorException.");
        } catch (TemplatePopulatorException e) {
        } finally {
            profile.deleteBag("bag1");
            removeUserProfile(profile.getUsername());
        }
    }

    public void testPopulateTemplateWithBag() throws Exception {
        Profile profile = setUpProfile();
        InterMineBag bag = profile.createBag("bag1", "Employee", "", classKeys);
        TemplateQuery res = TemplatePopulator.populateTemplateWithBag(simple, bag);
        assertEquals(1, res.getEditableConstraints().size());
        // constraint should now be on parent node
        assertEquals(1, res.getEditableConstraints("Employee").size());
        assertEquals(0, res.getEditableConstraints("Employee.name").size());

        PathConstraint resCon = res.getEditableConstraints().get(0);
        assertEquals(ConstraintOp.IN, resCon.getOp());
        assertEquals("bag1", ((PathConstraintBag) resCon).getBag());
        profile.deleteBag("bag1");
        removeUserProfile(profile.getUsername());
    }

    private void removeUserProfile(String username) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                              new QueryValue(username));
        q.setConstraint(sc);
        SingletonResults res = uosw.executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            uosw.delete(o);
        }
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
