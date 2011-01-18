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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.modmine.web.logic.PaginationUtil;

/**
 * SpanUploadResultsController is called immediately before the spanUploadResults.jsp is inserted.
 * Do pagination here.
 *
 * @author Fengyuan Hu
 */

public class SpanUploadResultsController extends TilesAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SpanUploadResultsController.class);

    private static final int DEFAULT_PAGESIZE = 10;
    private static final int DEFAULT_PAGENUM = 1;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();

        // From SpanDisplayAction
        if (request.getAttribute("fromSpanDisplayAction") != null
                && "true".equals((String) request
                        .getAttribute("fromSpanDisplayAction"))) {

            LinkedHashMap<Span, List<SpanQueryResultRow>> spanOverlapResultDisplayMap;
            try {
                long uniqueId = Long.valueOf((String) request.getAttribute("dataId"));
                request.setAttribute("dataId", uniqueId);

                // Find all the data set in session with unique id at tail and pass them to jsp
                spanOverlapResultDisplayMap = (LinkedHashMap<Span, List<SpanQueryResultRow>>)
                    session.getAttribute("spanOverlapResultDisplayMap_" + uniqueId);
                // TODO wrong id, error message to JSP?
                if (spanOverlapResultDisplayMap == null) { return null; }
                request.setAttribute("spanTrackMap",
                        session.getAttribute("spanTrackMap_" + uniqueId));
                request.setAttribute("errorMsg",
                        session.getAttribute("errorMsg_" + uniqueId));
                request.setAttribute("GBROWSE_BASE_URL",
                        session.getAttribute("GBROWSE_BASE_URL_" + uniqueId));
                request.setAttribute("selectedExp",
                        session.getAttribute("selectedExp_" + uniqueId));
                request.setAttribute("selectedFt",
                        session.getAttribute("selectedFt_" + uniqueId));
                request.setAttribute("spanWithNoFt",
                        session.getAttribute("spanWithNoFt_" + uniqueId));
                request.setAttribute("spanOrg",
                        session.getAttribute("spanOrg_" + uniqueId));
            } catch (Exception e1) {
                e1.printStackTrace();
                return null;
            }

            int displayRecordCount; // page size
            int currentPageNum; // page
            String method;
            LinkedHashMap<Span, List<SpanQueryResultRow>> paginatedResultsMap = null;

            try {
                currentPageNum = Integer.valueOf((String) request.getAttribute("page"));
                request.setAttribute("currentPage", currentPageNum);
            } catch (Exception e) {
                e.printStackTrace();
                currentPageNum = DEFAULT_PAGENUM;
                request.setAttribute("currentPage", currentPageNum);
            }

            try {
                displayRecordCount = Integer.valueOf((String) request.getAttribute("pageSize"));
                request.setAttribute("currentPageSize", displayRecordCount);
            } catch (Exception e) {
                e.printStackTrace();
                displayRecordCount = DEFAULT_PAGESIZE;
                request.setAttribute("currentPageSize", displayRecordCount);
            }

            if (request.getAttribute("method") != null) {
                method = (String) request.getAttribute("method");

                if (!"first".equals(method) && !"last".equals(method)) {
                    request.setAttribute("paginatedResultsMap", null);
                } else {
                    if ("first".equals(method)) {
                        request.setAttribute("currentPage", 1);
                    } else if ("last".equals(method)) {
                        request.setAttribute(
                                "currentPage",
                                getTotalPage(
                                        spanOverlapResultDisplayMap.size(),
                                        displayRecordCount));
                    }
                    paginatedResultsMap = paginate(spanOverlapResultDisplayMap,
                            displayRecordCount, method);
                }
            } else {
                paginatedResultsMap = paginate(
                        spanOverlapResultDisplayMap, currentPageNum, displayRecordCount);
            }

            request.setAttribute("paginatedResultsMap", paginatedResultsMap);
            request.setAttribute("totalRecord", spanOverlapResultDisplayMap.size());
            request.setAttribute(
                    "totalPage",
                    getTotalPage(spanOverlapResultDisplayMap.size(),
                            displayRecordCount));

            return null;
        }

        //-------------- initial pagination --------------
        // From spanUploadAction
        LinkedHashMap<Span, List<SpanQueryResultRow>> spanOverlapResultDisplayMap =
                (LinkedHashMap<Span, List<SpanQueryResultRow>>) request
                                .getAttribute("spanOverlapResultDisplayMap");
        LinkedHashMap<Span, LinkedHashMap<Integer, LinkedHashSet<GBrowseTrackInfo>>>
        spanTrackMap =
            (LinkedHashMap<Span, LinkedHashMap<Integer, LinkedHashSet<GBrowseTrackInfo>>>)
            request.getAttribute("spanTrackMap");
        String errorMsg = (String) request.getAttribute("errorMsg");
        String gbrowseBaseUrl = (String) request.getAttribute("GBROWSE_BASE_URL");
        String selectedExp = (String) request.getAttribute("selectedExp");
        String selectedFt = (String) request.getAttribute("selectedFt");
        String spanWithNoFt = (String) request.getAttribute("spanWithNoFt");
        String spanOrg = (String) request.getAttribute("spanOrg");

        long uniqueTail = System.currentTimeMillis();

        request.setAttribute("dataId", uniqueTail);
        session.setAttribute("spanOverlapResultDisplayMap_" + uniqueTail,
                spanOverlapResultDisplayMap);
        session.setAttribute("spanTrackMap_" + uniqueTail, spanTrackMap);
        session.setAttribute("errorMsg_" + uniqueTail, errorMsg);
        session.setAttribute("GBROWSE_BASE_URL_" + uniqueTail, gbrowseBaseUrl);
        session.setAttribute("selectedExp_" + uniqueTail, selectedExp);
        session.setAttribute("selectedFt_" + uniqueTail, selectedFt);
        session.setAttribute("spanWithNoFt_" + uniqueTail, spanWithNoFt);
        session.setAttribute("spanOrg_" + uniqueTail, spanOrg);

        int currentPageNum = DEFAULT_PAGENUM;
        int displayRecordCount = DEFAULT_PAGESIZE;

        LinkedHashMap<Span, List<SpanQueryResultRow>> paginatedResultsMap = paginate(
                spanOverlapResultDisplayMap, currentPageNum, displayRecordCount);

        request.setAttribute("paginatedResultsMap", paginatedResultsMap);
        request.setAttribute("currentPage", currentPageNum);
        request.setAttribute("currentPageSize", displayRecordCount);
        request.setAttribute(
                "totalPage",
                getTotalPage(spanOverlapResultDisplayMap.size(),
                        displayRecordCount));
        request.setAttribute("totalRecord", spanOverlapResultDisplayMap.size());

        //-------------- initial pagination --------------

        return null;
    }

    /**
     * Pagination logics
     * url has "currentPageNum" and/or "displayRecordCount"
     *
     * @param spanOverlapResultDisplayMap
     * @param currentPageNum
     * @param displayRecordCount
     * @return paginatedResultsMap
     */
    private LinkedHashMap<Span, List<SpanQueryResultRow>> paginate(
            LinkedHashMap<Span, List<SpanQueryResultRow>> spanOverlapResultDisplayMap,
            int currentPageNum, int displayRecordCount) {
        LinkedHashMap<Span, List<SpanQueryResultRow>> paginatedResultsMap =
            new LinkedHashMap<Span, List<SpanQueryResultRow>>();

        List<Entry<Span, List<SpanQueryResultRow>>> spanOverlapResultList =
            new ArrayList<Entry<Span, List<SpanQueryResultRow>>>(
                 spanOverlapResultDisplayMap.entrySet());

        PaginationUtil aPager = new PaginationUtil(currentPageNum, displayRecordCount, null);

        @SuppressWarnings("unchecked")
        List<Entry<Span, List<SpanQueryResultRow>>> paginatedResultsList =
            (List<Entry<Span, List<SpanQueryResultRow>>>)
            aPager.doPagination(spanOverlapResultList);

        if (paginatedResultsList == null) { return null; }

        for (Entry<Span, List<SpanQueryResultRow>> entry : paginatedResultsList) {
            paginatedResultsMap.put(entry.getKey(), entry.getValue());
        }

        return paginatedResultsMap;
    }

    /**
     * Pagination logics
     * url has "method"
     *
     * @param spanOverlapResultDisplayMap
     * @param lastDisplayRecordCount last page size
     * @param method first/last
     * @return paginatedResultsMap
     */
    private LinkedHashMap<Span, List<SpanQueryResultRow>> paginate(
            LinkedHashMap<Span, List<SpanQueryResultRow>> spanOverlapResultDisplayMap,
            int displayRecordCount, String method) {
        LinkedHashMap<Span, List<SpanQueryResultRow>> paginatedResultsMap =
            new LinkedHashMap<Span, List<SpanQueryResultRow>>();

        List<Entry<Span, List<SpanQueryResultRow>>> spanOverlapResultList =
            new ArrayList<Entry<Span, List<SpanQueryResultRow>>>(
                 spanOverlapResultDisplayMap.entrySet());
        PaginationUtil aPager = new PaginationUtil(0, displayRecordCount, method);

        @SuppressWarnings("unchecked")
        List<Entry<Span, List<SpanQueryResultRow>>> paginatedResultsList =
            (List<Entry<Span, List<SpanQueryResultRow>>>)
            aPager.doPagination(spanOverlapResultList);

//        LOG.info("paginatedResultsList >>>>> " + paginatedResultsList);

        if (paginatedResultsList == null) { return null; }

        for (Entry<Span, List<SpanQueryResultRow>> entry : paginatedResultsList) {
            paginatedResultsMap.put(entry.getKey(), entry.getValue());
        }

        return paginatedResultsMap;
    }

    /**
     * Get the total number of pages
     * @return totalPage
     */
    private int getTotalPage(int recordSize, int displayRecordCount) {

        int totalPage = recordSize / displayRecordCount;
        if (recordSize % displayRecordCount != 0) { totalPage++; }

        return totalPage;
    }
}
