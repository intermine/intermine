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

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Submission;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Displayer for submissionDetailsDisplayer.jsp
 *
 * @author julie sullivan
 * @author Fengyuan Hu
 *
 */
public class SubmissionDetailsDisplayer extends CustomDisplayer
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(SubmissionDetailsDisplayer.class);

	public SubmissionDetailsDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
		super(config, im);
	}

	@Override
	public void display(HttpServletRequest request, ReportObject reportObject) {
		// Removed logics from SubmissionDisplayerController

        // submission object
        Submission s = (Submission) reportObject.getObject();

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

	}
}
