package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.Reader;
import java.io.InputStreamReader;

import org.apache.struts.action.ActionErrors;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.web.bag.InterMineBag;

import junit.framework.TestCase;

public class TemplateHelperTest extends TestCase
{
    Map templates;

    public void setUp() {
        TemplateQueryBinding binding = new TemplateQueryBinding();
        Reader reader = new InputStreamReader(TemplateHelper.class.getClassLoader().getResourceAsStream("WEB-INF/classes/default-template-queries.xml"));
        templates = binding.unmarshal(reader);
    }
    
    public void testPrecomputeQuery() throws Exception {
        Iterator i = templates.keySet().iterator();
        TemplateQuery t = (TemplateQuery) templates.get("employeeByName");
        String expIql =
            "SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_";
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee Employee.name\"><node path=\"Employee\" type=\"Employee\"></node></query>";
        Map pathToQueryNode = new HashMap();
        MainHelper.makeQuery(PathQuery.fromXml(queryXml), new HashMap(), pathToQueryNode);
        List indexes = new ArrayList();
        String precomputeQuery = TemplateHelper.getPrecomputeQuery(t, indexes).toString();
        assertEquals(expIql, precomputeQuery);
        assertTrue(indexes.size() == 1);
        System.out.println("pathToQueryNode: " + pathToQueryNode);
        List expIndexes = new ArrayList(Collections.singleton(pathToQueryNode.get("Employee.name")));
        assertEquals(expIndexes.toString(), indexes.toString());
    }
    
    public void testTemplateFormToTemplateQuerySimple() throws Exception {
        // Set EmployeeName != "EmployeeA1"
        TemplateQuery template = (TemplateQuery) templates.get("employeeByName");

        TemplateForm tf = new TemplateForm();
        tf.setAttributeOps("1", "" + ConstraintOp.NOT_EQUALS.getIndex());
        tf.setAttributeValues("1", "EmployeeA1");
        tf.parseAttributeValues(template, null, new ActionErrors(), false);

        TemplateQuery expected = (TemplateQuery) template.clone();
        PathNode tmpNode = (PathNode) expected.getEditableNodes().get(0);
        PathNode node = (PathNode) expected.getNodes().get(tmpNode.getPath());
        Constraint c = node.getConstraint(0);
        node.getConstraints().set(0, new Constraint(ConstraintOp.NOT_EQUALS,
                "EmployeeA1", true, c.getDescription(), c.getCode(), c.getIdentifier()));
        expected.setEdited(true);
        
        TemplateQuery actual = TemplateHelper.templateFormToTemplateQuery(tf, template, new HashMap());
        assertEquals(expected.toXml(), actual.toXml());
    }
    
    public void testTemplateFormToTemplateQueryIdBag() throws Exception {
        TemplateQuery template = (TemplateQuery) templates.get("employeeByName");

        TemplateForm tf = new TemplateForm();
        tf.setUseBagConstraint("1", true);
        tf.setBagOp("1", "" + ConstraintOp.IN.getIndex());
        tf.setBag("1", "bag1");
        tf.parseAttributeValues(template, null, new ActionErrors(), false);
        InterMineBag bag1 = new InterMineBag(new Integer(101), "bag1", "Employee", 1, null, null);
        Map savedBags = new HashMap();
        savedBags.put("bag1", bag1);
        
        TemplateQuery expected = (TemplateQuery) template.clone();
        PathNode tmpNode = (PathNode) expected.getEditableNodes().get(0);
        PathNode node = (PathNode) expected.getNodes().get(tmpNode.getPath());
        PathNode parent = (PathNode) expected.getNodes().get(node.getParent().getPath());
        Constraint c = node.getConstraint(0);
        Constraint cc = new Constraint(ConstraintOp.IN,
                "bag1", true, c.getDescription(), c.getCode(), c.getIdentifier());
        expected.getNodes().remove(node.getPath());
        parent.getConstraints().add(cc);
        expected.setEdited(true);
        TemplateQuery actual = TemplateHelper.templateFormToTemplateQuery(tf, template, savedBags);
        assertEquals(expected.toXml(), actual.toXml());
    }
}
