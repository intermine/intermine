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

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;


/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class DoStatURLQuery implements WidgetURLQuery
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
    public DoStatURLQuery(ObjectStore os, InterMineBag bag, String key) {
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
        String pathStrings = "";

        String prefix = (bagType.equals("Protein") ? "Protein.genes" : "Gene");

        if (bagType.equals("Protein")) {
            pathStrings = "Protein.primaryAccession,";
        }

        pathStrings += prefix + ".primaryIdentifier,"
            + prefix + ".symbol,"
            + prefix + ".organism.name,"
            + prefix + ".doAnnotation.ontologyTerm.identifier,"
            + prefix + ".doAnnotation.ontologyTerm.name,"
            + prefix + ".doAnnotation.ontologyTerm.relations.parentTerm.identifier,"
            + prefix + ".doAnnotation.ontologyTerm.relations.parentTerm.name";

        q.setView(pathStrings);
        q.setOrderBy(pathStrings);

        q.addConstraint(bagType, Constraints.in(bag.getName()));

        // can't be a NOT relationship!
        String pathString = prefix + ".doAnnotation.qualifier";
        q.addConstraint(pathString, Constraints.isNull());

        // go term
        pathString = prefix + ".doAnnotation.ontologyTerm.relations.parentTerm";
        q.addConstraint(pathString, Constraints.lookup(key), "C", "DOTerm");

        q.setConstraintLogic("A and B and C");
        q.syncLogicExpression("and");
        return q;
    }
}
