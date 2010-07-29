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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
import org.intermine.bio.search.KeywordSearch;
import org.intermine.bio.search.KeywordSearchHit;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;

/**
 * controller for keyword search
 * @author nils
 *
 */
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
        long time = System.currentTimeMillis();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        KeywordSearch.initKeywordSearch(im);
        Vector<KeywordSearchResult> searchResultsParsed = new Vector<KeywordSearchResult>();
        List<BrowseFacet> categoryFacets = null;
        List<BrowseFacet> organismFacets = null;
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

        // faceting
        String searchCategory = request.getParameter("searchCategory");
        if (searchCategory == null) {
            searchCategory = "";
        }
        LOG.info("SEARCH CATEGORY: '" + searchCategory + "'");

        String searchOrganism = request.getParameter("searchOrganism");
        if (searchOrganism == null) {
            searchOrganism = "";
        }
        LOG.info("SEARCH ORGANISM: '" + searchOrganism + "'");

        LOG.info("Initializing took " + (System.currentTimeMillis() - time) + " ms");

        if (!StringUtils.isBlank(searchTerm) && !searchTerm.trim().equals('*')) {
            String[] categoryValues;
            if (!StringUtils.isBlank(searchCategory)) {
                categoryValues = new String[] {searchCategory};
            } else {
                categoryValues = null;
            }

            String[] organismValues;
            if (!StringUtils.isBlank(searchOrganism)) {
                organismValues = new String[] {searchOrganism};
            } else {
                organismValues = null;
            }

            // KeywordSearch.runLuceneSearch(searchTerm); //TODO remove - just
            // for performance testing

            Vector<KeywordSearchHit> searchHits = new Vector<KeywordSearchHit>();
            BrowseResult result =
                    KeywordSearch.runBrowseSearch(searchTerm, offset, categoryValues,
                            organismValues);

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

                Map<String, FacetAccessible> facets = result.getFacetMap();
                categoryFacets = facets.get("Category").getFacets();
                organismFacets = facets.get("Organism").getFacets();

                LOG.info("Parsing facets took " + (System.currentTimeMillis() - time2) + " ms");
                time2 = System.currentTimeMillis();
            }
        }

        LOG.info("SEARCH RESULTS: " + searchResultsParsed.size());

        request.setAttribute("searchResults", searchResultsParsed);
        request.setAttribute("searchTerm", searchTerm);
        // request.setAttribute("searchOffset", searchOffset);
        // request.setAttribute("searchCategory", searchCategory);

        if (!StringUtils.isBlank(searchTerm)) {
            context.putAttribute("searchTerm", searchTerm);
            context.putAttribute("searchResults", request.getAttribute("searchResults"));

            // pagination
            context.putAttribute("searchOffset", offset);
            context.putAttribute("searchPerPage", KeywordSearch.PER_PAGE);
            context.putAttribute("searchTotalHits", totalHits);

            // facet lists
            if (categoryFacets != null) {
                context.putAttribute("categoryFacets", categoryFacets);
            }
            if (organismFacets != null) {
                context.putAttribute("organismFacets", organismFacets);
            }

            // facet values
            context.putAttribute("searchCategory", searchCategory);
            context.putAttribute("searchOrganism", searchOrganism);

            // if (searchResultsParsed.size() == KeywordSearch.MAX_HITS) {
            // context.putAttribute("displayMax", KeywordSearch.MAX_HITS);
            // }
        }

        LOG.info("--> TOTAL: " + (System.currentTimeMillis() - time) + " ms");

        return null;
    }
}