package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.bio.web.logic.GenomicRegionSearchService;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;

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

    private static final String GALAXY_SERVER_CONNECTION_ERROR =
        "Failed to fetch genomic region data from Galaxy server";

    private static final String GALAXY_SERVER_CONNECTION_SUCCESSFUL =
        "Fetched genomic region data from Galaxy server";

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

        // Get genomic interval data from Galaxy
        String galaxyDataUrl = request.getParameter("DATA_URL");
        String genomeBuild = request.getParameter("GENOME");

        if (galaxyDataUrl != null) {
            galaxyDataUrl = galaxyDataUrl.trim();
            genomeBuild = genomeBuild.trim();
            String galaxyInput = fetchGalaxyData(galaxyDataUrl);
            if (galaxyInput.equals(GALAXY_SERVER_CONNECTION_ERROR)) {
                // Error message sent to options page
                String errMsg = GALAXY_SERVER_CONNECTION_ERROR + ": " + galaxyDataUrl;
                request.setAttribute("galaxyFetchDataError", errMsg);
            } else {
                // Escape javascript for newline breaks
                //galaxyInput = galaxyInput.replaceAll("\\s+", "\t"); // replace all space
                galaxyInput = galaxyInput.replaceAll("( )+", "\t"); // replace space except newline
                galaxyInput = StringEscapeUtils.escapeJavaScript(galaxyInput);

                request.setAttribute("galaxyIntervalData", galaxyInput);
                request.setAttribute("galaxyIntervalDataGenomeBuild", genomeBuild);


                // Success message sent to options page
                String successMsg = GALAXY_SERVER_CONNECTION_SUCCESSFUL + ": "
                        + galaxyDataUrl + "<br>Genome Build: " + genomeBuild + "<br>Format: bed";
//                successMsg = StringEscapeUtils.escapeJavaScript(successMsg);
                request.setAttribute("galaxyFetchDataSuccess", successMsg);
            }
        }

        return null;
    }

    private String fetchGalaxyData(String galaxyDataUrl) {

        String galaxyInput;

        try {
            URL url = new URL(galaxyDataUrl);
            URLConnection conn = url.openConnection();
            galaxyInput = new String(IOUtils.toCharArray(conn.getInputStream())).trim();
        } catch (Exception e) {
            e.printStackTrace();
            return GALAXY_SERVER_CONNECTION_ERROR;
        }

        return galaxyInput;
    }
}
