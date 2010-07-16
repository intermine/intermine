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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
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
    private static String PASS = "pass";
    private static String  ERROR = "error";

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
        // String chr = ((SpanUploadForm) form).getChr();

        // >> 1.2 Get uploaded file
        // parse spans to an arraylist ... handle empty content and non-integer spans
        // BED format: "chrom chromStart chromEnd"
        // put in a Set to reduce duplication
        Set<Span> spanSet = new HashSet<Span>();
        // ...
        List<Span> spanList = new ArrayList(spanSet);
;

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        // Step 2 - Logic >>>>
        // >> 2.1 Query chromosome info for span validation purpose
        Map<String, List<ChromosomeInfo>> chrInfoMap = SpanOverlapQuery.runSpanValidationQuery(im);

        // >> 2.2 Validate the spans (parse and validate by AJAX???)
        String orgName = ((SpanUploadForm) form).getOrgName();
        Map<String, List<Span>> resultMap = SpanValidator.runSpanValidation(
                orgName, spanList, chrInfoMap);

        // store the error in the session and return it to the webpage
        // what if all spans are wrong or no error???
        String errorMsg = resultMap.get(SpanUploadAction.ERROR).toString();

        // >> 2.3 Query the overlapped features
        Map<Span, Results> spanOverlapResultMap = SpanOverlapQuery
                .runSpanOverlapQuery((SpanUploadForm) form, resultMap
                        .get(SpanUploadAction.PASS), im);

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
        session.setAttribute("errorMsg", errorMsg);
        session.setAttribute("results", spanOverlapResultMap);

        return mapping.findForward("spanUploadResults");
    }
}
