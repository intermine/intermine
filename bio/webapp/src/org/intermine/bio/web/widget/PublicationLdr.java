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

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Publication;

/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class PublicationLdr implements EnrichmentWidgetLdr
{
    private Collection<String> organisms;
    private String externalLink, append;
    private InterMineBag bag;
    private Collection<String> organismsLower = new ArrayList<String>();
 
    /**
     * Constructor
     * @param bag the bag
     * @param os the ObjectStore
     * @param extraAttribute an extra attribute, probably organism
     */
    public PublicationLdr(InterMineBag bag, ObjectStore os, String extraAttribute) {
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

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcPub = new QueryClass(Publication.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);

        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfId = new QueryField(qcPub, "pubMedId");
        QueryField qfPubTitle = new QueryField(qcPub, "title");
        QueryField qfGeneIdentifier = new QueryField(qcGene, "primaryIdentifier");
        
        QueryFunction geneCount = new QueryFunction();

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (useBag) {
            cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb()));
        }
        QueryExpression qe = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        cs.addConstraint(new BagConstraint(qe, ConstraintOp.IN, organismsLower));
        
        QueryObjectReference qor = new QueryObjectReference(qcGene, "organism");
        cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS, qcOrganism));

        QueryCollectionReference qcr = new QueryCollectionReference(qcGene, "publications");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcPub));

        Query q = new Query();
        
        if (!calcTotal) {            
            q.setDistinct(false);
            q.addFrom(qcGene);
            q.addFrom(qcPub);
            q.addFrom(qcOrganism);        
            q.addToSelect(qfId);
            q.addToGroupBy(qfId);
            q.addToSelect(geneCount);
            if (useBag) {
                q.addToSelect(qfPubTitle);
                q.addToGroupBy(qfPubTitle);
            }
            q.setConstraint(cs);            
        } else {            
            Query subQ = new Query();
            subQ.setDistinct(true);

            subQ.addFrom(qcGene);
            subQ.addFrom(qcPub);
            subQ.addFrom(qcOrganism);
                
            subQ.addToSelect(qfGeneId);
            //subQ.addToGroupBy(qfGeneId);
            subQ.setConstraint(cs);
                      
            q.setDistinct(false);
            q.addFrom(subQ);
            q.addToSelect(geneCount);
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



