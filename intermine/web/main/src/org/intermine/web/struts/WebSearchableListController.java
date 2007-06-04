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
import java.util.List;
import java.util.Map;

import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
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

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for webSearchableLisp.tile
 * @author Kim Rutherford
 */

public class WebSearchableListController extends TilesAction
{
    /**
     * Set up the attributes for webSearchableLisp.tile
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
            searchRepository =
                (SearchRepository) servletContext.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
        } else {

            searchRepository = profile.getSearchRepository();
        }
        Map<String, ? extends WebSearchable> webSearchables =
            searchRepository.getWebSearchableMap(type);

        final ProfileManager pm = 
            (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);

        final List<String> tagList = Arrays.asList(StringUtil.split(tags, " "));

        Map<String, ? extends WebSearchable> filteredWebSearchables =
            pm.filterByTags(webSearchables, tagList, type, profile.getUsername());
        request.setAttribute("filteredWebSearchables", filteredWebSearchables);

        return null;
    }
}
