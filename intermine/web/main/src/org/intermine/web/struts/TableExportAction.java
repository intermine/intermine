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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.TableExportConfig;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ExporterFactory;
import org.intermine.web.logic.export.TableExporter;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;

/*
 * TODO   
 * - finish gff3
 * - rewrite exporters
 * - enables set position from which export should start 
 * - rewrite  web service for new exporters
 * - solve error handling in export action
 */

/**
 * Implementation of <strong>Action</strong> that allows the user to export a 
 * PagedTable to a file.
 *
 * @author Kim Rutherford
 * @author Jakub Kulaviak 
 */
public class TableExportAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(TableExportAction.class);

    //private static final ActionForward forward = null; 
    
    /**
     * Method called to export a PagedTable object.  Uses the type request parameter to choose the
     * export method.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        String type = request.getParameter("type");
        List<List<Object>> rowList = null;

        try {
            HttpSession session = request.getSession();
            PagedTable pt = getPagedTable(request, session);
            rowList = pt.getAllRows();
            if (rowList instanceof WebResults) {
                ((WebResults) rowList).goFaster();
            }
            
            if (type.equals("excel")) {
                return excel(mapping, request, response, pt);
            } else if (type.equals("csv")) {
                writeCSVHeader(response);
                Exporter exporter = ExporterFactory.createExporter(response.getOutputStream(), 
                        ExporterFactory.CSV);
                exporter.export(pt.getRearrangedResults());
            } else if (type.equals("tab")) {
                writeTSVHeader(response);
                Exporter exporter = ExporterFactory.createExporter(response.getOutputStream(), 
                        ExporterFactory.TAB);
                exporter.export(pt.getRearrangedResults());
            } else {
                WebConfig wc = (WebConfig) session.getServletContext().
                    getAttribute(Constants.WEBCONFIG);

                TableExportConfig tableExportConfig =
                    (TableExportConfig) wc.getTableExportConfigs().get(type);

                if (tableExportConfig == null) {
                    return mapping.findForward("error");
                } else {
                    TableExporter tableExporter =
                        (TableExporter) Class.forName(tableExportConfig.getClassName()).
                            newInstance();
                    return tableExporter.export(mapping, form, request, response);
                }
            }
            return null;
        } catch (RuntimeException ex) {
            ActionMessages messages = new ActionMessages();
            ActionMessage error = new ActionMessage("errors.export.exportfailed");
            messages.add(ActionMessages.GLOBAL_MESSAGE, error);
            request.setAttribute(Globals.ERROR_KEY, messages);
            LOG.error(ex);
            return mapping.findForward("error");
        } finally {
            if (rowList != null && rowList instanceof WebResults) {
                ((WebResults) rowList).releaseGoFaster();
            }
        }
    }
    
    private PagedTable getPagedTable(HttpServletRequest request, HttpSession session) {
        PagedTable pt;
        String tableType = request.getParameter("tableType");
        if (tableType.equals("bag")) {
            pt = SessionMethods.getResultsTable(session, "bag." 
                    + request.getParameter("table"));
        } else {
            pt = SessionMethods.getResultsTable(session, request.getParameter("table"));
        }
        return pt;
    }

    /**
     * Export the RESULTS_TABLE to Excel format by writing it to the OutputStream of the Response.
     * @param mapping The ActionMapping used to select this instance
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @param pt the PagedTable to export
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward excel(ActionMapping mapping,
                               HttpServletRequest request,
                               HttpServletResponse response, PagedTable pt)
        throws Exception {
        HttpSession session = request.getSession();

        writeExcelHeader(response);

        if (pt == null) {
            return mapping.getInputForward();
        }

        int defaultMax = 10000;

        int maxExcelSize =
            WebUtil.getIntSessionProperty(session, "max.excel.export.size", defaultMax);

        if (pt.getEstimatedSize() > maxExcelSize) {
            ActionMessage actionMessage =
                new ActionMessage("export.excelExportTooBig", new Integer(maxExcelSize));
            recordError(actionMessage, request);

            return mapping.getInputForward();
        }

        Exporter exporter = ExporterFactory.createExporter(response.getOutputStream(), 
                ExporterFactory.EXCEL);
        exporter.export(pt.getRearrangedResults());
        
        return null;
    }

    private void writeExcelHeader(HttpServletResponse response) {
        response.setContentType("Application/vnd.ms-excel");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", "attachment; filename=\"results-table.xls\"");
    }

    private void writeCSVHeader(HttpServletResponse response) {
        response.setContentType("text/comma-separated-values");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", "inline; filename=\"results-table.csv\"");
    }

    private void writeTSVHeader(HttpServletResponse response) {
        response.setContentType("text/tab-separated-values");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", "inline; filename=\"results-table.tsv\"");
    }

    /**
     * Return an int array containing the real column indexes to use while writing the given
     * PagedTable.
     * @param pt the PagedTable to export
     */
    private static int [] getOrder(PagedTable pt) {
        List columns = pt.getColumns();

        int [] returnValue = new int [columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            returnValue[i] = ((Column) columns.get(i)).getIndex();
        }

        return returnValue;
    }

    /**
     * Return an array containing the visibility of each column in the output
     * @param pt the PagedTable to export
     */
    private static boolean [] getVisible(PagedTable pt) {
        List columns = pt.getColumns();

        boolean [] returnValue = new boolean [columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            returnValue[i] = ((Column) columns.get(i)).isVisible();
        }

        return returnValue;
    }
}
