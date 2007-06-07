package org.intermine.web.struts;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.SavedQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateQuery;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import servletunit.ServletContextSimulator;
import servletunit.struts.MockStrutsTestCase;


public class ModifyBagActionTest extends MockStrutsTestCase
{
    PathQuery query, queryBag;
    SavedQuery sq, sqBag, hist, hist2;
    Date date = new Date();
    InterMineBag bag2;
    TemplateQuery template;
    ObjectStoreWriter userProfileOSW;
    Integer userId;
    ProfileManager profileManager;
    ObjectStore os;

    public ModifyBagActionTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        SessionMethods.initSession(this.getSession());
        userProfileOSW =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");

        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        ServletContext servletContext = new ServletContextSimulator();
        servletContext.setAttribute(Constants.CLASS_KEYS, Collections.emptyMap());
        profileManager = new ProfileManager(os, userProfileOSW, servletContext);

        Profile profile = new Profile(profileManager, "modifyBagActionTest", userId, "pass",
                                      new HashMap(), new HashMap(), new HashMap());
        profileManager.createProfile(profile);

        //Get the realUserId
        UserProfile realProfile = new UserProfile();
        realProfile.setUsername("modifyBagActionTest");
        Set fieldNames = new HashSet();
        fieldNames.add("username");
        ObjectStore uos = userProfileOSW.getObjectStore();
        UserProfile profile4ID = (UserProfile) uos.getObjectByExample(realProfile, fieldNames);
        userId = new Integer(profile4ID.getId());

        
        HttpSession session = getSession();
        session.setAttribute(Constants.PROFILE, profile);

        Model model = Model.getInstanceByName("testmodel");
        query = new PathQuery(model);
        query.getView().add(MainHelper.makePath(model, query, "Employee"));
        query.getView().add(MainHelper.makePath(model, query, "Employee.name"));
        queryBag = new PathQuery(model);
        
        queryBag.getView().add(MainHelper.makePath(model, query, "Employee"));
        queryBag.getView().add(MainHelper.makePath(model, query, "Employee.name"));
        queryBag.addNode("Employee.name").getConstraints().add(new Constraint(ConstraintOp.IN, "bag2"));
        sq = new SavedQuery("query1", date, query);
        sqBag = new SavedQuery("query3", date, queryBag);
        hist = new SavedQuery("query2", date, (PathQuery) query.clone());
        hist2 = new SavedQuery("query1", date, (PathQuery) query.clone());
        template = new TemplateQuery("template", "ttitle", "tdesc", "tcomment",
                                     new PathQuery(model),
                                     "");

        //Profile profile = (Profile) getSession().getAttribute(Constants.PROFILE);
        profile.saveQuery(sq.getName(), sq);
        profile.saveQuery(sqBag.getName(), sqBag);
        profile.saveBag("bag2", bag2);
        profile.saveHistory(hist);
        profile.saveHistory(hist2);
    }

    public void tearDown() throws Exception {
        cleanUserProfile();
    }

    private void cleanUserProfile() throws ObjectStoreException {
        if (userProfileOSW.isInTransaction()) {
            userProfileOSW.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue("modifyBagActionTest"));
        q.setConstraint(sc);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = userProfileOSW.getObjectStore();
//         SingletonResults res = new SingletonResults(q, userProfileOSW.getObjectStore(),
//                                                     userProfileOSW.getObjectStore()
//                                                     .getSequence());

        SingletonResults res = userProfileOSW.executeSingleton(q);

        Iterator resIter = res.iterator();
        userProfileOSW.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            userProfileOSW.delete(o);
        }
        userProfileOSW.commitTransaction();
        userProfileOSW.close();
    }

    private Profile getProfile() {
        return (Profile) getSession().getAttribute(Constants.PROFILE);
    }

    public void testDeleteBag() {
        addRequestParameter("selectedBags", "bag1");
        addRequestParameter("delete", "Delete");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("bag");
        assertEquals(1, getProfile().getSavedBags().size());
        assertTrue(getProfile().getSavedBags().containsKey("bag2"));
    }

    public void testDeleteBagInUse() {
        addRequestParameter("selectedBags", "bag2");
        addRequestParameter("delete", "Delete");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"history.baginuse"});
        verifyForward("bag");
        assertEquals(1, getProfile().getSavedBags().size());
    }

    public void testDeleteNothingSelected() {
        addRequestParameter("delete", "Delete");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyBag.none"});
        verifyForward("bag");
        assertEquals(1, getProfile().getSavedBags().size());
    }

    public void testUnionNoName() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.required"});
        verifyForward("bag");
        assertEquals(1, getProfile().getSavedBags().size());
    }

    public void testIntersectNoName() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.required"});
        verifyForward("bag");
        assertEquals(1, getProfile().getSavedBags().size());
    }
}
