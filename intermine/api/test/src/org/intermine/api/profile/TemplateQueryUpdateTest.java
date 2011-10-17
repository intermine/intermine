package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;

import junit.framework.TestCase;

public class TemplateQueryUpdateTest extends TestCase {
    private Map<String, String> renamedClasses = new HashMap<String, String>();
    private Map<String, String> renamedFields = new HashMap<String, String>();

    public TemplateQueryUpdateTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        renamedClasses.put("CEOTest", "CEO");

        renamedFields.put("Company.CEOTest", "CEO");
        renamedFields.put("CEOTest.sal", "salary");
    }
    
    public void testUpdate() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("oldtestmodel"));
        query.addView("CEOTest.name");
        query.addView("CEOTest.title");
        query.addView("CEOTest.company.name");
        query.addView("CEOTest.sal");
        query.setDescription("CEOTest.name", "CEO name");
        query.addConstraint(new PathConstraintAttribute("CEOTest.name", ConstraintOp.CONTAINS, "ploy"));
        query.addConstraint(new PathConstraintAttribute("CEOTest.company.name", ConstraintOp.CONTAINS, "pany"));
        query.addConstraint(new PathConstraintLookup("CEOTest.company.CEOTest.department.company.CEOTest", "ttt", "DepartmentA1"));
        query.setOuterJoinStatus("CEOTest.company", OuterJoinStatus.OUTER);
        query.addOrderBy("CEOTest.name", OrderDirection.ASC);
        query.addOrderBy("CEOTest.sal", OrderDirection.ASC);
        TemplateQuery templateQuery = new TemplateQuery("test", "title", "comment", query);
        
        TemplateQueryUpdate templateQueryUpdate = new TemplateQueryUpdate(templateQuery,
            Model.getInstanceByName("testmodel"), Model.getInstanceByName("oldtestmodel"));
        templateQueryUpdate.update(renamedClasses, renamedFields);
        TemplateQuery newTemplateQuery = templateQueryUpdate.getNewTemplateQuery();
        assertEquals("test", newTemplateQuery.getName());
        assertEquals("title", newTemplateQuery.getTitle());
        assertEquals("comment", newTemplateQuery.getComment());
        //verify view
        assertEquals(4, newTemplateQuery.getView().size());
        assertEquals("CEO.name", newTemplateQuery.getView().get(0));
        assertEquals("CEO.title", newTemplateQuery.getView().get(1));
        assertEquals("CEO.company.name", newTemplateQuery.getView().get(2));
        assertEquals("CEO.salary", newTemplateQuery.getView().get(3));
        //verify descriptions
        assertEquals(1, newTemplateQuery.getDescriptions().size());
        assertEquals("CEO name", newTemplateQuery.getDescription("CEO.name"));
        //verify constraint
        assertEquals(3, newTemplateQuery.getConstraints().size());
        assertEquals(1, newTemplateQuery.getConstraintsForPath("CEO.name").size());
        PathConstraintAttribute constraint1 = (PathConstraintAttribute) newTemplateQuery.getConstraintsForPath("CEO.name").get(0);
        assertEquals("ploy", constraint1.getValue());
        assertEquals(ConstraintOp.CONTAINS, constraint1.getOp().CONTAINS);
        assertEquals(1, newTemplateQuery.getConstraintsForPath("CEO.company.name").size());
        PathConstraintAttribute constraint2 = (PathConstraintAttribute) newTemplateQuery.getConstraintsForPath("CEO.company.name").get(0);
        assertEquals("pany", constraint2.getValue());
        assertEquals(ConstraintOp.CONTAINS, constraint2.getOp().CONTAINS);
        PathConstraintLookup constraint3 = (PathConstraintLookup) newTemplateQuery
            .getConstraintsForPath("CEO.company.CEO.department.company.CEO").get(0);
        assertEquals("ttt", constraint3.getValue());
        assertEquals("DepartmentA1", constraint3.getExtraValue());
        //verify outer join
        assertEquals(OuterJoinStatus.OUTER, newTemplateQuery.getOuterJoinStatus("CEO.company"));
        //verify order by
        assertEquals(2, newTemplateQuery.getOrderBy().size());
        assertEquals("CEO.name", newTemplateQuery.getOrderBy().get(0).getOrderPath());
        assertEquals("CEO.salary", newTemplateQuery.getOrderBy().get(1).getOrderPath());
    }
}
