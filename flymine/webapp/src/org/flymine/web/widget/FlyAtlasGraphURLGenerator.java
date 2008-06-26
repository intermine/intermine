package org.flymine.web.widget;

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
 * @author Xavier Watkins
 *
 */
public class FlyAtlasGraphURLGenerator implements GraphCategoryURLGenerator
{
    String bagName;

    /**
     * Creates a FlyAtlasGraphURLGenerator for the chart
     * @param bagName the bag name
     * @param extra unused
     */
    public FlyAtlasGraphURLGenerator(String bagName, @SuppressWarnings("unused") String extra) {
        super();
        this.bagName = bagName;
    }

    /**
     * Creates a FlyAtlasGraphURLGenerator for the chart
     * @param bagName the bag name
     */
    public FlyAtlasGraphURLGenerator(String bagName) {
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
        sb.append("&series=" + dataset.getRowKey(series));
        sb.append("&urlGen=org.flymine.web.widget.FlyAtlasGraphURLGenerator");
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

        Path secondaryIdentifier = PathQuery.makePath(model, q,
                                                       "FlyAtlasResult.genes.secondaryIdentifier");
        Path primaryIdentifier = PathQuery.makePath(model, q,
                                                     "FlyAtlasResult.genes.primaryIdentifier");
        Path name = PathQuery.makePath(model, q, "FlyAtlasResult.genes.name");
        Path org = PathQuery.makePath(model, q, "FlyAtlasResult.genes.organism.name");
        Path assays = PathQuery.makePath(model, q, "FlyAtlasResult.assays.name");
        Path enrichment = PathQuery.makePath(model, q, "FlyAtlasResult.enrichment");
        Path affyCall = PathQuery.makePath(model, q, "FlyAtlasResult.affyCall");
        Path signal = PathQuery.makePath(model, q, "FlyAtlasResult.mRNASignal");
        Path sem = PathQuery.makePath(model, q, "FlyAtlasResult.mRNASignalSEM");
        Path presentCall = PathQuery.makePath(model, q, "FlyAtlasResult.presentCall");

        List<Path> view = new ArrayList<Path>();

        view.add(secondaryIdentifier);
        view.add(primaryIdentifier);
        view.add(name);
        view.add(org);
        view.add(assays);
        view.add(affyCall);
        view.add(enrichment);
        view.add(signal);
        view.add(sem);
        view.add(presentCall);

        q.setViewPaths(view);

        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();

        String label = null, id = null, code = q.getUnusedConstraintCode();
        PathNode geneNode = q.addNode("FlyAtlasResult.genes");
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        geneNode.getConstraints().add(c);

        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode categoryNode = q.addNode("FlyAtlasResult.affyCall");
        Constraint categoryConstraint
                        = new Constraint(constraintOp, category, false, label, code, id, null);
        categoryNode.getConstraints().add(categoryConstraint);

        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode seriesNode = q.addNode("FlyAtlasResult.assays.name");
        Constraint seriesConstraint
                        = new Constraint(constraintOp, series, false, label, code, id, null);
        seriesNode.getConstraints().add(seriesConstraint);

        q.setConstraintLogic("A and B and C");

        String direction = "asc";
        if (category.toLowerCase().equals("up")) {
            direction = "desc";
        }

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();

        sortOrder.add(new OrderBy(enrichment, direction));
        sortOrder.add(new OrderBy(secondaryIdentifier, "asc"));
        sortOrder.add(new OrderBy(primaryIdentifier, "asc"));
        sortOrder.add(new OrderBy(name, "asc"));
        sortOrder.add(new OrderBy(assays, "asc"));
        sortOrder.add(new OrderBy(affyCall, "asc"));

        q.setSortOrder(sortOrder);

        q.syncLogicExpression("and");

        return q;
    }

}
