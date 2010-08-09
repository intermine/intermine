package org.intermine.web.struts;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.search.KeywordSearch;
import org.intermine.web.search.KeywordSearchHit;

import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;

/**
 * controller for keyword search
 * @author nils
 */
public class KeywordSearchResultsController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(KeywordSearchResultsController.class);

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        long time = System.currentTimeMillis();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        KeywordSearch.initKeywordSearch(im);
        Vector<KeywordSearchResult> searchResultsParsed = new Vector<KeywordSearchResult>();
        Vector<KeywordSearchFacet> searchResultsFacets = new Vector<KeywordSearchFacet>();
        Map<String, String> facets = KeywordSearch.getFacets();
        int totalHits = 0;

        WebConfig webconfig = SessionMethods.getWebConfig(request);
        Model model = im.getModel();
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();

        // term
        String searchTerm = request.getParameter("searchTerm");
        LOG.info("SEARCH TERM: '" + searchTerm + "'");

        // search in bag (list)
        String searchBag = request.getParameter("searchBag");
        if (searchBag == null) {
            searchBag = "";
        }
        LOG.info("SEARCH BAG: '" + searchBag + "'");

        // offset (-> paging)
        int offset = 0;
        try {
            if (!StringUtils.isBlank(request.getParameter("searchOffset"))) {
                offset = Integer.valueOf(request.getParameter("searchOffset"));
            }
        } catch (NumberFormatException e) {
            LOG.info("invalid offset", e);
        }
        LOG.info("SEARCH OFFSET: " + offset + "");

        // faceting - find all parameters that begin with facet_ and have a
        // value, add them to map
        HashMap<String, String> facetValues = new HashMap<String, String>();
        for (Entry<String, String[]> requestParameter : ((Map<String, String[]>) request
                .getParameterMap()).entrySet()) {
            if (requestParameter.getKey().startsWith("facet_")
                    && facets.containsKey(requestParameter.getKey().substring("facet_".length()))
                    && requestParameter.getValue().length > 0) {
                facetValues.put(requestParameter.getKey().substring("facet_".length()),
                        requestParameter.getValue()[0]);
            }
        }

        for (Entry<String, String> facetValue : facetValues.entrySet()) {
            LOG.info("SEARCH FACET: " + facetValue.getKey() + " = " + facetValue.getValue());
        }

        LOG.info("Initializing took " + (System.currentTimeMillis() - time) + " ms");

        if (!StringUtils.isBlank(searchTerm) && !searchTerm.trim().equals('*')) {
            // KeywordSearch.runLuceneSearch(searchTerm); //TODO remove - just
            // for performance testing

            Vector<KeywordSearchHit> searchHits = new Vector<KeywordSearchHit>();
            BrowseResult result = KeywordSearch.runBrowseSearch(searchTerm, offset, facetValues);

            long time2 = System.currentTimeMillis();
            if (result != null) {
                totalHits = result.getNumHits();
                LOG.info("Browse found " + result.getNumHits() + " hits");

                BrowseHit[] browseHits = result.getHits();

                HashSet<Integer> objectIds = new HashSet<Integer>();
                for (BrowseHit browseHit : browseHits) {
                    try {
                        Document doc = browseHit.getStoredFields();
                        if (doc != null) {
                            objectIds.add(Integer.valueOf(doc.getFieldable("id").stringValue()));
                        } else {
                            LOG.error("doc is null for browseHit " + browseHit);
                        }
                    } catch (NumberFormatException e) {
                        LOG.info("Invalid id '" + browseHit.getField("id") + "' for hit '"
                                + browseHit + "'", e);
                    }
                }

                LOG.info("Getting IDs took " + (System.currentTimeMillis() - time2) + " ms");
                time2 = System.currentTimeMillis();

                // fetch objects for the IDs returned by lucene search
                Map<Integer, InterMineObject> objMap = new HashMap<Integer, InterMineObject>();
                for (InterMineObject obj : im.getObjectStore().getObjectsByIds(objectIds)) {
                    objMap.put(obj.getId(), obj);
                }

                LOG.info("Getting objects took " + (System.currentTimeMillis() - time2) + " ms");
                time2 = System.currentTimeMillis();

                for (BrowseHit browseHit : browseHits) {
                    try {
                        Document doc = browseHit.getStoredFields();
                        if (doc != null) {
                            InterMineObject obj =
                                    objMap.get(Integer
                                            .valueOf(doc.getFieldable("id").stringValue()));

                            searchHits.add(new KeywordSearchHit(browseHit.getScore(), doc, obj));
                        } else {
                            LOG.error("doc is null for browseHit " + browseHit);
                        }
                    } catch (NumberFormatException e) {
                    }
                }

                LOG.info("Creating list of search hits took "
                        + (System.currentTimeMillis() - time2) + " ms");
                time2 = System.currentTimeMillis();

                for (KeywordSearchHit keywordSearchHit : searchHits) {
                    Class<?> objectClass =
                            DynamicUtil.decomposeClass(keywordSearchHit.getObject().getClass())
                                    .iterator().next();
                    ClassDescriptor classDescriptor =
                            model.getClassDescriptorByName(objectClass.getName());

                    searchResultsParsed.add(new KeywordSearchResult(webconfig, keywordSearchHit
                            .getObject(), classKeys, classDescriptor, keywordSearchHit.getScore()));
                }

                LOG
                        .info("Parsing search hits took " + (System.currentTimeMillis() - time2)
                                + " ms");
                time2 = System.currentTimeMillis();

                for (Entry<String, FacetAccessible> resultFacet : result.getFacetMap().entrySet()) {
                    searchResultsFacets.add(new KeywordSearchFacet(resultFacet.getKey(), facets
                            .get(resultFacet.getKey()), facetValues.get(resultFacet.getKey()),
                            resultFacet.getValue().getFacets()));
                }

                LOG.info("Parsing " + searchResultsFacets.size() + " facets took "
                        + (System.currentTimeMillis() - time2) + " ms");
                time2 = System.currentTimeMillis();
            }
        }

        LOG.info("SEARCH RESULTS: " + searchResultsParsed.size());

        request.setAttribute("searchResults", searchResultsParsed);
        request.setAttribute("searchTerm", searchTerm);

        if (!StringUtils.isBlank(searchTerm)) {
            context.putAttribute("searchTerm", searchTerm);
            context.putAttribute("searchResults", request.getAttribute("searchResults"));

            // pagination
            context.putAttribute("searchOffset", offset);
            context.putAttribute("searchPerPage", KeywordSearch.PER_PAGE);
            context.putAttribute("searchTotalHits", totalHits);

            // facet lists
            context.putAttribute("searchFacets", searchResultsFacets);

            // facet values
            for (Entry<String, String> facetValue : facetValues.entrySet()) {
                context.putAttribute("facet_" + facetValue.getKey(), facetValue.getValue());
            }
        }

        LOG.info("--> TOTAL: " + (System.currentTimeMillis() - time) + " ms");

        return null;
    }
}