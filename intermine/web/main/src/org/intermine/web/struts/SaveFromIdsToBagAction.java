package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.lucene.KeywordSearch;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.util.NameUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseResult;

/**
 * Saves selected items with InterMine ids and a Type in a new bag or combines
 * with existing bag.
 *
 * @author Fengyuan Hu
 */
public class SaveFromIdsToBagAction extends InterMineAction
{
    protected static final Logger LOG = Logger
            .getLogger(SaveFromIdsToBagAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);

        // where the request comes from, e.g. /experiment.do?...
        String source = (String) request.getParameter("source");
        Set<Integer> idSet = new LinkedHashSet<Integer>();
        try {
            String type = (String) request.getParameter("type");
            String allChecked = (String) request.getParameter("allChecked");

            if ("true".equals(allChecked)) {
                // TODO do something more clever than running the search again
                String totalHits = (String) request.getParameter("totalHits");
                int listSize = Integer.parseInt(totalHits);
                String searchTerm = (String) request.getParameter("searchTerm");
                JSONObject jsonRequest = new JSONObject(request.getParameter("jsonFacets"));
                Map<String, String> facetMap = jsonToJava(jsonRequest);
                int offset = 0;
                boolean pagination = false;
                BrowseResult result = KeywordSearch.runBrowseSearch(searchTerm, offset, facetMap,
                        new ArrayList<Integer>(), pagination, listSize);

                if (result != null) {
                    LOG.error("processing result! " + result.getNumHits());
                    BrowseHit[] browseHits = result.getHits();
                    LOG.error("browseHits " + browseHits.length);
                    idSet = KeywordSearch.getObjectIds(browseHits);
                    LOG.error("number of IDs " + idSet.size());

                } else {
                    LOG.error("NO RESULT");
                }
            } else {
                // ids are comma delimited
                String[] idArray = request.getParameter("ids").split(",");
                for (String id : idArray) {
                    idSet.add(Integer.valueOf(id.trim()));
                }
            }
            String bagName = request.getParameter("newBagName");
            if (bagName == null) {
                bagName = "new_list";
            }
            bagName = NameUtil.generateNewName(profile.getSavedBags().keySet(), bagName);
            InterMineAPI im = SessionMethods.getInterMineAPI(session);
            InterMineBag bag = profile.createBag(bagName, type, "", im.getClassKeys());
            bag.addIdsToBag(idSet, type);
            profile.saveBag(bag.getName(), bag);
            ForwardParameters forwardParameters = new ForwardParameters(
                    mapping.findForward("bagDetails"));
            return forwardParameters.addParameter("bagName", bagName).forward();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                recordError(new ActionMessage(e.toString()), request);

                ActionForward actionForward = mapping.findForward("bagDetails");
                ActionForward newActionForward = new ActionForward(
                        actionForward);
                if (request.getQueryString() == null) {
                    newActionForward.setPath("/" + source + ".do");
                } else {
                    newActionForward.setPath("/" + source + ".do?" + request.getQueryString());
                }
                return newActionForward;
            } catch (Exception ex) {
                ex.printStackTrace();
                recordError(new ActionMessage("Error...Please report..."),
                        request);

                return mapping.findForward("begin");
            }
        }
    }

    private Map<String, String> jsonToJava(JSONObject json) throws JSONException {
        JSONArray ja = json.getJSONArray("facets");
        Map<String, String> facets = new HashMap<String, String>();
        for (int i = 0; i < ja.length(); ++i) {
            JSONObject facet = ja.getJSONObject(i);
            String name = facet.getString("facetName");
            String value = facet.getString("facetValue");
            facets.put(name, value);
            LOG.error("faceting -- " + name + " value - " + value);
        }
        return facets;
    }
}
