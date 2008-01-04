package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinFeature;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author Julie Sullivan
 */
public class ProteinDomainLdr implements EnrichmentWidgetLdr
{

    Query sampleQuery;
    Query populationQuery;
    Collection organisms;
    int total, numberOfTests;
    String externalLink, append;
    InterMineBag bag;
    
    /**
     * @param request The HTTP request we are processing
     */
    public ProteinDomainLdr(HttpServletRequest request) {

        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStoreInterMineImpl os =
            (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);

        String bagName = request.getParameter("bagName");
        Map<String, InterMineBag> allBags =
            WebUtil.getAllBags(profile.getSavedBags(), servletContext);
        bag = allBags.get(bagName);

        // get organisms
        organisms = BioUtil.getOrganisms(os, bag);

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcProteinFeature = new QueryClass(ProteinFeature.class);

        QueryField qfProteinId = new QueryField(qcProtein, "id");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfName = new QueryField(qcProteinFeature, "name");
        QueryField qfId = new QueryField(qcProteinFeature, "interproId");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfInterpro = new QueryField(qcProteinFeature, "identifier");

        QueryFunction objectCount = new QueryFunction();

        // constraints
        ConstraintSet csSample = new ConstraintSet(ConstraintOp.AND);
        ConstraintSet csPopulation = new ConstraintSet(ConstraintOp.AND);

        // common constraints
        // limit to organisms in the bag
        BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);

        // protein.ProteinFeatures CONTAINS proteinFeature
        QueryCollectionReference qr3
        = new QueryCollectionReference(qcProtein, "proteinFeatures");
        ContainsConstraint cc3 =
            new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcProteinFeature);

        SimpleConstraint sc =
            new SimpleConstraint(qfInterpro, ConstraintOp.MATCHES, new QueryValue("IPR%"));

        //set the common constraints
        csSample.addConstraint(bc2);
        csSample.addConstraint(cc3);
        csSample.addConstraint(sc);

        // build sample (constrained by list) and population queries
        Query q = new Query();
        q.setDistinct(false);

        if (bag.getType().equalsIgnoreCase("gene")) {
            // further constraints for genes
            // genes must be in bag
            BagConstraint bc1 =
                new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
            csSample.addConstraint(bc1);

            // gene is from organism
            QueryObjectReference qr1 = new QueryObjectReference(qcGene, "organism");
            ContainsConstraint cc1
            = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism);
            csSample.addConstraint(cc1);

            // gene.Proteins CONTAINS protein
            QueryCollectionReference qr2 = new QueryCollectionReference(qcGene, "proteins");
            ContainsConstraint cc2 =
                new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcProtein);
            csSample.addConstraint(cc2);


            // sample query
            q.addFrom(qcGene);
            q.addFrom(qcProtein);
            q.addFrom(qcOrganism);
            q.addFrom(qcProteinFeature);

            q.addToSelect(qfId);
            q.addToSelect(objectCount);
            q.addToSelect(qfName);

            q.setConstraint(csSample);
            q.addToGroupBy(qfId);
            q.addToGroupBy(qfName);

            sampleQuery = q;

            // population query
            q = new Query();
            q.setDistinct(false);

            q.addFrom(qcGene);
            q.addFrom(qcProtein);
            q.addFrom(qcOrganism);
            q.addFrom(qcProteinFeature);

            q.addToSelect(qfId);
            q.addToSelect(objectCount);

            csPopulation.addConstraint(cc1);
            csPopulation.addConstraint(cc2);
            csPopulation.addConstraint(cc3);
            csPopulation.addConstraint(bc2);
            csPopulation.addConstraint(sc);
            q.setConstraint(csPopulation);

            q.addToGroupBy(qfId);

            populationQuery = q;

        } else if (bag.getType().equalsIgnoreCase("protein")) {

            // further constraints for proteins
            // proteins must be in bag
            BagConstraint bc1 =
                new BagConstraint(qfProteinId, ConstraintOp.IN, bag.getOsb());
            csSample.addConstraint(bc1);

            // protein is from organism
            QueryObjectReference qr1 = new QueryObjectReference(qcProtein, "organism");
            ContainsConstraint cc1
            = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism);
            csSample.addConstraint(cc1);

            // sample query
            q.addFrom(qcProtein);
            q.addFrom(qcOrganism);
            q.addFrom(qcProteinFeature);

            q.addToSelect(qfId);
            q.addToSelect(objectCount);
            q.addToSelect(qfName);

            q.setConstraint(csSample);
            q.addToGroupBy(qfId);
            q.addToGroupBy(qfName);

            sampleQuery = q;

            // population query
            q = new Query();
            q.setDistinct(false);

            q.addFrom(qcProtein);
            q.addFrom(qcOrganism);
            q.addFrom(qcProteinFeature);

            q.addToSelect(qfId);
            q.addToSelect(objectCount);

            csPopulation.addConstraint(cc1);
            csPopulation.addConstraint(cc3);
            csPopulation.addConstraint(bc2);
            csPopulation.addConstraint(sc);

            q.addToGroupBy(qfId);
            populationQuery = q;

        }

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
    public Collection getReferencePopulation() {
        return organisms;
    }

    /**
     * {@inheritDoc} 
     */
    public int getTotal(ObjectStore os) {
        return BioUtil.getTotal(os, organisms, bag.getType());
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




