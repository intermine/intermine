package org.modmine.webservice;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONObject;
import org.modmine.web.DisplayExperiment;
import org.modmine.web.MetadataCache;

/**
 * A modMine specific webservice for modENCODE home page data presentation purpose.
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
        tempMap.put("test", "testWebservice");
        tempMap.put("catexp.test", "testWebservice");
        RESOURCE_METHOD_MAP = Collections.unmodifiableMap(tempMap);
    }

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

        // Step One, create a new map: categories -> organsims -> experiments
        // TODO move this to MetadataCache
//        Map<String, List<DisplayExperiment>> ceFlyMap =
//            new LinkedHashMap<String, List<DisplayExperiment>>();
//
//        Map<String, List<DisplayExperiment>> ceWormMap =
//            new LinkedHashMap<String, List<DisplayExperiment>>();
//
//        for (String cat : ceMap.keySet()) {
//            List<DisplayExperiment> flyExpList = new ArrayList<DisplayExperiment>();
//            List<DisplayExperiment> wormExpList = new ArrayList<DisplayExperiment>();
//
//            for (DisplayExperiment de : ceMap.get(cat)) {
//                if (de.getOrganisms().contains("D. melanogaster")) {
//                    flyExpList.add(de);
//                }
//
//                if (de.getOrganisms().contains("C. elegans")) {
//                    wormExpList.add(de);
//                }
//            }
//
//            if (flyExpList.size() > 0) {
//                ceFlyMap.put(cat, flyExpList);
//            }
//
//            if (wormExpList.size() > 0) {
//                ceWormMap.put(cat, wormExpList);
//            }
//        }

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
//        Map<String, Object> orgMap = new LinkedHashMap<String, Object>();
//        List<Object> orgList = new ArrayList<Object>();
//
//        Map<String, Object> flyMap = new LinkedHashMap<String, Object>();
//        List<Object> flyList = new ArrayList<Object>();
//
//        for (String cat : ceFlyMap.keySet()) {
//            Map<String, Object> catExpMap = new LinkedHashMap<String, Object>();
//
//            List<Object> expList = new ArrayList<Object>();
//
//            for (DisplayExperiment de : ceFlyMap.get(cat)) {
//                Map<String, Object> expDetailMap = new LinkedHashMap<String, Object>();
//                expDetailMap.put("experiment_title", de.getName());
//                expDetailMap.put("pi", de.getPi());
//                expDetailMap.put("labs", de.getLabs());
//                // could add more information here
//
//                expList.add(expDetailMap);
//            }
//
//            catExpMap.put("category_title", cat);
//            catExpMap.put("experiments", expList);
//            flyList.add(catExpMap);
//        }
//        flyMap.put("categories", flyList);
//        flyMap.put("org_name", "D. melanogaster");
//
//        Map<String, Object> wormMap = new LinkedHashMap<String, Object>();
//        List<Object> wormList = new ArrayList<Object>();
//
//        for (String cat : ceWormMap.keySet()) {
//            Map<String, Object> catExpMap = new LinkedHashMap<String, Object>();
//
//            List<Object> expList = new ArrayList<Object>();
//
//            for (DisplayExperiment de : ceWormMap.get(cat)) {
//                Map<String, Object> expDetailMap = new LinkedHashMap<String, Object>();
//                expDetailMap.put("experiment_title", de.getName());
//                expDetailMap.put("pi", de.getPi());
//                expDetailMap.put("labs", de.getLabs());
//                // could add more information here
//
//                expList.add(expDetailMap);
//            }
//
//            catExpMap.put("category_title", cat);
//            catExpMap.put("experiments", expList);
//            wormList.add(catExpMap);
//        }
//        wormMap.put("categories", wormList);
//        wormMap.put("org_name", "C. elegans");
//
//        orgList.add(flyMap);
//        orgList.add(wormMap);
//
//        orgMap.put("organisms", orgList);
//
//        JSONObject jo = new JSONObject(orgMap);

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
                    expDetailMap.put("experiment_title", de.getName());
                    expDetailMap.put("pi", de.getPi());
                    expDetailMap.put("labs", de.getLabs());
                    // could add more information here

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
    private String getWebappPath() {
        String requestURI = request.getRequestURI();
        int index = nthOccurrence(requestURI, '/', 1);
        return requestURI.substring(0, index);
    }

    private static int nthOccurrence(String str, char c, int n) {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1) {
            pos = str.indexOf(c, pos + 1);
        }
        return pos;
    }
}
