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
import java.util.Iterator;

/**
 * A utility class for generating multi-column aligned text tables.
 *
 * @author Matthew Wakeling
 */
public class TextTable
{
    /** An object representing a horizontal line separating two rows - treated as a row. */
    public static final String[] ROW_SEPARATOR = new String[0];

    private ArrayList rows = new ArrayList();
    private ArrayList columnWidths = new ArrayList();
    private boolean leftBound;
    private boolean columnSeparators;
    private boolean rightBound;
    
    /**
     * Creates a new TextTable object, with or without column separating lines.
     *
     * @param leftBound true for a vertical line to the left of the leftmost column
     * @param columnSeparators true for vertical lines separating the columns
     * @param rightBound true for a vertical line to the right of the rightmost column
     */
    public TextTable(boolean leftBound, boolean columnSeparators, boolean rightBound) {
        this.leftBound = leftBound;
        this.columnSeparators = columnSeparators;
        this.rightBound = rightBound;
    }

    /**
     * Adds a single row to the table.
     *
     * @param row an array of Strings - each String being a column
     */
    public void addRow(String row[]) {
        for (int i = 0; i < row.length; i++) {
            int width = row[i].length();
            if (i >= columnWidths.size()) {
                columnWidths.add(i, new Integer(width));
            } else {
                int previousWidth = ((Integer) columnWidths.get(i)).intValue();
                if (width > previousWidth) {
                    columnWidths.set(i, new Integer(width));
                }
            }
        }
        rows.add(row);
    }

    /**
     * Returns a text-formatted representation of this table.
     *
     * @return a String, either empty or with a trailing newline
     */
    public String toString() {
        StringBuffer retval = new StringBuffer();
        Iterator rowIter = rows.iterator();
        boolean firstRow = true;
        while (rowIter.hasNext()) {
            String row[] = (String []) rowIter.next();
            if (row == ROW_SEPARATOR) {
                if (leftBound) {
                    if (firstRow) {
                        if (!rowIter.hasNext()) {
                            retval.append("--");
                        } else {
                            retval.append(".-");
                        }
                    } else if (!rowIter.hasNext()) {
                        retval.append("`-");
                    } else {
                        retval.append("|-");
                    }
                }
                for (int i = 0; i < columnWidths.size(); i++) {
                    int width = ((Integer) columnWidths.get(i)).intValue();
                    for (int o = 0; o < width; o++) {
                        retval.append('-');
                    }
                    if (i + 1 < columnWidths.size()) {
                        if (columnSeparators) {
                            if (firstRow || (!rowIter.hasNext())) {
                                retval.append("---");
                            } else {
                                retval.append("-|-");
                            }
                        } else {
                            retval.append("-");
                        }
                    }
                }
                if (rightBound) {
                    if (firstRow) {
                        if (!rowIter.hasNext()) {
                            retval.append("--");
                        } else {
                            retval.append("-.");
                        }
                    } else if (!rowIter.hasNext()) {
                        retval.append("-'");
                    } else {
                        retval.append("-|");
                    }
                }
            } else {
                if (leftBound) {
                    retval.append("| ");
                }
                for (int i = 0; i < columnWidths.size(); i++) {
                    int width = ((Integer) columnWidths.get(i)).intValue();
                    int textLength;
                    if (i >= row.length) {
                        textLength = 0;
                    } else {
                        retval.append(row[i]);
                        textLength = row[i].length();
                    }
                    for (int o = textLength; o < width; o++) {
                        retval.append(' ');
                    }
                    if (i + 1 < columnWidths.size()) {
                        if (columnSeparators) {
                            retval.append(" | ");
                        } else {
                            retval.append(" ");
                        }
                    }
                }
                if (rightBound) {
                    retval.append(" |");
                }
            }
            retval.append("\n");
            firstRow = false;
        }
        return retval.toString();
    }
}
