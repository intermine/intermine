package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMinePrimitiveBag;

public class ProfileTest extends TestCase
{
    PathQuery query;
    SavedQuery sq;
    Date date = new Date();
    InterMineBag bag;
    TemplateQuery template;
    
    public ProfileTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        query = new PathQuery(Model.getInstanceByName("testmodel"));
        bag = new InterMinePrimitiveBag();
        sq = new SavedQuery("query1", date, query);
        template = new TemplateQuery("template", "tdesc", "tcat",
                                     new PathQuery(Model.getInstanceByName("testmodel")), false,
                                     "");
    }

    public void testModifySavedMaps() throws Exception {
        Profile profile = new Profile(null, "bob", "pass",
                                      new HashMap(), new HashMap(), new HashMap());

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
        Profile profile = new Profile(null, "bob", "pass",
                                      new HashMap(), new HashMap(), new HashMap());
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
        
        Profile profile = new Profile(null, "bob", "pass", queries, bags, tmpls);
        profile.deleteQuery("query1");
        profile.deleteBag("bag1");
        profile.deleteTemplate("tmpl1");

        assertEquals(0, profile.getSavedQueries().size());
        assertEquals(0, profile.getSavedBags().size());
        assertEquals(0, profile.getSavedTemplates().size());
    }

    public void testSaveWithManager() throws Exception {
        ProfileManager profileManager = new DummyProfileManager(null);
        Profile profile = new Profile(profileManager, "bob", "pass",
                                      new HashMap(), new HashMap(), new HashMap());

        try {
            profile.saveQuery("query1", sq);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            profile.saveBag("bag1", bag);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        
        try {
            profile.saveTemplate("tmpl1", template);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        assertEquals(1, profile.getSavedQueries().size());
        assertEquals(profile.getSavedQueries().get("query1"), sq);
        assertEquals(1, profile.getSavedBags().size());
        assertEquals(profile.getSavedBags().get("bag1"), bag);
        assertEquals(1, profile.getSavedTemplates().size());
        assertEquals(profile.getSavedTemplates().get("tmpl1"), template);
        
        profileManager.close();
    }
    
    public void testDeleteWithManager() throws Exception {
        ProfileManager profileManager = new DummyProfileManager(null);
        Profile profile = new Profile(profileManager, "bob", "pass",
                                      new HashMap(), new HashMap(), new HashMap());

        try {
            profile.deleteQuery("query1");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            profile.deleteBag("bag1");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        
        try {
            profile.deleteTemplate("tmpl1");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        assertEquals(0, profile.getSavedQueries().size());
        assertEquals(0, profile.getSavedBags().size());
        assertEquals(0, profile.getSavedTemplates().size());

        profileManager.close();
    }

    class DummyProfileManager extends ProfileManager
    {
        public DummyProfileManager(ObjectStore os) throws ObjectStoreException {
            super(os, ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test"));
        }
        
        public void saveProfile(Profile profile) {
            throw new UnsupportedOperationException();
        }
    }
}
