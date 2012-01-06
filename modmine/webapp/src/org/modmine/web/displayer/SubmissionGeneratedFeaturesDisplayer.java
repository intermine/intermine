package org.modmine.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.MetadataCache;

/**
 * Displayer for submissionGeneratedFeaturesDisplayer.jsp
 *
 * @author julie sullivan
 * @author Fengyuan Hu
 * @author sc
 *
 */
public class SubmissionGeneratedFeaturesDisplayer extends ReportDisplayer
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SubmissionGeneratedFeaturesDisplayer.class);

    /**
     * Constructor
     *
     * @param config ReportDisplayerConfig
     * @param im InterMineAPI
     */
    public SubmissionGeneratedFeaturesDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        // Removed logics from SubmissionDisplayerController

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        final ServletContext servletContext = request.getSession().getServletContext();

        ObjectStore os = im.getObjectStore();

        // submission object
        Submission s = (Submission) reportObject.getObject();

        request.setAttribute("object", s);

        Map<String, Long> featureCounts =
                MetadataCache.getSubmissionFeatureCounts(os, s.getdCCid());

        request.setAttribute("featureCounts", featureCounts);

        Map<String, String> expFeatureDescription =
                MetadataCache.getFeatTypeDescription(servletContext);
        request.setAttribute("expFeatDescription", expFeatureDescription);


        Map<String, Map<String, Long>> expFeatEL =
                MetadataCache.getExperimentFeatureExpressionLevelCounts(os);
        request.setAttribute("expFeatEL", expFeatEL);

        Map<String, Map<String, Long>> subFeatEL =
                MetadataCache.getSubmissionFeatureExpressionLevelCounts(os);
        request.setAttribute("subFeatEL", subFeatEL);

        Map<String, Map<String, Map<String, Long>>> subFeatFileSource =
                MetadataCache.getSubFileSourceCounts(os);
        request.setAttribute("subFeatFileSource", subFeatFileSource);

        Map<String, List<String>> unlocatedFeatureTypes =
                MetadataCache.getUnlocatedFeatureTypes(os);
        request.setAttribute("unlocatedFeat", unlocatedFeatureTypes);

    }
}
