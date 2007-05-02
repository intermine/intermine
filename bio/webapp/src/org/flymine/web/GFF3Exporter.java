package org.flymine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.bio.util.GFF3Util;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.TypeUtil.FieldInfo;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.TableExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.results.WebTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.ExportController;

/**
 * An implementation of TableExporter that exports LocatedSequenceFeature objects in GFF3 format.
 *
 * @author Kim Rutherford
 */

public class GFF3Exporter implements TableExporter
{
    /**
     * The batch size to use when we need to iterate through the whole result set.
     */
    public static final int BIG_BATCH_SIZE = 10000;

    private static final Logger LOG = Logger.getLogger(GFF3Exporter.class);
    
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
        
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ",
                           "inline; filename=table" + StringUtil.uniqueString() + ".gff3");

        OutputStream outputStream = null;
        PrintWriter printWriter = null;
        
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

        int realFeatureIndex = ExportHelper
        .getFirstColumnForClass(pt, LocatedSequenceFeature.class);
        int writtenFeaturesCount = 0;

        try {
            WebTable rowList = pt.getAllRows();
            
            //  TODO if a query, increase the batch size - already goFaster()?

            for (int rowIndex = 0;
                 rowIndex < rowList.size() && rowIndex <= pt.getMaxRetrievableIndex();
                 rowIndex++) {
                List<ResultElement> row;
                try {
                    row = rowList.getResultElements(rowIndex);
                } catch (RuntimeException e) {
                    // re-throw as a more specific exception
                    if (e.getCause() instanceof ObjectStoreException) {
                        throw (ObjectStoreException) e.getCause();
                    } else {
                        throw e;
                    }
                }

                LocatedSequenceFeature lsf = (LocatedSequenceFeature) row.get(realFeatureIndex)
                    .getInterMineObject();

                Map extraAttributes = new HashMap();

                
                // add some fields as extra attributes if the object has them
                
                List<String> extraFields = Arrays.asList(new String[] {"symbol", "organismDbId",
                    "name"});
                for (String fieldName : extraFields) {
                    FieldInfo field = TypeUtil.getFieldInfo(lsf.getClass(), fieldName);
                    if (field != null && (TypeUtil.getFieldValue(lsf, fieldName) != null)) {
                        extraAttributes.put(fieldName, TypeUtil.getFieldValue(lsf, fieldName));
                    }
                }

                GFF3Record gff3Record =
                    GFF3Util.makeGFF3Record(lsf, getSoClassNames(servletContext), extraAttributes);
                                                                
                if (gff3Record == null) {
                    // no chromsome ref or no chromosomeLocation ref
                    continue;
                }
                
                if (outputStream == null) {
                    // try to avoid opening the OutputStream until we know that the query is
                    // going to work - this avoids some problems that occur when
                    // getOutputStream() is called twice (once by this method and again to
                    // write the error)
                    outputStream = response.getOutputStream();
                    printWriter = new PrintWriter(new OutputStreamWriter(outputStream));

                    printWriter.println("##gff-version 3");
                }
                
                writtenFeaturesCount++;

                printWriter.println(gff3Record.toGFF3());
            }

            if (printWriter != null) {
                printWriter.close();
            }
            
            if (outputStream != null) {
                outputStream.close();
            }

            if (writtenFeaturesCount == 0) {
                ActionErrors messages = new ActionErrors();
                ActionError error = new ActionError("errors.export.nothingtoexport");
                messages.add(ActionErrors.GLOBAL_ERROR, error);
                request.setAttribute(Globals.ERROR_KEY, messages);

                return mapping.findForward("results");
            }

        } catch (ObjectStoreException e) {
            ActionErrors messages = new ActionErrors();
            ActionError error = new ActionError("errors.query.objectstoreerror");
            messages.add(ActionErrors.GLOBAL_ERROR, error);
            request.setAttribute(Globals.ERROR_KEY, messages);
            LOG.error(e);
        }
        return null;
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
     * @see org.intermine.web.logic.export.TableExporter#canExport(PagedTable)
     */
    public boolean canExport(PagedTable pt) {
        return ExportHelper.canExport(pt, LocatedSequenceFeature.class);
    }

}
