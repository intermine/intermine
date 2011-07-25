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
import org.intermine.bio.web.model.ChromosomeInfo;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.objectstore.query.Query;
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

        // UUID
        String spanUUIDString = UUID.randomUUID().toString();
        request.setAttribute("spanUUIDString", spanUUIDString);

        GenomicRegionSearchService grsService = GenomicRegionSearchUtil
                .getGenomicRegionSearchService(request);

        ActionMessage actmsg = grsService.parseGenomicRegionSearchForm(grsForm);
        if (actmsg != null) {
            recordError(actmsg, request);
            return mapping.findForward("genomicRegionSearch");
        }

        // Span validation
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
                request.setAttribute("noneValidGenomicRegions", "true");
                return mapping.findForward("genomicRegionSearchResults");
            } else {
                grsService.getConstraint().setGenomicRegionList(resultMap.get("pass"));
            }

            request.setAttribute("errorMsg", errorMsg);
        }

        Map<GenomicRegion, Query> queryMap = grsService.createQueryList();

        GenomicRegionSearchQueryRunner grsqRunner = new GenomicRegionSearchQueryRunner(
                request, spanUUIDString, grsService.getConstraint(), queryMap);

        grsqRunner.search();

        // User selections
        request.setAttribute("selectionInfo", grsService.getSelectionInformation());

        // Results Page css
        request.setAttribute("resultsCss", grsService.getResultsCss());

        return mapping.findForward("genomicRegionSearchResults");
    }
}
