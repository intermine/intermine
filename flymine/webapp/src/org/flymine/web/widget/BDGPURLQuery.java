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
import java.util.Collection;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.Constraints;
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
    public PathQuery generatePathQuery(Collection<InterMineObject> keys) {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        String viewStrings = "Gene.secondaryIdentifier,Gene.name,Gene.organism.name,"
            + "Gene.primaryIdentifier";

        String expressionStrings = "Gene.mRNAExpressionResults.stageRange,"
            + "Gene.mRNAExpressionResults.mRNAExpressionTerms.name,"
            + "Gene.mRNAExpressionResults.dataSet.title";

        q.setView(viewStrings);
        if (keys == null) {
            q.addView(expressionStrings);
        }
        String bagType = bag.getType();
        q.addConstraint(bagType,  Constraints.in(bag.getName()));
        if (keys != null) {
            q.addConstraint(bagType,  Constraints.notIn(new ArrayList(keys)));
            q.setConstraintLogic("A and B");
        } else {
            q.addConstraint("Gene.mRNAExpressionResults.mRNAExpressionTerms",
                            Constraints.lookup(key));
            q.addConstraint("Gene.mRNAExpressionResults.expressed", Constraints.eq(Boolean.TRUE));
            q.addConstraint("Gene.mRNAExpressionResults.dataSet.title", Constraints.eq(DATASET));
            q.setConstraintLogic("A and B and C and D");
        }
        q.syncLogicExpression("and");
        if (keys == null) {
            q.setOrderBy(expressionStrings);
        } else {
            q.setOrderBy("Gene.primaryIdentifier");
        }
        return q;
    }
}
