package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2009 FlyMine
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
 * Builds a query to get all the genes (in bag) associated with specified go term.
 * @author Julie Sullivan
 */
public class ProteinDomainURLQuery implements WidgetURLQuery
{
    private InterMineBag bag;
    private String key;
    private ObjectStore os;

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
    public PathQuery generatePathQuery() {
        PathQuery q = new PathQuery(os.getModel());
        String bagType = bag.getType();
        String paths = "";
        if (bagType.equals("Gene")) {
            paths = "Gene.proteins.primaryIdentifier,Gene.proteins.primaryAccession,"
                + "Gene.proteins.organism.name,Gene.primaryIdentifier,"
                + "Gene.secondaryIdentifier,Gene.proteins.proteinDomains.primaryIdentifier,"
                + "Gene.proteins.proteinDomains.name";
        } else if (bagType.equals("Protein")) {
            paths = "Protein.primaryIdentifier,Protein.primaryAccession,Protein.organism.name";
        }
        q.setView(paths);
        q.addConstraint(bagType,  Constraints.in(bag.getName()));
        String pathString = (bagType.equals("Gene") ? "Gene.proteins.proteinDomains"
                                                         : "Protein.proteinDomains");
        q.addConstraint(pathString,  Constraints.lookup(key));
        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");
        q.setOrderBy(paths);
        return q;
    }
}
