package org.intermine.web.struts;

import java.util.Date;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.OldPathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

import servletunit.struts.MockStrutsTestCase;

public class ModifyQueryActionTest extends MockStrutsTestCase
{
    OldPathQuery query, queryBag;
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
        query = new OldPathQuery(testmodel);

        query.getView().add(OldPathQuery.makePath(testmodel, query, "Employee"));
        query.getView().add(OldPathQuery.makePath(testmodel, query, "Employee.name"));
        queryBag = new OldPathQuery(testmodel);

        queryBag.getView().add(OldPathQuery.makePath(testmodel, query, "Employee"));
        queryBag.getView().add(OldPathQuery.makePath(testmodel, query, "Employee.name"));
        queryBag.addNode("Employee.name").getConstraints().add(new Constraint(ConstraintOp.IN, "bag2"));
        sq = new SavedQuery("query1", date, query);
        sqBag = new SavedQuery("query3", date, queryBag);
        hist = new SavedQuery("query2", date, (OldPathQuery) query.clone());
        hist2 = new SavedQuery("query1", date, query.clone());
        template = new TemplateQuery("template", "ttitle", "tdesc", "tcomment",
                new OldPathQuery(testmodel));

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
