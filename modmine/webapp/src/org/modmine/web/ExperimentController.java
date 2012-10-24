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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.ResultFile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.GBrowseParser.GBrowseTrack;

/**
 * Set up modENCODE experiments for display.
 * @author Richard Smith
 *
 */

public class ExperimentController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        final ServletContext servletContext = servlet.getServletContext();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();

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

        Map<String, Set<ResultFile>> files = MetadataCache.getSubmissionFiles(os);
        request.setAttribute("files", files);

        Map<String, Integer> filesPerSub = MetadataCache.getFilesPerSubmission(os);
        request.setAttribute("filesPerSub", filesPerSub);

        Map<String, List<String[]>> submissionRepositoryEntries =
            MetadataCache.getRepositoryEntries(os);
        request.setAttribute("subRep", submissionRepositoryEntries);

        Map<String, List<String>> unlocatedFeatureTypes =
            MetadataCache.getUnlocatedFeatureTypes(os);
        request.setAttribute("unlocatedFeat", unlocatedFeatureTypes);

        Map<String, List<String>> sequencedFeatureTypes =
            MetadataCache.getSequencedFeatureTypes(os);
        request.setAttribute("sequencedFeat", sequencedFeatureTypes);

        Map<String, String> expFeatureDescription =
            MetadataCache.getFeatTypeDescription(servletContext);
        request.setAttribute("expFeatDescription", expFeatureDescription);

        Map<String, Map<String, Long>> subFeatEL =
            MetadataCache.getSubmissionFeatureExpressionLevelCounts(os);
        request.setAttribute("subFeatEL", subFeatEL);

        Map<String, Map<String, Long>> expFeatEL =
            MetadataCache.getExperimentFeatureExpressionLevelCounts(os);
        request.setAttribute("expFeatEL", expFeatEL);

        Map<String, Map<String, Map<String, Long>>> subFeatFileSource =
            MetadataCache.getSubFileSourceCounts(os);
        request.setAttribute("subFeatFileSource", subFeatFileSource);

        return null;
    }
}
