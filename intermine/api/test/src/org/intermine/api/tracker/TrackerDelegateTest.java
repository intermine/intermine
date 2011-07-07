package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;

import junit.framework.TestCase;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.tracker.util.ListBuildMode;
import org.intermine.api.tracker.util.TrackerUtil;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;

public class TrackerDelegateTest extends TestCase
{
    TrackerDelegate trackerDelegate;
    ObjectStore os;
    ObjectStoreWriter uosw;
    Profile superUser, user;
    ProfileManager pm;
    Connection conn;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        String[] trackerClassNames = {"org.intermine.api.tracker.TemplateTracker",
                                      "org.intermine.api.tracker.ListTracker",
                                      "org.intermine.api.tracker.LoginTracker",
                                      "org.intermine.api.tracker.KeySearchTracker"};
        trackerDelegate = new TrackerDelegate(trackerClassNames, uosw);

        pm = new ProfileManager(os, uosw);
        superUser = new Profile(pm, "superuser", null, "password", new HashMap(),
                new HashMap(), new HashMap(), null);
        user = new Profile(pm, "user", null, "password", new HashMap(),
                new HashMap(), new HashMap(), null);
        pm.createProfile(superUser);
        pm.createProfile(user);
        pm.setSuperuser("superuser");

        createTemplates();
        if (uosw instanceof ObjectStoreWriterInterMineImpl) {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getDatabase().getConnection();
        }
    }

    private void createTemplates() {
        Model model = os.getModel();
        TemplateQuery template1 = new TemplateQuery("template1", "template1", "",
                                  new PathQuery(model));
        template1.addViews("Employee.name", "Employee.age");
        PathConstraint nameCon1 = Constraints.eq("Employee.name", "EmployeeA1");
        template1.addConstraint(nameCon1);
        template1.setEditable(nameCon1, true);
        superUser.saveTemplate("template1", template1);
        TagManager tagManager = new TagManager(uosw);
        tagManager.addTag("im:public", "template1", TagTypes.TEMPLATE, "superuser");

        TemplateQuery template2 = new TemplateQuery("template2", "template2", "",
                                  new PathQuery(model));
        template1.addViews("Employee.name", "Employee.age");
        PathConstraint nameCon2 = Constraints.eq("Employee.name", "EmployeeB1");
        template1.addConstraint(nameCon2);
        template1.setEditable(nameCon2, true);
        user.saveTemplate("template2", template2);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        superUser.deleteTemplate("template1", null);
        user.deleteTemplate("template2", null);
        removeUserProfile("superuser");
        removeUserProfile("user");
        if (conn != null) {
            conn.close();
        }
        pm.close();
        trackerDelegate.close();
        trackerDelegate.finalize();
    }

    private void removeUserProfile(String username) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                              new QueryValue(username));
        q.setConstraint(sc);
        SingletonResults res = uosw.executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            uosw.delete(o);
        }
    }

    public void testTrackTemplate() throws SQLException, InterruptedException {
        trackerDelegate.trackTemplate("template1", superUser, "sessionId1");
        trackerDelegate.trackTemplate("template1", superUser, "sessionId1");
        trackerDelegate.trackTemplate("template1", superUser, "sessionId2");
        trackerDelegate.trackTemplate("template1", user, "sessionId3");
        trackerDelegate.trackTemplate("template2", user, "sessionId3");
        Thread.sleep(3000);
        String sql = "SELECT COUNT(*) FROM templatetrack";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        rs.next();
        assertEquals(5, rs.getInt(1));
        rs.close();
        stm.close();
    }

    private void deleteTrack(String tableName) throws SQLException {
        String sql = "DELETE FROM " + tableName;
        Statement stm = conn.createStatement();
        stm.executeUpdate(sql);
        stm.close();
    }
    public void testGetAccessCounter() throws SQLException, InterruptedException {
        //template1 is public, template2 not
        assertEquals(4, trackerDelegate.getAccessCounter().get("template1").intValue());
        assertNull(trackerDelegate.getAccessCounter().get("template2"));
        deleteTrack(TrackerUtil.TEMPLATE_TRACKER_TABLE);
    }

    public void testGetRank() {
        TemplateManager templateManager = new TemplateManager(superUser, uosw.getModel());
        assertEquals(1, trackerDelegate.getRank(templateManager).get("template1").intValue());
        assertNull(trackerDelegate.getRank(templateManager).get("template2"));
    }

    public void testTrackListCreation() throws SQLException, InterruptedException {
        trackerDelegate.trackListCreation("Address", 10, ListBuildMode.IDENTIFIERS, superUser, "sessionId1");
        trackerDelegate.trackListCreation("Address", 20, ListBuildMode.QUERY, superUser, "sessionId1");
        trackerDelegate.trackListCreation("Address", 30, ListBuildMode.OPERATION, superUser, "sessionId1");
        Thread.sleep(3000);
        String sql = "SELECT COUNT(*) FROM listtrack";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        rs.next();
        assertEquals(3, rs.getInt(1));
        rs.close();
        stm.close();
    }

    public void testTrackListExecution() throws SQLException, InterruptedException {
        trackerDelegate.trackListExecution("Address", 10, superUser, "sessionId1");
        trackerDelegate.trackListExecution("Address", 10, superUser, "sessionId1");
        Thread.sleep(3000);
        String sql = "SELECT COUNT(*) FROM listtrack WHERE event='EXECUTION'";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        rs.next();
        assertEquals(2, rs.getInt(1));
        rs.close();
        stm.close();
    }

    public void testGetListOperations() throws SQLException {
        assertEquals(5, trackerDelegate.getListOperations().size());
        deleteTrack(TrackerUtil.LIST_TRACKER_TABLE);
    }

    public void testTrackLogin() throws SQLException, InterruptedException {
        for (int index = 0; index < 20; index++) {
            trackerDelegate.trackLogin("user" + index);
        }
        Thread.sleep(4000);
        String sql = "SELECT COUNT(*) FROM logintrack";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        rs.next();
        assertEquals(20, rs.getInt(1));
        rs.close();
        stm.close();
    }

    public void testGetUserLogin() throws SQLException{
        assertEquals(20, trackerDelegate.getUserLogin().size());
        deleteTrack(TrackerUtil.LOGIN_TRACKER_TABLE);
    }

    public void testTrackKeywordSearch() throws SQLException, InterruptedException {
        for (int index = 0; index < 20; index++) {
            trackerDelegate.trackKeywordSearch("keyword" + index, superUser, "sessionId1");
        }
        Thread.sleep(4000);
        String sql = "SELECT COUNT(*) FROM searchtrack";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        rs.next();
        assertEquals(20, rs.getInt(1));
        rs.close();
        stm.close();
    }

    public void testGetKeywordSearches() throws SQLException{
        assertEquals(20, trackerDelegate.getKeywordSearches().size());
        deleteTrack(TrackerUtil.SEARCH_TRACKER_TABLE);
    }
}

