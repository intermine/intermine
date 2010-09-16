package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import org.intermine.objectstore.query.Results;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;

/**
 * @author Fengyuan Hu
 *
 */
public class SpanUploadAction extends InterMineAction
{
    private static final int READ_AHEAD_CHARS = 10000;

    private static String PASS = "pass";
    private static String ERROR = "error";

    private static final Logger LOG = Logger.getLogger(SpanUploadAction.class);

    /**
     * Action for querying overlap experimental features against uploaded spans
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

        // > Step 1 - Data preparation >>>>>
        // >> 1.1 Parse all the fields of SpanUploadForm
        SpanUploadForm spanUploadForm = (SpanUploadForm) form;
        String orgName = spanUploadForm.getOrgName();
        String whichInput = spanUploadForm.getWhichInput();
        FormFile formFile = spanUploadForm.getFormFile();

        // >>> Parse experiments strings
        // Due to jsTree, the checkbox values regarding to experiments is in one
        // string
        spanUploadForm.setExperiments(spanUploadForm.getExperiments()[0]
                .split(","));

        // >>> Check if experiment or feature type is empty
        if (spanUploadForm.getExperiments()[0].equals("")
                || spanUploadForm.getExperiments()[0] == null) {

            // TODO message is not shown in the spanUploadOptions page?
            recordError(new ActionMessage("spanBuild.spanFieldSelection",
                    "experiments"), request);
            return mapping.findForward("spanUploadOptions");
        }

        if (spanUploadForm.getFeatureTypes() == null) {
            recordError(new ActionMessage("spanBuild.spanFieldSelection",
                    "feature types"), request);
            return mapping.findForward("spanUploadOptions");
        }

        // >> 1.2 Get pasted text or uploaded file
        BufferedReader reader = null;

        /*
         * FormFile used from Struts works a bit strangely. 1. Although the file
         * does't exist formFile.getInputStream() doesn't throw
         * FileNotFoundException. 2. When user specified empty file path or very
         * invalid file path, like file path not starting at '/' then
         * formFile.getFileName() returns empty string.
         */
        if (whichInput.equals("paste")) {
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

        } else if (whichInput.equals("file")) {
            if (formFile != null && formFile.getFileName() != null
                    && formFile.getFileName().length() > 0) {

                String mimetype = formFile.getContentType();
                if (!mimetype.equals("application/octet-stream")
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

        // >>> Validate text format
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
        // <<<

        // >>> Parse uploaded spans to an arraylist; handle empty content and
        // non-integer spans
        // BED format: "chr(tab)start(tab)end" or "chr:start..end"
        // TODO is there a maxSize for spans?
        Set<Span> spanSet = new LinkedHashSet<Span>();
        String thisLine;
        while ((thisLine = reader.readLine()) != null) {
            Span aSpan = new Span();
            // >>> Use regular expression to validate user's input
            // span in the form of "chr:start..end" as in a pattern
            // [^:]+:\d+\.{2,}\d+
            // span in the form of "chr:start-end" as in a pattern
            // [^:]+:\d+\-\d+
            // span in the form of "chr(tab)start(tab)end" as in a pattern
            // [^\t]+\t\d+\t\d+
            String tabRegex = "[^:]+:\\d+\\.\\.\\d+";
            String bedRegex = "[^\\t]+\\t\\d+\\t\\d+";
            String dashRegex = "[^:]+:\\d+\\-\\d+";

            if (Pattern.matches(tabRegex, thisLine)) {
                aSpan.setChr((thisLine.split(":"))[0]);
                String[] spanItems = (thisLine.split(":"))[1].split("\\..");
                aSpan.setStart(Integer.valueOf(spanItems[0]));
                aSpan.setEnd(Integer.valueOf(spanItems[1]));
            } else if (Pattern.matches(bedRegex, thisLine)) {
                String[] spanItems = thisLine.split("\t");
                aSpan.setChr(spanItems[0]);
                aSpan.setStart(Integer.valueOf(spanItems[1]));
                aSpan.setEnd(Integer.valueOf(spanItems[2]));
            } else if (Pattern.matches(dashRegex, thisLine)) {
                aSpan.setChr((thisLine.split(":"))[0]);
                String[] spanItems = (thisLine.split(":"))[1].split("-");
                aSpan.setStart(Integer.valueOf(spanItems[0]));
                aSpan.setEnd(Integer.valueOf(spanItems[1]));
            } else {
                // redirect to the option page with error msg
                recordError(new ActionMessage("spanBuild.spanInWrongformat",
                        thisLine), request);
                return mapping.findForward("spanUploadOptions");
            }
            spanSet.add(aSpan);
        }
        List<Span> spanList = new ArrayList<Span>(spanSet);

        // ******
        // Refer to code from BuildBagAction.java
        // ******

        // > Step 2 - Logic >>>>>
        // >> 2.1 Query chromosome info for span validation purpose

        final InterMineAPI im = SessionMethods.getInterMineAPI(session);


        Map<String, List<ChromosomeInfo>> chrInfoMap = SpanOverlapQueryRunner
                .runSpanValidationQuery(im);

        Map<Span, Results> spanOverlapResultDisplayMap = new LinkedHashMap<Span, Results>();
        if (chrInfoMap.isEmpty()) {
            // >> 2.3 Query the overlapped features
            spanOverlapResultDisplayMap = SpanOverlapQueryRunner
             .runSpanOverlapQuery(spanUploadForm, spanList, im);

        } else {

            // >> 2.2 Validate the spans (parse and validate by AJAX???)
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

            // >> 2.3 Query the overlapped features
            spanOverlapResultDisplayMap = SpanOverlapQueryRunner
                    .runSpanOverlapQuery(spanUploadForm, resultMap
                            .get(SpanUploadAction.PASS), im);

            session.setAttribute("errorMsg", errorMsg);
        }


        // > Step3 - Session data preparation >>>>>
        String expString = "";
        for (String aExperiment : spanUploadForm.getExperiments()) {
            expString = expString + aExperiment + ",";
        }
        session.setAttribute("selectedExp", "Selected experiments: "
                + expString.substring(0, expString.lastIndexOf(",")));
        String ftString = "";
        for (String aFeaturetype : spanUploadForm.getFeatureTypes()) {
            ftString = ftString + aFeaturetype + ",";
        }
        session.setAttribute("selectedFt", "Selected feature types: "
                + ftString.substring(0, ftString.lastIndexOf(",")));

        session.setAttribute("results", spanOverlapResultDisplayMap);

        return mapping.findForward("spanUploadResults");
    }
}
