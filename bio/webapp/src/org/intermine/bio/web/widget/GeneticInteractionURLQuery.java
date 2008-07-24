package org.intermine.bio.web.widget;

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

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.Constraints;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a pathquery.  Used when a user clicks on a results record in an enrichment widget.
 * @author Julie Sullivan
 */

public class GeneticInteractionURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public GeneticInteractionURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(Collection<InterMineObject> keys) {

        PathQuery q = new PathQuery(os.getModel());
        q.setView("Gene.primaryIdentifier,Gene.symbol,Gene.organism.shortName,"
                  + "Gene.geneticInteractions.shortName,Gene.geneticInteractions.type,"
                  + "Gene.geneticInteractions.geneRole,"
                  + "Gene.geneticInteractions.interactingGenes.primaryIdentifier"
                  + "Gene.geneticInteractions.experiment.name");

        String bagType = bag.getType();

        q.addConstraint(bagType,  Constraints.in(bag.getName()));

        if (keys != null) {
            q.addConstraint(bagType,  Constraints.notIn(new ArrayList(keys)));
        } else {
            q.addConstraint("Gene.geneticInteractions.interactingGenes",  Constraints.lookup(key));
        }

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        q.setOrderBy("Gene.organism.shortName,Gene.primaryIdentifier,"
                  + "Gene.geneticInteractions.shortName,Gene.geneticInteractions.type");
        return q;
    }
}

