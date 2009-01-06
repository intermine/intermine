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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.template.TemplateHelper;

/**
 * @author "Xavier Watkins"
 *
 */
public class ObjectDetailsInListController extends TilesAction
{
   /**
     * {@inheritDoc}
     */
   @Override
   public ActionForward execute(ComponentContext context,
                                @SuppressWarnings("unused") ActionMapping mapping,
                                @SuppressWarnings("unused") ActionForm form,
                                HttpServletRequest request,
                                @SuppressWarnings("unused") HttpServletResponse response)
   throws Exception {
        String list = (String) context.getAttribute("list");
        String objectid = (String) context.getAttribute("objectid");
        Map globalWebSearchables = WebSearchableListController.getFilterWebSearchables(request,
                        "bag", TemplateHelper.GLOBAL_TEMPLATE, null);
        Map userWebSearchables = WebSearchableListController.getFilterWebSearchables(request,
                        "bag", TemplateHelper.USER_TEMPLATE, null);
        Map filteredWebSearchables = new HashMap<String, WebSearchable>(userWebSearchables);
        filteredWebSearchables.putAll(globalWebSearchables);
        if (list != null) {
            filteredWebSearchables = WebSearchableListController.filterByList(
                            filteredWebSearchables, list);
        }
        request.setAttribute("filteredWebSearchables", filteredWebSearchables);
        return null;
    }
}
