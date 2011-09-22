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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.util.NameUtil;
import org.intermine.bio.web.logic.GenomicRegionSearchService;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.GenomicRegionSearchConstraint;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
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

    private String spanUUIDString;
    private GenomicRegionSearchService grsService;
    private Map<String, Map<GenomicRegion, List<List<String>>>> spanOverlapFullResultMap;
    private Map<GenomicRegionSearchConstraint, String> spanConstraintMap;
    private HttpSession session;
    private WebConfig webConfig;
    private Profile profile;
    private InterMineAPI api;

    @SuppressWarnings("unchecked")
    private void init(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.session = request.getSession();
        this.spanUUIDString = request.getParameter("spanUUIDString");
        this.grsService = GenomicRegionSearchUtil.getGenomicRegionSearchService(request);
        //key - UUID
        this.spanOverlapFullResultMap = (Map<String, Map<GenomicRegion, List<List<String>>>>)
            session.getAttribute("spanOverlapFullResultMap");
        this.spanConstraintMap = (HashMap<GenomicRegionSearchConstraint, String>)
            session.getAttribute("spanConstraintMap");
        this.webConfig = SessionMethods.getWebConfig(request);
        this.profile = SessionMethods.getProfile(session);
        this.api = SessionMethods.getInterMineAPI(session);
    }

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        init(request, response);
        response.setContentType("text/plain");

        // An ajax call to request query progress
        if (request.getParameter("getProgress") != null) {
            getProgress(response);
        }

        // An ajax call to request result data
        if (request.getParameter("getData") != null
                && request.getParameter("fromIdx") != null
                && request.getParameter("toIdx") != null) {

            int fromIdx = Integer.parseInt((String) request.getParameter("fromIdx"));
            int toIdx = Integer.parseInt((String) request.getParameter("toIdx"));

            getData(fromIdx, toIdx, response);
        }

        // Get span overlap feature pids by giving a span (extended)
        if (request.getParameter("getFeatures") != null
                && request.getParameter("spanString") != null) {
            String spanString = request.getParameter("spanString");

            getFeatures(spanString, response);
        }

        // Check if any spans have features
        if (request.getParameter("isEmptyFeature") != null) {
            PrintWriter out = response.getWriter();
            out.println(grsService.isEmptyFeature(spanOverlapFullResultMap.get(spanUUIDString)));
        }

        // Generate a html string for create list use
        if (request.getParameter("generateCreateListHtml") != null) {
            PrintWriter out = response.getWriter();
            out.println(grsService
                    .generateCreateListHtml(spanOverlapFullResultMap
                            .get(spanUUIDString)));
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

            exportFeatures(criteria, facet, format, request, response);
        }

        // Create List
        if (request.getParameter("createList") != null) {
            String criteria = request.getParameter("criteria"); // all or location
            String facet = request.getParameter("facet"); // "SequenceFeature" or any featureType

            createListByFeatureType(criteria, facet, response);

//            return new ForwardParameters(mapping.findForward("saveFromIdsToBag"))
//                .addParameter("ids", ids)
//                .addParameter("type", facet)
//                .addParameter("source", "genomicRegionSearch").forward();
        }

        return null;
    }

    private void getProgress(HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        out.println(spanOverlapFullResultMap.get(spanUUIDString).size());
        out.flush();
        out.close();
    }

    private void getData(int fromIdx, int toIdx, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        // get span list from spanConstraintMap in the session
        List<GenomicRegion> genomicRegionList = null;
        for (Entry<GenomicRegionSearchConstraint, String> e : spanConstraintMap.entrySet()) {
            if (e.getValue().equals(spanUUIDString)) {
                genomicRegionList = e.getKey().getGenomicRegionList();
            }
        }

        String orgName = grsService.getGenomicRegionOrganismConstraint(
                spanUUIDString, spanConstraintMap);
        String htmlStr = grsService.convertResultMapToHTML(
                spanOverlapFullResultMap.get(spanUUIDString), genomicRegionList,
                fromIdx, toIdx, session,
                orgName);

        out.println(htmlStr);

        out.flush();
        out.close();
    }

    private void getFeatures(String spanString, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        int flankingSize = grsService
                .getGenomicRegionFlankingSizeConstraint(spanUUIDString, spanConstraintMap);
        String featureIds = grsService.getGenomicRegionOverlapFeaturesAsString(
                spanString, flankingSize,
                spanOverlapFullResultMap.get(spanUUIDString));

        out.println(featureIds);

        out.flush();
        out.close();
    }

    private void exportFeatures(String criteria, String facet, String format,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
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
            int flankingSize = grsService
                    .getGenomicRegionFlankingSizeConstraint(spanUUIDString,
                            spanConstraintMap);
            featureIdSet = grsService.getGenomicRegionOverlapFeaturesAsSet(
                    criteria, flankingSize, featureMap);
        }

        // Can read from web.properties to get pre-defined views
        Set<String> exportFeaturesViews = null;

        // == Experimental code ==
        String exportFeaturesViewsStr = SessionMethods.getWebProperties(
                session.getServletContext()).getProperty(
                "genomicRegionSearch.query." + facet + ".views");

        if (exportFeaturesViewsStr != null) {
            try {
                exportFeaturesViews = new LinkedHashSet<String>(Arrays.asList(StringUtil
                        .split(exportFeaturesViewsStr, ",")));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // == End of experimental code ==

        PathQuery q = grsService.getExportFeaturesQuery(featureIdSet,
                facet, exportFeaturesViews);

        String organism = new String();
        for (Entry<GenomicRegionSearchConstraint, String> e : spanConstraintMap.entrySet()) {
            if (e.getValue().equals(spanUUIDString)) {
                organism = e.getKey().getOrgName();
            }
        }

        Set<String> organisms = new HashSet<String>();
        organisms.add(organism);
        Set<Integer> taxIds = grsService.getTaxonIds(organisms);

        WebResultsExecutor executor = api.getWebResultsExecutor(profile);
        PagedTable pt = new PagedTable(executor.execute(q));

        if (pt.getWebTable() instanceof WebResults) {
            ((WebResults) pt.getWebTable()).goFaster();
        }

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

        if ("bed".equals(format)) {
            String ucscCompatibleCheck = "yes"; // TODO parameter pass from webpage
            exportForm = new BEDExportForm();
            exportForm.setDoGzip(doGzip);
            ((BEDExportForm) exportForm).setOrgansimString(organism);
            ((BEDExportForm) exportForm).setUcscCompatibleCheck(ucscCompatibleCheck);
        }

        exporter.export(pt, request, response, exportForm);
    }

    private void createListByFeatureType(String criteria, String facet,
            HttpServletResponse response) throws IOException {
        Set<Integer> featureIdSet = new LinkedHashSet<Integer>();
        Map<GenomicRegion, List<List<String>>> featureMap = spanOverlapFullResultMap
                .get(spanUUIDString);

        if ("all".equals(criteria)) {
            featureIdSet = grsService
                    .getAllGenomicRegionOverlapFeaturesByType(featureMap, facet);

            criteria = criteria + "_regions";
        } else {
            int flankingSize = grsService
                    .getGenomicRegionFlankingSizeConstraint(spanUUIDString,
                            spanConstraintMap);
            featureIdSet = grsService
                    .getGenomicRegionOverlapFeaturesByType(criteria,
                            flankingSize, featureMap, facet);

            criteria = criteria.replaceAll(":", "_").replaceAll("\\.\\.", "_");
        }

        // TODO Move creating bag code to a util class?
        String bagName = criteria + "_" + facet + "_list";
        bagName = NameUtil.generateNewName(profile.getSavedBags().keySet(),
                bagName);

        // Create bag
        InterMineAPI im = SessionMethods.getInterMineAPI(session);

        try {
            InterMineBag bag = profile.createBag(bagName, facet, "", im.getClassKeys());
            bag.addIdsToBag(featureIdSet, facet);
            profile.saveBag(bag.getName(), bag);
        } catch (ObjectStoreException e) {
            e.printStackTrace();
        }

        PrintWriter out = response.getWriter();
        out.println(bagName);

        out.flush();
        out.close();
    }

    // TODO more method to be created...
}
