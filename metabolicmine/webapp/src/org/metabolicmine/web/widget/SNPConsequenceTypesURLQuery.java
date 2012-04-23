package org.metabolicmine.web.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a pathquery.  Used when a user clicks on a results record in an enrichment widget.
 * @author Richard Smith
 */

public class SNPConsequenceTypesURLQuery implements WidgetURLQuery
{

    private InterMineBag bag;
    private String key;
    private ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public SNPConsequenceTypesURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {
        PathQuery q = new PathQuery(os.getModel());
        q.addViews("SNP.primaryIdentifier", "SNP.organism.shortName",
                "SNP.locations.locatedOn.primaryIdentifier",
                "SNP.locations.start", "SNP.locations.end", "SNP.locations.strand",
                "SNP.consequences.description", "SNP.consequences.peptideAlleles",
                "SNP.consequences.transcript.primaryIdentifier");
        q.addConstraint(Constraints.in(bag.getType(), bag.getName()));
        q.setOuterJoinStatus("SNP.consequences.transcript", OuterJoinStatus.OUTER);
        q.setOuterJoinStatus("SNP.locations", OuterJoinStatus.OUTER);
        if (!showAll) {
            String[] keys = key.split(",");
            q.addConstraint(Constraints.oneOfValues("SNP.consequences.description",
                    Arrays.asList(keys)));
        }
        q.addOrderBy("SNP.organism.shortName", OrderDirection.ASC);
        q.addOrderBy("SNP.primaryIdentifier", OrderDirection.ASC);
        q.addOrderBy("SNP.consequences.description", OrderDirection.ASC);
        return q;
    }
}

