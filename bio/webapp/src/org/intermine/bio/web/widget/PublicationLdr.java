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

import org.intermine.api.profile.InterMineBag;
import org.intermine.bio.util.BioUtil;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Publication;
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
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class PublicationLdr extends EnrichmentWidgetLdr
{
    private Collection<String> organisms;
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
        //  having attributes lowercase increases the chances the indexes will be used
        for (String s : organisms) {
            organismsLower.add(s.toLowerCase());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(String action, List<String> keys) {

        // classes for FROM clause
        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcPub = new QueryClass(Publication.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);

        // fields for SELECT clause
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfId = new QueryField(qcPub, "pubMedId");
        QueryField qfPubTitle = new QueryField(qcPub, "title");
        QueryField qfPrimaryIdentifier = new QueryField(qcGene, "primaryIdentifier");

        // constraints
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        cs.addConstraint(new SimpleConstraint(qfId, ConstraintOp.IS_NOT_NULL));

        // constrain genes to be in subset of list the user selected
        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfId, ConstraintOp.IN, keys));
        }

        // constrain genes to be in list
        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb()));
        }

        // organism in our list
        QueryExpression qe = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        cs.addConstraint(new BagConstraint(qe, ConstraintOp.IN, organismsLower));

        // gene.organism = organism
        QueryObjectReference qor = new QueryObjectReference(qcGene, "organism");
        cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS, qcOrganism));

        // gene.publication = publication
        QueryCollectionReference qcr = new QueryCollectionReference(qcGene, "publications");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcPub));



        Query q = new Query();
        q.setDistinct(true);

        // from statement
        q.addFrom(qcGene);
        q.addFrom(qcPub);
        q.addFrom(qcOrganism);

        // add constraints to query
        q.setConstraint(cs);

        // needed for the 'not analysed' number
        if ("analysed".equals(action)) {
            q.addToSelect(qfGeneId);
        // export query
        // needed for export button on widget
        } else if ("export".equals(action)) {
            q.addToSelect(qfId);
            q.addToSelect(qfPrimaryIdentifier);
            q.addToOrderBy(qfId);
        // total queries
        // needed for enrichment calculations
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfGeneId);
            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction()); // gene count
        // needed for enrichment calculations
        } else  {
            q.addToSelect(qfId);
            q.addToGroupBy(qfId);
            q.addToSelect(new QueryFunction()); // gene count
            if ("sample".equals(action)) {
                q.addToSelect(qfPubTitle);
                q.addToGroupBy(qfPubTitle);
            }
        }
        return q;
    }
}



