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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.search.Scope;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.SessionMethods;

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
        Map<String, ? extends WebSearchable> filteredWebSearchables 
                                                = new HashMap<String, WebSearchable>();
        
        // TODO filter results on list
        
        if (tags != null) {
            HttpSession session = request.getSession();

            ServletContext servletContext = session.getServletContext();

            Profile profile;
            if (scope.equals(Scope.GLOBAL)) {
                profile = SessionMethods.getSuperUserProfile(servletContext);            
            } else {
                profile = (Profile) session.getAttribute(Constants.PROFILE);
            }
            SearchRepository searchRepository;
            if (scope.equals(Scope.GLOBAL)) {
                searchRepository = (SearchRepository) 
                                    servletContext.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
            } else {
                searchRepository = profile.getSearchRepository();
            }
            Map<String, ? extends WebSearchable> webSearchables =
                                                        searchRepository.getWebSearchableMap(type);

            final ProfileManager pm = 
                (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);

            final List<String> tagList = Arrays.asList(StringUtil.split(tags, " "));

            filteredWebSearchables =
                pm.filterByTags(webSearchables, tagList, type, profile.getUsername());
        }
        
        // if we already have a list, use that      
//        if (list != null) {
//            
//        }
        
        // shorten list to be < limit
        if (limit != null) {
            filteredWebSearchables = WebUtil.shuffle(filteredWebSearchables, new Integer(limit));
        }
        request.setAttribute("filteredWebSearchables", filteredWebSearchables);
        return null;
    }
}
