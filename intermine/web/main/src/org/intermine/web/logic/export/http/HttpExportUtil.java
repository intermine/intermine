package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.intermine.web.logic.RequestUtil;
import org.intermine.web.logic.export.CustomPrintWriter;
import org.intermine.web.logic.export.Exporter;

/**
 * Util for export.
 * @author Jakub Kulaviak
 **/
public final class HttpExportUtil
{
    private HttpExportUtil() {

    }
    /**
     * @param request request
     * @param out output stream
     * @return implementation of print writer, that separates
     * lines according to the client operation system.
     */
    public static PrintWriter getPrintWriterForClient(HttpServletRequest request,
            OutputStream out) {
        PrintWriter ret;
        if (RequestUtil.isWindowsClient(request)) {
            ret = new CustomPrintWriter(out, Exporter.WINDOWS_SEPARATOR);
        } else {
            ret = new PrintWriter(out);
        }
        return ret;
    }

}
