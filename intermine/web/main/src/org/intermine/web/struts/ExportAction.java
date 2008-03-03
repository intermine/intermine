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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextWriter;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.TableExportConfig;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.TableExporter;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Implementation of <strong>Action</strong> that allows the user to export a PagedTable to a file
 *
 * @author Kim Rutherford
 */
public class ExportAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(ExportAction.class);

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
        String tableType = request.getParameter("tableType");

        List<List<Object>> rowList = null;

        try {
            HttpSession session = request.getSession();
            PagedTable pt = getPagedTable(request, tableType, session);
            rowList = pt.getAllRows();
            if (rowList instanceof WebResults) {
                ((WebResults) rowList).goFaster();
            }

            if (type.equals("excel")) {
                return excel(mapping, request, response, pt);
            } else if (type.equals("csv")) {
                return csv(request, response, pt);
            } else if (type.equals("tab")) {
                return tab(request, response, pt);
            } else {
                WebConfig wc = (WebConfig) session.getServletContext().
                    getAttribute(Constants.WEBCONFIG);

                TableExportConfig tableExportConfig =
                    (TableExportConfig) wc.getTableExportConfigs().get(type);

                if (tableExportConfig == null) {
                    return mapping.findForward("error");
                } else {
                    TableExporter tableExporter =
                        (TableExporter) Class.forName(tableExportConfig.getClassName()).newInstance();

                    return tableExporter.export(mapping, form, request, response);
                }
            }
        } finally {
            if (rowList instanceof WebResults) {
                ((WebResults) rowList).releaseGoFaster();
            }
        }
    }

    private PagedTable getPagedTable(HttpServletRequest request,
            String tableType, HttpSession session) {
        PagedTable pt;

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
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);

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

        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("results");

        try {
            List columns = pt.getColumns();
            List rowList = pt.getAllRows();

            for (int rowIndex = 0;
                 rowIndex < rowList.size() && rowIndex <= pt.getMaxRetrievableIndex();
                 rowIndex++) {
                List row;
                try {
                    row = (List) rowList.get(rowIndex);
                } catch (RuntimeException e) {
                    // re-throw as a more specific exception
                    if (e.getCause() instanceof ObjectStoreException) {
                        throw (ObjectStoreException) e.getCause();
                    } else {
                        throw e;
                    }
                }

                HSSFRow excelRow = sheet.createRow((short) rowIndex);

                // a count of the columns that we have seen so far are invisble - used to get
                // the correct columnIndex for the call to createCell()
                int invisibleColumns = 0;

                for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                    Column thisColumn = (Column) columns.get(columnIndex);

                    // the column order from PagedTable.getList() isn't necessarily the order
                    // that the user has chosen for the columns
                    int realColumnIndex = thisColumn.getIndex();

                    if (!thisColumn.isVisible()) {
                        invisibleColumns++;
                        continue;
                    }

                    Object thisObject = row.get(realColumnIndex);

                    // see comment on invisibleColumns
                    short outputColumnIndex = (short) (columnIndex - invisibleColumns);

                    if (thisObject == null) {
                        excelRow.createCell(outputColumnIndex).setCellValue("");
                        continue;
                    }

                    if (thisObject instanceof Number) {
                        float objectAsFloat = ((Number) thisObject).floatValue();
                        excelRow.createCell(outputColumnIndex).setCellValue(objectAsFloat);
                        continue;
                    }

                    if (thisObject instanceof Date) {
                        Date objectAsDate = (Date) thisObject;
                        excelRow.createCell(outputColumnIndex).setCellValue(objectAsDate);
                        continue;
                    }

                    excelRow.createCell(outputColumnIndex).setCellValue("" + thisObject);
                }
            }

            wb.write(response.getOutputStream());
        } catch (ObjectStoreException e) {
            recordError(new ActionMessage("errors.query.objectstoreerror"), request, e, LOG);
        }

        return null;
    }

    private void writeExcelHeader(HttpServletResponse response) {
        response.setContentType("Application/vnd.ms-excel");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", "attachment; filename=\"results-table.xls\"");
    }

    /**
     * Export the RESULTS_TABLE to Excel format by writing it to the OutputStream of the Response.
     *
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @param pt the PagedTable to export
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward csv(HttpServletRequest request,
                             HttpServletResponse response, PagedTable pt)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);

        response.setContentType("text/comma-separated-values");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", "inline; filename=\"results-table.csv\"");

        new FormattedTextWriter(response.getOutputStream(), 
                getOrder(pt), getVisible(pt), pt.getMaxRetrievableIndex() + 1).writeCSVTable(pt.getAllRows());
        return null;
    }

    /**
     * Export the RESULTS_TABLE to Excel format by writing it to the OutputStream of the Response.
     *
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @param pt the PagedTable to export
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward tab(HttpServletRequest request,
                             HttpServletResponse response, PagedTable pt)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);

        response.setContentType("text/tab-separated-values");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", "inline; filename=\"results-table.tsv\"");

        List allRows = pt.getAllRows();

        new FormattedTextWriter(response.getOutputStream(), 
                getOrder(pt), getVisible(pt),
                pt.getMaxRetrievableIndex() + 1).writeTabDelimitedTable(allRows);
        return null;
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
