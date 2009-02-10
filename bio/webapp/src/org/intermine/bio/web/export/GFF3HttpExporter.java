package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.flymine.model.genomic.LocatedSequenceFeature;
import org.intermine.pathquery.Path;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.export.http.HttpExportUtil;
import org.intermine.web.logic.export.http.HttpExporterBase;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.ExportResultsIterator;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.struts.TableExportForm;

/**
 * An implementation of TableHttpExporter that exports LocatedSequenceFeature
 * objects in GFF3 format.
 *
 * @author Kim Rutherford
 */

public class GFF3HttpExporter extends HttpExporterBase implements TableHttpExporter
{
    /**
     * The batch size to use when we need to iterate through the whole result set.
     */
    public static final int BIG_BATCH_SIZE = 10000;

    /**
     * Method called to export a PagedTable object as GFF3.  The PagedTable can only be exported if
     * there is exactly one LocatedSequenceFeature column and the other columns (if any), are simple
     * attributes (rather than objects).
     * {@inheritDoc}
     */
    public void export(PagedTable pt, HttpServletRequest request, HttpServletResponse response,
                       TableExportForm form) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();

        setGFF3Header(response);

        List<Integer> indexes = ExportHelper.getClassIndexes(ExportHelper.getColumnClasses(pt),
                LocatedSequenceFeature.class);

        Exporter exporter;
        try {
            PrintWriter writer = HttpExportUtil.
                getPrintWriterForClient(request, response.getOutputStream());
            List<String> paths = new LinkedList<String>(StringUtil.
                    serializedSortOrderToMap(form.getPathsString()).keySet());
            removeFirstItemInPaths(paths);
            exporter = new GFF3Exporter(writer,
                    indexes, getSoClassNames(servletContext), paths);
            ExportResultsIterator iter = null;
            try {
                iter = getResultRows(pt, request);
                iter.goFaster();
                exporter.export(iter);
            } finally {
                if (iter != null) {
                    iter.releaseGoFaster();    
                }
            }
        } catch (Exception e) {
            throw new ExportException("Export failed", e);
        }
        
        if (exporter.getWrittenResultsCount() == 0) {
            throw new ExportException("Nothing was found for export");
        }
    }
    
    private void removeFirstItemInPaths(List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i); 
            paths.set(i, path.substring(path.indexOf(".") + 1, path.length()));
        }
    }

    private void setGFF3Header(HttpServletResponse response) {
        ResponseUtil.setPlainTextHeader(response, "table" + StringUtil.uniqueString() + ".gff3");
    }

    /**
     * The intial export path list is just the paths from the columns of the PagedTable.
     * {@inheritDoc}
     */
    public List<Path> getInitialExportPaths(PagedTable pt) {
        return ExportHelper.getColumnPaths(pt);
    }

    /**
     * Read the SO term name to class name mapping file and return it as a Map from class name to
     * SO term name.  The Map is cached as the SO_CLASS_NAMES attribute in the servlet context.
     * @throws ServletException if the SO class names properties file cannot be found
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getSoClassNames(ServletContext servletContext)
        throws ServletException {
        final String soClassNames = "SO_CLASS_NAMES";
        Properties soNameProperties;
        if (servletContext.getAttribute(soClassNames) == null) {
            soNameProperties = new Properties();
            try {
                InputStream is =
                    servletContext.getResourceAsStream("/WEB-INF/soClassName.properties");
                soNameProperties.load(is);
            } catch (Exception e) {
                throw new ServletException("Error loading so class name mapping file", e);
            }

            servletContext.setAttribute(soClassNames, soNameProperties);
        } else {
            soNameProperties = (Properties) servletContext.getAttribute(soClassNames);
        }

        return new HashMap<String, String>((Map) soNameProperties);
    }

    /**
     * {@inheritDoc}
     */
    public boolean canExport(PagedTable pt) {
        return GFF3Exporter.canExportStatic(ExportHelper.getColumnClasses(pt));
    }

    /**
     * {@inheritDoc}
     */
    public List<Path> getExportClassPaths(@SuppressWarnings("unused") PagedTable pt) {
        return new ArrayList<Path>();
    }
}
