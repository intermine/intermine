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


import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.output.JSONResultsIterator;
import org.intermine.webservice.server.query.result.PathQueryBuilderForJSONObj;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Displayer for flyatlas expression data.
 * @author Alex
 */
public class FlyAtlasDisplayer extends ReportDisplayer
{

    /**
     * @param config configuration object
     * @param im intermine API
     */
    public FlyAtlasDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        PathQuery q = new PathQuery(im.getModel());
        q.addViews("Gene.microArrayResults.material.primaryIdentifier",
                "Gene.microArrayResults.tissue.name",
                "Gene.microArrayResults.affyCall", // up/down/none => colour
                "Gene.microArrayResults.enrichment", // fp
                "Gene.microArrayResults.mRNASignal",
                "Gene.microArrayResults.presentCall" /* Mouseover 1 out of  4 */);
        Integer objectId = reportObject.getId();
        q.addConstraint(Constraints.eq("Gene.id", objectId.toString()));
        q.addConstraint(Constraints.type("Gene.microArrayResults", "FlyAtlasResult"));
        q.addOrderBySpaceSeparated("Gene.microArrayResults.mRNASignal asc");

        q = PathQueryBuilderForJSONObj.processQuery(q);

        Profile profile = SessionMethods.getProfile(request.getSession());
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);

        List<Double> signals = new ArrayList<Double>();
        List<String> names = new ArrayList<String>();
        List<String> affyCalls = new ArrayList<String>();
        List<Double> enrichments = new ArrayList<Double>();
        List<Integer> presentCalls = new ArrayList<Integer>();
        List<String> objectIds = new ArrayList<String>();

        JSONResultsIterator jsonIterator = new JSONResultsIterator(executor.execute(q));
        while (jsonIterator.hasNext()) {
            try {
                JSONObject gene = jsonIterator.next();
                JSONArray microArrayResults = gene.getJSONArray("microArrayResults");
                for (int i = 0; i < microArrayResults.length(); i++) {
                    JSONObject flyAtlasResult = microArrayResults.getJSONObject(i);
                    objectIds.add(flyAtlasResult.getString("objectId"));
                    signals.add(flyAtlasResult.getDouble("mRNASignal"));
                    names.add(flyAtlasResult.getJSONObject("tissue").getString("name"));
                    affyCalls.add(flyAtlasResult.getString("affyCall"));
                    enrichments.add(flyAtlasResult.optDouble("enrichment"));
                    presentCalls.add(flyAtlasResult.getInt("presentCall"));
                }
            } catch (JSONException e) {
                //
            }
        }
        request.setAttribute("signals", signals.toString());
        request.setAttribute("names", new JSONArray(names));
        request.setAttribute("affyCalls", new JSONArray(affyCalls));
        request.setAttribute("enrichments", enrichments.toString());
        request.setAttribute("presentCalls", presentCalls.toString());
        request.setAttribute("objectIds", new JSONArray(objectIds));
    }

}
