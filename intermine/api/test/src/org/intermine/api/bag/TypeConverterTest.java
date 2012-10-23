package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.template.ApiTemplate;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;

/**
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public class TypeConverterTest extends StoreDataTestCase
{
    List<ApiTemplate> conversionTemplates;
    ObjectStoreWriter uosw;
    ObjectStore os;
    Profile profile;

    public TypeConverterTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        uosw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        ProfileManager pm = new ProfileManager(os, uosw);
        profile = new Profile(pm, "test", null, "test", null, null, null, true, false);
        pm.createProfile(profile);

        ApiTemplate template = new ApiTemplate("convertEmployeesToAddresses", "", "", new PathQuery(os.getModel()));
        template.addViews("Employee.id", "Employee.address.id");
        PathConstraint employeeId = Constraints.eq("Employee.id", "0");
        template.addConstraint(employeeId);
        template.setEditable(employeeId, true);
        conversionTemplates = new ArrayList<ApiTemplate>(Collections.singleton(template));
    }

    public void tearDown() throws Exception {
        removeUserProfile(profile.getUsername());
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

    public void executeTest(String type) {
    }

    public void testQueries() throws Throwable {
        // We don't want to run the standard queries, just load the test data
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public static Test suite() {
        return buildSuite(TypeConverterTest.class);
    }

    public void testGetConvertedObjectMap() throws Exception {

        Results r = getEmployeesAndAddresses();
        assertEquals("Results: " + r, 2, r.size());
        List<String> classKeys = new ArrayList<String>();
        classKeys.add("name");
        InterMineBag imb = new InterMineBag("Fred", "Employee", "Test bag", new Date(),
                                            BagState.CURRENT, os, null, uosw, classKeys);
        imb.addIdToBag(((Employee) ((List) r.get(0)).get(0)).getId(), "Employee");
        imb.addIdToBag(((Employee) ((List) r.get(1)).get(0)).getId(), "Employee");
        Map expected = new HashMap();
        expected.put(((List) r.get(0)).get(0), Collections.singletonList(((List) r.get(0)).get(1)));
        expected.put(((List) r.get(1)).get(0), Collections.singletonList(((List) r.get(1)).get(1)));

        Map<InterMineObject, List<InterMineObject>> got = TypeConverter.getConvertedObjectMap(conversionTemplates, Employee.class, Address.class, imb, os);

        assertEquals(expected, got);
    }

    private Results getEmployeesAndAddresses() throws Exception {
        List<String> names = Arrays.asList(new String[] {"EmployeeA3", "EmployeeB2"});
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        QueryClass qc2 = new QueryClass(Address.class);
        q.addFrom(qc1);
        q.addToSelect(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);
        cs.addConstraint(new BagConstraint(new QueryField(qc1, "name"), ConstraintOp.IN, names));
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc1, "address"),
                    ConstraintOp.CONTAINS, qc2));
        return os.execute(q);
    }

    public void testGetConversionMapQuery() throws Exception {
        List<String> classKeys = new ArrayList<String>();
        classKeys.add("name");
        InterMineBag bag = new InterMineBag("Fred", "Employee", "Test bag", new Date(),
                                            BagState.CURRENT, os, null, uosw, classKeys);
        PathQuery resQuery = TypeConverter.getConversionMapQuery(conversionTemplates, Employee.class, Address.class, bag);
        assertEquals(1, resQuery.getConstraints().size());
        PathConstraintBag resCon = (PathConstraintBag) resQuery.getConstraints().keySet().iterator().next();
        assertNotNull(resCon);
        assertEquals("Employee", resCon.getPath());
        assertEquals(ConstraintOp.IN, resCon.getOp());
        assertEquals(bag.getName(), resCon.getBag());
    }

    public void testGetConversionQuery() throws Exception {
        List<String> classKeys = new ArrayList<String>();
        classKeys.add("name");
        InterMineBag bag = new InterMineBag("Fred", "Employee", "Test bag", new Date(),
                                            BagState.CURRENT, os, null, uosw, classKeys);
        PathQuery resQuery = TypeConverter.getConversionQuery(conversionTemplates, Employee.class, Address.class, bag);
        assertEquals(1, resQuery.getConstraints().size());
        PathConstraintBag resCon = (PathConstraintBag) resQuery.getConstraints().keySet().iterator().next();
        assertNotNull(resCon);
        assertEquals("Employee", resCon.getPath());
        assertEquals(ConstraintOp.IN, resCon.getOp());
        assertEquals(bag.getName(), resCon.getBag());
        List<String> expectedView = new ArrayList<String>(Collections.singleton("Employee.address.id"));
        assertEquals(expectedView, resQuery.getView());
    }
}
