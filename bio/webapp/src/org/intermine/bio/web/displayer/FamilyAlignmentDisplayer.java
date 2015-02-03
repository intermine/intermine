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
import org.intermine.model.bio.ProteinFamily;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;


public class FamilyAlignmentDisplayer extends ReportDisplayer {

  /** @var sets the max number of locations to show in a table, TODO: match with DisplayObj*/
  private Integer maximumNumberOfLocations = 27;

  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public FamilyAlignmentDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
      super(config, im);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
      HttpSession session = request.getSession();
      final InterMineAPI im = SessionMethods.getInterMineAPI(session);

      ProteinFamily familyObj = (ProteinFamily)reportObject.getObject();

      // check for family cluster id and display it.
      String[] stringFields = {"methodname","clustername"};
      for( String field : stringFields ) {
        try {
            String value = (String) familyObj.getFieldValue(field);
            if (!StringUtils.isBlank(value)) {
                request.setAttribute(field, value);
            }
        } catch (IllegalAccessException e) {
            // we'll quietly ignore missing fields.
        }
      }

      String[] intFields = {"clusterid","methodid"};
      for( String field : intFields ) {
        try {
            Integer value = (Integer) familyObj.getFieldValue(field);
            if (value != null) {
                request.setAttribute(field, value.toString());
            }
        } catch (IllegalAccessException e) {
            // we'll quietly ignore missing fields.
        }
      }

      // query the family members
      PathQuery query = getFamilyMembers(familyObj.getClusterId().toString());
      Profile profile = SessionMethods.getProfile(session);
      PathQueryExecutor exec = im.getPathQueryExecutor(profile);
      
      ExportResultsIterator result;
      try {
        result = exec.execute(query);
      } catch (ObjectStoreException e) {
        LOG.warn("ObjectStoreException in FamilyAlignmentDisplayer.java");
        return;
      }
      StringBuffer bs = new StringBuffer();
      while (result.hasNext()) {
        List<ResultElement> row = result.next();
        if ( bs.length() > 0 ) {
          bs.append("+");
        }
        String fullname = row.get(1).getField().toString();
        //bs.append(fullname.substring(0,fullname.indexOf(familyObj.getPrimaryIdentifier().toString())-1));
        bs.append(fullname);
      }
      request.setAttribute("members",bs.toString()); 
  }

  private PathQuery getFamilyMembers(String identifier) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("ProteinFamily.clusterId",
                   "ProteinFamily.protein.primaryIdentifier");
    query.addOrderBy("ProteinFamily.protein.primaryIdentifier", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("ProteinFamily.clusterId",identifier));
    return query;
  }
}
