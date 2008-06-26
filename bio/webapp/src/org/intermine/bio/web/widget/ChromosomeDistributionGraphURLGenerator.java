package org.intermine.bio.web.widget;

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
 * @author Julie Sullivan
 */
public class ChromosomeDistributionGraphURLGenerator implements GraphCategoryURLGenerator
{
    String bagName;
    String organism = null;

    /**
     * Creates a ChromosomeDistributionGraphURLGenerator for the chart
     * @param model
     * @param bagName name of bag for which to render this widget
     * @param organism constrain query by organism
     */
    public ChromosomeDistributionGraphURLGenerator(String bagName, String organism) {
        super();
        this.bagName = bagName;
        this.organism = organism;
    }

    /**
     * {@inheritDoc}
     * @see org.jfree.chart.urls.CategoryURLGenerator#generateURL(
     *      org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateURL(CategoryDataset dataset,
                              @SuppressWarnings("unused") int series,
                              int category) {
        StringBuffer sb = new StringBuffer("queryForGraphAction.do?bagName=" + bagName);
        sb.append("&category=" + dataset.getColumnKey(category));
        sb.append("&series=");
        sb.append("&urlGen=org.intermine.bio.web.widget.ChromosomeDistributionGraphURLGenerator");
        sb.append("&extraKey=" + organism);

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(ObjectStore os,
                                       InterMineBag imBag,
                                       @SuppressWarnings("unused") String series,
                                       @SuppressWarnings("unused") String category) {

        Model model = os.getModel();
        InterMineBag bag = imBag;
        PathQuery q = new PathQuery(model);
        String bagType = bag.getType();

        Path identifier = PathQuery.makePath(model, q, bagType + ".secondaryIdentifier");
        Path primaryIdentifier = PathQuery.makePath(model, q, bagType + ".primaryIdentifier");
        Path name = PathQuery.makePath(model, q, bagType + ".organism.name");
        Path chromoIdentifier = PathQuery.makePath(model, q, bagType
                                                    + ".chromosome.primaryIdentifier");
        Path start = PathQuery.makePath(model, q, bagType + ".chromosomeLocation.start");
        Path end = PathQuery.makePath(model, q, bagType + ".chromosomeLocation.end");
        Path strand = PathQuery.makePath(model, q, bagType + ".chromosomeLocation.strand");

        List<Path> view = new ArrayList<Path>();

        view.add(identifier);
        view.add(primaryIdentifier);
        view.add(name);
        view.add(chromoIdentifier);
        view.add(start);
        view.add(end);
        view.add(strand);

        q.setViewPaths(view);

        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();

        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);

        //  constrain to be specific chromosome
        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode chromosomeNode = q.addNode(bagType + ".chromosome.primaryIdentifier");
        Constraint chromosomeConstraint
                        = new Constraint(constraintOp, series, false, label, code, id, null);
        chromosomeNode.getConstraints().add(chromosomeConstraint);

        if (organism != null) {
            constraintOp = ConstraintOp.LOOKUP;
            code = q.getUnusedConstraintCode();
            PathNode orgNode  = q.addNode("Gene.organism");
            orgNode.setType("Organism");
            Constraint orgConstraint
            = new Constraint(constraintOp, organism, false, label, code, id, null);
            orgNode.getConstraints().add(orgConstraint);
            q.setConstraintLogic("A and B and C");
        } else {
            q.setConstraintLogic("A and B");
        }

        q.syncLogicExpression("and");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();
        sortOrder.add(new OrderBy(start, "asc"));
        sortOrder.add(new OrderBy(identifier, "asc"));
        sortOrder.add(new OrderBy(primaryIdentifier, "asc"));
        sortOrder.add(new OrderBy(name, "asc"));
        sortOrder.add(new OrderBy(chromoIdentifier, "asc"));
        sortOrder.add(new OrderBy(end, "asc"));
        sortOrder.add(new OrderBy(strand, "asc"));
        q.setSortOrder(sortOrder);
        return q;
    }
}
