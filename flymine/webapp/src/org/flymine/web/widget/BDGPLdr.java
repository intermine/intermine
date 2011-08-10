package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.intermine.api.profile.InterMineBag;
import org.intermine.bio.util.BioUtil;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.MRNAExpressionResult;
import org.intermine.model.bio.MRNAExpressionTerm;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

/**
 * @author Julie Sullivan
 */
public class BDGPLdr extends EnrichmentWidgetLdr
{
    private Collection<String> organisms = new ArrayList<String>();
    private Collection<String> organismsLower = new ArrayList<String>();
    private InterMineBag bag;
    private final String dataset = "BDGP in situ data set";

    /**
     * Create a new BDGPLdr.
     * @param bag list of objects for this widget
     * @param os object store
     * @param extraAttribute an extra attribute for this widget (if needed)
     */
    public BDGPLdr(InterMineBag bag, ObjectStore os, String extraAttribute) {
        this.bag = bag;

        organisms = BioUtil.getOrganisms(os, bag, false);

        for (String s : organisms) {
            organismsLower.add(s.toLowerCase());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(String action, List<String> keys) {

        // tables in the FROM clause
        QueryClass qcMrnaResult = new QueryClass(MRNAExpressionResult.class);
        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcDataset = new QueryClass(DataSet.class);
        QueryClass qcTerm = new QueryClass(MRNAExpressionTerm.class);

        // fields in the SELECT clause
        QueryField qfPrimaryIdentifier = new QueryField(qcGene, "primaryIdentifier");
        QueryField qfGene = new QueryField(qcGene, "id");
        QueryField qfTerm = new QueryField(qcTerm, "name");

        // count(*)
        QueryFunction qfCount = new QueryFunction();

        // all constraints will be ANDed together
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // keys come from the widget form, if a user selects a specific expression term, eg.
        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfTerm, ConstraintOp.IN, keys));
        }

        // contrain to user's list of genes (or not)
        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfGene, ConstraintOp.IN, bag.getOsb()));
        }

        // JOIN gene.mRNAExpressionResults --> expression result
        QueryCollectionReference r1 = new QueryCollectionReference(qcGene, "mRNAExpressionResults");
        cs.addConstraint(new ContainsConstraint(r1, ConstraintOp.CONTAINS, qcMrnaResult));

        // expression result.expressed = true
        QueryField qfExpressed = new QueryField(qcMrnaResult, "expressed");
        SimpleConstraint scExpressed = new SimpleConstraint(qfExpressed, ConstraintOp.EQUALS,
                                                            new QueryValue(Boolean.TRUE));
        cs.addConstraint(scExpressed);

        // JOIN ExpressionResult.expressionTerm = expression term object
        QueryCollectionReference r2 = new QueryCollectionReference(qcMrnaResult,
                                                                   "mRNAExpressionTerms");
        cs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, qcTerm));

        // MRNAExpressionResult.dataset = dataset object
        QueryObjectReference qcr = new QueryObjectReference(qcMrnaResult, "dataSet");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcDataset));

        // dataset.name = BDGP (to filter out flyfish)
        QueryExpression qf2 = new QueryExpression(QueryExpression.LOWER,
                                                  new QueryField(qcDataset, "name"));
        cs.addConstraint(new SimpleConstraint(qf2, ConstraintOp.EQUALS,
                                              new QueryValue(dataset.toLowerCase())));
        Query q = new Query();
        q.setDistinct(false);

        Query subQ = new Query();
        subQ.setDistinct(true);

        subQ.addFrom(qcTerm);
        subQ.addFrom(qcMrnaResult);
        subQ.addFrom(qcGene);
        subQ.addFrom(qcDataset);

        subQ.setConstraint(cs);

        if ("export".equals(action)) {
            subQ.addToSelect(qfTerm);
            subQ.addToSelect(qfPrimaryIdentifier);
            subQ.addToOrderBy(qfTerm);
            return subQ;
        } else if ("analysed".equals(action)) {
            subQ.addToSelect(qfGene);
            return subQ;
        } else if (action.endsWith("Total")) {
            subQ.addToSelect(new QueryField(qcGene, "id"));
            q.addFrom(subQ);
            q.addToSelect(qfCount);
        } else {
            subQ.addToSelect(new QueryField(qcTerm, "id"));
            subQ.addToSelect(new QueryField(qcGene, "id"));
            subQ.addToSelect(qfTerm);

            QueryField outerQfTerm = new QueryField(subQ, qfTerm);

            q.addFrom(subQ);
            q.addToSelect(outerQfTerm);
            q.addToGroupBy(outerQfTerm);
            q.addToSelect(qfCount);
            if ("sample".equals(action)) {
                q.addToSelect(outerQfTerm);
            }
        }
        return q;
    }
}
