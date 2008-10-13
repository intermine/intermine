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
import java.util.List;

import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.UniProtFeature;
import org.intermine.bio.web.logic.BioUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class UniProtFeaturesLdr extends EnrichmentWidgetLdr
{

    private Collection<String> organisms;
    private InterMineBag bag;
    private Collection<String> organismsLower = new ArrayList<String>();

    /**
     * Create a new UniProtFeaturesLdr.
     * @param bag the bag to process
     * @param os the ObjectStore
     * @param extraAttribute an extra attribute for this widget (if needed)
     */
    public UniProtFeaturesLdr(InterMineBag bag, ObjectStore os, String extraAttribute) {
        this.bag = bag;
        organisms = BioUtil.getOrganisms(os, bag, false);
        for (String s : organisms) {
            organismsLower.add(s.toLowerCase());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(String action, List<String> keys) {

        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcUniProtFeature = new QueryClass(UniProtFeature.class);

        QueryField qfProtId = new QueryField(qcProtein, "id");
        QueryField qfName = new QueryField(qcUniProtFeature, "type");
        QueryField qfPrimaryIdentifier = new QueryField(qcProtein, "primaryIdentifier");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfName, ConstraintOp.IN, keys));
        }

        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfProtId, ConstraintOp.IN, bag.getOsb()));
        }

        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryExpression qf = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, organismsLower));

        QueryObjectReference qor = new QueryObjectReference(qcProtein, "organism");
        cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS, qcOrganism));

        QueryCollectionReference qcr = new QueryCollectionReference(qcProtein, "features");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcUniProtFeature));

        Query q = new Query();
        q.setDistinct(true);

        q.addFrom(qcProtein);
        q.addFrom(qcOrganism);
        q.addFrom(qcUniProtFeature);
        q.setConstraint(cs);


        // needed for the 'not analysed' number
        if (action.equals("analysed")) {
            q.addToSelect(qfProtId);
        // export button on the widget
        } else if (action.equals("export")) {
            q.addToSelect(qfName);
            q.addToSelect(qfPrimaryIdentifier);
            q.addToOrderBy(qfName);
        // used to calculate total values
        // needed for enrichment calculations
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfProtId);
            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction());
        } else  {
            Query subQ = new Query();
            subQ = q;
            subQ.addToSelect(qfName);
            QueryField qfType = new QueryField(subQ, qfName);

            q.setDistinct(false);
            q.addFrom(subQ);
            q.addToSelect(qfType);
            q.addToSelect(new QueryFunction());
            if (action.equals("sample")) {
                q.addToSelect(qfType);
            }
            q.addToGroupBy(qfType);
        }
        return q;
    }
}




