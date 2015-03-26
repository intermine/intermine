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
 * Class that generates heatMap data for a correlated expression of genes.
 *
 * @author J Carlson for phytozome
 *
 */
public class CorrelationMapController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(CorrelationMapController.class);

    private Float correlationMax = null;
    private Float correlationMin = null;
    private static HashSet<Integer> organisms = null;
    private static HashSet<String> experimentGroups = null;
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

        LOG.info("In CorrelationMapController for bag "+bag.getName());
        Model model = im.getModel();

        Profile profile = SessionMethods.getProfile(session);
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);

        try {
            findCorrelationData(request, model, bag, executor, os);
        } catch (ObjectStoreException e) {
            LOG.warn("ObjectStoreException in CorrelationMapController.java");
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
    private void findCorrelationData(HttpServletRequest request, Model model,
            InterMineBag bag, PathQueryExecutor executor,
            ObjectStore os) throws ObjectStoreException {

        DecimalFormat df = new DecimalFormat("#.###");
        organisms = new HashSet<Integer>();
        experimentGroups = new HashSet<String>();

        String expressionType = bag.getType().toLowerCase();

        // get the JSON string
        String correlationJSON =
            getJSONString(model, bag, executor, expressionType);

        LOG.info("JSON string is "+correlationJSON);
        LOG.info("Min and max "+correlationMin + " and " +
            correlationMax);
       
        // set the attributes
        request.setAttribute("correlationJSON",
                correlationJSON);
        request.setAttribute("minCorrelation",
                 df.format(correlationMin));
        request.setAttribute("maxCorrelation",
                 df.format(correlationMax));
        // Gene or MRNA
        request.setAttribute("ExpressionType", expressionType);
        // number of genes or mrnas
        request.setAttribute("bioentityCount", bag.getSize());
        // organism count. We'll only display if == 1
        request.setAttribute("organismCount", organisms.size());
        // we need this when setting controllers.
        request.setAttribute("ExperimentCount",experimentGroups.size());
    }


    private String getJSONString (Model model,
        InterMineBag bag, PathQueryExecutor executor,
        String expressionType) throws ObjectStoreException {

      String correlationJSON = null;

      // Key: gene symbol or PID - Value: list of correlated genes with scores.
      Map<String,HashMap<String,Float>> correlationMap =
          new LinkedHashMap<String, HashMap<String,Float>>();

      PathQuery query = new PathQuery(model);
      query = queryCorrelation(bag, query);
       
      ExportResultsIterator result = executor.execute(query);
      LOG.debug("GGS QUERY: -->" + query + "<--");

      while (result.hasNext()) {
        List<ResultElement> row = result.next();

        String gene1 = (String) row.get(0).getField();
        String gene2 = (String) row.get(1).getField();
        Float score = (Float) row.get(2).getField();
        Integer thisProteome = (Integer) row.get(3).getField();
        organisms.add(thisProteome);

        if (!correlationMap.containsKey(gene1)) {
          correlationMap.put(gene1,new HashMap<String,Float>());
        }
        if (!correlationMap.containsKey(gene2)) {
          correlationMap.put(gene2,new HashMap<String,Float>());
        }
        correlationMap.get(gene1).put(gene2,score);
      }
      
      correlationJSON = parseToJSON(correlationMap);

      return correlationJSON;

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


    private PathQuery queryCorrelation(InterMineBag bag, PathQuery query) {

        String bagType = bag.getType();
        String type = bagType.toLowerCase();

        // Add views
        query.addViews(
                "Coexpression.gene.primaryIdentifier",
                "Coexpression.coexpressedGene.primaryIdentifier",
                "Coexpression.correlation",
                "Coexpression.gene.organism.proteomeId"
        );

        // Add orderby
        query.addOrderBy("Coexpression.gene.primaryIdentifier",
            OrderDirection.ASC);
        query.addOrderBy("Coexpression.coexpressedGene.primaryIdentifier",
            OrderDirection.ASC);

        // Add constraints
        query.addConstraint(Constraints.in("Coexpression.gene",
                bag.getName()));
        query.addConstraint(Constraints.in("Coexpression.coexpressedGene",
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
    private String parseToJSON(Map<String,HashMap<String,Float>> correlationMap) {

        // if no scores returns an empty JSON string
        if (correlationMap.size() == 0) {
            return "{}";
        }

        // the gene/mrna ids will be put into an Set
        // X axis genes
        TreeSet<String> bioentityX = new TreeSet<String>();
        // Y axis genes
        TreeSet<String> bioentityY = new TreeSet<String>();
        Iterator<Map.Entry<String,HashMap<String, Float> > > idIterator =
            correlationMap.entrySet().iterator();
        while (idIterator.hasNext() ) {
          Map.Entry<String,HashMap<String,Float> > nextId = idIterator.next();
          bioentityX.add(nextId.getKey());
          Iterator<Map.Entry<String,Float>> correlationIterator =
              nextId.getValue().entrySet().iterator();
          while(correlationIterator.hasNext() ) {
            bioentityY.add(correlationIterator.next().getKey());
          }
        }

        Map<String, Object> heatmapData = new LinkedHashMap<String, Object>();
        Map<String, Object> yInHeatmapData =  new LinkedHashMap<String, Object>();
        List<String> desc =  new ArrayList<String>();
        desc.add("Pearson Correlation");

        double[][] data = new double[bioentityX.size()][bioentityY.size()];
        correlationMax = null;
        correlationMin = null;

        int j=0;
        for (String primaryId : bioentityX) {
          int i=0;
          for (String correlated: bioentityY) {
            if (correlationMap.containsKey(primaryId) &&
                correlationMap.get(primaryId).containsKey(correlated)) {
              data[i][j] = correlationMap.get(primaryId).get(correlated);
              correlationMax = (correlationMax == null || correlationMax < data[i][j])?
                  new Float(data[i][j]):correlationMax;
              correlationMin = (correlationMin == null ||correlationMin > data[i][j])?
                      new Float(data[i][j]):correlationMin;
            } else if (primaryId.equals(correlated)) {
              // in case self-correlations are not stored.
              data[i][j] = 1.;
              correlationMax = (correlationMax == null || correlationMax < 1)?
                  new Float(1.):correlationMax;
              correlationMin = (correlationMin == null || correlationMin > 1)?
                  new Float(1.):correlationMin;
            } else {
              data[i][j] = 1;
            }
            i++;
          }
          j++;
        }
        
        
        yInHeatmapData.put("smps", bioentityX);
        yInHeatmapData.put("vars", bioentityY);
        yInHeatmapData.put("desc", desc);
        yInHeatmapData.put("data", data);
        heatmapData.put("y", yInHeatmapData);
        JSONObject jo = new JSONObject(heatmapData);

        return jo.toString();
    }

}
