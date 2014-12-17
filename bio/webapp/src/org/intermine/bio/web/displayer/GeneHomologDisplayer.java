package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.displayer.GeneSNPDisplayer.GenoSample;
import org.intermine.bio.web.displayer.GeneSNPDisplayer.SNPList;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;


public class GeneHomologDisplayer extends ReportDisplayer {

  protected static final Logger LOG = Logger.getLogger(GeneSNPDisplayer.class);

  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public GeneHomologDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
      super(config, im);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
      HttpSession session = request.getSession();
      final InterMineAPI im = SessionMethods.getInterMineAPI(session);

      Gene geneObj = (Gene)reportObject.getObject();
      
      LOG.info("Entering GeneHomologDisplayer.display for "+geneObj.getPrimaryIdentifier());
      LOG.info("Id is "+geneObj.getId());

      // query the consequences, snps and location
      PathQuery query = getHomologTable(geneObj.getId());
      Profile profile = SessionMethods.getProfile(session);
      PathQueryExecutor exec = im.getPathQueryExecutor(profile);
      ExportResultsIterator result;
      try {
        result = exec.execute(query);
      } catch (ObjectStoreException e) {
        // silently return
        LOG.warn("Had an ObjectStoreException in GeneHomologDisplayer.java: "+e.getMessage());
        return;
      }

      ArrayList<ArrayList<String>> homologList = new ArrayList<ArrayList<String>>();
      
      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();

        ArrayList<String> thisRow = new ArrayList<String>();

        // copy columns 1-9:
        // replace NULL with &nbsp;
        for(int i=0;i<9;i++) {
          if ( (resElement.get(i) != null) && (resElement.get(i).getField() != null)) {
            thisRow.add(resElement.get(i).getField().toString());
          } else {
            thisRow.add("&nbsp;");
          }
        }
        homologList.add(thisRow);
      }
        
      request.setAttribute("list",homologList);
      request.setAttribute("id",geneObj.getId());
      
  }

  private PathQuery getHomologTable(Integer id) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews( "Homolog.gene1.id",
        "Homolog.gene1.primaryIdentifier",
        "Homolog.gene1.organism.shortName",
        "Homolog.gene2.id",
        "Homolog.gene2.primaryIdentifier",
        "Homolog.gene2.organism.shortName",
        "Homolog.relationship",
        "Homolog.proteinfamily.id",
        "Homolog.proteinfamily.clusterId"
        );
query.addOrderBy("Homolog.gene1.primaryIdentifier", OrderDirection.ASC);
query.addOrderBy("Homolog.gene2.primaryIdentifier", OrderDirection.ASC);
query.addConstraint(Constraints.eq("Homolog.gene1.id",id.toString()));
    return query;
  }
  
  
}
