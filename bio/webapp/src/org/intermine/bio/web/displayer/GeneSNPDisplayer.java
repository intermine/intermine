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
      PathQuery query = getConsequenceTable(geneObj.getPrimaryIdentifier());
      Profile profile = SessionMethods.getProfile(session);
      PathQueryExecutor exec = im.getPathQueryExecutor(profile);
      ExportResultsIterator result;
      try {
        result = exec.execute(query);
      } catch (ObjectStoreException e) {
        // silently return
        LOG.warn("Had an ObjectStoreException in GeneSNPDisplayer.java");
        return;
      }
      // we're allowing the genotype to be embedded, but it must be delimited with :'s
      Pattern genotypePattern = Pattern.compile("^(.+:)?GT=(\\d+([|/]\\d+)*)(:.+)?$");

      // go through the results and coalesce. Join together everything with the same
      // columns 0-4 and make the genotype/sample a sub table
      ArrayList<SNPList> snpLists = new ArrayList<SNPList>();
      SNPList lastSNPList = null;

      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();

        ArrayList<String> thisRow = new ArrayList<String>();

        // copy columns 1-6:
        // position, reference, alternate, substitution, classification and transcript
        // and column 8: sample name.
        // column 7 has the genotype. we'll be rewriting that
        for(int i=0;i<8;i++) {
          if ( (resElement.get(i) != null) && (resElement.get(i).getField() != null)) {
            thisRow.add(resElement.get(i).getField().toString());
          } else {
            thisRow.add("&nbsp;");
          }
        }
        // column 6 has the genotype (N/M) along with other tags and field ids.
        // We want to extract the N/M and toss the rest
        String fullGenotype = thisRow.get(6);
        if (fullGenotype != null) {
          Matcher match = genotypePattern.matcher(fullGenotype);
          boolean gotIt = false;
          while (match.find()) {
            // there should only be 1. The genotype field is in the second capturing group
            thisRow.set(6,fullGenotype.substring(match.start(2),match.end(2)));
            gotIt = true;
            break;
          }
          if (!gotIt) {
            thisRow.set(6,"&nbsp;");
          }
        } else {
          thisRow.set(6,"&nbsp;");
        }

        if (lastSNPList == null ) {
          lastSNPList = new SNPList(thisRow);
        } else {
          // now see if this is a new
          // (postion,reference,alternate,substition,classification)
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

  private PathQuery getConsequenceTable(String identifier) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews( "SNP.locations.start",
                    "SNP.reference",
                    "SNP.alternate",
                    "SNP.consequences.substitution",
                    "SNP.consequences.type.type",
                    "SNP.consequences.transcript.primaryIdentifier",
                    "SNP.snpDiversitySamples.genotype",
                    "SNP.snpDiversitySamples.diversitySample.name");
    query.addOrderBy("SNP.locations.start", OrderDirection.ASC);
    query.addOrderBy("SNP.reference", OrderDirection.ASC);
    query.addOrderBy("SNP.alternate", OrderDirection.ASC);
    query.addOrderBy("SNP.consequences.substitution", OrderDirection.ASC);
    query.addOrderBy("SNP.consequences.type.type", OrderDirection.ASC);
    query.addOrderBy("SNP.consequences.transcript.primaryIdentifier", OrderDirection.ASC);
    query.addOrderBy("SNP.snpDiversitySamples.genotype", OrderDirection.DESC);
    query.addConstraint(Constraints.eq("SNP.consequences.gene.primaryIdentifier",identifier));
    return query;
  }

  public class SNPList {
    private String position;
    private String reference;
    private String alternate;
    private String substitution;
    private String classification;
    private TreeSet<String> transcripts;
    private TreeMap<String,GenoSample> genoSamples;
    public SNPList(ArrayList<String> record) {
      position = new String(record.get(0));
      reference = new String(record.get(1));
      alternate = new String(record.get(2));
      substitution = new String(record.get(3));
      classification = new String(record.get(4));
      transcripts = new TreeSet<String>();
      transcripts.add(record.get(5));
      genoSamples = new TreeMap<String,GenoSample>();
      genoSamples.put(record.get(6),new GenoSample(record.get(6),record.get(7)));
    }

    public boolean canCoalesce(ArrayList<String> record) {
      return position.equals(record.get(0)) &&
             reference.equals(record.get(1)) &&
             alternate.equals(record.get(2)) &&
             substitution.equals(record.get(3)) &&
             classification.equals(record.get(4));
    }

    private void join(ArrayList<String> record) {
      transcripts.add(record.get(5));
      if (!genoSamples.containsKey(record.get(6))) {
        genoSamples.put(record.get(6),new GenoSample(record.get(6),record.get(7)));
      } else {
        genoSamples.get(record.get(6)).addSample(record.get(7));
      }
    }

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
      genotype = new String(g);
      samples = new StringBuffer(s);
    }
    public void addSample(String s) { samples.append(", ").append(s); }
    public String getGenotype() { return genotype; }
    public String getSamples() { return samples.toString(); }
  }
}
