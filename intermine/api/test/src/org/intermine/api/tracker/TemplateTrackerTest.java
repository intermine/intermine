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
import java.util.Map;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
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

import junit.framework.TestCase;

public class TemplateTrackerTest extends TestCase
{
    TemplateTracker templateTracker;
    TrackerDelegate trackerDelegate;
    ObjectStore os;
    ObjectStoreWriter uosw;
    Profile superUser, user;
    Connection conn;
    TemplateManager templateManager;

    protected void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        if (uosw instanceof ObjectStoreWriterInterMineImpl) {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
        }
        templateTracker = TemplateTracker.getInstance(conn);
        Map<String, Tracker> trackers = new HashMap<String, Tracker>();
        trackers.put(templateTracker.getName(), templateTracker);
        trackerDelegate = new TrackerDelegate(trackers);

        ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
        ProfileManager pm = new ProfileManager(os, uosw);
        superUser = new Profile(pm, "superuser", null, "password", new HashMap(),
                new HashMap(), new HashMap());
        user = new Profile(pm, "user", null, "password", new HashMap(),
                new HashMap(), new HashMap());
        pm.createProfile(superUser);
        pm.createProfile(user);
        pm.setSuperuser("superuser");

        createTemplates();
        templateManager = new TemplateManager(superUser, uosw.getModel());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        superUser.deleteTemplate("template1", null);
        user.deleteTemplate("template2", null);
        removeUserProfile("superuser");
        removeUserProfile("user");
        templateTracker = null;
        ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
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
        templateTracker.trackTemplate("template1", superUser, "sessionId1");
        templateTracker.trackTemplate("template1", superUser, "sessionId1");
        templateTracker.trackTemplate("template1", superUser, "sessionId2");
        templateTracker.trackTemplate("template1", user, "sessionId3");
        templateTracker.trackTemplate("template2", user, "sessionId3");
        Thread.sleep(1000);
        String sql = "SELECT COUNT(*) FROM templatetrack";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        rs.next();
        assertEquals(5, rs.getInt(1));
        rs.close();
        stm.close();
    }

   public void testGetAccessCounter() throws SQLException, InterruptedException {
        //template1 is public, template2 is not
        assertEquals(4, templateTracker.getAccessCounter().get("template1").intValue());
        assertNull(templateTracker.getAccessCounter().get("template2"));
        //delete templatetracks from the db
        String sql = "DELETE FROM templatetrack";
        Statement stm = conn.createStatement();
        stm.executeUpdate(sql);
        stm.close();
    }

    public void testGetRank() {
        assertEquals(1, templateTracker.getRank(templateManager).get("template1").intValue());
        assertNull(templateTracker.getRank(templateManager).get("template2"));
    }

}

