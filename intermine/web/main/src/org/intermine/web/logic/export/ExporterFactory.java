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

import java.io.OutputStream;


/**
 * @author Jakub Kulaviak
 **/
public class ExporterFactory
{
    /** flag denoting that comma separated values export format is required **/
    public static final int CSV = 0;
    /** flag denoting that tab separated values export format is required **/
    public static final int TAB = 1;
    /** flag denoting that excel export format is required **/
    public static final int EXCEL = 2;
    
    /**
     * Creates exporter.
     * @param out output stream
     * @param format required format
     * @return exporter that makes the export
     */
    public static Exporter createExporter(OutputStream out, int format) {
        switch (format) {
        case TAB:
            RowFormatter rowFormatter = new RowFormatterImpl("\t", false);
            return new ExporterImpl(out, rowFormatter);
        case CSV:
            rowFormatter = new RowFormatterImpl(",", true);
            return new ExporterImpl(out, rowFormatter);
        case EXCEL:
            return new ExcelExporter(out);
        default:
            throw new IllegalArgumentException("Unknown format.");
        }
    }
}
    