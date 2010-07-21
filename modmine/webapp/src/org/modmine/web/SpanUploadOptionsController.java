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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletContext;

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
 * Set up environment for the spanUploadOptions page. It is called immediately before the
 * spanUploadOptions.jsp is inserted.
 *
 * @author Fengyuan Hu
 *
 */

public class SpanUploadOptionsController extends TilesAction
{
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

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();

        final ServletContext servletContext = servlet.getServletContext();

        // >>>>> Get data from MetadataCache and CategoryExperiments <<<<<
        Set<String> orgSet = new HashSet<String>();

        // Category-Experiment Map
        Map<String, List<DisplayExperiment>> cagExpMap = CategoryExperiments
                .getCategoryExperiments(servletContext, os);




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

        // Submission-LocatedFeatureTypes Map
        Map<Integer, List<String>> subFTMap = MetadataCache.getLocatedFeatureTypes(os);

        // Set of DisplayExperiment objects
        Set<DisplayExperiment> expSet =
            new HashSet<DisplayExperiment>(MetadataCache.getExperiments(os));

        // Build organism set from DisplayExperiment set
        for (DisplayExperiment exp: expSet) {
            Set<String> orgs = exp.getOrganisms();
            orgSet.addAll(orgs);
        }

        // >>>>> Prepare data <<<<<
        // Organism-Org Tree Map
        Map<String, String> orgMap = new HashMap<String, String>();

        // Make a complex data structure
        //[org [cag [exp [sub {ft}]]]], [] for Map, {} for List
        Map<String, Map<String, Map<DisplayExperiment, Map<Integer, List<String>>>>> theMap =
            new HashMap<String, Map<String, Map<DisplayExperiment, Map<Integer,
            List<String>>>>>();
        for (String org : orgSet) {
            Map<String, Map<DisplayExperiment, Map<Integer, List<String>>>> cagMap =
                new HashMap<String, Map<DisplayExperiment, Map<Integer, List<String>>>>();
            for (String cag : cagExpMap.keySet()) {
                Map<DisplayExperiment, Map<Integer, List<String>>> expMap =
                     new HashMap<DisplayExperiment, Map<Integer, List<String>>>();
                for (DisplayExperiment exp : expSet) {
                    if (exp.getOrganisms().contains(org) // DisplayExperiment uses org short name
                            && (new HashSet<String>(expCagMap.get(exp)).contains(cag))) {
                        Map<Integer, List<String>> subMap =
                            new HashMap<Integer, List<String>>();
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
            theMap.put(org, cagMap);
        }

        // set trees as request attributes
        for (String org : orgSet) {
            //request.setAttribute(org, buildHtmlTree(orgMap, org));
            orgMap.put(org, buildHtmlTree(theMap, org));
        }

        // >>>>> Setup request <<<<<
        request.setAttribute("orgSet", orgSet);
        request.setAttribute("orgMap", orgMap);

        return null;
    }

    /**
     * Build a tree for spanUploadOptions.jsp use.
     * Write HTML tags in Java might not be a good idea. (Alternative options?)
     *
     * @param orgMap Organism-Org Tree Map
     * @param org organism name
     * @return aTree the HTML tree
     */
    private String buildHtmlTree(
            Map<String, Map<String, Map<DisplayExperiment, Map<Integer, List<String>>>>> orgMap,
            String org) {

        StringBuffer aTree = new StringBuffer();
        Set<String> ftSet = new HashSet<String>();

        // id = organism.shortname
        aTree.append("<ul id='" + org + "'>");

        Map<String, Map<DisplayExperiment, Map<Integer, List<String>>>> cagMap = orgMap.get(org);
        for (String cag : cagMap.keySet()) {
            aTree.append("<li><input type='checkbox' value='" + cag + "'/>");
            aTree.append(cag);
            aTree.append("<ul>");
            Map<DisplayExperiment, Map<Integer, List<String>>> expMap = cagMap.get(cag);
            for (DisplayExperiment exp : expMap.keySet()) {
                // Add all feature types to ftSet for one organism
                // a better scenario is - after users select experiments, the feature types change
                // according to the experiment, filter out the ft that don't belong to the selected
                // experiments, maybe not...
                for (Integer sub : expMap.get(exp).keySet()) {
                    ftSet.addAll(expMap.get(exp).get(sub));
                }
                // Continue building the tree
                aTree.append("<li><input type='checkbox' name='experiments' value='"
                    + exp.getName() + "'/>");
                aTree.append(exp.getName());
                aTree.append("</li>");
            }

            aTree.append("</ul></li>");
        }

        aTree.append("</ul>");

        // Add feature types to html as checkboxes
        // e.g.
        // <input type="checkbox" name="vehicle" value="Bike" /> I have a bike<br />
        // <input type="checkbox" name="vehicle" value="Car" /> I have a car
        aTree.append("<div id='featureType'>");
        for (String ft : ftSet) {
            aTree.append("<input type='checkbox' name='featureTypes' value='"
                    + ft + "'/>" + ft + "<br/>");
        }
        aTree.append("</div>");

        return aTree.toString();
    }
}
