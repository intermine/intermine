package org.intermine.web.struts;

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for availableColumns.tile.
 * @author Jakub Kulaviak
 *
 */
public class AvailableColumnsController extends InterMineAction
{

    private static final Logger LOG = Logger.getLogger(AvailableColumnsController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String table = request.getParameter("table");
        PagedTable pt = SessionMethods.getResultsTable(session, table);
        request.setAttribute("availableColumns", getColumnsThatCanBeAdded(pt));
        return null;
    }

    private Map<String, String> getColumnsThatCanBeAdded(PagedTable pt) {
        Map<String, String> ret = new LinkedHashMap<String, String>();
        PathQuery query = pt.getWebTable().getPathQuery();
        List<String> availPaths = getPathsThatCanBeAdded(pt);
        for (String availPath : availPaths) {
            ret.put(availPath, WebUtil.formatColumnName(
                    query.getGeneratedPathDescription(availPath)));
        }
        return ret;
    }

    /**
     * Returns paths that can be added to the given results table.
     * For each original path (column of result table) new paths are created adding other fields of
     * the last type in path.
     * For example:<br />
     * For table with column Gene.name new paths like Gene.length, Gene.ncbiGeneId
     * and others are returned.
     * @param pt results table
     * @return columns that can be added
     */
    private List<String> getPathsThatCanBeAdded(PagedTable pt) {
        PathQuery pathQuery = pt.getWebTable().getPathQuery();
        List<String> ret = new ArrayList<String>();

        ret.addAll(getAllLastFieldsPaths(pathQuery));
        return getWithoutOriginalPaths(ret, pathQuery.getView());
    }

    private List<String> getAllLastFieldsPaths(PathQuery pathQuery) {
        List<Path> paths = new ArrayList<Path>();
        for (String pathString : pathQuery.getView()) {
            try {
                paths.add(pathQuery.makePath(pathString));
            } catch (PathException e) {
                LOG.warn("Invalid path '" + pathString + " finding available columns for query:"
                        + pathQuery);
            }
        }

        Set<String> processed = new HashSet<String>();
        List<String> ret = new ArrayList<String>();
        for (Path path : paths) {
            ClassDescriptor desc = getPenultimateDescriptor(path);
            String key = path.getPrefix().toStringNoConstraints();
            if (!processed.contains(key)) {
                processed.add(key);
                ret.addAll(getFieldPaths(desc, path.getPrefix().toStringNoConstraints()));
            }
        }
        return ret;
    }

    private List<String> getWithoutOriginalPaths(List<String> output, List<String> paths) {
        List<String> ret = new ArrayList<String>();
        Set<String> filterOut = new TreeSet<String>();
        for (String path : paths) {
            filterOut.add(path);
        }
        for (String o : output) {
            if (!filterOut.contains(o)) {
                ret.add(o);
            }
        }
        return ret;
    }

    private List<String> getFieldPaths(ClassDescriptor desc, String prefix) {
        List<String> ret = new ArrayList<String>();
        Set<AttributeDescriptor> atts = desc.getAllAttributeDescriptors();
        for (AttributeDescriptor att : atts) {
            if (!"id".equals(att.getName())) {
                ret.add(prefix + "." + att.getName());
            }
        }
        return ret;
    }

    private ClassDescriptor getPenultimateDescriptor(Path path) {
        return path.getPrefix().getLastClassDescriptor();
    }
}
