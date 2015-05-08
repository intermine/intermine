package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.MSA;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;


public class AlignmentDisplayer extends ReportDisplayer {

  /** @var sets the max number of locations to show in a table, TODO: match with DisplayObj*/
  private Integer maximumNumberOfLocations = 27;

  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public AlignmentDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
      super(config, im);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
      HttpSession session = request.getSession();
      final InterMineAPI im = SessionMethods.getInterMineAPI(session);

      MSA msaObj = (MSA)reportObject.getObject();

      // check for family cluster id and display it.
      String[] stringFields = {"primaryIdentifier","alignment","HMM"};
      for( String field : stringFields ) {
        try {
            String value = (String) msaObj.getFieldValue(field);
            if (!StringUtils.isBlank(value)) {
                request.setAttribute(field, value);
            }
        } catch (IllegalAccessException e) {
            // we'll quietly ignore missing fields.
        }
      }

  }

}
