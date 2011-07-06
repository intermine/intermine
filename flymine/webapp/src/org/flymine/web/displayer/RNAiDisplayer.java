package org.flymine.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.HashSet;
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
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.Util;
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
        Map<String, Set<ResultElement>> rnaiResults = initMap();
        Path rnaiResultPath = null;
        try {
            rnaiResultPath = new Path(im.getModel(), "Gene.rnaiResults.rnaiScreen");
        } catch (PathException e) {
            return;
        }
        Gene gene = (Gene) reportObject.getObject();
        PathQuery q = getQuery(im, gene.getId());
        Profile profile = SessionMethods.getProfile(request.getSession());
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        ExportResultsIterator it = executor.execute(q);
        boolean noResults = (it.hasNext() ? false : true);
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String rnaiResult =  (String) row.get(0).getField();
            InterMineObject rnaiScreen = (InterMineObject) row.get(1).getObject();
            ResultElement re = new ResultElement(rnaiScreen, rnaiResultPath, true);
            Util.addToSetMap(rnaiResults, rnaiResult, re);
        }
        if (noResults) {
            final String noRNAiMessage = "No RNAi results found";
            request.setAttribute("noRNAiMessage", noRNAiMessage);
        } else {
            request.setAttribute("rnaiResults", rnaiResults);
        }
    }

    /*
    <query name="" model="genomic" view="Gene.rnaiResults.result Gene.rnaiResults.rnaiScreen.name
    Gene.rnaiResults.rnaiScreen.publication.pubMedId" sortOrder="Gene.rnaiResults.result asc">
    <constraint path="Gene.rnaiResults" type="RNAiScreenHit"/>
   </query>
    */
    private static PathQuery getQuery(InterMineAPI im, Integer geneId) {
        PathQuery q = new PathQuery(im.getModel());
        q.addViews("Gene.rnaiResults.result", "Gene.rnaiResults.rnaiScreen.name",
                "Gene.rnaiResults.rnaiScreen.publication.pubMedId");
        q.addConstraint(Constraints.eq("Gene.id", "" + geneId));
        q.addConstraint(Constraints.type("Gene.rnaiResults", "RNAiScreenHit"));
        q.addOrderBy("Gene.rnaiResults.result", OrderDirection.ASC);
        return q;
    }

    private Map<String, Set<ResultElement>> initMap() {
        Map<String, Set<ResultElement>> scores = new LinkedHashMap();
        for (String score : RESULT_SCORES) {
            Util.addToSetMap(scores, score, new HashSet<ResultElement>());
        }
        return scores;
    }
}
