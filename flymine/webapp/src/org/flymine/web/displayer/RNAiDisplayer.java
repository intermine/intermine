package org.flymine.web.displayer;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.bio.Gene;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Displayer for fly rnai data
 *
 * @author Julie Sullivan
 */
public class RNAiDisplayer extends ReportDisplayer
{
    private static final Set<String> RESULT_SCORES = new LinkedHashSet<String>();


    /**
     * @param config report object config
     * @param im intermine API
     */
    public RNAiDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    static {
        RESULT_SCORES.add("Strong Hit");
        RESULT_SCORES.add("Medium Hit");
        RESULT_SCORES.add("Weak Hit");
        RESULT_SCORES.add("Not a Hit");
        RESULT_SCORES.add("Not Screened");
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        Map<String, Map<ResultElement, ResultElement>> rnaiResults = initMap();
        Gene gene = (Gene) reportObject.getObject();
        PathQuery q = getQuery(im, gene.getId());
        boolean noResults = true;
        if (q.isValid()) {
            Profile profile = SessionMethods.getProfile(request.getSession());
            PathQueryExecutor executor = im.getPathQueryExecutor(profile);
            ExportResultsIterator it = executor.execute(q);
            while (it.hasNext()) {
                List<ResultElement> row = it.next();
                String score =  (String) row.get(0).getField();
                ResultElement screen =  (ResultElement) row.get(1);
                ResultElement pub =  (ResultElement) row.get(2);
                Map<ResultElement, ResultElement> screens = rnaiResults.get(score);
                screens.put(screen, pub);
                noResults = false;
            }
        }
        if (noResults) {
            request.setAttribute("noRNAiMessage", "No RNAi results found");
        } else {
            request.setAttribute("results", rnaiResults);
        }
    }

    /*
<query name="" model="genomic" view="RNAiScreen.rnaiScreenHits.result RNAiScreen.name
RNAiScreen.publication.pubMedId RNAiScreen.rnaiScreenHits.gene.primaryIdentifier"
sortOrder="RNAiScreen.name asc">
</query>
    */
    private static PathQuery getQuery(InterMineAPI im, Integer geneId) {
        PathQuery q = new PathQuery(im.getModel());
        q.addViews("RNAiScreen.rnaiScreenHits.result", "RNAiScreen.name",
                "RNAiScreen.publication.pubMedId");
        q.addConstraint(Constraints.eq("RNAiScreen.rnaiScreenHits.gene.id", "" + geneId));
        q.addOrderBy("RNAiScreen.name", OrderDirection.ASC);
        return q;
    }

    // we want the scores to be in order - strong first, etc.
    private Map<String, Map<ResultElement, ResultElement>> initMap() {
        Map<String, Map<ResultElement, ResultElement>> scores
            = new LinkedHashMap<String, Map<ResultElement, ResultElement>>();
        for (String score : RESULT_SCORES) {
            scores.put(score, new LinkedHashMap<ResultElement, ResultElement>());
        }
        return scores;
    }
}
