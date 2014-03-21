package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2013 FlyMine
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.HashSet;

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
import org.apache.tools.ant.BuildException;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.xml.full.Item;
import org.jfree.util.Log;
import org.json.JSONObject;
import org.intermine.model.bio.CufflinksScore;

/**
 * Class that generates heatMap data for a list of genes or exons.
 *
 * @author Sergio
 * @author Fengyuan Hu
 * @author J Carlson for phytozome
 *
 */
public class HeatMapController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(HeatMapController.class);



    // Separate two sets of conditions
    private static final List<String> EXPRESSION_CONDITION_LIST_GENE = null;
    private static final List<String> EXPRESSION_CONDITION_LIST_MRNA = null;

    private static final String FPKM = "fpkm";
    private static final String COUNT = "count";

    private static Float fpkmCufflinksScoreMax = new Float(100.);
    private static Float fpkmCufflinksScoreMin = new Float(0.);
    private static Float countCufflinksScoreMax = new Float(100.);
    private static Float countCufflinksScoreMin = new Float(0.);
    private static HashSet<Integer> organisms = null;;
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


    /**
     * @param request
     * @param model
     * @param bag
     * @param executor
     * @param os
     * @throws ObjectStoreException
     */
    private void findExpression(HttpServletRequest request, Model model,
            InterMineBag bag, PathQueryExecutor executor,
            ObjectStore os) throws ObjectStoreException {

        DecimalFormat df = new DecimalFormat("#.##");
        organisms = new HashSet<Integer>();

        String expressionType = bag.getType().toLowerCase();

        // get the 2 JSON strings
        String CufflinksScoreJSONFpkm =
            getJSONString(model, bag, executor, expressionType, FPKM);

        //LOG.info("FPKM json string is "+CufflinksScoreJSONFpkm);
        //LOG.info("FPKM min and max are "+fpkmCufflinksScoreMin + " and " +
            //fpkmCufflinksScoreMax);
        
        String CufflinksScoreJSONCount =
            getJSONString(model, bag, executor, expressionType,COUNT);

        
        LOG.info("Count json string is "+CufflinksScoreJSONCount);
        LOG.info("Count min and max are "+countCufflinksScoreMin + " and " +
            countCufflinksScoreMax);
        
        // set the attributes
        request.setAttribute("cufflinksScoreJSONFpkm",
                CufflinksScoreJSONFpkm);
        request.setAttribute("cufflinksScoreJSONCount",
                CufflinksScoreJSONCount);
/*
        request.setAttribute("minFpkmCufflinksScore",
            df.format(Math.log(fpkmCufflinksScoreMin+1)/Math.log(2.)));
        request.setAttribute("maxFpkmCufflinksScore",
            df.format(Math.log(fpkmCufflinksScoreMax+1)/Math.log(2.)));
        request.setAttribute("maxFpkmCufflinksScoreCeiling",
            df.format(Math.ceil(Math.log(fpkmCufflinksScoreMax+1)/Math.log(2.))));
        request.setAttribute("minCountCufflinksScore",
            df.format(Math.log(countCufflinksScoreMin+1)/Math.log(2.)));
        request.setAttribute("maxCountCufflinksScore",
            df.format(Math.log(countCufflinksScoreMax+1)/Math.log(2.)));
        request.setAttribute("maxCountCufflinksScoreCeiling",
            df.format(Math.ceil(Math.log(fpkmCufflinksScoreMax-1)/Math.log(2.))));*/

        request.setAttribute("minFpkmCufflinksScore",
            df.format(fpkmCufflinksScoreMin));
        request.setAttribute("maxFpkmCufflinksScore",
            df.format(fpkmCufflinksScoreMax));
        request.setAttribute("maxFpkmCufflinksScoreCeiling",
            df.format(Math.ceil(fpkmCufflinksScoreMax)));
        request.setAttribute("minCountCufflinksScore",
            df.format(countCufflinksScoreMin+1));
        request.setAttribute("maxCountCufflinksScore",
            df.format(countCufflinksScoreMax+1));
        request.setAttribute("maxCountCufflinksScoreCeiling",
            df.format(Math.ceil(fpkmCufflinksScoreMax)));
        request.setAttribute("ExpressionType", expressionType);
        request.setAttribute("FeatureCount", bag.getSize());
        request.setAttribute("OrganismCount", organisms.size());
        // request.setAttribute("FeatureCount", geneCufflinksScoreMapDevelopmentalStage.size());
    }


    private String getJSONString (Model model,
        InterMineBag bag, PathQueryExecutor executor,
        String expressionType, String scoreType) {

      String CufflinksScoreJSON = null;

      // Key: gene symbol or PID - Value: list of CufflinksScore objs
      Map<String,HashMap<String,Float>> CufflinksScoreMap =
          new LinkedHashMap<String, HashMap<String,Float>>();

      PathQuery query = new PathQuery(model);
      query = queryCufflinksScore(bag, scoreType, query);

      ExportResultsIterator result = executor.execute(query);
      LOG.debug("GGS QUERY: -->" + query + "<--");

      while (result.hasNext()) {
        List<ResultElement> row = result.next();

        String id = (String) row.get(0).getField();
        Float score = (Float) row.get(1).getField();
        String condition = (String) row.get(2).getField();
        Integer thisTaxon = (Integer) row.get(3).getField();
        organisms.add(thisTaxon);

        if (!CufflinksScoreMap.containsKey(id)) {
          CufflinksScoreMap.put(id,new HashMap<String,Float>());
        }
        CufflinksScoreMap.get(id).put(condition,score);
      }

      CufflinksScoreJSON = parseToJSON(scoreType,CufflinksScoreMap);

      return CufflinksScoreJSON;

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


    private PathQuery queryCufflinksScore(InterMineBag bag, String scoreType,
            PathQuery query) {

        String bagType = bag.getType();
        String type = bagType.toLowerCase();

        // Add views
        query.addViews(
                bagType + ".cufflinksscores.bioentity.primaryIdentifier",
                bagType + ".cufflinksscores." + scoreType,
                bagType + ".cufflinksscores.experiment.name",
                bagType + ".organism.taxonId"
        );

        // Add orderby
        query.addOrderBy(bagType + ".cufflinksscores.bioentity.primaryIdentifier",
            OrderDirection.ASC);
        query.addOrderBy(bagType + ".cufflinksscores.experiment.name",
            OrderDirection.ASC);

        // Add constraints
        query.addConstraint(Constraints.in(bagType + ".cufflinksscores.bioentity",
                bag.getName()));

        return query;
    }



    /**
     * Parse CufflinksScoreMap to JSON string
     *
     * @param conditionType CellLine or DevelopmentalStage
     * @param geneCufflinksScoreMap
     * @return json string
     */
    private String parseToJSON(String scoreType,
            Map<String,HashMap<String,Float>> cufflinksScoreMap) {

        // if no scores returns an empty JSON string
        if (cufflinksScoreMap.size() == 0) {
            return "{}";
        }

        // the sample names and gene/mrna ids will be put into an 
        // TreeSet to keep them sorted.
        // vars - gene/mrna ids
        TreeSet<String> vars = new TreeSet<String>();
        // smps - sample/experiment names
        TreeSet<String> smps = new TreeSet<String>();
        Iterator<Map.Entry<String,HashMap<String, Float> > > idIterator =
            cufflinksScoreMap.entrySet().iterator();
        while (idIterator.hasNext() ) {
          Map.Entry<String,HashMap<String,Float> > nextId = idIterator.next();
          vars.add(nextId.getKey());
          Iterator<Map.Entry<String,Float>> experimentIterator =
              nextId.getValue().entrySet().iterator();
          while(experimentIterator.hasNext() ) {
            smps.add(experimentIterator.next().getKey());
          }
        }

        Map<String, Object> heatmapData = new LinkedHashMap<String, Object>();
        Map<String, Object> yInHeatmapData =  new LinkedHashMap<String, Object>();
        List<String> desc =  new ArrayList<String>();
        desc.add("Intensity");

        double[][] data = new double[smps.size()][vars.size()];
        Float dataMax = null;
        Float dataMin = null;

        int j=0;
        for (String primaryId : vars) {
          LOG.info("scanning for id "+primaryId);
          int i=0;
          for (String experiment: smps) {
            LOG.info("scanning for experiment "+experiment);
            if (cufflinksScoreMap.containsKey(primaryId) &&
                cufflinksScoreMap.get(primaryId).containsKey(experiment)) {
              data[i][j] = cufflinksScoreMap.get(primaryId).get(experiment);
              if (data[i][j] > 0) {
                data[i][j] = Math.log(data[i][j])/Math.log(2.);
              } else {
                data[i][j] = 0.;
              }
              dataMax = (dataMax == null || dataMax < data[i][j])?
                      new Float(data[i][j]):dataMax;
              dataMin = (dataMin == null ||dataMin > data[i][j])?
                      new Float(data[i][j]):dataMin;
            } else {
              data[i][j] = 0;
            }
            i++;
          }
          j++;
        }
        if (scoreType.equals(FPKM) ) {
          fpkmCufflinksScoreMax = dataMax;
          fpkmCufflinksScoreMin = dataMin;
          //LOG.info("Setting fpkm min/max.");
        } else {
          countCufflinksScoreMax = dataMax;
          countCufflinksScoreMin = dataMin;
          //LOG.info("Setting count min/max.");
        }
        // Rotate data
        double[][] rotatedData = new double[vars.size()][smps.size()];

        int ii = 0;
        for (int i = 0; i < vars.size(); i++) {
            int jj = 0;
            for (j = 0; j < smps.size(); j++) {
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
