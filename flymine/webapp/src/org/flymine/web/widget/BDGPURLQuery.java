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

import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * @author Julie Sullivan
 */
public class BDGPURLQuery implements WidgetURLQuery
{
    private InterMineBag bag;
    private String key;
    private ObjectStore os;
    private static final String DATASET = "BDGP in situ data set";

    /**
     * @param key which record the user clicked on
     * @param bag bag
     * @param os object store
     */
    public BDGPURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery() {
        PathQuery q = new PathQuery(os.getModel());
        String viewStrings = "Gene.secondaryIdentifier,Gene.name,Gene.organism.name,"
            + "Gene.primaryIdentifier";
        String expressionStrings = "Gene.mRNAExpressionResults.stageRange,"
            + "Gene.primaryIdentifier, Gene.mRNAExpressionResults.stageRange,"
            + "Gene.mRNAExpressionResults.mRNAExpressionTerms.name,"
            + "Gene.mRNAExpressionResults.dataSet.title";
        q.setView(viewStrings);
        q.addView(expressionStrings);
        q.addConstraint(bag.getType(),  Constraints.in(bag.getName()));
        q.addConstraint("Gene.mRNAExpressionResults.mRNAExpressionTerms", Constraints.lookup(key));
        q.addConstraint("Gene.mRNAExpressionResults.expressed", Constraints.eq(Boolean.TRUE));
        q.addConstraint("Gene.mRNAExpressionResults.dataSet.title", Constraints.eq(DATASET));
        q.setConstraintLogic("A and B and C and D");
        q.syncLogicExpression("and");
        q.setOrderBy(expressionStrings);
        return q;
    }
}
