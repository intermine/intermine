package org.intermine.web.history;

import java.util.Arrays;
import java.util.Date;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.web.Constants;
import org.intermine.web.Constraint;
import org.intermine.web.PathQuery;
import org.intermine.web.Profile;
import org.intermine.web.SavedQuery;
import org.intermine.web.SessionMethods;
import org.intermine.web.TemplateQuery;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMineIdBag;
import org.intermine.web.bag.InterMinePrimitiveBag;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;

import servletunit.struts.MockStrutsTestCase;

public class ModifyBagActionTest extends MockStrutsTestCase
{
    PathQuery query, queryBag;
    SavedQuery sq, sqBag, hist, hist2;
    Date date = new Date();
    InterMineBag bag, bag2;
    TemplateQuery template;
    
    public ModifyBagActionTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        
        query = new PathQuery(Model.getInstanceByName("testmodel"));
        query.setView(Arrays.asList(new String[]{"Employee", "Employee.name"}));
        queryBag = new PathQuery(Model.getInstanceByName("testmodel"));
        queryBag.setView(Arrays.asList(new String[]{"Employee", "Employee.name"}));
        queryBag.addNode("Employee.name").getConstraints().add(new Constraint(ConstraintOp.IN, "bag2"));
        bag = new InterMinePrimitiveBag();
        bag2 = new InterMineIdBag();
        sq = new SavedQuery("query1", date, query);
        sqBag = new SavedQuery("query3", date, queryBag);
        hist = new SavedQuery("query2", date, (PathQuery) query.clone());
        hist2 = new SavedQuery("query1", date, (PathQuery) query.clone());
        template = new TemplateQuery("template", "tdesc",
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
    
    public void testDeleteBag() {
        addRequestParameter("selectedBags", "bag1");
        addRequestParameter("delete", "Delete");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("history");
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
        verifyForward("history");
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testDeleteNothingSelected() {
        addRequestParameter("delete", "Delete");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyBag.none"});
        verifyForward("history");
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testRenameBag() {
        addRequestParameter("newBagName", ""); // always there
        addRequestParameter("newName", "bagA"); // new name edit field
        addRequestParameter("name", "bag1"); // hidden target bag name
        addRequestParameter("type", "bag"); // 
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("history");
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testRenameBagNameInUse() {
        addRequestParameter("newBagName", ""); // always there
        addRequestParameter("newName", "bag2"); // new name edit field
        addRequestParameter("name", "bag1"); // hidden target bag name
        addRequestParameter("type", "bag"); // 
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyQuery.queryExists"});
        assertEquals("/history.do?action=rename&type=bag&name=bag1", getActualForward());
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testRenameBagEmptyName() {
        addRequestParameter("newBagName", ""); // always there
        addRequestParameter("newName", ""); // new name edit field
        addRequestParameter("name", "bag1"); // hidden target bag name
        addRequestParameter("type", "bag"); // 
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.required"});
        assertEquals("/history.do?action=rename&type=bag&name=bag1", getActualForward());
    }
    
    public void testUnionConflictingTypes() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"bag.typesDontMatch"});
        verifyForward("history");
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testIntersectConflictingTypes() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"bag.typesDontMatch"});
        verifyForward("history");
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testUnionConflictingTypesNothingSelected() {
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyBag.none"});
        verifyForward("history");
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testIntersectConflictingTypesNothingSelected() {
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyBag.none"});
        verifyForward("history");
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testUnionNoName() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.required"});
        verifyForward("history");
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testIntersectNoName() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.required"});
        verifyForward("history");
        assertEquals(2, getProfile().getSavedBags().size());
    }
    
    public void testIntersectPrimitiveBags() {
        InterMinePrimitiveBag bag = new InterMinePrimitiveBag();
        getProfile().saveBag("bag3", bag);
        addRequestParameter("selectedBags", new String[]{"bag1", "bag3"});
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("history");
        assertEquals(4, getProfile().getSavedBags().size());
    }
    
    public void testIntersectObjectBags() {
        InterMineIdBag bag = new InterMineIdBag();
        getProfile().saveBag("bag3", bag);
        addRequestParameter("selectedBags", new String[]{"bag2", "bag3"});
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("history");
        assertEquals(4, getProfile().getSavedBags().size());
    }
    
    public void testUnionPrimitiveBags() {
        InterMinePrimitiveBag bag = new InterMinePrimitiveBag();
        getProfile().saveBag("bag3", bag);
        addRequestParameter("selectedBags", new String[]{"bag1", "bag3"});
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("history");
        assertEquals(4, getProfile().getSavedBags().size());
    }
    
    public void testUnionObjectBags() {
        InterMineIdBag bag = new InterMineIdBag();
        getProfile().saveBag("bag3", bag);
        addRequestParameter("selectedBags", new String[]{"bag2", "bag3"});
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("history");
        assertEquals(4, getProfile().getSavedBags().size());
    }
}
