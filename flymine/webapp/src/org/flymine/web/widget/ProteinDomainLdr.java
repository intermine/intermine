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
import java.util.List;
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
import org.flymine.model.genomic.ProteinDomain;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class ProteinDomainLdr implements EnrichmentWidgetLdr
{

    Query sampleQuery;
    Query populationQuery;
    String externalLink, append;
    InterMineBag bag;
    Collection<String> organisms;

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
        sampleQuery = getQuery(os, true);
        populationQuery = getQuery(os, false);
    }

    private Query getQuery(ObjectStore os, boolean useBag) {

        organisms = BioUtil.getOrganisms(os, bag);

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcProteinFeature = new QueryClass(ProteinDomain.class);

        QueryField qfProteinId = new QueryField(qcProtein, "id");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfName = new QueryField(qcProteinFeature, "name");
        QueryField qfId = new QueryField(qcProteinFeature, "identifier");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");

        QueryFunction objectCount = new QueryFunction();

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        cs.addConstraint(new BagConstraint(qfOrganismName, ConstraintOp.IN, organisms));
        QueryCollectionReference qr = new QueryCollectionReference(qcProtein, "proteinDomains");
        cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcProteinFeature));
        cs.addConstraint(new SimpleConstraint(qfId, ConstraintOp.MATCHES, new QueryValue("IPR%")));

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

        q.addToSelect(qfId);
        q.addToSelect(objectCount);

        q.setConstraint(cs);

        q.addToGroupBy(qfId);
        if (useBag) {
            q.addToSelect(qfName);
            q.addToGroupBy(qfName);
        }

        return q;
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




