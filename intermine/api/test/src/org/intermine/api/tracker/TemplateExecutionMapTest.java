package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.tracker.track.TemplateTrack;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.PathQuery;
import org.intermine.api.template.TemplateManager;

/**
 * Test the TemplateExecutionMap class
 * @author dbutano
 *
 */
public class TemplateExecutionMapTest extends TestCase
{
    MokaTemplatesExecutionMap templateExecutionsMap = new MokaTemplatesExecutionMap();
    TemplateTrack tt1, tt2, tt3, tt4, tt5, tt6;
    private ObjectStoreWriter uosw;

    /**
     * Create some template track objects
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        templateExecutionsMap = new MokaTemplatesExecutionMap();
        tt1 = new TemplateTrack("template1", "userName1", "sessionId1");
        tt2 = new TemplateTrack("template1", "userName1", "sessionId1");
        tt3 = new TemplateTrack("template1", "userName1", "sessionId2");
        tt4 = new TemplateTrack("template1", "userName2", "sessionId3");
        tt5 = new TemplateTrack("template1", "", "sessionId1");
        tt6 = new TemplateTrack("template2", "userName2", "sessionId3");
        templateExecutionsMap.addExecution(tt1);
        templateExecutionsMap.addExecution(tt2);
        templateExecutionsMap.addExecution(tt3);
        templateExecutionsMap.addExecution(tt4);
        templateExecutionsMap.addExecution(tt5);
        templateExecutionsMap.addExecution(tt6);
    }

    /**
     * Test the methd addExecution
     */
    public void testAddExecution() {
        assertEquals(2, templateExecutionsMap.getTemplateExecutions().size());
        Map<String, Integer> templateExecutions = templateExecutionsMap.getTemplateExecutions()
                                                                       .get("template1");
        assertEquals(3, templateExecutions.get("userName1").intValue());
        assertEquals(1, templateExecutions.get("sessionId1").intValue());
    }

    /**
     * Moka class adding the method getTemplateExecutions to the TemplatesExecutionMap class
     */
    public class MokaTemplatesExecutionMap extends TemplatesExecutionMap
    {
        /**
         * Return the template execxutions map
         * @return the map
         */
        Map<String, Map<String, Integer>> getTemplateExecutions() {
            return templateExecutions;
        }
    }

    private Profile setUpProfile() throws Exception {
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter(
                                                           "osw.userprofile-test");
        ProfileManager pm = new ProfileManager(os, uosw);
        Profile profile = new Profile(pm, "user", null, "password", new HashMap(),
                new HashMap(), new HashMap(), null, true, false);
        pm.createProfile(profile);
        return profile;
    }

    private void removeProfile() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc =
            new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue("user"));
        q.setConstraint(sc);
        SingletonResults res = uosw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            uosw.delete(o);
        }

        uosw.close();
    }

    /**
     * Moka class to simulate the getValidGlobalTemplates method of the TemplateManager
     */
    public class MokaTemplateManager extends TemplateManager
    {
        public MokaTemplateManager() throws Exception{
            super(setUpProfile(), null);
        }
        @Override
        public Map<String, ApiTemplate> getValidGlobalTemplates() {
            Model model = Model.getInstanceByName("testmodel");
            ApiTemplate tq1 = new ApiTemplate("template1", "template1", "",
                                                  new PathQuery(model));
            ApiTemplate tq2 = new ApiTemplate("template2", "template2", "",
                                                  new PathQuery(model));
            Map<String, ApiTemplate> validGlobalTemplates = new HashMap<String, ApiTemplate>();
            validGlobalTemplates.put("template1", tq1);
            validGlobalTemplates.put("template2", tq2);
            return validGlobalTemplates;
        }
    }

    public void testGetLogarithmMap() throws Exception {
        MokaTemplateManager templateManager = new MokaTemplateManager();
        assertEquals(Math.log(4), templateExecutionsMap.getLogarithmMap("userName1",
                                                        templateManager).get("template1"));
        assertNull(templateExecutionsMap.getLogarithmMap("userName1",
                                                         templateManager).get("template2"));
        assertEquals(Math.log(2), templateExecutionsMap.getLogarithmMap("userName2",
                                                         templateManager).get("template1"));
        assertEquals(Math.log(2), templateExecutionsMap.getLogarithmMap("sessionId1",
                                                        templateManager).get("template1"));

        assertEquals(Math.log(4) + Math.log(2) + Math.log(2),
                     templateExecutionsMap.getLogarithmMap(null, templateManager).get("template1"));
        assertEquals(Math.log(2),
                    templateExecutionsMap.getLogarithmMap(null, templateManager).get("template2"));

        removeProfile();
    }
}
