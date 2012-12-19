package org.intermine.bio.webservice;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.export.GenomicRegionSequenceExporter;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.util.StringUtil;
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

        JSONArray regs = jsonRequest.getJSONArray("regions");
        int noOfRegs = regs.length();
        List<GenomicRegion> regions = new ArrayList<GenomicRegion>();
        List<String> regionStrList = new ArrayList<String>();
        for (int i = 0; i < noOfRegs; i++) {
            regionStrList.add(regs.getString(i));
        }
        regions = GenomicRegionSearchUtil.createGenomicRegionsFromString(
                regionStrList, org, null, false);
        return regions;
    }
}
