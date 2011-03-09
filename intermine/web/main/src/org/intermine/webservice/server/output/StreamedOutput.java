package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Immediately as the data or error messages are added they are streamed via http connection.
 * So the data can not be retrieved later. Before streaming they are formatted with
 * associated formatter.
 * @author Jakub Kulaviak
 **/
public class StreamedOutput extends Output
{

    private int resultsCount = 0;

    private PrintWriter writer;

    private Formatter formatter;

    private boolean headerPrinted = false;

    /** Constructor.
     * @param writer writer where the data will be printed
     * @param formatter associated formatter that formats data
     * before printing
     */
    public StreamedOutput(PrintWriter writer, Formatter formatter) {
        this.writer = writer;
        this.formatter = formatter;
    }

    private void printHeader() {
        String header = formatter.formatHeader(getHeaderAttributes());
        if (header != null && header.length() > 0) {
            writer.println(header);
        }
        headerPrinted = true;
    }

    /** Forwards data to associated writer
     * @param item data
     * **/
    @Override
    public void addResultItem(List<String> item) {
        if (!headerPrinted) { printHeader(); }
        writer.println(formatter.formatResult(item));
        resultsCount++;
    }

    /** Returns associated writer
     * @return writer
     * **/
    public PrintWriter getWriter() {
        return writer;
    }

    /** Sets associated writer
     * @param writer writer
     * **/
    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }


    /**
     * Finish writing. Writes footer ...
     */
    @Override
    public void flush() {
        if (headerPrinted) {
            writer.print(formatter.formatFooter());
        }
        writer.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeaderAttributes(Map<String, String> attributes) {
        if (headerPrinted) {
            throw new RuntimeException("Attempt to set header attributes "
                + "although header was printed already.");
        }
        super.setHeaderAttributes(attributes);
    }

    /**
     * {@inheritDoc}
     */
    public int getResultsCount() {
        return resultsCount;
    }
}
