package org.intermine.bio.web.widget;

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

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Gene;

import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 *
 * @author Julie Sullivan
 */
public class ChromosomeDistributionGraphURLGenerator implements CategoryURLGenerator
{
    String bagName;

    /**
     * Creates a ChromosomeDistributionGraphURLGenerator for the chart
     * @param bagName the bag name
     */
    public ChromosomeDistributionGraphURLGenerator(String bagName) {
        super();
        this.bagName = bagName;
    }

    /**
     * {@inheritDoc}
     * @see org.jfree.chart.urls.CategoryURLGenerator#generateURL(
     *      org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateURL(CategoryDataset dataset, @SuppressWarnings("unused") int series,
                              int category) {
        StringBuffer sb = new StringBuffer("queryForGraphAction.do?bagName=" + bagName);
        
        Query q = new Query();
        QueryClass chromosomeQC = new QueryClass(Chromosome.class);
        QueryClass geneQC = new QueryClass(Gene.class); 
        
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
