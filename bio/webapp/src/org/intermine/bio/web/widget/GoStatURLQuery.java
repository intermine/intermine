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
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;


/**
 * @author Julie Sullivan
 */
public class GoStatURLQuery implements WidgetURLQuery
{
    //private static final Logger LOG = Logger.getLogger(GoStatURLQuery.class);
    private ObjectStore os;
    private InterMineBag bag;
    private String key;

    /**
     * @param os object store
     * @param key go terms user selected
     * @param bag bag page they were on
     */
    public GoStatURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {

        PathQuery q = new PathQuery(os.getModel());
        String bagType = bag.getType();

        String prefix = ("Protein".equals(bagType) ? "Protein.genes" : bagType);

        if ("Protein".equals(bagType)) {
            q.addViews("Protein.primaryAccession");
        } else {
            q.addViews(prefix + ".secondaryIdentifier", prefix + ".symbol");
        }

        q.addViews(prefix + ".primaryIdentifier",
                prefix + ".organism.name",
                prefix + ".goAnnotation.ontologyTerm.identifier",
                prefix + ".goAnnotation.ontologyTerm.name",
                prefix + ".goAnnotation.ontologyTerm.parents.identifier",
                prefix + ".goAnnotation.ontologyTerm.parents.name");

        q.addConstraint(Constraints.in(bagType, bag.getName()));
        // can't be a NOT relationship!
        q.addConstraint(Constraints.isNull(prefix + ".goAnnotation.qualifier"));

        if (!showAll) {
            String[] keys = key.split(",");
            q.addConstraint(Constraints.oneOfValues(prefix
                    + ".goAnnotation.ontologyTerm.parents.identifier",
                    Arrays.asList(keys)));
        }
        return q;
    }
}
