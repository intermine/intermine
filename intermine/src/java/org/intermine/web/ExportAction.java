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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.apache.poi.hssf.usermodel.*;

import org.intermine.web.results.PagedTable;
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

        List rowList = pt.getList();

        for (int i = 0; i < rowList.size(); i++) {
            List row = (List) rowList.get(i);
            HSSFRow excelRow = sheet.createRow((short) i);
            for (int j = 0; j < row.size(); j++) {
                Object thisObject = row.get(j);

                if (thisObject instanceof Number) {
                    float objectAsFloat = ((Number) thisObject).floatValue();
                    excelRow.createCell((short) j).setCellValue(objectAsFloat);
                } else {
                    excelRow.createCell((short) j).setCellValue(thisObject.toString());
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

        TextFileUtil.writeCSVTable(response.getOutputStream(), pt.getList());

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

        TextFileUtil.writeTabDelimitedTable(response.getOutputStream(), pt.getList());

        return null;
    }
}
