package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
//import org.intermine.bio.web.displayer.ProteinAnalysisDisplayer.ProteinAnalysisFeatureRecord;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
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


public class OntologyAnnotationDisplayer extends ReportDisplayer {

  protected static final Logger LOG = Logger.getLogger(OntologyAnnotationDisplayer.class);
  PathQueryExecutor exec;
  private HashMap<Integer,String> organismMap = new HashMap<Integer,String>();

  // when it comes time to display the results, how do we rank them?
  private static final HashMap<String,Integer> dbRank = new HashMap<String,Integer>();
  static {
  // this is tough; how can I set this better?
  dbRank.put("GO",         new Integer(0));
  dbRank.put("PFAM",       new Integer(1));
  dbRank.put("PANTHER",    new Integer(2));
  dbRank.put("KEGG",       new Integer(3));
  dbRank.put("KOG",        new Integer(4));
  dbRank.put("ENZYME",     new Integer(5));
  dbRank.put("PIR",        new Integer(6));
  dbRank.put("PIRSF",      new Integer(7));
  dbRank.put("PROFILE",    new Integer(8));
  dbRank.put("PRODOM",     new Integer(9));
  dbRank.put("PROSITE",    new Integer(10));
  dbRank.put("SMART",      new Integer(11));
  dbRank.put("SIGNALP",    new Integer(12));
  dbRank.put("SUPERFAMILY",new Integer(13));
  dbRank.put("TIGRFAMs",   new Integer(14));
  dbRank.put("PRINTS",     new Integer(15));
  dbRank.put("TMHMM",      new Integer(16));
  dbRank.put("GENE3D",     new Integer(17));
  }
  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public OntologyAnnotationDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
      super(config, im);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
      HttpSession session = request.getSession();
      final InterMineAPI im = SessionMethods.getInterMineAPI(session);

      BioEntity geneObj = (BioEntity)reportObject.getObject();
      
      LOG.info("Entering OntologyAnnotationDisplayer.display for "+geneObj.getPrimaryIdentifier());
      LOG.info("Id is "+geneObj.getId());

      // query the annotations
      PathQuery query = getOntologyAnnotationTable(geneObj.getId());
      Profile profile = SessionMethods.getProfile(session);
      exec = im.getPathQueryExecutor(profile);
      ExportResultsIterator result;
      try {
        result = exec.execute(query);
      } catch (ObjectStoreException e) {
        // silently return
        LOG.warn("Had an ObjectStoreException in OntologyAnnotationDisplayer.java: "+e.getMessage());
        return;
      }

      ArrayList<OntologyRecord> ontList = new ArrayList<OntologyRecord>();
      
      // protect against duplicates! (##hack#3 ##hack##)
      String lastOntology = null;
      String lastName = null;
      String lastTerm = null;
      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();
        OntologyRecord r = new OntologyRecord(resElement);
        if (!r.getOntology().equals(lastOntology) || 
            !r.getName().equals(lastName) || 
            !r.getTerm().equals(lastTerm)) {
          ontList.add(r);
        }
        lastOntology = r.getOntology();
        lastName = r.getName();
        lastTerm = r.getTerm();
      }

      // order per our guardian angel.
      Collections.sort(ontList);
      // for accessing this within the jsp
      request.setAttribute("geneName",geneObj.getPrimaryIdentifier());
      request.setAttribute("list",ontList);
      request.setAttribute("id",geneObj.getId());
      
  }

  private PathQuery getOntologyAnnotationTable(Integer id) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews( "OntologyAnnotation.ontologyTerm.ontology.name",
        "OntologyAnnotation.ontologyTerm.identifier",
        "OntologyAnnotation.ontologyTerm.name",
        "OntologyAnnotation.ontologyTerm.namespace",
        "OntologyAnnotation.ontologyTerm.description"
        );
    query.addOrderBy("OntologyAnnotation.ontologyTerm.ontology.name", OrderDirection.ASC);
    query.addOrderBy("OntologyAnnotation.ontologyTerm.identifier", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("OntologyAnnotation.subject.id",id.toString()));
    return query;
  }
  
  
  public class OntologyRecord implements Comparable {
    private String ontology;
    private String term;
    private String name;
    private String namespace;
    private String description;
    private Integer rank;

    public OntologyRecord(List<ResultElement> resElement) {
      // the fields are a copy of the query results
      ontology = ((resElement.get(0)!=null) && (resElement.get(0).getField()!= null))?
                                 resElement.get(0).getField().toString():"&nbsp;";
      term = ((resElement.get(1)!=null) && (resElement.get(1).getField()!= null))?
                                 resElement.get(1).getField().toString():"&nbsp;";
      name = ((resElement.get(2)!=null) && (resElement.get(2).getField()!= null))?
                                 resElement.get(2).getField().toString():"&nbsp;";
      namespace = ((resElement.get(3)!=null) && (resElement.get(3).getField()!= null))?
                                 resElement.get(3).getField().toString():"&nbsp;";
      description = ((resElement.get(4)!=null) && (resElement.get(4).getField()!= null))?
                                 resElement.get(4).getField().toString():"&nbsp;";
      // TODO: hope that GENE3D is better some day
      if(ontology.equals("GENE3D")) { term = "&nbsp;";}

      rank = (dbRank.containsKey(ontology))?(dbRank.get(ontology)):(dbRank.size());    
 
    }
    public String getOntology() { return ontology; }
    public String getTerm() { return term; }
    public String getName() { return name; }
    public String getNamespace() { return namespace; }
    public String getDescription() { return description; }
    public int getRank() { return rank.intValue(); }

    @Override
    public int compareTo(Object o) {
      return this.getRank() - ((OntologyRecord)o).getRank();
    }
  }
  
}
