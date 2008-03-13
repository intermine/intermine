package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.export.ExcelExporter;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.results.PagedTable;


/**
 * Exporter that exports table with results in excel format.
 * @author Jakub Kulaviak
 **/
public class ExcelHttpExporter extends HttpExporterBase implements TableHttpExporter
{

    /**
     * Constructor.
     */
    public ExcelHttpExporter() { }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void export(PagedTable pt, HttpServletRequest request, HttpServletResponse response) {
        int defaultMax = 10000;

        int maxExcelSize =
            WebUtil.getIntSessionProperty(request.getSession(), 
                    "max.excel.export.size", defaultMax);

        if (pt.getExactSize() > maxExcelSize) {
            ActionMessages messages = new ActionMessages();
            messages.add(ActionMessages.GLOBAL_MESSAGE, 
                    new ActionMessage("export.excelExportTooBig", new Integer(maxExcelSize)));
            request.setAttribute(Globals.ERROR_KEY, messages);
        }
        super.export(pt, request, response);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Exporter getExporter(OutputStream out) {
        return new ExcelExporter(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setResponseHeader(HttpServletResponse response) {
        ResponseUtil.setExcelHeader(response, "results-table.xls");
    }
}
