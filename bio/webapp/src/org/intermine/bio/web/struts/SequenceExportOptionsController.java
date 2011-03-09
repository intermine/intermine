package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
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
import org.intermine.api.results.Column;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.pathquery.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for sequence export tile.
 * @author Kim Rutherford
 */
public class SequenceExportOptionsController extends TilesAction
{
    /**
     * Set up the bagUploadConfirm page.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        String tableName = request.getParameter("table");
        HttpSession session = request.getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, tableName);

        List<Path> exportClassPaths = getExportClassPaths(pt);

        Map<String, String> pathMap = new LinkedHashMap<String, String>();

        for (Path path: exportClassPaths) {
            String pathString = path.toStringNoConstraints();
            String displayPath = pathString.replace(".", " &gt; ");
            pathMap.put(pathString, displayPath);
        }

        request.setAttribute("exportClassPaths", pathMap);

        return null;
    }


    /**
     * From the columns of the PagedTable, return a List of the Paths that this exporter will
     * use to find sequences to export.  The returned Paths are a subset of the prefixes of the
     * column paths.
     * eg. if the columns are ("Gene.primaryIdentifier", "Gene.secondaryIdentifier",
     * "Gene.proteins.primaryIdentifier") return ("Gene", "Gene.proteins").
     * @param pt the PagedTable
     * @return a list of Paths that have sequence
     */
    public static List<Path> getExportClassPaths(PagedTable pt) {
        List<Path> retPaths = new ArrayList<Path>();

        List<Column> columns = pt.getColumns();

        for (Column column: columns) {
            Path prefix = column.getPath().getPrefix();
            ClassDescriptor prefixCD = prefix.getLastClassDescriptor();
            Class<? extends FastPathObject> prefixClass = DynamicUtil.getSimpleClass(prefixCD
                    .getType());
            if (Protein.class.isAssignableFrom(prefixClass)
                || SequenceFeature.class.isAssignableFrom(prefixClass)
                || Sequence.class.isAssignableFrom(prefixClass)) {
                if (!retPaths.contains(prefix)) {
                    retPaths.add(prefix);
                }
            }
        }

        return retPaths;
    }
}
