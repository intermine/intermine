package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.bio.web.logic.BioUtil;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;
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
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

/**
 * @author Julie Sullivan
 */
public class ProteinDomainLdr extends EnrichmentWidgetLdr
{
    private Collection<String> organisms = new ArrayList<String>();
    private Collection<String> organismsLower = new ArrayList<String>();
    private InterMineBag bag;
    private static final Logger LOG = Logger.getLogger(ProteinDomainLdr.class);
    private Model model;

    /**
     * Create a new PublicationLdr
     * @param bag the bag to process
     * @param os the ObjectStore
     * @param extraAttribute (not used)
     */
    public ProteinDomainLdr(InterMineBag bag, ObjectStore os, String extraAttribute) {
        this.bag = bag;

        organisms = BioUtil.getOrganisms(os, bag, false);
        model = os.getModel();
        for (String s : organisms) {
            organismsLower.add(s.toLowerCase());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(String action, List<String> keys) {

        String bagType = bag.getType();

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcProteinFeature = null;
        try {
            qcProteinFeature = new QueryClass(Class.forName(model.getPackageName()
                                                          + ".ProteinDomain"));
        } catch (ClassNotFoundException e) {
            LOG.error("Error rendering protein domain widget", e);
            // don't throw an exception, return NULL instead.  The widget will display 'no
            // results'. the javascript that renders widgets assumes a valid widget and thus
            // can't handle an exception thrown here.
            return null;
        }
        QueryField qfProteinId = new QueryField(qcProtein, "id");
        QueryField qfGeneId = new QueryField(qcGene, "id");

        QueryField qfId = new QueryField(qcProteinFeature, "primaryIdentifier");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfPrimaryIdentifier = null;
        if ("Protein".equals(bagType)) {
            qfPrimaryIdentifier = new QueryField(qcProtein, "primaryIdentifier");
        } else {
            qfPrimaryIdentifier = new QueryField(qcGene, "primaryIdentifier");
        }
        QueryFunction objectCount = new QueryFunction();

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfId, ConstraintOp.IN, keys));
        }

        QueryExpression qf1 = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        cs.addConstraint(new BagConstraint(qf1, ConstraintOp.IN, organismsLower));
        QueryCollectionReference qr = new QueryCollectionReference(qcProtein, "proteinDomains");
        cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcProteinFeature));
        QueryExpression qf2 = new QueryExpression(QueryExpression.LOWER, qfId);
        cs.addConstraint(new SimpleConstraint(qf2, ConstraintOp.MATCHES, new QueryValue("ipr%")));

        if (!action.startsWith("population")) {
            if ("Protein".equals(bagType)) {
                cs.addConstraint(new BagConstraint(qfProteinId, ConstraintOp.IN, bag.getOsb()));
            } else {
                cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb()));
            }
        }

        if ("Protein".equals(bagType)) {
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

        Query subQ = new Query();
        subQ.setDistinct(true);

        subQ.addFrom(qcProtein);
        subQ.addFrom(qcOrganism);
        subQ.addFrom(qcProteinFeature);
        if ("Gene".equals(bagType)) {
            subQ.addFrom(qcGene);
        }
        subQ.setConstraint(cs);

        if ("Protein".equals(bagType)) {
            subQ.addToSelect(qfProteinId);
        } else {
            subQ.addToSelect(qfGeneId);
        }

        if ("analysed".equals(action)) {
            return subQ;
        } else  if ("export".equals(action)) {
            subQ.clearSelect();
            subQ.addToSelect(qfId);
            subQ.addToSelect(qfPrimaryIdentifier);
            subQ.addToOrderBy(qfId);
            return subQ;
        } else if (action.endsWith("Total")) {  // n and N
            q.addFrom(subQ);
            q.addToSelect(objectCount);
        } else  {   // k and M
            subQ.addToSelect(qfId);
            QueryField qfName = new QueryField(qcProteinFeature, "name");
            subQ.addToSelect(qfName);

            QueryField qfInterProId = new QueryField(subQ, qfId);
            QueryField qfInterProName = new QueryField(subQ, qfName);
            q.addFrom(subQ);
            q.addToSelect(qfInterProId);
            q.addToGroupBy(qfInterProId);
            q.addToSelect(new QueryFunction());
            if ("sample".equals(action)) {
                q.addToSelect(qfInterProName);
                q.addToGroupBy(qfInterProName);
            }
        }
        return q;
    }
}




