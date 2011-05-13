package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
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
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.bio.web.logic.GenomicRegionSearchService;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.GenomicRegionSearchConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.http.TableExporterFactory;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.TableExportForm;

/**
 * Genomic region search ajax calls.
 *
 * @author Fengyuan Hu
 */
public class GenomicRegionSearchAjaxAction extends Action
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(GenomicRegionSearchAjaxAction.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        String spanUUIDString = request.getParameter("spanUUIDString");

        GenomicRegionSearchService grsService = GenomicRegionSearchUtil
        .getGenomicRegionSearchService(request);

        @SuppressWarnings("unchecked")
        Map<String, Map<GenomicRegion, List<List<String>>>> spanOverlapFullResultMap =
             (Map<String, Map<GenomicRegion, List<List<String>>>>) request
                            .getSession().getAttribute("spanOverlapFullResultMap");

        @SuppressWarnings("unchecked")
        Map<GenomicRegionSearchConstraint, String> spanConstraintMap =
            (HashMap<GenomicRegionSearchConstraint, String>)  request
            .getSession().getAttribute("spanConstraintMap");

        response.setContentType("text/plain");

        // An ajax call to request query progress
        if (request.getParameter("getProgress") != null) {
            PrintWriter out = response.getWriter();
            out.println(spanOverlapFullResultMap.get(spanUUIDString).size());

            out.flush();
            out.close();
        }

        // An ajax call to request result data
        if (request.getParameter("getData") != null
                && request.getParameter("fromIdx") != null
                && request.getParameter("toIdx") != null) {
            PrintWriter out = response.getWriter();

            int fromIdx = Integer.parseInt((String) request.getParameter("fromIdx"));
            int toIdx = Integer.parseInt((String) request.getParameter("toIdx"));

            // get span list from spanConstraintMap in the session
            List<GenomicRegion> spanList = null;
            for (Entry<GenomicRegionSearchConstraint, String> e : spanConstraintMap.entrySet()) {
                if (e.getValue().equals(spanUUIDString)) {
                    spanList = e.getKey().getSpanList();
                }
            }

            String orgName = grsService.getSpanOrganism(spanUUIDString, spanConstraintMap);
            String htmlStr = grsService.convertResultMapToHTML(
                    spanOverlapFullResultMap.get(spanUUIDString), spanList,
                    fromIdx, toIdx, request.getSession(),
                    orgName);

            out.println(htmlStr);

            out.flush();
            out.close();
        }

        // Get span overlap feature pids by giving a span
        if (request.getParameter("getFeatures") != null
                && request.getParameter("spanString") != null) {

            PrintWriter out = response.getWriter();

            String featurePids = grsService.getSpanOverlapFeatures(spanUUIDString,
                    request.getParameter("spanString"),
                    spanOverlapFullResultMap.get(spanUUIDString));

            out.println(featurePids);

            out.flush();
            out.close();
        }

        // Check if any spans have features
        if (request.getParameter("isEmptyFeature") != null) {
            PrintWriter out = response.getWriter();
            out.println(grsService.isEmptyFeature(spanOverlapFullResultMap.get(spanUUIDString)));
        }

        // Search function
        if (request.getParameter("isSearch") != null
                && request.getParameter("spansToSearch") != null) {
            //TODO to be implemented:
            // 1.search should be enable after all queries finished
            // 2.parse spansToSearch to a list of Spans
            // 3.loop the result map to find matches
            // 4.create JSON string
            // 5.print out
        }

        // Export features
        if (request.getParameter("exportFeatures") != null) {

            String criteria = request.getParameter("criteria"); // all or location
            String facet = request.getParameter("facet"); // "SequenceFeature" or any featureType
            String format = request.getParameter("format"); // TSV etc.

            boolean doGzip = false;

            Set<Integer> featureIdSet = new LinkedHashSet<Integer>();
            Map<GenomicRegion, List<List<String>>> featureMap = spanOverlapFullResultMap
                    .get(spanUUIDString);

            if ("all".equals(criteria)) {
                for (List<List<String>> l : featureMap.values()) {
                    if (l != null) {
                        for (List<String> r : l) {
                            featureIdSet.add(Integer.valueOf(r.get(0)));
                        }
                    }
                }

            } else {
                GenomicRegion spanToExport = new GenomicRegion(criteria);
                for (List<String> r : featureMap.get(spanToExport)) {
                    featureIdSet.add(Integer.valueOf(r.get(0)));
                }
            }

            PathQuery q = grsService.getExportFeaturesQuery(featureIdSet, facet);

            String organism = new String();
            for (Entry<GenomicRegionSearchConstraint, String> e : spanConstraintMap.entrySet()) {
                if (e.getValue().equals(spanUUIDString)) {
                    organism = e.getKey().getOrgName();
                }
            }

            Set<String> organisms = new HashSet<String>();
            organisms.add(organism);
            Set<Integer> taxIds = grsService.getTaxonIds(organisms);

            Profile profile = SessionMethods.getProfile(request.getSession());
            WebResultsExecutor executor = SessionMethods.getInterMineAPI(
                    request.getSession()).getWebResultsExecutor(profile);
            PagedTable pt = new PagedTable(executor.execute(q));

            if (pt.getWebTable() instanceof WebResults) {
                ((WebResults) pt.getWebTable()).goFaster();
            }

            WebConfig webConfig = SessionMethods.getWebConfig(request);
            TableExporterFactory factory = new TableExporterFactory(webConfig);

            TableHttpExporter exporter = factory.getExporter(format);

            if (exporter == null) {
                throw new RuntimeException("unknown export format: " + format);
            }

            TableExportForm exportForm = new TableExportForm();
            // Ref to StandardHttpExporter
            exportForm.setIncludeHeaders(true);

            if ("gff3".equals(format)) {
                exportForm = new GFF3ExportForm();
                exportForm.setDoGzip(doGzip);
                ((GFF3ExportForm) exportForm).setOrganisms(taxIds);
            }

            if ("sequence".equals(format)) {
                exportForm = new SequenceExportForm();
                exportForm.setDoGzip(doGzip);
            }

            exporter.export(pt, request, response, exportForm);

            return null;
        }

        return null;
    }
}
