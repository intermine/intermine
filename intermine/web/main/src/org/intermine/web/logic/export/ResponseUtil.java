package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletResponse;


/**
 * Response util that sets content type and header for various formats.
 * @author Jakub Kulaviak
 **/
public class ResponseUtil
{

    /**
     * Sets response header and content type for excel output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setExcelHeader(HttpServletResponse response, String fileName) {
        response.setContentType("Application/vnd.ms-excel");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + fileName + "\"");        
    }

    /**
     * Sets response header and content type for tab separated 
     * values output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setTabHeader(HttpServletResponse response, String fileName) {
        response.setContentType("text/tab-separated-values");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
    }
    
    /**
     * Sets response header and content type for comma separated
     * values output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setCSVHeader(HttpServletResponse response, String fileName) {
        response.setContentType("text/comma-separated-values");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
    }

    /**
     * Sets response header and content type for plain text output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setPlainTextHeader(HttpServletResponse response, String fileName) {
        response.setContentType("text/plain");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition ",
                           "inline; filename=\"" + fileName + "\"");        
    }
}
