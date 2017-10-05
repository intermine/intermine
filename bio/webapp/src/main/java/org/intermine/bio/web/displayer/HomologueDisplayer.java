package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.metadata.Util;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Displayer for Homology
 * @author Richard Smith
 */
public class HomologueDisplayer extends ReportDisplayer
{
    protected static final Logger LOG = Logger.getLogger(HomologueDisplayer.class);

    /**
     * Construct with config information read from webconfig-model.xml and the API.
     * @param config config information
     * @param im the InterMine API
     */
    public HomologueDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        Map<String, Set<ResultElement>> homologues =
            new TreeMap<String, Set<ResultElement>>();
        Map<String, String> organismIds = new HashMap<String, String>();

        Path symbolPath = null;
        Path primaryIdentifierPath = null;
        try {
            symbolPath = new Path(im.getModel(), "Gene.symbol");
            primaryIdentifierPath = new Path(im.getModel(), "Gene.primaryIdentifier");
        } catch (PathException e) {
            return;
        }
        Gene gene = (Gene) reportObject.getObject();
        Set<String> dataSets = new HashSet<String>();
        JSONObject params = config.getParameterJson();
        try {
            JSONArray dataSetsArray = params.getJSONArray("dataSets");
            for (int i = 0; i < dataSetsArray.length(); i++) {
                dataSets.add(dataSetsArray.getString(i));
            }
        } catch (JSONException e) {
            throw new RuntimeException("Error parsing configuration value 'dataSets'", e);
        }
        PathQuery q = getQuery(im, gene.getId(), dataSets);
        Profile profile = SessionMethods.getProfile(request.getSession());
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        ExportResultsIterator it;
        try {
            it = executor.execute(q);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            Organism organism = (Organism) row.get(0).getObject();
            InterMineObject homologueObject = (InterMineObject) row.get(1).getObject();

            organismIds.put(organism.getSpecies(), organism.getId().toString());
            try {
                if (homologueObject.getFieldValue("symbol") != null) {
                    ResultElement re = new ResultElement(homologueObject,
                            symbolPath, true);
                    Util.addToSetMap(homologues, organism.getShortName(), re);
                } else {
                    ResultElement re = new ResultElement(homologueObject,
                            primaryIdentifierPath, true);
                    Util.addToSetMap(homologues, organism.getShortName(), re);
                }
            } catch (IllegalAccessException e) {
                LOG.error("Failed to resolve path: " + symbolPath + " for gene: " + gene);
            }
        }

        request.setAttribute("organismIds", organismIds);
        request.setAttribute("homologues", homologues);
    }

    private static PathQuery getQuery(InterMineAPI im, Integer geneId, Set<String> dataSets) {
        PathQuery q = new PathQuery(im.getModel());
        q.addViews("Gene.homologues.homologue.organism.shortName",
                "Gene.homologues.homologue.primaryIdentifier");
        q.addConstraint(Constraints.eq("Gene.id", "" + geneId));
        if (dataSets != null && !dataSets.isEmpty()) {
            q.addConstraint(Constraints.oneOfValues("Gene.homologues.dataSets.name", dataSets));
        }
        q.addConstraint(Constraints.neq("Gene.homologues.type", "paralogue"));
        q.addOrderBy("Gene.homologues.homologue.organism.shortName", OrderDirection.ASC);
        return q;
    }
}

