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

    // these will be the returned attributes
    private String fpkmCufflinksJSON = null;
    private Float fpkmCufflinksScoreMax = null;
    private Float fpkmCufflinksScoreMin = null;
    private static HashSet<Integer> organisms = null;
    private static HashSet<String> experiments = null;
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
            LOG.warn("ObjectStoreException in HeatMapController.java");
            return null;
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
        experiments = new HashSet<String>();

        String expressionType = bag.getType().toLowerCase();

        // get the JSON strings
        fpkmCufflinksJSON = getJSONString(model, bag, executor);
        
        // set the attributes
        if (fpkmCufflinksScoreMin != null && fpkmCufflinksScoreMax != null ) {
          request.setAttribute("cufflinksScoreJSON",
              fpkmCufflinksJSON);
          request.setAttribute("minFpkmCufflinksScore",
              df.format(fpkmCufflinksScoreMin));
          request.setAttribute("maxFpkmCufflinksScore",
              df.format(fpkmCufflinksScoreMax));
          request.setAttribute("maxFpkmCufflinksScoreCeiling",
              df.format(Math.ceil(fpkmCufflinksScoreMax)));
          // Gene or MRNA
          request.setAttribute("ExpressionType", expressionType);
          // number of genes or mrnas
          request.setAttribute("FeatureCount", bag.getSize());
          // organism count. We'll only display if == 1
          request.setAttribute("OrganismCount", organisms.size());
          // we need this when setting controllers.
          request.setAttribute("ExperimentCount",experiments.size());
        }
    }


    private String getJSONString (Model model,
        InterMineBag bag, PathQueryExecutor executor) throws ObjectStoreException {


      // Key: gene symbol or PID - Value: list of CufflinksScore objs
      Map<String,HashMap<String,Float>> CufflinksScoreMap =
          new LinkedHashMap<String, HashMap<String,Float>>();

      PathQuery query = new PathQuery(model);
      query = queryCufflinksScore(bag, query);
       
      ExportResultsIterator result = executor.execute(query);

      while (result.hasNext()) {
        List<ResultElement> row = result.next();

        String id = (String) row.get(0).getField();
        Float score = (Float) row.get(1).getField();
        String experimentName = (String) row.get(2).getField();
        Integer thisTaxon = (Integer) row.get(3).getField();
        organisms.add(thisTaxon);
        experiments.add(experimentName);

        if (!CufflinksScoreMap.containsKey(id)) {
          CufflinksScoreMap.put(id,new HashMap<String,Float>());
        }
        CufflinksScoreMap.get(id).put(experimentName,score);
      }
      
      return parseToJSON(CufflinksScoreMap);
    }

    /**
     * Parse CufflinksScoreMap to JSON string
     *
     * @param conditionType CellLine or DevelopmentalStage
     * @param geneCufflinksScoreMap
     * @return json string
     */
    private String parseToJSON(Map<String,HashMap<String,Float>> cufflinksScoreMap) {

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
        desc.add("Cufflinks FPKM");

        double[][] data = new double[smps.size()][vars.size()];

        int j=0;
        for (String primaryId : vars) {
          int i=0;
          for (String experiment: smps) {
            if (cufflinksScoreMap.containsKey(primaryId) &&
                cufflinksScoreMap.get(primaryId).containsKey(experiment)) {
              try {
                data[i][j] = cufflinksScoreMap.get(primaryId).get(experiment);
                if (data[i][j] > 0) {
                  data[i][j] = Math.log(data[i][j])/Math.log(2.);
                } else {
                  data[i][j] = -10.;
                }

                Float trial = new Float(data[i][j]);
                fpkmCufflinksScoreMax =
                    (fpkmCufflinksScoreMax == null || fpkmCufflinksScoreMax < trial)?
                        trial:fpkmCufflinksScoreMax;
                fpkmCufflinksScoreMin =
                    (fpkmCufflinksScoreMin == null || fpkmCufflinksScoreMin > trial)?
                        trial:fpkmCufflinksScoreMin;
              } catch (Exception e) {
                return "{}";
              }
            } else {
              data[i][j] = 0;
            }
            i++;
          }
          j++;
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

    private PathQuery queryCufflinksScore(InterMineBag bag, PathQuery query) {

        String bagType = bag.getType();
        String type = bagType.toLowerCase();

        // Add views
        query.addViews(
                bagType + ".cufflinksscores.bioentity.primaryIdentifier",
                bagType + ".cufflinksscores.fpkm",
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
}
