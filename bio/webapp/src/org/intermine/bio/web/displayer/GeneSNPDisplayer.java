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


public class GeneSNPDisplayer extends ReportDisplayer {

  protected static final Logger LOG = Logger.getLogger(GeneSNPDisplayer.class);

  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public GeneSNPDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
      super(config, im);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
      HttpSession session = request.getSession();
      final InterMineAPI im = SessionMethods.getInterMineAPI(session);

      Gene geneObj = (Gene)reportObject.getObject();
      
      LOG.info("Entering GeneSNPDisplayer.display for "+geneObj.getPrimaryIdentifier());
      LOG.info("Id is "+geneObj.getId());

      // query the consequences, snps and location
      PathQuery query = getConsequenceTable(geneObj);
      Profile profile = SessionMethods.getProfile(session);
      PathQueryExecutor exec = im.getPathQueryExecutor(profile);
      ExportResultsIterator result;
      try {
        result = exec.execute(query);
      } catch (ObjectStoreException e) {
        // silently return
        LOG.error("Had an ObjectStoreException in GeneSNPDisplayer: "+e.getMessage());
        return;
      }

      // go through the results and coalesce. Join together everything with the same
      // columns 0-4 and make the genotype/sample a sub table
      ArrayList<SNPList> snpLists = new ArrayList<SNPList>();
      SNPList lastSNPList = null;

      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();

        ArrayList<String> thisRow = new ArrayList<String>();

        // copy columns 1-10:
        // id,name position, reference, alternate, substitution, classification and transcript
        for(int i=0;i<10;i++) {
          if ( (resElement.get(i) != null) && (resElement.get(i).getField() != null)) {
            thisRow.add(resElement.get(i).getField().toString());
          } else {
            thisRow.add("&nbsp;");
          }
        }
        
        if (lastSNPList == null ) {
          lastSNPList = new SNPList(thisRow);
        } else {
          // now see if this is a new
          // (id,name,postion,reference,alternate,substition,classification)
          if (lastSNPList.canCoalesce(thisRow) ) {
            lastSNPList.join(thisRow);
          } else {
            snpLists.add(lastSNPList);
            lastSNPList = new SNPList(thisRow);
          }
        }
      }
      // final row
      if (lastSNPList != null) {
        snpLists.add(lastSNPList);
      }
          
      request.setAttribute("list",snpLists);
      request.setAttribute("id",geneObj.getId());
      
  }

  private PathQuery getConsequenceTable(Gene identifier) {
    PathQuery query = new PathQuery(im.getModel());

    query.addViews( "Genotype.snp.id","Genotype.snp.name",
        "Genotype.snp.locations.start",
        "Genotype.snp.reference",
        "Genotype.snp.alternate",
        "Genotype.snp.consequences.substitution",
        "Genotype.snp.consequences.type.type",
        "Genotype.snp.consequences.transcript.primaryIdentifier",
        "Genotype.genotype","Genotype.sampleInfo");
    query.addOrderBy("Genotype.snp.locations.start", OrderDirection.ASC);
    query.addOrderBy("Genotype.snp.reference", OrderDirection.ASC);
    query.addOrderBy("Genotype.snp.alternate", OrderDirection.ASC);
    query.addOrderBy("Genotype.snp.consequences.substitution", OrderDirection.ASC);
    query.addOrderBy("Genotype.snp.consequences.type.type", OrderDirection.ASC);
    query.addOrderBy("Genotype.genotype", OrderDirection.ASC);
    query.addOrderBy("Genotype.snp.consequences.transcript.primaryIdentifier", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("Genotype.snp.consequences.gene.id",identifier.getId().toString()));
    return query;
  }

  public class SNPList {
    private Integer id;
    private String name;
    private String position;
    private String reference;
    private String alternate;
    private String substitution;
    private String classification;
    private TreeSet<String> transcripts;
    private TreeMap<String,GenoSample> genoSamples;
    public SNPList(ArrayList<String> record) {
      id = new Integer(record.get(0));
      name = new String(record.get(1));
      position = new String(record.get(2));
      reference = new String(record.get(3));
      alternate = new String(record.get(4));
      substitution = new String(record.get(5));
      classification = new String(record.get(6));
      transcripts = new TreeSet<String>();
      transcripts.add(record.get(7));
      genoSamples = new TreeMap<String,GenoSample>();
      genoSamples.put(record.get(8),new GenoSample(record.get(8),record.get(9)));
    }


    public boolean canCoalesce(ArrayList<String> record) {
      return name.equals(record.get(1)) &&
             position.equals(record.get(2)) &&
             reference.equals(record.get(3)) &&
             alternate.equals(record.get(4)) &&
             substitution.equals(record.get(5)) &&
             classification.equals(record.get(6));
    }

    private void join(ArrayList<String> record) {
      transcripts.add(record.get(7));
      if (!genoSamples.containsKey(record.get(8))) {
        genoSamples.put(record.get(8),new GenoSample(record.get(8),record.get(9)));
      }
    }

    public String getId() { return id.toString(); }
    public String getName() { return name; }
    public String getPosition() { return position; }
    public String getReference() { return reference; }
    public String getAlternate() { return alternate; }
    public String getSubstitution() { return substitution; }
    public String getClassification() { return classification; }
    public String getTranscripts() { StringBuffer retString = new StringBuffer();
                                     for(String s: transcripts) {
                                       if( !retString.toString().isEmpty() ) retString.append(", ");
                                       retString.append(s);
                                     }
                                     return retString.toString(); }
    public ArrayList<GenoSample> getGenoSamples() { ArrayList<GenoSample> retArray = new ArrayList<GenoSample>();
                                                    for(String s: genoSamples.keySet()) {
                                                      retArray.add(genoSamples.get(s));
                                                    }
                                                    return retArray; }
    public Integer getGenoSampleCount() { return genoSamples.keySet().size() ; }
  }

  public class GenoSample {
    private String genotype;
    private StringBuffer samples;
    public GenoSample(String g,String s) {
      genotype = g;
      samples = new StringBuffer();
      final Pattern pattern = Pattern.compile("\"([^\"]+)\":\"([^\"]+)\"");
      final Matcher matcher = pattern.matcher(s);
      while (matcher.find()) {
        if (samples.length() > 0) {
          samples.append(", ");
        }
        samples.append(matcher.group(1));
      }
    }
    public void addSample(String s) { samples.append(", ").append(s); }
    public String getGenotype() { return genotype; }
    public String getSamples() { return samples.toString(); }
  }
}
