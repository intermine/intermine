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

import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.MRNAExpressionResult;
import org.flymine.model.genomic.MRNAExpressionTerm;
import org.flymine.model.genomic.Organism;

/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class BDGPLdr implements EnrichmentWidgetLdr
{

    private Query annotatedSampleQuery;
    private Query annotatedPopulationQuery;
    private String externalLink, append;
    private Collection<String> organisms = new ArrayList<String>();
    private Collection<String> organismsLower = new ArrayList<String>();    
    private InterMineBag bag;
 
    /**
     * @param bag list of objects for this widget
     * @param os object store
     * @param extraAttribute an extra attribute
     */
    public BDGPLdr(InterMineBag bag, ObjectStore os, String extraAttribute) {
        this.bag = bag;
        
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

        
        QueryClass qcMrnaResult = new QueryClass(MRNAExpressionResult.class);
        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcDataset = new QueryClass(DataSet.class);
        QueryClass qcTerm = new QueryClass(MRNAExpressionTerm.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
                
        QueryField qfGene = new QueryField(qcGene, "id");
        QueryField qfTerm = new QueryField(qcTerm, "name");        
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (useBag) {
            cs.addConstraint(new BagConstraint(qfGene, ConstraintOp.IN, bag.getOsb()));
        }

        QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
        cs.addConstraint(new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism));

        QueryCollectionReference r1 = new QueryCollectionReference(qcGene, "mRNAExpressionResults");
        cs.addConstraint(new ContainsConstraint(r1, ConstraintOp.CONTAINS, qcMrnaResult));

        // we only want results that showed expression
        QueryField qfExpressed = new QueryField(qcMrnaResult, "expressed");
        SimpleConstraint scExpressed = new SimpleConstraint(qfExpressed, ConstraintOp.EQUALS,
                                                            new QueryValue(Boolean.TRUE));
        cs.addConstraint(scExpressed);
        
        QueryCollectionReference r2 = new QueryCollectionReference(qcMrnaResult, 
                                                                   "mRNAExpressionTerms");
        cs.addConstraint(new ContainsConstraint(r2, ConstraintOp.CONTAINS, qcTerm));
        
        QueryObjectReference qcr = new QueryObjectReference(qcMrnaResult, "source");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcDataset));

        String dataset = "BDGP in situ data set";
        QueryExpression qf2 = new QueryExpression(QueryExpression.LOWER,
                                                  new QueryField(qcDataset, "title"));
        cs.addConstraint(new SimpleConstraint(qf2, ConstraintOp.EQUALS, 
                                              new QueryValue(dataset.toLowerCase())));

        Query subQ = new Query();
        subQ.setDistinct(true);

        subQ.addFrom(qcTerm);
        subQ.addFrom(qcMrnaResult);
        subQ.addFrom(qcGene);
        subQ.addFrom(qcDataset);
        subQ.addFrom(qcOrganism);
        
        subQ.addToSelect(new QueryField(qcTerm, "id"));
        subQ.addToSelect(new QueryField(qcGene, "id"));
        subQ.addToSelect(qfTerm);

        subQ.setConstraint(cs);

        Query q = new Query();
        q.setDistinct(false);
        QueryFunction qfCount = new QueryFunction();
        QueryField outerQfTerm = new QueryField(subQ, qfTerm);
        
        q.addFrom(subQ);
        
        if (!calcTotal) {            
            q.addToSelect(outerQfTerm);
            q.addToGroupBy(outerQfTerm);
        }
        
        q.addToSelect(qfCount);
        
        if (!calcTotal) { 
            if (useBag) {
                q.addToSelect(outerQfTerm);
            }
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




