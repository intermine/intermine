
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

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.model.testmodel.Employee;

public class QueryBuildActionTest extends MockStrutsTestCase
{
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

    public void testAddSuccessful() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add to query");
        session.setAttribute("query", new Query());
        session.setAttribute("queryClass", new QueryClass(Employee.class));
        session.setAttribute("model", new DisplayModel(model));

        QueryBuildForm form = new QueryBuildForm();
        form.setFieldValue("name", "Dave");
        form.setFieldOp("name", ConstraintOp.EQUALS.toString());
        setActionForm(form);

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNotNull(session.getAttribute("query"));
        assertNull(session.getAttribute("queryClass"));
        assertEquals(1, ((Query) session.getAttribute("query")).getFrom().size());
    }

    public void testAddSuccessfulNoQuery() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add to query");
        session.setAttribute("queryClass", new QueryClass(Employee.class));
        session.setAttribute("model", new DisplayModel(model));

        QueryBuildForm form = new QueryBuildForm();
        form.setFieldValue("name", "Dave");
        form.setFieldOp("name", ConstraintOp.EQUALS.toString());
        setActionForm(form);

        actionPerform();
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNotNull(session.getAttribute("query"));
        assertNull(session.getAttribute("queryClass"));
        assertEquals(1, ((Query) session.getAttribute("query")).getFrom().size());
    }

//     public void testAddNoModel() throws Exception {
//         HttpSession session = getSession();
//         setRequestPathInfo("/query");
//         addRequestParameter("action", "Add to query");
//         session.setAttribute("queryClass", new QueryClass(Employee.class));
//         session.setAttribute("query", new Query());

//         QueryBuildForm form = new QueryBuildForm();
//         form.setFieldValue("name", "Dave");
//         form.setFieldOp("name", ConstraintOp.EQUALS);
//         setActionForm(form);

//         actionPerform();
//         verifyForward("error");
//         verifyActionErrors(new String[] {"exception.message"});
//         assertNotNull(getSession().getAttribute("query"));
//         assertNull(getSession().getAttribute("queryClass"));
//     }

    //commented out because we're overriding the ActionForm reset() method in QueryActionForm,
    //which is called before the form is displayed, clearing anything we set in preparation for
    //testing. The alternative is to use our own reset (clear()?) method and call it explicitly
    //in QueryAction.

//     public void testSubmitSuccessfulConstraint() throws Exception {
//         setRequestPathInfo("/query");
//         addRequestParameter("action", "Submit");
//         QueryBuildForm queryBuildForm = new QueryBuildForm();
//         queryBuildForm.setFieldValue("name", "bob");
//         setActionForm(queryBuildForm);
//         getSession().setAttribute("cld", cld);
//         actionPerform();
//         queryBuildForm = (QueryBuildForm) getActionForm();
//         assertNull(queryBuildForm.getFieldValue("name"));
//         verifyForward("buildquery");
//         verifyNoActionErrors();
//         System.out.println(getSession().getAttribute("query").toString());
//         assertEquals("SELECT  FROM org.flymine.model.testmodel.Types AS a1_ WHERE (a1_.name = 'bob')", getSession().getAttribute("query").toString());
//     }

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

     public void testAddUnparseable() {
         HttpSession session = getSession();
         setRequestPathInfo("/query");
         addRequestParameter("action", "Add to query");
         session.setAttribute("query", new Query());
         session.setAttribute("model", new DisplayModel(model));
         session.setAttribute("queryClass", new QueryClass(Employee.class));

         QueryBuildForm queryBuildForm = new QueryBuildForm();
         queryBuildForm.setFieldValue("dateObjType", "not_a_date");
         setActionForm(queryBuildForm);

         actionPerform();
         verifyForward("error");
         assertNotNull(session.getAttribute("query"));
         assertNull(session.getAttribute("queryClass"));
     }
}
