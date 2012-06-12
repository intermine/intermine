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

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.bio.web.logic.GenomicRegionSearchQueryRunner;
import org.intermine.bio.web.logic.GenomicRegionSearchService;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.logic.LiftOverService;
import org.intermine.bio.web.model.ChromosomeInfo;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.objectstore.query.Query;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;

/**
 * For genomic region search.
 *
 * @author Fengyuan Hu
 *
 */
public class GenomicRegionSearchAction extends InterMineAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(GenomicRegionSearchAction.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        GenomicRegionSearchForm grsForm = (GenomicRegionSearchForm) form;

        String organism = (String) grsForm.get("organism");

        if ("".equals(organism)) {
            recordError(new ActionMessage("genomicRegionSearch.organismEmpty"), request);
            return mapping.findForward("genomicRegionSearchOptions");
        }

        String spanUUIDString = UUID.randomUUID().toString(); // Generate UUID
        request.setAttribute("spanUUIDString", spanUUIDString);

        GenomicRegionSearchService grsService = GenomicRegionSearchUtil
                .getGenomicRegionSearchService(request);

        // Parse form
        ActionMessage actmsg = grsService.parseGenomicRegionSearchForm(grsForm);
        if (actmsg != null) {
            recordError(actmsg, request);
            return mapping.findForward("genomicRegionSearchOptions");
        }

        // LiftOver
        // TODO move to GenomicRegionSearchService, return liftedGenomicRegionMap
        Properties webProperties = SessionMethods.getWebProperties(
                request.getSession().getServletContext());
        String liftOver = webProperties.getProperty("genomicRegionSearch.liftOver");
        String liftOverServiceUrl = webProperties.getProperty("genomicRegionSearch.liftOver.url");

        if ("true".equals(liftOver) && !"".equals(liftOverServiceUrl)
                && liftOverServiceUrl != null) {

            String liftOverServiceAvailable = (String) grsForm.get("liftover-service-available");
            String genomeVersionSource = (String) grsForm.get("liftover-genome-version-source");
            String genomeVersionTarget = (String) grsForm.get("liftover-genome-version-target");

            if (genomeVersionSource.contains("/")) {
                genomeVersionSource =
                    genomeVersionSource.substring(genomeVersionSource.indexOf("/") + 1);
            }

            if (genomeVersionTarget.contains("/")) {
                genomeVersionTarget =
                    genomeVersionTarget.substring(genomeVersionTarget.indexOf("/") + 1);
            }

            // liftOverServiceAvailable == true
            // liftOver == true
            // url != null or empty
            // genomeVersionSource != null or empty
            // genomeVersionTarget != null or empty
            // genomeVersionSource != genomeVersionTarget
            if ("true".equals(liftOverServiceAvailable)
                    && "true".equals(liftOver)
                    && liftOverServiceUrl.length() > 0
                    && !genomeVersionSource.equals(genomeVersionTarget)) {

                LiftOverService los = new LiftOverService();
                Map<String, List<GenomicRegion>> liftedGenomicRegionMap = los.doLiftOver(
                        grsService.getConstraint(), organism, genomeVersionSource,
                        genomeVersionTarget, liftOverServiceUrl);

                // TODO verbose
                if (liftedGenomicRegionMap == null) {
                    // 1.service unavailable
                    // 2.fail to convert
                    request.setAttribute(
                            "liftOverStatus",
                            "<i>liftOver service is temporarily inaccessible, "
                            + "genomic region coordinates are not converted</i>");
                } else {
                    grsService.getConstraint().setGenomicRegionList(
                            liftedGenomicRegionMap.get("lifedGenomicRegions"));
                }
            }
        }

        // Genomic region validation
        Map<String, Map<String, ChromosomeInfo>> chrInfoMap = grsService
                .getChromosomeInfomationMap();
        Map<String, List<GenomicRegion>> resultMap = grsService.validateGenomicRegions();

        if (chrInfoMap != null && chrInfoMap.size() > 0 && resultMap != null) {

            String errorMsg = "";
            if (resultMap.get("error").size() == 0) {
                errorMsg = null;
            } else {
                String spanString = "";
                for (GenomicRegion span : resultMap.get("error")) {
                    spanString = spanString + span.getChr() + ":" + span.getStart()
                            + ".." + span.getEnd() + ", ";
                }
                errorMsg = "<b>Invalid genomic regions in <i>"
                        + grsService.getConstraint().getOrgName()
                        + "</i>:</b> " + spanString.substring(0, spanString.lastIndexOf(","));
            }

            if (resultMap.get("error").size() == grsService.getConstraint()
                    .getGenomicRegionList().size()) { // all genomic regions are invalid
                recordError(new ActionMessage("genomicRegionSearch.allRegionInvalid"), request);
                return mapping.findForward("genomicRegionSearchOptions");
            } else {
                grsService.getConstraint().setGenomicRegionList(resultMap.get("pass"));
            }

            request.setAttribute("errorMsg", errorMsg);
        }

        // regions will be extended in this step
        Map<GenomicRegion, Query> queryMap = grsService.createQueryList();

        GenomicRegionSearchQueryRunner grsqRunner = new GenomicRegionSearchQueryRunner(
                request, spanUUIDString, grsService.getConstraint(), queryMap);

        grsqRunner.search();

        // User selections
        request.setAttribute("selectionInfo", grsService.getSelectionInformation());

        // Results Page css
        request.setAttribute("resultsCss", grsService.getResultsCss());
        // Results Page javascript
        request.setAttribute("resultsJavascript", grsService.getResultsJavascript());

        return mapping.findForward("genomicRegionSearchResults");
    }
}
