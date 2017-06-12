package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.metadata.ConstraintOp;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;

/**
 * Test the PathQueryUpdate behaviour
 * @author butano
 *
 */
public class PathQueryUpdateTest extends TestCase 
{
    private Map<String, String> renamedClasses = new HashMap<String, String>();
    private Map<String, String> renamedFields = new HashMap<String, String>();

    public PathQueryUpdateTest(String arg) {
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
        query.addConstraint(new PathConstraintAttribute("CEOTest.name",
                            ConstraintOp.CONTAINS, "ploy"));
        query.addConstraint(new PathConstraintAttribute("CEOTest.company.name",
                            ConstraintOp.CONTAINS, "pany"));
        query.addConstraint(new PathConstraintLookup(
            "CEOTest.company.CEOTest.department.company.CEOTest", "ttt", "DepartmentA1"));
        query.setOuterJoinStatus("CEOTest.company", OuterJoinStatus.OUTER);
        query.addOrderBy("CEOTest.name", OrderDirection.ASC);
        query.addOrderBy("CEOTest.sal", OrderDirection.ASC);

        PathQueryUpdate pathQueryUpdated = new PathQueryUpdate(query,
            Model.getInstanceByName("oldtestmodel"));
        pathQueryUpdated.update(renamedClasses, renamedFields);
        PathQuery queryUpdated = pathQueryUpdated.getUpdatedPathQuery();
        //verify view
        assertEquals(4, queryUpdated.getView().size());
        assertEquals("CEO.name", queryUpdated.getView().get(0));
        assertEquals("CEO.title", queryUpdated.getView().get(1));
        assertEquals("CEO.company.name", queryUpdated.getView().get(2));
        assertEquals("CEO.salary", queryUpdated.getView().get(3));
        //verify descriptions
        assertEquals(1, queryUpdated.getDescriptions().size());
        assertEquals("CEO name", queryUpdated.getDescription("CEO.name"));
        //verify constraint
        assertEquals(3, queryUpdated.getConstraints().size());
        assertEquals(1, queryUpdated.getConstraintsForPath("CEO.name").size());
        PathConstraintAttribute constraint1 = (PathConstraintAttribute) queryUpdated
                                              .getConstraintsForPath("CEO.name").get(0);
        assertEquals("ploy", constraint1.getValue());
        assertEquals(ConstraintOp.CONTAINS, constraint1.getOp().CONTAINS);
        assertEquals(1, queryUpdated.getConstraintsForPath("CEO.company.name").size());
        PathConstraintAttribute constraint2 = (PathConstraintAttribute) queryUpdated
                                              .getConstraintsForPath("CEO.company.name").get(0);
        assertEquals("pany", constraint2.getValue());
        assertEquals(ConstraintOp.CONTAINS, constraint2.getOp().CONTAINS);
        PathConstraintLookup constraint3 = (PathConstraintLookup) queryUpdated
            .getConstraintsForPath("CEO.company.CEO.department.company.CEO").get(0);
        assertEquals("ttt", constraint3.getValue());
        assertEquals("DepartmentA1", constraint3.getExtraValue());
        //verify outer join
        assertEquals(OuterJoinStatus.OUTER, queryUpdated.getOuterJoinStatus("CEO.company"));
        //verify order by
        assertEquals(2, queryUpdated.getOrderBy().size());
        assertEquals("CEO.name", queryUpdated.getOrderBy().get(0).getOrderPath());
        assertEquals("CEO.salary", queryUpdated.getOrderBy().get(1).getOrderPath());
    }
}
