package org.intermine.bio.web.logic;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;
import org.intermine.api.profile.Profile;
import org.intermine.bio.web.model.ChromosomeInfo;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.GenomicRegionSearchConstraint;
import org.intermine.bio.web.struts.GenomicRegionSearchForm;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.OverlapConstraint;
import org.intermine.objectstore.query.OverlapRange;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONObject;

/**
 * A class to provide genomic region search services in general.
 *
 * @author Fengyuan Hu
 */
public class GenomicRegionSearchService
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(GenomicRegionSearchService.class);

    private HttpServletRequest request = null;

    private static String orgFeatureJSONString = "";

    private static final String GENOMIC_REGION_SEARCH_OPTIONS_DEFAULT =
        "genomic_region_search_options_default";

    private static final String GENOMIC_REGION_SEARCH_RESULTS_DEFAULT =
        "genomic_region_search_results_default";

    private static final int READ_AHEAD_CHARS = 10000;

    private GenomicRegionSearchConstraint grsc = null;

    private static Map<String, List<ChromosomeInfo>> chrInfoMap = null;

    private static Set<String> featureTypesInOrgs = null;

    private static Map<String, List<String>> featureTypeToSOTermMap = null;

    private static Map<String, Integer> orgTaxonIdMap = null;

    private List<String> selectionInfo = new ArrayList<String>();

    /**
     * Constructor
     *
     * @param request HttpServletRequest
     */
    public GenomicRegionSearchService(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * To call the queryOrganismAndSequenceFeatureTypes method in GenomicRegionSearchQueryRunner.
     *
     * @return a JSON string
     * @throws Exception e
     */
    public String setupWebData() throws Exception {
        // By default, query all organisms in the database
        // pre defined organism short names can be read out from web.properties
        String defaultOrganisms = (String) SessionMethods.getWebProperties(
                request.getSession().getServletContext()).get(
                "genomicRegionSearch.defaultOrganisms");

        List<String> orgList = new ArrayList<String>();
        List<String> orgWithNoChrInfoList = new ArrayList<String>();
        Set<String> chrOrgSet = getChrInfoMap().keySet();
        if ((defaultOrganisms == null || "".equals(defaultOrganisms))
                && chrOrgSet == null) {
            throw new RuntimeException("None of the sequence features has location information...");
        } else {
            if ((defaultOrganisms == null || "".equals(defaultOrganisms))
                    && chrOrgSet != null && chrOrgSet.size() > 0) {
                orgList = new ArrayList<String>(chrOrgSet);
            }

            if (defaultOrganisms != null
                    && !"".equals(defaultOrganisms)
                    && (chrOrgSet == null || chrOrgSet.size() == 0)) {
                List<String> defaultOrgList = Arrays.asList(defaultOrganisms.split(","));
                for (String s : defaultOrgList) {
                    if (!"".equals(s.trim())) {
                        orgList.add(s.trim());
                    }
                }
            }

            if (defaultOrganisms != null && !"".equals(defaultOrganisms)
                    && chrOrgSet != null && chrOrgSet.size() > 0) {
                List<String> defaultOrgList = Arrays.asList(defaultOrganisms.split(","));
                List<String> newDefultOrgList = new ArrayList<String>();

                for (String s : defaultOrgList) {
                    if (!"".equals(s.trim())) {
                        newDefultOrgList.add(s.trim());
                    }
                }

                for (String o : newDefultOrgList) {
                    if (chrOrgSet.contains(o)) {
                        chrOrgSet.remove(o);
                    }
                    // If chrOrgSet doesn't include pre-defined organisms?
                    // The admin needs to make sure pre-defined organisms have chromosome location
                    // information in the database
                }

                orgList.addAll(newDefultOrgList);
                orgList.addAll(chrOrgSet);
            }

        }

        if ("".equals(orgFeatureJSONString)) {
            orgFeatureJSONString = prepareWebData(orgList);
            return orgFeatureJSONString;
        } else {
            return orgFeatureJSONString;
        }
    }

    /**
     * Get the name of customized options javascript, by default, it is
     * named "genomic_region_search_options_default.js"
     *
     * @return the name of options javascript name
     */
    public String getOptionsJavascript() {
        String optionsJavascriptName = (String) SessionMethods.getWebProperties(
                request.getSession().getServletContext()).get(
                "genomicRegionSearch.optionsJavascript");

        if (optionsJavascriptName == null || "".equals(optionsJavascriptName)) {
            optionsJavascriptName = GENOMIC_REGION_SEARCH_OPTIONS_DEFAULT;
        }

        return optionsJavascriptName;
    }

    /**
     * Get the name of customized results javascript
     *
     * @return the name of results page
     */
    public String getResultsJavascript() {
        String resultsJavascriptName = (String) SessionMethods.getWebProperties(
                request.getSession().getServletContext()).get(
                "genomicRegionSearch.resultsJavascript");

        if (resultsJavascriptName == null || "".equals(resultsJavascriptName)) {
            resultsJavascriptName = GENOMIC_REGION_SEARCH_RESULTS_DEFAULT;
        }

        return resultsJavascriptName;
    }

    /**
     * Get the name of customized options CSS
     *
     * @return the name of options css
     */
    public String getOptionsCss() {
        String optionsCssName = (String) SessionMethods.getWebProperties(
                request.getSession().getServletContext()).get(
                "genomicRegionSearch.optionsCss");

        if (optionsCssName == null || "".equals(optionsCssName)) {
            optionsCssName = GENOMIC_REGION_SEARCH_OPTIONS_DEFAULT;
        }

        return optionsCssName;
    }

    /**
     * Get the name of customized results CSS
     *
     * @return the name of results css
     */
    public String getResultsCss() {
        String resultsCssName = (String) SessionMethods.getWebProperties(
                request.getSession().getServletContext()).get(
                "genomicRegionSearch.resultsCss");

        if (resultsCssName == null || "".equals(resultsCssName)) {
            resultsCssName = GENOMIC_REGION_SEARCH_RESULTS_DEFAULT;
        }

        return resultsCssName;
    }

    /**
     * To parse form data
     *
     * @param grsForm GenomicRegionSearchForm
     * @return genomic region search constraint
     */
    public ActionMessage parseGenomicRegionSearchForm(GenomicRegionSearchForm grsForm) {
        grsc = new GenomicRegionSearchConstraint();

        ActionMessage actmsg = parseBasicInput(grsForm);
        if (actmsg != null) {
            return actmsg;
        }

        return null;
    }

    /**
     *
     * @param grsForm GenomicRegionSearchForm
     * @return ActionMessage
     */
    public ActionMessage parseBasicInput(GenomicRegionSearchForm grsForm) {

        // Parse form
        String organism = (String) grsForm.get("organism");
        String[] featureTypes = (String[]) grsForm.get("featureTypes");
        String whichInput = (String) grsForm.get("whichInput");
        String dataFormat = (String) grsForm.get("dataFormat");
        FormFile formFile = (FormFile) grsForm.get("fileInput");
        String pasteInput = (String) grsForm.get("pasteInput");

        // Organism
        grsc.setOrgName(organism);

        selectionInfo.add("<b>Selected organism: </b><i>" + organism + "</i>");

        // Feature types
        if (featureTypes == null) {
            return new ActionMessage("genomicRegionSearch.spanFieldSelection", "feature types");
        }

        //// featureTypes in this case are (the last bit of) class instead of featuretype in the db
        //// table; gain the full name by Model.getQualifiedTypeName(className)
        @SuppressWarnings("rawtypes")
        List<Class> ftList = new ArrayList<Class>();
        String modelPackName = SessionMethods
                .getInterMineAPI(request.getSession()).getModel()
                .getPackageName();
        for (String f : featureTypes) {
            try {
                ftList.add(Class.forName(modelPackName + "." + f));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        String ftString = "";
        for (String aFeaturetype : featureTypes) {
            ftString = ftString + aFeaturetype + ", ";
        }
        selectionInfo.add("<b>Selected feature types: </b>"
                + ftString.substring(0, ftString.lastIndexOf(", ")));

        grsc.setFtList(ftList);

        // File parsing
        BufferedReader reader = null;

        /*
         * FormFile used from Struts works a bit strangely. 1. Although the file
         * does't exist formFile.getInputStream() doesn't throw
         * FileNotFoundException. 2. When user specified empty file path or very
         * invalid file path, like file path not starting at '/' then
         * formFile.getFileName() returns empty string.
         */
        if ("paste".equals(whichInput)) {
            if (pasteInput != null
                    && pasteInput.length() != 0) {
                String trimmedText = pasteInput.trim();
                if (trimmedText.length() == 0) {
                    return new ActionMessage("genomicRegionSearch.noSpanPaste");
                }
                reader = new BufferedReader(new StringReader(trimmedText));
            } else {
                return new ActionMessage("genomicRegionSearch.noSpanFile");
            }

        } else if ("file".equals(whichInput)) {
            if (formFile != null && formFile.getFileName() != null
                    && formFile.getFileName().length() > 0) {

                String mimetype = formFile.getContentType();
                if (!"application/octet-stream".equals(mimetype)
                        && !mimetype.startsWith("text")) {
                    return new ActionMessage("genomicRegionSearch.isNotText", mimetype);
                }
                if (formFile.getFileSize() == 0) {
                    return new ActionMessage("genomicRegionSearch.noSpanFileOrEmpty");
                }
                try {
                    reader = new BufferedReader(new InputStreamReader(formFile
                            .getInputStream()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            return new ActionMessage("genomicRegionSearch.spanInputType");
        }

        // Validate text format
        try {
            reader.mark(READ_AHEAD_CHARS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        char[] buf = new char[READ_AHEAD_CHARS];
        int read;
        Set<String> spanStringSet = new LinkedHashSet<String>();

        try {
            read = reader.read(buf, 0, READ_AHEAD_CHARS);

            for (int i = 0; i < read; i++) {
                if (buf[i] == 0) {
                    return new ActionMessage("genomicRegionSearch.isNotText", "binary");
                }
            }

            reader.reset();

            // Remove duplication
            String thisLine;
            while ((thisLine = reader.readLine()) != null) {
                spanStringSet.add(thisLine);
            }

        } catch (IOException e) {
            e.printStackTrace();
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
            // "chr:singlePosition" - [^:]+:[\d]+$
            String ddotsRegex = "[^:]+: ?\\d+\\.\\.\\d+$";
            String tabRegex = "[^\\t]+\\t\\d+\\t\\d+$";
            String dashRegex = "[^:]+: ?\\d+\\-\\d+$";
            String snpRegex = "[^:]+: ?\\d+$";

            if (Pattern.matches(ddotsRegex, spanStr)) {
                aSpan.setChr((spanStr.split(":"))[0]);
                String[] spanItems = (spanStr.split(":"))[1].split("\\..");
                String start = spanItems[0].trim();
                if ("isInterBaseCoordinate".equals(dataFormat)) {
                    aSpan.setStart(Integer.valueOf(start) + 1);
                } else {
                    aSpan.setStart(Integer.valueOf(start));
                }
                aSpan.setEnd(Integer.valueOf(spanItems[1]));
            } else if (Pattern.matches(tabRegex, spanStr)) {
                String[] spanItems = spanStr.split("\t");
                aSpan.setChr(spanItems[0]);
                if ("isInterBaseCoordinate".equals(dataFormat)) {
                    aSpan.setStart(Integer.valueOf(spanItems[1]) + 1);
                } else {
                    aSpan.setStart(Integer.valueOf(spanItems[1]));
                }
                aSpan.setEnd(Integer.valueOf(spanItems[2]));
            } else if (Pattern.matches(dashRegex, spanStr)) {
                aSpan.setChr((spanStr.split(":"))[0]);
                String[] spanItems = (spanStr.split(":"))[1].split("-");
                String start = spanItems[0].trim();
                if ("isInterBaseCoordinate".equals(dataFormat)) {
                    aSpan.setStart(Integer.valueOf(start) + 1);
                } else {
                    aSpan.setStart(Integer.valueOf(start));
                }
                aSpan.setEnd(Integer.valueOf(spanItems[1]));
            } else if (Pattern.matches(snpRegex, spanStr)) {
                aSpan.setChr((spanStr.split(":"))[0]);
                String start = (spanStr.split(":"))[1].trim();
                if ("isInterBaseCoordinate".equals(dataFormat)) {
                    aSpan.setStart(Integer.valueOf(start) + 1);
                } else {
                    aSpan.setStart(Integer.valueOf(start));
                }
                aSpan.setEnd(Integer.valueOf((spanStr.split(":"))[1].trim()));

            } else {
                return new ActionMessage("genomicRegionSearch.spanInWrongformat", spanStr);
            }
            spanList.add(aSpan);
        }

        // Chromesome starts with "chr" - UCSC formats
        for (GenomicRegion aSpan : spanList) {
            if (aSpan.getChr().startsWith("chr")) {
                aSpan.setChr(aSpan.getChr().substring(3));
            }
        }

        grsc.setSpanList(spanList);

        return null;
    }


    /**
     * To prepare queries for genomic regions
     *
     * @return a list of prepared queries for genomic regions
     */
    public Map<GenomicRegion, Query> createQueryList() {
        Map<GenomicRegion, Query> queryMap = new LinkedHashMap<GenomicRegion, Query>();

        for (GenomicRegion aSpan: grsc.getSpanList()) {
            Query q = new Query();
            q.setDistinct(true);

            String chrPID = aSpan.getChr();
            Integer start = aSpan.getStart();
            Integer end = aSpan.getEnd();

            QueryClass qcOrg = new QueryClass(Organism.class);
            QueryClass qcChr = new QueryClass(Chromosome.class);
            QueryClass qcFeature = new QueryClass(SequenceFeature.class);
            QueryClass qcLoc = new QueryClass(Location.class);

            QueryField qfOrgName = new QueryField(qcOrg, "shortName");
            QueryField qfFeatureId = new QueryField(qcFeature, "id");
            QueryField qfFeaturePID = new QueryField(qcFeature, "primaryIdentifier");
            QueryField qfFeatureClass = new QueryField(qcFeature, "class");
            QueryField qfChr = new QueryField(qcChr, "primaryIdentifier");
            QueryField qfLocStart = new QueryField(qcLoc, "start");
            QueryField qfLocEnd = new QueryField(qcLoc, "end");

            q.addToSelect(qfFeatureId);
            q.addToSelect(qfFeaturePID);
            q.addToSelect(qfFeatureClass);
            q.addToSelect(qfChr);
            q.addToSelect(qfLocStart);
            q.addToSelect(qfLocEnd);

            q.addFrom(qcChr);
            q.addFrom(qcOrg);
            q.addFrom(qcFeature);
            q.addFrom(qcLoc);

            q.addToOrderBy(qfLocStart, "ascending");

            ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);

            q.setConstraint(constraints);

            // SequenceFeature.organism = Organism
            QueryObjectReference organism = new QueryObjectReference(qcFeature,
                    "organism");
            ContainsConstraint ccOrg = new ContainsConstraint(organism,
                    ConstraintOp.CONTAINS, qcOrg);
            constraints.addConstraint(ccOrg);

            // Organism.name = orgName
            SimpleConstraint scOrg = new SimpleConstraint(qfOrgName,
                    ConstraintOp.EQUALS, new QueryValue(grsc.getOrgName()));
            constraints.addConstraint(scOrg);

            // Location.feature = SequenceFeature
            QueryObjectReference locSubject = new QueryObjectReference(qcLoc,
                    "feature");
            ContainsConstraint ccLocSubject = new ContainsConstraint(locSubject,
                    ConstraintOp.CONTAINS, qcFeature);
            constraints.addConstraint(ccLocSubject);

            // Location.locatedOn = Chromosome
            QueryObjectReference locObject = new QueryObjectReference(qcLoc,
                    "locatedOn");
            ContainsConstraint ccLocObject = new ContainsConstraint(locObject,
                    ConstraintOp.CONTAINS, qcChr);
            constraints.addConstraint(ccLocObject);

            // Chromosome.primaryIdentifier = chrPID
            SimpleConstraint scChr = new SimpleConstraint(qfChr,
                    ConstraintOp.EQUALS, new QueryValue(chrPID));
            constraints.addConstraint(scChr);

            // SequenceFeature.class in a list
            constraints.addConstraint(new BagConstraint(qfFeatureClass,
                ConstraintOp.IN, grsc.getFtList()));

            OverlapRange overlapInput = new OverlapRange(new QueryValue(start),
                    new QueryValue(end), locObject);
            OverlapRange overlapFeature = new OverlapRange(new QueryField(qcLoc,
                    "start"), new QueryField(qcLoc, "end"), locObject);
            OverlapConstraint oc = new OverlapConstraint(overlapInput,
                    ConstraintOp.OVERLAPS, overlapFeature);
            constraints.addConstraint(oc);

            queryMap.put(aSpan, q);
        }

        return queryMap;
    }

    /**
     * @return the grsc
     */
    public GenomicRegionSearchConstraint getConstraint() {
        return this.grsc;
    }

    /**
     *
     * @return chrInfoMap
     */
    public Map<String, List<ChromosomeInfo>> getChrInfoMap() {
        if (chrInfoMap == null) {
            chrInfoMap = GenomicRegionSearchQueryRunner
            .getChromosomeInfo(SessionMethods.getInterMineAPI(request.getSession()));
        }
        return chrInfoMap;
    }

    /**
    *
    * @return featureTypeToSOTermMap
    */
    public Map<String, List<String>> getFtToSoMap() {

        if (featureTypeToSOTermMap == null) {
            featureTypeToSOTermMap = GenomicRegionSearchQueryRunner
            .getFeatureAndSOInfo(SessionMethods.getInterMineAPI(request.getSession()));

            if (!(featureTypesInOrgs.size() == featureTypeToSOTermMap.size() && featureTypesInOrgs
                    .containsAll(featureTypeToSOTermMap.keySet()))) {
                Map<String, List<String>> newFeatureTypeToSOTermMap =
                    new HashMap<String, List<String>>();

                for (String ft : featureTypesInOrgs) {
                    if (featureTypeToSOTermMap.keySet().contains(ft)) {
                        newFeatureTypeToSOTermMap.put(ft, featureTypeToSOTermMap.get(ft));
                    } else {
                        List<String> des = new ArrayList<String>();
                        des.add(ft);
                        des.add("description not avaliable");
                        newFeatureTypeToSOTermMap.put(ft, des);
                    }
                }

                featureTypeToSOTermMap = newFeatureTypeToSOTermMap;
            }
        }

        return featureTypeToSOTermMap;
    }

    /**
    *
    * @return orgTaxonIdMap
    */
    public Map<String, Integer> getOrgTaxonIdMap() {
        Profile profile = SessionMethods.getProfile(request.getSession());
        if (orgTaxonIdMap == null) {
            orgTaxonIdMap = GenomicRegionSearchQueryRunner
            .getTaxonInfo(SessionMethods.getInterMineAPI(request.getSession()), profile);
        }
        return orgTaxonIdMap;
    }

    /**
     *
     * @return resultMap
     */
    public Map<String, List<GenomicRegion>> validateGenomicRegions() {
        // the Map has two key-value mappings
        // PASS-ArrayList<passedSpan>
        // ERROR-ArrayList<errorSpan>
        Map<String, List<GenomicRegion>> resultMap = new HashMap<String, List<GenomicRegion>>();
        List<GenomicRegion> passedSpanList = new ArrayList<GenomicRegion>();
        List<GenomicRegion> errorSpanList = new ArrayList<GenomicRegion>();

        List<ChromosomeInfo> chrInfoList = chrInfoMap.get(grsc.getOrgName());

        if (chrInfoList == null) {
            return null;
        }

        // make passedSpanList
        for (GenomicRegion aSpan : grsc.getSpanList()) {
            for (ChromosomeInfo chrInfo : chrInfoList) {
                if (aSpan.getChr().equals(chrInfo.getChrPID())) {
                    if ((aSpan.getStart() >= 0 && aSpan.getStart() <= chrInfo
                            .getChrLength())
                            && (aSpan.getEnd() >= 0 && aSpan.getEnd() <= chrInfo
                                    .getChrLength())) {
                        if (aSpan.getStart() > aSpan.getEnd()) { // Start must be smaller than End
                            GenomicRegion newSpan = new GenomicRegion();
                            newSpan.setChr(aSpan.getChr());
                            newSpan.setStart(aSpan.getEnd());
                            newSpan.setEnd(aSpan.getStart());
                            passedSpanList.add(newSpan);
                        } else {
                            passedSpanList.add(aSpan);
                        }
                    }
                }
            }

        }

        // make errorSpanList
        for (GenomicRegion aSpan : grsc.getSpanList()) {
            if (!passedSpanList.contains(aSpan)) {
                errorSpanList.add(aSpan);
            }
        }

        resultMap.put("pass", passedSpanList);
        resultMap.put("error", errorSpanList);

        return resultMap;
    }

    /**
     * The message passed to results page
     *
     * @return resultMessages
     */
    public List<String> getSelectionInformation() {

        return this.selectionInfo;
    }

    private String prepareWebData(List<String> orgList) {

        Query q = new Query();
        q.setDistinct(true);

        QueryClass qcOrg = new QueryClass(Organism.class);
        QueryClass qcFeature = new QueryClass(SequenceFeature.class);

        QueryField qfOrgName = new QueryField(qcOrg, "shortName");
        QueryField qfFeatureClass = new QueryField(qcFeature, "class");

        q.addToSelect(qfOrgName);
        q.addToSelect(qfFeatureClass);

        q.addFrom(qcOrg);
        q.addFrom(qcFeature);

        q.addToOrderBy(qfOrgName, "ascending");

        ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);

        q.setConstraint(constraints);

        // SequenceFeature.organism = Organism
        QueryObjectReference organism = new QueryObjectReference(qcFeature,
                "organism");
        ContainsConstraint ccOrg = new ContainsConstraint(organism,
                ConstraintOp.CONTAINS, qcOrg);
        constraints.addConstraint(ccOrg);

//        constraints.addConstraint(new BagConstraint(qfOrgName,
//                    ConstraintOp.IN, orgList));

        Results results =
            SessionMethods.getInterMineAPI(request.getSession()).getObjectStore().execute(q);

        // Parse results data to a map
        Map<String, Set<String>> resultsMap = new LinkedHashMap<String, Set<String>>();
        Set<String> featureTypeSet = new LinkedHashSet<String>();

        // TODO this will be very slow when query too many features
        if (results == null || results.size() < 0) {
            return "";
        }
        else {
            for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
                ResultsRow<?> row = (ResultsRow<?>) iter.next();

                String org = (String) row.get(0);
                @SuppressWarnings("rawtypes")
                // TODO exception - feature type is NULL
                String featureType = ((Class) row.get(1)).getSimpleName();

                if (!"Chromosome".equals(featureType) && orgList.contains(org)) {
                    if (resultsMap.size() < 1) {
                        featureTypeSet.add(featureType);
                        resultsMap.put(org, featureTypeSet);
                    } else {
                        if (resultsMap.keySet().contains(org)) {
                            resultsMap.get(org).add(featureType);
                        } else {
                            Set<String> s = new LinkedHashSet<String>();
                            s.add(featureType);
                            resultsMap.put(org, s);
                        }
                    }
                }
            }
        }

        // Get all feature types
        for (Set<String> ftSet : resultsMap.values()) {
            if (featureTypesInOrgs == null) {
                featureTypesInOrgs = new HashSet<String>();
                featureTypesInOrgs.addAll(ftSet);
            }
        }

        getFtToSoMap();
        getOrgTaxonIdMap();

        // Parse data to JSON string
        List<Object> l = new ArrayList<Object>();
        Map<String, Object> ma = new LinkedHashMap<String, Object>();

        for (Entry<String, Set<String>> e : resultsMap.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();

            m.put("organism", e.getKey());
            m.put("features", new ArrayList<String>(e.getValue()));

            l.add(m);
        }

        ma.put("organisms", orgList);
        ma.put("featureTypes", l);
        JSONObject jo = new JSONObject(ma);

        return jo.toString();
    }


    /**
     * for GenomicRegionSearchAjaxAction use.
     *
     * @param spanUUIDString uuid
     * @param spanConstraintMap map of contraints
     * @return the organism
     */
    public String getSpanOrganism(String spanUUIDString,
            Map<GenomicRegionSearchConstraint, String> spanConstraintMap) {

        for (Entry<GenomicRegionSearchConstraint, String> e : spanConstraintMap.entrySet()) {
            if (e.getValue().equals(spanUUIDString)) {
                return e.getKey().getOrgName();
            }
        }
        return null;
    }

    /**
     * Get a comma separated string of a span's overlap features.
     * for GenomicRegionSearchAjaxAction use.
     *
     * @param spanUUIDString uuid
     * @param spanString span in string
     * @param resultMap map of search results
     * @return String
     */
    public String getSpanOverlapFeatures(String spanUUIDString, String spanString,
            Map<GenomicRegion, List<List<String>>> resultMap) {

        Set<String> featureSet = new HashSet<String>();

        GenomicRegion spanToExport = new GenomicRegion(spanString);
        for (List<String> r : resultMap.get(spanToExport)) {
            featureSet.add(r.get(0));
        }

        return StringUtil.join(featureSet, ",");
    }

    /**
     * Check whether the results have empty features.
     * for GenomicRegionSearchAjaxAction use.
     *
     * @param resultMap map of search results
     * @return String
     */
    public String isEmptyFeature(Map<GenomicRegion, List<List<String>>> resultMap) {
        for (List<List<String>> l : resultMap.values()) {
            if (l != null) {
                return "hasFeature";
            }
        }
        return "emptyFeature";
    }

    /**
     * Convert result map to HTML string.
     *
     * @param resultMap resultMap
     * @param spanList spanList
     * @param fromIdx offsetStart
     * @param toIdx offsetEnd
     * @param session the current session
     * @param orgName organism
     * @return a String
     */
    public String convertResultMapToHTML(
            Map<GenomicRegion, List<List<String>>> resultMap,
            List<GenomicRegion> spanList,
            int fromIdx,
            int toIdx,
            HttpSession session,
            String orgName) {

        String baseURL = SessionMethods.getWebProperties(
                session.getServletContext()).getProperty("webapp.baseurl");
        String path = SessionMethods.getWebProperties(
                session.getServletContext()).getProperty("webapp.path");

        List<GenomicRegion> subSpanList = spanList.subList(fromIdx, toIdx + 1);

        // start to build the html for results table
        StringBuffer sb = new StringBuffer();

        sb.append("<thead><tr valign=\"middle\">");
        sb.append("<th align=\"center\">Genome Region</th>");
        sb.append("<th align=\"center\">Feature</th>");
        sb.append("<th align=\"center\">Feature Type</th>");
        sb.append("<th align=\"center\">Location</th>");
        sb.append("</tr></thead>");
        sb.append("<tbody>");

        for (GenomicRegion s : subSpanList) {

            String span = s.toString();
            List<List<String>> features = resultMap.get(s);

            /*
             * order:
             * 0.id
             * 1.feature name
             * 2.feature type
             * 3.chr
             * 4.start
             * 5.end
             */
            if (features != null) {
                int length = features.size();
                List<String> firstFeature = features.get(0);
                String loc = firstFeature.get(3) + ":" + firstFeature.get(4)
                        + ".." + firstFeature.get(5);

                String firstSoTerm = featureTypeToSOTermMap.get(firstFeature.get(2)).get(0);
                String firstSoTermDes = featureTypeToSOTermMap.get(firstFeature.get(2)).get(1);
                firstSoTermDes = firstSoTermDes.replaceAll("'", "\\\\'");

                // hack - feature name is null, use id
                if (firstFeature.get(0) == null || "".equals(firstFeature.get(0))) {
                    sb.append("<tr><td valign='top' rowspan='" + length + "'><b>" + span
                            + "</b><br>"
                            + "<div style='align:center; padding-bottom:12px'>"
                            + "<span class='fakelink exportDiv'> Export data </span>"
                            + "<img class='exportDiv' style='position:relative; top:3px;' "
                            + "border='0' src='model/images/download.png' title='export data' "
                            + "height='18' width='18'/><ul class='contextMenu'><li class='tab'>"
                            + "<a href='#javascript: exportFeatures(\"" + span + "\", "
                            + "\"SequenceFeature\", \"tab\");' class='ext_link'>TAB</a></li>"
                            + "<li class='csv'><a href='#javascript: exportFeatures(\"" + span
                            + "\", \"SequenceFeature\", \"csv\");' class='ext_link'>CSV</a></li>"
                            + "<li class='gff'><a href='#javascript: exportFeatures(\"" + span
                            + "\", \"SequenceFeature\", \"gff3\");' class='ext_link'>GFF3</a>"
                            + "</li><li class='seq'><a href='#javascript: exportFeatures(\"" + span
                            + "\", \"SequenceFeature\", \"sequence\");' class='ext_link'>SEQ</a>"
                            + "</li></ul></div><div style='align:center'>"
                            + "<a href='javascript: exportToGalaxy(\"" + span + "\");' "
                            + "class='ext_link'> Export to Galaxy <img border='0' "
                            + "title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' "
                            + "class='arrow' style='height:5%; width:5%'></a></div></td><td>"
                            + "<a target='_blank' title='' href='"
                            + baseURL + "/" + path + "/report.do?id=" + firstFeature.get(0)
                            + "'><i>PrimaryIdentifier not avaliable</i></a></td><td>" + firstSoTerm
                            + "<a onclick=\"document.getElementById('ctxHelpTxt').innerHTML='"
                            + firstSoTerm + ": " + firstSoTermDes
                            + "';document.getElementById('ctxHelpDiv').style.display='';"
                            + "window.scrollTo(0, 0);return false\" title=\"" + firstSoTermDes
                            + "\"><img class=\"tinyQuestionMark\" "
                            + "src=\"images/icons/information-small-blue.png\" alt=\"?\"></a>"
                            + "</td><td>" + loc + "</td></tr>");
                } else {
                    sb.append("<tr><td valign='top' rowspan='" + length + "'><b>" + span
                            + "</b><br>"
                            + "<div style='align:center; padding-bottom:12px'>"
                            + "<span class='fakelink exportDiv'> Export data </span>"
                            + "<img class='exportDiv' style='position:relative; top:3px;' "
                            + "border='0' src='model/images/download.png' title='export data' "
                            + "height='18' width='18'/><ul class='contextMenu'><li class='tab'>"
                            + "<a href='#javascript: exportFeatures(\"" + span + "\", "
                            + "\"SequenceFeature\", \"tab\");' class='ext_link'>TAB</a></li>"
                            + "<li class='csv'><a href='#javascript: exportFeatures(\"" + span
                            + "\", \"SequenceFeature\", \"csv\");' class='ext_link'>CSV</a></li>"
                            + "<li class='gff'><a href='#javascript: exportFeatures(\"" + span
                            + "\", \"SequenceFeature\", \"gff3\");' class='ext_link'>GFF3</a>"
                            + "</li><li class='seq'><a href='#javascript: exportFeatures(\"" + span
                            + "\", \"SequenceFeature\", \"sequence\");' class='ext_link'>SEQ</a>"
                            + "</li></ul></div><div style='align:center'>"
                            + "<a href='javascript: exportToGalaxy(\"" + span + "\");' "
                            + "class='ext_link'> Export to Galaxy <img border='0' "
                            + "title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' "
                            + "class='arrow' style='height:5%; width:5%'></a></div></td><td>"
                            + "<a target='_blank' title='" + firstFeature.get(1) + "' href='"
                            + baseURL + "/" + path + "/portal.do?externalid=" + firstFeature.get(1)
                            + "&class=" + firstFeature.get(2) + "'>" + firstFeature.get(1)
                            + "</a></td><td>" + firstSoTerm
                            + "<a onclick=\"document.getElementById('ctxHelpTxt').innerHTML='"
                            + firstSoTerm + ": " + firstSoTermDes
                            + "';document.getElementById('ctxHelpDiv').style.display='';"
                            + "window.scrollTo(0, 0);return false\" title=\"" + firstSoTermDes
                            + "\"><img class=\"tinyQuestionMark\" "
                            + "src=\"images/icons/information-small-blue.png\" alt=\"?\"></a>"
                            + "</td><td>" + loc + "</td></tr>");
                }

                for (int i = 1; i < length; i++) {
                    String soTerm = featureTypeToSOTermMap.get(features.get(i).get(2)).get(0);
                    String soTermDes = featureTypeToSOTermMap.get(features.get(i).get(2)).get(1);
                    soTermDes = soTermDes.replaceAll("'", "\\\\'");

                    String location = features.get(i).get(3) + ":"
                            + features.get(i).get(4) + ".."
                            + features.get(i).get(5);

                    if (features.get(i).get(1) == null || "".equals(features.get(i).get(3))) {
                        sb.append("<tr><td><a target='_blank' title='' href='" + baseURL + "/"
                                + path + "/report.do?id=" + features.get(i).get(0)
                                + "'><i>PrimaryIdentifier not avaliable</i></a></td><td>" + soTerm
                                + "<a onclick=\"document.getElementById('ctxHelpTxt').innerHTML='"
                                + soTerm + ": " + soTermDes
                                + "';document.getElementById('ctxHelpDiv').style.display='';"
                                + "window.scrollTo(0, 0);return false\" title=\"" + soTermDes
                                + "\"><img class=\"tinyQuestionMark\" "
                                + "src=\"images/icons/information-small-blue.png\" alt=\"?\"></a>"
                                + "</td><td>" + location
                                + "</td></tr>");
                    } else {
                        sb.append("<tr><td><a target='_blank' title='"
                                + features.get(i).get(1)
                                + "' href='"
                                + baseURL + "/" + path
                                + "/portal.do?externalid=" + features.get(i).get(1)
                                + "&class=" + features.get(i).get(2) + "'>"
                                + features.get(i).get(1) + "</a></td><td>"
                                + soTerm
                                + "<a onclick=\"document.getElementById('ctxHelpTxt').innerHTML='"
                                + soTerm + ": " + soTermDes
                                + "';document.getElementById('ctxHelpDiv').style.display='';"
                                + "window.scrollTo(0, 0);return false\" title=\"" + soTermDes
                                + "\"><img class=\"tinyQuestionMark\" "
                                + "src=\"images/icons/information-small-blue.png\" alt=\"?\"></a>"
                                + "</td><td>" + location
                                + "</td></tr>");
                    }

                }
            } else {
                sb.append("<tr><td><b>"
                        + span
                        + "</b></td><td colspan='3'><i>No overlap features found</i></td></tr>");
            }
        }

        sb.append("</tbody>");

        return sb.toString();
    }

    /**
     * Calculate the number of matched bases.
     *
     * @param s a span object
     * @param r a list of attributes
     * @return matched base count as String
     */
    protected String getMatchedBaseCount(GenomicRegion s, List<String> r) {

        int spanStart = s.getStart();
        int spanEnd = s.getEnd();
        int featureStart = Integer.valueOf(r.get(3));
        int featureEnd = Integer.valueOf(r.get(4));

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
     *
     * @param featureIds set of feature intermine ids
     * @param featureType feature class name
     * @return a pathquery
     */
    public PathQuery getExportFeaturesQuery(Set<Integer> featureIds, String featureType) {

        PathQuery q = new PathQuery(SessionMethods
                .getInterMineAPI(request.getSession()).getModel());

        String path = featureType;
        q.addView(path + ".primaryIdentifier");
        q.addView(path + ".symbol");
        q.addView(path + ".chromosomeLocation.locatedOn.primaryIdentifier");
        q.addView(path + ".chromosomeLocation.start");
        q.addView(path + ".chromosomeLocation.end");
        q.addView(path + ".organism.name");

        q.addConstraint(Constraints.inIds(featureType, featureIds), "A");

        return q;
    }

    /**
     *
     * @param organisms set of org names
     * @return set of taxonIds
     */
    public Set<Integer> getTaxonIds(Set<String> organisms) {
        Set<Integer> taxIds = new HashSet<Integer>();

        for (String org : organisms) {
            taxIds.add(this.getOrgTaxonIdMap().get(org));
        }

        return taxIds;
    }
}
