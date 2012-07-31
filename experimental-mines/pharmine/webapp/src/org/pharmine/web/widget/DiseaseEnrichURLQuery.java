package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class DiseaseEnrichURLQuery implements WidgetURLQuery
{
    //private static final Logger LOG = Logger.getLogger(DiseaseEnrichURLQuery.class);
    private ObjectStore os;
    private InterMineBag bag;
    private String key;

    /**
     * @param os object store
     * @param key go terms user selected
     * @param bag bag page they were on
     */
    public DiseaseEnrichURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery() {
        PathQuery q = new PathQuery(os.getModel());
        String bagType = bag.getType();
        String prefix = ("Gene".equals(bagType) ? "Gene" : "Disease.associatedGenes");

        String paths = "";

        if ("Disease".equals(bagType)) {
            paths += "Disease.diseaseId,";
        }
        if ("Gene".equals(bagType)) {
        	paths += "Gene.diseases.diseaseId,";
        }

        paths += prefix + ".primaryIdentifier";

        q.setView(paths);
        q.setOrderBy(paths);

        // bag constraint
        q.addConstraint(bagType,  Constraints.in(bag.getName()));

        // constrain to domains user selected
        //q.addConstraint(prefix + ".diseases",  Constraints.lookup(key));
        q.addConstraint(prefix + ".diseases.diseaseId",  Constraints.eq(key) );

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        return q;
    }
}
