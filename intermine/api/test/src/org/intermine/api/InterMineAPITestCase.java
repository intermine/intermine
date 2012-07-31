package org.intermine.api;

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

import junit.framework.TestCase;

import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryHelper;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.BagSet;
import org.intermine.api.profile.DeletingProfileManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.InvalidBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.PropertiesUtil;

/**
 * A TestCase that sets up a working InterMineAPI for use in TestCases that extend this class.  The
 * setUp() method creates a new InterMineAPI instance for each test with a superuser account called
 * 'superUser'/'password' already created.  The tearDown clears the main and userprofile databases.
 * @author Richard Smith
 *
 */
public class InterMineAPITestCase extends TestCase {

    protected InterMineAPI im;
    protected ObjectStore os;
    protected ObjectStoreWriter uosw;
    protected Profile testUser;
    protected TrackerDelegate trackerDelegate;

    /**
     * @param arg
     */
    public InterMineAPITestCase(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {

        os = ObjectStoreFactory.getObjectStore("os.unittest");
        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");

        clearDatabase();
        clearUserprofile();

        ObjectStoreSummary oss = new ObjectStoreSummary(new Properties());
        Map<String, List<FieldDescriptor>> classKeys = getClassKeys(os.getModel());

        InputStream configStream = getClass().getClassLoader().getResourceAsStream("bag-queries.xml");
        BagQueryConfig bagQueryConfig = BagQueryHelper.readBagQueryConfig(os.getModel(), configStream);

        // When we construct the InterMineAPI it expects to have superuser account already created
        // and the superuser.account property set.  This would be the normal webapp state.
        Properties props = PropertiesUtil.getProperties();
        props.put("superuser.account", "superUser");

        ProfileManager pmTmp = new ProfileManager(os, uosw);
        Profile superUser = new Profile(pmTmp, "superUser", null, "password", new HashMap(),
                                        new HashMap(), new HashMap(), true, true);
        pmTmp.createProfile(superUser);

        String apiKey = "abcdef012345";
        Integer userId = null;
        Map<String, InvalidBag> invalidBags = new HashMap<String, InvalidBag>();
        Map<String, InterMineBag> validBags = new HashMap<String, InterMineBag>();

        testUser = new Profile(pmTmp, "testUser", userId, "password",
                               new HashMap(), new BagSet(validBags, invalidBags),
                               new HashMap(), apiKey, true, false);
        pmTmp.createProfile(testUser);

        String[] trackerClassNames = {"org.intermine.api.tracker.TemplateTracker",
                "org.intermine.api.tracker.ListTracker",
                "org.intermine.api.tracker.LoginTracker",
                "org.intermine.api.tracker.KeySearchTracker"};
        trackerDelegate = new TrackerDelegate(trackerClassNames, uosw);

        im = new InterMineAPI(os, uosw, classKeys, bagQueryConfig, oss, trackerDelegate, null);
    }

    public void tearDown() throws Exception {
        trackerDelegate.close();
        trackerDelegate.finalize();
        clearDatabase();
        clearUserprofile();
        uosw.close();
    }

    private void clearDatabase() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        SingletonResults res = os.executeSingleton(q, 100000, false, false, true);
        ObjectStoreWriter osw = os.getNewWriter();
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.close();
    }

    private void clearUserprofile() throws Exception {
        DeletingProfileManager pm = new DeletingProfileManager(os, uosw);
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        ObjectStore uos = uosw.getObjectStore();
        SingletonResults res = uos.executeSingleton(q, 1000, false, false, false);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            UserProfile userProfile = (UserProfile) resIter.next();
            pm.deleteProfile(userProfile.getId());
        }
    }

    private Map<String, List<FieldDescriptor>> getClassKeys(Model model) {
        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(getClass().getClassLoader()
                    .getResourceAsStream("class_keys.properties"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        Map<String, List<FieldDescriptor>> classKeys =
            ClassKeyHelper.readKeys(model, classKeyProps);
        return classKeys;
    }
}
