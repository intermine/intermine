/**
 * 
 */
package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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


public class ProteinAnalysisDisplayer extends ReportDisplayer {

  protected static final Logger LOG = Logger.getLogger(ProteinAnalysisDisplayer.class);

  // when it comes time to display the results, how do we rank them?
  private static final HashMap<String,Integer> dbRank = new HashMap<String,Integer>();
  static {
  // this is tough; how can I set this better?
  dbRank.put("PFAM",       new Integer(0));
  dbRank.put("PANTHER",    new Integer(1));
  dbRank.put("KEGG",       new Integer(2));
  dbRank.put("KOG",        new Integer(3));
  dbRank.put("PIR",        new Integer(4));
  dbRank.put("PIRSF",      new Integer(5));
  dbRank.put("PROFILE",    new Integer(6));
  dbRank.put("PRODOM",     new Integer(7));
  dbRank.put("PROSITE",    new Integer(8));
  dbRank.put("SMART",      new Integer(9));
  dbRank.put("SIGNALP",    new Integer(10));
  dbRank.put("SUPERFAMILY",new Integer(11));
  dbRank.put("TIGRFAMs",   new Integer(12));
  dbRank.put("PRINTS",     new Integer(13));
  dbRank.put("TMHMM",      new Integer(14));
  dbRank.put("GENE3D",     new Integer(15));
  }
  
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
      PathQuery query = getAnalysisTable(proteinObj.getSecondaryIdentifier());
      Profile profile = SessionMethods.getProfile(session);
      PathQueryExecutor exec = im.getPathQueryExecutor(profile);
      ArrayList<ProteinAnalysisFeatureRecord> list = new ArrayList<ProteinAnalysisFeatureRecord>();
       
      ExportResultsIterator result;
      try {
        result = exec.execute(query);
      } catch (ObjectStoreException e) {
        LOG.warn("ObjectStoreException in ProteinAnalysisDisplayer.java "+e.getMessage());
        return;
      }

      while (result.hasNext()) {
        List<ResultElement> row = result.next();
        list.add(new ProteinAnalysisFeatureRecord(row));
      }
      
      // order per our guardian angel.
      Collections.sort(list);
      
      request.setAttribute("list",list);
  }

  private PathQuery getAnalysisTable(String identifier) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews(
        "Protein.proteinAnalysisFeatures.sourceDatabase.name",
        "Protein.proteinAnalysisFeatures.crossReference.identifier",
        "Protein.proteinAnalysisFeatures.crossReference.subject.primaryIdentifier",
        "Protein.proteinAnalysisFeatures.crossReference.subject.id",
        "Protein.proteinAnalysisFeatures.locations.start",
        "Protein.proteinAnalysisFeatures.locations.end",
        "Protein.proteinAnalysisFeatures.rawscore",
        "Protein.proteinAnalysisFeatures.significance");
    query.setOuterJoinStatus("Protein.proteinAnalysisFeatures.crossReference.subject",OuterJoinStatus.OUTER);
    query.addOrderBy("Protein.proteinAnalysisFeatures.sourceDatabase.name", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("Protein.secondaryIdentifier",identifier));
    return query;
  }

  public class ProteinAnalysisFeatureRecord implements Comparable {
    private String database;
    private String subject;
    private String domain;
    private String domainId;
    private String start;
    private String end;
    private String score;
    private String significance;
    // this is the sorting rank
    private Integer rank;
    
    public ProteinAnalysisFeatureRecord(List<ResultElement> resElement) {
      // the fields are a copy of the query results
      database = ((resElement.get(0)!=null) && (resElement.get(0).getField()!= null))?
                                 resElement.get(0).getField().toString():"&nbsp;";
      subject = ((resElement.get(1)!=null) && (resElement.get(1).getField()!= null))?
                                 resElement.get(1).getField().toString():"&nbsp;";
      domain = ((resElement.get(2)!=null) && (resElement.get(2).getField()!= null))?
                                 resElement.get(2).getField().toString():"&nbsp;";
      domainId = ((resElement.get(3)!=null) && (resElement.get(3).getField()!= null))?
                                 resElement.get(3).getField().toString():"";
      start = ((resElement.get(4)!=null) && (resElement.get(4).getField()!= null))?
                                 resElement.get(4).getField().toString():"&nbsp;";
      end = ((resElement.get(5)!=null) && (resElement.get(5).getField()!= null))?
                                 resElement.get(5).getField().toString():"&nbsp;";
      score = ((resElement.get(6)!=null) && (resElement.get(6).getField()!= null))?
                                 resElement.get(6).getField().toString():"&nbsp;";
      significance = ((resElement.get(7)!=null) && (resElement.get(7).getField()!= null))?
                                 resElement.get(7).getField().toString():"&nbsp;";
      rank = (dbRank.containsKey(database))?(dbRank.get(database)):(dbRank.size());           
                                 

    }
    public String getDatabase() { return database; }
    public String getSubject() { return subject; }
    public String getDomain() { return domain; }
    public String getDomainId() { return domainId; }
    public String getStart() { return start; }
    public String getEnd() { return end; }
    public String getScore() { return score; }
    public String getSignificance() { return significance; }
    public int getRank() { return rank.intValue(); }
    
    @Override
    public int compareTo(Object o) {
      return this.getRank() - ((ProteinAnalysisFeatureRecord)o).getRank();
    }
  }


}
