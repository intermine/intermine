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

/**
 * RunQueryMonitor that writes repeatedly to an http response output stream
 * while a query runs and cancels the query if an error occurs while writing
 * to the client.
 *
 * @author Tom Riley
 */
public class RunQueryMonitorDots implements RunQueryMonitor
{
    /** Number of times queryProgress has been called. */
    //protected int tickCount = 0;
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
     * @param results  the Results object associated with the running query
     */
    public void queryProgress(Results results) {
        //tickCount++;
        try {
            //if (tickCount % 40 == 0) {
            //    writer.write("<br>");
            //}
            writer.write(". ");
            writer.flush();
        } catch (IOException _) {
            cancelQuery(results);
        }
    }
    
    /**
     * Cancel a query given a Results object.
     *
     * @param results  the Results object associated with the running query
     */
    public void cancelQuery(Results results) {
        // write me
    }
    
    /**
     * Forward the client to another URL.
     *
     * @param url the URL to forward to
     * @throws IOException if writing to the response stream fails
     */
    public void forwardClient(String url) throws IOException {
        writer.write("<script language=\"JavaScript\">document.location=\"" + url + "\"</script>\n\n");
        writer.flush();
    }
}
