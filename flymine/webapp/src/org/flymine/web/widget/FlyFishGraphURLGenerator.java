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
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.iql.IqlQuery;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MRNALocalisationResult;

import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 * 
 * @author Julie Sullivan
 *
 */
public class FlyFishGraphURLGenerator implements CategoryURLGenerator
{
    String bagName;

    /**
     * Creates a FlyAtlasGraphURLGenerator for the chart
     * @param bagName the bag name
     */
    public FlyFishGraphURLGenerator(String bagName) {
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

        Query q = new Query();
        QueryClass mrnaResult = new QueryClass(MRNALocalisationResult.class);
        QueryClass gene = new QueryClass(Gene.class);
       
        q.addFrom(mrnaResult);
        q.addFrom(gene);
        
        q.addToSelect(gene);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
       
        QueryCollectionReference r = new QueryCollectionReference(gene, "mRNALocalisationResults");
        cs.addConstraint(new ContainsConstraint(r, ConstraintOp.CONTAINS, mrnaResult));

        // TODO fix for 3 datasets

        
        cs.addConstraint(new SimpleConstraint(new QueryField(mrnaResult, "localisation"),
                                             ConstraintOp.EQUALS, new QueryValue("localised")));
        
        cs.addConstraint(new SimpleConstraint(new QueryField(mrnaResult, "stage"), 
                                              ConstraintOp.EQUALS,
                                              new QueryValue(dataset.getColumnKey(category))));


        q.setConstraint(cs);
       
        IqlQuery iqlQuery = new IqlQuery(q);

        sb.append("&query=" + iqlQuery.getQueryString());

        return sb.toString();
    }

}
