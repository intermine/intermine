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
import org.intermine.model.bio.BioEntity;
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


public class CufflinksScoreDisplayer extends ReportDisplayer {

  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public CufflinksScoreDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
      super(config, im);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
      HttpSession session = request.getSession();
      final InterMineAPI im = SessionMethods.getInterMineAPI(session);
      

      BioEntity bioObj = (BioEntity)reportObject.getObject();
      
      PathQuery query = getScores(bioObj);
      Profile profile = SessionMethods.getProfile(session);
      PathQueryExecutor exec = im.getPathQueryExecutor(profile);
      ExportResultsIterator result;
      try {
        result = exec.execute(query);
      } catch (ObjectStoreException e) {
        // silently return
        LOG.error("Had an ObjectStoreException in CufflinksScoreDisplayer: "+e.getMessage());
        return;
      }
      
      ArrayList<Score> returnList = new ArrayList<Score>();
      
      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();
        String eName = (resElement.get(0).getField()==null)?"&nbsp;":
                             resElement.get(0).getField().toString();

        String eGroup = (resElement.get(1).getField()==null)?"&nbsp;":
                             resElement.get(1).getField().toString();
        
        Score thisRow = new Score(eName,eGroup,(Float)resElement.get(2).getField());
        returnList.add(thisRow);
      }
      request.setAttribute("list",returnList);
      request.setAttribute("id",bioObj.getId());
  }
  private PathQuery getScores(BioEntity b) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("CufflinksScore.experiment.name","CufflinksScore.experiment.experimentGroup",
        "CufflinksScore.fpkm");
    query.addOrderBy("CufflinksScore.experiment.name", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("CufflinksScore.bioentity.id",b.getId().toString()));
    return query;
  }
  public class Score {
    private String experiment;
    private String group;
    private Float fpkm;
    public Score(String experiment,String group, Float fpkm) {
      this.experiment = experiment;
      this.group = group;
      this.fpkm = fpkm;
    }
    public String getExperiment() { return experiment; }
    public String getGroup() { return group; }
    public Float getFpkm() { return fpkm; }
    }
}
