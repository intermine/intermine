package org.modmine.web.displayer;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Submission;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Displayer for submissionDetailsDisplayer.jsp
 *
 * @author julie sullivan
 * @author Fengyuan Hu
 *
 */
public class SubmissionDetailsDisplayer extends ReportDisplayer
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SubmissionDetailsDisplayer.class);

    /**
     *
     * @param config ReportDisplayerConfig
     * @param im InterMineAPI
     */
    public SubmissionDetailsDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        // Removed logics from SubmissionDisplayerController

        Submission s = (Submission) reportObject.getObject();

        if (s.getRelatedSubmissions().size() > 0) {
            Set<Submission> subs = s.getRelatedSubmissions();

            Set<String> dCCidSet = new LinkedHashSet<String>();

            for (Submission sub : subs) {
                dCCidSet.add(sub.getdCCid());
            }

            request.setAttribute("relatedSubmissions", dCCidSet);
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy");
        String publicReleaseDate = formatter.format(s.getPublicReleaseDate());

        Date today = Calendar.getInstance().getTime();
        if (s.getEmbargoDate().after(today)) { // in the future
            request.setAttribute("embargoDate", formatter.format(s.getEmbargoDate()));
        }

        request.setAttribute("expType", s.getExperimentType());
        request.setAttribute("design", s.getDesign());
        request.setAttribute("dccId", s.getdCCid());
        request.setAttribute("publicReleaseDate", publicReleaseDate);
        request.setAttribute("qualityControl", s.getQualityControl());
        request.setAttribute("replicate", s.getReplicate());
        request.setAttribute("replacesSubmission", s.getReplacesSubmission());
        request.setAttribute("subId", s.getId());
        request.setAttribute("labId", s.getLab().getId());
        request.setAttribute("labName", s.getLab().getName());
        request.setAttribute("labAffiliation", s.getLab().getAffiliation());
        request.setAttribute("labProjectId", s.getLab().getProject().getId());
        request.setAttribute("labProjectName", s.getLab().getProject().getName());
        request.setAttribute("labProjectSurnamePI", s.getLab().getProject().getSurnamePI());
        request.setAttribute("organismId", s.getOrganism().getId());
        request.setAttribute("organismShortName", s.getOrganism().getShortName());
        request.setAttribute("experimentName", s.getExperiment().getName());
        request.setAttribute("subDescription", s.getDescription());
        request.setAttribute("rnaSize", s.getrNAsize());
        request.setAttribute("multiplyMappedReadCount", s.getMultiplyMappedReadCount());
        request.setAttribute("uniquelyMappedReadCount", s.getUniquelyMappedReadCount());
        request.setAttribute("totalReadCount", s.getTotalMappedReadCount());
        request.setAttribute("notice", s.getNotice());
        request.setAttribute("url", s.getUrl());

    }
}
