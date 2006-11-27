package org.intermine.web.history;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.web.Constants;
import org.intermine.web.Constraint;
import org.intermine.web.PathQuery;
import org.intermine.web.Profile;
import org.intermine.web.SavedQuery;
import org.intermine.web.SessionMethods;
import org.intermine.web.TemplateQuery;
//import org.intermine.web.bag.InterMineBag;
//import org.intermine.web.bag.InterMinePrimitiveBag;

import servletunit.struts.MockStrutsTestCase;

public class ModifyQueryActionTest extends MockStrutsTestCase
{
    /*
    PathQuery query, queryBag;
    SavedQuery sq, sqBag, hist, hist2;
    Date date = new Date();
    InterMineBag bag, bag2;
    TemplateQuery template;
    ObjectStoreDummyImpl userprofileOS = new ObjectStoreDummyImpl();
    Integer userId;

    public ModifyQueryActionTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();

        userprofileOS.setModel(Model.getInstanceByName("userprofile"));
        query = new PathQuery(Model.getInstanceByName("testmodel"));
        query.setView(Arrays.asList(new String[]{"Employee", "Employee.name"}));
        queryBag = new PathQuery(Model.getInstanceByName("testmodel"));
        queryBag.setView(Arrays.asList(new String[]{"Employee", "Employee.name"}));
        queryBag.addNode("Employee.name").getConstraints().add(new Constraint(ConstraintOp.IN, "bag2"));
        bag = new InterMinePrimitiveBag(userId, "bag1", userprofileOS, Collections.singleton("entry1"));
        sq = new SavedQuery("query1", date, query);
        sqBag = new SavedQuery("query3", date, queryBag);
        hist = new SavedQuery("query2", date, (PathQuery) query.clone());
        hist2 = new SavedQuery("query1", date, (PathQuery) query.clone());
        template = new TemplateQuery("template", "ttitle", "tdesc", "tcomment",
                                     new PathQuery(Model.getInstanceByName("testmodel")), false,
                                     "");

        SessionMethods.initSession(this.getSession());
        Profile profile = (Profile) getSession().getAttribute(Constants.PROFILE);
        profile.saveQuery(sq.getName(), sq);
        profile.saveQuery(sqBag.getName(), sqBag);
        profile.saveBag("bag1", bag);
        profile.saveBag("bag2", bag2);
        profile.saveHistory(hist);
        profile.saveHistory(hist2);
    }

    private Profile getProfile() {
        return (Profile) getSession().getAttribute(Constants.PROFILE);
    }

    public void testDeleteSavedQuery() {
        testDeleteNothingSelected("saved");
    }

    public void testDeleteHistory() {
        testDeleteNothingSelected("history");
    }

    public void testDeleteQuery(String type, String name) {
        addRequestParameter("selectedQueries", name);
        addRequestParameter("type", type);
        addRequestParameter("delete", "Delete");
        setRequestPathInfo("/modifyQuery");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("history");
        assertEquals(1, getProfile().getSavedQueries().size());
        assertTrue(getProfile().getSavedQueries().containsKey("query3"));
    }

    public void testDeleteSavedSelected() {
        testDeleteNothingSelected("saved");
    }

    public void testDeleteHistoryNothingSelected() {
        testDeleteNothingSelected("history");
    }

    public void testDeleteNothingSelected(String type) {
        addRequestParameter("type", type);
        addRequestParameter("delete", "Delete");
        setRequestPathInfo("/modifyQuery");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyQuery.none"});
        verifyForward("history");
        assertEquals(2, getProfile().getSavedQueries().size());
    }

    public void testRenameSavedQuery() {
        addRequestParameter("newName", "queryA"); // new name edit field
        addRequestParameter("name", "query1"); // hidden target bag name
        addRequestParameter("type", "saved"); //
        setRequestPathInfo("/modifyQuery");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("history");
        assertEquals(2, getProfile().getSavedQueries().size());
        assertTrue(getProfile().getSavedQueries().containsKey("queryA"));
        assertFalse(getProfile().getSavedQueries().containsKey("query1"));
    }

    public void testRenameHistory() {
        addRequestParameter("newName", "queryA"); // new name edit field
        addRequestParameter("name", "query1"); // hidden target bag name
        addRequestParameter("type", "history"); //
        setRequestPathInfo("/modifyQuery");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("history");
        assertEquals(2, getProfile().getHistory().size());
        assertTrue(getProfile().getHistory().containsKey("queryA"));
        assertFalse(getProfile().getHistory().containsKey("query1"));
    }

    public void testRenameSavedQueryNameInUse() {
        testRenameNameInUse("saved", "query1", "query3");
    }

    public void testRenameHistoryNameInUse() {
        testRenameNameInUse("history", "query1", "query2");
    }

    private void testRenameNameInUse(String type, String name, String toName) {
        addRequestParameter("newName", toName); // new name edit field
        addRequestParameter("name", name); // hidden target bag name
        addRequestParameter("type", type); //
        setRequestPathInfo("/modifyQuery");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyQuery.queryExists"});
        assertEquals("/history.do?action=rename&type=" + type + "&name=" + name, getActualForward());
        assertEquals(2, getProfile().getSavedQueries().size());
    }

    public void testRenameSavedQueryEmptyName() {
        testRenameEmptyName("saved", "query1");
    }

    public void testRenameHistoryEmptyName() {
        testRenameEmptyName("history", "query1");
    }

    private void testRenameEmptyName(String type, String queryName) {
        addRequestParameter("newName", ""); // new name edit field
        addRequestParameter("name", queryName); // hidden target bag name
        addRequestParameter("type", type); //
        setRequestPathInfo("/modifyQuery");
        actionPerform();
        verifyActionErrors(new String[]{"errors.required"});
        assertEquals("/history.do?action=rename&type=" + type + "&name=query1", getActualForward());
    }
    */
}
