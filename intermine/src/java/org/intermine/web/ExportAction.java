package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Date;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.apache.poi.hssf.usermodel.*;

import org.intermine.web.results.PagedTable;
import org.intermine.web.results.Column;
import org.intermine.util.TextFileUtil;

/**
 * Implementation of <strong>Action</strong> that allows the user to export a PagedTable to a file
 *
 * @author Kim Rutherford
 */

public class ExportAction extends DispatchAction
{
    /**
     * Export the RESULTS_TABLE to Excel format by writing it to the OutputStream of the Response.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward excel(ActionMapping mapping,
                               ActionForm form,
                               HttpServletRequest request,
                               HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        response.setContentType("Application/vnd.ms-excel");
        response.setHeader("Content-Disposition ", "inline; filename=results-table.xsl");

        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("results");

        List columns = pt.getColumns();
        List rowList = pt.getList();

        for (int rowIndex = 0; rowIndex < rowList.size(); rowIndex++) {
            List row = (List) rowList.get(rowIndex);
            HSSFRow excelRow = sheet.createRow((short) rowIndex);

            // a count of the columns that we have seen so far are invisble - used to get the
            // correct columnIndex for the call to createCell()
            int invisibleColumns = 0;
                
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                Column thisColumn = (Column) columns.get(columnIndex);

                // the column order from PagedTable.getList() isn't necessarily the order that the
                // user has chosen for the columns
                int realColumnIndex = thisColumn.getIndex();

                if (!thisColumn.isVisible()) {
                    invisibleColumns++;
                    continue;
                }

                Object thisObject = row.get(realColumnIndex);

                // see comment on invisibleColumns
                short outputColumnIndex = (short) (columnIndex - invisibleColumns);

                if (thisObject instanceof Number) {
                    float objectAsFloat = ((Number) thisObject).floatValue();
                    excelRow.createCell(outputColumnIndex).setCellValue(objectAsFloat);
                } else {
                    if (thisObject instanceof Date) {
                        Date objectAsDate = (Date) thisObject;
                        excelRow.createCell(outputColumnIndex).setCellValue(objectAsDate);
                    } else {
                        excelRow.createCell(outputColumnIndex).setCellValue(thisObject.toString());
                    }
                }

            }
        }

        wb.write(response.getOutputStream());

        return null;
    }

    /**
     * Export the RESULTS_TABLE to Excel format by writing it to the OutputStream of the Response.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward csv(ActionMapping mapping,
                             ActionForm form,
                             HttpServletRequest request,
                             HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        response.setContentType("text/comma-separated-values");
        response.setHeader("Content-Disposition ", "inline; filename=results-table.txt");

        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        
        TextFileUtil.writeCSVTable(response.getOutputStream(), pt.getList(),
                                   getOrder(pt), getVisible(pt));

        return null;
    }

    /**
     * Export the RESULTS_TABLE to Excel format by writing it to the OutputStream of the Response.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward tab(ActionMapping mapping,
                             ActionForm form,
                             HttpServletRequest request,
                             HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        response.setContentType("text/comma-separated-values");
        response.setHeader("Content-Disposition ", "inline; filename=results-table.txt");

        PagedTable pt = (PagedTable) session.getAttribute(Constants.RESULTS_TABLE);

        TextFileUtil.writeTabDelimitedTable(response.getOutputStream(), pt.getList(),
                                            getOrder(pt), getVisible(pt));

        return null;
    }

    /**
     * Return an int array containing the real column indexes to use while writing the given
     * PagedTable.
     * @param pt the PagedTable
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
     * @param pt the PagedTable
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
