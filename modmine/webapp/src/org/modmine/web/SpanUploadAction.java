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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
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
    private static String  ERROR = "error";

    private static final Logger LOG = Logger.getLogger(SpanUploadAction.class);

    /**
     * Method called when user has submitted search form.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        // Step 1 - Data preparation >>>>
        // >> 1.1 Parse all the fields of SpanUploadForm
        SpanUploadForm spanUploadForm = (SpanUploadForm) form;
        String orgName = spanUploadForm.getOrgName();
        FormFile formFile = spanUploadForm.getFormFile();
        // String chr = ((SpanUploadForm) form).getChr();

        // >> 1.2 Get uploaded file
        // parse spans to an arraylist ... handle empty content and non-integer spans
        // BED format: "chrom chromStart chromEnd" or "chr:start..end"
        Set<Span> spanSet = new LinkedHashSet<Span>();

        BufferedReader reader = null;


        // >>>>>>
        /*
         * FormFile used from Struts works a bit strangely.
         * 1. Although the file does't exist formFile.getInputStream() doesn't
         * throw FileNotFoundException.
         * 2. When user specified empty file path or very invalid file path,
         * like file path not starting at '/' then formFile.getFileName() returns empty string.
         */
//        if (formFile != null && formFile.getFileName() != null
//                && formFile.getFileName().length() > 0) {
//
//            String mimetype = formFile.getContentType();
//            if (!mimetype.equals("application/octet-stream") && !mimetype.startsWith("text")) {
//                recordError(new ActionMessage("bagBuild.notText", mimetype), request);
//                return mapping.findForward("bags");
//            }
//            if (formFile.getFileSize() == 0) {
//                recordError(new ActionMessage("bagBuild.noBagFileOrEmpty"), request);
//                return mapping.findForward("bags");
//            }
//            reader = new BufferedReader(new InputStreamReader(formFile.getInputStream()));
//        } else if (spanUploadForm.getText() != null && spanUploadForm.getText().length() != 0) {
//            String trimmedText = spanUploadForm.getText().trim();
//            if (trimmedText.length() == 0) {
//                recordError(new ActionMessage("bagBuild.noBagPaste"), request);
//                return mapping.findForward("bags");
//            }
//            reader = new BufferedReader(new StringReader(trimmedText));
//        } else {
//            recordError(new ActionMessage("bagBuild.noBagFile"), request);
//            return mapping.findForward("bags");
//        }
//
//        reader.mark(READ_AHEAD_CHARS);
//
//        char buf[] = new char[READ_AHEAD_CHARS];
//
//        int read = reader.read(buf, 0, READ_AHEAD_CHARS);
//
//        for (int i = 0; i < read; i++) {
//            if (buf[i] == 0) {
//                recordError(new ActionMessage("bagBuild.notText", "binary"), request);
//                return mapping.findForward("bags");
//            }
//        }
//
//        reader.reset();
//
//        String thisLine;
//        List<String> list = new ArrayList<String>();
//        int elementCount = 0;
//        while ((thisLine = reader.readLine()) != null) {
//            StrMatcher matcher = StrMatcher.charSetMatcher("\n\t, ");
//            StrTokenizer st = new StrTokenizer(thisLine, matcher, StrMatcher.doubleQuoteMatcher());
//            while (st.hasNext()) {
//                String token = st.nextToken();
//                list.add(token);
//                elementCount++;
//                if (elementCount > maxBagSize) {
//                    ActionMessage actionMessage = null;
//                    if (profile == null || profile.getUsername() == null) {
//                        actionMessage = new ActionMessage("bag.bigNotLoggedIn",
//                                                          new Integer(maxBagSize));
//                    } else {
//                        actionMessage = new ActionMessage("bag.tooBig", new Integer(maxBagSize));
//                    }
//                    recordError(actionMessage, request);
//
//                    return mapping.findForward("bags");
//                }
//            }
//        }
//
//        BagQueryResult bagQueryResult =
//            bagRunner.searchForBag(type, list, buildBagForm.getExtraFieldValue(), false);
//        session.setAttribute("bagQueryResult", bagQueryResult);
//        request.setAttribute("bagType", type);

        // <<<<<<

        String trimmedText = spanUploadForm.getText().trim();

        reader = new BufferedReader(new StringReader(trimmedText));
        String thisLine;
        while ((thisLine = reader.readLine()) != null) {
            Span aSpan = new Span();

            String[] spanItems = thisLine.split("\t");
            aSpan.setChr(spanItems[0]);
            aSpan.setStart(Integer.valueOf(spanItems[1]));
            aSpan.setEnd(Integer.valueOf(spanItems[2]));

            spanSet.add(aSpan);
        }
        List<Span> spanList = new ArrayList<Span>(spanSet);

        // Step 2 - Logic >>>>
        // >> 2.1 Query chromosome info for span validation purpose

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Map<String, List<ChromosomeInfo>> chrInfoMap = SpanOverlapQueryRunner
                .runSpanValidationQuery(im);

        // >> 2.2 Validate the spans (parse and validate by AJAX???)
//        Map<String, List<Span>> resultMap = SpanValidator.runSpanValidation(
//                orgName, spanList, chrInfoMap);

        // store the error in the session and return it to the webpage
        // what if all spans are wrong or no error???
//        String errorMsg = resultMap.get(SpanUploadAction.ERROR).toString();

        // >> 2.3 Query the overlapped features
        Map<String, Results> spanOverlapResultDisplayMap = SpanOverlapQueryRunner
//                .runSpanOverlapQuery(spanUploadForm, resultMap
//                        .get(SpanUploadAction.PASS), im);
                .runSpanOverlapQuery(spanUploadForm, spanList, im);

        //>>>>> for test use <<<<<
//        SpanUploadForm suform = new SpanUploadForm();
//        suform.setOrgName("Drosophila melanogaster");
//
//        Span span = new Span();
//        span.setChr("2L");
//        span.setStart(60000);
//        span.setEnd(70000);
//        List<Span> list = new ArrayList<Span>();
//        list.add(span);
//        Map<Span, Results> spanOverlapResultMap = SpanOverlapQuery
//        .runSpanOverlapQuery(suform, list, im);
//
//        session.setAttribute("resultsize", spanOverlapResultMap.get(span).size());
        //>>>>> for test use <<<<<

        // Step3 - Session data preparation
//        session.setAttribute("errorMsg", errorMsg);
        session.setAttribute("results", spanOverlapResultDisplayMap);

        return mapping.findForward("spanUploadResults");
    }
}
