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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.model.GeneExpressionAtlasExpressions;
import org.intermine.bio.web.model.ProteinAtlasExpressions;
import org.intermine.bio.web.model.ProteinAtlasExpressions.ByLevelComparator;
import org.intermine.bio.web.model.ProteinAtlasExpressions.ExpressionList;
import org.intermine.bio.web.model.ProteinAtlasExpressions.ExpressionType;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

import org.intermine.model.bio.Gene;

public class GeneExpressionAtlasDisplayer extends ReportDisplayer {

    /** @var column keys we have in the results table */
    private ArrayList<String> expressionColumns =  new ArrayList<String>() {{
        add("condition");
        add("expression");
        add("pValue");
        add("tStatistic");
        add("type");
    }};

    public GeneExpressionAtlasDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        // get the gene/protein in question from the request
        InterMineObject object = reportObject.getObject();

        // API connection
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        PathQuery query = new PathQuery(model);

        // cast me Gene
        Gene gene = (Gene) object;
        Object genePrimaryIDObj = gene.getPrimaryIdentifier();
        if (genePrimaryIDObj != null) {
            // fetch the expression
            String genePrimaryID = String.valueOf(genePrimaryIDObj);
            query = geneExpressionAtlasQuery(genePrimaryID, query);

            // execute the query
            Profile profile = SessionMethods.getProfile(session);
            PathQueryExecutor executor = im.getPathQueryExecutor(profile);
            ExportResultsIterator values = executor.execute(query);

            // convert to a map
            GeneExpressionAtlasExpressions geae = new GeneExpressionAtlasExpressions(values);

            // attach to results
            request.setAttribute("expressions", geae);
            request.setAttribute("url", "http://www.ebi.ac.uk/gxa/experiment/E-MTAB-62/"+genePrimaryID);
        }
    }

    /**
     * Return an API query fetching all tissue expressions
     * @author radek
     *
     * @param genePrimaryID
     * @param query
     * @return
     */
    private PathQuery geneExpressionAtlasQuery(String genePrimaryID, PathQuery query) {
        query.addViews(
                "Gene.atlasExpression.condition",
                "Gene.atlasExpression.expression",
                "Gene.atlasExpression.pValue",
                "Gene.atlasExpression.tStatistic",
                "Gene.atlasExpression.type",
                "Gene.primaryIdentifier");
        query.addConstraint(Constraints.eq("Gene.primaryIdentifier", genePrimaryID));
        query.addConstraint(Constraints.eq("Gene.atlasExpression.type", "organism_part"));
        query.addConstraint(Constraints.notLike("Gene.atlasExpression.condition", "(empty)"));
        query.addOrderBy("Gene.atlasExpression.condition", OrderDirection.ASC);

        return query;
    }

}
