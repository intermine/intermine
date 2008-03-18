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
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinDomain;

/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class ProteinDomainLdr implements EnrichmentWidgetLdr
{

    private Query annotatedSampleQuery;
    private Query annotatedPopulationQuery;
    private String externalLink, append;
    private Collection<String> organisms;
    private Collection<String> organismsLower = new ArrayList<String>();
    private ObjectStore os;
    private InterMineBag bag;
 
    /**
     * @param bag list of objects for this widget
     * @param os object store
     */
    public ProteinDomainLdr(InterMineBag bag, ObjectStore os) {
        this.bag = bag;
        this.os = os;
        organisms = BioUtil.getOrganisms(os, bag, false);
        
        for (String s : organisms) {
            organismsLower.add(s.toLowerCase());
        }
        
        annotatedSampleQuery = getQuery(false, true);
        annotatedPopulationQuery = getQuery(false, false);
    }
    
    /**
     * {@inheritDoc}
     */
    public Query getQuery(boolean calcTotal, boolean useBag) {

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcProteinFeature = new QueryClass(ProteinDomain.class);

        QueryField qfProteinId = new QueryField(qcProtein, "id");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfName = new QueryField(qcProteinFeature, "name");
        QueryField qfId = new QueryField(qcProteinFeature, "primaryIdentifier");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");

        QueryFunction objectCount = new QueryFunction();

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryExpression qf1 = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        cs.addConstraint(new BagConstraint(qf1, ConstraintOp.IN, organismsLower));
        QueryCollectionReference qr = new QueryCollectionReference(qcProtein, "proteinDomains");
        cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcProteinFeature));
        QueryExpression qf2 = new QueryExpression(QueryExpression.LOWER, qfId);
        cs.addConstraint(new SimpleConstraint(qf2, ConstraintOp.MATCHES, new QueryValue("ipr%")));

        if (useBag) {
            if (bag.getType().equalsIgnoreCase("protein")) {
                cs.addConstraint(new BagConstraint(qfProteinId, ConstraintOp.IN, bag.getOsb()));
            } else {
                cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb()));
            }
        }

        if (bag.getType().equalsIgnoreCase("protein")) {
            QueryObjectReference qr1 = new QueryObjectReference(qcProtein, "organism");
            cs.addConstraint(new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism));
        } else {
            QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
            cs.addConstraint(new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism));

            QueryCollectionReference qr2 = new QueryCollectionReference(qcGene, "proteins");
            cs.addConstraint(new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcProtein));
        }

        Query q = new Query();
        q.setDistinct(false);
        if (bag.getType().equalsIgnoreCase("gene")) {
            q.addFrom(qcGene);
        }
        q.addFrom(qcProtein);
        q.addFrom(qcOrganism);
        q.addFrom(qcProteinFeature);

        if (!calcTotal) {
            q.addToSelect(qfId);
        }
        q.addToSelect(objectCount);

        q.setConstraint(cs);
        
        if (!calcTotal) {            
            if (useBag) {
                q.addToSelect(qfName);
                q.addToGroupBy(qfName);
            }
            q.addToGroupBy(qfId);
        }

        return q;
    }

    /**
     * {@inheritDoc}
     */
    public Query getAnnotatedSample() {
        return annotatedSampleQuery;
    }

    /**
     * {@inheritDoc}
     */
    public Query getAnnotatedPopulation() {
        return annotatedPopulationQuery;
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




