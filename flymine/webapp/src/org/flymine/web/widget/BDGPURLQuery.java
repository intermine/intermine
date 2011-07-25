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
import java.util.Arrays;
import java.util.List;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
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
    public PathQuery generatePathQuery(boolean showAll) {
        PathQuery q = new PathQuery(os.getModel());
        q.addViews("Gene.secondaryIdentifier", "Gene.symbol", "Gene.organism.name");
        List<String> expressionStrings = new ArrayList<String>(Arrays.asList(new String[] {
            "Gene.mRNAExpressionResults.stageRange",
            "Gene.mRNAExpressionResults.mRNAExpressionTerms.name",
            "Gene.mRNAExpressionResults.dataSet.name"
        }));
        q.addViews(expressionStrings);
        q.addConstraint(Constraints.in(bag.getType(), bag.getName()));

        q.addConstraint(Constraints.eq("Gene.mRNAExpressionResults.expressed",
                Boolean.TRUE.toString()));
        q.addConstraint(Constraints.eq("Gene.mRNAExpressionResults.dataSet.name", DATASET));
        if (!showAll) {
            String[] keys = key.split(",");
            q.addConstraint(Constraints.oneOfValues(
                    "Gene.mRNAExpressionResults.mRNAExpressionTerms.name", Arrays.asList(keys)));
        }
        for (String orderPath : expressionStrings) {
            q.addOrderBy(orderPath, OrderDirection.ASC);
        }

        return q;
    }
}
