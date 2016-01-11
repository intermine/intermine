package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.io.PrintWriter;


/**
 * Customized PrintWriter that terminates end of the lines with defined line
 * separator. Can be used for example for windows export with CR+LF at end of the lines.
 * @author Jakub Kulaviak
 **/
public class CustomPrintWriter extends PrintWriter
{

    private String lineSeparator;

    /**
     * Constructor.
     * @param out output stream
     * @param lineSeparator line separator
     */
    public CustomPrintWriter(OutputStream out, String lineSeparator) {
        super(out);
        this.lineSeparator = lineSeparator;
    }


    /**
     *{@inheritDoc}
     */
    @Override public void println() {
        write(lineSeparator);
    }
}
