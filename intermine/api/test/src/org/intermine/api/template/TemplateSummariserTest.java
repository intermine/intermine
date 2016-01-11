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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.Test;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;

/**
 * Tests for the TemplateSummariser.
 */

public class TemplateSummariserTest extends StoreDataTestCase
{
    private Profile profile;
    private ProfileManager pm;
    private ObjectStore os;
    private ObjectStoreWriter uosw;

    public TemplateSummariserTest(String arg) {
        super(arg);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");

        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        pm = new ProfileManager(os, uosw);
        profile = new Profile(pm, "testUser", null, "password", new HashMap(),
                new HashMap(), new HashMap(), null, true, false);
        pm.createProfile(profile);

        TemplateQuery twoConstraints = new TemplateQuery("twoConstraints", "twoConstraints", "", new PathQuery(model));
        twoConstraints.addViews("Employee.name", "Employee.age");
        PathConstraint ageCon = Constraints.greaterThan("Employee.age", "30");
        twoConstraints.addConstraint(ageCon);
        twoConstraints.setEditable(ageCon, true);
        PathConstraint depCon = Constraints.greaterThan("Employee.department.name", "Finance");
        twoConstraints.addConstraint(depCon);
        twoConstraints.setEditable(depCon, true);
        profile.saveTemplate("template", new ApiTemplate(twoConstraints));
    }

    @Override
    public void tearDown() throws Exception {
        profile.deleteTemplate("template", null, true);
        removeUserProfile(profile.getUsername());
        uosw.close();
    }

    private void removeUserProfile(String username) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(username));
        q.setConstraint(sc);
        SingletonResults res = uosw.executeSingleton(q);
        Iterator<?> resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            uosw.delete(o);
        }
    }
    @Override
    public void executeTest(String type) {
    }

    @Override
    public void testQueries() throws Throwable {
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public static Test suite() {
        return buildSuite(TemplateSummariserTest.class);
    }

    public void test1() throws Exception {
        Properties ossConfig = new Properties();
        ObjectStoreSummary oss = new ObjectStoreSummary(ossConfig);
        ApiTemplate t = profile.getSavedTemplates().get("template");
        TemplateSummariser summariser = new TemplateSummariser(os, uosw, oss);
        assertFalse(summariser.isSummarised(t));
        summariser.summarise(t);
        assertTrue(summariser.isSummarised(t));
        Map<String, List<Object>> possibleValues = summariser.getPossibleValues(t);
        assertEquals(2, possibleValues.size());
        assertEquals("Employee.age", possibleValues.keySet().iterator().next());
        Set<Object> expected = new HashSet<Object>(Arrays.asList(10, 20, 30, 40, 50, 60));
        assertEquals(expected, new HashSet<Object>(possibleValues.values().iterator().next()));
    }
}
