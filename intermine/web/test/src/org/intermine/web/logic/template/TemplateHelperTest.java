package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.struts.action.ActionErrors;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.xml.TemplateQueryBinding;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.struts.TemplateForm;

public class TemplateHelperTest extends TestCase
{
    private Map templates;

    public void setUp() throws Exception {
        super.setUp();
        TemplateQueryBinding binding = new TemplateQueryBinding();
        Reader reader = new InputStreamReader(TemplateHelper.class.getClassLoader().getResourceAsStream("default-template-queries.xml"));
        templates = binding.unmarshal(reader, new HashMap(), PathQuery.USERPROFILE_VERSION);
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
        PathNode node = (PathNode) expected.getNodes().get(tmpNode.getPathString());
        Constraint c = node.getConstraint(0);
        node.getConstraints().set(0, new Constraint(ConstraintOp.NOT_EQUALS,
                "EmployeeA1", true, c.getDescription(), c.getCode(), c.getIdentifier(), null));
        expected.setEdited(true);

        TemplateQuery actual = TemplateHelper.templateFormToTemplateQuery(tf, template, new HashMap());
        assertEquals(expected.toXml(PathQuery.USERPROFILE_VERSION), actual.toXml(PathQuery.USERPROFILE_VERSION));
    }

}
