package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

public class ProfileTest extends TestCase
{
    PathQuery query;
    InterMineBag bag;
    
    public ProfileTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        query = new PathQuery(Model.getInstanceByName("testmodel"));
        bag = new InterMineBag();
    }

    public void testModifySavedMaps() throws Exception {
        Profile profile = new Profile(null, "bob", new HashMap(), new HashMap());

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
        Profile profile = new Profile(null, "bob", new HashMap(), new HashMap());
        profile.saveQuery("query1", query);
        profile.saveBag("bag1", bag);

        assertEquals(1, profile.getSavedQueries().size());
        assertEquals(profile.getSavedQueries().get("query1"), query);
        assertEquals(1, profile.getSavedBags().size());
        assertEquals(profile.getSavedBags().get("bag1"), bag);
    }

    public void testDeleteNoManager() throws Exception {
        Map queries = new HashMap();
        queries.put("query1", query);
        Map bags = new HashMap();
        bags.put("bag1", bag);

        Profile profile = new Profile(null, "bob", queries, bags);
        profile.deleteQuery("query1");
        profile.deleteBag("bag1");

        assertEquals(0, profile.getSavedQueries().size());
        assertEquals(0, profile.getSavedBags().size());
    }

    public void testSaveWithManager() throws Exception {
        ProfileManager profileManager = new DummyProfileManager(null);
        Profile profile = new Profile(profileManager, "bob", new HashMap(), new HashMap());

        try {
            profile.saveQuery("query1", query);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            profile.saveBag("bag1", bag);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        assertEquals(1, profile.getSavedQueries().size());
        assertEquals(profile.getSavedQueries().get("query1"), query);
        assertEquals(1, profile.getSavedBags().size());
        assertEquals(profile.getSavedBags().get("bag1"), bag);

        profileManager.close();
    }
    
    public void testDeleteWithManager() throws Exception {
        ProfileManager profileManager = new DummyProfileManager(null);
        Profile profile = new Profile(profileManager, "bob", new HashMap(), new HashMap());

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

        assertEquals(0, profile.getSavedQueries().size());
        assertEquals(0, profile.getSavedBags().size());

        profileManager.close();
    }

    class DummyProfileManager extends ProfileManager
    {
        public DummyProfileManager(ObjectStore os) throws ObjectStoreException {
            super(os);
        }
        
        public void saveProfile(Profile profile) {
            throw new UnsupportedOperationException();
        }
    }
}
