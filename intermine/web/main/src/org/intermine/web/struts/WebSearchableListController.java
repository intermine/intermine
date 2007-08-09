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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.util.GenericCompositeMap;
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
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateHelper;

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
        Map<String, ? extends WebSearchable> filteredWebSearchables;
        
        HttpSession session = request.getSession();
     
        if (session.getAttribute("IS_SUPERUSER") != null 
                        && session.getAttribute("IS_SUPERUSER").equals(Boolean.TRUE)) {
                filteredWebSearchables = filterWebSearchables(request, type, 
                                                              TemplateHelper.USER_TEMPLATE, tags);
        
        } else if (scope.equals(TemplateHelper.ALL_TEMPLATE)) {
                Map<String, ? extends WebSearchable> globalWebSearchables =
                    filterWebSearchables(request, type, TemplateHelper.GLOBAL_TEMPLATE, tags); 
                Map<String, ? extends WebSearchable> userWebSearchables =
                    filterWebSearchables(request, type, TemplateHelper.USER_TEMPLATE, tags);
                GenericCompositeMap.PriorityOrderMapMutator<String, WebSearchable> mutator =
                    new GenericCompositeMap.PriorityOrderMapMutator<String, WebSearchable>();
                filteredWebSearchables = 
                    new GenericCompositeMap<String, WebSearchable>(globalWebSearchables, 
                                                               userWebSearchables, mutator);

        } else {
            filteredWebSearchables = filterWebSearchables(request, type, scope, tags);
       }

        if (list != null) {
            filteredWebSearchables = filterByList(filteredWebSearchables, list);
        }

        // shorten list to be < limit
        if (limit != null) {
            limit = limit.trim();
            if (limit.length() > 0) {
                try {
                    filteredWebSearchables = WebUtil.shuffle(filteredWebSearchables,
                                                             new Integer(limit).intValue());
                } catch (NumberFormatException e) {
                    // ignore - don't shuffle 
                }
            }
        }
        
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ArrayList userWebSearchables = new ArrayList();
        
        request.setAttribute("userWebSearchables", 
                             profile.getWebSearchablesByType(type));
        
        request.setAttribute("filteredWebSearchables", filteredWebSearchables);
        return null;
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
