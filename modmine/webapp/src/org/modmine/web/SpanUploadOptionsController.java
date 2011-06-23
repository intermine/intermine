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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.intermine.web.logic.session.SessionMethods;

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
        session.setAttribute("tabName", "spanUpload");

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();

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

        // Submission(DCCid)-LocatedFeatureTypes(Class) Map
        Map<String, List<String>> subFTMap = MetadataCache.getLocatedFeatureTypes(os);

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
//                for (DisplayExperiment exp : expSet) {
                // Show only experiments with feature types
                for (DisplayExperiment exp : expWithFtSet) {
                    if (exp.getOrganisms().contains(org) // DisplayExperiment uses org short name
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
        List<String> orgWithFTList = new ArrayList<String>(orgWithFTSet);
        Collections.sort(orgList);

        request.setAttribute("orgList", orgWithFTList);
        request.setAttribute("expFTMap", expFTMap);
        request.setAttribute("orgMap", orgMap);
        request.setAttribute("spanConstraint", "Organism");

        return null;
    }
}
