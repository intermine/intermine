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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.logic.SpanOverlapQueryRunner;

/**
 * Controller Action for spanUploadOptions.jsp
 * Set up environment for the spanUploadOptions page. It is called immediately before the
 * spanUploadOptions.jsp is inserted.
 *
 * @author Fengyuan Hu
 *
 */

public class SpanUploadOptionsController extends TilesAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SpanUploadOptionsController.class);

    private static Map<String, String> subOrgMap = new HashMap<String, String>();

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();

        // Submission(DCCid)-LocatedFeatureTypes(Class) Map
        Map<String, List<String>> subFTMap = MetadataCache.getLocatedFeatureTypes(os);

        if ("facetedSearch".equals(request.getParameter("source"))
                && request.getParameter("submissions") != null && !request
                        .getParameter("submissions").trim().isEmpty()) {
            String subIdString = request.getParameter("submissions");
            // Assume no duplicated submission ids sent from faceted search
            List<String> subKeys = new ArrayList<String>();
            subKeys.addAll(Arrays.asList(StringUtil .split(subIdString, ",")));
            Set<String> orgSet = new TreeSet<String>();
            Map<String, List<String>> orgSubMap = new HashMap<String, List<String>>();
            List<String> invalidSubList = new ArrayList<String>(); // subs not in modMine
            Map<String, Set<String>> orgFtMap = new HashMap<String, Set<String>>();

            for (String sub : subKeys) {
                Submission subObj = MetadataCache.getSubmissionByDccId(os, sub);
                if (subObj == null) {
                    invalidSubList.add(sub);
                } else {
                    // Assume a submission always has a organsim
                    String orgName = MetadataCache.getSubmissionByDccId(os, sub)
                            .getOrganism().getShortName();

                    if (subOrgMap.get(sub) == null) {
                        orgSet.add(orgName);
                        subOrgMap.put(sub, orgName);
                    } else {
                        orgSet.add(subOrgMap.get(sub));
                    }

                    // Build orgSubMap
                    if (orgSubMap.containsKey(orgName)) {
                        orgSubMap.get(orgName).add(sub);
                    } else {
                        List<String> subList = new ArrayList<String>();
                        subList.add(sub);
                        orgSubMap.put(orgName, subList);
                    }

                    // Build orgFtMap
                    if (orgFtMap.containsKey(orgName)) {
                        if (subFTMap.get(sub) != null) {
                            orgFtMap.get(orgName).addAll(subFTMap.get(sub));
                        }
                    } else {
                        Set<String> ftSet = new TreeSet<String>();
                        if (subFTMap.get(sub) != null) {
                            ftSet.addAll(subFTMap.get(sub));
                        }
                        orgFtMap.put(orgName, ftSet);
                    }
                }
            }

            if (invalidSubList.size() > 0) {
                subKeys.removeAll(invalidSubList);
                subIdString = StringUtil.join(subKeys, ",");
            }

            request.setAttribute("source", "facetedSearch");
            orgSet.retainAll(SpanOverlapQueryRunner.getChrInfo(im).keySet());
            request.setAttribute("orgList", new ArrayList<String>(orgSet));
            request.setAttribute("orgSubMap", orgSubMap);
            request.setAttribute("submissions", subIdString);
            request.setAttribute("invalidSubList", invalidSubList);
            request.setAttribute("orgFtMap", orgFtMap);

        } else {
            // Why did I do this???
            session.setAttribute("tabName", "spanUpload");

            // >>>>> Get data from MetadataCache and CategoryExperiments <<<<<
            Set<String> orgSet = new HashSet<String>();

            // Category-Experiment Map, Category is ordered
            Map<String, List<DisplayExperiment>> cagExpMap = MetadataCache
                    .getCategoryExperiments(os);

            // Read GBrowse tracks
            MetadataCache.getGBrowseTracks();

            // Experiment-Category Map
            // One experiment can belong to different categories, make cag a list here
            Map<DisplayExperiment, List<String>> expCagMap =
                new HashMap<DisplayExperiment, List<String>>();
            for (String cag : cagExpMap.keySet()) {
                if (cagExpMap.get(cag) != null) {
                    for (DisplayExperiment exp : cagExpMap.get(cag)) {
                        if (expCagMap.get(exp) != null) {
                            expCagMap.get(exp).add(cag);
                        }
                        else {
                            List<String> cagList = new ArrayList<String>();
                            cagList.add(cag);
                            expCagMap.put(exp, cagList);
                        }
                    }
                }
            }

            // Experiment(name) - LocatedFeatureTypes(Class) Map
            Map<String, List<String>> expFTMap = new HashMap<String, List<String>>();

            // Set of DisplayExperiment objects
            Set<DisplayExperiment> expSet =
                new LinkedHashSet<DisplayExperiment>(MetadataCache.getExperiments(os));

            // A subset of expSet which contains experiments with feature types
            Set<DisplayExperiment> expWithFtSet = new LinkedHashSet<DisplayExperiment>();

            for (DisplayExperiment exp: expSet) {
                // Build organism set from DisplayExperiment set
                Set<String> orgs = exp.getOrganisms();
                orgSet.addAll(orgs);

                // Build Experiment(name) - FeatureTypes Map
                if (exp.getFeatureCountsRecords() != null) {
                    Set<String> ftSet = new LinkedHashSet<String>();
                    for (FeatureCountsRecord fcr : exp.getFeatureCountsRecords()) {
                        ftSet.add(fcr.getFeatureType());
                    }
                    expFTMap.put(exp.getName(), new ArrayList<String>(ftSet));
                    expWithFtSet.add(exp);

                }
            }

            // Sort orgSet to orgList in ascending order
            List<String> orgList = new ArrayList<String>(orgSet);
            Collections.sort(orgList);

            // >>>>> Prepare data <<<<<

            // Make a complex data structure
            //[org [cag [exp [sub {ft}]]]], [] for Map, {} for List
            Map<String, Map<String, Map<DisplayExperiment, Map<String, List<String>>>>> orgMap =
                new HashMap<String, Map<String, Map<DisplayExperiment, Map<String,
                    List<String>>>>>();
            for (String org : orgSet) {
                Map<String, Map<DisplayExperiment, Map<String, List<String>>>> cagMap =
                    new LinkedHashMap<String, Map<DisplayExperiment, Map<String, List<String>>>>();
                for (String cag : cagExpMap.keySet()) {
                    Map<DisplayExperiment, Map<String, List<String>>> expMap =
                         new LinkedHashMap<DisplayExperiment, Map<String, List<String>>>();
//                    for (DisplayExperiment exp : expSet) {
                    // Show only experiments with feature types
                    for (DisplayExperiment exp : expWithFtSet) {
                        // DisplayExperiment uses org short name
                        if (exp.getOrganisms().contains(org)
                                && (new HashSet<String>(expCagMap.get(exp)).contains(cag))) {
                            Map<String, List<String>> subMap =
                                new HashMap<String, List<String>>();
                            for (Submission sub : exp.getSubmissions()) {
                                if (subFTMap.containsKey(sub.getdCCid())) {
                                    // [sub {ft}]
                                    subMap.put(sub.getdCCid(), subFTMap.get(sub.getdCCid()));
                                }
                            }
                            // [exp [sub {ft}]]
                            expMap.put(exp, subMap);
                        }
                    }
                    // [cag [exp [sub {ft}]]]
                    cagMap.put(cag, expMap);
                }
                // [org [cag [exp [sub {ft}]]]]
                orgMap.put(org, cagMap);
            }

            // Only organisms with experiments (which must have features) will be shown
            Set<String> orgWithFTSet = new LinkedHashSet<String>();
            for (DisplayExperiment exp : expWithFtSet) {
                orgWithFTSet.addAll(exp.getOrganisms());
            }
            orgWithFTSet.retainAll(SpanOverlapQueryRunner.getChrInfo(im).keySet());
            List<String> orgWithFTList = new ArrayList<String>(orgWithFTSet);
            Collections.sort(orgList);

            request.setAttribute("orgList", orgWithFTList);
            request.setAttribute("expFTMap", expFTMap);
            request.setAttribute("orgMap", orgMap);
        }
        request.setAttribute("spanConstraint", "Organism");

        return null;
    }
}
