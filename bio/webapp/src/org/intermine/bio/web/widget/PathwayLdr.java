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
import org.intermine.bio.util.BioUtil;
import org.intermine.metadata.Model;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
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
public class PathwayLdr extends EnrichmentWidgetLdr
{
    private static final Logger LOG = Logger.getLogger(PathwayLdr.class);
    private Collection<String> taxonIds;
    private InterMineBag bag;
    private String namespace;
    private Model model;
    private String dataset;
    private static final String KEGG = "KEGG pathways data set";
    private static final String REACTOME = "Reactome data set";

    /**
     * @param extraAttribute the main ontology to filter by (biological_process, molecular_function,
     * or cellular_component)
     * @param bag list of objects for this widget
     * @param os object store
     */
    public PathwayLdr (InterMineBag bag, ObjectStore os, String extraAttribute) {
        this.bag = bag;
        taxonIds = BioUtil.getOrganisms(os, bag, false, "taxonId");
        model = os.getModel();
        dataset = extraAttribute;
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(String action, List<String> keys) {

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcPathway = null;
        QueryClass qcOrganism = new QueryClass(Organism.class);

        try {
            qcPathway = new QueryClass(Class.forName(model.getPackageName() + ".Pathway"));
        } catch (ClassNotFoundException e) {
            LOG.error("Error rendering pathway enrichment widget", e);
            // don't throw an exception, return NULL instead.  The widget will display 'no
            // results'. the javascript that renders widgets assumes a valid widget and thus
            // can't handle an exception thrown here.
            return null;
        }

        QueryField qfPathwayIdentifier = new QueryField(qcPathway, "identifier");
        QueryField qfPathwayName = new QueryField(qcPathway, "name");
        QueryField qfTaxonId = new QueryField(qcOrganism, "taxonId");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfPrimaryIdentifier = new QueryField(qcGene, "primaryIdentifier");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // gene.pathways CONTAINS Pathway
        QueryCollectionReference c1 = new QueryCollectionReference(qcGene, "pathways");
        cs.addConstraint(new ContainsConstraint(c1, ConstraintOp.CONTAINS, qcPathway));

        Collection<Integer> taxonIdInts = new ArrayList<Integer>();
        // constrained only for memory reasons
        for (String taxonId : taxonIds) {
            try {
                taxonIdInts.add(new Integer(taxonId));
            } catch (NumberFormatException e) {
                LOG.error("Error rendering pathway widget, invalid taxonIds: " + taxonIds);
                // don't throw an exception, return NULL instead.  The widget will display 'no
                // results'. the javascript that renders widgets assumes a valid widget and thus
                // can't handle an exception thrown here.
                return null;
            }
        }
        cs.addConstraint(new BagConstraint(qfTaxonId, ConstraintOp.IN, taxonIdInts));

        // gene is from organism
        QueryObjectReference c9 = new QueryObjectReference(qcGene, "organism");
        cs.addConstraint(new ContainsConstraint(c9, ConstraintOp.CONTAINS, qcOrganism));

        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb()));
        }

        Query q = new Query();

        if ("KEGG".equals(dataset) || "Reactome".equals(dataset)) {

            String datasetTitle = ("KEGG".equals(dataset) ? KEGG : REACTOME);

            QueryClass qcDataset = new QueryClass(DataSet.class);
            QueryField qfDataset = new QueryField(qcDataset, "name");

            QueryCollectionReference c2 = new QueryCollectionReference(qcPathway, "dataSets");
            cs.addConstraint(new ContainsConstraint(c2, ConstraintOp.CONTAINS, qcDataset));

            // dataset (if user selects)
            QueryExpression c10 = new QueryExpression(QueryExpression.LOWER, qfDataset);
            cs.addConstraint(new SimpleConstraint(c10, ConstraintOp.EQUALS,
                                                  new QueryValue(datasetTitle.toLowerCase())));

            q.addFrom(qcDataset);
        }


        q.setDistinct(true);
        q.addFrom(qcGene);
        q.addFrom(qcPathway);
        q.addFrom(qcOrganism);

        q.setConstraint(cs);

        if ("analysed".equals(action)) {
            q.addToSelect(qfGeneId);
        } else if ("export".equals(action)) {
            q.addToSelect(qfPathwayIdentifier);
            q.addToSelect(qfPrimaryIdentifier);
            q.addToOrderBy(qfPathwayIdentifier);
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfGeneId);
            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction());
        } else {    // calculating enrichment

            q.addToSelect(qfPathwayIdentifier);
            q.addToGroupBy(qfPathwayIdentifier);
            q.addToSelect(new QueryFunction()); // gene count
            if ("sample".equals(action)) {
                q.addToSelect(qfPathwayName);
                q.addToGroupBy(qfPathwayName);
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
