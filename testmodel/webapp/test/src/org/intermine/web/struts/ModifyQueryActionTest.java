package org.intermine.web.struts;

import java.util.Arrays;
import java.util.Date;

import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.SavedQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateQuery;

import servletunit.struts.MockStrutsTestCase;

public class ModifyQueryActionTest extends MockStrutsTestCase
{
    PathQuery query, queryBag;
    SavedQuery sq, sqBag, hist, hist2;
    Date date = new Date();
    TemplateQuery template;
    ObjectStoreDummyImpl userprofileOS = new ObjectStoreDummyImpl();
    Integer userId;

    public ModifyQueryActionTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();

        userprofileOS.setModel(Model.getInstanceByName("userprofile"));
        Model testmodel = Model.getInstanceByName("testmodel");
        query = new PathQuery(testmodel);
        
        query.getView().add(MainHelper.makePath(testmodel, query, "Employee"));
        query.getView().add(MainHelper.makePath(testmodel, query, "Employee.name"));
        queryBag = new PathQuery(testmodel);
        
        queryBag.getView().add(MainHelper.makePath(testmodel, query, "Employee"));
        queryBag.getView().add(MainHelper.makePath(testmodel, query, "Employee.name"));
        queryBag.addNode("Employee.name").getConstraints().add(new Constraint(ConstraintOp.IN, "bag2"));
        sq = new SavedQuery("query1", date, query);
        sqBag = new SavedQuery("query3", date, queryBag);
        hist = new SavedQuery("query2", date, (PathQuery) query.clone());
        hist2 = new SavedQuery("query1", date, query.clone());
        template = new TemplateQuery("template", "ttitle", "tdesc", "tcomment",
                                     new PathQuery(testmodel),
                                     "");

        SessionMethods.initSession(this.getSession());
        Profile profile = (Profile) getSession().getAttribute(Constants.PROFILE);
        profile.saveQuery(sq.getName(), sq);
        profile.saveQuery(sqBag.getName(), sqBag);
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
        verifyActionErrors(new String[]{"errors.modifyQuery.noselect"});
        verifyForward("history");
        assertEquals(2, getProfile().getSavedQueries().size());
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

}
