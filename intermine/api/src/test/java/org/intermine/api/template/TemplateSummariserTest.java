package org.intermine.api.template;

/*
 * Copyright (C) 2002-2021 FlyMine
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

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.api.userprofile.UserProfile;
import org.intermine.objectstore.*;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class TemplateSummariserTest
{
    private Profile profile;
    private ProfileManager pm;
    private ObjectStore os;
    private ObjectStoreWriter uosw;

    @Before
    public void setUp() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();

        Map data = ObjectStoreTestUtils.getTestData("testmodel", "testmodel_data.xml");
        ObjectStoreTestUtils.storeData(osw, data);
        osw.close();

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

    @After
    public void teardown() throws Exception {
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

    @org.junit.Test
    public void test1() throws Exception {
        Properties ossConfig = new Properties();
        ObjectStoreSummary oss = new ObjectStoreSummary(ossConfig);
        ApiTemplate t = profile.getSavedTemplates().get("template");
        TemplateSummariser summariser = new TemplateSummariser(os, uosw, oss);
        Assert.assertFalse(summariser.isSummarised(t));
        summariser.summarise(t);
        Assert.assertTrue(summariser.isSummarised(t));
        Map<String, List<Object>> possibleValues = summariser.getPossibleValues(t);
        Assert.assertEquals(2, possibleValues.size());
        Assert.assertEquals("Employee.age", possibleValues.keySet().iterator().next());
        Set<Object> expected = new HashSet<Object>(Arrays.asList(10, 20, 30, 40, 50, 60));
        Assert.assertEquals(expected, new HashSet<Object>(possibleValues.values().iterator().next()));
    }
}
