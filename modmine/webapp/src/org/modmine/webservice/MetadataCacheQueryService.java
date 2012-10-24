package org.modmine.webservice;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONObject;
import org.modmine.web.DisplayExperiment;
import org.modmine.web.FeatureCountsRecord;
import org.modmine.web.GBrowseParser;
import org.modmine.web.MetadataCache;
import org.modmine.web.GBrowseParser.GBrowseTrack;

/**
 * A modMine specific webservice for modENCODE home page data display purpose.
 *
 * @author Fengyuan Hu
 *
 */
public class MetadataCacheQueryService
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(MetadataCacheQueryService.class);

    private static final Map<String, String> RESOURCE_METHOD_MAP;
    static {
        Map<String, String> tempMap = new HashMap<String, String>();
        tempMap.put("catexp", "getCatExpJsonString");
        tempMap.put("webapp_path", "getWebappPath");
        tempMap.put("gbrowse_base_url", "getGBrowseBaseURL");
        tempMap.put("test", "testWebservice");
        tempMap.put("catexp.test", "testWebservice");
        RESOURCE_METHOD_MAP = Collections.unmodifiableMap(tempMap);
    }

    private static final String GBROWSE_DEFAULT_URL =
        "http://modencode.oicr.on.ca/cgi-bin/gb2/gbrowse/";

    private ObjectStore os;
    private String resourcePath;
    private HttpServletRequest request;

    private static final String NO_DATA_RETURN = "No data return";

    /**
     * Generate JSON string
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param resourcePath resource path such as "/catexp/fly"
     * @throws Exception Exception
     */
    protected void service(HttpServletRequest request,
            HttpServletResponse response, String resourcePath) throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        this.os = im.getObjectStore();
        this.resourcePath = resourcePath;
        this.request = request;

        if (RESOURCE_METHOD_MAP.keySet().contains(resourcePath)) {
            String methodName = RESOURCE_METHOD_MAP.get(resourcePath);

            // Reflection
            Method classMethod = this.getClass().getDeclaredMethod(methodName);
            classMethod.setAccessible(true); // access private methods

            String jsonString = (String) classMethod.invoke(this);
            doWrite(response, jsonString);
        }
        else {
            if ("metadatacache".equals(resourcePath)) {
                String responseString = NO_DATA_RETURN
                        + "\nMissing resource path (e.g. /catexp)...";
                doWrite(response, responseString);
            }
            else {
                String responseString = NO_DATA_RETURN
                    + "\nResource not available: /" + resourcePath.replaceAll("\\.", "/");
                doWrite(response, responseString);
            }
        }
    }

    private void doWrite(HttpServletResponse response, String jsonString) throws IOException {
        PrintWriter out = response.getWriter();
        out.println(jsonString);
        out.flush();
        out.close();
    }

    @SuppressWarnings("unused")
    private String getCatExpJsonString() {

        Map<String, List<DisplayExperiment>> ceMap = MetadataCache.getCategoryExperiments(os);
        Map<String, List<GBrowseTrack>> gBrowseTracks =
            MetadataCache.getExperimentGBrowseTracks(os);

        // Step One, create a new map: categories -> organsims -> experiments
        Map<String, Map<String, List<DisplayExperiment>>> newCEMap =
            new LinkedHashMap<String, Map<String, List<DisplayExperiment>>>();

        for (String cat : ceMap.keySet()) {
            List<DisplayExperiment> flyExpList = new ArrayList<DisplayExperiment>();
            List<DisplayExperiment> wormExpList = new ArrayList<DisplayExperiment>();

            Map<String, List<DisplayExperiment>> orgExpMap =
                new LinkedHashMap<String, List<DisplayExperiment>>();
            for (DisplayExperiment de : ceMap.get(cat)) {
                if (de.getOrganisms().contains("D. melanogaster")) {
                    flyExpList.add(de);
                }

                if (de.getOrganisms().contains("C. elegans")) {
                    wormExpList.add(de);
                }
            }

            orgExpMap.put("D. melanogaster", flyExpList);
            orgExpMap.put("C. elegans", wormExpList);

            newCEMap.put(cat, orgExpMap);
        }

        // Step Two, create JSON from newCEMap
        Map<String, Object> catMap = new LinkedHashMap<String, Object>();
        List<Object> catList = new ArrayList<Object>();

        for (String cat : newCEMap.keySet()) {
            Map<String, Object> catOrgMap = new LinkedHashMap<String, Object>();
            List<Object> orgList = new ArrayList<Object>();

            for (String org : newCEMap.get(cat).keySet()) {
                Map<String, Object> orgExpMap = new LinkedHashMap<String, Object>();

                List<Object> expList = new ArrayList<Object>();

                for (DisplayExperiment de : newCEMap.get(cat).get(org)) {
                    Map<String, Object> expDetailMap = new LinkedHashMap<String, Object>();
                    expDetailMap.put("experiment_name", de.getName());
                    expDetailMap.put("project_name", de.getProjectName());
                    expDetailMap.put("pi", de.getPi());
                    expDetailMap.put("labs", de.getLabs());
                    expDetailMap.put("submission_count", de.getSubmissionCount());
                    expDetailMap.put("factor_types", de.getFactorTypes());

                    // Features, could be null
                    Set<Object> featureSet = new LinkedHashSet<Object>();
                    if (de.getFeatureCountsRecords() != null) {
                        for (FeatureCountsRecord fcr : de.getFeatureCountsRecords()) {
                            Map<String, Object> featureMap = new LinkedHashMap<String, Object>();
                            featureMap.put("feature_type", fcr.getFeatureType());
                            featureMap.put("feature_counts", fcr.getFeatureCounts());
                            featureSet.add(featureMap);
                        }
                    }
                    expDetailMap.put("features", featureSet);

                    // GBrowse, could be null
                    Map<String, Object> gbrowseMap = new HashMap<String, Object>();
                    if (!gBrowseTracks.get(de.getName()).isEmpty()) {
                        gbrowseMap.put("track_counts", gBrowseTracks.get(de.getName()).size());
                        gbrowseMap.put("track_link",
                                generateGBrowseTrackLink(gBrowseTracks.get(de
                                        .getName())));
                    }
                    expDetailMap.put("gbrowse_tracks", gbrowseMap);

                    expList.add(expDetailMap);
                }
                orgExpMap.put("experiments", expList);
                orgExpMap.put("organism", org);
                orgList.add(orgExpMap);
            }
            catOrgMap.put("organisms", orgList);
            catOrgMap.put("category", cat);
            catList.add(catOrgMap);
        }

        catMap.put("categories", catList);
        JSONObject jo = new JSONObject(catMap);

        return jo.toString();
    }


    @SuppressWarnings("unused")
    private String testWebservice() {
        return "Resource available: /" + this.resourcePath.replaceAll("\\.", "/");
    }

    @SuppressWarnings("unused")
    /**
     * return webapp path, e.g. modminepreview
     */
    private String getWebappPath() {
        return SessionMethods.getWebProperties(
                request.getSession().getServletContext()).getProperty("webapp.path");
    }

    @SuppressWarnings("unused")
    private String getGBrowseBaseURL() {
        String gbrowseBaseUrl = null;

        gbrowseBaseUrl = GBrowseParser.getGBrowsePrefix();

        if (gbrowseBaseUrl == null || gbrowseBaseUrl.isEmpty()) {
            gbrowseBaseUrl = GBROWSE_DEFAULT_URL;
        }

        return gbrowseBaseUrl;
    }

    private String generateGBrowseTrackLink(List<GBrowseTrack> tracks) {
        String prefix = null;
        String org = tracks.get(0).getOrganism();
        if ("fly".equals(org)) {
            prefix = "fly/?label=";
        } else {
            prefix = "worm/?label=";
        }

        Set<String> trackSet = new LinkedHashSet<String>();
        for (GBrowseTrack gt : tracks) {
            String track = gt.getTrack();
            String subTrack = gt.getSubTrack();
            trackSet.add(track + "/" + subTrack);
        }
        return prefix + StringUtil.join(trackSet, "-");
    }
}
