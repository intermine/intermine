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

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Project;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Displayer for projectDetailsDisplayer.jsp
 *
 * @author Fengyuan Hu
 *
 */

public class ProjectDetailsDisplayer extends ReportDisplayer
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(ProjectDetailsDisplayer.class);

	public ProjectDetailsDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
		super(config, im);
	}

	@Override
	public void display(HttpServletRequest request, ReportObject reportObject) {

		Project p = (Project) reportObject.getObject();

        request.setAttribute("title", p.getTitle());
        request.setAttribute("orgs", p.getOrganisms());
        request.setAttribute("PI", p.getNamePI() + " " + p.getSurnamePI());
        request.setAttribute("url", p.getUrl());

	}
}
