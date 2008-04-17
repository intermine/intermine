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
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.flymine.model.genomic.GOAnnotation;
import org.flymine.model.genomic.GOTerm;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;

/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class GoStatLdr implements EnrichmentWidgetLdr
{
    private Query annotatedSampleQuery;
    private Query annotatedPopulationQuery;
    private Collection<String> organisms;
    private String externalLink, append;
    private InterMineBag bag;
    private String namespace;    
    private Collection<String> organismsLower = new ArrayList<String>();
    /**
     * @param bag list of objects for this widget
     * @param os object store
     */
    public GoStatLdr (InterMineBag bag, ObjectStore os, String extraAttribute) {
        this.bag = bag;
        namespace = extraAttribute;
        organisms = BioUtil.getOrganisms(os, bag, false);
        
        for (String s : organisms) {
            organismsLower.add(s.toLowerCase());
        }
        annotatedSampleQuery = getQuery(true);
        annotatedPopulationQuery = getQuery(false);
                
    }

    // adds 3 main ontologies to array.  these 3 will be excluded from the query
    private String[] getOntologies() {

        String[] ids = new String[3];

        ids[0] = "go:0008150";  // biological_process
        ids[1] = "go:0003674";  // molecular_function
        ids[2] = "go:0005575";  // cellular_component

        return ids;

    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(boolean useBag) {

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcGoAnnotation = new QueryClass(GOAnnotation.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcGo = new QueryClass(GOTerm.class);
        QueryClass qcProtein = new QueryClass(Protein.class);

        QueryField qfQualifier = new QueryField(qcGoAnnotation, "qualifier");
        QueryField qfGoTerm = new QueryField(qcGoAnnotation, "name");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfNamespace = new QueryField(qcGo, "namespace");
        QueryField qfGoTermId = new QueryField(qcGo, "identifier");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfProteinId = new QueryField(qcProtein, "id");

        QueryFunction objectCount = new QueryFunction();
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
      
        QueryExpression qf1 = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        cs.addConstraint(new BagConstraint(qf1, ConstraintOp.IN, organismsLower));
        
        // gene.goAnnotation CONTAINS GOAnnotation
        QueryCollectionReference qcr1 = new QueryCollectionReference(qcGene, "allGoAnnotation");
        cs.addConstraint(new ContainsConstraint(qcr1, ConstraintOp.CONTAINS, qcGoAnnotation));


        String[] ids = getOntologies();
        QueryExpression qf2 = new QueryExpression(QueryExpression.LOWER, qfGoTermId);
        for (int i = 0; i < ids.length; i++) {                
            cs.addConstraint(new SimpleConstraint(qf2, ConstraintOp.NOT_EQUALS, 
                                                  new QueryValue(ids[i])));
        }
        // gene is from organism
        QueryObjectReference qor1 = new QueryObjectReference(qcGene, "organism");
        cs.addConstraint(new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qcOrganism));

        // goannotation contains go term
        QueryObjectReference qor2 = new QueryObjectReference(qcGoAnnotation, "property");
        cs.addConstraint(new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qcGo));

        // can't be a NOT relationship!
        cs.addConstraint(new SimpleConstraint(qfQualifier, ConstraintOp.IS_NULL));

        // go term is of the specified namespace
        QueryExpression qf3 = new QueryExpression(QueryExpression.LOWER, qfNamespace);
        cs.addConstraint(new SimpleConstraint(qf3, ConstraintOp.EQUALS, 
                                              new QueryValue(namespace.toLowerCase())));

        if (useBag) {
            if (bag.getType().equalsIgnoreCase("protein")) {
                cs.addConstraint(new BagConstraint(qfProteinId, ConstraintOp.IN, bag.getOsb()));
            } else {
                cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb()));
            }
        }

        if (bag.getType().equalsIgnoreCase("protein")) {
            QueryObjectReference qor3 = new QueryObjectReference(qcProtein, "organism");
            cs.addConstraint(new ContainsConstraint(qor3, ConstraintOp.CONTAINS, qcOrganism));

            QueryCollectionReference qcr2 = new QueryCollectionReference(qcProtein, "genes");
            cs.addConstraint(new ContainsConstraint(qcr2, ConstraintOp.CONTAINS, qcGene));
        } else {
            QueryObjectReference qor4 = new QueryObjectReference(qcGene, "organism");
            cs.addConstraint(new ContainsConstraint(qor4, ConstraintOp.CONTAINS, qcOrganism));
        }

        Query q = new Query();
        q.setDistinct(false);

        q.addFrom(qcGene);
        q.addFrom(qcGoAnnotation);
        q.addFrom(qcOrganism);

        q.addFrom(qcGo);

        if (bag.getType().equalsIgnoreCase("protein")) {
            q.addFrom(qcProtein);
        }

        q.addToSelect(qfGoTermId);
        q.addToSelect(objectCount);

        q.setConstraint(cs);
        q.addToGroupBy(qfGoTermId);

        if (useBag) {
            q.addToSelect(qfGoTerm);
            q.addToGroupBy(qfGoTerm);            
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

    /**
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}



