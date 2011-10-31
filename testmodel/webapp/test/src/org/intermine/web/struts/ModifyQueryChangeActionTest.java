package org.intermine.web.struts;

import java.util.Date;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

import servletunit.struts.MockStrutsTestCase;

public class ModifyQueryChangeActionTest extends MockStrutsTestCase
{
    PathQuery query;
    SavedQuery sq, hist, hist2;
    Date date = new Date();
    InterMineBag bag;
    TemplateQuery template;
    ObjectStoreDummyImpl userprofileOS = new ObjectStoreDummyImpl();
    Integer userId;

    public ModifyQueryChangeActionTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();

        userprofileOS.setModel(Model.getInstanceByName("userprofile"));
        Model testmodel = Model.getInstanceByName("testmodel");
        query = new PathQuery(testmodel);

        query.addView("Employee");
        query.addView("Employee.name");
        sq = new SavedQuery("query1", date, query);
        hist = new SavedQuery("query2", date, (PathQuery) query.clone());
        hist2 = new SavedQuery("query1", date, (PathQuery) query.clone());
        template = new TemplateQuery("template", "ttitle", "tdesc", new PathQuery(testmodel));

        SessionMethods.initSession(this.getSession());
        Profile profile = (Profile) getSession().getAttribute(Constants.PROFILE);
        profile.saveQuery(sq.getName(), sq);
        profile.saveBag("bag1", bag);
        profile.saveHistory(hist);
        profile.saveHistory(hist2);
    }

    public void testLoadQuery() {
        addRequestParameter("type", "saved");
        addRequestParameter("name", "query1");
        addRequestParameter("method", "load");
        setRequestPathInfo("/modifyQueryChange");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("query");
        assertEquals(query, SessionMethods.getQuery(getSession()));
    }

    public void testLoadHistory() {
        addRequestParameter("type", "history");
        addRequestParameter("name", "query2");
        addRequestParameter("method", "load");
        setRequestPathInfo("/modifyQueryChange");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("query");
        assertEquals(query, SessionMethods.getQuery(getSession()));
    }

    public void testSave() {
        Profile profile = (Profile) getSession().getAttribute(Constants.PROFILE);
        assertEquals(1, profile.getSavedQueries().size());

        addRequestParameter("name", "query2");
        addRequestParameter("method", "save");
        setRequestPathInfo("/modifyQueryChange");
        actionPerform();
        verifyNoActionErrors();
        assertEquals("/mymine.do?action=rename&subtab=saved&name=query2", getActualForward());

        assertEquals(2, profile.getSavedQueries().size());
        assertEquals(2, profile.getHistory().size());
    }

    public void testSaveWithNameClash() {
        Profile profile = (Profile) getSession().getAttribute(Constants.PROFILE);
        assertEquals(1, profile.getSavedQueries().size());

        addRequestParameter("name", "query1");
        addRequestParameter("method", "save");
        setRequestPathInfo("/modifyQueryChange");
        actionPerform();
        verifyNoActionErrors();

        assertEquals("/mymine.do?action=rename&subtab=saved&name=query_1", getActualForward());

        assertEquals(2, profile.getSavedQueries().size());
        assertEquals(2, profile.getHistory().size());
    }
}
