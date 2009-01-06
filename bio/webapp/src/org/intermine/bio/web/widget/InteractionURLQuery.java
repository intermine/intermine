package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2009 FlyMine
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
 * Builds a pathquery.  Used when a user clicks on a results record in an enrichment widget.
 * @author Julie Sullivan
 */

public class InteractionURLQuery implements WidgetURLQuery
{

    private InterMineBag bag;
    private String key;
    private ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public InteractionURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery() {
        PathQuery q = new PathQuery(os.getModel());
        q.setView("Gene.primaryIdentifier,Gene.symbol,Gene.organism.shortName,"
                  + "Gene.interactions.shortName,Gene.interactions.type,"
                  + "Gene.interactions.geneRole,"
                  + "Gene.interactions.interactingGenes.primaryIdentifier"
                  + "Gene.interactions.experiment.name");
        q.addConstraint(bag.getType(), Constraints.in(bag.getName()));
        q.addConstraint("Gene.interactions.interactingGenes", Constraints.lookup(key));
        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");
        q.setOrderBy("Gene.organism.shortName,Gene.primaryIdentifier,"
                  + "Gene.interactions.shortName,Gene.interactions.type");
        return q;
    }
}

