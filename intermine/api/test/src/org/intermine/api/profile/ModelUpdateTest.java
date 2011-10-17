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
import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.CacheMap;

public class ModelUpdateTest extends InterMineAPITestCase {

    private ModelUpdate modelUpdate;

    public ModelUpdateTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        try {
            modelUpdate = new ModelUpdate(os, uosw, "oldtestmodel");
        } catch (BuildException be) {
            tearDown();
            throw be;
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

/*    private void setUpUserProfiles() throws Exception {
        //
        MokaIntermineBag bag1 = new MokaInterMineBag("bag1", "Department", "This is some description",
                date, BagState.CURRENT, os, testUser.getUserId(), uosw);
        testUser.savedBags.put("bag1", bag1);
        testUser.createBag("bag2", "CEOTest", "description bag2", im.getClassKeys(), false);
        testUser.createBag("bag3", "Address", "description bag3", im.getClassKeys(), false);

        TemplateQuery template =
            new TemplateQuery("template", "ttitle", "tcomment",
                              new PathQuery(Model.getInstanceByName("oldtestmodel")));
        testUser.saveTemplate("template", template);
    }*/

    public void testConstructor() throws Exception {
        assertEquals(1, modelUpdate.getDeletedClasses().size());
        assertEquals(true, modelUpdate.getDeletedClasses().contains("Address"));

        assertEquals(1, modelUpdate.getRenamedClasses().size());
        assertEquals(true, modelUpdate.getRenamedClasses().containsKey("CEOTest"));
        assertEquals("CEO", modelUpdate.getRenamedClasses().get("CEOTest"));

        assertEquals(2, modelUpdate.getRenamedFields().size());
        assertEquals(true, modelUpdate.getRenamedFields().containsKey("CEOTest.sal"));
        assertEquals("salary", modelUpdate.getRenamedFields().get("CEOTest.sal"));
        assertEquals(true, modelUpdate.getRenamedFields().containsKey("Company.CEOTest"));
        assertEquals("CEO", modelUpdate.getRenamedFields().get("Company.CEOTest"));
    }
    /*
    public void testDeleteBags() throws Exception {
        setUpUserProfiles();
        assertEquals(3, testUser.getSavedBags().size());
        modelUpdate.deleteBags();
        assertEquals(2, testUser.getSavedBags().size());
    }

    public void testUpdateTypeBag() throws Exception {
        setUpUserProfiles();
        InterMineBag bag2 = testUser.getSavedBags().get("bag2");
        assertEquals("CEOTest", bag2.getType());
        modelUpdate.updateTypeBag();
        assertEquals("CEO", bag2.getType());
    }
*/
/*
    public void testUpdateReferredQueryAndTemplate() throws Exception {
        //create savedquery
        PathQuery query = new PathQuery(Model.getInstanceByName("oldtestmodel"));
        query.addView("CEOTest.name");
        query.addView("CEOTest.title");
        query.addView("CEOTest.company.name");
        query.setDescription("CEOTest.name", "CEO name");
        query.addConstraint(new PathConstraintAttribute("CEOTest.name", ConstraintOp.CONTAINS, "ploy"));
        query.addConstraint(new PathConstraintAttribute("CEOTest.company.name", ConstraintOp.CONTAINS, "pany"));
        query.setOuterJoinStatus("CEOTest.company", OuterJoinStatus.OUTER);
        query.addOrderBy("CEOTest.name", OrderDirection.ASC);
        Date date = new Date();
        SavedQuery sq = new SavedQuery("query1", date, query);
        testUser.saveQuery("query1", sq);

        modelUpdate.updateReferredQueryAndTemplate();
        
        PathQuery queryUpdated = testUser.getSavedQueries().get("query1").getPathQuery();
        //verify view
        assertEquals(3, queryUpdated.getView().size());
        assertEquals("CEO.name", queryUpdated.getView().get(0));
        assertEquals("CEO.title", queryUpdated.getView().get(1));
        assertEquals("CEO.company.name", queryUpdated.getView().get(2));
        //verify descriptions
        assertEquals(1, queryUpdated.getDescriptions().size());
        assertEquals("CEO name", queryUpdated.getDescription("CEOTest.name"));
        //verify constraints
        assertEquals(3, queryUpdated.getConstraints().size());
        assertEquals(1, queryUpdated.getConstraintsForPath("CEO.name").size());
        PathConstraintAttribute constraint1 = (PathConstraintAttribute) queryUpdated.getConstraintsForPath("CEO.name").get(0);
        assertEquals("ploy", constraint1.getValue());
        assertEquals(ConstraintOp.CONTAINS, constraint1.getOp().CONTAINS);
        assertEquals(1, queryUpdated.getConstraintsForPath("Company.name").size());
        PathConstraintAttribute constraint2 = (PathConstraintAttribute) queryUpdated.getConstraintsForPath("Company.name").get(0);
        assertEquals("pany", constraint2.getValue());
        assertEquals(ConstraintOp.CONTAINS, constraint2.getOp().CONTAINS);
        //verify outer join
        assertEquals(OuterJoinStatus.OUTER, queryUpdated.getOuterJoinStatus("CEO.company"));
        //verify order by
        assertEquals(1, queryUpdated.getOrderBy().size());
        assertEquals("CEO.name", queryUpdated.getOrderBy().get(0).getOrderPath());
    }
    */
}
