package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Results;

import java.io.Writer;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

/**
 * RunQueryMonitor that writes repeatedly to an http response output stream
 * while a query runs and cancels the query if an error occurs while writing
 * to the client.
 *
 * @author Tom Riley
 */
public class RunQueryMonitorDots implements RunQueryMonitor
{
    /** Writer to draw progress dots to. */
    protected Writer writer;
    
    /**
     * Construct a new instance of RunQueryMonitorDots.
     *
     * @param writer the http response output stream writer 
     */
    public RunQueryMonitorDots(Writer writer) {
        this.writer = writer;
    }
    
    /**
     * Called intermittently while a query is run.
     *
     * @see RunQueryMonitor#queryProgress(HttpServletRequest, Results)
     */
    public boolean queryProgress(HttpServletRequest request, Results results) {
        try {
            writer.write(". ");
            writer.flush();
            return true;
        } catch (IOException _) {
            return false;
        }
    }
    
    /**
     * Forward the client to another URL.
     *
     * @param url the URL to forward to
     * @throws IOException if writing to the response stream fails
     */
    public void forwardClient(String url) throws IOException {
        writer.write("<script language=\"JavaScript\">document.location=\""
                + url + "\"</script>\n\n");
        writer.flush();
    }
}
