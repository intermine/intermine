package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for sequence export tile.
 * @author Kim Rutherford
 */
public class SequenceExportOptionsController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        String tableName = request.getParameter("table");
        HttpSession session = request.getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, tableName);

        List<Path> exportClassPaths =
            SequenceFeatureExportUtil.getExportClassPaths(pt.getPathQuery());

        Map<String, String> pathMap = new LinkedHashMap<String, String>();

        for (Path path: exportClassPaths) {
            String pathString = path.toStringNoConstraints();
            String displayPath = pathString.replace(".", " &gt; ");
            pathMap.put(pathString, displayPath);
        }

        request.setAttribute("exportClassPaths", pathMap);

        return null;
    }
}
