package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * @author Alex Kalderimis
 **/
public class StreamedOutput extends Output
{

    private int resultsCount = 0;

    private PrintWriter writer;

    private final Formatter formatter;

    private boolean headerPrinted = false;

    private final String separator;

    /** Constructor.
     * @param writer writer where the data will be printed
     * @param formatter associated formatter that formats data
     * before printing
     */
    public StreamedOutput(PrintWriter writer, Formatter formatter) {
        this.writer = writer;
        this.formatter = formatter;
        this.separator = null;
    }

    /** Constructor.
     * @param writer writer where the data will be printed
     * @param formatter associated formatter that formats data
     * before printing
     * @param separator Platform specific line-separator for the request.
     */
    public StreamedOutput(PrintWriter writer, Formatter formatter, String separator) {
        this.writer = writer;
        this.formatter = formatter;
        this.separator = separator;
    }

    private void ensureHeaderIsPrinted() {
        if (!headerPrinted) {
            String header = formatter.formatHeader(getHeaderAttributes());
            if (header != null && header.length() > 0) {
                writeLn(header);
            }
            headerPrinted = true;
        }
        return;
    }

    private void writeLn(String s) {
        writer.print(s);
        if (separator == null) {
            writer.println();
        } else {
            writer.print(separator);
        }
    }

    /** Forwards data to associated writer
     * @param item data
     * **/
    @Override
    public void addResultItem(List<String> item) {
        ensureHeaderIsPrinted();
        writeLn(formatter.formatResult(item));
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
        ensureHeaderIsPrinted();
        writer.print(formatter.formatFooter(getError(), getCode()));
        writer.flush();
        writer.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeaderAttributes(Map<String, Object> attributes) {
        if (headerPrinted) {
            throw new RuntimeException("Attempt to set header attributes "
                + "although header was printed already.");
        }
        super.setHeaderAttributes(attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getResultsCount() {
        return resultsCount;
    }
}
