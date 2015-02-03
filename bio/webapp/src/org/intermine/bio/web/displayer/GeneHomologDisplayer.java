package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

  protected static final Logger LOG = Logger.getLogger(GeneHomologDisplayer.class);
  PathQueryExecutor exec;
  private HashMap<Integer,String> organismMap = new HashMap<Integer,String>();

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

      // query the homologs
      PathQuery query = getHomologTable(geneObj.getId());
      Profile profile = SessionMethods.getProfile(session);
      exec = im.getPathQueryExecutor(profile);
      ExportResultsIterator result;
      try {
        result = exec.execute(query);
      } catch (ObjectStoreException e) {
        // silently return
        LOG.warn("Had an ObjectStoreException in GeneHomologDisplayer.java: "+e.getMessage());
        return;
      }

      ArrayList<HomologRecord> homologList = new ArrayList<HomologRecord>();
      
      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();
        HomologRecord r = new HomologRecord(resElement);
        String groupName = r.getGroupName();
        // the inParanoide group names are proteome1_proteome2_counter
        String[] fields = groupName.split("_");
        if (fields.length == 3) {
          try {
            Integer p1 = Integer.parseInt(fields[0]);
            Integer p2 = Integer.parseInt(fields[1]);
            if (!organismMap.containsKey(p1)) {
              organismMap.put(p1, getShortName(p1));
            }
            if (!organismMap.containsKey(p2)) {
              organismMap.put(p2, getShortName(p2));
            }
            r.setNodeName(organismMap.get(p1)+"-"+organismMap.get(p2));
          } catch (NumberFormatException e) {
            // radio silence.
          }
        }
        homologList.add(r);
      }
        
      // for accessing this within the jsp
      request.setAttribute("geneName",geneObj.getPrimaryIdentifier());
      request.setAttribute("list",homologList);
      request.setAttribute("id",geneObj.getId());
      
  }

  private PathQuery getHomologTable(Integer id) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews( "Homolog.id",
        "Homolog.groupName",
        "Homolog.gene2.id",
        "Homolog.gene2.primaryIdentifier",
        "Homolog.gene2.briefDescription",
        "Homolog.gene2.organism.shortName",
        "Homolog.relationship"
        );
    query.addOrderBy("Homolog.groupName", OrderDirection.ASC);
    query.addOrderBy("Homolog.gene2.primaryIdentifier", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("Homolog.gene1.id",id.toString()));
    return query;
  }
  
  private String getShortName(Integer p) {
    PathQuery q = new PathQuery(im.getModel());
    q.addViews("Organism.shortName");
    q.addConstraint(Constraints.eq("Organism.proteomeId",p.toString()));

    ExportResultsIterator result;
    try {
      result = exec.execute(q);
    } catch (ObjectStoreException e) {
      // silently return
      LOG.warn("Had an ObjectStoreException in GeneHomologDisplayer.java: "+e.getMessage());
      return null;
    }
    
    while (result.hasNext()) {
      List<ResultElement> resElement = result.next();
      return resElement.get(0).getField().toString();
    }
    return null;
  }
  
  public class HomologRecord {
    private String id;
    private String groupName;
    private String geneId;
    private String geneName;
    private String geneDefline;
    private String organism;
    private String relationship;
    private String nodeName;

    public HomologRecord(List<ResultElement> resElement) {
      // the fields are a copy of the query results
      id = ((resElement.get(0)!=null) && (resElement.get(0).getField()!= null))?
                                 resElement.get(0).getField().toString():"&nbsp;";
      groupName = ((resElement.get(1)!=null) && (resElement.get(1).getField()!= null))?
                                 resElement.get(1).getField().toString():"&nbsp;";
      geneId = ((resElement.get(2)!=null) && (resElement.get(2).getField()!= null))?
                                 resElement.get(2).getField().toString():"&nbsp;";
      geneName = ((resElement.get(3)!=null) && (resElement.get(3).getField()!= null))?
                                 resElement.get(3).getField().toString():"&nbsp;";
      geneDefline = ((resElement.get(4)!=null) && (resElement.get(4).getField()!= null))?
                                 resElement.get(4).getField().toString():"&nbsp;";
      organism = ((resElement.get(5)!=null) && (resElement.get(5).getField()!= null))?
                                 resElement.get(5).getField().toString():"&nbsp;";
      relationship = ((resElement.get(6)!=null) && (resElement.get(6).getField()!= null))?
                                 resElement.get(6).getField().toString():"&nbsp;";
      nodeName = "&nbsp";
 
    }
    private void setNodeName(String s) { nodeName = s;}
    public String getId() { return id; }
    public String getGroupName() { return groupName; }
    public String getGeneId() { return geneId; }
    public String getGeneName() { return geneName; }
    public String getGeneDefline() { return geneDefline; }
    public String getOrganism() { return organism; }
    public String getRelationship() { return relationship; }
    public String getNodeName() { return nodeName; }
  }
  
}
