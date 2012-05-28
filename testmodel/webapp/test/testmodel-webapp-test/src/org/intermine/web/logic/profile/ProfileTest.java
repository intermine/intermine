package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.template.ApiTemplate;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.pathquery.PathQuery;

public class ProfileTest extends TestCase
{
    PathQuery query;
    SavedQuery sq;
    Date date = new Date();
    InterMineBag bag;
    ApiTemplate template;
    private Integer bobId = new Integer(101);
    private ObjectStoreWriter userprofileOS;
    private ObjectStore objectstoreOS;
    ProfileManager profileManager;

    public ProfileTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        query = new PathQuery(Model.getInstanceByName("testmodel"));
        userprofileOS = ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        objectstoreOS = ObjectStoreFactory.getObjectStore("os.unittest");
        bag = new InterMineBag("bob", "Company", "Description", new Date(), BagState.CURRENT, objectstoreOS, bobId, userprofileOS);
        //Collections.singleton("testElement"));
//        bag = new InterMinePrimitiveBag(bobId, "bob", userprofileOS, Collections.singleton("1234"));
        sq = new SavedQuery("query1", date, query);
        template = new ApiTemplate("template", "ttitle", "tdesc", new PathQuery(Model.getInstanceByName("testmodel")));
        profileManager = new DummyProfileManager();

    }

    public void tearDown() throws Exception {
        profileManager.close();
        userprofileOS.close();
    }

    public void testModifySavedMaps() throws Exception {
        Profile profile = new Profile(null, "bob", bobId, "pass",
                                  new HashMap(), new HashMap(), new HashMap(), true, false);

        try {
            profile.getSavedQueries().put("query0", null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            profile.getSavedBags().put("bag0", null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    public void testSaveNoManager() throws Exception {
        Profile profile = new Profile(null, "bob", bobId, "pass",
                                  new HashMap(), new HashMap(), new HashMap(), true, false);
        profile.saveQuery("query1", sq);
        profile.saveBag("bag1", bag);
        profile.saveTemplate("template", template);
        assertEquals(1, profile.getSavedQueries().size());
        assertEquals(profile.getSavedQueries().get("query1"), sq);
        assertEquals(1, profile.getSavedBags().size());
        assertEquals(profile.getSavedBags().get("bag1"), bag);
        assertEquals(1, profile.getSavedTemplates().size());
        assertEquals(profile.getSavedTemplates().get("template"), template);
    }

    public void testDeleteNoManager() throws Exception {
        Map queries = new HashMap();
        queries.put("query1", query);
        Map bags = new HashMap();
        bags.put("bag1", bag);
        Map tmpls = new HashMap();
        tmpls.put("tmpl1", template);

        Profile profile = new Profile(null, "bob", bobId, "pass", queries, bags, tmpls, true, false);
        profile.deleteQuery("query1");
        // It isn't possible to delete a bag without a manager but we never do in the code
        //profile.deleteBag("bag1");
        profile.deleteTemplate("tmpl1", null, false);

        assertEquals(0, profile.getSavedQueries().size());
        //assertEquals(0, profile.getSavedBags().size());
        assertEquals(0, profile.getSavedTemplates().size());
    }

    public void testSaveWithManager() throws Exception {
        Profile profile = new Profile(profileManager, "bob", bobId, "pass",
                                  new HashMap(), new HashMap(), new HashMap(), true, false);

        try {
            profile.saveQuery("query1", sq);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            profile.saveTemplate("tmpl1", template);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        assertEquals(1, profile.getSavedQueries().size());
        assertEquals(sq, profile.getSavedQueries().get("query1"));
        assertEquals(0, profile.getSavedBags().size());
        assertEquals(1, profile.getSavedTemplates().size());
        assertEquals(template, profile.getSavedTemplates().get("tmpl1"));
    }

    public void testDeleteWithManager() throws Exception {
        Profile profile = new Profile(profileManager, "bob", bobId, "pass",
                                      new HashMap(), new HashMap(), new HashMap(), true, false);

        try {
            profile.deleteQuery("query1");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

//        try {
//            profile.deleteBag("bag1");
//            fail("Expected UnsupportedOperationException");
//        } catch (UnsupportedOperationException e) {
//        }

        try {
            profile.deleteTemplate("tmpl1", null, false);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        assertEquals(0, profile.getSavedQueries().size());
        assertEquals(0, profile.getSavedBags().size());
        assertEquals(0, profile.getSavedTemplates().size());

    }

    class DummyProfileManager extends ProfileManager
    {
        public DummyProfileManager()
            throws ObjectStoreException {
            super(objectstoreOS, userprofileOS);
        }
        public void saveProfile(Profile profile) {
            throw new UnsupportedOperationException();
        }
    }
}
