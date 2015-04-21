package org.intermine.bio.webservice;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.export.GenomicRegionSequenceExporter;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A service for exporting DNA sequence of given regions in FASTA format.
 *
 * @author Fengyuan Hu
 *
 */
public class GenomicRegionSequenceExportService extends WebService
{
    private OutputStream out;

    /**
     * Constructor.
     *
     * @param im A reference to an InterMine API settings bundle.
     */
    public GenomicRegionSequenceExportService(InterMineAPI im) {
        super(im);
    }

    @Override
    public Format getDefaultFormat() {
        return Format.UNKNOWN;
    }

    @Override
    public String getDefaultFileName() {
        return "results" + StringUtil.uniqueString() + ".fa";
    }

    @Override
    protected Output getDefaultOutput(PrintWriter out, OutputStream os, String sep) {
        this.out = os;
        output = new StreamedOutput(out, new PlainFormatter(), sep);
        if (isUncompressed()) {
            ResponseUtil.setCustomTypeHeader(response, getDefaultFileName(), getContentType());
        }
        return output;
    }

    private String getContentType() {
        return "text/x-fasta";
    }

    @Override
    protected void execute() throws Exception {
        GenomicRegionSequenceExporter exporter = new GenomicRegionSequenceExporter(
                im.getObjectStore(), out);
        exporter.export(parseRegionRequest());
    }

    /**
     * Derived from GenomicRegionSearchListInput.java
     */
    private List<GenomicRegion> parseRegionRequest() throws Exception {
      String input = "";
      if ("application/x-www-form-urlencoded".equals(request.getContentType())
          || "GET".equalsIgnoreCase(request.getMethod())) {
        input = getRequiredParameter("query");
      } else {
        input = IOUtils.toString(request.getInputStream());
      }
      JSONObject jsonRequest;
      try {
        jsonRequest = new JSONObject(input);
      } catch (JSONException e) {
        throw new BadRequestException("Invalid query: " + input);
      }

      String org = jsonRequest.getString("organism");

      int noOfRegs;
      int noOfFeats;
      JSONArray regs = null;
      JSONArray featRegs = null;
      Integer extendStart = new Integer(0);
      Integer extendEnd = new Integer(0);
      if (jsonRequest.has("regions")) {
        regs = jsonRequest.getJSONArray("regions");
        noOfRegs = regs.length();
      } else {
        noOfRegs = 0;
      }
      if( jsonRequest.has("features")) {
        featRegs = jsonRequest.getJSONArray("features");
        noOfFeats = featRegs.length();
      } else {
        noOfFeats = 0;
      }
      try {
        extendStart = new Integer(jsonRequest.getInt("extendStart"));
      } catch (JSONException e) { }
      try {
        extendEnd = new Integer(jsonRequest.getInt("extendEnd"));
      } catch (JSONException e) { }
      
      List<GenomicRegion> regions = new ArrayList<GenomicRegion>();
      List<String> regionStrList = new ArrayList<String>();
      for (int i = 0; i < noOfRegs; i++) {
        regionStrList.add(regs.getString(i));
      }
      for (int i = 0; i < noOfFeats; i++) {
        String reg = findFeatJSONSpan(featRegs.getString(i),org,extendStart,extendEnd);
        if (reg != null) {
          regionStrList.add(reg);
        }
      }
      regions = GenomicRegionSearchUtil.createGenomicRegionsFromString(
          regionStrList, org, null, false);
      return regions;
    }


    private String findFeatJSONSpan(String specifier,String org,Integer extendStart,Integer extendEnd) {
      // specifier is a path element : value pair
      String[] specParts = specifier.split(":",2);
      ClassDescriptor featureClass = null;
      try {
        Path p = new Path(im.getModel(),specParts[0]);
        featureClass = p.getStartClassDescriptor();
      } catch (PathException e) {
        // invalid path?
        return null;
      }
      String location = featureClass.getSimpleName()+".chromosomeLocation";
      PathQuery pQ = new PathQuery(im.getModel());
      pQ.addViews(location+".locatedOn.primaryIdentifier",location+".start",location+".end",location+".strand");
      pQ.addConstraint(Constraints.eq(specParts[0],specParts[1]));
      // phytozome specific: organism is specified by a proteome id
      pQ.addConstraint(Constraints.eq(featureClass.getSimpleName()+".organism.proteomeId",org));
      ExportResultsIterator result;
      PathQueryExecutor exec = im.getPathQueryExecutor();
      try {
        result = exec.execute(pQ);
      } catch (ObjectStoreException e) {
        return null;
      }
      String chr = null;
      Integer featStart = null;
      Integer featEnd = null;
      String strand = new String("1");
      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();
        chr = resElement.get(0).getField().toString();
        featStart = (Integer)resElement.get(1).getField();
        featEnd = (Integer)resElement.get(2).getField();
        strand = resElement.get(3).getField().toString();
      }
      boolean is_plus_strand = true;
      try {
        if (strand != null && !strand.isEmpty() && (Integer.valueOf(strand)<0) ) {
          is_plus_strand = false;
        }
      } catch (NumberFormatException e) {}
      if (is_plus_strand) {
        featStart -= extendStart;
        featEnd += extendEnd;
        return chr+":"+featStart+".."+featEnd;
      } else {
        featEnd += extendStart;
        featStart -= extendEnd;
        return chr+":"+featEnd+".."+featStart;
      }
    }
}
