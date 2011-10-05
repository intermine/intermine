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
import java.text.DecimalFormat;
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
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.bio.web.model.ChromosomeInfo;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.GenomicRegionSearchConstraint;
import org.intermine.bio.web.struts.GenomicRegionSearchForm;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.WebConfig;
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
    private static final Logger LOG = Logger
            .getLogger(GenomicRegionSearchService.class);

    private InterMineAPI interMineAPI = null;
    private Model model = null;
    private ObjectStore objectStore = null;
    private Properties webProperties = null;
    private Profile profile = null;
    private WebConfig webConfig = null;
    private static String orgFeatureJSONString = "";
    private static final String GENOMIC_REGION_SEARCH_OPTIONS_DEFAULT =
        "genomic_region_search_options_default";
    private static final String GENOMIC_REGION_SEARCH_RESULTS_DEFAULT =
        "genomic_region_search_results_default";
    private static final int READ_AHEAD_CHARS = 10000;
    private GenomicRegionSearchConstraint grsc = null;
    // Key - organism : Value - map <key - chr : value - chromosome information>
    private static Map<String, Map<String, ChromosomeInfo>> chrInfoMap = null;
    private static Set<String> featureTypesInOrgs = null;
    private static Map<String, List<String>> featureTypeToSOTermMap = null;
    private static Map<String, Integer> orgTaxonIdMap = null;
    private List<String> selectionInfo = new ArrayList<String>();

    /**
     * Constructor
     */
    public GenomicRegionSearchService() {
    }

    /**
     * To set globally used variables.
     * @param request HttpServletRequest
     */
    public void init (HttpServletRequest request) {
        this.webProperties = SessionMethods.getWebProperties(
                request.getSession().getServletContext());
        this.webConfig = SessionMethods.getWebConfig(request);
        this.interMineAPI = SessionMethods.getInterMineAPI(request.getSession());
        this.profile = SessionMethods.getProfile(request.getSession());
        this.model = this.interMineAPI.getModel();
        this.objectStore = this.interMineAPI.getObjectStore();
    }

    /**
     * To call the queryOrganismAndSequenceFeatureTypes method in
     * GenomicRegionSearchQueryRunner.
     *
     * @return a JSON string
     * @throws Exception e
     */
    public String setupWebData() throws Exception {
        // By default, query all organisms in the database
        // pre defined organism short names can be read out from web.properties
        String presetOrganisms = webProperties.getProperty(
                "genomicRegionSearch.defaultOrganisms");

        List<String> orgList = new ArrayList<String>();

        Set<String> chrOrgSet = getChromosomeInfomationMap().keySet();

        if (chrOrgSet == null || chrOrgSet.size() == 0) {
            throw new RuntimeException(
                    "Chromosome location information is missing...");
        } else {
            if (presetOrganisms == null || "".equals(presetOrganisms)) {
                orgList = new ArrayList<String>(chrOrgSet);
            } else {
                // e.g. presetCollection [f,b,a], orgFromDBCollection [g,a,e,f]
                // results => [f,a] + [e,g] = [f,a,e,g]
                // some items in preset organisms will be removed if chro information not available

                List<String> presetOrgList = Arrays.asList(presetOrganisms
                        .split(","));
                List<String> trimedPresetOrgList = new ArrayList<String>();

                for (String s : presetOrgList) {
                    if (!"".equals(s.trim())) {
                        trimedPresetOrgList.add(s.trim());
                    }
                }

                // Don't remove any items from chrOrgSet, just make a copy
                Set<String> chrOrgSetCopy = new TreeSet<String>();
                for (String s : chrOrgSet) {
                    chrOrgSetCopy.add(s);
                }

                for (String o : trimedPresetOrgList) {
                    if (chrOrgSet.contains(o)) {
                        chrOrgSetCopy.remove(o);
                    }
                }

                trimedPresetOrgList.retainAll(chrOrgSet);

                orgList.addAll(trimedPresetOrgList);
                orgList.addAll(chrOrgSetCopy);
            }
        }

        // Exclude preset feature types (global) to display
        // Data should be comma separated class names
        String excludedFeatureTypes = webProperties.getProperty(
            "genomicRegionSearch.featureTypesExcluded.global");

        List<String> excludedFeatureTypeList = new ArrayList<String>();
        if (excludedFeatureTypes == null || "".equals(excludedFeatureTypes)) {
            excludedFeatureTypeList = null;
        } else {
            excludedFeatureTypeList = Arrays.asList(excludedFeatureTypes.split("[, ]+"));
        }

        if ("".equals(orgFeatureJSONString)) {
            orgFeatureJSONString = prepareWebData(orgList, excludedFeatureTypeList);
            return orgFeatureJSONString;
        } else {
            return orgFeatureJSONString;
        }
    }

    private String prepareWebData(List<String> orgList, List<String> excludedFeatureTypeList) {

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

        // constraints.addConstraint(new BagConstraint(qfOrgName,
        // ConstraintOp.IN, orgList));

        Results results = objectStore.execute(q);

        // Parse results data to a map
        Map<String, Set<String>> resultsMap = new LinkedHashMap<String, Set<String>>();
        Set<String> featureTypeSet = new LinkedHashSet<String>();

        // TODO this will be very slow when query too many features
        if (results == null || results.size() < 0) {
            return "";
        } else {
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
            // Exclude some feature types
            if (excludedFeatureTypeList != null) {
                ftSet.removeAll(excludedFeatureTypeList);
            }

            if (featureTypesInOrgs == null) {
                featureTypesInOrgs = new HashSet<String>();
                featureTypesInOrgs.addAll(ftSet);
            }
        }

        getFeatureTypeToSOTermMap();
        getOrganismToTaxonMap();

        // Parse data to JSON string
        List<Object> ft = new ArrayList<Object>();
        List<Object> gb = new ArrayList<Object>();
        Map<String, Object> ma = new LinkedHashMap<String, Object>();

        for (Entry<String, Set<String>> e : resultsMap.entrySet()) {
            Map<String, Object> mft = new LinkedHashMap<String, Object>();
            Map<String, Object> mgb = new LinkedHashMap<String, Object>();

            mft.put("organism", e.getKey());

            List<Object> featureTypeAndDespMapList = new ArrayList<Object>();
            for (String className : e.getValue()) {
                Map<String, String> featureTypeAndDespMap = new LinkedHashMap<String, String>();

                String soTermDes = "description not avaliable";
                List<String> soInfo = featureTypeToSOTermMap.get(className);

                if (soInfo != null) {
                    soTermDes = featureTypeToSOTermMap.get(className).get(1);
                }

                featureTypeAndDespMap.put("featureType", className);
                featureTypeAndDespMap.put("description", soTermDes);
                featureTypeAndDespMapList.add(featureTypeAndDespMap);
            }
            mft.put("features", featureTypeAndDespMapList);

            ft.add(mft);

            mgb.put("organism", e.getKey());
            mgb.put("genomeBuild",
                    (OrganismGenomeBuildLookup
                            .getGenomeBuildbyOrgansimAbbreviation(e.getKey()) == null)
                            ? "not available"
                            : OrganismGenomeBuildLookup
                                    .getGenomeBuildbyOrgansimAbbreviation(e
                                            .getKey()));

            gb.add(mgb);
        }

        ma.put("organisms", orgList);
        ma.put("genomeBuilds", gb);
        ma.put("featureTypes", ft);
        JSONObject jo = new JSONObject(ma);

        // Note: JSONObject toString will replace \' to \\', so don't convert it before the method
        //       was called. Replace "\" in java -> "\\\\"

        String preDataStr = jo.toString();
        preDataStr = preDataStr.replaceAll("'", "\\\\'");

        return preDataStr;
    }

    /**
     * Get the name of customized options javascript, by default, it is named
     * "genomic_region_search_options_default.js"
     *
     * @return the name of options javascript name
     */
    public String getOptionsJavascript() {
        String optionsJavascriptName = webProperties
                .getProperty("genomicRegionSearch.optionsJavascript");

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
        String resultsJavascriptName = webProperties
                .getProperty("genomicRegionSearch.resultsJavascript");

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
        String optionsCssName = webProperties.getProperty(
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
        String resultsCssName = webProperties.getProperty(
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
    public ActionMessage parseGenomicRegionSearchForm(
            GenomicRegionSearchForm grsForm) {
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
        String extendedRegionSize = (String) grsForm.get("extendedRegionSize");

        // Organism
        grsc.setOrgName(organism);
        grsc.setExtededRegionSize(Integer.parseInt(extendedRegionSize));

        selectionInfo.add("<b>Selected organism: </b><i>" + organism + "</i>");

        // Feature types
        if (featureTypes == null) {
            return new ActionMessage("genomicRegionSearch.spanFieldSelection",
                    "feature types");
        }

        // featureTypes in this case are (the last bit of) class instead of
        // featuretype in the db table; gain the full name by Model.getQualifiedTypeName(className)
        Set<Class<?>> ftSet = new HashSet<Class<?>>();
        for (String f : featureTypes) {
            ClassDescriptor cld = model.getClassDescriptorByName(f);
            ftSet.add(cld.getType());
            for (ClassDescriptor subCld : model.getAllSubs(cld)) {
                ftSet.add(subCld.getType());
            }
        }

        String ftString = "";
        for (String aFeaturetype : featureTypes) {
            aFeaturetype = WebUtil.formatPath(aFeaturetype, interMineAPI, webConfig);
            ftString = ftString + aFeaturetype + ", ";
        }
        selectionInfo.add("<b>Selected feature types: </b>"
                + ftString.substring(0, ftString.lastIndexOf(", ")));

        if (Integer.parseInt(extendedRegionSize) > 0) {
            if (Integer.parseInt(extendedRegionSize) >= 1000) {
                selectionInfo.add("<b>Extend Regions: </b>"
                        + new DecimalFormat("#.##").format(Integer
                                .parseInt(extendedRegionSize) / 1000) + " kbp");
            } else {
                selectionInfo.add("<b>Extend Regions: </b>" + extendedRegionSize + "bp");
            }
        }

        grsc.setFeatureTypes(ftSet);

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
            if (pasteInput != null && pasteInput.length() != 0) {
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
                    return new ActionMessage("genomicRegionSearch.isNotText",
                            mimetype);
                }
                if (formFile.getFileSize() == 0) {
                    return new ActionMessage(
                            "genomicRegionSearch.noSpanFileOrEmpty");
                }
                try {
                    reader = new BufferedReader(new InputStreamReader(
                            formFile.getInputStream()));
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
                    return new ActionMessage("genomicRegionSearch.isNotText",
                            "binary");
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

            // Use regular expression to validate user's input:
            String ddotsRegex = "[^:]+: ?\\d+\\.\\.\\d+$"; // "chr:start..end" - [^:]+:\d+\.{2,}\d+
            String tabRegex = "[^\\t]+\\t\\d+\\t\\d+$"; // "chr:start-end" - [^:]+:\d+\-\d+
            String dashRegex = "[^:]+: ?\\d+\\-\\d+$"; // "chr(tab)start(tab)end" - [^\t]+\t\d+\t\d+
            String snpRegex = "[^:]+: ?\\d+$"; // "chr:singlePosition" - [^:]+:[\d]+$

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
                return new ActionMessage(
                        "genomicRegionSearch.spanInWrongformat", spanStr);
            }
            spanList.add(aSpan);
        }

        grsc.setGenomicRegionList(spanList);

        return null;
    }

    /**
     * To prepare queries for genomic regions
     *
     * @return a list of prepared queries for genomic regions
     */
    public Map<GenomicRegion, Query> createQueryList() {
        return GenomicRegionSearchUtil.createQueryList(
            grsc.getGenomicRegionList(),
            grsc.getExtendedRegionSize(),
            getChromosomeInfomationMap().get(grsc.getOrgName()),
            grsc.getOrgName(),
            grsc.getFeatureTypes());
    }

    /**
     * @return the grsc
     */
    public GenomicRegionSearchConstraint getConstraint() {
        return this.grsc;
    }

    /**
     * Get chromosome information as in a map, keys are lowercased chromosome ids
     * @return chrInfoMap
     */
    public Map<String, Map<String, ChromosomeInfo>> getChromosomeInfomationMap() {
        if (chrInfoMap == null) {
            chrInfoMap = GenomicRegionSearchQueryRunner
                    .getChromosomeInfo(interMineAPI, profile);
        }

        return chrInfoMap;
    }

    /**
     *
     * @return featureTypeToSOTermMap
     */
    public Map<String, List<String>> getFeatureTypeToSOTermMap() {

        if (featureTypeToSOTermMap == null) {
            featureTypeToSOTermMap = GenomicRegionSearchQueryRunner
                    .getFeatureAndSOInfo(interMineAPI);

            if (!(featureTypesInOrgs.size() == featureTypeToSOTermMap.size() && featureTypesInOrgs
                    .containsAll(featureTypeToSOTermMap.keySet()))) {
                Map<String, List<String>> newFeatureTypeToSOTermMap =
                    new HashMap<String, List<String>>();

                for (String ft : featureTypesInOrgs) {
                    if (featureTypeToSOTermMap.keySet().contains(ft)) {
                        newFeatureTypeToSOTermMap.put(ft,
                                featureTypeToSOTermMap.get(ft));
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
    public Map<String, Integer> getOrganismToTaxonMap() {
        if (orgTaxonIdMap == null) {
            orgTaxonIdMap = GenomicRegionSearchQueryRunner.getTaxonInfo(interMineAPI,
                    profile);
        }
        return orgTaxonIdMap;
    }

    /**
     * Validate input genomic regions
     * @return resultMap
     */
    public Map<String, List<GenomicRegion>> validateGenomicRegions() {
        /* the Map has two key-value mappings
         * PASS-ArrayList<passedSpan>
         * ERROR-ArrayList<errorSpan>
         */
        Map<String, List<GenomicRegion>> resultMap = new HashMap<String, List<GenomicRegion>>();
        List<GenomicRegion> passedSpanList = new ArrayList<GenomicRegion>();
        List<GenomicRegion> errorSpanList = new ArrayList<GenomicRegion>();

        Map<String, ChromosomeInfo> chrInfo = chrInfoMap.get(grsc.getOrgName());

        if (chrInfo == null) { // this should not happen
            return null;
        }

        // Create passedSpanList
        for (GenomicRegion gr : grsc.getGenomicRegionList()) {
            // User input could be x instead of X for human chromosome, converted to lowercase
            ChromosomeInfo ci = null;
            String chr = gr.getChr().toLowerCase();

            if (chrInfo.containsKey(chr)) {
                ci = chrInfo.get(chr);
            } else {
                if (chr.startsWith("chr")) { // UCSC format
                    if (chrInfo.containsKey(chr.substring(3))) {
                        ci = chrInfo.get(chr.substring(3));
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            if ((gr.getStart() >= 1 && gr.getStart() <= ci
                    .getChrLength())
                    && (gr.getEnd() >= 1 && gr.getEnd() <= ci
                            .getChrLength())) {
                if (gr.getStart() > gr.getEnd()) { // Start must be smaller than End
                    GenomicRegion newSpan = new GenomicRegion();
                    newSpan.setChr(ci.getChrPID()); // converted to the right case
                    newSpan.setStart(gr.getEnd());
                    newSpan.setEnd(gr.getStart());
                    passedSpanList.add(newSpan);
                } else {
                    gr.setChr(ci.getChrPID());
                    passedSpanList.add(gr);
                }
            }
        }

        // make errorSpanList
        errorSpanList.addAll(grsc.getGenomicRegionList());
        errorSpanList.removeAll(passedSpanList);

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

    /**
     * Get organism for GenomicRegionSearchAjaxAction use.
     *
     * @param spanUUIDString uuid
     * @param spanConstraintMap map of contraints
     * @return the organism
     */
    public String getGenomicRegionOrganismConstraint(String spanUUIDString,
            Map<GenomicRegionSearchConstraint, String> spanConstraintMap) {

        for (Entry<GenomicRegionSearchConstraint, String> e : spanConstraintMap
                .entrySet()) {
            if (e.getValue().equals(spanUUIDString)) {
                return e.getKey().getOrgName();
            }
        }
        return null;
    }

    /**
     * Get flanking size for GenomicRegionSearchAjaxAction use.
     *
     * @param spanUUIDString uuid
     * @param spanConstraintMap map of contraints
     * @return the flanking size
     */
    public int getGenomicRegionFlankingSizeConstraint(String spanUUIDString,
            Map<GenomicRegionSearchConstraint, String> spanConstraintMap) {

        for (Entry<GenomicRegionSearchConstraint, String> e : spanConstraintMap
                .entrySet()) {
            if (e.getValue().equals(spanUUIDString)) {
                return e.getKey().getExtendedRegionSize();
            }
        }
        return 0;
    }

    /**
     * Get a set of ids of a span's overlap features. for
     * GenomicRegionSearchAjaxAction use.
     *
     * @param grString a genomic region in string
     * @param flankingSize int value of extended genomic region size
     * @param resultMap map of search results
     * @return String feature ids joined by comma
     */
    public Set<Integer> getGenomicRegionOverlapFeaturesAsSet(String grString, int flankingSize,
            Map<GenomicRegion, List<List<String>>> resultMap) {

        Set<Integer> featureIdSet = new LinkedHashSet<Integer>();

        GenomicRegion spanToExport = new GenomicRegion();

        // spanString is extended span
        String[] temp = grString.split(":");
        spanToExport.setChr(temp[0]);
        temp = temp[1].split("\\.\\.");
        spanToExport.setExtendedStart(Integer.parseInt(temp[0]));
        spanToExport.setExtendedEnd(Integer.parseInt(temp[1]));
        spanToExport.setExtendedRegionSize(flankingSize);
        spanToExport.setStart(spanToExport.getExtendedStart()
                + spanToExport.getExtendedRegionSize());
        spanToExport.setEnd(spanToExport.getExtendedEnd() - spanToExport.getExtendedRegionSize());

        for (List<String> sf : resultMap.get(spanToExport)) {
            // the first element (0) is InterMine Id, second (1) is PID
            featureIdSet.add(Integer.valueOf(sf.get(0)));
        }

        return featureIdSet;
    }

    /**
     * Get a set of ids of a span's overlap features by given feature type. for
     * GenomicRegionSearchAjaxAction use.
     *
     * @param grString a genomic region in string
     * @param flankingSize int value of extended genomic region size
     * @param resultMap map of search results
     * @param featureType e.g. Gene
     * @return String feature ids joined by comma
     */
    public Set<Integer> getGenomicRegionOverlapFeaturesByType(String grString, int flankingSize,
            Map<GenomicRegion, List<List<String>>> resultMap, String featureType) {

        Set<Integer> featureIdSet = new LinkedHashSet<Integer>();

        GenomicRegion spanToExport = new GenomicRegion();

        // spanString is extended span
        String[] temp = grString.split(":");
        spanToExport.setChr(temp[0]);
        temp = temp[1].split("\\.\\.");
        spanToExport.setExtendedStart(Integer.parseInt(temp[0]));
        spanToExport.setExtendedEnd(Integer.parseInt(temp[1]));
        spanToExport.setExtendedRegionSize(flankingSize);
        spanToExport.setStart(spanToExport.getExtendedStart()
                + spanToExport.getExtendedRegionSize());
        spanToExport.setEnd(spanToExport.getExtendedEnd() - spanToExport.getExtendedRegionSize());

        for (List<String> sf : resultMap.get(spanToExport)) {
            // the first element (0) is InterMine Id, second (1) is PID, 4 featureType
            if (sf.get(3).equals(featureType)) {
                featureIdSet.add(Integer.valueOf(sf.get(0)));
            }
        }

        return featureIdSet;
    }

    /**
     * Get a set of ids of all span's overlap features by given feature type. for
     * GenomicRegionSearchAjaxAction use.
     *
     * @param resultMap map of search results
     * @param featureType e.g. Gene
     * @return String feature ids joined by comma
     */
    public Set<Integer> getAllGenomicRegionOverlapFeaturesByType(
            Map<GenomicRegion, List<List<String>>> resultMap, String featureType) {

        Set<Integer> featureIdSet = new LinkedHashSet<Integer>();

        for (Entry<GenomicRegion, List<List<String>>> e : resultMap.entrySet()) {

            if (e.getValue() != null) {
                for (List<String> sf : e.getValue()) {
                    if (sf.get(3).equals(featureType)) { // 3 featureType
                        featureIdSet.add(Integer.valueOf(sf.get(0))); // 0 id
                    }
                }
            }
        }

        return featureIdSet;
    }

    /**
     * Get a comma separated string of a span's overlap features. for
     * GenomicRegionSearchAjaxAction use.
     *
     * @param spanString span in string
     * @param flankingSize int value of extended genomic region size
     * @param resultMap map of search results
     * @return String feature ids joined by comma
     */
    public String getGenomicRegionOverlapFeaturesAsString(String spanString, int flankingSize,
            Map<GenomicRegion, List<List<String>>> resultMap) {

        Set<Integer> featureSet = getGenomicRegionOverlapFeaturesAsSet(
                spanString, flankingSize, resultMap);

        return StringUtil.join(featureSet, ",");
    }

    /**
     * Check whether the results have empty features. for
     * GenomicRegionSearchAjaxAction use.
     *
     * @param resultMap map of search results
     * @return String "hasFeature" or "emptyFeature"
     */
    public String isEmptyFeature(
            Map<GenomicRegion, List<List<String>>> resultMap) {
        for (List<List<String>> l : resultMap.values()) {
            if (l != null) {
                return "hasFeature";
            }
        }
        return "emptyFeature";
    }

    /**
     * Generate a html string with all feature type for list creation.
     *
     * @param resultMap map of search results
     * @return a html string
     */
    public String generateCreateListHtml(Map<GenomicRegion, List<List<String>>> resultMap) {

        Set<String> ftSet = new TreeSet<String>();

        for (List<List<String>> l : resultMap.values()) {
            if (l != null) {
                for (List<String> feature : l) {
                    ftSet.add(feature.get(3)); // the 3rd is feature type
                }
            }
        }

        String clHtml = " or <a href=\"javascript: createList('all','all-regions');\">"
            + "Create List by feature type:</a>"
            + "<select id=\"all-regions\" style=\"margin: 4px 3px\">";

        for (String ft : ftSet) {
            clHtml += "<option value=\"" + ft + "\">"
                    + WebUtil.formatPath(ft, interMineAPI, webConfig)
                    + "</option>";
        }

        clHtml += "</select>";

        return clHtml;
    }

    /**
     * Convert result map to HTML string.
     *
     * @param resultMap resultMap
     * @param genomicRegionList spanList
     * @param fromIdx offsetStart
     * @param toIdx offsetEnd
     * @param session the current session
     * @param orgName organism
     * @return a String of HTML
     */
    public String convertResultMapToHTML(
            Map<GenomicRegion, List<List<String>>> resultMap,
            List<GenomicRegion> genomicRegionList, int fromIdx, int toIdx,
            HttpSession session, String orgName) {

        String baseURL = webProperties.getProperty("webapp.baseurl");
        String path = webProperties.getProperty("webapp.path");

        List<GenomicRegion> subGenomicRegionList = genomicRegionList.subList(fromIdx, toIdx + 1);

        // start to build the html for results table
        StringBuffer sb = new StringBuffer();

        //TODO use HTML Template
        sb.append("<thead><tr valign=\"middle\">");
        sb.append("<th align=\"center\">Genome Region</th>");
        sb.append("<th align=\"center\">Feature</th>");
        sb.append("<th align=\"center\">Feature Type</th>");
        sb.append("<th align=\"center\">Location</th>");
        sb.append("</tr></thead>");
        sb.append("<tbody>");

        for (GenomicRegion s : subGenomicRegionList) {

            String span = s.getExtendedRegion();
            List<List<String>> features = resultMap.get(s);

            // get list of featureTypes
            String ftHtml = categorizeFeatureTypes(features, s);
            Set<String> ftSet = getFeatureTypeSet(features);

            /*
             * order: 0.id
             *        1.feature PID
             *        2.symbol
             *        3.feature type
             *        4.chr
             *        5.start
             *        6.end
             * see query fields in createQueryList method
             */
            if (features != null) {
                int length = features.size();
                List<String> firstFeature = features.get(0);

                String firstId = firstFeature.get(0);
                String firstPid = firstFeature.get(1);
                String firstSymbol = firstFeature.get(2);
                String firstFeatureType = firstFeature.get(3);
                String firstChr = firstFeature.get(4);
                String firstStart = firstFeature.get(5);
                String firstEnd = firstFeature.get(6);

                String loc = firstChr + ":" + firstStart + ".." + firstEnd;

                // translatedClassName
                String firstSoTerm = WebUtil.formatPath(firstFeatureType, interMineAPI,
                        webConfig);
                String firstSoTermDes = featureTypeToSOTermMap.get(firstFeatureType).get(1);
                firstSoTermDes = firstSoTermDes.replaceAll("'", "\\\\'");

                // hack - feature name is null, use id
                sb.append("<tr><td valign='top' rowspan='" + length + "'><b>" + span + "</b><br>");

                if (s.isRegionExtended()) {
                    String os = s.getOriginalRegion();
                    sb.append("<i>Original input: " + os + "</i><br>");
                }

                String facet = "SequenceFeature";
                if (ftSet != null) {
                    if (ftSet.size() == 1) {
                        facet = ftSet.iterator().next();
                    }
                }

                sb.append("<div style='align:center; padding-bottom:12px'>"
                        + "<span class='fakelink exportDiv'> Export data </span>"
                        + "<img class='exportDiv' style='position:relative; top:3px;' "
                        + "border='0' src='model/images/download.png' title='export data' "
                        + "height='18' width='18'/><ul class='contextMenu'><li class='tab'>"
                        + "<a href='#javascript: exportFeatures(\""
                        + span + "\", "
                        + "\"" + facet + "\", \"tab\");' class='ext_link'>TAB</a></li>"
                        + "<li class='csv'><a href='#javascript: exportFeatures(\""
                        + span
                        + "\", \"" + facet + "\", \"csv\");' class='ext_link'>CSV</a></li>"
                        + "<li class='gff'><a href='#javascript: exportFeatures(\""
                        + span
                        + "\", \"" + facet + "\", \"gff3\");' class='ext_link'>GFF3</a>"
                        + "</li><li class='seq'><a href='#javascript: exportFeatures(\""
                        + span
                        + "\", \"" + facet + "\", \"sequence\");' class='ext_link'>SEQ</a>"
                        + "</li></ul></div><div style='align:center'>"
                        + "<a href='javascript: exportToGalaxy(\"" + span + "\", \"" + orgName
                        + "\");' class='ext_link'> Export to Galaxy <img border='0' "
                        + "title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' "
                        + "class='arrow' style='height:5%; width:5%'></a></div>" + ftHtml
                        + "</td><td><a target='' title='' href='" + baseURL + "/" + path
                        + "/report.do?id=" + firstId + "'>");

                if (firstSymbol == null || "".equals(firstSymbol)) {
                    sb.append("<strong><i>unknown symbol</i></strong>");
                } else {
                    sb.append("<strong>" + firstSymbol + "</strong>");
                }

                sb.append(" ");

                if (firstPid == null || "".equals(firstPid)) {
                    sb.append("<span style='font-size: 11px;'><i>unknown DB identifier</i></span>");
                } else {
                    sb.append("<span style='font-size: 11px;'>" + firstPid + "</span>");
                }

                sb.append("</a></td><td>" + firstSoTerm
                        + "<a onclick=\"document.getElementById('ctxHelpTxt').innerHTML='"
                        + firstSoTerm + ": " + firstSoTermDes
                        + "';document.getElementById('ctxHelpDiv').style.display='';"
                        + "window.scrollTo(0, 0);return false\" title=\"" + firstSoTermDes
                        + "\"><img class=\"tinyQuestionMark\" "
                        + "src=\"images/icons/information-small-blue.png\" alt=\"?\"></a>"
                        + "</td><td>" + loc + "</td></tr>");


                for (int i = 1; i < length; i++) {

                    String id = features.get(i).get(0);
                    String pid = features.get(i).get(1);
                    String symbol = features.get(i).get(2);
                    String featureType = features.get(i).get(3);
                    String chr = features.get(i).get(4);
                    String start = features.get(i).get(5);
                    String end = features.get(i).get(6);

                    String soTerm = WebUtil.formatPath(featureType, interMineAPI,
                            webConfig);
                    String soTermDes = featureTypeToSOTermMap.get(featureType).get(1);
                    soTermDes = soTermDes.replaceAll("'", "\\\\'");

                    String location = chr + ":" + start + ".." + end;

                    sb.append("<tr><td><a target='' title='' href='"
                            + baseURL + "/" + path + "/report.do?id="  + id + "'>");

                    if (symbol == null || "".equals(symbol)) {
                        sb.append("<strong><i>unknown symbol</i></strong>");
                    } else {
                        sb.append("<strong>" + symbol + "</strong>");
                    }

                    sb.append(" ");

                    if (pid == null || "".equals(pid)) {
                        sb.append("<span style='font-size: 11px;'>"
                                +  "<i>unknown DB identifier</i></span>");
                    } else {
                        sb.append("<span style='font-size: 11px;'>" + pid + "</span>");
                    }

                    sb.append("</a></td><td>"
                            + soTerm
                            + "<a onclick=\"document.getElementById('ctxHelpTxt').innerHTML='"
                            + soTerm + ": " + soTermDes
                            + "';document.getElementById('ctxHelpDiv').style.display='';"
                            + "window.scrollTo(0, 0);return false\" title=\"" + soTermDes
                            + "\"><img class=\"tinyQuestionMark\" "
                            + "src=\"images/icons/information-small-blue.png\" alt=\"?\"></a>"
                            + "</td><td>" + location + "</td></tr>");
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
     * Get all feature types from a list of sequence features
     * @param features list of sequence features
     * @param s GenomicRegion
     * @return A html string with a dropdown list of feature types
     */
    public String categorizeFeatureTypes(List<List<String>> features, GenomicRegion s) {
        String id = s.getChr() + "-" + s.getStart() + "-" + s.getEnd();

        Set<String> ftSet = getFeatureTypeSet(features);

        if (ftSet == null) {
            return "";
        } else {
            String ftHtml = "<div>"
                + "<a href=\"javascript: createList('" + s.getExtendedRegion() + "', '" + id + "');\">"
                + "Create List by</a>"
                + "<select id=\"" + id + "\" style=\"margin: 4px 3px\">";

            for (String ft : ftSet) {
                ftHtml += "<option value=\"" + ft + "\">"
                        + WebUtil.formatPath(ft, interMineAPI, webConfig)
                        + "</option>";
            }

            ftHtml += "</select></div>";

            return ftHtml;
        }
    }

    /**
     * Get all feature types in a set
     * @param features list of sequence features
     * @param s GenomicRegion
     * @return a set of feature types of a genomic region
     */
    public Set<String> getFeatureTypeSet(List<List<String>> features) {
        Set<String> ftSet = null;

        if (features != null) {
            ftSet = new TreeSet<String>();

            for (List<String> feature : features) {
                ftSet.add(feature.get(3)); // the 3rd is feature type
            }
        }

        return ftSet;
    }

    /**
     * Calculate the number of matched bases.
     *
     * @param gr a GenomicRegion object
     * @param r a list of attributes
     * @return matched base count as String
     */
    protected String getMatchedBaseCount(GenomicRegion gr, List<String> r) {

        int spanStart = gr.getStart();
        int spanEnd = gr.getEnd();
        int featureStart = Integer.valueOf(r.get(3));
        int featureEnd = Integer.valueOf(r.get(4));

        int matchedBaseCount = 0;

        if (featureStart <= spanStart && featureEnd >= spanStart
                && featureEnd <= spanEnd) {
            matchedBaseCount = featureEnd - spanStart + 1;
        }

        if (featureStart >= spanStart && featureStart <= spanEnd
                && featureEnd >= spanEnd) {
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
     * A flexiable way of setting query fields.
     *
     * @param featureIds set of feature intermine ids
     * @param featureType feature class name
     * @param views user defined views in web.properties
     * @return a pathquery
     */
    public PathQuery getExportFeaturesQuery(Set<Integer> featureIds,
            String featureType, Set<String> views) {

        PathQuery q = new PathQuery(model);
        String path = featureType;

        if (views == null) {
            q.addView(path + ".primaryIdentifier");
            q.addView(path + ".symbol");
            q.addView(path + ".chromosomeLocation.locatedOn.primaryIdentifier");
            q.addView(path + ".chromosomeLocation.start");
            q.addView(path + ".chromosomeLocation.end");
            q.addView(path + ".organism.name");
        } else {
            for (String view : views) {
                q.addView(view.trim().replace("{0}", path));
            }
        }

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
            taxIds.add(this.getOrganismToTaxonMap().get(org));
        }

        return taxIds;
    }

    /**
     *  To serve UCSC Lift-Over
     */
    public void serveLiftOver() {

    }
}
