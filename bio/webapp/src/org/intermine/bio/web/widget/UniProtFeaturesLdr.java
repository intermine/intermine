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

import org.intermine.bio.web.logic.BioUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.UniProtFeature;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author Julie Sullivan
 */
public class UniProtFeaturesLdr implements EnrichmentWidgetLdr
{

    Query sampleQuery;
    Query populationQuery;
    Collection organisms;
    int total, numberOfTests;
    String externalLink, append;

    /**
     * @param request The HTTP request we are processing
     */
    public UniProtFeaturesLdr(HttpServletRequest request) {

        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStoreInterMineImpl os =
            (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);

        String bagName = request.getParameter("bagName");
        Map<String, InterMineBag> allBags =
            WebUtil.getAllBags(profile.getSavedBags(), servletContext);
        InterMineBag bag = allBags.get(bagName);

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcUniProtFeature = new QueryClass(UniProtFeature.class);

        QueryField qfProtId = new QueryField(qcProtein, "id");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfName = new QueryField(qcUniProtFeature, "type");

        QueryFunction protCount = new QueryFunction();

        q.addFrom(qcProtein);
        q.addFrom(qcOrganism);
        q.addFrom(qcUniProtFeature);

        q.addToSelect(qfName);
        q.addToSelect(protCount);
        q.addToSelect(qfName);

        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);

        BagConstraint bc1 = new BagConstraint(qfProtId, ConstraintOp.IN, bag.getOsb());
        cs1.addConstraint(bc1);

        organisms = BioUtil.getOrganisms(os, bag);

        BagConstraint bc2 = new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms);
        cs1.addConstraint(bc2);

        QueryObjectReference qr1 = new QueryObjectReference(qcProtein, "organism");
        ContainsConstraint cc1
        = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcOrganism);
        cs1.addConstraint(cc1);

        QueryCollectionReference qr2 = new QueryCollectionReference(qcProtein, "features");
        ContainsConstraint cc2 =
            new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcUniProtFeature);
        cs1.addConstraint(cc2);

        q.setConstraint(cs1);
        q.addToGroupBy(qfName);

        sampleQuery = q;

        // construct population query
        q = new Query();
        q.setDistinct(false);

        q.addFrom(qcProtein);
        q.addFrom(qcOrganism);
        q.addFrom(qcUniProtFeature);

        q.addToSelect(qfName);
        q.addToSelect(protCount);

        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(cc1);
        cs2.addConstraint(cc2);
        cs2.addConstraint(bc2);
        q.setConstraint(cs2);
        q.addToGroupBy(qfName);
        populationQuery = q;
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
        return BioUtil.getTotal(os, organisms, "Protein");
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
     * {@inheritDoc} 
     */
    public int getNumberOfTests() {
        return numberOfTests;
    }
}




