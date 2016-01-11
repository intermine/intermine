package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;

/**
 * HTMLOutput extends is similar to MemoryOutput, only difference is, that it prints out errors
 * immediately to output.
 * @author Jakub Kulaviak
 **/
public class HTMLOutput extends MemoryOutput
{
    private PrintWriter writer;

    private boolean htmlHeaderWritten = false;

    /**
     * Constructor.
     * @param writer output stream writer
     */
    public HTMLOutput(PrintWriter writer) {
        this.writer = writer;
    }

    /**
     * Prints html closing tags.
     */
    @Override
    public void flush() {
        super.flush();
        if (htmlHeaderWritten) {
            writer.println("</body>");
            writer.println("</html>");
        }
    }
}
