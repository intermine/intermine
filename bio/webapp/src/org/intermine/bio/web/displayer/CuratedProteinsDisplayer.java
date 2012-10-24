package org.intermine.bio.web.displayer;

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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.API;
import org.intermine.api.InterMineAPI;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

public class CuratedProteinsDisplayer extends ReportDisplayer
{
    protected static final Logger LOG = Logger.getLogger(CuratedProteinsDisplayer.class);

    /** @var column keys of PathQuery results. */
    private ArrayList<String> columns =  new ArrayList<String>() {{
        add("primaryIdentifier");
        add("id");
        add("primaryAccession");
        add("organismName");
        add("isUniprotCanonical");
        add("dataSetsName");
        add("length");
    }};

    /**
     * Construct with config and the InterMineAPI.
     *
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public CuratedProteinsDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        // Get the gene/protein in question from the request.
        InterMineObject object = reportObject.getObject();

        // API connection.
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        PathQuery query = new PathQuery(model);

        // Cast me Gene.
        Gene gene = (Gene) object;
        Object genePrimaryIDObj = gene.getPrimaryIdentifier();
        if (genePrimaryIDObj != null) {
            // Build query.
            query = buildQuery(String.valueOf(genePrimaryIDObj), query);

            // Execute the query.
            Profile profile = SessionMethods.getProfile(session);
            PathQueryExecutor executor = im.getPathQueryExecutor(profile);
            ExportResultsIterator values = executor.execute(query);

            // Listize.
            Map<String, Map<String, Object>> results = new LinkedHashMap<String, Map<String, Object>>();
            while (values.hasNext()) {
            	List<ResultElement> row = values.next();
            	// Build the internal map.
            	Map<String, Object> map = new HashMap<String, Object>();
            	for (String column : columns) {
            		map.put(column, row.get(columns.indexOf(column)).getField());
            	}
            	
            	// Is this SwissProt curate?
            	if (map.get("dataSetsName").equals("Swiss-Prot data set")) {
            		map.put("isSwissProtCurate", true);
            	} else {
            		map.put("isSwissProtCurate", false);
            	}            	
            	
            	// Find in map.
            	String key = (String) map.get("primaryIdentifier");
            	Map<String, Object> mapObj = results.get(key);
            	
            	if (mapObj != null) {
            		if (!(Boolean) mapObj.get("isSwissProtCurate") && (Boolean) map.get("isSwissProtCurate")) {
            			results.put(key, map);
            		}
            	} else {
            		results.put(key, map);
            	}
            }

            // Set.
            request.setAttribute("results", results);
        }
    }
    
	/**
	 * Build PathQuery.    
	 * @param genePrimaryID
	 * @param query
	 * @return
	 */
    private PathQuery buildQuery(String genePrimaryID, PathQuery query) {
        // Select the output columns:
        query.addViews("Gene.proteins.primaryIdentifier",
        		"Gene.proteins.id",
                "Gene.proteins.primaryAccession",
                "Gene.proteins.organism.name",
                "Gene.proteins.isUniprotCanonical",
                "Gene.proteins.dataSets.name",
                "Gene.proteins.length");

        // Add orderby
        query.addOrderBy("Gene.proteins.primaryIdentifier", OrderDirection.ASC);

        // Filter the results with the following constraints:
        query.addConstraint(Constraints.eq("Gene.primaryIdentifier", genePrimaryID));

        return query;
    }

}
