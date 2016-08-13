package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.WebTable;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * AddColumnAction adds new column to existing WebTable that should be displayed. There are some
 * limitations - only columns that are already contained in web table but are not displayed can
 * be added, because web table contains original InterMine objects and only required columns
 * (columns with required fields) are displayed.
 *
 * @author Jakub Kulaviak
 *
 */
@SuppressWarnings("deprecation")
public class AddColumnAction extends InterMineAction
{
    /**
     * Executes action. @see AddColumnAction
     * @param mapping action mapping
     * @param form not used
     * @param request request
     * @param response response
     * @return action forward
     * @throws Exception if an error happens
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        String columnToAdd = request.getParameter("columnToAdd");
        HttpSession session = request.getSession();
        String tableId = request.getParameter("table");
        PagedTable pt = SessionMethods.getResultsTable(session, tableId);
        if (columnToAdd != null && columnToAdd.length() != 0) {
            changeView(pt.getWebTable(), columnToAdd, request);
        }
        return new ForwardParameters(mapping.findForward("results"))
            .addParameter("table", tableId)
            .addParameter("page", "" + pt.getPage())
            .addParameter("trail", request.getParameter("trail")).forward();
    }

    private void changeView(WebTable table, String columnToAdd,
            HttpServletRequest request) throws PathException {
        if (columnAlreadyAdded(columnToAdd, table.getPathQuery().getView())) {
            return;
        }

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        Model model = im.getModel();

        List<Path> paths = new ArrayList<Path>();
        Path path = new Path(model, columnToAdd);
        paths.add(path);
        table.getColumnsPath().add(path);
        table.addColumns(paths);

        // add to path query as well
        PathQuery query = table.getPathQuery();
        query.addView(columnToAdd);
    }

    private boolean columnAlreadyAdded(String columnToAdd, List<String> displayedPaths) {
        return displayedPaths.contains(columnToAdd);
    }
}
