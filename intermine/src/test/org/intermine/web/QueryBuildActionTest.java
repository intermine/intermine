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

import org.flymine.objectstore.query.Query;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;

public class QueryBuildActionTest extends MockStrutsTestCase
{
    protected ClassDescriptor cld;

    public QueryBuildActionTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        cld = Model.getInstanceByName("testmodel").getClassDescriptorByName("org.flymine.model.testmodel.Types");
    }

    public void testSubmitSuccessful() throws Exception {
        setRequestPathInfo("/query");
        addRequestParameter("action", "Submit");
        getSession().setAttribute("cld", cld);
        actionPerform();
        assertNull(getSession().getAttribute("cld"));
        verifyForward("buildquery");
        verifyNoActionErrors();
        assertNotNull(getSession().getAttribute("query"));
    }

    // commented out because we're overriding the ActionForm reset() method in QueryActionForm,
    // which is called before the form is displayed, clearing anything we set in preparation for
    // testing. The alternative is to use our own reset (clear()?) method and call it explicitly
    // in QueryAction.

//     public void testSubmitSuccessfulConstraint() throws Exception {
//         setRequestPathInfo("/query");
//         addRequestParameter("action", "Submit");
//         QueryBuildForm queryBuildForm = new QueryBuildForm();
//         queryBuildForm.setFieldValue("name", "bob");
//         setActionForm(queryBuildForm);
//         getSession().setAttribute("cld", new DisplayClassDescriptor(cld));
//         actionPerform();
//         queryBuildForm = (QueryBuildForm) getActionForm();
//         assertNull(queryBuildForm.getFieldValue("name"));
//         verifyForward("buildquery");
//         verifyNoActionErrors();
//         System.out.println(getSession().getAttribute("query").toString());
//         assertEquals("SELECT  FROM org.flymine.model.testmodel.Types AS a1_ WHERE (a1_.name = 'bob')", getSession().getAttribute("query").toString());
//     }

     public void testSubmitUnparseable() {
         setRequestPathInfo("/query");
         addRequestParameter("action", "Submit");
         QueryBuildForm queryBuildForm = new QueryBuildForm();
         queryBuildForm.setFieldValue("dateObjType", "not_a_date");
         setActionForm(queryBuildForm);
         getSession().setAttribute("cld", cld);
         actionPerform();
         verifyForward("error");
         //current behaviour is to create queryclass but not touch its constraints
         assertNull(((Query) getSession().getAttribute("query")).getConstraint());
     }
}
