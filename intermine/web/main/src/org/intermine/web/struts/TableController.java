package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.results.Column;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.metadata.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.stringtree.json.JSONWriter;

/**
 * Implementation of <strong>TilesAction</strong>. Sets up PagedTable
 * for table tile.
 *
 * @author Thomas Riley
 */
public class TableController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(TableController.class);

    /**
     * Set up table tile.
     *
     * @param context The Tiles ComponentContext
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String trail = request.getParameter("trail");

        request.setAttribute("trail", trail);

        SaveBagForm bagForm = (SaveBagForm) session.getAttribute("saveBagForm");
        if (bagForm != null) {
            bagForm.reset(mapping, request);
        }

        String table = request.getParameter("table");

        request.setAttribute("table", table);

        Map<Path, String> pathNames = new HashMap<Path, String> ();
        request.setAttribute("pathNames", pathNames);

        return null;
    }
}
