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
import javax.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.Model;

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

    public void testEditFql() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("buttons(editFql)", "");

        String anAlias = "ClassAlias_0";
        Map queryClasses = new HashMap();
        DisplayQueryClass displayQueryClass = new DisplayQueryClass();
        displayQueryClass.setType("org.flymine.model.testmodel.Department");
        queryClasses.put(anAlias, displayQueryClass);

        session.setAttribute(Constants.EDITING_ALIAS, anAlias);
        session.setAttribute(Constants.QUERY_CLASSES, queryClasses);

        actionPerform();
        
        verifyNoActionErrors();
        verifyForward("buildfqlquery");
        assertNull(session.getAttribute(Constants.QUERY_CLASSES));
        assertNull(session.getAttribute(Constants.EDITING_ALIAS));
        assertNotNull(session.getAttribute(Constants.QUERY));
        Query q = (Query) session.getAttribute(Constants.QUERY);
        assertEquals(1, q.getFrom().size());
    }

    public void testAddConstraint() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("buttons(addConstraint)", "");

        Map queryClasses = new HashMap();

        DisplayQueryClass displayQueryClass = new DisplayQueryClass();
        displayQueryClass.setType("org.flymine.model.testmodel.Department");

        String newAlias =
            QueryBuildHelper.aliasClass(queryClasses.keySet(), displayQueryClass.getType());

        session.setAttribute(Constants.EDITING_ALIAS, newAlias);
        queryClasses.put(newAlias, displayQueryClass);
        session.setAttribute(Constants.QUERY_CLASSES, queryClasses);

        QueryBuildForm form = new QueryBuildForm() {
            public void reset(ActionMapping mapping, HttpServletRequest request) {
                // override to avoid the form being reset by Struts
            }
        };

        form.setNewFieldName("name");

        setActionForm(form);
        actionPerform();

        verifyForward("buildquery");
        verifyNoActionErrors();
        assertEquals(newAlias, session.getAttribute(Constants.EDITING_ALIAS));
        Map afterQueryClasses = (Map)session.getAttribute(Constants.QUERY_CLASSES);
        assertEquals(1, afterQueryClasses.size());
        DisplayQueryClass afterDisplayQueryClass =
            (DisplayQueryClass) afterQueryClasses.get(newAlias);
        assertNotNull(afterDisplayQueryClass);

        DisplayQueryClass expected = new DisplayQueryClass();
        expected.setType("org.flymine.model.testmodel.Department");
        expected.getConstraintNames().add("name_0");
        expected.setFieldName("name_0", "name");
        assertEquals(expected, afterDisplayQueryClass);
    }

    public void testAddNoModel() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add");

        QueryBuildForm form = new QueryBuildForm() {
            public void reset(ActionMapping mapping, HttpServletRequest request) {
                // override to avoid the form being reset by Struts
            }
        };

        form.setFieldValue("name_1", "Dave");
        form.setFieldOp("name_1", ConstraintOp.EQUALS.getIndex().toString());
        setActionForm(form);

        actionPerform();
        verifyForward("error");
    }

    public void testAddNoQueryClass() throws Exception {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("action", "Add");
        session.setAttribute(Constants.QUERY, new Query());

        actionPerform();
        verifyForward("error");
        assertNotNull(session.getAttribute(Constants.QUERY));
    }

    // test that we can parse all possible types without an error
    public void testTypes() {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("buttons(updateClass)", "");
        //this is necessary because the mock session is US locale by default
        session.setAttribute(Globals.LOCALE_KEY, Locale.getDefault());

        Map queryClasses = new HashMap();

        DisplayQueryClass displayQueryClass = new DisplayQueryClass();
        displayQueryClass.setType("org.flymine.model.testmodel.Types");

        List constraintNames = new ArrayList();

        constraintNames.add("floatType_0");
        constraintNames.add("doubleType_0");
        constraintNames.add("shortType_0");
        constraintNames.add("intType_0");
        constraintNames.add("longType_0");
        constraintNames.add("floatObjType_0");
        constraintNames.add("doubleObjType_0");
        constraintNames.add("shortObjType_0");
        constraintNames.add("intObjType_0");
        constraintNames.add("longObjType_0");
        constraintNames.add("bigDecimalObjType_0");
        constraintNames.add("dateObjType_0");

        Map nameMap = new HashMap();

        for (Iterator i = constraintNames.iterator(); i.hasNext() ; ) {
            String constraintName = (String) i.next();
            nameMap.put(constraintName, QueryBuildHelper.getFieldName(constraintName));
        }

        displayQueryClass.setConstraintNames(constraintNames);
        displayQueryClass.setFieldNames(nameMap);

        String newAlias =
            QueryBuildHelper.aliasClass(queryClasses.keySet(), displayQueryClass.getType());

        session.setAttribute(Constants.EDITING_ALIAS, newAlias);
        queryClasses.put(newAlias, displayQueryClass);
        session.setAttribute(Constants.QUERY_CLASSES, queryClasses);

        QueryBuildForm queryBuildForm = new QueryBuildForm() {
            public void reset(ActionMapping mapping, HttpServletRequest request) {
                // override to avoid the form being reset by Struts
            }
        };

        queryBuildForm.setFieldValue("floatType_0", "1.234");
        queryBuildForm.setFieldOp("floatType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("doubleType_0", "1.234");
        queryBuildForm.setFieldOp("doubleType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("shortType_0", "1234");
        queryBuildForm.setFieldOp("shortType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("intType_0", "1234");
        queryBuildForm.setFieldOp("intType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("longType_0", "1234");
        queryBuildForm.setFieldOp("longType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("floatObjType_0", "1.234");
        queryBuildForm.setFieldOp("floatObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("doubleObjType_0", "1.234");
        queryBuildForm.setFieldOp("doubleObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("shortObjType_0", "1234");
        queryBuildForm.setFieldOp("shortObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("intObjType_0", "1234");
        queryBuildForm.setFieldOp("intObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("longObjType_0", "1234");
        queryBuildForm.setFieldOp("longObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("bigDecimalObjType_0", "12345678901234567890.123456789");
        queryBuildForm.setFieldOp("bigDecimalObjType_0",
                                  ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("dateObjType_0", "21/5/04 9:30 AM");
        queryBuildForm.setFieldOp("dateObjType_0",
                                  ConstraintOp.EQUALS.getIndex().toString());

        setActionForm(queryBuildForm);

        actionPerform();

        verifyForward("buildquery");

        DisplayQueryClass expected = new DisplayQueryClass();
        expected.setType("org.flymine.model.testmodel.Types");
        expected.setConstraintNames(constraintNames);
        expected.setFieldNames(nameMap);

        expected.getFieldValues().put("floatType_0", new Float(1.234));
        expected.getFieldValues().put("doubleType_0", new Double(1.234));
        short s = 1234;
        expected.getFieldValues().put("shortType_0", new Short(s));
        expected.getFieldValues().put("intType_0", new Integer(1234));
        expected.getFieldValues().put("longType_0", new Long(1234));
        expected.getFieldValues().put("floatObjType_0", new Float(1.234));
        expected.getFieldValues().put("doubleObjType_0", new Double(1.234));
        expected.getFieldValues().put("shortObjType_0", new Short(s));
        expected.getFieldValues().put("intObjType_0", new Integer(1234));
        expected.getFieldValues().put("longObjType_0", new Long(1234));
        expected.getFieldValues().put("bigDecimalObjType_0",
                                      new BigDecimal("12345678901234567890.123456789"));
        DateFormat df = new SimpleDateFormat();
        try {
            expected.getFieldValues().put("dateObjType_0", df.parse("21/5/04 9:30 AM"));
        } catch (ParseException e) {
        }

        expected.getFieldOps().put("floatType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("doubleType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("shortType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("intType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("longType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("floatObjType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("doubleObjType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("shortObjType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("intObjType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("longObjType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("bigDecimalObjType_0", ConstraintOp.EQUALS);
        expected.getFieldOps().put("dateObjType_0", ConstraintOp.EQUALS);

        assertEquals(expected, displayQueryClass);
    }

    // test that we report an error if the user puts something unparsable in
    // a constraint value field
    public void testAddUnparseable() {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("buttons(updateClass)", "");

        Map queryClasses = new HashMap();

        DisplayQueryClass displayQueryClass = new DisplayQueryClass();
        displayQueryClass.setType("org.flymine.model.testmodel.Types");

        List constraintNames = new ArrayList();

        constraintNames.add("floatType_0");
        constraintNames.add("doubleType_0");
        constraintNames.add("shortType_0");
        constraintNames.add("intType_0");
        constraintNames.add("longType_0");
        constraintNames.add("floatObjType_0");
        constraintNames.add("doubleObjType_0");
        constraintNames.add("shortObjType_0");
        constraintNames.add("intObjType_0");
        constraintNames.add("longObjType_0");
        constraintNames.add("bigDecimalObjType_0");
        constraintNames.add("dateObjType_0");

        Map nameMap = new HashMap();

        for (Iterator i = constraintNames.iterator(); i.hasNext() ; ) {
            String constraintName = (String) i.next();
            nameMap.put(constraintName, QueryBuildHelper.getFieldName(constraintName));
        }

        displayQueryClass.setConstraintNames(constraintNames);
        displayQueryClass.setFieldNames(nameMap);

        String newAlias =
            QueryBuildHelper.aliasClass(queryClasses.keySet(), displayQueryClass.getType());

        session.setAttribute(Constants.EDITING_ALIAS, newAlias);
        queryClasses.put(newAlias, displayQueryClass);
        session.setAttribute(Constants.QUERY_CLASSES, queryClasses);

        QueryBuildForm queryBuildForm = new QueryBuildForm() {
            public void reset(ActionMapping mapping, HttpServletRequest request) {
                // override to avoid the form being reset by Struts
            }
        };

        queryBuildForm.setFieldValue("floatType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("floatType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("doubleType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("doubleType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("shortType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("shortType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("intType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("intType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("longType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("longType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("floatObjType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("floatObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("doubleObjType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("doubleObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("shortObjType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("shortObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("intObjType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("intObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("longObjType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("longObjType_0", ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("bigDecimalObjType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("bigDecimalObjType_0",
                                  ConstraintOp.EQUALS.getIndex().toString());
        queryBuildForm.setFieldValue("dateObjType_0", "a string that can't be parsed");
        queryBuildForm.setFieldOp("dateObjType_0",
                                  ConstraintOp.EQUALS.getIndex().toString());

        setActionForm(queryBuildForm);

        actionPerform();

        verifyForward("buildquery");

        ActionErrors actionErrors = (ActionErrors) getRequest().getAttribute(Globals.ERROR_KEY);

        Iterator doubleTypeIter = actionErrors.get("doubleType_0");
        assertNotNull(doubleTypeIter.next());
        assertFalse(doubleTypeIter.hasNext());

        Iterator shortTypeIter = actionErrors.get("shortType_0");
        assertNotNull(shortTypeIter.next());
        assertFalse(shortTypeIter.hasNext());

        Iterator intTypeIter = actionErrors.get("intType_0");
        assertNotNull(intTypeIter.next());
        assertFalse(intTypeIter.hasNext());

        Iterator longTypeIter = actionErrors.get("longType_0");
        assertNotNull(longTypeIter.next());
        assertFalse(longTypeIter.hasNext());

        Iterator floatObjTypeIter = actionErrors.get("floatObjType_0");
        assertNotNull(floatObjTypeIter.next());
        assertFalse(floatObjTypeIter.hasNext());

        Iterator doubleObjTypeIter = actionErrors.get("doubleObjType_0");
        assertNotNull(doubleObjTypeIter.next());
        assertFalse(doubleObjTypeIter.hasNext());

        Iterator shortObjTypeIter = actionErrors.get("shortObjType_0");
        assertNotNull(shortObjTypeIter.next());
        assertFalse(shortObjTypeIter.hasNext());

        Iterator intObjTypeIter = actionErrors.get("intObjType_0");
        assertNotNull(intObjTypeIter.next());
        assertFalse(intObjTypeIter.hasNext());

        Iterator longObjTypeIter = actionErrors.get("longObjType_0");
        assertNotNull(longObjTypeIter.next());
        assertFalse(longObjTypeIter.hasNext());

        Iterator bigDecimalObjTypeIter = actionErrors.get("bigDecimalObjType_0");
        assertNotNull(bigDecimalObjTypeIter.next());
        assertFalse(bigDecimalObjTypeIter.hasNext());

        Iterator dateObjTypeIter = actionErrors.get("dateObjType_0");
        assertNotNull(dateObjTypeIter.next());
        assertFalse(dateObjTypeIter.hasNext());

        Iterator noFieldIter = actionErrors.get("a_feild_with_no_error");
        assertFalse(noFieldIter.hasNext());
    }

    public void testRunQuery() {
        HttpSession session = getSession();
        setRequestPathInfo("/query");
        addRequestParameter("buttons(runQuery)", "");

        String anAlias = "ClassAlias_0";
        Map queryClasses = new HashMap();
        DisplayQueryClass displayQueryClass = new DisplayQueryClass();

        displayQueryClass.setType("org.flymine.model.testmodel.Department");
        queryClasses.put(anAlias, displayQueryClass);

        session.setAttribute(Constants.EDITING_ALIAS, anAlias);
        session.setAttribute(Constants.QUERY_CLASSES, queryClasses);

        actionPerform();

        verifyForward("runquery");
        assertNotNull(session.getAttribute(Constants.QUERY));
        Query q = (Query) session.getAttribute(Constants.QUERY);
        assertEquals(1, q.getFrom().size());
    }
}
