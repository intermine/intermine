package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import servletunit.struts.MockStrutsTestCase;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.HashMap;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.model.testmodel.Employee;
import org.flymine.model.testmodel.Types;

import org.apache.log4j.Logger;

public class QueryBuildActionTest extends MockStrutsTestCase
{
    protected static final Logger LOG = Logger.getLogger(QueryBuildActionTest.class);

    protected Model model;
    protected ClassDescriptor cld;

    public QueryBuildActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
        cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Types");
    }

    public void testAddNoConstraint() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add to query");
        session.setAttribute("query", new Query());
        session.setAttribute("queryClass", new QueryClass(Employee.class));
        session.setAttribute("model", new DisplayModel(model));

        QueryBuildForm form = new QueryBuildForm();
        setActionForm(form);

        actionPerform();

        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNotNull(session.getAttribute("query"));
        assertNull(session.getAttribute("queryClass"));
        assertNull(session.getAttribute("constraints"));
        LOG.info("testAddNoConstraint(): query: " + ((Query) session.getAttribute("query")));
        assertEquals(1, ((Query) session.getAttribute("query")).getFrom().size());
    }

    public void testAddConstraint() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add to query");
        session.setAttribute("query", new Query());
        session.setAttribute("queryClass", new QueryClass(Employee.class));
        session.setAttribute("model", new DisplayModel(model));

        QueryBuildForm form = new QueryBuildForm();
        setActionForm(form);

        form.setFieldValue("name_1", "Dave");
        form.setFieldOp("name_1", ConstraintOp.EQUALS.getIndex().toString());

        actionPerform();

        LOG.info("testAddConstraint(): query: " + ((Query) session.getAttribute("query")));

        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNotNull(session.getAttribute("query"));
        assertNull(session.getAttribute("queryClass"));
        assertNull(session.getAttribute("constraints"));
        assertEquals(1, ((Query) session.getAttribute("query")).getFrom().size());
        assertEquals(1, ((Query) session.getAttribute("query")).getFrom().size());
    }

    public void testAddSuccessfulNoQuery() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add to query");
        session.setAttribute("queryClass", new QueryClass(Employee.class));
        session.setAttribute("model", new DisplayModel(model));

        QueryBuildForm form = new QueryBuildForm();
        form.setFieldValue("name_1", "Dave");
        form.setFieldOp("name_1", ConstraintOp.EQUALS.getIndex().toString());
        setActionForm(form);

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNotNull(session.getAttribute("query"));
        assertNull(session.getAttribute("queryClass"));
        assertNull(session.getAttribute("constraints"));
        assertEquals(1, ((Query) session.getAttribute("query")).getFrom().size());
    }

    public void testAddNoModel() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add to query");
        session.setAttribute("queryClass", new QueryClass(Employee.class));
        session.setAttribute("query", new Query());

        QueryBuildForm form = new QueryBuildForm();
        form.setFieldValue("name_1", "Dave");
        form.setFieldOp("name_1", ConstraintOp.EQUALS.getIndex().toString());
        setActionForm(form);

        actionPerform();
        verifyForward("error");
        verifyActionErrors(new String[] {"exception.message"});
        assertNotNull(getSession().getAttribute("query"));
        assertNotNull(getSession().getAttribute("queryClass"));
    }

    public void testAddNoQueryClass() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add to query");
        session.setAttribute("query", new Query());
        session.setAttribute("model", new DisplayModel(model));

        actionPerform();
        verifyForward("error");
        assertNotNull(session.getAttribute("query"));
        assertNull(session.getAttribute("queryClass"));
    }


    // test that we report an error if the user puts something unparsable in
    // a constraint value field
    public void testAddUnparseable() {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add to query");
        session.setAttribute("query", new Query());
        session.setAttribute("model", new DisplayModel(model));
        session.setAttribute("queryClass", new QueryClass(Types.class));

        QueryBuildForm queryBuildForm = new QueryBuildForm();
        queryBuildForm.setFieldValue("floatType_0", "can't parse this");
        queryBuildForm.setFieldOp("floatType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("doubleType_0", "can't parse this");
        queryBuildForm.setFieldOp("doubleType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("shortType_0", "can't parse this");
        queryBuildForm.setFieldOp("shortType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("intType_0", "can't parse this");
        queryBuildForm.setFieldOp("intType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("longType_0", "can't parse this");
        queryBuildForm.setFieldOp("longType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("floatObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("floatObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("doubleObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("doubleObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("shortObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("shortObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("intObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("intObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("longObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("longObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("bigDecimalObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("bigDecimalObjType_0",
                                  ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("dateObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("dateObjType_0",
                                  ConstraintOp.EQUALS.getIndex().toString());

        setActionForm(queryBuildForm);

        actionPerform();
        verifyForward("buildquery");
        assertNotNull(session.getAttribute("query"));
        assertNotNull(getRequest().getAttribute("constraintErrors"));

        Map constraintErrors = (Map) getRequest().getAttribute("constraintErrors");

        assertNotNull(constraintErrors.get("doubleType_0"));
        assertNotNull(constraintErrors.get("shortType_0"));
        assertNotNull(constraintErrors.get("intType_0"));
        assertNotNull(constraintErrors.get("longType_0"));
        assertNotNull(constraintErrors.get("floatObjType_0"));
        assertNotNull(constraintErrors.get("doubleObjType_0"));
        assertNotNull(constraintErrors.get("shortObjType_0"));
        assertNotNull(constraintErrors.get("intObjType_0"));
        assertNotNull(constraintErrors.get("longObjType_0"));
        assertNotNull(constraintErrors.get("bigDecimalObjType_0"));
        assertNotNull(constraintErrors.get("dateObjType_0"));

        assertNotNull(session.getAttribute("queryClass"));
    }


    // test that the constraintErrors attribute it cleared
    public void testAddUnparseable() {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add to query");
        session.setAttribute("query", new Query());
        session.setAttribute("model", new DisplayModel(model));
        session.setAttribute("queryClass", new QueryClass(Types.class));

        QueryBuildForm queryBuildForm = new QueryBuildForm();
        queryBuildForm.setFieldValue("floatType_0", "can't parse this");
        queryBuildForm.setFieldOp("floatType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("doubleType_0", "can't parse this");
        queryBuildForm.setFieldOp("doubleType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("shortType_0", "can't parse this");
        queryBuildForm.setFieldOp("shortType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("intType_0", "can't parse this");
        queryBuildForm.setFieldOp("intType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("longType_0", "can't parse this");
        queryBuildForm.setFieldOp("longType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("floatObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("floatObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("doubleObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("doubleObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("shortObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("shortObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("intObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("intObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("longObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("longObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("bigDecimalObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("bigDecimalObjType_0",
                                  ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("dateObjType_0", "can't parse this");
        queryBuildForm.setFieldOp("dateObjType_0",
                                  ConstraintOp.EQUALS.getIndex().toString());

        setActionForm(queryBuildForm);

        actionPerform();
        verifyForward("buildquery");
        assertNotNull(session.getAttribute("query"));
        assertNotNull(getRequest().getAttribute("constraintErrors"));

        Map constraintErrors = (Map) getRequest().getAttribute("constraintErrors");

        assertNotNull(constraintErrors.get("doubleType_0"));
        assertNotNull(constraintErrors.get("shortType_0"));
        assertNotNull(constraintErrors.get("intType_0"));
        assertNotNull(constraintErrors.get("longType_0"));
        assertNotNull(constraintErrors.get("floatObjType_0"));
        assertNotNull(constraintErrors.get("doubleObjType_0"));
        assertNotNull(constraintErrors.get("shortObjType_0"));
        assertNotNull(constraintErrors.get("intObjType_0"));
        assertNotNull(constraintErrors.get("longObjType_0"));
        assertNotNull(constraintErrors.get("bigDecimalObjType_0"));
        assertNotNull(constraintErrors.get("dateObjType_0"));

        assertNotNull(session.getAttribute("queryClass"));
    }

    public void testAddIncompleteConstraint() {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add constraint");
        session.setAttribute("constraints", new HashMap());

        QueryBuildForm queryBuildForm = new QueryBuildForm();
        queryBuildForm.setNewFieldName("name");
        setActionForm(queryBuildForm);

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();

        Map expected = new HashMap();
        expected.put("name_0", "name");

        assertEquals(expected, session.getAttribute("constraints"));
    }
}
