package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * An implementation of TableHttpExporter that exports LocatedSequenceFeature 
 * objects in GFF3 format.
 *
 * @author Kim Rutherford
 */

public class GFF3HttpExporter implements TableHttpExporter
{
    /**
     * The batch size to use when we need to iterate through the whole result set.
     */
    public static final int BIG_BATCH_SIZE = 10000;

    /**
     * Method called to export a PagedTable object as GFF3.  The PagedTable can only be exported if
     * there is exactly one LocatedSequenceFeature column and the other columns (if any), are simple
     * attributes (rather than objects).
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward export(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();

        setGFF3Header(response);

        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

        if (pt == null) {
            return mapping.getInputForward();
        }        
        
        int realFeatureIndex = ExportHelper.getFirstColumnForClass(
                ExportHelper.getColumnClasses(pt), LocatedSequenceFeature.class);

        Exporter exporter;
        try {
            exporter = new GFF3Exporter(response.getWriter(), 
                    realFeatureIndex, getSoClassNames(servletContext));
        } catch (Exception e) {
            throw new ExportException("Export failed.", e);
        } 
        exporter.export(pt.getRearrangedResults());
        if (exporter.getWrittenResultsCount() == 0) {
            throw new ExportException("Nothing was found for export.");
        }
        return null;
    }

    private void setGFF3Header(HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ",
                           "inline; filename=table" + StringUtil.uniqueString() + ".gff3");
    }

    /**
     * Read the SO term name to class name mapping file and return it as a Map from class name to
     * SO term name.  The Map is cached as the SO_CLASS_NAMES attribute in the servlet context.
     * @throws ServletException if the SO class names properties file cannot be found
     */
    private Map getSoClassNames(ServletContext servletContext) throws ServletException {
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

        return soNameProperties;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canExport(PagedTable pt) {
        return GFF3Exporter.canExport2(ExportHelper.getColumnClasses(pt));
    }
}
