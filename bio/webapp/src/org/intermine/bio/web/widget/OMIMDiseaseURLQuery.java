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

import java.util.Arrays;

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a pathquery.  Used when a user clicks on a results record in an enrichment widget.
 * @author Dominik Grimm
 */
public class OMIMDiseaseURLQuery implements WidgetURLQuery
{

    private InterMineBag bag;
    private String key;
    private ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public OMIMDiseaseURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {
        Model model = os.getModel();
        PathQuery q = new PathQuery(model);
        q.addViews("Gene.primaryIdentifier", "Gene.symbol", "Gene.omimDiseases.omimId",
                "Gene.omimDiseases.title", "Gene.omimDiseases.description");
        q.addConstraint(Constraints.in("Gene", bag.getName()));
        if (!showAll) {
            q.addConstraint(Constraints.oneOfValues("Gene.ominDiseases.omimId",
                    Arrays.asList(key)));
        }
        q.addOrderBy("Gene.symbol", OrderDirection.ASC);
        return q;
    }
}
