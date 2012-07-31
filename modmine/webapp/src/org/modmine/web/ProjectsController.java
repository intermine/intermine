package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.GBrowseParser.GBrowseTrack;

/**
 *
 * @author contrino
 *
 */

public class ProjectsController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(MetadataCache.class);
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        try {
            final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
            ObjectStore os = im.getObjectStore();
            final ServletContext servletContext = servlet.getServletContext();
            List<DisplayExperiment> experiments;
            String experimentName = request.getParameter("experiment");
            if (experimentName != null) {
                experiments = new ArrayList<DisplayExperiment>();
                experiments.add(MetadataCache.getExperimentByName(os, experimentName));
            } else {
                experiments = MetadataCache.getExperiments(os);
            }
            request.setAttribute("experiments", experiments);
            Map<String, List<GBrowseTrack>> tracks = MetadataCache.getExperimentGBrowseTracks(os);
            request.setAttribute("tracks", tracks);
            Map<String, List<GBrowseTrack>> subTracks = MetadataCache.getGBrowseTracks();
            request.setAttribute("subTracks", subTracks);


            Map<String, List<String[]>> submissionRepositoryEntries =
                MetadataCache.getRepositoryEntries(os);
            request.setAttribute("subRep", submissionRepositoryEntries);

            Map<String, List<String>> unlocatedFeatureTypes =
                MetadataCache.getUnlocatedFeatureTypes(os);
            request.setAttribute("unlocatedFeat", unlocatedFeatureTypes);

            Map<String, String> expFeatureDescription =
                MetadataCache.getFeatTypeDescription(servletContext);
            request.setAttribute("expFeatDescription", expFeatureDescription);
            
//            Map<String, List<DisplayExperiment>> categories =
//                MetadataCache.getCategoryExperiments(os);
//
//            Map <String, List<String>> expCat = new HashMap<String, List<String>>();
//            for (String cat : categories.keySet()) {
//                List<String> exps = new ArrayList<String>();
//                for (DisplayExperiment de : categories.get(cat)) {
//                    exps.add(de.getName());
//                }
//                expCat.put(cat, exps);
//            }
//
//            request.setAttribute("expCats", expCat);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
