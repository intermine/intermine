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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.template.TemplateListHelper;

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

        if (aspect.startsWith(AspectController.ASPECT_PREFIX)) {
            aspect = aspect.substring(7).trim();
        }

        InterMineBag interMineIdBag = (InterMineBag) context.getAttribute("interMineIdBag");
        DisplayObject object = (DisplayObject) context.getAttribute("displayObject");
        List templates = null;
        
        if (StringUtils.equals("global", scope)) {
            if (interMineIdBag != null) {
                templates = TemplateListHelper.getAspectTemplatesForType(aspect, servletContext, 
                                                                  interMineIdBag, new HashMap());
            } else if (object == null) {
                templates = TemplateListHelper.getAspectTemplates(aspect, servletContext);
            } else {
                Map fieldExprs = new HashMap();
                templates = TemplateListHelper
                    .getAspectTemplateForClass(aspect, servletContext, object.getObject(),
                            fieldExprs);
                request.setAttribute("fieldExprMap", fieldExprs);
            }
        } else if (StringUtils.equals("user", scope)) {
            //templates = profile.get
        }
        
        request.setAttribute("templates", templates);
        
        return null;
    }
}
