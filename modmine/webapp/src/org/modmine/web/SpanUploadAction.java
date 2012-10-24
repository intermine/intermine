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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;
import org.modmine.web.logic.SpanOverlapQueryRunner;
import org.modmine.web.logic.SpanValidator;

/**
 * Action for querying overlap experimental features against uploaded spans (genome regions)
 * from either a text area or a file in certain formats.
 *
 * @author Fengyuan Hu
 *
 */
public class SpanUploadAction extends InterMineAction
{
    private static final int READ_AHEAD_CHARS = 10000;

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SpanUploadAction.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();

        SpanUploadForm spanUploadForm = (SpanUploadForm) form;
        String orgName = spanUploadForm.getOrgName();

        if ("".equals(orgName)) {
            return mapping.findForward("spanUploadOptions");
        }

        String isInterBaseCoordinate = spanUploadForm.getIsInterBaseCoordinate();
        String whichInput = spanUploadForm.getWhichInput();
        FormFile formFile = spanUploadForm.getFormFile();

        if (spanUploadForm.getFeatureTypes() == null) {
            recordError(new ActionMessage("spanBuild.spanFieldSelection",
                    "feature types"), request);
            return mapping.findForward("spanUploadOptions");
        }

        List<String> subKeys;
        if ("facetedSearch".equals(spanUploadForm.getSource())) {
            subKeys = Arrays.asList(StringUtil.split(spanUploadForm.getSubmissions(), ","));
        } else {
            // Check if experiment or feature type is empty
            if (spanUploadForm.getExperiments()[0] == null
                    || "".equals(spanUploadForm.getExperiments()[0])) {

                recordError(new ActionMessage("spanBuild.spanFieldSelection",
                        "experiments"), request);
                return mapping.findForward("spanUploadOptions");
            }

            // Parse experiments strings
            // Due to jsTree, the checkbox values regarding to experiments is in one string
            spanUploadForm.setExperiments(spanUploadForm.getExperiments()[0]
                    .split(","));

            // Get submission dcc ids of selected experiments
            Set<String> dCCIdSet = new HashSet<String>();
            Map<String, Set<String>> expSubIdMap = MetadataCache.getExperimentSubmissionDCCids(os);
            for (String s : spanUploadForm.getExperiments()) {
                dCCIdSet.addAll(expSubIdMap.get(s));
            }
            subKeys = new ArrayList<String>(dCCIdSet);
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

        char[] buf = new char[READ_AHEAD_CHARS];

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
        List<GenomicRegion> spanList = new ArrayList<GenomicRegion>();
        for (String spanStr : spanStringSet) {
            GenomicRegion aSpan = new GenomicRegion();
            // >>> Use regular expression to validate user's input
            // "chr:start..end" - [^:]+:\d+\.{2,}\d+
            // "chr:start-end" - [^:]+:\d+\-\d+
            // "chr(tab)start(tab)end" - [^\t]+\t\d+\t\d+
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
        Map<String, List<ChromosomeInfo>> chrInfoMap = SpanOverlapQueryRunner.getChrInfo(im);

        // Chromesome starts with "chr" - UCSC formats
        for (GenomicRegion aSpan : spanList) {
            if (aSpan.getChr().startsWith("chr")) {
                aSpan.setChr(aSpan.getChr().substring(3));
            }
        }

        // Create a UUID and set in request
        String spanUUIDString = UUID.randomUUID().toString();
        request.setAttribute("spanUUIDString", spanUUIDString);

        // featureTypes in this case are (the last bit of) class instead of featuretype in the db
        // table; gain the full name by Model.getQualifiedTypeName(className)
        @SuppressWarnings("rawtypes")
        List<Class> ftKeys = new ArrayList<Class>();
        String modelPackName = im.getModel().getPackageName();
        for (String aClass : spanUploadForm.getFeatureTypes()) {
            ftKeys.add(Class.forName(modelPackName + "." + aClass));
        }

        if (chrInfoMap == null || chrInfoMap.size() < 1) {
            SpanOverlapQueryRunner queryRunner = new SpanOverlapQueryRunner(
                    spanUUIDString, spanList, ftKeys, subKeys, orgName, im,
                    request);

            queryRunner.runSpanOverlapQuery(); // Query the overlapped features

        } else { // Validate spans
            SpanValidator v = new SpanValidator();
            Map<String, List<GenomicRegion>> resultMap = v.runSpanValidation(
                    orgName, spanList, chrInfoMap);

            String errorMsg = "";
            if (resultMap.get("error").size() == 0) {
                errorMsg = null;
            } else {
                String spanString = "";
                for (GenomicRegion span : resultMap.get("error")) {
                    spanString = spanString + span.getChr() + ":" + span.getStart()
                            + ".." + span.getEnd() + ", ";
                }
                errorMsg = "<b>Invalid spans in <i>" + orgName + "</i>:</b> "
                        + spanString.substring(0, spanString.lastIndexOf(","));
            }

            if (resultMap.get("error").size() == spanList.size()) { // all spans are wrong
                request.setAttribute("SpanAllWrong", "true");
            } else {
                SpanOverlapQueryRunner queryRunner = new SpanOverlapQueryRunner(
                        spanUUIDString, resultMap.get("pass"),
                        ftKeys, subKeys, orgName, im, request);

                queryRunner.runSpanOverlapQuery(); // Query the overlapped features

                // GBrowse tracks
                if (request.getSession().getAttribute("GBROWSE_BASE_URL") == null) {
                    String gbrowseDefaultUrl = "http://modencode.oicr.on.ca/cgi-bin/gb2/gbrowse/";
                    String gbrowseBaseUrl = GBrowseParser.getGBrowsePrefix();

                    String gbrowseImageDefaultUrl =
                        "http://modencode.oicr.on.ca/cgi-bin/gb2/gbrowse_img/";
                    String gbrowseImageBaseUrl = gbrowseBaseUrl.replaceFirst(
                            "gbrowse", "gbrowse_img");

                    if (gbrowseBaseUrl == null || gbrowseBaseUrl.isEmpty()) {
                        request.getSession().setAttribute("GBROWSE_BASE_URL", gbrowseDefaultUrl);
                        request.getSession().setAttribute("GBROWSE_IMAGE_URL",
                                gbrowseImageDefaultUrl);
                    } else {
                        request.getSession().setAttribute("GBROWSE_BASE_URL", gbrowseBaseUrl);
                        request.getSession().setAttribute("GBROWSE_IMAGE_URL", gbrowseImageBaseUrl);
                    }
                }
            }

            request.setAttribute("errorMsg", errorMsg);
        }

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

        return mapping.findForward("spanUploadResults");
    }
}
