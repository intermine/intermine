package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import servletunit.struts.MockStrutsTestCase;
import org.apache.struts.tiles.ComponentContext;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.fql.FqlQuery;
import org.intermine.objectstore.query.fql.FqlQueryParser;
import org.intermine.objectstore.query.presentation.PrintableConstraint;

public class QueryViewControllerTest extends MockStrutsTestCase
{
    public QueryViewControllerTest(String arg1) {
        super(arg1);
    }

    public void test1() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initQueryView");
        getRequest().getSession().setAttribute(Constants.QUERY, new Query());
        actionPerform();
        assertEquals(Collections.EMPTY_MAP, context.getAttribute("perFromConstraints"));
        assertEquals(Collections.EMPTY_SET, context.getAttribute("noFromConstraints"));
        assertEquals(Collections.EMPTY_MAP, context.getAttribute("perFromTitle"));
        assertEquals(Collections.EMPTY_MAP, context.getAttribute("perFromAlias"));
        /*
        String errorMessage = "";
        boolean firstTime = true;
        Enumeration atEnum = getRequest().getAttributeNames();
        while (atEnum.hasMoreElements()) {
            errorMessage += (firstTime ? "request = (" : ", ");
            firstTime = false;
            String atName = (String) atEnum.nextElement();
            errorMessage += atName + " = \"" + getRequest().getAttribute(atName) + "\"";
        }
        errorMessage += ")";
        throw new Exception(errorMessage);*/
    }


    public void test2() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initQueryView");
        getRequest().getSession().setAttribute(Constants.QUERY, null);
        actionPerform();
        assertEquals(Collections.EMPTY_MAP, context.getAttribute("perFromConstraints"));
        assertEquals(Collections.EMPTY_SET, context.getAttribute("noFromConstraints"));
        assertEquals(Collections.EMPTY_MAP, context.getAttribute("perFromTitle"));
        assertEquals(Collections.EMPTY_MAP, context.getAttribute("perFromAlias"));
    }

    public void test3() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initQueryView");
        Query q = FqlQueryParser.parse(new FqlQuery("select a from Company as a", "org.intermine.model.testmodel"));
        getRequest().getSession().setAttribute(Constants.QUERY, q);
        actionPerform();
        FromElement from1 = (FromElement) q.getFrom().iterator().next();
        Map expected1 = Collections.singletonMap(from1, new HashSet());
        Set expected2 = Collections.EMPTY_SET;
        Map expected3 = Collections.singletonMap(from1, "Company");
        Map expected4 = Collections.singletonMap(from1, "a");
        assertEquals(expected1, context.getAttribute("perFromConstraints"));
        assertEquals(expected2, context.getAttribute("noFromConstraints"));
        assertEquals(expected3, context.getAttribute("perFromTitle"));
        assertEquals(expected4, context.getAttribute("perFromAlias"));
    }

    public void test4() throws Exception {
        ComponentContext context = new ComponentContext();
        ComponentContext.setContext(context, getRequest());
        setRequestPathInfo("/initQueryView");
        Query q = new Query();
        Query subQ = new Query();
        QueryClass subQC = new QueryClass(Company.class);
        subQ.alias(subQC, "Company");
        subQ.addFrom(subQC);
        subQ.addToSelect(subQC);
        QueryClass qcA = new QueryClass(Company.class);
        QueryClass qcB = new QueryClass(Department.class);
        q.alias(qcA, "a");
        q.addFrom(qcA);
        q.addToSelect(qcA);
        q.alias(qcB, "b");
        q.addFrom(qcB);
        q.alias(subQ, "c");
        q.addFrom(subQ);
        Constraint c1 = new SimpleConstraint(new QueryField(qcA, "vatNumber"), ConstraintOp.EQUALS, new QueryValue(new Integer(5)));
        Constraint c2 = new ContainsConstraint(new QueryCollectionReference(qcA, "departments"), ConstraintOp.CONTAINS, qcB);
        Constraint c3 = new SimpleConstraint(new QueryField(subQ, subQC, "name"), ConstraintOp.EQUALS, new QueryField(qcA, "name"));
        ConstraintSet c4 = new ConstraintSet(ConstraintOp.OR);
        c4.addConstraint(new SimpleConstraint(new QueryField(qcA, "vatNumber"), ConstraintOp.EQUALS, new QueryField(subQ, subQC, "vatNumber")));
        c4.addConstraint(new SimpleConstraint(new QueryField(subQ, subQC, "vatNumber"), ConstraintOp.EQUALS, new QueryValue(new Integer(3))));
        Constraint c5 = new SimpleConstraint(new QueryField(qcB, "name"), ConstraintOp.EQUALS, new QueryValue("hello"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(c1);
        c.addConstraint(c2);
        c.addConstraint(c3);
        c.addConstraint(c4);
        c.addConstraint(c5);
        q.setConstraint(c);
        //Query q = FqlQueryParser.parse(new FqlQuery("select a from Company a, Department b, (select Company from Company) as c where a.vatNumber = 5 and a.departments CONTAINS b and c.Company.name = a.name and (a.vatNumber = c.Company.vatNumber OR c.Company.vatNumber = 3) and b.name = 'hello'", "org.intermine.model.testmodel"));
        getRequest().getSession().setAttribute(Constants.QUERY, q);
        actionPerform();
        Map expected1 = new HashMap();
        Set setA = new HashSet();
        setA.add(new PrintableConstraint(q, c1));
        setA.add(new PrintableConstraint(q, c2));
        expected1.put(qcA, setA);
        expected1.put(qcB, Collections.singleton(new PrintableConstraint(q, c5)));
        expected1.put(subQ, new HashSet());
        // c3 is a cross-reference constraint so not associated with a QueryClass
        //expected1.put(subQ, Collections.singleton(new PrintableConstraint(q, c3)));
        Set expected2 = new HashSet();
        expected2.add(new PrintableConstraint(q, c3));
        expected2.add(new PrintableConstraint(q, c4));
        Map expected3 = new HashMap();
        expected3.put(qcA, "Company");
        expected3.put(qcB, "Department");
        expected3.put(subQ, "SELECT DISTINCT Company FROM org.intermine.model.testmodel.Company AS Company");
        Map expected4 = new HashMap();
        expected4.put(qcA, "a");
        expected4.put(qcB, "b");
        expected4.put(subQ, "c");
        assertEquals(expected1, context.getAttribute("perFromConstraints"));
        assertEquals(expected2, context.getAttribute("noFromConstraints"));
        assertEquals(expected3, context.getAttribute("perFromTitle"));
        assertEquals(expected4, context.getAttribute("perFromAlias"));
    }
}
