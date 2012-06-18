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

import java.util.ArrayList;
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
import org.json.JSONArray;
import org.json.JSONObject;

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
                String liftOverResponse = los.doLiftOver(
                        grsService.getConstraint(), organism, genomeVersionSource,
                        genomeVersionTarget, liftOverServiceUrl);

                // parse the response in json
                List<GenomicRegion> liftedList = new ArrayList<GenomicRegion>();
                StringBuffer unmapped = new StringBuffer();
                try {
                    JSONObject json = new JSONObject(liftOverResponse);

                    JSONArray liftedArray = json.getJSONArray("coords");
                    JSONArray unmappedArray = json.getJSONArray("unmapped");

                    for (int i = 0; i < liftedArray.length(); i++) {
                        String coord = (String) liftedArray.get(i);
                        coord.trim();
                        GenomicRegion gr = new GenomicRegion();
                        gr.setOrganism(organism);
                        gr.setExtendedRegionSize(grsService.getConstraint()
                                .getExtendedRegionSize());
                        gr.setChr(coord.split("\t")[0].trim());
                        gr.setStart(Integer.valueOf(coord.split("\t")[1].trim()));
                        gr.setEnd(Integer.valueOf(coord.split("\t")[2].trim()));
                        liftedList.add(gr);
                    }

                    for (int i = 0; i < unmappedArray.length(); i++) {
                        String info = (String) unmappedArray.get(i);
                        info.trim();
                        if (info.startsWith("#")) { // e.g. "#Partially deleted in new\n"
                            unmapped.append(info.subSequence(1, info.length() - 1))
                                    .append(" - ");
                        } else {
                            String chr = info.split("\t")[0].trim();
                            String start = info.split("\t")[1].trim();
                            String end = info.split("\t")[2].trim();
                            unmapped.append(chr + ":" + start + ".." + end)
                                    .append("<br>");
                        }
                    }

                    if (!unmapped.toString().isEmpty()) {
                        request.setAttribute(
                                "liftOverStatus",
                                "Genomic region cannot be lifted:<br>"
                                + unmapped.toString().substring(
                                        0,
                                        unmapped.toString()
                                                .lastIndexOf("<br>")));
                    } else {
                        request.setAttribute(
                                "liftOverStatus", "All coordinates are lifted");
                    }

                    if (!liftedList.isEmpty()) {
                        grsService.getConstraint().setGenomicRegionList(liftedList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    request.setAttribute(
                            "liftOverStatus",
                            "<i>Genomic region coordinates are not lifted. " +
                            "liftOver service error, please contact system admin</i>");
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
