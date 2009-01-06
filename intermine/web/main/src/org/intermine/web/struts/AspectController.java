package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.aspects.Aspect;
import org.intermine.web.logic.profile.TagManager;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagNames;

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
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Map aspects = (Map) servletContext.getAttribute(Constants.ASPECTS);
        Aspect set = (Aspect) aspects.get(request.getParameter("name"));
        if (set == null) {
            LOG.error("no such aspect: " + request.getParameter("name"));
            return null;
        }
        context.putAttribute("aspect", set);
        // look up the classes for this aspect
        TagManager tagManager = SessionMethods.getTagManager(session);
        String superuser = SessionMethods.getSuperUserProfile(session.getServletContext())
            .getUsername();
        String tagName = TagNames.IM_ASPECT_PREFIX + request.getParameter("name");
        List<Tag> tags = tagManager.getTags(tagName, null, "class", superuser);
        CollectionUtils.transform(tags,
                TransformerUtils.invokerTransformer("getObjectIdentifier"));
        context.putAttribute("startingPoints", tags);
        return null;
    }
}
