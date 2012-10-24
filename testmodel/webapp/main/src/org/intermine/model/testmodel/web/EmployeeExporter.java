package org.intermine.model.testmodel.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.results.Column;
import org.intermine.api.results.ResultElement;
import org.intermine.api.results.flatouterjoins.MultiRow;
import org.intermine.api.results.flatouterjoins.MultiRowValue;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.http.HttpExportUtil;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.struts.TableExportForm;

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
     * @param form the form containing the columns paths to export
     */
    public void export(PagedTable pt, HttpServletRequest request, HttpServletResponse response,
            TableExportForm form, Collection<Path> pathCollection,
            Collection<Path> newPathCollection) {

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=exployee.txt");

        PrintWriter printWriter;
        try {
            printWriter = HttpExportUtil.getPrintWriterForClient(request,
                    response.getOutputStream());
        } catch (IOException e) {
            throw new ExportException("Export failed.", e);
        }

        try {
            List<Column> columns = pt.getColumns();
            List<MultiRow<ResultsRow<MultiRowValue<ResultElement>>>> rowList = pt.getAllRows();

            for (int rowIndex = 0; rowIndex < rowList.size(); rowIndex++) {
                MultiRow<ResultsRow<MultiRowValue<ResultElement>>> row;
                try {
                    row = rowList.get(rowIndex);
                } catch (RuntimeException e) {
                    // re-throw as a more specific exception
                    if (e.getCause() instanceof ObjectStoreException) {
                        throw (ObjectStoreException) e.getCause();
                    }
                    throw e;
                }

                for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                    Column thisColumn = columns.get(columnIndex);

                    // the column order from PagedTable.getList() isn't necessarily the order that
                    // the user has chosen for the columns
                    int realColumnIndex = thisColumn.getIndex();

                    // FIXME: The next line is utterly broken.
                    Employee employee = (Employee) ((Object) row.get(realColumnIndex));

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
     * For EmployeeExporter we always return an empty list because all columns and classes are
     * equal for this exporter.
     * @param pt
     * @return List
     */
    public List<Path> getExportClassPaths(@SuppressWarnings("unused") PagedTable pt) {
        return new ArrayList<Path>();
    }

    /**
     * The intial export path list is just the paths from the columns of the PagedTable.
     * {@inheritDoc}
     */
    public List<Path> getInitialExportPaths(PagedTable pt) {
        return ExportHelper.getColumnPaths(pt);
    }

    /**
     * {@inheritDoc}
     */
    public boolean canExport(PagedTable pt) {
        List<Column> columns = pt.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            Object columnType = columns.get(i).getType();
            if (columnType instanceof ClassDescriptor) {
                ClassDescriptor cd = (ClassDescriptor) columnType;
                if (Employee.class.isAssignableFrom(cd.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

}
