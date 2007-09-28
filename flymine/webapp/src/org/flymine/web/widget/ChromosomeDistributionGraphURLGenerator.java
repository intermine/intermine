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

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.iql.IqlQuery;

import org.intermine.metadata.Model;
import org.intermine.web.logic.bag.InterMineBag;

import org.flymine.model.genomic.Chromosome;

import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 *
 * @author Julie Sullivan
 */
public class ChromosomeDistributionGraphURLGenerator implements CategoryURLGenerator
{
    InterMineBag bag;
    Model model;
    
    /**
     * Creates a ChromosomeDistributionGraphURLGenerator for the chart
     * @param model
     * @param bag the bag
     */
    public ChromosomeDistributionGraphURLGenerator(Model model, InterMineBag bag) {
        super();
        this.bag = bag;
        this.model = model;
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
        
        StringBuffer sb = new StringBuffer("queryForGraphAction.do?bagName=" + bag.getName());
        
        Query q = new Query();
        QueryClass chromosomeQC = new QueryClass(Chromosome.class);
        QueryClass geneQC = null;
        try {
            geneQC = new QueryClass(Class.forName(model.getPackageName() 
                                                           + "." + bag.getType())); 
        } catch (ClassNotFoundException e) {
            return null;
        }
        q.addFrom(geneQC);
        q.addFrom(chromosomeQC);
        
        q.addToSelect(geneQC);
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        
        // gene.chromosome CONTAINS chromosome.identifier
        QueryObjectReference r = new QueryObjectReference(geneQC, "chromosome");
        //ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS, chromosomeQC);
        cs.addConstraint(new ContainsConstraint(r, ConstraintOp.CONTAINS, chromosomeQC));
        
        // constrain to be specific chromosome
        cs.addConstraint(new SimpleConstraint(new QueryField(chromosomeQC, "identifier"), 
                                                  ConstraintOp.EQUALS,
                                                  new QueryValue(dataset.getColumnKey(category))));
                
        q.setConstraint(cs);

        IqlQuery iqlQuery = new IqlQuery(q);

        sb.append("&query=" + iqlQuery.getQueryString());

        return sb.toString();
    }
}
