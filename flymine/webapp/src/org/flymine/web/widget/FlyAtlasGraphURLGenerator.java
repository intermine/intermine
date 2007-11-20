package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
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

    
    public PathQuery generatePathQuery(ObjectStore os,
                                       InterMineBag bag,
                                       String series, 
                                       String category) {
       
        Model model = os.getModel();
        PathQuery q = new PathQuery(model);
        
        List view = new ArrayList();
        view.add(MainHelper.makePath(model, q, "FlyAtlasResult.genes.identifier"));
        view.add(MainHelper.makePath(model, q, "FlyAtlasResult.genes.organismDbId"));
        view.add(MainHelper.makePath(model, q, "FlyAtlasResult.assays.name"));
        q.setView(view);
        
        String bagType = bag.getType();
        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode("FlyAtlasResult.genes.identifier").getConstraints().add(c);
        
        // series
        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode seriesNode = q.addNode("FlyAtlasResult.affyCall");
        Constraint seriesConstraint 
                        = new Constraint(constraintOp, category, false, label, code, id, null);
        seriesNode.getConstraints().add(seriesConstraint);
        
        // series
        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode catNode = q.addNode("FlyAtlasResult.assays.name");
        Constraint catConstraint 
                        = new Constraint(constraintOp, series, false, label, code, id, null);
        catNode.getConstraints().add(catConstraint);
        
        q.setConstraintLogic("A and B and C");
        q.syncLogicExpression("and");
        
        return q; 
    }    
    
}
