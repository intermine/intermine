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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.path.Path;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for availableColumns.tile.
 * @author Jakub Kulaviak
 *
 */
public class AvailableColumnsController extends InterMineAction
{

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused")
                                 ActionForm form, HttpServletRequest request,
                                 @SuppressWarnings("unused")
    HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String table = request.getParameter("table");
        PagedTable pt = SessionMethods.getResultsTable(session, table);
        List<String> availableColumns = getColumnsThatCanBeAdded(pt);
        request.setAttribute("availableColumns", availableColumns);
        return null;
    }

    /**
     * Returns paths that can be added to the given results table. 
     * For each original path (column of result table) new paths are created adding other fields of the last
     * type in path.
     * For example:<br /> 
     * For table with column Gene.name new paths like Gene.length, Gene.ncbiGeneId 
     * and others are returned.
     * @param pt results table
     * @return columns that can be added
     */
    private List<String> getColumnsThatCanBeAdded(PagedTable pt) {
        List<Path> paths = pt.getWebTable().getPathQuery().getView();
        List<String> ret = new ArrayList<String>();
        Set<ClassDescriptor> processedDescs = new TreeSet<ClassDescriptor>();

        ret.addAll(getAllLastFieldsPaths(paths, processedDescs));
        return getWithoutOriginalPaths(ret, paths);
    }

    private List<String> getAllLastFieldsPaths(List<Path> paths, Set<ClassDescriptor> processedDescs) {
        List<String> ret = new ArrayList<String>();
        for (Path path : paths) {
            ClassDescriptor desc = getPenultimateDescriptor(path);
            if (!processedDescs.contains(desc)) {
                processedDescs.add(desc);
                ret.addAll(getFieldPaths(desc, path.getPrefix().toString()));    
            }
        }
        return ret;
    }

    private List<String> getWithoutOriginalPaths(List<String> output, List<Path> paths) {
        List<String> ret = new ArrayList<String>();        
        Set<String> filterOut = new TreeSet<String>();
        for (Path path : paths) {
            filterOut.add(path.toStringNoConstraints());
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
            if (att.getName() != "id") {
                ret.add(prefix + "." + att.getName());    
            }
        }
        return ret;
    }

    private ClassDescriptor getPenultimateDescriptor(Path path) {
        return path.getPrefix().getLastClassDescriptor();
    }
}
