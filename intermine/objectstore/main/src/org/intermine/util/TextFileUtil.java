package org.intermine.util;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.xml.utils.UnImplNode;

/**
 * Utility methods for dealing with text files.
 *
 * @author Kim Rutherford
 */

public abstract class TextFileUtil
{
    /**
     * Write a list of lists using tab characters to delimit the fields.
     * @param os the OutputStream to write to
     * @param listOfLists the table to write
     * @param columnOrder the real order of the column in the output - a map from the column index
     * in the output to the column index in the listOfLists
     * @param columnVisible an array mapping from columns in listOfLists to their visibility
     * @param maxRows the maximum number of rows to output - read only range 0..maxRows-1 from
     * listOfLists
     */
    public static void writeTabDelimitedTable(OutputStream os, List listOfLists,
                                              int [] columnOrder, boolean [] columnVisible,
                                              int maxRows) {
        writeDelimitedTable(os, listOfLists, columnOrder, columnVisible, maxRows, '\t');
    }

    /**
     * Write a list of lists using comma characters to delimit the fields.
     * @param os the OutputStream to write to
     * @param listOfLists the table to write
     * @param columnOrder the real order of the column in the output - a map from the column index
     * in the output to the column index in the listOfLists
     * @param columnVisible an array mapping from columns in listOfLists to their visibility
     * @param maxRows the maximum number of rows to output - read only range 0..maxRows-1 from
     * listOfLists
     */
    public static void writeCSVTable(OutputStream os, List listOfLists,
                                     int [] columnOrder, boolean [] columnVisible,
                                     int maxRows) {
        writeDelimitedTable(os, listOfLists, columnOrder, columnVisible, maxRows, ',');
    }

    /**
     * Write a list of lists using the given delimiter character to delimit the fields.
     * @param os the OutputStream to write to
     * @param listOfLists the table to write
     * @param columnOrder the real order of the column in the output - a map from the column index
     * in the output to the column index in the listOfLists
     * @param columnVisible an array mapping from columns in listOfLists to their visibility
     * @param delimiter the character to use to separate the fields in the output
     * @param maxRows the maximum number of rows to output - read only range 0..maxRows-1 from
     * listOfLists
     */
    public static void writeDelimitedTable(OutputStream os, List listOfLists,
                                           int [] columnOrder, boolean [] columnVisible,
                                           int maxRows, char delimiter) {
        PrintStream printStream = new PrintStream(os);

        String delimiters = "" + delimiter;

        // a count of the columns that are invisble - used to get the correct columnIndex
        int invisibleColumns = 0;

        for (int columnIndex = 0; columnIndex < columnVisible.length; columnIndex++) {
            if (!columnVisible[columnIndex]) {
                invisibleColumns++;
            }
        }

        int rowCount = 0;

        Iterator rowIterator = listOfLists.iterator();
        while (rowIterator.hasNext()) {
            if (rowCount == maxRows) {
                break;
            }

            List row = (List) rowIterator.next();

            List realRow = new ArrayList();
            
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                int realColumnIndex = columnOrder[columnIndex];

                Object o = row.get(realColumnIndex);

                if (!columnVisible[columnIndex]) {
                    continue;
                }

                realRow.add(o);
                
                
            }
            
            for (int columnIndex = 0; columnIndex < realRow.size(); columnIndex++) {
                Object o = realRow.get(columnIndex);

                if (o instanceof Number) {
                    writeUnQuoted(printStream, o);
                } else {
                    writeQuoted(printStream, o);
                }

                if (columnIndex < realRow.size() - 1) {
                    printStream.print(delimiter);
                }
            }
            int realRowSize = row.size () - 1 - invisibleColumns;

            printStream.println();

            rowCount++;
        }

        printStream.flush();
    }

    private static final String QUOTE = "\"";

    /**
     * Write an Object as a String to an OutputStream with quoting.  Any double-quote characters
     * are quoted doubling them.  The output will be surrounded by double quotes.
     * ie.  fred"eric -> "fred""eric"
     * @param printStream the PrintStream to write to
     * @param o the Object to write
     */
    public static void writeQuoted(PrintStream printStream, Object o) {
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
     * @param printStream the PrintStream to write to
     * @param o the Object to write
     */
    public static void writeUnQuoted(PrintStream printStream, Object o) {
        printStream.print(o.toString());
    }

    /**
     * Return an Iterator over a tab delimited file.  Iterator.next() splits the current line at the
     * tabs and returns a String[] of the bits.  No attempt is made to deal with quoted tabs.
     */
    public static Iterator parseTabDelimitedReader(final Reader reader) throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(reader);

        return new Iterator() {
            String line = null;

            {
                line = bufferedReader.readLine();
            }

            public boolean hasNext() {
                return line != null;
            }

            public Object next() {
                if (line == null) {
                    throw new NoSuchElementException();
                } else {
                    String currentLine = line;
                    try {
                        line = bufferedReader.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("error while reading from " + reader, e);
                    }

                    return StringUtil.split(currentLine, "\t");
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
