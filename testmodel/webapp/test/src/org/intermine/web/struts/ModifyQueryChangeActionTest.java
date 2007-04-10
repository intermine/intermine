package org.intermine.web.struts;

import java.util.Arrays;
import java.util.Date;

import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.SavedQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateQuery;

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
        query = new PathQuery(Model.getInstanceByName("testmodel"));
        query.setView(Arrays.asList(new String[]{"Employee", "Employee.name"}));
        sq = new SavedQuery("query1", date, query);
        hist = new SavedQuery("query2", date, (PathQuery) query.clone());
        hist2 = new SavedQuery("query1", date, (PathQuery) query.clone());
        template = new TemplateQuery("template", "ttitle", "tdesc", "tcomment",
                                     new PathQuery(Model.getInstanceByName("testmodel")),
                                     "");

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
        assertEquals(query, getSession().getAttribute(Constants.QUERY));
    }

    public void testLoadHistory() {
        addRequestParameter("type", "history");
        addRequestParameter("name", "query2");
        addRequestParameter("method", "load");
        setRequestPathInfo("/modifyQueryChange");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("query");
        assertEquals(query, getSession().getAttribute(Constants.QUERY));
    }

    public void testSave() {
        Profile profile = (Profile) getSession().getAttribute(Constants.PROFILE);
        assertEquals(1, profile.getSavedQueries().size());

        addRequestParameter("name", "query2");
        addRequestParameter("method", "save");
        setRequestPathInfo("/modifyQueryChange");
        actionPerform();
        verifyNoActionErrors();
        assertEquals("/mymine.do?action=rename&page=saved&name=query2", getActualForward());

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

        assertEquals("/mymine.do?action=rename&page=saved&name=query_1", getActualForward());

        assertEquals(2, profile.getSavedQueries().size());
        assertEquals(2, profile.getHistory().size());
    }
}
