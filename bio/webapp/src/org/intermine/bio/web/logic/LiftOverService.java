package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.GenomicRegionSearchConstraint;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Communication between intermine and liftover server in a way as
 * posting http request from the webapp, avoiding cross-domian issue.
 *
 * @author Fengyuan Hu
 *
 */
public class LiftOverService
{
    private static final Logger LOG = Logger.getLogger(LiftOverService.class);

    private static final Map<String, String> ORGANISM_COMMON_NAME_MAP;
    static {
        Map<String, String> tempMap = new HashMap<String, String>();
        tempMap.put("H. sapiens", "human");
        tempMap.put("M. musculus", "mouse");
        tempMap.put("D. melanogaster", "fly");
        tempMap.put("C. elegans", "worm");
        ORGANISM_COMMON_NAME_MAP = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Send a HTTP POST request to liftOver service.
     *
     * @param grsc the Genomic Region Search constraint
     * @param org human or mouse
     * @param genomeVersionSource older genome version
     * @param genomeVersionTarget intermine genome version
     * @param liftOverServerURL url
     * @return a list of GenomicRegion
     */
    public Map<String, List<GenomicRegion>> doLiftOver(
            GenomicRegionSearchConstraint grsc, String org,
            String genomeVersionSource, String genomeVersionTarget,
            String liftOverServerURL) {

        List<GenomicRegion> genomicRegionList = grsc.getGenomicRegionList();
        Map<String, List<GenomicRegion>> liftedGenomicRegionMap =
            new HashMap<String, List<GenomicRegion>>();

        String coords = converToBED(genomicRegionList);
        String organism = ORGANISM_COMMON_NAME_MAP.get(org);

        try {
            // Construct data
            String data = URLEncoder.encode("coords", "UTF-8") + "="
                    + URLEncoder.encode(coords, "UTF-8");
            data += "&" + URLEncoder.encode("source", "UTF-8") + "="
                    + URLEncoder.encode(genomeVersionSource, "UTF-8");
            data += "&" + URLEncoder.encode("target", "UTF-8") + "="
                    + URLEncoder.encode(genomeVersionTarget, "UTF-8");

            // Send data
            URL url;
            // liftOverServerURL ends with "/"
            if (!liftOverServerURL.endsWith("/")) {
                url = new URL(liftOverServerURL + "/" + organism);
            } else {
                url = new URL(liftOverServerURL + organism);
            }
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            wr.close();

            String liftOverResponse = new String(IOUtils.toCharArray(conn.getInputStream()));

            LOG.info("LiftOver response message: \n" + liftOverResponse);

            // Get the response
            try {
                JSONObject json = new JSONObject(liftOverResponse);

                JSONArray liftedArray = json.getJSONArray("coords");
                JSONArray unmappedArray = json.getJSONArray("unmapped");

                // parse json object
                List<GenomicRegion> liftedList = new ArrayList<GenomicRegion>();
                for (int i = 0; i < liftedArray.length(); i++) {
                    String coord = (String) liftedArray.get(i);
                    coord.trim();
                    GenomicRegion gr = new GenomicRegion();
                    gr.setOrganism(org);
                    gr.setExtendedRegionSize(grsc.getExtendedRegionSize());
                    gr.setChr(coord.split("\t")[0].trim());
                    gr.setStart(Integer.valueOf(coord.split("\t")[1].trim()));
                    gr.setEnd(Integer.valueOf(coord.split("\t")[2].trim()));
                    liftedList.add(gr);
                }

                List<GenomicRegion> unmappedList = new ArrayList<GenomicRegion>();
                for (int i = 0; i < unmappedArray.length(); i++) {
                    String coord = (String) unmappedArray.get(i);
                    coord.trim();
                    // TODO lose information about why the regions not lifted
                    // e.g. "#Partially deleted in new\n"
                    if (!coord.startsWith("#")) {
                        GenomicRegion gr = new GenomicRegion();
                        gr.setOrganism(org);
                        gr.setExtendedRegionSize(grsc.getExtendedRegionSize());
                        gr.setChr(coord.split("\t")[0].trim());
                        gr.setStart(Integer.valueOf(coord.split("\t")[1].trim()));
                        gr.setEnd(Integer.valueOf(coord.split("\t")[2].trim()));
                        unmappedList.add(gr);
                    }
                }

                liftedGenomicRegionMap.put("lifedGenomicRegions", liftedList);
                liftedGenomicRegionMap.put("unmappedGenomicRegions", unmappedList);

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return liftedGenomicRegionMap;
    }

    private String converToBED(List<GenomicRegion> genomicRegionList) {

        StringBuffer coords = new StringBuffer();

        for (GenomicRegion gr : genomicRegionList) {
            coords.append(gr.getChr()).append("\t").append(gr.getStart())
                    .append("\t").append(gr.getEnd()).append("\n");
        }
        return coords.toString();
    }
}
