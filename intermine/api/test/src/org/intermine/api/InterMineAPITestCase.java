package org.intermine.api;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryHelper;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.SharingInvite;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.BagSet;
import org.intermine.api.profile.DeletingProfileManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.InvalidBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.api.types.ClassKeys;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.PropertiesUtil;


/**
 * A TestCase that sets up a working InterMineAPI for use in TestCases that extend this class.  The
 * setUp() method creates a new InterMineAPI instance for each test with a superuser account called
 * 'superUser'/'password' already created.  The tearDown clears the main and userprofile databases.
 * @author Richard Smith
 *
 */
public class InterMineAPITestCase extends TestCase {

    private static final Logger LOG = Logger.getLogger(InterMineAPITestCase.class);

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

        // When we construct the InterMineAPI it expects to have superuser account already created
        // and the superuser.account property set.  This would be the normal application state.
        Properties props = PropertiesUtil.getProperties();
        props.put("superuser.account", "superUser");

        os = ObjectStoreFactory.getObjectStore("os.unittest");
        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");

        clearDatabase();
        clearUserprofile();

        ObjectStoreSummary oss = new ObjectStoreSummary(new Properties());
        ClassKeys classKeys = getClassKeys(os.getModel());

        InputStream configStream = getClass().getClassLoader().getResourceAsStream("bag-queries.xml");
        BagQueryConfig bagQueryConfig = BagQueryHelper.readBagQueryConfig(os.getModel(), configStream);

        ProfileManager pmTmp = new ProfileManager(os, uosw);
        pmTmp.createSuperUser("superUser", "password", null);

        String apiKey = "abcdef012345";
        Integer userId = null;
        Map<String, InvalidBag> invalidBags = new HashMap<String, InvalidBag>();
        Map<String, InterMineBag> validBags = new HashMap<String, InterMineBag>();

        testUser = new Profile(pmTmp, "testUser", userId, "password",
                               Profile.NO_QUERIES, new BagSet(validBags, invalidBags),
                               Profile.NO_TEMPLATES, apiKey, true, false);
        pmTmp.createProfile(testUser);

        String[] trackerClassNames = {
                "org.intermine.api.tracker.TemplateTracker",
                "org.intermine.api.tracker.ListTracker",
                "org.intermine.api.tracker.LoginTracker",
                "org.intermine.api.tracker.KeySearchTracker"
        };
        trackerDelegate = new TrackerDelegate(trackerClassNames, uosw);

        im = new InterMineAPI(os, uosw, classKeys, bagQueryConfig, oss, trackerDelegate, null);
    }

    public void tearDown() throws Exception {
        if (trackerDelegate != null) {
            trackerDelegate.close();
            try {
                trackerDelegate.finalize();
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (os != null) {
            clearDatabase();
        }
        if (uosw != null) {
            clearUserprofile();
            uosw.close();
        }

        // should be closed!
        im.getBagManager().close();
    }

    private void clearDatabase() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        SingletonResults res = os.executeSingleton(q, 20000, false, false, true);
        ObjectStoreWriter osw = os.getNewWriter();
        Iterator<?> resIter = res.iterator();
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
        Iterator<?> resIter = res.iterator();
        while (resIter.hasNext()) {
            UserProfile userProfile = (UserProfile) resIter.next();
            pm.deleteProfile(userProfile.getId());
        }
        Connection con = null;
        PreparedStatement stm1 = null, stm2 = null;
        try {
            // Horrible, I know, but necessary.
            con = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();

            if (DatabaseUtil.tableExists(con, SharedBagManager.SHARED_BAGS)) {
                stm1 = con.prepareStatement("DROP TABLE " + SharedBagManager.SHARED_BAGS);
                stm1.executeUpdate();
            }

            if (DatabaseUtil.tableExists(con, SharingInvite.TABLE_NAME)) {
                stm2 = con.prepareStatement("DROP TABLE " + SharingInvite.TABLE_NAME);
                stm2.executeUpdate();
            }
        } catch (Exception e) {
            LOG.error("Error dropping extra tables", e);
        } finally {
            for (Statement stm: new Statement[]{stm1, stm2}) {
                if (stm != null) {
                    try {
                        stm.close();
                    } catch (SQLException e) {
                        throw new RuntimeException("Problem closing resources", e);
                    }
                }
            }
            ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(con);
        }

    }

    private ClassKeys getClassKeys(Model model) {
        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(getClass().getClassLoader()
                    .getResourceAsStream("class_keys.properties"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        ClassKeys classKeys = ClassKeyHelper.readKeys(model, classKeyProps);
        return classKeys;
    }
}
