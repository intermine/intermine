package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
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
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagNames;
import org.intermine.web.logic.aspects.Aspect;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.model.userprofile.Tag;


/**
 * Contoller for a single data set tile embedded in a page. Expects the request parameter
 * "name" to refer to a data set name. Places a reference to the corresponding DataSet object
 * in the tile ComponentContext.
 *
 * @author Thomas Riley
 */
public class AspectController extends TilesAction
{

    private static final Logger LOG = Logger.getLogger(AspectController.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ServletContext servletContext = session.getServletContext();

        Map<String, Aspect> aspects = SessionMethods.getAspects(servletContext);
        Aspect set = aspects.get(request.getParameter("name"));

        if (set == null) {
            LOG.error("no such aspect: " + request.getParameter("name"));
            return null;
        }
        context.putAttribute("aspect", set);
        // look up the classes for this aspect
        TagManager tagManager = im.getTagManager();
        String tagName = TagNames.IM_ASPECT_PREFIX + request.getParameter("name");
        List<Tag> tags = tagManager.getTags(tagName, null, "class",
                                            im.getProfileManager().getSuperuser());
        List<String> startingPoints = new ArrayList<String>();
        for (Tag tag : tags) {
            startingPoints.add(tag.getObjectIdentifier());
        }
        context.putAttribute("startingPoints", startingPoints);
        return null;
    }
}
