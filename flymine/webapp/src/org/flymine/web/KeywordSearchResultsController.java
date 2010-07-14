package org.flymine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
import org.intermine.bio.search.KeywordSearch;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;

public class KeywordSearchResultsController extends TilesAction
{

    private static final Logger LOG = Logger.getLogger(KeywordSearchResultsController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
            @SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        KeywordSearch.initKeywordSearch(im);
        Vector<KeywordSearchResult> searchResultsParsed = new Vector<KeywordSearchResult>();

        WebConfig webconfig = SessionMethods.getWebConfig(request);
        Model model = im.getModel();
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();

        String searchTerm = request.getParameter("searchTerm");
        LOG.info("SEARCH TERM: '" + searchTerm + "'");

        if (!StringUtils.isBlank(searchTerm) && !searchTerm.trim().equals('*')) {
            Map<Integer, Float> searchResults = KeywordSearch.runLuceneSearch(searchTerm);

            Set<Integer> objectIds = searchResults.keySet();

            LOG.info("SEARCH HITS: " + searchResults.size());

            // fetch objects for the IDs returned by lucene search
            Map<Integer, InterMineObject> objMap = new HashMap<Integer, InterMineObject>();
            for (InterMineObject obj : im.getObjectStore().getObjectsByIds(objectIds)) {
                objMap.put(obj.getId(), obj);
            }

            LOG.info("SEARCH OBJMAP: " + objMap.size());

            for (Map.Entry<Integer, Float> entry : searchResults.entrySet()) {
                Class<?> objectClass = DynamicUtil.decomposeClass(
                        objMap.get(entry.getKey()).getClass()).iterator().next();
                ClassDescriptor classDescriptor = model.getClassDescriptorByName(objectClass
                        .getName());

                searchResultsParsed.add(new KeywordSearchResult(webconfig, objMap.get(entry
                        .getKey()), classKeys, classDescriptor, entry.getValue()));
            }
        }

        LOG.info("SEARCH RESULTS: " + searchResultsParsed.size());

        request.setAttribute("searchResults", searchResultsParsed);
        request.setAttribute("searchTerm", searchTerm);

        if (!StringUtils.isBlank(searchTerm)) {
            context.putAttribute("searchTerm", searchTerm);
            context.putAttribute("searchResults", request.getAttribute("searchResults"));

            if (searchResultsParsed.size() == KeywordSearch.MAX_HITS) {
                context.putAttribute("displayMax", KeywordSearch.MAX_HITS);
            }
        }

        return null;
    }
}