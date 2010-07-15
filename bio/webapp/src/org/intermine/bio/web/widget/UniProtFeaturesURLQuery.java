package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Generates the query to run when a user clicks on a results record in an enrichment widget.
 * @author Julie Sullivan
 */
public class UniProtFeaturesURLQuery implements WidgetURLQuery
{

    private InterMineBag bag;
    private String key;
    private ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public UniProtFeaturesURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {
        PathQuery q = new PathQuery(os.getModel());
        q.setView("Protein.primaryIdentifier,Protein.primaryAccession,Protein.organism.name,"
                      + "Protein.features.feature.name,Protein.features.type,"
                      + "Protein.features.description,"
                      + "Protein.features.begin,Protein.features.end");
        q.setOrderBy("Protein.features.feature.name, Protein.primaryAccession");
        q.addConstraint(bag.getType(), Constraints.in(bag.getName()));
        if (!showAll) {
            q.addConstraint("Protein.features.feature", Constraints.lookup(key));
            q.setConstraintLogic("A and B");
            q.syncLogicExpression("and");
        }
        return q;
    }
}
