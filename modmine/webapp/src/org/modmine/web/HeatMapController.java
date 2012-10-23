package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONObject;
import org.modmine.web.logic.ModMineUtil;

/**
 * Class that generates heatMap data for a list of genes or exons.
 *
 * @author Sergio
 * @author Fengyuan Hu
 *
 */
public class HeatMapController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(HeatMapController.class);

    // TO be visualised in the CellLine heatmap
    private static final String[] EXPRESSION_ORDERED_CONDITION_CELLLINE = {"CME L1", "Sg4",
        "ML-DmD11", "ML-DmD20-c2", "ML-DmD20-c5", "Kc167", "GM2", "S2-DRSC", "S2R+", "S1",
        "1182-4H", "ML-DmD16-c3", "ML-DmD32", "ML-DmD17-c3", "ML-DmD8", "CME W1 Cl.8+", "ML-DmD9",
        "ML-DmBG1-c1", "ML-DmD21", "ML-DmD4-c1", "ML-DmBG3-c2", "S3", "CME W2", "mbn2",
        "ML-DmBG2-c2"};

    // TO be visualised in the DevelopmentalStage heatmap
    private static final String[] EXPRESSION_ORDERED_CONDITION_DEVELOPMENTALSTAGE = {"Embryo 0-2 h",
        "Embryo 2-4 h", "Embryo 4-6 h", "Embryo 6-8 h", "emb 8-10h",
        "emb 10-12h", "emb 12-14h", "emb 14-16h", "emb 16-18h", "emb 18-20h", "emb 20-22h",
        "emb 22-24h", "L1 stage larvae", "L2 stage larvae", "L3 stage larvae 12hr",
        "L3 stage larvae dark blue", "L3 stage larvae light blue", "L3 stage larvae clear",
        "White prepupae (WPP)", "White prepupae (WPP) 12 h", "White prepupae (WPP) 24 h",
        "White prepupae (WPP) 2days", "White prepupae (WPP) 3days", "White prepupae (WPP) 4days",
        "Adult F Ecl 1day", "Adult M Ecl 1day", "Adult F Ecl 5day", "Adult M Ecl 5day",
        "Adult F Ecl 30day", "Adult M Ecl 30day"};


    // Separate two sets of conditions
    private static final List<String> EXPRESSION_CONDITION_LIST_CELLLINE = Arrays
    .asList(EXPRESSION_ORDERED_CONDITION_CELLLINE);
    private static final List<String> EXPRESSION_CONDITION_LIST_DEVELOPMENTALSTAGE = Arrays
    .asList(EXPRESSION_ORDERED_CONDITION_DEVELOPMENTALSTAGE);

    private static final String CELLLINE = "cellLine";
    private static final String DEVSTAGE = "developmentalStage";

    private static final String GENE = "gene";
    private static final String EXON = "exon";

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStore os = im.getObjectStore();
        InterMineBag bag = (InterMineBag) request.getAttribute("bag");

        Model model = im.getModel();

        Profile profile = SessionMethods.getProfile(session);
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);

        try {
            findExpression(request, model, bag, executor, os);
        } catch (ObjectStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }


    private void findExpression(HttpServletRequest request, Model model,
            InterMineBag bag, PathQueryExecutor executor,
            ObjectStore os) throws ObjectStoreException {

        DecimalFormat df = new DecimalFormat("#.##");

        String expressionType = bag.getType().toLowerCase();

        // get the 2 JSON strings
        String expressionScoreJSONCellLine =
            getJSONString(model, bag, executor, expressionType, CELLLINE);

        String expressionScoreJSONDevelopmentalStage =
            getJSONString(model, bag, executor, expressionType, DEVSTAGE);

        // set the attributes
        request.setAttribute("expressionScoreJSONCellLine",
                expressionScoreJSONCellLine);
        request.setAttribute("expressionScoreJSONDevelopmentalStage",
                expressionScoreJSONDevelopmentalStage);

        // To make a legend for the heat map
        Double logExpressionScoreMin = 0.0;
        Double logExpressionScoreMax = 0.0;

        if (expressionType.equals(GENE)) {
            logExpressionScoreMin =
                Math.log(ModMineUtil.getMinGeneExpressionScore(os) + 1) / Math.log(2);
            logExpressionScoreMax =
                Math.log(ModMineUtil.getMaxGeneExpressionScore(os) + 1) / Math.log(2);
        }
        if (expressionType.equals(EXON)) {
            logExpressionScoreMin =
                Math.log(ModMineUtil.getMinExonExpressionScore(os) + 1) / Math.log(2);
            logExpressionScoreMax =
                Math.log(ModMineUtil.getMaxExonExpressionScore(os) + 1) / Math.log(2);
        }

        request.setAttribute("minExpressionScore", df.format(logExpressionScoreMin));
        request.setAttribute("maxExpressionScore", df.format(logExpressionScoreMax));
        request.setAttribute("maxExpressionScoreCeiling", Math.ceil(logExpressionScoreMax));
        request.setAttribute("ExpressionType", expressionType);
        request.setAttribute("FeatureCount", bag.getSize());
        // request.setAttribute("FeatureCount", geneExpressionScoreMapDevelopmentalStage.size());
    }


    private List<String> getConditionsList(String conditionType) {
        if (conditionType.equalsIgnoreCase(CELLLINE)) {
            return EXPRESSION_CONDITION_LIST_CELLLINE;
        }
        if (conditionType.equalsIgnoreCase(DEVSTAGE)) {
            return EXPRESSION_CONDITION_LIST_DEVELOPMENTALSTAGE;
        }
        return null;
    }


    private String getJSONString (Model model,
            InterMineBag bag, PathQueryExecutor executor,
            String expressionType, String conditionType) {

        String expressionScoreJSON = null;

        // Key: gene symbol or PID - Value: list of ExpressionScore objs
        Map<String, List<ExpressionScore>> expressionScoreMap =
            new LinkedHashMap<String, List<ExpressionScore>>();

        PathQuery query = new PathQuery(model);
        query = queryExpressionScore(bag, conditionType, query);


        ExportResultsIterator result = executor.execute(query);
        LOG.debug("GGS QUERY: -->" + query + "<--");

        List<String> conditions = getConditionsList(conditionType);

        while (result.hasNext()) {
            List<ResultElement> row = result.next();

            String id = (String) row.get(0).getField();
            String symbol = (String) row.get(1).getField();
            Double score = (Double) row.get(2).getField();
            String condition = (String) row.get(3).getField();
//            String dCCid = (String) row.get(4).getField();

            if (symbol == null) {
                symbol = id;
            }
// should be fine with release 4.2 of canvasxpress
//            symbol = fixSymbol(symbol);

            if (!expressionScoreMap.containsKey(symbol)) {
                // Create a list with space for n (size of conditions) ExpressionScore
                List<ExpressionScore> expressionScoreList = new ArrayList<ExpressionScore>(
                        Collections.nCopies(conditions.size(),
                                new ExpressionScore()));
                ExpressionScore aScore = new ExpressionScore(condition,
                        score, id, symbol);

                expressionScoreList.set(conditions.indexOf(condition), aScore);
                expressionScoreMap.put(symbol, expressionScoreList);

            } else {
                ExpressionScore aScore = new ExpressionScore(
                        condition, score, id, symbol);
                expressionScoreMap
                .get(symbol).set(conditions.indexOf(condition), aScore);
            }
        }

        expressionScoreJSON = parseToJSON(StringUtils.capitalize(conditionType),
                expressionScoreMap);

        return expressionScoreJSON;

    }


    /**
     * To encode '(' and ')', which canvasExpress uses as separator in the cluster tree building
     * also ':' that gives problem in the clustering
     * @param symbol
     * @return a fixed symbol
     */
    private String fixSymbol(String symbol) {
        symbol = symbol.replace("(", "%28");
        symbol = symbol.replace(")", "%29");
        symbol = symbol.replace(":", "%3A");
        return symbol;
    }


    private PathQuery queryExpressionScore(InterMineBag bag, String conditionType,
            PathQuery query) {

        String bagType = bag.getType();
        String type = bagType.toLowerCase();

        // Add views
        query.addViews(
                bagType + "ExpressionScore." + type + ".primaryIdentifier",
                bagType + "ExpressionScore." + type + ".symbol",
                bagType + "ExpressionScore.score",
                bagType + "ExpressionScore." + conditionType + ".name"
//                ,bagType + "ExpressionScore.submission.DCCid"
        );

        // Add orderby
        query.addOrderBy(bagType + "ExpressionScore." + type
                + ".primaryIdentifier", OrderDirection.ASC);

        // Add constraints and you can edit the constraint values below
        query.addConstraint(Constraints.in(bagType + "ExpressionScore." + type,
                bag.getName()));

        return query;
    }


//    /**
//     * Create a path query to retrieve gene expression score.
//     *
//     * @param bagName the bag includes the query genes
//     * @param query a pathquery
//     * @return the pathquery
//     */
//    private PathQuery queryGeneExpressionScore(String bagName, PathQuery query) {
//
//        // Add views
//        query.addViews(
//                "GeneExpressionScore.gene.primaryIdentifier",
//                "GeneExpressionScore.gene.symbol",
//                "GeneExpressionScore.score",
//                "GeneExpressionScore.cellLine.name",
//                "GeneExpressionScore.developmentalStage.name",
//                "GeneExpressionScore.submission.DCCid"
//        );
//
//        // Add orderby
//        query.addOrderBy("GeneExpressionScore.gene.primaryIdentifier", OrderDirection.ASC);
//
//        // Add constraints and you can edit the constraint values below
//        query.addConstraint(Constraints.in("GeneExpressionScore.gene", bagName));
//
//        // Add join status
//        query.setOuterJoinStatus("GeneExpressionScore.cellLine", OuterJoinStatus.OUTER);
//        query.setOuterJoinStatus("GeneExpressionScore.developmentalStage", OuterJoinStatus.OUTER);
//
//        return query;
//    }
//
//
//

    /**
     * Parse expressionScoreMap to JSON string
     *
     * @param conditionType CellLine or DevelopmentalStage
     * @param geneExpressionScoreMap
     * @return json string
     */
    private String parseToJSON(String conditionType,
            Map<String, List<ExpressionScore>> expressionScoreMap) {

        // if no scores returns an empty JSON string
        if (expressionScoreMap.size() == 0) {
            return "{}";
        }

        // vars - conditions
        // smps - genes/exons
        List<String> vars =  new ArrayList<String>();
        if ("CellLine".equals(conditionType)) {
            vars = EXPRESSION_CONDITION_LIST_CELLLINE;
        } else if ("DevelopmentalStage".equals(conditionType)) {
            vars = EXPRESSION_CONDITION_LIST_DEVELOPMENTALSTAGE;
        } else {
            String msg = "Wrong argument: " + conditionType
                    + ". Should be 'CellLine' or 'DevelopmentalStage'";
            throw new RuntimeException(msg);
        }

        Map<String, Object> heatmapData = new LinkedHashMap<String, Object>();
        Map<String, Object> yInHeatmapData =  new LinkedHashMap<String, Object>();

        List<String> smps =  new ArrayList<String>(expressionScoreMap.keySet());

        List<String> desc =  new ArrayList<String>();
        desc.add("Intensity");

        //        List<ArrayList<Double>> data =  new ArrayList<ArrayList<Double>>();

        //      for (String seqenceFeature : expressionScoreMap.keySet()) {
        //      ArrayList<Double> dataLine = new ArrayList<Double>();
        //      for (ExpressionScore es : expressionScoreMap.get(seqenceFeature)) {
        //          dataLine.add(es.getLogScore());
        //      }
        //      data.add(dataLine);
        //  }
        double[][] data = new double[smps.size()][vars.size()];

        for (int i = 0; i < smps.size(); i++) {
            String seqenceFeature = smps.get(i);
            for (int j = 0; j < vars.size(); j++) {
                data[i][j] = expressionScoreMap.get(seqenceFeature).get(j).getLogScore();
            }
        }

        // Rotate data
        double[][] rotatedData = new double[vars.size()][smps.size()];

        int ii = 0;
        for (int i = 0; i < vars.size(); i++) {
            int jj = 0;
            for (int j = 0; j < smps.size(); j++) {
                rotatedData[ii][jj] = data[j][i];
                jj++;
            }
            ii++;
        }

        yInHeatmapData.put("vars", vars);
        yInHeatmapData.put("smps", smps);
        yInHeatmapData.put("desc", desc);
        yInHeatmapData.put("data", rotatedData);
        heatmapData.put("y", yInHeatmapData);
        JSONObject jo = new JSONObject(heatmapData);

        return jo.toString();
    }
}
