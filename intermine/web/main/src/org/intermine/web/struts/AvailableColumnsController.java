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

import java.io.StringReader;
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
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.WebUtil;
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
    public ActionForward execute(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();

        String queryXML = (String) request.getAttribute("queryXML");
        String table = (String) request.getAttribute("table");

        @SuppressWarnings("deprecation")
        PathQuery q = (queryXML != null) ? PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXML), PathQuery.USERPROFILE_VERSION)
                : SessionMethods.getResultsTable(session, table).getWebTable()
                        .getPathQuery();

        request.setAttribute("availableColumns", getColumnsThatCanBeAdded(q));

        return null;
    }

    private Map<String, String> getColumnsThatCanBeAdded(PathQuery query) {
        Map<String, String> ret = new LinkedHashMap<String, String>();
        List<String> availPaths = getPathsThatCanBeAdded(query);
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
    private List<String> getPathsThatCanBeAdded(PathQuery pathQuery) {
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
