package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 *
 * If we are mouse, fetch results straight off of us, otherwise create a PathQuery from other
 *  organisms.
 * @author radek
 *
 */
public class MouseAllelesDisplayer extends ReportDisplayer
{

    protected static final Logger LOG = Logger.getLogger(MouseAllelesDisplayer.class);

/**
 * Construct with config and the InterMineAPI.
 * @param config to describe the report displayer
 * @param im the InterMine API
 */
    public MouseAllelesDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @SuppressWarnings({ "unchecked", "unused" })
    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        PathQueryExecutor executor = im.getPathQueryExecutor(SessionMethods.getProfile(session));

        // Counts of HLPT Names
        PathQuery q = new PathQuery(model);

        Boolean mouser = false;
        if (!this.isThisAMouser(reportObject)) {
            // to give us some homologue identifier
            q.addViews("Gene.homologues.homologue.symbol");
            q.addViews("Gene.homologues.homologue.primaryIdentifier");
            q.addViews("Gene.homologues.homologue.id");
            // the actual names we want to tag-cloudize
            q.addViews("Gene.homologues.homologue.alleles.highLevelPhenotypeTerms.name");
            // mouse homologues only
            q.addConstraint(Constraints.eq("Gene.homologues.homologue.organism.shortName",
                    "M. musculus"), "A");
            // for our gene object
            q.addConstraint(Constraints.eq("Gene.id", reportObject.getObject().getId().toString()),
                    "B");
            // we want only those homologues that have a non-empty alleles collection
            q.addConstraint(Constraints.isNotNull("Gene.homologues.homologue.alleles.id"));
            q.setConstraintLogic("A and B");
            // order by the homologue db id, just to keep the alleles in a reasonable order
            q.addOrderBy("Gene.homologues.homologue.id", OrderDirection.ASC);
        } else {
            mouser = true;
            // to give us our homologue identifier
            q.addViews("Gene.symbol");
            q.addViews("Gene.primaryIdentifier");
            q.addViews("Gene.id");
            // the actual names we want to tag-cloudize
            q.addViews("Gene.alleles.highLevelPhenotypeTerms.name");
            // for our gene object
            q.addConstraint(Constraints.eq("Gene.id", reportObject.getObject().getId().toString()),
                    "A");
            // we want only those homologues that have a non-empty alleles collection
            q.addConstraint(Constraints.isNotNull("Gene.alleles.id"));
        }

        ExportResultsIterator qResults = executor.execute((PathQuery) q);
        // traverse so we get a nice map from homologue symbol to a map of allele term names (and
        //  some extras)
        HashMap<String, HashMap<String, Object>> counts = new HashMap<String, HashMap<String,
                Object>>();
        while (qResults.hasNext()) {
            List<ResultElement> row = qResults.next();
            String sourceGeneSymbol = getIdentifier(row);
            // a per source gene map
            HashMap<String, Integer> terms;
            if (!counts.containsKey(sourceGeneSymbol)) {
                HashMap<String, Object> wrapper = new HashMap<String, Object>();
                wrapper.put("terms", terms = new LinkedHashMap<String, Integer>());
                wrapper.put("homologueId", row.get(2).getField().toString());
                wrapper.put("isMouser", mouser);
                counts.put(sourceGeneSymbol, wrapper);
            } else {
                terms = (HashMap<String, Integer>) counts.get(sourceGeneSymbol).get("terms");
            }
            // populate the allele term with count
            String alleleTerm = row.get(3).getField().toString();
            if (!alleleTerm.isEmpty()) {
                Object k = (!terms.containsKey(alleleTerm)) ? terms.put(alleleTerm, 1)
                        : terms.put(alleleTerm, terms.get(alleleTerm) + 1);
            }
        }

        request.setAttribute("counts", counts);
    }

/**
 * Given columns: [symbol, primaryId, id] in a List<ResultElement> row, give us a nice
 *  identifier back
 * @param row
 * @return
 */
    private String getIdentifier(List<ResultElement> row) {
        String id = null;
        return (!(id = row.get(0).getField().toString()).isEmpty()) ? id : ((id = row.get(1).getField().toString()).isEmpty()) ? id : row.get(2).getField().toString();
    }

/**
 *
 * @return true if we are on a mouseified gene
 */
    private Boolean isThisAMouser(ReportObject reportObject) {
        try {
            return "Mus".equals(((InterMineObject) reportObject.getObject()
                        .getFieldValue("organism"))
                .getFieldValue("genus"));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

}
