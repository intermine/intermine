/**
 * 
 */
package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Protein;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;


public class ProteinAnalysisDisplayer extends ReportDisplayer {

  protected static final Logger LOG = Logger.getLogger(ProteinAnalysisDisplayer.class);

  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public ProteinAnalysisDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
      super(config, im);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
      HttpSession session = request.getSession();
      final InterMineAPI im = SessionMethods.getInterMineAPI(session);

      Protein proteinObj = (Protein)reportObject.getObject();
      
      LOG.info("Entering GeneSNPDisplayer.display for "+proteinObj.getPrimaryIdentifier());

      // query the consequences, snps and location
      PathQuery query = getAnalysisTable(proteinObj.getPrimaryIdentifier());
      Profile profile = SessionMethods.getProfile(session);
      PathQueryExecutor exec = im.getPathQueryExecutor(profile);
      ExportResultsIterator result = exec.execute(query);

      ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
      while (result.hasNext()) {
        List<ResultElement> row = result.next();

        ArrayList<String> columns = new ArrayList<String>();

        for(int i=0;i<6;i++) {
          if ( (row.get(i) != null) && (row.get(i).getField() != null)) {
            columns.add(row.get(i).getField().toString());
          } else {
            columns.add(null);
          }
        }
        list.add(columns);
      }
      
      request.setAttribute("list",list);
  }

  private PathQuery getAnalysisTable(String identifier) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews(
        "Protein.proteinAnalysisFeatures.programname",
        "Protein.proteinAnalysisFeatures.crossReference.identifier",
        "Protein.proteinAnalysisFeatures.locations.start",
        "Protein.proteinAnalysisFeatures.locations.end",
        "Protein.proteinAnalysisFeatures.rawscore",
        "Protein.proteinAnalysisFeatures.significance");
    query.addOrderBy("Protein.proteinAnalysisFeatures.programname", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("Protein.primaryIdentifier",identifier));
    return query;
  }
}
