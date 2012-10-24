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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SneakyTagAdder;
import org.intermine.api.profile.TagChecker;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
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

/**
 *
 * @author Kim Rutherford
 */
public class InitialiserPluginTest extends TestCase
{
    private Profile bobProfile;
    private ProfileManager pm;
    private ObjectStore os, userProfileOS;
    private ObjectStoreWriter userProfileOSW;
    private Integer bobId = new Integer(101);
    private String bobPass = "bob_pass";

    public InitialiserPluginTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        os =  ObjectStoreFactory.getObjectStore("os.unittest");
        userProfileOSW =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        userProfileOS = userProfileOSW.getObjectStore();
        pm = new NonCheckingProfileManager(os, userProfileOSW);
    }

    private class NonCheckingProfileManager extends ProfileManager {
        public NonCheckingProfileManager(ObjectStore os, ObjectStoreWriter userProfileOSW) {
            super(os, userProfileOSW);
        }

        // override to prevent the checker from objecting to
        // "org.intermine.model.testmodel.Wibble" in testCleanTags()
        protected Map makeTagCheckers(final Model model) {
            Map checkersMap = new HashMap();
            TagChecker classChecker = new TagChecker() {
                public void isValid(String tagName, String objectIdentifier, String type,
                             UserProfile userProfile) {
                    // empty
                }
            };
            checkersMap.put("class", classChecker);
            return checkersMap;
        };

    }
    private void setUpUserProfiles() throws Exception {
        // bob's details
        String bobName = "bob";

        bobProfile = new Profile(pm, bobName, bobId, bobPass,
                                 new HashMap(), new HashMap(), new HashMap(), true, false);

        pm.createProfile(bobProfile);
    }


    public void tearDown() throws Exception {
        cleanUserProfile();

    }

    private void cleanUserProfile() throws ObjectStoreException {
        if (userProfileOSW.isInTransaction()) {
            userProfileOSW.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(Tag.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "tagName"), ConstraintOp.MATCHES, new QueryValue("test%")));
        SingletonResults res = userProfileOS.executeSingleton(q);
        Iterator resIter = res.iterator();
        userProfileOSW.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            userProfileOSW.delete(o);
        }

        removeUserProfile("bob");
        removeUserProfile("sally");

        userProfileOSW.commitTransaction();
        userProfileOSW.close();
    }

    private void removeUserProfile(String username) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(username));
        q.setConstraint(sc);
        SingletonResults res = userProfileOS.executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            userProfileOSW.delete(o);
        }
    }

    public void testCleanTags() throws Exception {
        setUpUserProfiles();
        TagManager tagManager = new TagManagerFactory(userProfileOSW).getTagManager();

        int bobsTagsClasses = tagManager.getTags(null, null, "class", "bob").size();

        Model m = Model.getInstanceByName("testmodel");
        ClassDescriptor dep = m.getClassDescriptorByName("Department");
        tagManager.addTag("test-tag1", dep, bobProfile);
        tagManager.addTag("test-tag2", dep, bobProfile);
        tagManager.addTag("test-tag2", dep, bobProfile);

        List tags = tagManager.getTags(null, null, "class", "bob");
        assertEquals(bobsTagsClasses + 3, tags.size());

        SneakyTagAdder sta = new SneakyTagAdder(tagManager);

        // test that these go away
        sta.sneakilyAddTag("test-tag", "org.intermine.model.testmodel.Wibble", "class", "bob");
        sta.sneakilyAddTag("test-tag", "org.intermine.model.testmodel.Aardvark", "class", "bob");

        InitialiserPlugin.cleanTags(tagManager);

        tags = tagManager.getTags(null, null, "class", "bob");
        assertEquals(bobsTagsClasses + 3, tags.size());
    }
}
