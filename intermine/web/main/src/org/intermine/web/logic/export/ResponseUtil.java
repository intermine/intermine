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
 * Response util that sets content type and header for various formats and has
 * util methods for setting headers controlling cache.
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
        setNoCache(response);
        setExcelContentType(response);
        response.setContentType("Application/vnd.ms-excel");
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
        setNoCache(response);
        setTabContentType(response);
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
    }
    
    /**
     * Sets response header and content type for comma separated
     * values output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setCSVHeader(HttpServletResponse response, String fileName) {
        setNoCache(response);
        setCSVContentType(response);
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
    }

    /**
     * Sets response header and content type for XML output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setXMLHeader(HttpServletResponse response, String fileName) {
        setNoCache(response);
        setXMLContentType(response);
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
    }


    /**
     * Sets response header and content type for plain text output.
     * @param response response
     * @param fileName file name of downloaded file
     */
    public static void setPlainTextHeader(HttpServletResponse response, String fileName) {
        setNoCache(response);
        setPlainTextContentType(response);
        response.setHeader("Content-Disposition ",
                           "inline; filename=\"" + fileName + "\"");        
    }
    
    /**
     * Sets that the result must not be cached. Old implementation was set
     * Cache-Control to no-cache,no-store,max-age=0. But this caused problems 
     * in IE. File couldn't be opened directly.   
     * @param response response
     */
    public static void setNoCache(HttpServletResponse response) {
        // http://www.phord.com/experiment/cache/
        // http://support.microsoft.com/kb/243717
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "must-revalidate, max-age=0");
    }
    
    /**
     * Sets enforced no-cache headers to completely disable cache for this response. 
     * Page is reloaded always, for example when the user uses Go Back button.  
     * @param response response
     */
    public static void setNoCacheEnforced(HttpServletResponse response) {
        // should work for firefox and IE to refresh always the page when back button is pressed
        // http://forums.mozillazine.org/viewtopic.php?f=25&t=673135&start=30
        response.setHeader("Cache-Control", "max-age=0, must-revalidate, no-store, no-cache");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "Wed, 11 Jan 1984 05:00:00 GMT");            
    }
    
    /**
     * Sets tab separated values content type.
     * @param response response
     */
    public static void setTabContentType(HttpServletResponse response) {
        response.setContentType("text/tab-separated-values");
    }
    
    /**
     * Sets comma separated values content type.
     * @param response response
     */    
    public static void setCSVContentType(HttpServletResponse response) {
        response.setContentType("text/comma-separated-values");
    }
    
    /**
     * Sets plain text content type.
     * @param response response
     */
    public static void setPlainTextContentType(HttpServletResponse response) {
        response.setContentType("text/plain");
    }
    
    /**
     * Sets Excel content type.
     * @param response response
     */
    private static void setExcelContentType(HttpServletResponse response) {
        response.setContentType("Application/vnd.ms-excel");
    }
 
    /**
     * Sets XML content type.
     * @param response response
     */
    public static void setXMLContentType(HttpServletResponse  response) {
        response.setContentType("text/xml");
    }
    
    /**
     * Sets HTML content type.
     * @param response response
     */
    public static void setHTMLContentType(HttpServletResponse response) {
        response.setContentType("text/html");
    }
}