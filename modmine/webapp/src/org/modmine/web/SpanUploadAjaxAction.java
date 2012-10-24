package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.util.StringUtil;
import org.json.JSONObject;
import org.modmine.web.model.SpanQueryResultRow;
import org.modmine.web.model.SpanUploadConstraint;

/**
 * Span overlap query results will be loaded by calling this class via ajax.
 *
 * @author Fengyuan Hu
 */
public class SpanUploadAjaxAction extends Action
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SpanUploadAjaxAction.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        String spanUUIDString = (String) request.getParameter("spanUUIDString");

        @SuppressWarnings("unchecked")
        Map<String, Map<GenomicRegion, List<SpanQueryResultRow>>> spanOverlapFullResultMap =
             (Map<String, Map<GenomicRegion, List<SpanQueryResultRow>>>) request
                            .getSession().getAttribute("spanOverlapFullResultMap");

        @SuppressWarnings("unchecked")
        Map<SpanUploadConstraint, String> spanConstraintMap =
            (HashMap<SpanUploadConstraint, String>)  request
            .getSession().getAttribute("spanConstraintMap");

        @SuppressWarnings("unchecked")
        Map<String, Map<GenomicRegion, LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>>>>
        gbrowseFullTrackMap = (HashMap<String, Map<GenomicRegion, LinkedHashMap<String,
                LinkedHashSet<GBrowseTrackInfo>>>>) request.getSession()
                    .getAttribute("gbrowseFullTrackMap");

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        // An ajax call to request query progress
        if (request.getParameter("getProgress") != null) {
            out.println(spanOverlapFullResultMap.get(spanUUIDString).size());
        }

        // An ajax call to request result data
        if (request.getParameter("getData") != null
                && request.getParameter("fromIdx") != null
                && request.getParameter("toIdx") != null) {

            int fromIdx = Integer.parseInt((String) request.getParameter("fromIdx"));
            int toIdx = Integer.parseInt((String) request.getParameter("toIdx"));

            // get span list from spanConstraintMap in the session
            List<GenomicRegion> spanList = null;
            for (Entry<SpanUploadConstraint, String> e : spanConstraintMap.entrySet()) {
                if (e.getValue().equals(spanUUIDString)) {
                    spanList = e.getKey().getSpanList();
                }
            }

            String orgName = getSpanOrganism(spanUUIDString, spanConstraintMap);
            String js = convertResultMapToJSONString(
                    spanOverlapFullResultMap.get(spanUUIDString), spanList,
                    fromIdx, toIdx, gbrowseFullTrackMap.get(spanUUIDString),
                    orgName);

            out.println(js);
        }

        // Get span overlap feature pids by giving a span
        if (request.getParameter("getFeatures") != null
                && request.getParameter("spanString") != null) {

            String featurePids = getSpanOverlapFeatures(spanUUIDString,
                    request.getParameter("spanString"),
                    spanOverlapFullResultMap.get(spanUUIDString));

            out.println(featurePids);
        }

        // Check if any spans have features
        if (request.getParameter("isEmptyFeature") != null) {
            out.println(isEmptyFeature(spanOverlapFullResultMap.get(spanUUIDString)));
        }

        // search function
        if (request.getParameter("isSearch") != null
                && request.getParameter("spansToSearch") != null) {
            //TODO to be implemented:
            // 1.search should be enable after all queries finished
            // 2.parse spansToSearch to a list of Spans
            // 3.loop the result map to find matches
            // 4.create JSON string
            // 5.print out
        }

        out.flush();
        out.close();

        return null;
    }

    /**
     * Convert result map to JSON string.
     *
     * @param resultMap
     * @param spanList
     * @param offsetStart
     * @param offsetEnd
     * @return a String
     */
    private String convertResultMapToJSONString(
            Map<GenomicRegion, List<SpanQueryResultRow>> resultMap,
            List<GenomicRegion> spanList,
            int fromIdx,
            int toIdx,
            Map<GenomicRegion, LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>>> trackMap,
            String orgName) {

        Map<String, Object> jsonMap = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> resultArray = new ArrayList<Map<String, Object>>();

        List<GenomicRegion> subSpanList = spanList.subList(fromIdx, toIdx + 1);

        for (GenomicRegion s : subSpanList) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("span", s.getOriginalRegion());
            m.put("organism", orgName);

            List<Map<String, String>> featureMapList = new ArrayList<Map<String, String>>();

            if (resultMap.get(s) != null) {
                for (SpanQueryResultRow r : resultMap.get(s)) {
                    Map<String, String> featureMap = new LinkedHashMap<String, String>();

                    featureMap.put("featurePId", r.getFeaturePID());
                    featureMap.put("featureType", r.getFeatureClass());
                    featureMap.put("location", r.locationToString());
                    featureMap.put("machedBaseCount", getMatchedBaseCount(s, r));
                    featureMap.put("dccId", r.getSubDCCid());
                    featureMap.put("subTitle", r.getSubTitle());

                    featureMapList.add(featureMap);
                }
                m.put("features", featureMapList);
                m.put("gbrowseurl", getGbrowseTrackURL(trackMap, s));
            } else {
                m.put("features", null);
                m.put("gbrowseurl", null);
            }

            resultArray.add(m);
        }

        jsonMap.put("paginatedSpanResult", resultArray);
//        JSONArray ja = new JSONArray(resultArray);
        JSONObject jo = new JSONObject(jsonMap);
        String js = jo.toString();

        return js;
    }

    /**
     * Calculate the number of matched bases.
     *
     * @param s a span object
     * @param r a SpanQueryResultRow object
     * @return matched base count as String
     */
    private String getMatchedBaseCount(GenomicRegion s, SpanQueryResultRow r) {

        int spanStart = s.getStart();
        int spanEnd = s.getEnd();
        int featureStart = r.getStart();
        int featureEnd = r.getEnd();

        int matchedBaseCount = 0;

        if (featureStart <= spanStart && featureEnd >= spanStart && featureEnd <= spanEnd) {
            matchedBaseCount = featureEnd - spanStart + 1;
        }

        if (featureStart >= spanStart && featureStart <= spanEnd && featureEnd >= spanEnd) {
            matchedBaseCount = spanEnd - featureStart + 1;
        }

        if (featureStart >= spanStart && featureEnd <= spanEnd) {
            matchedBaseCount = featureEnd - featureStart + 1;
        }

        if (featureStart <= spanStart && featureEnd >= spanEnd) {
            matchedBaseCount = spanEnd - spanStart + 1;
        }

        return String.valueOf(matchedBaseCount);
    }

    /**
     * Get Gbrowse tracks of the overlapped features of a given span and form them to a URL string.
     *
     * @param trackMap
     * @param span
     * @return String
     */
    private String getGbrowseTrackURL(
            Map<GenomicRegion, LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>>> trackMap,
            GenomicRegion span) {

        Set<String> subURLSet = new LinkedHashSet<String>();

        LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>> m = trackMap.get(span);

        if (m != null && m.size() > 0) {
            for (LinkedHashSet<GBrowseTrackInfo> s : m.values()) {
                for (GBrowseTrackInfo g : s) {
                    subURLSet.add(g.getTrack() + "/" + g.getSubTrack());
                }
            }

            // TODO whether add flanks
            /*
            int spanLength = span.getEnd() - span.getStart();
            int flank = (int) Math.rint(spanLength * 0.1); // 10%
                // TODO overflow not tested
            int newStart = span.getStart() - flank; // newStart >= 0
            int newEnd = span.getEnd() + flank; // newEnd <= the length of the chr
            */

            StringBuffer sb = new StringBuffer();
            sb.append("start=" + span.getStart() + ";")
                .append("end=" + span.getEnd() + ";")
                .append("ref=" + span.getChr() + ";")
                .append("label=Genes;")
                .append("label=" + StringUtil.join(subURLSet, "-"));

            return sb.toString();
        }

        return null;
    }

    private String getSpanOrganism(String spanUUIDString,
            Map<SpanUploadConstraint, String> spanConstraintMap) {

        for (Entry<SpanUploadConstraint, String> e : spanConstraintMap.entrySet()) {
            if (e.getValue().equals(spanUUIDString)) {
                return e.getKey().getSpanOrgName();
            }
        }

        return null;
    }

    /**
     * Get a comma separated string of a span's overlap features.
     *
     * @param spanUUIDString
     * @param spanString
     * @param resultMap
     * @return String
     */
    private String getSpanOverlapFeatures(String spanUUIDString, String spanString,
            Map<GenomicRegion, List<SpanQueryResultRow>> resultMap) {

        Set<String> featureSet = new HashSet<String>();

        GenomicRegion spanToExport = new GenomicRegion();
        String[] temp = spanString.split(":");
        spanToExport.setChr(temp[0]);
        temp = temp[1].split("\\.\\.");
        spanToExport.setStart(Integer.parseInt(temp[0]));
        spanToExport.setEnd(Integer.parseInt(temp[1]));
        for (SpanQueryResultRow r : resultMap.get(spanToExport)) {
            featureSet.add(r.getFeaturePID());
        }

        return StringUtil.join(featureSet, ",");
    }

    /**
     * Check whether the results have empty features.
     *
     * @param resultMap
     * @return String
     */
    private String isEmptyFeature(Map<GenomicRegion, List<SpanQueryResultRow>> resultMap) {
        for (List<SpanQueryResultRow> l : resultMap.values()) {
            if (l != null) {
                return "hasFeature";
            }
        }
        return "emptyFeature";
    }
}
