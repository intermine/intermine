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
import org.intermine.model.bio.Ontology;
import org.intermine.model.bio.OntologyTerm;
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
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class UniProtKeywordsLdr extends EnrichmentWidgetLdr
{

    private Collection<String> organisms;
    private InterMineBag bag;
    private Collection<String> organismsLower = new ArrayList<String>();

    /**
     * @param bag list of objects for this widget
     * @param os object store
     * @param extraAttribute an extra attribute, probably organism
     */
    public UniProtKeywordsLdr(InterMineBag bag, ObjectStore os, String extraAttribute) {
        this.bag = bag;
        organisms = BioUtil.getOrganisms(os, bag, false);

        for (String s : organisms) {
            organismsLower.add(s.toLowerCase());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(String action, List<String> keys) {

        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcOntology = new QueryClass(Ontology.class);
        QueryClass qcOntoTerm = new QueryClass(OntologyTerm.class);

        QueryField qfProtId = new QueryField(qcProtein, "id");
        QueryField qfName = new QueryField(qcOntoTerm, "name");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfOnto = new QueryField(qcOntology, "name");
        QueryField qfPrimaryIdentifier = new QueryField(qcProtein, "primaryIdentifier");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfName, ConstraintOp.IN, keys));
        }

        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfProtId, ConstraintOp.IN, bag.getOsb()));
        }
        QueryExpression qf = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, organismsLower));

        QueryObjectReference qor1 = new QueryObjectReference(qcProtein, "organism");
        cs.addConstraint(new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qcOrganism));

        QueryCollectionReference qcr = new QueryCollectionReference(qcProtein, "keywords");
        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcOntoTerm));

        QueryObjectReference qor2 = new QueryObjectReference(qcOntoTerm, "ontology");
        cs.addConstraint(new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qcOntology));

        cs.addConstraint(new SimpleConstraint(qfOnto, ConstraintOp.EQUALS,
                                              new QueryValue("UniProtKeyword")));

        Query q = new Query();
        q.setDistinct(true);

        q.addFrom(qcProtein);
        q.addFrom(qcOrganism);
        q.addFrom(qcOntology);
        q.addFrom(qcOntoTerm);
        q.setConstraint(cs);

        if ("analysed".equals(action)) {
            q.addToSelect(qfProtId);
        } else if ("export".equals(action)) {
            q.addToSelect(qfName);
            q.addToSelect(qfPrimaryIdentifier);
            q.addToOrderBy(qfName);
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfProtId);
            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction()); // protein count
        } else {
            q.addToSelect(qfName);
            q.addToSelect(new QueryFunction()); // protein count
            if ("sample".equals(action)) {
                q.addToSelect(qfName);
            }
            q.addToGroupBy(qfName);
        }
        return q;
    }
}




