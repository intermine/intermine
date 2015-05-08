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
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;


public class ProteinFamilyDisplayer extends ReportDisplayer {

  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public ProteinFamilyDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
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
        // silently return on error
        LOG.error("Had an ObjectStoreException in ProteinFamilyDisplayer: "+e.getMessage());
        return;
      }
      
      ArrayList<ProteinFamily> returnList = new ArrayList<ProteinFamily>();
      
      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();
        
        ProteinFamily thisRow = new ProteinFamily(resElement);
        returnList.add(thisRow);
      }
      request.setAttribute("list",returnList);
  }
  private PathQuery getScores(BioEntity b) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("ProteinFamily.id","ProteinFamily.clusterId","ProteinFamily.clusterName",
        "ProteinFamily.memberCount","ProteinFamily.methodId","ProteinFamily.methodName",
        "ProteinFamily.member.membershipDetail","ProteinFamily.msa.id");
    query.addOrderBy("ProteinFamily.clusterId", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("ProteinFamily.member.protein.name",b.getPrimaryIdentifier()));
    query.setOuterJoinStatus("ProteinFamily.msa",OuterJoinStatus.OUTER);
    return query;
  }
  public class ProteinFamily {
    private Integer familyId;
    private Integer clusterId;
    private String clusterName;
    private Integer memberCount;
    private Integer methodId;
    private String methodName;
    private String membershipDetail;
    private Integer msaId;
    public ProteinFamily(List<ResultElement> resultRow) {
      familyId = (Integer) resultRow.get(0).getField();
      clusterId = (Integer) resultRow.get(1).getField();
      clusterName = (String) resultRow.get(2).getField();
      memberCount = (Integer) resultRow.get(3).getField();
      methodId = (Integer) resultRow.get(4).getField();
      methodName = (String) resultRow.get(5).getField();
      membershipDetail = (String) resultRow.get(6).getField();
      msaId = (Integer) resultRow.get(7).getField();
    }
    public Integer getFamilyId() { return familyId;}
    public Integer getClusterId() { return clusterId;}
    public String getClusterName() { return clusterName;}
    public Integer getMemberCount() { return memberCount;}
    public Integer getMethodId() { return methodId;}
    public String getMethodName() { return methodName;}
    public String getMembershipDetail() { return membershipDetail;}
    public Integer getMsaId() { return msaId;}
    }
}
