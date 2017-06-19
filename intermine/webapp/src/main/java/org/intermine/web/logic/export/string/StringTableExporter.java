package org.intermine.web.logic.export.string;

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
import java.util.List;

import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.RowFormatter;


/**
 * Simple exporter exporting table of strings. Format depends at given rowFormatter.
 * @author Jakub Kulaviak
 **/
public class StringTableExporter
{

    private PrintWriter writer;
    private RowFormatter rowFormatter;

    /**
     * Constructor.
     * @param writer used writer
     * @param rowFormatter object formatting row from list of objects (in this case strings)
     * to string flushed  as line
     */
    public StringTableExporter(PrintWriter writer, RowFormatter rowFormatter) {
        this.writer = writer;
        this.rowFormatter = rowFormatter;
    }

    /**
     * Perform export.
     * @param rows rows to be exported
     */
    public void export(List<List<String>> rows) {
        try {
            for (List<String> row : rows) {
                writer.println(rowFormatter.format(row));
            }
            writer.flush();
        } catch (RuntimeException e) {
            throw new ExportException("Export failed.", e);
        }
    }
}
