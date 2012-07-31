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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Submission;
import org.intermine.web.logic.session.SessionMethods;

public class ModMineSearchResultsController extends TilesAction
{

    private static final Logger LOG = Logger.getLogger(ModMineSearchResultsController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
            @SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        ModMineSearch.initModMineSearch(im);
        LinkedHashMap<Submission, Integer> submissions = new LinkedHashMap<Submission, Integer>();

        String searchTerm = request.getParameter("searchTerm");
        LOG.info("SEARCH TERM: '" + searchTerm + "'");
        if (!StringUtils.isBlank(searchTerm) && !searchTerm.trim().equals('*')) {
            Map<Integer, Float> searchResults = ModMineSearch.runLuceneSearch(searchTerm);

            Set<Integer> objectIds = searchResults.keySet();

            LOG.info("SEARCH HITS: " + searchResults.size());

            Map<Integer, Submission> objMap = new HashMap<Integer, Submission>();
            for (InterMineObject obj : im.getObjectStore().getObjectsByIds(objectIds)) {
                objMap.put(obj.getId(), (Submission) obj);
            }
            LOG.info("SEARCH - OBJS: " + objMap.size());

            for (Map.Entry<Integer, Float> entry : searchResults.entrySet()) {
                //make sure scores are in the range [1, 10]
                submissions.put(objMap.get(entry.getKey()), new Integer(
                        Math.round(Math.max(0.1F, Math.min(1, entry.getValue())) * 10)));
            }
        }
        LOG.info("SEARCH SUBS: " + submissions.size());
        request.setAttribute("submissions", submissions);

        request.setAttribute("searchTerm", "THE SEARCH TERM");
        if (searchTerm != null) {
            context.putAttribute("searchTerm", searchTerm);
            context.putAttribute("submissions", request.getAttribute("submissions"));

            if (submissions.size() == ModMineSearch.MAX_HITS) {
                context.putAttribute("displayMax", ModMineSearch.MAX_HITS);
            }
        }
        return null;
    }
}