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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.util.NameUtil;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.web.logic.GenomicRegionSearchService;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.GenomicRegionSearchConstraint;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.export.http.HttpExportUtil;
import org.intermine.web.logic.export.http.TableExporterFactory;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.export.rowformatters.CSVRowFormatter;
import org.intermine.web.logic.export.rowformatters.TabRowFormatter;
import org.intermine.web.logic.export.string.StringTableExporter;
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
    private static final Logger LOG = Logger
            .getLogger(GenomicRegionSearchAjaxAction.class);

    private String spanUUIDString;
    private GenomicRegionSearchService grsService;
    private Map<String, Map<GenomicRegion, List<List<String>>>> spanOverlapFullResultMap;
    private Map<String, Map<GenomicRegion, Map<String, Integer>>> spanOverlapFullStatMap;
    private Map<GenomicRegionSearchConstraint, String> spanConstraintMap;
    private HttpSession session;
    private WebConfig webConfig;
    private Profile profile;
    private InterMineAPI api;
    private static Map<MultiKey, String> chromosomeSequenceMap = new HashMap<MultiKey, String>();

    @SuppressWarnings("unchecked")
    private void init(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        this.session = request.getSession();
        this.spanUUIDString = request.getParameter("spanUUIDString");
        this.grsService = GenomicRegionSearchUtil
                .getGenomicRegionSearchService(request);
        // key - UUID
        this.spanOverlapFullResultMap = (Map<String, Map<GenomicRegion, List<List<String>>>>)
            session.getAttribute("spanOverlapFullResultMap");
        this.spanOverlapFullStatMap = (Map<String, Map<GenomicRegion, Map<String, Integer>>>)
            session.getAttribute("spanOverlapFullStatMap");
        this.spanConstraintMap = (HashMap<GenomicRegionSearchConstraint, String>) session
                .getAttribute("spanConstraintMap");
        this.webConfig = SessionMethods.getWebConfig(request);
        this.profile = SessionMethods.getProfile(session);
        this.api = SessionMethods.getInterMineAPI(session);
    }

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
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

            int fromIdx = Integer.parseInt((String) request
                    .getParameter("fromIdx"));
            int toIdx = Integer
                    .parseInt((String) request.getParameter("toIdx"));

            getData(fromIdx, toIdx, response);
        }

        // Get genomic region overlap feature pids by giving a genomic region (extended)
        if (request.getParameter("getFeatures") != null
                && request.getParameter("grString") != null) {
            String grString = request.getParameter("grString");

            getFeatures(grString, response);
        }

        // Check if any spans have features
        if (request.getParameter("isEmptyFeature") != null) {
            PrintWriter out = response.getWriter();
            out.println(grsService.isEmptyFeature(spanOverlapFullResultMap
                    .get(spanUUIDString)));
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
            // TODO to be implemented:
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
            String criteria = request.getParameter("criteria"); // all or ocation
            String facet = request.getParameter("facet"); // "SequenceFeature" or any featureType

            createListByFeatureType(criteria, facet, response);

            // return new
            // ForwardParameters(mapping.findForward("saveFromIdsToBag"))
            // .addParameter("ids", ids)
            // .addParameter("type", facet)
            // .addParameter("source", "genomicRegionSearch").forward();
        }

        // Get regions to create a select (DropDownList) list
        if (request.getParameter("getDropDownList") != null) {
            getRegionsForDropDownList(response);
        }

        // get given regions results as HTML
        if (request.getParameter("getGivenRegionsResults") != null) {
            String regions = request.getParameter("regions");

            Set<String> regionSet = new LinkedHashSet<String>();
            // Start with a single region
            regionSet.add(regions);

            getDataByRegions(regionSet, response);
        }

        if (request.getParameter("groupRegions") != null) {
            String interval = request.getParameter("interval");

            getGroupRegions(interval, response);
        }

        return null;
    }

    private void getProgress(HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        out.println(spanOverlapFullResultMap.get(spanUUIDString).size());
        out.flush();
        out.close();
    }

    private void getData(int fromIdx, int toIdx, HttpServletResponse response)
        throws IOException {
        PrintWriter out = response.getWriter();

        // get span list from spanConstraintMap in the session
        List<GenomicRegion> genomicRegionList = null;
        for (Entry<GenomicRegionSearchConstraint, String> e : spanConstraintMap
                .entrySet()) {
            if (e.getValue().equals(spanUUIDString)) {
                genomicRegionList = e.getKey().getGenomicRegionList();
            }
        }

        String htmlStr = grsService.convertResultMapToHTML(
                spanOverlapFullResultMap.get(spanUUIDString),
                spanOverlapFullStatMap.get(spanUUIDString),
                genomicRegionList, fromIdx, toIdx, session);

        out.println(htmlStr);

        out.flush();
        out.close();
    }

    private void getDataByRegions(Set<String> regionSet,
            HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();

        // Parse region string to a list of genomic region objects
        List<GenomicRegion> grList = GenomicRegionSearchUtil.generateGenomicRegion(regionSet);

        String htmlStr = grsService.convertResultMapToHTML(
                spanOverlapFullResultMap.get(spanUUIDString),
                spanOverlapFullStatMap.get(spanUUIDString),
                grList, 0, grList.size() - 1, session);

        out.println(htmlStr);

        out.flush();
        out.close();
    }

    private void getFeatures(String grInfo, HttpServletResponse response)
        throws Exception {
        PrintWriter out = response.getWriter();

        String featureIds = grsService.getGenomicRegionOverlapFeaturesAsString(
                grInfo, spanOverlapFullResultMap.get(spanUUIDString));

        out.println(featureIds);

        out.flush();
        out.close();
    }

    private void exportFeatures(String criteria, String facet, String format,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        Map<GenomicRegion, List<List<String>>> featureMap = spanOverlapFullResultMap
                .get(spanUUIDString);

        if ("all".equals(criteria) && ("tab".equals(format) || "csv".equals(format))) {
            // Use StringTableExporter
            StringTableExporter stringExporter;
            PrintWriter writer = HttpExportUtil.
                getPrintWriterForClient(request, response.getOutputStream());
            if ("csv".equals(format)) {
                stringExporter = new StringTableExporter(writer, new CSVRowFormatter());
                ResponseUtil.setCSVHeader(response, "results-table.csv");
            } else {
                stringExporter = new StringTableExporter(writer, new TabRowFormatter());
                ResponseUtil.setTabHeader(response, "results-table.tsv");
            }
            // parse export rows
            List<List<String>> exportResults = new ArrayList<List<String>>();

            String[] hearderArr = {"DB identifier", "Symbol", "Chr",
                "Start", "End", "User input"};
            List<String> headerList = new ArrayList<String>(Arrays.asList(hearderArr));

            int extendedSize = featureMap.keySet().iterator().next().getExtendedRegionSize();
//            String organism = featureMap.keySet().iterator().next().getOrganism();

            if (extendedSize > 0) {
                headerList.add("Extended input");
            }

            if (featureMap.keySet().iterator().next().getTag() != null) {
                headerList.add("User identifier");
            }

            exportResults.add(headerList);

            for (Entry<GenomicRegion, List<List<String>>> e : featureMap.entrySet()) {
                if (e.getValue() != null) {
                    for (List<String> l : e.getValue()) {
                        String pid = l.get(1);
                        String symbol = l.get(2);
                        String chr = l.get(4);
                        String start = l.get(5);
                        String end = l.get(6);
                        String userInput = e.getKey().getOriginalRegion();

                        String[] rowArr = {pid, symbol, chr, start, end, userInput};
                        List<String> row = new ArrayList<String>(Arrays.asList(rowArr));

                        if (extendedSize > 0) {
                            String extendedInput = e.getKey().getExtendedRegion();
                            row.add(extendedInput);
                        }

                        if (e.getKey().getTag() != null) {
                            String tag = e.getKey().getTag().toString();
                            row.add(tag);
                        }

                        exportResults.add(row);
                    }
                }
            }

            stringExporter.export(exportResults);
        } else {
            if ("chrSeg".equals(format)) {
                // download chromosome segment
                // TODO port to webservice
                if ("all".equals(criteria)) {
                    List<GenomicRegion> grList = new ArrayList<GenomicRegion>(
                            spanOverlapFullResultMap.get(spanUUIDString).keySet());
                    GenomicRegion aRegion = grList.get(0);
                    Organism org = (Organism) DynamicUtil.createObject(Collections
                            .singleton(Organism.class));
                    org.setShortName(aRegion.getOrganism());

                    try {
                        org = (Organism) api.getObjectStore().getObjectByExample(org,
                                Collections.singleton("shortName"));
                    } catch (ObjectStoreException e) {
                        throw new RuntimeException(
                                "unable to fetch Organism object", e);
                    }

                    String exportFileName = "chromosome_region_all.fasta";
                    response.setContentType("text/plain");
                    response.setHeader("Content-Disposition",
                            "attachment; filename=\"" + exportFileName + "\"");
                    OutputStream out = response.getOutputStream();

                    for (GenomicRegion gr : grList) {
                        Chromosome chr = (Chromosome) DynamicUtil
                        .createObject(Collections.singleton(Chromosome.class));
                        chr.setPrimaryIdentifier(gr.getChr());
                        chr.setOrganism(org);

                        try {
                            chr = (Chromosome) api.getObjectStore().getObjectByExample(
                                    chr,
                                    new HashSet<String>(Arrays.asList(new String[] {
                                        "primaryIdentifier", "organism" })));
                        } catch (ObjectStoreException e) {
                            throw new RuntimeException(
                                    "unable to fetch Chromosome object", e);
                        }

                        String chrResidueString;
                        if (chromosomeSequenceMap.get(new MultiKey(gr.getChr(), gr
                                .getOrganism())) == null) {
                            chrResidueString = chr.getSequence().getResidues()
                                    .toString();
                            chromosomeSequenceMap.put(
                                    new MultiKey(gr.getChr(), gr.getOrganism()), chr
                                            .getSequence().getResidues().toString());
                        } else {
                            chrResidueString = chromosomeSequenceMap.get(new MultiKey(
                                    gr.getChr(), gr.getOrganism()));
                        }

                        int chrLength = chr.getLength();
                        int start;
                        int end;

                        if (gr.getExtendedRegionSize() > 0) {
                            start = gr.getExtendedStart();
                            end = gr.getExtendedEnd();
                        } else {
                            start = gr.getStart();
                            end = gr.getEnd();
                        }

                        end = Math.min(end, chrLength);
                        start = Math.max(start, 1);

                        List<String> headerBits = new ArrayList<String>();
                        headerBits.add(gr.getChr() + ":" + start + ".." + end);
                        headerBits.add(end - start + 1 + "bp");
                        headerBits.add(gr.getOrganism());
                        String header = StringUtil.join(headerBits, " ");

                        String seqName = "chromosome_region_" + gr.getChr() + "_"
                                + start + "_" + end + "_"
                                + gr.getOrganism().replace("\\. ", "_");

                        Sequence chrSeg = DNATools
                                .createDNASequence(
                                        chrResidueString.substring(start - 1, end),
                                        seqName);
                        chrSeg.getAnnotation().setProperty(
                                FastaFormat.PROPERTY_DESCRIPTIONLINE, header);

                        // write it out
                        SeqIOTools.writeFasta(out, chrSeg);
                    }
                    out.flush();
                } else {
                    GenomicRegion gr = GenomicRegionSearchUtil
                    .generateGenomicRegion(Arrays.asList(new String[] {criteria}))
                        .get(0);

                    Organism org = (Organism) DynamicUtil.createObject(Collections
                            .singleton(Organism.class));
                    org.setShortName(gr.getOrganism());

                    try {
                        org = (Organism) api.getObjectStore().getObjectByExample(org,
                                Collections.singleton("shortName"));
                    } catch (ObjectStoreException e) {
                        throw new RuntimeException(
                                "unable to fetch Organism object", e);
                    }

                    Chromosome chr = (Chromosome) DynamicUtil
                            .createObject(Collections.singleton(Chromosome.class));
                    chr.setPrimaryIdentifier(gr.getChr());
                    chr.setOrganism(org);

                    try {
                        chr = (Chromosome) api.getObjectStore().getObjectByExample(
                                chr,
                                new HashSet<String>(Arrays.asList(new String[] {
                                    "primaryIdentifier", "organism" })));
                    } catch (ObjectStoreException e) {
                        throw new RuntimeException(
                                "unable to fetch Chromosome object", e);
                    }

                    String chrResidueString;
                    if (chromosomeSequenceMap.get(new MultiKey(gr.getChr(), gr
                            .getOrganism())) == null) {
                        chrResidueString = chr.getSequence().getResidues()
                                .toString();
                        chromosomeSequenceMap.put(
                                new MultiKey(gr.getChr(), gr.getOrganism()), chr
                                        .getSequence().getResidues().toString());
                    } else {
                        chrResidueString = chromosomeSequenceMap.get(new MultiKey(
                                gr.getChr(), gr.getOrganism()));
                    }

                    int chrLength = chr.getLength();
                    int start;
                    int end;

                    if (gr.getExtendedRegionSize() > 0) {
                        start = gr.getExtendedStart();
                        end = gr.getExtendedEnd();
                    } else {
                        start = gr.getStart();
                        end = gr.getEnd();
                    }

                    end = Math.min(end, chrLength);
                    start = Math.max(start, 1);

                    List<String> headerBits = new ArrayList<String>();
                    headerBits.add(gr.getChr() + ":" + start + ".." + end);
                    headerBits.add(end - start + 1 + "bp");
                    headerBits.add(gr.getOrganism());
                    String header = StringUtil.join(headerBits, " ");

                    String seqName = "chromosome_segment_" + gr.getChr() + "_"
                            + start + "_" + end + "_"
                            + gr.getOrganism().replace("\\. ", "_");
                    String exportFileName = seqName + ".fasta";

                    Sequence chrSeg = DNATools
                            .createDNASequence(
                                    chrResidueString.substring(start - 1, end),
                                    seqName);
                    chrSeg.getAnnotation().setProperty(
                            FastaFormat.PROPERTY_DESCRIPTIONLINE, header);

                    // write it out
                    response.setContentType("text/plain");
                    response.setHeader("Content-Disposition",
                            "attachment; filename=\"" + exportFileName + "\"");
                    OutputStream out = response.getOutputStream();
                    SeqIOTools.writeFasta(out, chrSeg);
                    out.flush();
                }
            } else {
                boolean doGzip = false;
                Set<Integer> featureIdSet = new LinkedHashSet<Integer>();

                if ("all".equals(criteria)) {
                    for (List<List<String>> l : featureMap.values()) {
                        if (l != null) {
                            for (List<String> r : l) {
                                featureIdSet.add(Integer.valueOf(r.get(0)));
                            }
                        }
                    }

                } else {
                    featureIdSet = grsService.getGenomicRegionOverlapFeaturesAsSet(
                            criteria, featureMap);
                }

                // Can read from web.properties to get pre-defined views
                Set<String> exportFeaturesViews = null;
                List<String> exportFeaturesSortOrder = null;

                // == Experimental code ==
                String exportFeaturesViewsStr = SessionMethods.getWebProperties(
                        session.getServletContext()).getProperty(
                        "genomicRegionSearch.query." + facet + ".views");

                String exportFeaturesSortOrderStr = SessionMethods.getWebProperties(
                        session.getServletContext()).getProperty(
                        "genomicRegionSearch.query." + facet + ".sortOrder");

                if (exportFeaturesViewsStr != null) {
                     if (!exportFeaturesViewsStr.isEmpty()) {
                         try {
                             exportFeaturesViews = new LinkedHashSet<String>(
                                     Arrays.asList(StringUtil.split(exportFeaturesViewsStr,
                                             ",")));
                             exportFeaturesSortOrder = Arrays.asList(StringUtil
                                     .split(exportFeaturesSortOrderStr, " "));
                         } catch (Exception e) {
                             throw new RuntimeException(e);
                         }
                     }
                }
                // == End of experimental code ==

                PathQuery q = grsService.getExportFeaturesQuery(featureIdSet, facet,
                        exportFeaturesViews, exportFeaturesSortOrder);

                String organism = new String();
                for (Entry<GenomicRegionSearchConstraint, String> e : spanConstraintMap
                        .entrySet()) {
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
                    // TODO parameter passed from webpage?
                    String ucscCompatibleCheck = "yes";
                    exportForm = new BEDExportForm();
                    exportForm.setDoGzip(doGzip);
                    ((BEDExportForm) exportForm).setOrgansimString(organism);
                    ((BEDExportForm) exportForm)
                            .setUcscCompatibleCheck(ucscCompatibleCheck);
                }

                exporter.export(pt, request, response, exportForm, null, null);
            }
        }
    }

    private void createListByFeatureType(String criteria, String facet,
            HttpServletResponse response) throws Exception {
        Set<Integer> featureIdSet = new LinkedHashSet<Integer>();
        Map<GenomicRegion, List<List<String>>> featureMap = spanOverlapFullResultMap
                .get(spanUUIDString);

        if ("all".equals(criteria)) {
            featureIdSet = grsService.getAllGenomicRegionOverlapFeaturesByType(
                    featureMap, facet);

            criteria = criteria + "_regions";
        } else {
            featureIdSet = grsService.getGenomicRegionOverlapFeaturesByType(
                    criteria, featureMap, facet);

            criteria = criteria.split("\\|")[0].replaceAll(":", "_").replaceAll("\\.\\.", "_");
        }

        // TODO Move creating bag code to a util class?
        String bagName = criteria + "_" + facet + "_list";
        bagName = NameUtil.generateNewName(profile.getSavedBags().keySet(),
                bagName);

        // Create bag
        InterMineAPI im = SessionMethods.getInterMineAPI(session);

        try {
            InterMineBag bag = profile.createBag(bagName, facet, "",
                    im.getClassKeys());
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

    /**
     * Get a list of regions (comma separated) to populate a dropdown list on jsp
     */
    private void getRegionsForDropDownList(HttpServletResponse response)
        throws IOException {

        List<GenomicRegion> grList = new ArrayList<GenomicRegion>(
                spanOverlapFullResultMap.get(spanUUIDString).keySet());
        Collections.sort(grList);

        List<String> grToStringList = new ArrayList<String>();

        // Functional programming will handle better?
        for (GenomicRegion gr : grList) {
            grToStringList.add(gr.getFullRegionInfo());
        }

        String jointed = StringUtil.join(grToStringList, ",");

        PrintWriter out = response.getWriter();
        out.println(jointed);

        out.flush();
        out.close();
    }

    /**
     * Given an interval, look for genomic regions within and overlap with that interval
     * @throws Exception
     */
    private void getGroupRegions(String interval, HttpServletResponse response) throws Exception {

        List<GenomicRegion> grList = GenomicRegionSearchUtil
                .groupGenomicRegionByInterval(interval,
                        spanOverlapFullResultMap.get(spanUUIDString).keySet());

        String htmlStr = grsService.convertResultMapToHTML(
                spanOverlapFullResultMap.get(spanUUIDString),
                spanOverlapFullStatMap.get(spanUUIDString),
                grList, 0, grList.size() - 1, session);

        PrintWriter out = response.getWriter();
        out.println(htmlStr);

        out.flush();
        out.close();
    }

    // TODO more method to be created...
}
