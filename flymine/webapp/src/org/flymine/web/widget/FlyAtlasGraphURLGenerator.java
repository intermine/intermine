package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.model.genomic.FlyAtlasResult;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MicroArrayAssay;
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
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 * 
 * @author Xavier Watkins
 *
 */
public class FlyAtlasGraphURLGenerator implements CategoryURLGenerator
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
     * @see org.jfree.chart.urls.CategoryURLGenerator#generateURL(
     *      org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateURL(CategoryDataset dataset, int series, int category) {
        StringBuffer sb = new StringBuffer("queryForGraphAction.do?bagName=" + bagName);

        Query q = new Query();
        QueryClass far = new QueryClass(FlyAtlasResult.class);
        QueryClass maa = new QueryClass(MicroArrayAssay.class);
        QueryClass gene = new QueryClass(Gene.class);

        q.addFrom(far);
        q.addFrom(maa);
        q.addFrom(gene);

        q.addToSelect(gene);

        ConstraintSet maincs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference r = new QueryCollectionReference(far, "genes");
        maincs.addConstraint(new ContainsConstraint(r, ConstraintOp.CONTAINS, gene));
        QueryCollectionReference r2 = new QueryCollectionReference(far, "assays");
        maincs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, maa));

        // Add the tissue / affycall constraints
        maincs.addConstraint(new SimpleConstraint(new QueryField(far, "affyCall"),
                                                  ConstraintOp.EQUALS, new QueryValue(dataset
                                                      .getRowKey(series))));
        maincs.addConstraint(new SimpleConstraint(new QueryField(maa, "name"), ConstraintOp.EQUALS,
                                                  new QueryValue(dataset.getColumnKey(category))));

        q.setConstraint(maincs);

        IqlQuery iqlQuery = new IqlQuery(q);

        sb.append("&query=" + iqlQuery.getQueryString());

        return sb.toString();
    }

}
