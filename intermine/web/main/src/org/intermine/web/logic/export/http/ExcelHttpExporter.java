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

import javax.servlet.http.HttpServletResponse;

import org.intermine.web.logic.export.ExcelExporter;
import org.intermine.web.logic.export.Exporter;


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
    protected Exporter getExporter(OutputStream out) {
        return new ExcelExporter(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setResponseHeader(HttpServletResponse response) {
        response.setContentType("Application/vnd.ms-excel");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", 
                "attachment; filename=\"results-table.xls\"");        
    }

}
