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

import java.util.List;

import javax.servlet.ServletOutputStream;

import org.intermine.util.FormattedTextWriter;


/**
 * @author Jakub Kulaviak
 **/
public class TableExporterFactory
{
    /** csv format flag **/
    public static final int CSV = 0;
    /** tab format flag **/
    public static final int TAB = 1;
    private List<List<Object>> rows;
    private ServletOutputStream outputStream;
    private int[] order;
    private boolean[] visible;
    private int maxResults;
    private int format;

    /**
     * Constructor.
     * @param rows data to be exported
     * @param outputStream output stream 
     * @param order order
     * @param visible visible
     * @param maxResults max results
     * @param format format
     */
    public TableExporterFactory(List<List<Object>> rows,
            ServletOutputStream outputStream, int[] order, boolean[] visible,
            int maxResults, int format) {
        this.rows = rows;
        this.outputStream = outputStream;
        this.order = order;
        this.visible = visible;
        this.maxResults = maxResults;
        this.format = format;
    }

    /**
     * Creates exporter.
     * @return exporter
     */
    public Exporter createExporter() {
        return new Exporter() {

            public void export() {
                switch (format) {
                    case CSV:
                      new FormattedTextWriter(outputStream, order, visible, 
                              maxResults).writeCSVTable(rows);
                      break;
                    case TAB:
                      new FormattedTextWriter(outputStream, order, visible, 
                              maxResults).writeTabDelimitedTable(rows);
                    default:
                        throw new RuntimeException("Invalid specified format.");
                }
            }
            
        };
    }
}
