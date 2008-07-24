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
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class GoStatURLQuery implements WidgetURLQuery
{
    ObjectStore os;
    InterMineBag bag;
    String key;

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
    public PathQuery generatePathQuery(Collection<InterMineObject> keys) {

        PathQuery q = new PathQuery(os.getModel());
        String bagType = bag.getType();

        String pathStrings = "";
        String widgetStrings = "";

        if (bagType.equals("Protein")) {
            pathStrings = "Protein.genes.primaryAccession,Protein.genes.primaryIdentifier,"
                + "Protein.genes.name,Protein.genes.organism.name,"
                + "Protein.primaryIdentifier,Protein.primaryAccession";
            widgetStrings = "Protein.genes.allGoAnnotation.identifier,"
                + "Protein.genes.allGoAnnotation.name,"
                + "Protein.genes.allGoAnnotation.actualGoTerms.name,"
                + "Protein.genes.allGoAnnotation.actualGoTerms.identifier";
        } else {

            pathStrings = "Gene.secondaryIdentifier,Gene.primaryIdentifier,"
                + "Gene.name,Gene.organism.name";
            widgetStrings = "Gene.allGoAnnotation.identifier,"
                + "Gene.allGoAnnotation.name,"
                + "Gene.allGoAnnotation.actualGoTerms.name,"
                + "Gene.allGoAnnotation.actualGoTerms.identifier";
        }

        q.setView(pathStrings);
        q.setOrderBy(pathStrings);
        if (keys == null) {
            q.addView(widgetStrings);
            q.addOrderBy(widgetStrings);
        }

        q.addConstraint(bagType,  Constraints.in(bag.getName()));

        if (keys != null) {
            q.addConstraint(bagType,  Constraints.notIn(new ArrayList(keys)));
            q.setConstraintLogic("A and B");
        } else {
            // can't be a NOT relationship!
            String pathString = (bagType.equals("Protein")
                   ? "Protein.genes.allGoAnnotation.qualifier" : "Gene.allGoAnnotation.qualifier");
            q.addConstraint(pathString,  Constraints.isNull());

            // go term
            pathString = (bagType.equals("Protein")
            ? "Protein.genes.allGoAnnotation.property" : "Gene.allGoAnnotation.property");
            q.addConstraint(pathString,  Constraints.lookup(key), "C", "GOTerm");
            q.setConstraintLogic("A and B and C");
        }
        q.syncLogicExpression("and");
        return q;
    }
}
