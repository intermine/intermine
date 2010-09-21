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
import java.util.List;

import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.path.Path;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.OrderBy;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.GraphCategoryURLGenerator;

import org.jfree.data.category.CategoryDataset;
/**
 *
 * @author Dominik Grimm
 *
 */
public class HaemAtlasGraphURLGenerator implements GraphCategoryURLGenerator
{
    String bagName;

    /**
     * Creates a HaemAtlasGraphURLGenerator for the chart
     * @param bagName the bag name
     * @param extra unused
     */
    public HaemAtlasGraphURLGenerator(String bagName, @SuppressWarnings("unused") String extra) {
        super();
        this.bagName = bagName;
    }

    /**
     * Creates a HaemAtlasGraphURLGenerator for the chart
     * @param bagName the bag name
     */
    public HaemAtlasGraphURLGenerator(String bagName) {
        super();
        this.bagName = bagName;
    }

    /**
     * {@inheritDoc}
     * @see org.jfree.chart.urls.CategoryURLGenerator#generateURL(
     *      org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateURL(CategoryDataset dataset, int series, int category) {
        StringBuffer sb = new StringBuffer("queryForGraphAction.do?bagName=" + bagName);
        sb.append("&category=" + dataset.getColumnKey(category));
        sb.append("&series=" + dataset.getColumnKey(category));
        sb.append("&urlGen=org.intermine.bio.web.widget.HaemAtlasGraphURLGenerator");
        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(ObjectStore os,
                                       InterMineBag bag,
                                       String series,
                                       String category) {

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
                          = new Constraint(constraintOp, series, false, label, code, id, null);
        categoryNode.getConstraints().add(categoryConstraint);
        
        constraintOp = ConstraintOp.LESS_THAN;
        code = q.getUnusedConstraintCode();
        PathNode seriesNode = q.addNode("Gene.probeSets.haemAtlasResults.detectionProbabilities");
        Constraint seriesConstraint
                          = new Constraint(constraintOp, 
                                  0.01 , false, label, code, id, null);
        seriesNode.getConstraints().add(seriesConstraint);

        q.setConstraintLogic("A and B and C");
        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();

        sortOrder.add(new OrderBy(geneSymbol, "asc"));
        sortOrder.add(new OrderBy(genePrimary));

        q.setSortOrder(sortOrder);


        return q;
    }

}
