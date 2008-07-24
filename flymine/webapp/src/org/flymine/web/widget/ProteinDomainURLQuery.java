package org.flymine.web.widget;

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

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.Constraints;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a query to get all the genes (in bag) associated with specified go term.
 * @author Julie Sullivan
 */
public class ProteinDomainURLQuery implements WidgetURLQuery
{

    InterMineBag bag;
    String key;
    ObjectStore os;

    /**
     * @param key which protein domain the user clicked on
     * @param bag bag
     * @param os object store
     */
    public ProteinDomainURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(Collection<InterMineObject> keys) {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);
        String bagType = bag.getType();

        String viewStrings = "";
        String domainStrings = "";

        if (bagType.equals("Gene")) {
            viewStrings = "Gene.proteins.primaryIdentifier,Gene.proteins.primaryAccession,"
                + "Gene.proteins.organism.name,Gene.primaryIdentifier,"
                + "Gene.secondaryIdentifier";
            domainStrings = "Gene.proteins.proteinDomains.primaryIdentifier,"
                + "Gene.proteins.proteinDomains.name";
        } else if (bagType.equals("Protein")) {
            viewStrings = "Protein.primaryIdentifier,"
                + "Protein.primaryAccession,Protein.organism.name";
        }

        q.setView(viewStrings);
        if (keys == null) {
            q.addView(domainStrings);
        }

        q.addConstraint(bagType,  Constraints.in(bag.getName()));

        if (keys != null) {
            q.addConstraint(bagType,  Constraints.notIn(new ArrayList(keys)));
        } else {
            String pathString = (bagType.equals("Gene") ? "Gene.proteins.proteinDomains"
                                                          : "Protein.proteinDomains");
            q.addConstraint(pathString,  Constraints.lookup(key));
        }

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        if (keys == null) {
            q.setOrderBy(domainStrings);
        }
        q.addOrderBy(viewStrings);

        return q;
    }
}
