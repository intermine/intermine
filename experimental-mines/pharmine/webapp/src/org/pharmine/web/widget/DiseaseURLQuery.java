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
public class DiseaseURLQuery implements WidgetURLQuery
{
    //private static final Logger LOG = Logger.getLogger(DiseaseURLQuery.class);
    private ObjectStore os;
    private InterMineBag bag;
    private String key;

    /**
     * @param os object store
     * @param key go terms user selected
     * @param bag bag page they were on
     */
    public DiseaseURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery() {
        PathQuery q = new PathQuery(os.getModel());
        q.setView("Gene.secondaryIdentifier,Gene.primaryIdentifier,Gene.name,Gene.organism.name"
                      + "Gene.publications.title,Gene.publications.firstAuthor,"
                      + "Gene.publications.journal,Gene.publications.year,"
                      + "Gene.publications.pubMedId");
        q.setOrderBy("Gene.publications.pubMedId, Gene.primaryIdentifier");
        q.addConstraint(bag.getType(), Constraints.in(bag.getName()));
        q.addConstraint("Gene.publications", Constraints.lookup(key));
        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");
        return q;
    }
}
