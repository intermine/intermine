package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.Constants;
import org.intermine.web.Profile;
import org.intermine.web.config.WebConfig;
import org.intermine.web.results.PagedCollection;
import org.intermine.web.results.WebCollection;

/**
 * @author Xavier Watkins
 */
public class BagDetailsController extends TilesAction
{

    /**
     * @see TilesAction#execute(ComponentContext, ActionMapping, ActionForm, HttpServletRequest,
     *      HttpServletResponse)
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        String bagName = request.getParameter("bagName");
        InterMineBag imBag = (InterMineBag) profile.getSavedBags().get(bagName);

        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Model model = os.getModel();
        
        WebCollection webCollection = 
            new WebCollection(os, imBag.getType(), imBag, model, webConfig, classKeys);
        PagedCollection pagedColl = new PagedCollection(webCollection);
        request.setAttribute("bag", imBag);
        request.setAttribute("pagedColl", pagedColl);

        return null;
    }
}
