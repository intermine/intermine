package org.heightmine.bio.web.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.path.Path;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.OrderBy;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.widget.WidgetURLQuery;

import uk.ltd.getahead.dwr.WebContextFactory;

/**
 * Builds a pathquery.  Used when a user clicks on a results record in an enrichment widget.
 * @author Dominik Grimm
 */
public class HaemAtlasGridURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public HaemAtlasGridURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(Collection<InterMineObject> keys) {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        Path geneSymbol = PathQuery.makePath(model, q, "Gene.symbol");
        Path genePrimary = PathQuery.makePath(model, q, "Gene.primaryIdentifier");
        Path haemAtlasSampleName = PathQuery.makePath(model, q,
                "Gene.probeSets.haemAtlasResults.sampleName");
        Path haemAtlasGroup = PathQuery.makePath(model, q, "Gene.probeSets.haemAtlasResults.group");
        Path haemAtlasAverage = PathQuery.makePath(model, q,
                "Gene.probeSets.haemAtlasResults.averageIntensity");
        Path haemAtlasSample = PathQuery.makePath(model, q,
                "Gene.probeSets.haemAtlasResults.sample");
        Path haemAtlasP = PathQuery.makePath(model, q,
                "Gene.probeSets.haemAtlasResults.detectionProbabilities");
        Path haemAtlasIlluId = PathQuery.makePath(model, q, "Gene.probeSets.illuId");

        List<Path> view = new ArrayList<Path>();

        view.add(geneSymbol);
        view.add(genePrimary);
        view.add(haemAtlasSampleName);
        view.add(haemAtlasGroup);
        view.add(haemAtlasAverage);
        view.add(haemAtlasSample);
        view.add(haemAtlasP);
        view.add(haemAtlasIlluId);

        q.setViewPaths(view);
        
        String bagType = bag.getType();
        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();

        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);
        
        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode categoryNode = q.addNode("Gene.probeSets.haemAtlasResults.sampleName");
        Constraint categoryConstraint
                          = new Constraint(constraintOp, key.split("_")[0],
                                  false, label, code, id, null);
        categoryNode.getConstraints().add(categoryConstraint);
        
        constraintOp = ConstraintOp.LESS_THAN;
        code = q.getUnusedConstraintCode();
        PathNode seriesNode = q.addNode("Gene.probeSets.haemAtlasResults.detectionProbabilities");
        Constraint seriesConstraint
                          = new Constraint(constraintOp, 
                                  0.01 , false, label, code, id, null);
        seriesNode.getConstraints().add(seriesConstraint);

        q.setConstraintLogic("A and B and D");
        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();

        sortOrder.add(new OrderBy(geneSymbol, "asc"));
        sortOrder.add(new OrderBy(genePrimary));

        q.setSortOrder(sortOrder);
        
        ObjectStoreWriter osw;
        try {
            HttpSession session = WebContextFactory.get().getSession();
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            osw = ((ProfileManager) servletContext.getAttribute(Constants
                    .PROFILE_MANAGER)).getUserProfileObjectStore();
            InterMineBag imBag = new InterMineBag("GeneIntersection", bag.getType(),
                    null, new Date(), os, profile.getUserId(), osw); 
            
            Query q1 = MainHelper.makeQuery(q, null, null, servletContext, null);
            
            ObjectStoreWriter oswi = os.getNewWriter();
            try {
                oswi.addToBagFromQuery(imBag.getOsb(), q1);
            } finally {
                if (oswi != null) {
                    oswi.close();
                }
            }
            
            view = new ArrayList<Path>();

            q = new PathQuery(model);
            
            view.add(geneSymbol);
            view.add(genePrimary);
            view.add(haemAtlasSampleName);
            view.add(haemAtlasGroup);
            view.add(haemAtlasAverage);
            view.add(haemAtlasSample);
            view.add(haemAtlasP);
            view.add(haemAtlasIlluId);

            q.setViewPaths(view);
            
            bagType = imBag.getType();
            constraintOp = ConstraintOp.IN;
            constraintValue = imBag.getName();

            c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
            q.addNode(bagType).getConstraints().add(c);
            
            constraintOp = ConstraintOp.EQUALS;
            code = q.getUnusedConstraintCode();
            categoryNode = q.addNode("Gene.probeSets.haemAtlasResults.sampleName");
            categoryConstraint
                              = new Constraint(constraintOp, key.split("_")[1],
                                      false, label, code, id, null);
            categoryNode.getConstraints().add(categoryConstraint);

            q.setConstraintLogic("A and B");
            q.syncLogicExpression("and");

            sortOrder = new ArrayList<OrderBy>();

            sortOrder.add(new OrderBy(geneSymbol, "asc"));
            sortOrder.add(new OrderBy(genePrimary));

            q.setSortOrder(sortOrder);
       
        } catch (ObjectStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return q;
    }
}
