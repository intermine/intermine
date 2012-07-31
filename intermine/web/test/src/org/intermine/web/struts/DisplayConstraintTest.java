package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.AdditionalConverter;
import org.intermine.api.bag.BagQuery;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.query.DisplayConstraint;
import org.intermine.web.logic.query.DisplayConstraintFactory;
import org.intermine.web.logic.query.DisplayConstraint.DisplayConstraintOption;


public class DisplayConstraintTest extends TestCase
{
    protected ProfileManager pm;
    protected ObjectStore os;
    protected ObjectStoreWriter uosw;
    protected Map<String, List<FieldDescriptor>> classKeys;
    protected Profile superUser, testUser, emptyUser;
    protected DisplayConstraint dcAttribute, dcNull, dcBag, dcLookup, dcSubclass,
    dcLoop, dcNullPathConstraint, dcAttribute2, dcInTemplate;
    protected InterMineBag firstEmployeeBag, secondEmployeeBag;

    public void setUp() throws Exception
    {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");

        pm = new ProfileManager(os, uosw);
        superUser = new Profile(pm, "superUser", null, "password",
            new HashMap(), new HashMap(), new HashMap(), true, true);
        pm.createProfile(superUser);

        testUser = new Profile(pm, "testUser", null, "password",
            new HashMap(), new HashMap(), new HashMap(), true, false);
        pm.createProfile(testUser);

        emptyUser = new Profile(pm, "emptyUser", null, "password",
            new HashMap(), new HashMap(), new HashMap(), true, false);
        pm.createProfile(emptyUser);

        initializeDisplayConstraints();

        firstEmployeeBag = superUser.createBag("firstEmployeeBag", "Employee", "", classKeys);
        secondEmployeeBag = superUser.createBag("secondEmployeeBag", "Employee", "", classKeys);
    }

    private void initializeDisplayConstraints() {
        Model model = os.getModel();

        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(getClass().getClassLoader()
                .getResourceAsStream("class_keys.properties"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        classKeys = ClassKeyHelper.readKeys(model, classKeyProps);

        InputStream is = getClass().getClassLoader().getResourceAsStream("bag-queries.xml");
        MokaBagQueryConfig bagQueryConfig = new MokaBagQueryConfig();

        Properties ossProps = new Properties();
        ossProps.put("org.intermine.model.testmodel.Department.classCount", "3");
        ossProps.put("org.intermine.model.testmodel.Department.name.fieldValues",
            "DepartmentA1$_^DepartmentB1$_^DepartmentB2");
        ossProps.put("org.intermine.model.testmodel.Department.id.fieldValues",
            "12000014$_^12000016$_^12000017");
        ossProps.put("org.intermine.model.testmodel.Department.nullFields", "rejectedEmployee");
        ObjectStoreSummary oss = new ObjectStoreSummary(ossProps);


        PathQuery query = new PathQuery(model);
        TemplateQuery template = new TemplateQuery("MyFirstTemplate", "FirstTemplate", "", query);
        InterMineAPI im = new TestInterMineAPI(os, pm, classKeys, bagQueryConfig, oss);
        DisplayConstraintFactory dcf =
            new DisplayConstraintFactory(im, null);

        try {
            PathConstraint pathConstraintAttribute =
                new PathConstraintAttribute("Department.id", ConstraintOp.EQUALS, "11000014");
            dcAttribute = dcf.get(pathConstraintAttribute, superUser, query);
            PathConstraint pathConstraintNull =
                new PathConstraintNull("Employee.id", ConstraintOp.IS_NOT_NULL);
            dcNull = dcf.get(pathConstraintNull, superUser, query);
            PathConstraint pathConstraintBag =
                new PathConstraintBag("Employee", ConstraintOp.IN, "MySecondEmployeeList");
            dcBag = dcf.get(pathConstraintBag, superUser, query);
            PathConstraint pathConstraintLookup =
                new PathConstraintLookup("Employee", "Employee", "DepartmentA1");
            dcLookup = dcf.get(pathConstraintLookup, superUser, query);
            PathConstraint pathConstraintSubclass =
                new PathConstraintSubclass("Department.employees", "Manager");
            dcSubclass = dcf.get(pathConstraintSubclass, superUser, query);
            PathConstraint pathConstraintLoop =
                new PathConstraintLoop("Company.contractors.oldComs",
                                       ConstraintOp.EQUALS, "Company");
            dcLoop = dcf.get(pathConstraintLoop, superUser, query);
            Path path = new Path(model, "Employee.id");
            dcNullPathConstraint = dcf.get(path, superUser, query);
            PathConstraint pathConstraintAttribute2 =
                new PathConstraintAttribute("Employee.id", ConstraintOp.EQUALS, "11000014");
            dcAttribute2 = dcf.get(pathConstraintAttribute2, superUser, query);

            //template
            PathConstraint pathConstraintInTemplate =
                new PathConstraintAttribute("Employee.id", ConstraintOp.EQUALS, "11000014");
            template.addConstraint(pathConstraintInTemplate, "A");
            template.setEditable(pathConstraintInTemplate, true);
            template.setSwitchOffAbility(pathConstraintInTemplate, SwitchOffAbility.ON);
            dcInTemplate = dcf.get(pathConstraintInTemplate, superUser, template);
        } catch (PathException pe) {
            pe.printStackTrace();
            try {
                tearDown();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void tearDown() throws Exception {
        superUser.deleteBag(firstEmployeeBag.getName());
        superUser.deleteBag(secondEmployeeBag.getName());
        removeUserProfile(superUser.getUsername());
        removeUserProfile(testUser.getUsername());
        removeUserProfile(emptyUser.getUsername());

        uosw.close();
    }

    private void removeUserProfile(String username) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc =
            new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(username));
        q.setConstraint(sc);
        SingletonResults res = uosw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            uosw.delete(o);
        }
    }

    // this test isn't closing database connections properly
    // or cleaning up the userprofile db, I had 400+ "test users" in my db
    public void testIAmABadTest() {
        // TODO please fix this test!
        assertEquals("I am a working test", "false");
    }


//    public void testGetCode() {
//        assertNull(dcAttribute.getCode());
//        assertNull(dcNullPathConstraint.getCode());
//        assertEquals("A", dcInTemplate.getCode());
//    }
//
//    public void testIsEditableInTemplate() {
//        assertEquals(false, dcAttribute.isEditableInTemplate());
//        assertEquals(false, dcNullPathConstraint.isEditableInTemplate());
//    }
//
//    public void testGetSelectedValue() {
//        assertEquals("11000014", dcAttribute.getSelectedValue());
//        assertEquals("IS NOT NULL", dcNull.getSelectedValue());
//        assertEquals("MySecondEmployeeList", dcBag.getSelectedValue());
//        assertEquals("Employee", dcLookup.getSelectedValue());
//        assertEquals("Manager", dcSubclass.getSelectedValue());
//        assertEquals("Company", dcLoop.getSelectedValue());
//        assertNull(dcNullPathConstraint.getSelectedValue());
//
//        dcAttribute2.setBagSelected(true);
//        dcAttribute2.setSelectedBagValue("secondEmployeeBag");
//        assertEquals("secondEmployeeBag", dcAttribute2.getSelectedValue());
//    }
//
//    public void testIsBagSelected() {
//        assertEquals(false, dcAttribute.isBagSelected());
//        dcAttribute.setBagSelected(true);
//        assertEquals(true, dcAttribute.isBagSelected());
//        dcAttribute.setBagSelected(false);
//        assertEquals(true, dcBag.isBagSelected());
//        assertEquals(false, dcNullPathConstraint.isBagSelected());
//    }
//
//    public void testIsNullSelected() {
//        assertEquals(false, dcAttribute.isNullSelected());
//        assertEquals(true, dcNull.isNullSelected());
//        assertEquals(false, dcNullPathConstraint.isNullSelected());
//    }
//
//    public void testIsValueSelected() {
//        assertEquals(true, dcAttribute.isValueSelected());
//        assertEquals(true, dcLookup.isValueSelected());
//        assertEquals(false, dcNull.isValueSelected());
//        assertEquals(false, dcBag.isValueSelected());
//        assertEquals(true, dcSubclass.isValueSelected());
//        assertEquals(false, dcLoop.isValueSelected());
//        assertEquals(false, dcNullPathConstraint.isValueSelected());
//    }
//
//    public void testIsLoopSelected() {
//        assertEquals(false, dcAttribute.isLoopSelected());
//        assertEquals(true, dcLoop.isLoopSelected());
//        assertEquals(false, dcNullPathConstraint.isLoopSelected());
//    }
//
//    /**
//     * Return the last class in the path and fieldname as the title for the constraint.
//     */
//    public void testGetTitle() {
//        assertEquals("Department id", dcAttribute.getTitle());
//        assertEquals("Company oldComs", dcLoop.getTitle());
//        assertEquals("Employee id", dcNullPathConstraint.getTitle());
//    }
//
//    /**
//     * Return the label associated with a constraint if editing a template query constraint.
//     */
//    public void testGetDescription() {
//        assertNull(dcAttribute.getDescription());
//        assertNull(dcNullPathConstraint.getDescription());
//    }
//
//    /**
//     * Return a help message to display alongside the constraint, this will examine the constraint
//     * type and generate and appropriate message, e.g. list the key fields for LOOKUP constraints
//     * and explain the use of wildcards.  Returns null when there is no appropriate help.
//     */
//    public void testGetHelpMessage() {
//        assertNotNull(dcAttribute.getHelpMessage());
//        assertNotNull(dcLookup.getHelpMessage());
//        assertNotNull(dcNullPathConstraint.getHelpMessage());
//    }
//
//    /**
//     * If the bag is selected, return the value setted with the method setSelectedBagOp
//     * If editing an existing constraint return the operation used.
//     * Otherwise return null.
//     */
//    public void testGetSelectedOp() {
//        DisplayConstraintOption dco = null;
//        dco = dcAttribute.new DisplayConstraintOption(ConstraintOp.EQUALS.toString(),
//                ConstraintOp.EQUALS.getIndex());
//        assertEquals(dco.getLabel(), dcAttribute.getSelectedOp().getLabel());
//        assertEquals(dco.getProperty(), dcAttribute.getSelectedOp().getProperty());
//
//        dco = dcNull.new DisplayConstraintOption(ConstraintOp.IS_NOT_NULL.toString(),
//                ConstraintOp.IS_NOT_NULL.getIndex());
//        assertEquals(dco.getLabel(), dcNull.getSelectedOp().getLabel());
//        assertEquals(dco.getProperty(), dcNull.getSelectedOp().getProperty());
//
//        dco = dcBag.new DisplayConstraintOption(ConstraintOp.IN.toString(),
//                ConstraintOp.IN.getIndex());
//        assertEquals(dco.getLabel(), dcBag.getSelectedOp().getLabel());
//        assertEquals(dco.getProperty(), dcBag.getSelectedOp().getProperty());
//
//        dco = dcLookup.new DisplayConstraintOption(ConstraintOp.LOOKUP.toString(),
//                ConstraintOp.LOOKUP.getIndex());
//        assertEquals(dco.getLabel(), dcLookup.getSelectedOp().getLabel());
//        assertEquals(dco.getProperty(), dcLookup.getSelectedOp().getProperty());
//
//        assertNull(dcSubclass.getSelectedOp());
//
//        dco = dcLoop.new DisplayConstraintOption(ConstraintOp.EQUALS.toString(),
//                ConstraintOp.EQUALS.getIndex());
//        assertEquals(dco.getLabel(), dcLoop.getSelectedOp().getLabel());
//        assertEquals(dco.getProperty(), dcLoop.getSelectedOp().getProperty());
//
//        assertNull(dcNullPathConstraint.getSelectedOp());
//
//        dcAttribute2.setBagSelected(true);
//        dcAttribute2.setSelectedBagOp(ConstraintOp.IN);
//        dco = dcAttribute2.new DisplayConstraintOption(ConstraintOp.IN.toString(),
//                ConstraintOp.IN.getIndex());
//        assertEquals(dco.getLabel(), dcAttribute2.getSelectedOp().getLabel());
//        assertEquals(dco.getProperty(), dcAttribute2.getSelectedOp().getProperty());
//    }
//
//    /**
//     * If editing an existing LOOKUP constraint return the value selected for the extra constraint
//     * field.  Otherwise return null
//     */
//    public void testGetSelectedExtraValue() {
//        assertNull(dcAttribute.getSelectedExtraValue());
//        assertEquals("DepartmentA1", dcLookup.getSelectedExtraValue());
//        assertNull(dcNullPathConstraint.getSelectedExtraValue());
//    }
//
//    /**
//     * Given the path being constrained return the valid constraint operations.  If constraining an
//     * attribute the valid ops depend on the type being constraint - String, Integer, Boolean, etc.
//     */
//    public void testGetValidOps() {
//        assertEquals(8, dcAttribute.getValidOps().size());
//        assertEquals(6, dcNull.getValidOps().size());
//        assertEquals(2, dcBag.getValidOps().size());
//        assertEquals(1, dcLookup.getValidOps().size());
//        assertEquals(0, dcSubclass.getValidOps().size());
//        assertEquals(2, dcLoop.getValidOps().size());
//        assertEquals(6, dcNullPathConstraint.getValidOps().size());
//    }
//
//    public void testGetLoopQueryOps() {
//        assertEquals(2, dcLoop.getLoopQueryOps().size());
//    }
//
//    /**
//     * Return true if this constraint should be a LOOKUP, true if constraining a class (ref/col)
//     * instead of an attribute and that class has class keys defined.
//     */
//    public void testIsLookup() {
//        assertEquals(false, dcAttribute.isLookup());
//        assertEquals(true, dcLookup.isLookup());
//        assertEquals(false, dcNullPathConstraint.isLookup());
//    }
//
//    public void testGetLookupOp() {
//        assertEquals(ConstraintOp.LOOKUP.toString(), dcLookup.getLookupOp().getLabel());
//        assertEquals(ConstraintOp.LOOKUP.getIndex(), dcLookup.getLookupOp().getProperty());
//    }
//
//    /**
//     * Return the autocompleter for this path if one is available.  Otherwise return null.
//     * @return an autocompleter for this path or null
//     */
//    /*
//    public void testGetAutoCompleter() {
//        if (ac != null && ac.hasAutocompleter(endCls, fieldName)) {
//            return ac;
//        }
//        return null;
//    }
//    */
//
//    /**
//     * Values to populate a dropdown for the path if possible values are available.
//     */
//    public void testGetPossibleValues() {
//        assertEquals(3, dcAttribute.getPossibleValues().size());
//        assertNull(dcNull.getPossibleValues());
//        assertNull(dcBag.getPossibleValues());
//        assertNull(dcNullPathConstraint.getPossibleValues());
//    }
//
//    /**
//     * If a dropdown is available for a constraint fewer operations are possible, return the list
//     * of operations.
//     */
//    public void testGetFixedOps() {
//        assertEquals(6, dcAttribute.getFixedOps().size());
//        assertNull(dcNull.getFixedOps());
//        assertNull(dcNullPathConstraint.getFixedOps());
//    }
//
//    /**
//     * Return true if this is a LOOKUP constraint and an extra constraint should be available.
//     */
//    public void testIsExtraConstraint() {
//        assertEquals(false, dcAttribute.isExtraConstraint());
//        assertEquals(true, dcLookup.isExtraConstraint());
//        assertEquals(false, dcNullPathConstraint.isExtraConstraint());
//    }
//
//    /**
//     * If a LOOKUP constraint and an extra constraint is available for this path, return a list of
//     * the possible values for populating a dropdown.  Otherwise return null.
//     */
//    public void testGetExtraConstraintValues() {
//        assertNull(dcAttribute.getExtraConstraintValues());
//        assertEquals(3, dcLookup.getExtraConstraintValues().size());
//    }
//
//    /**
//     * If a LOOKUP constraint and an extra value constraint is available return the classname of
//     * the extra constraint so it can be displayed.  Otherwise return null.
//     */
//    public void testGetExtraConstraintClassName() {
//        assertNull(dcAttribute.getExtraConstraintClassName());
//        assertEquals("Department", dcLookup.getExtraConstraintClassName());
//    }
//
//    /**
//     * Get a list of public and user bag names available for this path.  If none available return
//     * null.
//     */
//    public void testGetBags() {
//        assertNull(dcAttribute.getBags());
//        assertEquals(2, dcBag.getBags().size());
//        assertEquals(2, dcNullPathConstraint.getBags().size());
//    }
//
//    public void testGetBagsOps() {
//        assertEquals(2, dcBag.getBagOps().size());
//    }
//
//    /**
//     * Returns the bag type that the constraint can be constrained to.
//     *
//     */
//    public void testGetBagType() {
//        assertNull(dcAttribute.getBagType());
//        assertEquals("Employee", dcNull.getBagType());
//        assertEquals("Employee", dcNullPathConstraint.getBagType());
//    }
//
//    /**
//     * Returns the constraint type selected.
//     *
//     */
//    public void testGetSelectedConstraint() {
//        assertEquals("attribute", dcAttribute.getSelectedConstraint());
//        assertEquals("empty", dcNull.getSelectedConstraint());
//        assertEquals("bag", dcBag.getSelectedConstraint());
//        assertEquals("loopQuery", dcLoop.getSelectedConstraint());
//        assertEquals("attribute", dcLookup.getSelectedConstraint());
//        assertEquals("attribute", dcSubclass.getSelectedConstraint());
//        assertEquals("attribute", dcNullPathConstraint.getSelectedConstraint());
//    }
//
//    /**
//     * Returns the set of paths that could feasibly be loop constrained onto the constraint's path,
//     * given the query's outer join situation. A candidate path must be a class path, of the same
//     * type, and in the same outer join group.
//     */
//    public void testGetCandidateLoops() {
//        try {
//            assertEquals(0, dcAttribute.getCandidateLoops().size());
//            assertEquals(1, dcLoop.getCandidateLoops().size());
//        } catch (PathException pe) {
//            pe.printStackTrace();
//        }
//    }
//
//    /**
//     * Return true if the constraint is locked, it should'n be enabled or disabled.
//     */
//    public void testIsLocked() {
//        assertEquals(true, dcAttribute.isLocked());
//        assertEquals(true, dcNullPathConstraint.isLocked());
//        assertEquals(false, dcInTemplate.isLocked());
//    }
//
//    /**
//     * Return true if the constraint is enabled, false if it is disabled or locked.
//     */
//    public void testIsEnabled() {
//        assertEquals(false, dcAttribute.isEnabled());
//        assertEquals(false, dcNullPathConstraint.isEnabled());
//        assertEquals(true, dcInTemplate.isEnabled());
//    }
//
//    /**
//     * Return the value on, off, locked depending on the constraint SwitchOffAbility .
//     */
//    public void testGetSwitchable() {
//        assertEquals("locked", dcAttribute.getSwitchable());
//        assertEquals("locked", dcNullPathConstraint.getSwitchable());
//        assertEquals("on", dcInTemplate.getSwitchable());
//    }

    public class MokaBagQueryConfig extends BagQueryConfig {

        public MokaBagQueryConfig() {
            super(new HashMap<String, List<BagQuery>>(), new HashMap<String, List<BagQuery>>(),
                  new HashMap<String, Set<AdditionalConverter>>());
            this.setConnectField("department");
            this.setConstrainField("name");
            this.setExtraConstraintClassName("org.intermine.model.testmodel.Department");
        }
    }
}
