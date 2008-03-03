package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;
/**
 * Formats and writes formatted text data.
 *
 * @author Kim Rutherford
 * @author Jakub Kulaviak
 */

public class FormattedTextWriter
{
    private PrintStream printStream;

    private boolean[] columnVisible;

    private int maxRows = Integer.MAX_VALUE;

    private int[] columnOrder;
    
    protected static final Logger LOG = Logger.getLogger(FormattedTextWriter.class);

    /**
     * Constructor.
     * @param os the OutputStream to write to
     * @param columnOrder the real order of the column in the output - a map from the column index
     * in the output to the column index in the listOfLists.  null means use the original order
     * @param columnVisible an array mapping from columns in listOfLists to their visibility.  null
     * means all columns are visible
     * @param maxRows the maximum number of rows to output - read only range 0..maxRows-1 from
     * listOfLists.  -1 means output all
     * toString() should be called instead
     */
    public FormattedTextWriter(OutputStream os, int[] columnOrder, boolean[] columnVisible, 
            int maxRows) {
        printStream = new PrintStream(os);
        this.columnOrder = columnOrder;
        this.columnVisible = columnVisible;
        this.maxRows = maxRows;
    }

    /**
     * Constructor.
     * @param os the OutputStream to write to
     */    
    public FormattedTextWriter(OutputStream os) {
        printStream = new PrintStream(os);
    }
    
    /**
     * Write a list of lists using tab characters to delimit the fields.
     * @param listOfLists the table to write
     */
    public void writeTabDelimitedTable(List<List<Object>> listOfLists) {
        writeDelimitedTable(listOfLists, '\t', false);
    }

    /**
     * Write a list of lists using comma characters to delimit the fields.
     * @param listOfLists the table to write
     */
    public void writeCSVTable(List<List<Object>> listOfLists) {
        writeDelimitedTable(listOfLists,  ',', true);
    }

    /**
     * Write a list of lists using the given delimiter character to delimit the fields.
     * @param listOfLists the table to write
     * @param delimiter the character to use to separate the fields in the output
     * @param quote quote all strings
     * listOfLists
     */
    public void writeDelimitedTable(List<List<Object>> listOfLists, char delimiter, boolean quote) {
 
        for (int i = 0; i < listOfLists.size(); i++) {
            List<Object> origRow = listOfLists.get(i);
            if (i == maxRows) {
                break;
            }
            List<Object> row = reorderRow(columnOrder, columnVisible, origRow);
            writeRow(delimiter, quote, row);
        }
        printStream.flush();
    }

    private void writeRow(char delimiter, boolean quote, List<Object> row) {
        for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
            Object o = row.get(columnIndex);

            if (o == null) {
                writeUnQuoted("");
            } else {
                if (o instanceof Number
                    || (!quote && o.toString().indexOf(delimiter) < 0
                        && !o.toString().equals(""))) {
                    writeUnQuoted(o);
                } else {
                    writeQuoted(o);
                }
            }

            if (columnIndex < row.size() - 1) {
                printStream.print(delimiter);
            }
        }

        printStream.println();
    }

    private static List<Object> reorderRow(int[] columnOrder, boolean[] columnVisible,
            List<Object> row) {
        List<Object> realRow = new ArrayList<Object>();

        for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
            int realColumnIndex;

            if (columnOrder == null) {
                realColumnIndex = columnIndex;
            } else {
                realColumnIndex = columnOrder[columnIndex];
            }

            Object o = row.get(realColumnIndex);

            if (columnVisible != null && !columnVisible[columnIndex]) {
                continue;
            }

            realRow.add(o);
        }
        return realRow;
    }

    private static final String QUOTE = "\"";

    /**
     * Write an Object as a String to an OutputStream with quoting.  Any double-quote characters
     * are quoted doubling them.  The output will be surrounded by double quotes.
     * ie.  fred"eric -&gt; "fred""eric"
     * @param o the Object to write
     */
    public void writeQuoted(Object o) {
        // don't use toString() in case o is null
        String objectString = "" + o;

        StringBuffer buffer = new StringBuffer();

        final StringTokenizer tokeniser =
            new StringTokenizer (objectString, QUOTE, true);

        buffer.append(QUOTE);

        while (tokeniser.hasMoreTokens ()) {
            final String tokenValue = tokeniser.nextToken ();

            if (tokenValue.equals(QUOTE)) {
                // quotes are quoted by doubling
                buffer.append(tokenValue);
                buffer.append(tokenValue);
            } else {
                buffer.append(tokenValue);
            }
        }

        buffer.append('"');

        printStream.print(buffer);
    }

    /**
     * Write an Object as a String to an OutputStream with quoting special characters
     * @param o the Object to write
     */
    public void writeUnQuoted(Object o) {
        printStream.print(o.toString());
    }

    /**
     * @return columns visible
     */
    public boolean[] getColumnVisible() {
        return columnVisible;
    }
    
    /**
     * @param columnVisible columns visible
     */
    public void setColumnVisible(boolean[] columnVisible) {
        this.columnVisible = columnVisible;
    }
    
    /**
     * @return max rows
     */
    public int getMaxRows() {
        return maxRows;
    }
    
    /**
     * @param maxRows max rows
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * @return column order
     */
    public int[] getColumnOrder() {
        return columnOrder;
    }

    /**
     * @param columnOrder column order
     */
    public void setColumnOrder(int[] columnOrder) {
        this.columnOrder = columnOrder;
    }
}
