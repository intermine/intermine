package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.WidgetURLQuery;


/**
 * @author Julie Sullivan
 */
public class GWASEnrichmentURLQuery implements WidgetURLQuery
{
    //private static final Logger LOG = Logger.getLogger(GoStatURLQuery.class);
    private ObjectStore os;
    private InterMineBag bag;
    private String key;

    /**
     * @param os object store
     * @param key go terms user selected
     * @param bag bag page they were on
     */
    public GWASEnrichmentURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(boolean showAll) {
        PathQuery q = new PathQuery(os.getModel());
        q.addViews("SNP.primaryIdentifier", "SNP.symbol", "SNP.organism.name",
                  "SNP.GWASResults.study.firstAuthor",
                  "SNP.GWASResults.phenotype",
                  "SNP.GWASResults.pValue",
                  "SNP.GWASResults.associatedVariantRiskAllele",
                  "SNP.GWASResults.riskAlleleFreqInControls");

        q.addConstraint(Constraints.in("SNP", bag.getName()));
        if (!showAll) {
            String[] keys = key.split(",");
            q.addConstraint(Constraints.oneOfValues("SNP.GWASResults.phenotype",
                    Arrays.asList(keys)));
        }
        return q;
    }
}
