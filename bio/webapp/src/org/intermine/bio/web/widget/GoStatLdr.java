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
import java.util.List;

import org.flymine.model.genomic.GOAnnotation;
import org.flymine.model.genomic.GOTerm;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;
import org.intermine.bio.web.logic.BioUtil;
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
public class GoStatLdr extends EnrichmentWidgetLdr
{
    private Collection<String> organisms;
    private InterMineBag bag;
    private String namespace;
    private Collection<String> organismsLower = new ArrayList<String>();
    /**
     * @param extraAttribute the main ontology to filter by (biological_process, molecular_function,
     * or cellular_component)
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
    public Query getQuery(String action, List<String> keys) {

        String bagType = bag.getType();

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

        QueryField qfPrimaryIdentifier = null;
        QueryField qfId = null;

        if (bagType.equals("Protein")) {
            qfPrimaryIdentifier = new QueryField(qcProtein, "primaryIdentifier");
            qfId = qfProteinId;
        } else {
            qfPrimaryIdentifier = new QueryField(qcGene, "primaryIdentifier");
            qfId = qfGeneId;
        }
        QueryFunction objectCount = new QueryFunction();

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfGoTermId, ConstraintOp.IN, keys));
        }

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

        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfId, ConstraintOp.IN, bag.getOsb()));
        }

        if (bagType.equals("Protein")) {
            QueryCollectionReference qcr2 = new QueryCollectionReference(qcProtein, "genes");
            cs.addConstraint(new ContainsConstraint(qcr2, ConstraintOp.CONTAINS, qcGene));
        }

        Query q = new Query();
        q.setDistinct(true);
        q.addFrom(qcGene);
        q.addFrom(qcGoAnnotation);
        q.addFrom(qcOrganism);
        q.addFrom(qcGo);
        if (bagType.equals("Protein")) {
            q.addFrom(qcProtein);
        }
        q.setConstraint(cs);

        if (action.equals("analysed")) {
            q.addToSelect(qfId);
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfId);
            Query superQ = new Query();
            superQ.addFrom(q);
            superQ.addToSelect(objectCount);
            return superQ;
        } else if (action.equals("export")) {
            q.addToSelect(qfGoTermId);
            q.addToSelect(qfPrimaryIdentifier);
            q.addToOrderBy(qfGoTermId);
        } else {    // calculating enrichment
            q.setDistinct(false);
            q.addToSelect(qfGoTermId);
            q.addToGroupBy(qfGoTermId);
            q.addToSelect(objectCount);
            if (action.equals("sample")) {
                q.addToSelect(qfGoTerm);
                q.addToGroupBy(qfGoTerm);
            }
        }
        return q;
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



