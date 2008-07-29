package org.intermine.model.testmodel.web;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.http.HttpExportUtil;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.PagedTable;

/**
 * An implementation of TableExporter that exports Employee objects.
 *
 * @author Kim Rutherford
 */

public class EmployeeExporter implements TableHttpExporter
{
    /**
     * Method called to export a RESULTS_TABLE containing an Employee by writing it to the
     * OutputStream of the Response.
     * @param pt exported PagedTable
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     */
    public void export(PagedTable pt, HttpServletRequest request,
                                HttpServletResponse response) {

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=exployee.txt");

        PrintWriter printWriter;
        try {
            printWriter = HttpExportUtil.getPrintWriterForClient(request, response.getOutputStream());
        } catch (IOException e) {
            throw new ExportException("Export failed.", e);
        }

        try {
            List columns = pt.getColumns();
            List rowList = pt.getAllRows();

            for (int rowIndex = 0; rowIndex < rowList.size(); rowIndex++) {
                List row;
                try {
                    row = (List) rowList.get(rowIndex);
                } catch (RuntimeException e) {
                    // re-throw as a more specific exception
                    if (e.getCause() instanceof ObjectStoreException) {
                        throw (ObjectStoreException) e.getCause();
                    }
                    throw e;
                }

                for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                    Column thisColumn = (Column) columns.get(columnIndex);

                    if (!thisColumn.isVisible()) {
                        continue;
                    }

                    // the column order from PagedTable.getList() isn't necessarily the order that
                    // the user has chosen for the columns
                    int realColumnIndex = thisColumn.getIndex();

                    Employee employee = (Employee) row.get(realColumnIndex);

                    printWriter.println("Employee:");
                    printWriter.println("  name: " + employee.getName());
                    printWriter.println("  age: " + employee.getAge());
                    printWriter.println("  fullTime: " + employee.getFullTime());
                }
            }

            printWriter.close();
        } catch (ObjectStoreException e) {
            ActionMessages messages = new ActionMessages();
            ActionMessage error = new ActionMessage("errors.query.objectstoreerror");
            messages.add(ActionMessages.GLOBAL_MESSAGE, error);
            request.setAttribute(Globals.ERROR_KEY, messages);

        }
    }


    /**
     * @see TableHttpExporter#canExport
     */
    public boolean canExport(PagedTable pt) {
        List columns = pt.getColumns();

        if (pt.getVisibleColumnCount() == 1) {
            for (int i = 0; i < columns.size(); i++) {
                if (((Column) columns.get(i)).isVisible()) {
                    Object columnType = ((Column) columns.get(i)).getType();

                    if (columnType instanceof ClassDescriptor) {

                        ClassDescriptor cd = (ClassDescriptor) columnType;

                        if (Employee.class.isAssignableFrom(cd.getType())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
