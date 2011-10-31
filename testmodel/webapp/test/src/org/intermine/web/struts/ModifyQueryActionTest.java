package org.intermine.web.struts;

import java.util.Date;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

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

        query.addView("Employee");
        query.addView("Employee.name");
        queryBag = new PathQuery(testmodel);

        queryBag.addView("Employee");
        queryBag.addView("Employee.name");
        queryBag.addConstraint(Constraints.in("Employee",  "bag2"));
        sq = new SavedQuery("query1", date, query);
        sqBag = new SavedQuery("query3", date, queryBag);
        hist = new SavedQuery("query2", date, (PathQuery) query.clone());
        hist2 = new SavedQuery("query1", date, query.clone());
        template = new TemplateQuery("template", "ttitle", "tdesc", new PathQuery(testmodel));

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
