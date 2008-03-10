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


import java.util.Collection;
import java.util.Map;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.flymine.model.genomic.Ontology;
import org.flymine.model.genomic.OntologyTerm;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;

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
    private ObjectStoreInterMineImpl os;
    private InterMineBag bag;

    /**
     * @param request The HTTP request we are processing
     */
    public UniProtKeywordsLdr(HttpServletRequest request) {

        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        os = (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);
        String bagName = request.getParameter("bagName");
        Map<String, InterMineBag> allBags =
            WebUtil.getAllBags(profile.getSavedBags(), servletContext);
        bag = allBags.get(bagName);
        organisms = BioUtil.getOrganisms(os, bag, false);
        
        annotatedSampleQuery = getQuery(false, true);
        annotatedPopulationQuery = getQuery(false, false);
       
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

        cs.addConstraint(new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms));

        QueryObjectReference qor1 = new QueryObjectReference(qcProtein, "organism");
        cs.addConstraint(new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qcOrganism));

        QueryCollectionReference qcr = new QueryCollectionReference(qcProtein, "keywords");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcOntoTerm));

        QueryObjectReference qor2 = new QueryObjectReference(qcOntoTerm, "ontology");
        cs.addConstraint(new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qcOntology));

        cs.addConstraint(new SimpleConstraint(qfOnto, ConstraintOp.MATCHES, 
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




