package org.modmine.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;
import org.modmine.web.GBrowseParser.GBrowseTrack;

/**
 * @author Fengyuan Hu
 *
 */
public class SpanUploadAction extends InterMineAction
{
    private static final int READ_AHEAD_CHARS = 10000;

    private static final String PASS = "pass";
    private static final String ERROR = "error";

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SpanUploadAction.class);

    /**
     * Action for querying overlap experimental features against uploaded spans (genome regions)
     * from either a text area or a file in certain formats.
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        SpanUploadForm spanUploadForm = (SpanUploadForm) form;
        String orgName = spanUploadForm.getOrgName();
        String isInterBaseCoordinate = spanUploadForm.getIsInterBaseCoordinate();
        String whichInput = spanUploadForm.getWhichInput();
        FormFile formFile = spanUploadForm.getFormFile();

        // Parse experiments strings
        // Due to jsTree, the checkbox values regarding to experiments is in one string
        spanUploadForm.setExperiments(spanUploadForm.getExperiments()[0]
                .split(","));

        // Check if experiment or feature type is empty
        if (spanUploadForm.getExperiments()[0] == null
                || "".equals(spanUploadForm.getExperiments()[0])) {

            recordError(new ActionMessage("spanBuild.spanFieldSelection",
                    "experiments"), request);
            return mapping.findForward("spanUploadOptions");
        }

        if (spanUploadForm.getFeatureTypes() == null) {
            recordError(new ActionMessage("spanBuild.spanFieldSelection",
                    "feature types"), request);
            return mapping.findForward("spanUploadOptions");
        }

        // Get pasted text or uploaded file
        BufferedReader reader = null;

        /*
         * FormFile used from Struts works a bit strangely. 1. Although the file
         * does't exist formFile.getInputStream() doesn't throw
         * FileNotFoundException. 2. When user specified empty file path or very
         * invalid file path, like file path not starting at '/' then
         * formFile.getFileName() returns empty string.
         */
        if ("paste".equals(whichInput)) {
            if (spanUploadForm.getText() != null
                    && spanUploadForm.getText().length() != 0) {
                String trimmedText = spanUploadForm.getText().trim();
                if (trimmedText.length() == 0) {
                    recordError(new ActionMessage("spanBuild.noSpanPaste"),
                        request);
                    return mapping.findForward("spanUploadOptions");
                }
                reader = new BufferedReader(new StringReader(trimmedText));
            } else {
                recordError(new ActionMessage("spanBuild.noSpanFile"),
                    request);
                return mapping.findForward("spanUploadOptions");
            }

        } else if ("file".equals(whichInput)) {
            if (formFile != null && formFile.getFileName() != null
                    && formFile.getFileName().length() > 0) {

                String mimetype = formFile.getContentType();
                if (!"application/octet-stream".equals(mimetype)
                        && !mimetype.startsWith("text")) {
                    recordError(new ActionMessage("spanBuild.notText",
                        mimetype), request);
                    return mapping.findForward("spanUploadOptions");
                }
                if (formFile.getFileSize() == 0) {
                    recordError(new
                        ActionMessage("spanBuild.noSpanFileOrEmpty"), request);
                    return mapping.findForward("spanUploadOptions");
                }
                reader = new BufferedReader(new InputStreamReader(formFile
                        .getInputStream()));
            }
        } else {
            recordError(new ActionMessage("spanBuild.spanInputType"), request);
            return mapping.findForward("spanUploadOptions");
        }

        // Validate text format
        reader.mark(READ_AHEAD_CHARS);

        char buf[] = new char[READ_AHEAD_CHARS];

        int read = reader.read(buf, 0, READ_AHEAD_CHARS);

        for (int i = 0; i < read; i++) {
            if (buf[i] == 0) {
                recordError(new ActionMessage("spanBuild.notText", "binary"),
                    request);
                return mapping.findForward("spanUploadOptions");
            }
        }

        reader.reset();

        // Remove duplication
        Set<String> spanStringSet = new LinkedHashSet<String>();
        String thisLine;
        while ((thisLine = reader.readLine()) != null) {
            spanStringSet.add(thisLine);
        }

        // Parse uploaded spans to an arraylist; handle empty content and non-integer spans
        // Tab delimited format: "chr(tab)start(tab)end" or "chr:start..end"
        List<Span> spanList = new ArrayList<Span>();
        for (String spanStr : spanStringSet) {
            Span aSpan = new Span();
            // >>> Use regular expression to validate user's input
            // span in the form of "chr:start..end" as in a pattern
            // [^:]+:\d+\.{2,}\d+
            // span in the form of "chr:start-end" as in a pattern
            // [^:]+:\d+\-\d+
            // span in the form of "chr(tab)start(tab)end" as in a pattern
            // [^\t]+\t\d+\t\d+
            String ddotsRegex = "[^:]+:\\d+\\.\\.\\d+";
            String tabRegex = "[^\\t]+\\t\\d+\\t\\d+";
            String dashRegex = "[^:]+:\\d+\\-\\d+";

            if (Pattern.matches(ddotsRegex, spanStr)) {
                aSpan.setChr((spanStr.split(":"))[0]);
                String[] spanItems = (spanStr.split(":"))[1].split("\\..");
                if ("isInterBaseCoordinate".equals(isInterBaseCoordinate)) {
                    aSpan.setStart(Integer.valueOf(spanItems[0]) + 1);
                } else {
                    aSpan.setStart(Integer.valueOf(spanItems[0]));
                }
                aSpan.setEnd(Integer.valueOf(spanItems[1]));
            } else if (Pattern.matches(tabRegex, spanStr)) {
                String[] spanItems = spanStr.split("\t");
                aSpan.setChr(spanItems[0]);
                if ("isInterBaseCoordinate".equals(isInterBaseCoordinate)) {
                    aSpan.setStart(Integer.valueOf(spanItems[1]) + 1);
                } else {
                    aSpan.setStart(Integer.valueOf(spanItems[1]));
                }
                aSpan.setEnd(Integer.valueOf(spanItems[2]));
            } else if (Pattern.matches(dashRegex, spanStr)) {
                aSpan.setChr((spanStr.split(":"))[0]);
                String[] spanItems = (spanStr.split(":"))[1].split("-");
                if ("isInterBaseCoordinate".equals(isInterBaseCoordinate)) {
                    aSpan.setStart(Integer.valueOf(spanItems[0]) + 1);
                } else {
                    aSpan.setStart(Integer.valueOf(spanItems[0]));
                }
                aSpan.setEnd(Integer.valueOf(spanItems[1]));
            } else {
                recordError(new ActionMessage("spanBuild.spanInWrongformat",
                        spanStr), request);
                return mapping.findForward("spanUploadOptions");
            }
            spanList.add(aSpan);
        }

        // Query chromosome info to validate span
        Map<String, List<ChromosomeInfo>> chrInfoMap = SpanOverlapQueryRunner
                .runSpanValidationQuery(im);

        // Chromesome starts with "chr" - UCSC formats
        for (Span aSpan : spanList) {
            if (aSpan.getChr().startsWith("chr")) {
                aSpan.setChr(aSpan.getChr().substring(3));
            }
        }

        Map<Span, List<SpanQueryResultRow>> spanOverlapResultMap =
            new LinkedHashMap<Span, List<SpanQueryResultRow>>();
        if (chrInfoMap == null || chrInfoMap.size() < 1) {
            // Query the overlapped features
            spanOverlapResultMap = SpanOverlapQueryRunner
             .runSpanOverlapQuery(spanUploadForm, spanList, im);

        } else {
            // Validate spans
            Map<String, List<Span>> resultMap = SpanValidator.runSpanValidation(
                    orgName, spanList, chrInfoMap);

            // store the error in the session and return it to the webpage
            // what if all spans are wrong or no error???
            String errorMsg = "";
            if (resultMap.get(SpanUploadAction.ERROR).size() == 0) {
                errorMsg = null;
            } else {
                String spanString = "";
                for (Span span : resultMap.get(SpanUploadAction.ERROR)) {
                    spanString = spanString + span.getChr() + ":" + span.getStart()
                            + ".." + span.getEnd() + ",";
                }
                errorMsg = "Invalid spans: " + spanString.substring(0, spanString.lastIndexOf(","));
            }

            // Query the overlapped features
            spanOverlapResultMap = SpanOverlapQueryRunner
                    .runSpanOverlapQuery(spanUploadForm, resultMap
                            .get(SpanUploadAction.PASS), im);

            request.setAttribute("errorMsg", errorMsg);
        }

        // ------------ GBrowse tracks ------------ //
        // first key - Span; second key - DCCid
        LinkedHashMap<Span, LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>>> track =
            new LinkedHashMap<Span, LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>>>();
        for (Map.Entry<Span, List<SpanQueryResultRow>> entry : spanOverlapResultMap
                .entrySet()) {
            LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>> subGTrack =
                new LinkedHashMap<String, LinkedHashSet<GBrowseTrackInfo>>();
            if (entry.getValue() != null) {
                for (SpanQueryResultRow aRow : entry.getValue()) {
                    if (MetadataCache.getTracksByDccId(aRow.getSubDCCid()).size() > 0) {
                        List<GBrowseTrack> trackList =
                            MetadataCache.getTracksByDccId(aRow.getSubDCCid());
                        LinkedHashSet<GBrowseTrackInfo> trackInfoList =
                            new LinkedHashSet<GBrowseTrackInfo>();
                        for (GBrowseTrack aTrack : trackList) {
                            GBrowseTrackInfo aTrackInfo = new GBrowseTrackInfo(
                                    aTrack.getOrganism(), aTrack.getTrack(),
                                    aTrack.getSubTrack(), aTrack.getDCCid());
                            trackInfoList.add(aTrackInfo);
                        }
                        subGTrack.put(aRow.getSubDCCid(), trackInfoList);
                    }
                }
                track.put(entry.getKey(), subGTrack);
            }
        }
        request.setAttribute("spanTrackMap", track);

        String gbrowseDefaultUrl = "http://modencode.oicr.on.ca/cgi-bin/gb2/gbrowse/";
        String gbrowseBaseUrl = GBrowseParser.getGBrowsePrefix();

        if (gbrowseBaseUrl == null || gbrowseBaseUrl.isEmpty()) {
            request.setAttribute("GBROWSE_BASE_URL", gbrowseDefaultUrl);
        } else {
            request.setAttribute("GBROWSE_BASE_URL", gbrowseBaseUrl);
        }
        // ------------ GBrowse tracks ------------ //

        String expString = "";
        for (String aExperiment : spanUploadForm.getExperiments()) {
            expString = expString + aExperiment + ", ";
        }
        request.setAttribute("selectedExp", expString.substring(0, expString.lastIndexOf(", ")));
        String ftString = "";
        for (String aFeaturetype : spanUploadForm.getFeatureTypes()) {
            ftString = ftString + aFeaturetype + ", ";
        }
        request.setAttribute("selectedFt", ftString.substring(0, ftString.lastIndexOf(", ")));

//        request.setAttribute("spanOverlapResultMap", spanOverlapResultMap);

        //----- Extra information about spans without overlap features -----//
        StringBuffer strBuf = new StringBuffer();
        // Save entries with value in a new map called spanOverlapResultDisplayMap
        Map<Span, List<SpanQueryResultRow>> spanOverlapResultDisplayMap =
            new LinkedHashMap<Span, List<SpanQueryResultRow>>();
        for (Map.Entry<Span, List<SpanQueryResultRow>> entry : spanOverlapResultMap
                .entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                strBuf.append(entry.getKey().toString());
                strBuf.append(", ");
            } else {
                spanOverlapResultDisplayMap.put(entry.getKey(), entry.getValue());
            }
        }

        if (strBuf.length() > 1) {
            String spanWithNoFtStr = "No overlap features found for "
                    + strBuf.delete(strBuf.lastIndexOf(","),
                            strBuf.length() - 1).toString();
            request.setAttribute("spanWithNoFt", spanWithNoFtStr);
        }
        else {
            request.setAttribute("spanWithNoFt", null);
        }

        request.setAttribute("spanOverlapResultDisplayMap", spanOverlapResultDisplayMap);
       //----- Extra information about spans without overlap features -----//

        request.setAttribute("spanOrg", orgName);

        // Set <forward name="spanUploadResults" path="/spanUploadResults.do" redirect="false"/>
        // instead of redirect="true"; here it is doing a forward, not redirecting. Redirecting
        // causes the browser to make a new request, and that's why the things I put in the
        // request aren't there anymore.
        return mapping.findForward("spanUploadResults");
    }
}
