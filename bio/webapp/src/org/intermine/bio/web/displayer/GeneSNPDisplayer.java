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
import org.intermine.model.bio.Gene;
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
      ExportResultsIterator result = exec.execute(query);

      ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
      ArrayList<String> lastColumns = null;
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
        if (lastColumns != null) {
          boolean coalesce = true;
          for(int i=1;i<6;i++) {
            if((columns.get(i)==null && lastColumns.get(i)!=null) ||
                (columns.get(i)!=null && lastColumns.get(i)==null) ||
                (columns.get(i)!=null && !columns.get(i).equals(lastColumns.get(i)))) {
              LOG.debug(columns.get(i)+ " does not match " + lastColumns.get(i));
              coalesce = false;
              break;
            }
          }
          if (coalesce) {
            lastColumns.set(0,lastColumns.get(0) + ", " + columns.get(0));
          } else {
            list.add(lastColumns);
            lastColumns = columns;
          }
        } else {
          lastColumns = columns;
        }
      }

      if( lastColumns != null) {
        list.add(lastColumns);
      }

      request.setAttribute("list",list);
      request.setAttribute("id",geneObj.getId());
  }

  private PathQuery getConsequenceTable(String identifier) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("SNP.snpDiversitySamples.diversitySample.name",
        "SNP.snpLocation.start",
        "SNP.snpLocation.reference",
        "SNP.alternate",
        "SNP.consequences.substitution",
        "SNP.consequences.type.type");
    query.addOrderBy("SNP.snpLocation.start", OrderDirection.ASC);
    query.addOrderBy("SNP.snpLocation.reference", OrderDirection.ASC);
    query.addOrderBy("SNP.alternate", OrderDirection.ASC);
    query.addOrderBy("SNP.consequences.substitution", OrderDirection.ASC);
    query.addOrderBy("SNP.consequences.type.type", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("SNP.consequences.gene.primaryIdentifier",identifier));
    return query;
  }
}
