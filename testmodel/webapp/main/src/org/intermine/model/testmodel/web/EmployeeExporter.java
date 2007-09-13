package org.intermine.model.testmodel.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.export.TableExporter;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * An implementation of TableExporter that exports Employee objects.
 *
 * @author Kim Rutherford
 */

public class EmployeeExporter implements TableExporter
{
    /** 
     * Method called to export a RESULTS_TABLE containing an Employee by writing it to the
     * OutputStream of the Response.
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

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=exployee.txt");

        PrintStream printStream = new PrintStream(response.getOutputStream());

        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

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
                    } else {
                        throw e;
                    }
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

                    printStream.println("Employee:");
                    printStream.println("  name: " + employee.getName());
                    printStream.println("  age: " + employee.getAge());
                    printStream.println("  fullTime: " + employee.getFullTime());
                }
            }

            printStream.close();
        } catch (ObjectStoreException e) {
            ActionMessages messages = new ActionMessages();
            ActionMessage error = new ActionMessage("errors.query.objectstoreerror");
            messages.add(ActionMessages.GLOBAL_MESSAGE, error);
            request.setAttribute(Globals.ERROR_KEY, messages);
     
        }

        return null;
    }


    /**
     * @see TableExporter#canExport
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
