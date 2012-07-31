package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.metadata.Model;
import org.apache.log4j.Logger;
import org.intermine.bio.web.logic.BioUtil;
import org.intermine.model.bio.Disease;
import org.intermine.model.bio.Gene;
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
public class DiseaseEnrichLdr extends EnrichmentWidgetLdr
{
    private Collection<String> organisms = new ArrayList<String>();
    private Collection<String> organismsLower = new ArrayList<String>();
    private InterMineBag bag;
    private static final Logger LOG = Logger.getLogger(DiseaseEnrichLdr.class);
    private Model model;

    /**
     * Create a new PublicationLdr
     * @param bag the bag to process
     * @param os the ObjectStore
     * @param extraAttribute (not used)
     */
    public DiseaseEnrichLdr(InterMineBag bag, ObjectStore os, String extraAttribute) {
    	LOG.error("HERE!");
        this.bag = bag;
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(String action, List<String> keys) {
    	
        // classes for FROM clause
        QueryClass qcDisease = new QueryClass(Disease.class);
        QueryClass qcGene = new QueryClass(Gene.class);
        
        // fields for SELECT clause
        QueryField qfDiseaseName = new QueryField(qcDisease, "diseaseId");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfGeneName = new QueryField(qcGene, "primaryIdentifier");
        
        // constraints
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // constrain genes to be in list
        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb()));
        }
        
        // disease.publication = publication
        QueryCollectionReference qcr = new QueryCollectionReference(qcGene, "diseases");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcDisease));
        

        Query q = new Query();
        q.setDistinct(true);
        
        // from statement
        q.addFrom(qcGene);
        q.addFrom(qcDisease);
        
        // add constraints to query
        q.setConstraint(cs);
        
        // needed for the 'not analysed' number
        if ("analysed".equals(action)) {
            q.addToSelect(qfGeneId);
        // export query
        // needed for export button on widget
        } else if ("export".equals(action)) {
        	
            q.addToSelect(qfDiseaseName);
            q.addToSelect(qfGeneName);
            q.addToOrderBy(qfDiseaseName);
            
        // total queries
        // needed for enrichment calculations
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfGeneId);
            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction()); // disease count
            
        // needed for enrichment calculations
        } else  {
        	
            q.addToSelect(qfDiseaseName);
            q.addToGroupBy(qfDiseaseName);
            q.addToSelect(new QueryFunction()); // disease count
            if ("sample".equals(action)) {
                q.addToSelect(qfDiseaseName);
                q.addToGroupBy(qfDiseaseName);
            }
        }
        return q;
    }
}




