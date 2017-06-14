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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.search.Scope;
import org.intermine.api.search.SearchFilterEngine;
import org.intermine.api.search.SearchRepository;
import org.intermine.api.search.SearchResults;
import org.intermine.api.search.WebSearchable;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.TemplateManager;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.stringtree.json.JSONWriter;

/**
 * Controller for webSearchableList.tile
 * @author Kim Rutherford
 */

public class WebSearchableListController extends TilesAction
{

    private static final Logger LOG = Logger.getLogger(WebSearchableListController.class);

    private InterMineAPI im;
    private static TagManager tagManager;

    /**
     * Set up the attributes for webSearchableList.tile
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(
            ComponentContext context,
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String type = (String) context.getAttribute("type");
        String scope = (String) context.getAttribute("scope");
        String tags = (String) context.getAttribute("tags");
        String list = (String) context.getAttribute("list");
        String limit = (String) context.getAttribute("limit");
        String templatesPublicPage = (String) context.getAttribute("templatesPublicPage");
        Map<String, WebSearchable> filteredWebSearchables = new HashMap<String, WebSearchable>();
        HttpSession session = request.getSession();
        im = SessionMethods.getInterMineAPI(session);
        tagManager = im.getTagManager();
        if (type.equals(TagTypes.BAG)
            && im.getBagManager().isAnyBagToUpgrade(SessionMethods.getProfile(session))) {
            ActionMessages actionErrors = getErrors(request);
            actionErrors.add(ActionMessages.GLOBAL_MESSAGE,
                new ActionMessage("login.upgradeListManually"));
            saveErrors(request, actionErrors);
        }
        if (scope.equals(Scope.ALL)) {
            Map<String, ? extends WebSearchable> globalWebSearchables =
                getFilterWebSearchables(request, type, Scope.GLOBAL, tags);
            Map<String, ? extends WebSearchable> userWebSearchables =
                getFilterWebSearchables(request, type, Scope.USER, tags);
            filteredWebSearchables.putAll(userWebSearchables);
            filteredWebSearchables.putAll(globalWebSearchables);
        } else {
            filteredWebSearchables.putAll(getFilterWebSearchables(request, type, scope, tags));
        }

        if (list != null) {
            filteredWebSearchables = filterByList(filteredWebSearchables, list);
        }

        if (type.equals(TagTypes.BAG)) {
            filteredWebSearchables = filterByCurrent(filteredWebSearchables);
        }

        // shorten list to be < limit
        int limitInt = 0;
        if (limit != null) {
            try {
                limitInt = new Integer(limit.trim()).intValue();
            } catch (NumberFormatException e) {
                // ignore - don't shuffle
            }
        }

        if (limitInt > 0) {
            filteredWebSearchables = WebUtil.shuffle(filteredWebSearchables, limitInt);
        } else {
            if ("true".equals(templatesPublicPage)) {
                filteredWebSearchables = sortListByMostPopular(filteredWebSearchables, session);
            } else {
                filteredWebSearchables = sortList(filteredWebSearchables);
            }
        }

        Map<String, Object> wsMapForJS = new HashMap<String, Object>();

        SearchResults.filterOutInvalidTemplates(filteredWebSearchables);
        for (String wsName: (Set<String>) filteredWebSearchables.keySet()) {
            wsMapForJS.put(wsName, new Integer(1));
        }

        Profile profile = SessionMethods.getProfile(session);
        request.setAttribute("userWebSearchables", profile.getWebSearchablesByType(type));
        request.setAttribute("filteredWebSearchables", filteredWebSearchables);
        if (type.equals(TagTypes.BAG)) {
            ProfileManager pm = profile.getProfileManager();
            Map<String, InterMineBag> sharedBags = profile.getSharedBags();
            Map<String, String> sharedBagsByOwner = new HashMap<String, String>();
            for (Map.Entry<String, InterMineBag> entry : sharedBags.entrySet()) {
                int id = entry.getValue().getProfileId();
                Profile owner = pm.getProfile(id);
                sharedBagsByOwner.put(entry.getKey(), owner.getName());
            }
            request.setAttribute("sharedBagWebSearchables", sharedBagsByOwner);
        }
        JSONWriter jsonWriter = new JSONWriter();
        request.setAttribute("wsNames", jsonWriter.write(wsMapForJS));
        return null;
    }


    /**
     * Return a copy of the given Map sorted by creation date, then by name.
     */
    private Map<String, WebSearchable> sortList(final Map<String, WebSearchable>
        filteredWebSearchables) {

        Comparator<String> comparator = new Comparator<String>() {

            private List<Tag> resolveTagsInBag(InterMineBag bag) {
                @SuppressWarnings("deprecation")
                List<Tag> tags = tagManager.getTags(null, bag.getName(), TagTypes.BAG, null);
                return tags;
            }

            private Integer resolveOrderFromTagsList(List<Tag> tags) {
                for (Tag t : tags) {
                    String name = t.getTagName();
                    if (name.startsWith("im:order:")) {
                        return Integer.parseInt(name.replaceAll("[^0-9]", ""));
                    }
                }
                return 666;
            }

            public int compare(String o1, String o2) {
                WebSearchable ws1 = filteredWebSearchables.get(o1);
                WebSearchable ws2 = filteredWebSearchables.get(o2);
                if (ws1 instanceof InterMineBag && ws2 instanceof InterMineBag) {
                    InterMineBag bag1 = (InterMineBag) ws1;
                    InterMineBag bag2 = (InterMineBag) ws2;
                    return compareBags(bag1, bag2);
                } else if (ws1.getTitle().equals(ws2.getTitle())) {
                    return compareByName(ws1, ws2);
                } else {
                    return ws1.getTitle().compareTo(ws2.getTitle());
                }
            }

            private int compareBags(InterMineBag bag1, InterMineBag bag2) {
                Integer aO = resolveOrderFromTagsList(resolveTagsInBag(bag1));
                Integer bO = resolveOrderFromTagsList(resolveTagsInBag(bag2));

                if (aO < bO) {
                    return -1;
                } else {
                    if (aO > bO) {
                        return 1;
                    } else {
                        if (bag1.getDateCreated() != null && bag2.getDateCreated() != null) {
                            if (!bag1.getDateCreated().equals(bag2.getDateCreated())) {
                                return bag2.getDateCreated().compareTo(bag1.getDateCreated());
                            }
                            return compareByName(bag1, bag2);
                        }
                        return compareByName(bag1, bag2);
                    }
                }
            }

            private int compareByName(WebSearchable ws1, WebSearchable ws2) {
                if (!ws1.getName().equals(ws2.getName())) {
                    return ws1.getName().compareTo(ws2.getName());
                }
                // at the sort order doesn't matter, two same items
                return 1;
            }
        };

        Map<String, WebSearchable> sortedMap =
               new TreeMap<String, WebSearchable>(comparator);
        sortedMap.putAll(filteredWebSearchables);
        if (filteredWebSearchables.size() != sortedMap.size()) {
            LOG.error("Important error. Sorting of web searchables removed some items.");
        }
        return sortedMap;
    }

    private Map<String, WebSearchable> sortListByMostPopular(final Map<String, WebSearchable>
        filteredWebSearchables, HttpSession session) {
        TemplateManager tm = im.getTemplateManager();
        Profile profile = SessionMethods.getProfile(session);
        List<String> mostPopulareTemplateNames;
        if (profile.isLoggedIn()) {
            mostPopulareTemplateNames = tm.getMostPopularTemplateOrder(profile.getUsername(),
                                                                       session.getId(), null);
        } else {
            mostPopulareTemplateNames = tm.getMostPopularTemplateOrder(null);
        }
        if (mostPopulareTemplateNames == null) {
            return sortList(filteredWebSearchables);
        }
        MostPopularTemplateComparator comparator = new MostPopularTemplateComparator(
            mostPopulareTemplateNames, filteredWebSearchables);
        Map<String, WebSearchable> sortedMap =
            new TreeMap<String, WebSearchable>(comparator);
        sortedMap.putAll(filteredWebSearchables);
        if (filteredWebSearchables.size() != sortedMap.size()) {
            LOG.error("Important error."
                + "Sorting of web searchables by most popular removed some items.");
        }
        return sortedMap;
    }

    private class MostPopularTemplateComparator implements Comparator<String>
    {
        private List<String> mostPopulareTemplateNames;
        private Map<String, WebSearchable> filteredWebSearchables;

        public MostPopularTemplateComparator(List<String> mostPopulareTemplateNames,
            final Map<String, WebSearchable> filteredWebSearchables) {
            this.mostPopulareTemplateNames = mostPopulareTemplateNames;
            this.filteredWebSearchables = filteredWebSearchables;
        }

        public int compare(String templateName1, String templateName2) {
            if (!mostPopulareTemplateNames.contains(templateName1)
                && !mostPopulareTemplateNames.contains(templateName2)) {
                WebSearchable ws1 = filteredWebSearchables.get(templateName1);
                WebSearchable ws2 = filteredWebSearchables.get(templateName2);
                String t1 = ws1.getTitle();
                String t2 = ws2.getTitle();
                if (t1 == null || t2 == null || t1.equals(t2)) {
                    return ws1.getName().compareTo(ws2.getName());
                } else {
                    return ws1.getTitle().compareTo(ws2.getTitle());
                }
            }
            if (!mostPopulareTemplateNames.contains(templateName1)) {
                return +1;
            }
            if (!mostPopulareTemplateNames.contains(templateName2)) {
                return -1;
            }
            return (mostPopulareTemplateNames.indexOf(templateName1)
                   < mostPopulareTemplateNames.indexOf(templateName2)) ? -1 : 1;
        }
    }
    /**
     * Get all the WebSearchables in the given scope and of the given type.
     * @param request request
     * @param type type
     * @param scope  private or global scope
     * @param tags tags
     * @return websearchables of specified type, scope and with specified tags
     */
    public static Map<String, ? extends WebSearchable> getFilterWebSearchables(
            HttpServletRequest request, String type, String scope, String tags) {
        Map<String, ? extends WebSearchable> filteredWebSearchables;
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        ServletContext servletContext = session.getServletContext();

        Profile profile;
        // TODO what about "all" scopes?
        if (scope.equals(Scope.GLOBAL)) {
            profile = im.getProfileManager().getSuperuserProfile();
        } else {
            profile = SessionMethods.getProfile(session);
        }
        SearchRepository searchRepository;
        if (scope.equals(Scope.GLOBAL)) {
            searchRepository = SessionMethods.getGlobalSearchRepository(servletContext);
        } else {
            searchRepository = profile.getSearchRepository();
        }
        Map<String, ? extends WebSearchable> webSearchables =
            searchRepository.getWebSearchableMap(type);

        filteredWebSearchables = webSearchables;

        if (tags != null) {
            if (tags.length() > 0) {
                final List<String> tagList = Arrays.asList(StringUtil.split(tags.trim(), " "));
                filteredWebSearchables =
                    new SearchFilterEngine().filterByTags(filteredWebSearchables, tagList, type,
                                                          profile.getUsername(), tagManager, false);
            }
        }

        return filteredWebSearchables;
    }

    /**
     * Filters websearchables. Loops through the websearchables,
     * removes item if item is not on the list.
     * @param filteredWebSearchables items to be filtered
     * @param list list
     * @return map with items that were on the list and on the map as well
     */
    public static Map<String, WebSearchable> filterByList(
            Map<String, WebSearchable> filteredWebSearchables, String list) {

        Map<String, WebSearchable> clone = new HashMap<String, WebSearchable>();
        clone.putAll(filteredWebSearchables);

        String tmp = list.replaceAll(" ", "");
        String[] s = tmp.split(",");
        HashSet<String> set = new HashSet<String>();
        set.addAll(Arrays.asList(s));

        // iterate through map
        for (Object o : filteredWebSearchables.values()) {
            InterMineBag bag = (InterMineBag) o;
            ObjectStoreBag osb = bag.getOsb();
            Integer i = new Integer(osb.getBagId());
            // check that this is in our list
            if (!set.contains(i.toString())) {
                clone.remove(bag.getName());
            }
        }

        return clone;
    }

    private Map<String, WebSearchable> filterByCurrent(
            Map<String, WebSearchable> filteredWebSearchables) {
        Map<String, WebSearchable> clone = new HashMap<String, WebSearchable>();
        clone.putAll(filteredWebSearchables);
        for (Object o : filteredWebSearchables.values()) {
            InterMineBag bag = (InterMineBag) o;
            if (!bag.isCurrent()) {
                clone.remove(bag.getName());
            }
        }
        return clone;
    }
}
