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
public class UniProtFeaturesLdr implements EnrichmentWidgetLdr
{

    private Collection<String> organisms;
    private String externalLink, append;
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
    public Query getQuery(boolean calcTotal, boolean useBag) {

        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcUniProtFeature = new QueryClass(UniProtFeature.class);

        QueryField qfProtId = new QueryField(qcProtein, "id");
        QueryField qfName = new QueryField(qcUniProtFeature, "type");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (useBag) {
            cs.addConstraint(new BagConstraint(qfProtId, ConstraintOp.IN, bag.getOsb()));
        }

        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryExpression qf = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, organismsLower));

        QueryObjectReference qor = new QueryObjectReference(qcProtein, "organism");
        cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS, qcOrganism));

        QueryCollectionReference qcr = new QueryCollectionReference(qcProtein, "features");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcUniProtFeature));

        Query subQ = new Query();
        subQ.setDistinct(true);

        subQ.addFrom(qcProtein);
        subQ.addFrom(qcOrganism);
        subQ.addFrom(qcUniProtFeature);
        subQ.addToSelect(qfProtId);
        subQ.addToSelect(qfName);

        subQ.setConstraint(cs);

        QueryFunction protCount = new QueryFunction();
        QueryField qfType = new QueryField(subQ, qfName);

        Query q = new Query();
        q.setDistinct(false);
        q.addFrom(subQ);
        if (!calcTotal) {
            q.addToSelect(qfType);
        }
        q.addToSelect(protCount);
        if (useBag && !calcTotal) {
            q.addToSelect(qfType);
        }
        if (!calcTotal) {
            q.addToGroupBy(qfType);
        }

        return q;
    }

    /**
     * {@inheritDoc}
     */
    public Query getAnnotatedSampleQuery(boolean calcTotal) {
        return getQuery(calcTotal, true);
    }

    /**
     * {@inheritDoc}
     */
    public Query getAnnotatedPopulationQuery() {
        return getQuery(false, false);
    }
    
    /**
     * {@inheritDoc}
     */
    public Collection<String> getPopulationDescr() {
        return organisms;
    }

    /**
     * {@inheritDoc}
     */
    public String getExternalLink() {
        return externalLink;
    }

    /**
     * {@inheritDoc}
     */
    public String getAppendage() {
        return append;
    }
}




