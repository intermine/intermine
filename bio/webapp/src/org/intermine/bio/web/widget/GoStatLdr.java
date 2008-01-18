package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
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
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.flymine.model.genomic.GOAnnotation;
import org.flymine.model.genomic.GOTerm;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * {@inheritDoc}
 */
public class GoStatLdr implements EnrichmentWidgetLdr
{
    Query sampleQuery;
    Query populationQuery;
    Collection<String> organisms;
    int total, numberOfTests;
    String externalLink, append;

    /**
     * @param request The HTTP request we are processing
     */
    public GoStatLdr (HttpServletRequest request) {

        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStoreInterMineImpl os =
            (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);

        String bagName = request.getParameter("bagName");
        Map<String, InterMineBag> allBags =
            WebUtil.getAllBags(profile.getSavedBags(), servletContext);
        InterMineBag bag = allBags.get(bagName);
        String bagType = bag.getType();
        String namespace = (request.getParameter("filter") != null
                        ? request.getParameter("filter") : "biological_process");

        // list of ontologies to ignore
        Collection<String> badOntologies = getOntologies();

        // build query constrained by bag
        Query q = new Query();
        q.setDistinct(false);
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

        q.addFrom(qcGene);
        q.addFrom(qcGoAnnotation);
        q.addFrom(qcOrganism);
        q.addFrom(qcGo);
        if (bagType.toLowerCase().equals("protein")) {
            q.addFrom(qcProtein);
        }

        q.addToSelect(qfGoTermId);
        q.addToSelect(objectCount);
        q.addToSelect(qfGoTerm);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // objects (genes, proteins) must be in bag
        BagConstraint bc1 = null;
        if (bagType.toLowerCase().equals("protein")) {
            bc1 = new BagConstraint(qfProteinId, ConstraintOp.IN, bag.getOsb());
        } else {
            bc1 = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
        }
        cs.addConstraint(bc1);

        // get organisms
        organisms = BioUtil.getOrganisms(os, bag);

        // limit to organisms in the bag
        BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);

        cs.addConstraint(bc2);

        ContainsConstraint cc = null;
        if (bagType.toLowerCase().equals("protein")) {
            // constrain gene to protein
            QueryCollectionReference qr = new QueryCollectionReference(qcProtein, "genes");
            cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcGene);
            cs.addConstraint(cc);
        }

        // ignore main 3 ontologies
        BagConstraint bc3 = new BagConstraint(qfGoTermId, ConstraintOp.NOT_IN, badOntologies);
        cs.addConstraint(bc3);

        // gene.goAnnotation CONTAINS GOAnnotation
        QueryCollectionReference qr1 = new QueryCollectionReference(qcGene, "allGoAnnotation");
        ContainsConstraint cc1 =
            new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcGoAnnotation);
        cs.addConstraint(cc1);

        // gene is from organism
        QueryObjectReference qr2 = new QueryObjectReference(qcGene, "organism");
        ContainsConstraint cc2
        = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcOrganism);
        cs.addConstraint(cc2);

        // goannotation contains go term
        QueryObjectReference qr3 = new QueryObjectReference(qcGoAnnotation, "property");
        ContainsConstraint cc3 = new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcGo);
        cs.addConstraint(cc3);

        // can't be a NOT relationship!
        SimpleConstraint sc1 = new SimpleConstraint(qfQualifier,
                                                    ConstraintOp.IS_NULL);
        cs.addConstraint(sc1);

        // go term is of the specified namespace
        SimpleConstraint sc2 = new SimpleConstraint(qfNamespace,
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue(namespace));
        cs.addConstraint(sc2);
        q.setConstraint(cs);
        q.addToGroupBy(qfGoTerm);
        q.addToGroupBy(qfGoTermId);

        sampleQuery = q;

        // construct population query
        q = new Query();
        q.setDistinct(false);

        q.addFrom(qcGene);
        q.addFrom(qcGoAnnotation);
        q.addFrom(qcOrganism);
        q.addFrom(qcGo);
        if (bagType.toLowerCase().equals("protein")) {
            q.addFrom(qcProtein);
        }

        q.addToSelect(qfGoTermId);
        q.addToSelect(objectCount);

        cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cc1);
        cs.addConstraint(cc2);
        cs.addConstraint(cc3);
        cs.addConstraint(sc1);
        cs.addConstraint(sc2);
        cs.addConstraint(bc2);
        cs.addConstraint(bc3);
        if (bagType.toLowerCase().equals("protein")) {
            cs.addConstraint(cc);
        }
        q.setConstraint(cs);

        q.addToGroupBy(qfGoTermId);

        populationQuery = q;

    }

    // adds 3 main ontologies to array.  these 3 will be excluded from the query
    private Collection<String> getOntologies() {

        Collection<String> ids = new ArrayList<String>();

        ids.add("GO:0008150");  // biological_process
        ids.add("GO:0003674");  // molecular_function
        ids.add("GO:0005575");  // cellular_component

        return ids;

    }

    /**
     * {@inheritDoc} 
     */
    public Query getSample() {
        return sampleQuery;
    }

    /**
     * {@inheritDoc} 
     */
    public Query getPopulation() {
        return populationQuery;
    }

    /**
     * {@inheritDoc} 
     */
    public Collection<String> getReferencePopulation() {
        return organisms;
    }

    /**
     * {@inheritDoc} 
     */
    public int getTotal(ObjectStore os) {
        return BioUtil.getTotal(os, organisms, "Gene");
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



