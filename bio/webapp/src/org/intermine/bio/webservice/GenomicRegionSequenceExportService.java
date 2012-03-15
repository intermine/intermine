package org.intermine.bio.webservice;

import java.util.ArrayList;
import java.util.List;

import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.export.GenomicRegionSequenceExporter;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.webservice.server.WebService;
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
    /**
     * Constructor.
     *
     * @param im A reference to an InterMine API settings bundle.
     */
    public GenomicRegionSequenceExportService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        doExport();
    }

    private void doExport() {
        GenomicRegionSequenceExporter exporter = new GenomicRegionSequenceExporter(
                im.getObjectStore(), response);
        try {
            exporter.export(parseRegionRequest());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Derived from GenomicRegionSearchListInput.java
     */
    private List<GenomicRegion> parseRegionRequest() throws JSONException {
        JSONObject jsonRequest = new JSONObject(request.getParameter("query"));

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
