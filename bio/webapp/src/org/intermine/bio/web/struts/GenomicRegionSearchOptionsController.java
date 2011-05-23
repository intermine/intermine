package org.intermine.bio.web.struts;

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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.bio.web.logic.GenomicRegionSearchService;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Class to prepare data for genomicRegionSeachOptions.jsp.
 *
 * @author Fengyuan Hu
 *
 */
public class GenomicRegionSearchOptionsController extends TilesAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(GenomicRegionSearchOptionsController.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        session.setAttribute("tabName", "genomicRegionSearch");

        GenomicRegionSearchService grsService = GenomicRegionSearchUtil
                .getGenomicRegionSearchService(request);

        String webData = grsService.setupWebData();
        String optionsJavascript = grsService.getOptionsJavascript();

        request.setAttribute("webData", webData);
        request.setAttribute("optionsJavascript", optionsJavascript);

        return null;
    }
}
