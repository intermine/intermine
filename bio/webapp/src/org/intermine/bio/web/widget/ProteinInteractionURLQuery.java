package org.intermine.bio.web.widget;

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
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a pathquery.  Used when a user clicks on a results record in an enrichment widget.
 * @author Dominik Grimm & Michael Menden
 */
public class ProteinInteractionURLQuery implements WidgetURLQuery
{

    private InterMineBag bag;
    private String key;
    private ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public ProteinInteractionURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {
        PathQuery q = new PathQuery(os.getModel());
        q.addViews("Protein.primaryIdentifier, Protein.primaryAccession",
                  "Protein.proteinInteractions.interactingProteins.primaryIdentifier",
                  "Protein.proteinInteractions.interactingProteins.primaryAccession",
                  "Protein.proteinInteractions.interactingProteins.name",
                  "Protein.proteinInteractions.shortName",
                  "Protein.proteinInteractions.proteinRole",
                  "Protein.proteinInteractions.experiment.publication.pubMedId");
        q.addConstraint(Constraints.in(bag.getType(), bag.getName()));
        if (!showAll) {
            String[] keys = key.split(",");
            q.addConstraint(Constraints.oneOfValues(
                    "Protein.proteinInteractions.interactingProteins.primaryAccession",
                    Arrays.asList(keys)));
        }
        return q;
    }
}
