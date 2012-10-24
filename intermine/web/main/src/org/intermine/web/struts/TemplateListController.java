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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.search.Scope;
import org.intermine.api.tag.AspectTagUtil;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateManager;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the template list tile.
 * @author Richard Smith
 */
public class TemplateListController extends TilesAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(TemplateListController.class);
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        Model model = im.getModel();
        String scope = (String) context.getAttribute("scope");
        String aspect = (String) context.getAttribute("placement");
        ReportObject object = (ReportObject) context.getAttribute("reportObject");

        if (AspectTagUtil.isAspectTag(aspect)) {
            aspect = AspectTagUtil.getAspect(aspect);
        }

        InterMineBag interMineIdBag = (InterMineBag) context.getAttribute("interMineIdBag");
        List<ApiTemplate> templates = null;
        TemplateManager templateManager = im.getTemplateManager();
        Set<String> allClasses = new HashSet<String>();
        if (StringUtils.equals(Scope.GLOBAL, scope)) {
            if (interMineIdBag != null) {
                allClasses.add(interMineIdBag.getType());
                templates = templateManager.getReportPageTemplatesForAspect(aspect, allClasses);
            } else if (object != null) {
                ClassDescriptor thisCld = model.getClassDescriptorByName(DynamicUtil
                        .getFriendlyName(object.getObject().getClass()));
                for (ClassDescriptor cld : model.getClassDescriptorsForClass(thisCld.getType())) {
                    allClasses.add(cld.getUnqualifiedName());
                }
                templates = templateManager.getReportPageTemplatesForAspect(aspect, allClasses);
            } else {
                templates = templateManager.getAspectTemplates(aspect);
            }

        } else if (StringUtils.equals(Scope.USER, scope)) {
            // no user template functionality implemented
        }

        request.setAttribute("templates", templates);

        return null;
    }
}
