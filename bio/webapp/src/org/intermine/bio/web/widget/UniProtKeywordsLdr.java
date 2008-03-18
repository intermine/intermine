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

import org.flymine.model.genomic.Ontology;
import org.flymine.model.genomic.OntologyTerm;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;
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
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class UniProtKeywordsLdr implements EnrichmentWidgetLdr
{

    private Query annotatedSampleQuery;
    private Query annotatedPopulationQuery;
    private Collection<String> organisms;
    private String externalLink, append;
    private ObjectStore os;
    private InterMineBag bag;
    private Collection<String> organismsLower = new ArrayList<String>();
    /**
     * @param bag list of objects for this widget
     * @param os object store
     */
    public UniProtKeywordsLdr(InterMineBag bag, ObjectStore os) {
        this.bag = bag;
        this.os = os;
        organisms = BioUtil.getOrganisms(os, bag, false);
        
        annotatedSampleQuery = getQuery(false, true);
        annotatedPopulationQuery = getQuery(false, false);
        for (String s : organisms) {
            organismsLower.add(s.toLowerCase());
        }
        organismsLower = new ArrayList<String>();
    }
    
    /**
     * {@inheritDoc}
     */    
    public Query getQuery(boolean calcTotal, boolean useBag) {
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcOntology = new QueryClass(Ontology.class);
        QueryClass qcOntoTerm = new QueryClass(OntologyTerm.class);

        QueryField qfProtId = new QueryField(qcProtein, "id");
        QueryField qfName = new QueryField(qcOntoTerm, "name");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfOnto = new QueryField(qcOntology, "title");

        QueryFunction protCount = new QueryFunction();

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (useBag) {
            cs.addConstraint(new BagConstraint(qfProtId, ConstraintOp.IN, bag.getOsb()));
        }
        QueryExpression qf = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, organismsLower));
        
        QueryObjectReference qor1 = new QueryObjectReference(qcProtein, "organism");
        cs.addConstraint(new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qcOrganism));

        QueryCollectionReference qcr = new QueryCollectionReference(qcProtein, "keywords");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcOntoTerm));

        QueryObjectReference qor2 = new QueryObjectReference(qcOntoTerm, "ontology");
        cs.addConstraint(new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qcOntology));

        cs.addConstraint(new SimpleConstraint(qfOnto, ConstraintOp.EQUALS, 
                                               new QueryValue("UniProtKeyword")));

        Query q = new Query();
        q.setDistinct(false);

        q.addFrom(qcProtein);
        q.addFrom(qcOrganism);
        q.addFrom(qcOntology);
        q.addFrom(qcOntoTerm);

        if (!calcTotal) {
            q.addToSelect(qfName);
        }
        q.addToSelect(protCount);        
        q.setConstraint(cs);
        if (!calcTotal) {            
            if (useBag) {
                q.addToSelect(qfName);
            } 
            q.addToGroupBy(qfName);
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




