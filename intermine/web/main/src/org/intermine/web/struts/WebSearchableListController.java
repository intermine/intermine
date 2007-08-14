package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.intermine.objectstore.query.ObjectStoreBag;

import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.search.Scope;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for webSearchableList.tile
 * @author Kim Rutherford
 */

public class WebSearchableListController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(WebSearchableListController.class);
    
    /**
     * Set up the attributes for webSearchableList.tile
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {
        String type = (String) context.getAttribute("type");
        String scope = (String) context.getAttribute("scope");
        String tags = (String) context.getAttribute("tags");
        String list = (String) context.getAttribute("list");
        String limit = (String) context.getAttribute("limit");
        Map filteredWebSearchables;
        
        HttpSession session = request.getSession();
     
        if (session.getAttribute("IS_SUPERUSER") != null 
                        && session.getAttribute("IS_SUPERUSER").equals(Boolean.TRUE)) {
            filteredWebSearchables = filterWebSearchables(request, type, 
                                                          TemplateHelper.USER_TEMPLATE, tags);
        
        } else if (scope.equals(TemplateHelper.ALL_TEMPLATE)) {
            Map globalWebSearchables =
                filterWebSearchables(request, type, TemplateHelper.GLOBAL_TEMPLATE, tags); 
            Map userWebSearchables =
                filterWebSearchables(request, type, TemplateHelper.USER_TEMPLATE, tags);
            filteredWebSearchables = new HashMap<String, WebSearchable>(userWebSearchables);
            filteredWebSearchables.putAll(globalWebSearchables);

        } else {
            filteredWebSearchables = filterWebSearchables(request, type, scope, tags);
       }

        if (list != null) {
            filteredWebSearchables = filterByList(filteredWebSearchables, list);
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
            filteredWebSearchables = WebUtil.shuffle(filteredWebSearchables,
                                                     limitInt);
        } else {
            filteredWebSearchables = sortList(filteredWebSearchables);
        }
        
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        request.setAttribute("userWebSearchables", profile.getWebSearchablesByType(type));
        
        request.setAttribute("filteredWebSearchables", filteredWebSearchables);
        return null;
    }

    /**
     * Return a copy of the given Map sorted by creation date, then by name. 
     */
    private Map sortList(final Map filteredWebSearchables) {
        Map sortedMap = new TreeMap<String, WebSearchable>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                WebSearchable ws1 = (WebSearchable) filteredWebSearchables.get(o1);
                WebSearchable ws2 = (WebSearchable) filteredWebSearchables.get(o2);
                if (ws1 instanceof InterMineBag) {
                    InterMineBag bag1 = (InterMineBag) ws1;
                    if (ws2 instanceof InterMineBag) {
                        InterMineBag bag2 = (InterMineBag) ws2;
                        return bag2.getDateCreated().compareTo(bag1.getDateCreated());
                    }
                }
                
                return ((Comparable) o1).compareTo(o2);
            }
            
        });
        sortedMap.putAll(filteredWebSearchables);
        return sortedMap;
    }

    /**
     * Get all the WebSearchables in the given scope and of the given type. 
     */
    private Map<String, ? extends WebSearchable> filterWebSearchables(HttpServletRequest request,
                                                                      String type, String scope,
                                                                      String tags) {
        Map<String, ? extends WebSearchable> filteredWebSearchables;
        HttpSession session = request.getSession();

        ServletContext servletContext = session.getServletContext();

        Profile profile;
        // TODO what about "all" scopes?  
        if (scope.equals(Scope.GLOBAL)) {
            profile = SessionMethods.getSuperUserProfile(servletContext);            
        } else {
            profile = (Profile) session.getAttribute(Constants.PROFILE);
        }
        SearchRepository searchRepository;
        if (scope.equals(Scope.GLOBAL)) {
            searchRepository 
               = (SearchRepository) servletContext.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
        } else {
            searchRepository = profile.getSearchRepository();
        }
        Map<String, ? extends WebSearchable> webSearchables =
            searchRepository.getWebSearchableMap(type);

        final ProfileManager pm = 
            (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);

        filteredWebSearchables = webSearchables;
        
        if (tags != null) {
            // filter by tag if there are any otherwise return all
            if (tags.length() > 0) {
                final List<String> tagList = Arrays.asList(StringUtil.split(tags.trim(), " "));
                filteredWebSearchables =
                    pm.filterByTags(filteredWebSearchables, tagList, type, profile.getUsername());
            }
        }

        return filteredWebSearchables;
    }
    
    // loops through the websearchables
    // removes item if item is not on the list
    private Map<String, ? extends WebSearchable>
    filterByList(Map<String, ? extends WebSearchable> filteredWebSearchables, String list) {

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
}
