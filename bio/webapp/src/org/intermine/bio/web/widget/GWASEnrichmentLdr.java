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
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
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
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

/**
 *
 * @author Julie Sullivan
 */
public class GWASEnrichmentLdr extends EnrichmentWidgetLdr
{
    private static final Logger LOG = Logger.getLogger(GWASEnrichmentLdr.class);
    private Collection<String> taxonIds;
    private InterMineBag bag;
    private String namespace;
    private Model model;

    /**
     * @param extraAttribute the main ontology to filter by (biological_process, molecular_function,
     * or cellular_component)
     * @param bag list of objects for this widget
     * @param os object store
     */
    public GWASEnrichmentLdr (InterMineBag bag, ObjectStore os, String extraAttribute) {
        this.bag = bag;
        namespace = extraAttribute;
        taxonIds = BioUtil.getOrganisms(os, bag, false, "taxonId");
        model = os.getModel();
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(String action, List<String> keys) {

        QueryClass qcSNP = null;
        QueryClass qcGWAS = null;

        try {
            qcGWAS = new QueryClass(Class.forName(model.getPackageName() + ".GWASResult"));
            qcSNP = new QueryClass(Class.forName(model.getPackageName() + ".SNP"));
        } catch (ClassNotFoundException e) {
            LOG.error("Error rendering GWAS enrichment widget", e);
            // don't throw an exception, return NULL instead.  The widget will display 'no results'.
            // the javascript that renders widgets assumes a valid widget and thus can't handle
            // an exception thrown here.
            return null;
        }
        QueryClass qcOrganism = new QueryClass(Organism.class);

        QueryField qfId = new QueryField(qcSNP, "id");
        QueryField qfTaxonId = new QueryField(qcOrganism, "taxonId");
        QueryField qfPrimaryIdentifier = new QueryField(qcSNP, "primaryIdentifier");
        QueryField qfPhenotype = new QueryField(qcGWAS, "phenotype");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // SNP.GWASResults = GWASResult
        QueryCollectionReference c1 = new QueryCollectionReference(qcSNP, "GWASResults");
        cs.addConstraint(new ContainsConstraint(c1, ConstraintOp.CONTAINS, qcGWAS));

        // GWASResult selected by user = SNP.GWASResults.phenotype
        if (keys != null) {
            cs.addConstraint(new BagConstraint(qfPhenotype, ConstraintOp.IN, keys));
        }

        Collection<Integer> taxonIdInts = new ArrayList<Integer>();
        // constrained only for memory reasons
        for (String taxonId : taxonIds) {
            try {
                taxonIdInts.add(new Integer(taxonId));
            } catch (NumberFormatException e) {
                LOG.error("Error rendering gwas stat widget, invalid taxonIds: " + taxonIds);
                // don't throw an exception, return NULL instead.  The widget will display 'no
                // results'. the javascript that renders widgets assumes a valid widget and thus
                // can't handle an exception thrown here.
                return null;
            }
        }
        cs.addConstraint(new BagConstraint(qfTaxonId, ConstraintOp.IN, taxonIdInts));

        // SNP is from organism
        QueryObjectReference c9 = new QueryObjectReference(qcSNP, "organism");
        cs.addConstraint(new ContainsConstraint(c9, ConstraintOp.CONTAINS, qcOrganism));

        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfId, ConstraintOp.IN, bag.getOsb()));
        }

        Query q = new Query();
        q.setDistinct(true);
        q.addFrom(qcSNP);
        q.addFrom(qcGWAS);
        q.addFrom(qcOrganism);

        q.setConstraint(cs);

        if ("analysed".equals(action)) {
            q.addToSelect(qfId);
        } else if ("export".equals(action)) {
            q.addToSelect(qfPhenotype);
            q.addToSelect(qfPrimaryIdentifier);
            q.addToOrderBy(qfPhenotype);
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfId);
            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction());
        } else {    // calculating enrichment

            /*
            these need to be uniquified because there are duplicates.
            2 go terms can have multiple entries which just the relationship type being
            different.

            the first query gets all of the gene --> go term relationships unique
            the second query then counts the genes per each go term
             */

            // subquery
            Query subq = q;
            subq.addToSelect(qfId);
            subq.addToSelect(qfPhenotype);

            // needed so we can select this field in the parent query
            QueryField qfIdentifier = new QueryField(subq, qfPhenotype);

            // main query
            q = new Query();
            q.setDistinct(false);
            q.addFrom(subq);
            q.addToSelect(qfIdentifier);
            q.addToSelect(new QueryFunction());
            q.addToGroupBy(qfIdentifier);
            if ("sample".equals(action)) {
                q.addToSelect(qfIdentifier);
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
