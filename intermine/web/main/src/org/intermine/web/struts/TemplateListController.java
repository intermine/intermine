package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagNames;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateListHelper;
import org.intermine.web.logic.template.TemplateQuery;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for the template list tile.
 * @author Thomas Riley
 */
public class TemplateListController extends TilesAction
{
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
        String scope = (String) context.getAttribute("scope");
        String aspect = (String) context.getAttribute("placement");

        if (ProfileManager.isAspectTag(aspect)) {
            aspect = ProfileManager.getAspect(aspect);
        }

        InterMineBag interMineIdBag = (InterMineBag) context.getAttribute("interMineIdBag");
        DisplayObject object = (DisplayObject) context.getAttribute("displayObject");
        List<TemplateQuery> templates = null;

        if (StringUtils.equals("global", scope)) {
            if (interMineIdBag != null) {
                templates = TemplateListHelper.getAspectTemplatesForType(aspect, servletContext,
                            interMineIdBag, new HashMap<TemplateQuery, List<String>>());
            } else if (object == null) {
                templates = TemplateListHelper.getAspectTemplates(aspect, servletContext);
            } else {
                Map<TemplateQuery, List<String>> fieldExprs =
                    new HashMap<TemplateQuery, List<String>>();
                templates = TemplateListHelper
                    .getAspectTemplateForClass(aspect, servletContext, object.getObject(),
                            fieldExprs);
                request.setAttribute("fieldExprMap", fieldExprs);
            }

            ProfileManager pm = SessionMethods.getProfileManager(servletContext);
            String sup = pm.getSuperuser();
            List<Tag> noReportTags =
                pm.getTags(TagNames.IM_NO_REPORT, null, TagTypes.TEMPLATE, sup);

            Set<String> noReportNames = new HashSet<String>();

            for (Tag tag: noReportTags) {
                noReportNames.add(tag.getObjectIdentifier());
            }

            Iterator<TemplateQuery> templateIter = templates.iterator();

            while (templateIter.hasNext()) {
                TemplateQuery tq = templateIter.next();
                if (noReportNames.contains(tq.getName())) {
                    templateIter.remove();
                }
            }

        } else if (StringUtils.equals("user", scope)) {
            //templates = profile.get
        }

        request.setAttribute("templates", templates);

        return null;
    }
}
