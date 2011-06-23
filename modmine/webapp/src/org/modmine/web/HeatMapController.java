package org.modmine.web;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
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

//    private static final String[] EXPRESSION_ORDERED_CONDITION = { "CME L1",
//            "Sg4", "ML-DmD11", "ML-DmD20-c2", "ML-DmD20-c5", "Kc167", "GM2",
//            "S2-DRSC", "S2R+", "S1", "1182-4H", "ML-DmD16-c3", "ML-DmD32",
//            "ML-DmD17-c3", "ML-DmD8", "CME W1 Cl.8+", "ML-DmD9", "ML-DmBG1-c1",
//            "ML-DmD21", "ML-DmD4-c1", "ML-DmBG3-c2", "S3", "CME W2", "mbn2",
//            "ML-DmBG2-c2", "Embryo 0-2 h", "Embryo 2-4 h", "Embryo 4-6 h",
//            "Embryo 6-8 h", "emb 8-10h", "emb 10-12h", "emb 12-14h",
//            "emb 14-16h", "emb 16-18h", "emb 18-20h", "emb 20-22h",
//            "emb 22-24h", "L1 stage larvae", "L2 stage larvae",
//            "L3 stage larvae 12hr", "L3 stage larvae dark blue",
//            "L3 stage larvae light blue", "L3 stage larvae clear",
//            "White prepupae (WPP)", "White prepupae (WPP) 12 h",
//            "White prepupae (WPP) 24 h", "White prepupae (WPP) 2days",
//            "White prepupae (WPP) 3days", "White prepupae (WPP) 4days",
//            "Adult F Ecl 1day", "Adult M Ecl 1day", "Adult F Ecl 5day",
//            "Adult M Ecl 5day", "Adult F Ecl 30day", "Adult M Ecl 30day" };

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
        DecimalFormat df = new DecimalFormat("#.##");

        Model model = im.getModel();
        PathQuery query = new PathQuery(model);

        String dCCid = null;

        Profile profile = SessionMethods.getProfile(session);
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);

        if ("Gene".equals(bag.getType())) {

            findGeneExpression(request, query, bag, executor, dCCid, os, df);

        } else if ("Exon".equals(bag.getType())) {

            findExonExpression(request, query, bag, executor, dCCid, os, df);

        } else {
            return null;
        }

        return null;
    }

    private void findGeneExpression(HttpServletRequest request, PathQuery query,
            InterMineBag bag, PathQueryExecutor executor, String dCCid,
            ObjectStore os, DecimalFormat df) {
        // for Gene expression
        query = queryGeneExpressionScore(bag.getName(), query);
        ExportResultsIterator result = executor.execute(query);

        // Key: gene symbol or PID - Value: list of ExpressionScore objs
        Map<String, List<ExpressionScore>> geneExpressionScoreMapCellLine =
            new LinkedHashMap<String, List<ExpressionScore>>();

        Map<String, List<ExpressionScore>> geneExpressionScoreMapDevelopmentalStage =
            new LinkedHashMap<String, List<ExpressionScore>>();

        while (result.hasNext()) {
            List<ResultElement> row = result.next();

            String geneId = (String) row.get(0).getField();
            String geneSymbol = (String) row.get(1).getField();
            Double score = (Double) row.get(2).getField();
            String cellLine = (String) row.get(3).getField();
            String developmentalStage = (String) row.get(4).getField();
            dCCid = (String) row.get(5).getField();

            if (geneSymbol == null) {
                geneSymbol = geneId;
            }

            if (cellLine == null && developmentalStage != null) {
                if (!geneExpressionScoreMapDevelopmentalStage.containsKey(geneSymbol)) {
                    // Create a list with space for n (size of conditions) ExpressionScore
                    List<ExpressionScore> expressionScoreList = new ArrayList<ExpressionScore>(
                            Collections.nCopies(
                                    EXPRESSION_CONDITION_LIST_DEVELOPMENTALSTAGE
                                            .size(), new ExpressionScore()));

                    ExpressionScore aScore = new ExpressionScore(
                            developmentalStage, score, geneId, geneSymbol);

                    expressionScoreList.set(
                            EXPRESSION_CONDITION_LIST_DEVELOPMENTALSTAGE
                                    .indexOf(developmentalStage), aScore);

                    geneExpressionScoreMapDevelopmentalStage.put(
                            geneSymbol, expressionScoreList);
                } else {
                    ExpressionScore aScore = new ExpressionScore(
                            developmentalStage, score, geneId, geneSymbol);

                    geneExpressionScoreMapDevelopmentalStage
                            .get(geneSymbol).set(EXPRESSION_CONDITION_LIST_DEVELOPMENTALSTAGE
                                    .indexOf(developmentalStage), aScore);
                }
            } else if (developmentalStage == null && cellLine != null) {
                if (!geneExpressionScoreMapCellLine.containsKey(geneSymbol)) {
                    // Create a list with space for n (size of conditions) ExpressionScore
                    List<ExpressionScore> expressionScoreList = new ArrayList<ExpressionScore>(
                            Collections.nCopies(EXPRESSION_CONDITION_LIST_CELLLINE.size(),
                                    new ExpressionScore()));

                    ExpressionScore aScore = new ExpressionScore(cellLine,
                            score, geneId, geneSymbol);

                    expressionScoreList.set(
                            EXPRESSION_CONDITION_LIST_CELLLINE
                                    .indexOf(cellLine), aScore);

                    geneExpressionScoreMapCellLine.put(geneSymbol,
                            expressionScoreList);
                } else {
                    ExpressionScore aScore = new ExpressionScore(
                            developmentalStage, score, geneId, geneSymbol);

                    geneExpressionScoreMapCellLine
                            .get(geneSymbol).set(EXPRESSION_CONDITION_LIST_CELLLINE
                                    .indexOf(cellLine), aScore);
                }
            } else {
                String msg = "CellLine and DevelopmentalStage must be mutually exclusive and "
                    + "can not be NULL at the same time...";
                throw new RuntimeException(msg);
            }
        }

        String geneExpressionScoreJSONCellLine = parseToJSON("CellLine",
                geneExpressionScoreMapCellLine);
        String geneExpressionScoreJSONDevelopmentalStage = parseToJSON("DevelopmentalStage",
                geneExpressionScoreMapDevelopmentalStage);

        request.setAttribute("expressionScoreJSONCellLine",
                geneExpressionScoreJSONCellLine);
        request.setAttribute("expressionScoreJSONDevelopmentalStage",
                geneExpressionScoreJSONDevelopmentalStage);

        // To make a legend for the heat map
        Double logGeneExpressionScoreMin =
            Math.log(ModMineUtil.getMinGeneExpressionScore(os) + 1) / Math.log(2);
        Double logGeneExpressionScoreMax =
            Math.log(ModMineUtil.getMaxGeneExpressionScore(os) + 1) / Math.log(2);

        request.setAttribute("minExpressionScore", df.format(logGeneExpressionScoreMin));
        request.setAttribute("maxExpressionScore", df.format(logGeneExpressionScoreMax));
        request.setAttribute("maxExpressionScoreCeiling", Math.ceil(logGeneExpressionScoreMax));
        request.setAttribute("expressionScoreDCCid", dCCid);
        request.setAttribute("ExpressionType", "gene");
        request.setAttribute("FeatureCount", geneExpressionScoreMapDevelopmentalStage.size());
    }

    private void findExonExpression(HttpServletRequest request, PathQuery query,
            InterMineBag bag, PathQueryExecutor executor, String dCCid,
            ObjectStore os, DecimalFormat df) {
        // for Exon expression
        query = queryExonExpressionScore(bag.getName(), query);
        ExportResultsIterator result = executor.execute(query);

        Map<String, List<ExpressionScore>> exonExpressionScoreMapCellLine =
            new LinkedHashMap<String, List<ExpressionScore>>();

        Map<String, List<ExpressionScore>> exonExpressionScoreMapDevelopmentalStage =
            new LinkedHashMap<String, List<ExpressionScore>>();

        while (result.hasNext()) {
            List<ResultElement> row = result.next();

            String exonId = (String) row.get(0).getField();
            String exonSymbol = (String) row.get(1).getField();
            Double score = (Double) row.get(2).getField();
            String cellLine = (String) row.get(3).getField();
            String developmentalStage = (String) row.get(4).getField();
            dCCid = (String) row.get(5).getField();

            if (exonSymbol == null) {
                exonSymbol = exonId;
            }

            if (cellLine == null && developmentalStage != null) {
                if (!exonExpressionScoreMapDevelopmentalStage.containsKey(exonSymbol)) {
                    List<ExpressionScore> expressionScoreList = new ArrayList<ExpressionScore>(
                            Collections.nCopies(
                                    EXPRESSION_CONDITION_LIST_DEVELOPMENTALSTAGE
                                            .size(), new ExpressionScore()));

                    ExpressionScore aScore = new ExpressionScore(
                            developmentalStage, score, exonId, exonSymbol);

                    expressionScoreList.set(
                            EXPRESSION_CONDITION_LIST_DEVELOPMENTALSTAGE
                                    .indexOf(developmentalStage), aScore);

                    exonExpressionScoreMapDevelopmentalStage.put(
                            exonSymbol, expressionScoreList);
                } else {
                    ExpressionScore aScore = new ExpressionScore(
                            developmentalStage, score, exonId, exonSymbol);

                    exonExpressionScoreMapDevelopmentalStage
                            .get(exonSymbol).set(EXPRESSION_CONDITION_LIST_DEVELOPMENTALSTAGE
                                    .indexOf(developmentalStage), aScore);
                }
            } else if (developmentalStage == null && cellLine != null) {
                if (!exonExpressionScoreMapCellLine.containsKey(exonSymbol)) {
                    // Create a list with space for n (size of conditions) ExpressionScore
                    List<ExpressionScore> expressionScoreList = new ArrayList<ExpressionScore>(
                            Collections.nCopies(EXPRESSION_CONDITION_LIST_CELLLINE.size(),
                                    new ExpressionScore()));

                    ExpressionScore aScore = new ExpressionScore(cellLine,
                            score, exonId, exonSymbol);

                    expressionScoreList.set(
                            EXPRESSION_CONDITION_LIST_CELLLINE
                                    .indexOf(cellLine), aScore);

                    exonExpressionScoreMapCellLine.put(exonSymbol,
                            expressionScoreList);
                } else {
                    ExpressionScore aScore = new ExpressionScore(
                            developmentalStage, score, exonId, exonSymbol);

                    exonExpressionScoreMapCellLine
                            .get(exonSymbol).set(EXPRESSION_CONDITION_LIST_CELLLINE
                                    .indexOf(cellLine), aScore);
                }
            } else {
                String msg = "CellLine and DevelopmentalStage must be mutually exclusive and "
                    + "can not be NULL at the same time...";
                throw new RuntimeException(msg);
            }
        }

        String exonExpressionScoreJSONCellLine = parseToJSON("CellLine",
                exonExpressionScoreMapCellLine);
        String exonExpressionScoreJSONDevelopmentalStage = parseToJSON("DevelopmentalStage",
                exonExpressionScoreMapDevelopmentalStage);

        request.setAttribute("expressionScoreJSONCellLine",
                exonExpressionScoreJSONCellLine);
        request.setAttribute("expressionScoreJSONDevelopmentalStage",
                exonExpressionScoreJSONDevelopmentalStage);

        // To make a legend for the heat map, take log2 of the original scores
        Double logExonExpressionScoreMin =
            Math.log(ModMineUtil.getMinExonExpressionScore(os) + 1) / Math.log(2);
        Double logExonExpressionScoreMax =
            Math.log(ModMineUtil.getMaxExonExpressionScore(os) + 1) / Math.log(2);

        request.setAttribute("minExpressionScore", df.format(logExonExpressionScoreMin));
        request.setAttribute("maxExpressionScore", df.format(logExonExpressionScoreMax));
        request.setAttribute("maxExpressionScoreCeiling", Math.ceil(logExonExpressionScoreMax));
        request.setAttribute("expressionScoreDCCid", dCCid);
        request.setAttribute("ExpressionType", "exon");
        request.setAttribute("FeatureCount", exonExpressionScoreMapDevelopmentalStage.size());
    }

    /**
     * Create a path query to retrieve gene expression score.
     *
     * @param bagName the bag includes the query genes
     * @param query a pathquery
     * @return the pathquery
     */
    private PathQuery queryGeneExpressionScore(String bagName, PathQuery query) {

        // Add views
        query.addViews(
                "GeneExpressionScore.gene.primaryIdentifier",
                "GeneExpressionScore.gene.symbol",
                "GeneExpressionScore.score",
                "GeneExpressionScore.cellLine.name",
                "GeneExpressionScore.developmentalStage.name",
                "GeneExpressionScore.submission.DCCid"
        );

        // Add orderby
        query.addOrderBy("GeneExpressionScore.gene.primaryIdentifier", OrderDirection.ASC);

        // Add constraints and you can edit the constraint values below
        query.addConstraint(Constraints.in("GeneExpressionScore.gene", bagName));

        // Add join status
        query.setOuterJoinStatus("GeneExpressionScore.cellLine", OuterJoinStatus.OUTER);
        query.setOuterJoinStatus("GeneExpressionScore.developmentalStage", OuterJoinStatus.OUTER);

        return query;
    }

    /**
     * Create a path query to retrieve exon expression score.
     *
     * @param bagName the bag includes the query exons
     * @param query a pathquery
     * @return the pathquery
     */
    private PathQuery queryExonExpressionScore(String bagName, PathQuery query) {

        // Add views
        query.addViews(
                "ExonExpressionScore.exon.primaryIdentifier",
                "ExonExpressionScore.exon.symbol",
                "ExonExpressionScore.score",
                "ExonExpressionScore.cellLine.name",
                "ExonExpressionScore.developmentalStage.name",
                "ExonExpressionScore.submission.DCCid"
        );

        // Add orderby
        query.addOrderBy("ExonExpressionScore.exon.primaryIdentifier", OrderDirection.ASC);

        // Add constraints and you can edit the constraint values below
        query.addConstraint(Constraints.in("ExonExpressionScore.exon", bagName));

        // Add join status
        query.setOuterJoinStatus("ExonExpressionScore.cellLine", OuterJoinStatus.OUTER);
        query.setOuterJoinStatus("ExonExpressionScore.developmentalStage", OuterJoinStatus.OUTER);

        return query;
    }

    /**
     * Parse expressionScoreMap to JSON string
     *
     * @param conditionType CellLine or DevelopmentalStage
     * @param geneExpressionScoreMap
     * @return json string
     */
    private String parseToJSON(String conditionType,
            Map<String, List<ExpressionScore>> expressionScoreMap) {

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
