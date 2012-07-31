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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.tracker.util.ListBuildMode;
import org.intermine.api.tracker.util.TrackerUtil;
import org.intermine.metadata.Model;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.api.template.TemplateManager;

public class TrackerDelegateTest extends InterMineAPITestCase
{
    public TrackerDelegateTest(String arg) {
        super(arg);
        // TODO Auto-generated constructor stub
    }

    Profile superUser, testUser;
    ProfileManager pm;
    Connection conn;


    public void setUp() throws Exception {
        super.setUp();
        pm = im.getProfileManager();
        superUser = im.getProfileManager().getProfile("superUser");
        testUser = im.getProfileManager().getProfile("testUser");
        conn = ((ObjectStoreWriterInterMineImpl) uosw).getDatabase().getConnection();
        createTemplates();
    }

    public void tearDown() throws Exception {
        if (conn != null) {
            conn.close();
        }
        super.tearDown();
    }

    private void createTemplates() {
        Model model = os.getModel();
        ApiTemplate template1 = new ApiTemplate("template1", "template1", "",
                new PathQuery(model));
        template1.addViews("Employee.name", "Employee.age");
        PathConstraint nameCon1 = Constraints.eq("Employee.name", "EmployeeA1");
        template1.addConstraint(nameCon1);
        template1.setEditable(nameCon1, true);
        superUser.saveTemplate("template1", template1);
        TagManager tagManager = new TagManager(uosw);

        try {
            tagManager.addTag("im:public", "template1", TagTypes.TEMPLATE, superUser);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        ApiTemplate template2 = new ApiTemplate("template2", "template2", "",
                new PathQuery(model));
        template1.addViews("Employee.name", "Employee.age");
        PathConstraint nameCon2 = Constraints.eq("Employee.name", "EmployeeB1");
        template1.addConstraint(nameCon2);
        template1.setEditable(nameCon2, true);
        testUser.saveTemplate("template2", template2);
    }

    public void testTrackTemplate() throws SQLException, InterruptedException {
        try {
            trackerDelegate.trackTemplate("template1", superUser, "sessionId1");
            trackerDelegate.trackTemplate("template1", superUser, "sessionId1");
            trackerDelegate.trackTemplate("template1", superUser, "sessionId2");
            trackerDelegate.trackTemplate("template1", testUser, "sessionId3");
            trackerDelegate.trackTemplate("template2", testUser, "sessionId3");
            Thread.sleep(5000);
            String sql = "SELECT COUNT(*) FROM templatetrack";
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            rs.next();
            assertEquals(5, rs.getInt(1));
            rs.close();
            stm.close();

        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void deleteTrack(String tableName) throws SQLException {
        try {
            String sql = "DELETE FROM " + tableName;
            Statement stm = conn.createStatement();
            stm.executeUpdate(sql);
            stm.close();


        } finally {
            if (conn != null) {
                conn.close();
            }
        }

    }
    public void testGetAccessCounter() throws SQLException, InterruptedException {
        //template1 is public, template2 not
        try {
            assertEquals(4, trackerDelegate.getAccessCounter().get("template1").intValue());
            assertNull(trackerDelegate.getAccessCounter().get("template2"));
        } finally {
            deleteTrack(TrackerUtil.TEMPLATE_TRACKER_TABLE);
        }
    }

    public void testGetRank() {
        TemplateManager templateManager = new TemplateManager(superUser, uosw.getModel());
        assertEquals(1, trackerDelegate.getRank(templateManager).get("template1").intValue());
        assertNull(trackerDelegate.getRank(templateManager).get("template2"));
    }

    public void testTrackListCreation() throws SQLException, InterruptedException {
        try {

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


        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
    public void testTrackListExecution() throws SQLException, InterruptedException {
        try {
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


        } catch (Exception e) {

        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public void testGetListOperations() throws SQLException {
        try {
            assertEquals(5, trackerDelegate.getListOperations().size());
        } finally {
            deleteTrack(TrackerUtil.LIST_TRACKER_TABLE);
        }
    }

    public void testTrackLogin() throws SQLException, InterruptedException {
        try {
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

        } catch (Exception e) {

        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public void testGetUserLogin() throws SQLException{
        try {
            assertEquals(20, trackerDelegate.getUserLogin().size());
        } finally {
            deleteTrack(TrackerUtil.LOGIN_TRACKER_TABLE);
        }
    }

    public void testTrackKeywordSearch() throws SQLException, InterruptedException {
        try {
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

        } catch (Exception e) {

        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public void testGetKeywordSearches() throws SQLException {
        try {
            assertEquals(20, trackerDelegate.getKeywordSearches().size());
        } finally {
            deleteTrack(TrackerUtil.SEARCH_TRACKER_TABLE);
        }
    }
}

