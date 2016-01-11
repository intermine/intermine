package org.intermine.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;

/**
 * Class for methods and util methods for parsing csv files and others.
 * @author Kim Rutherford
 * @author Jakub Kulaviak
 **/
public final class FormattedTextParser
{
    private static final char DEFAULT_QUOTE = '"';

    private FormattedTextParser() {
        // don't
    }

    /**
     * Return an Iterator over a tab delimited file.  Iterator.next() splits the current line at the
     * tabs and returns a String[] of the bits.  Double quotes are ignored.
     * Lines beginning with # are ignored.
     * @param reader the Reader to read from
     * @return an Iterator over the lines of the Reader
     * @throws IOException if there is an error while reading from the Reader
     */
    public static Iterator<String[]> parseTabDelimitedReader(final Reader reader)
        throws IOException {
        return parseDelimitedReader(reader, '\t', CSVParser.NULL_CHARACTER);
    }

    /**
     * Return an Iterator over a comma/tab delimited file.  Iterator.next() splits the current line
     * and returns a String[] of the bits, stripped of all quotes.
     * Lines beginning with # are ignored.
     * @param reader the Reader to read from
     * @return an Iterator over the lines of the Reader
     * @throws IOException if there is an error while reading from the Reader
     */
    public static Iterator<String[]> parseCsvDelimitedReader(final Reader reader)
        throws IOException {
        return parseDelimitedReader(reader, ',', DEFAULT_QUOTE);
    }

    /**
     * Return an Iterator over a delimited file.  Iterator.next() splits the current line
     * and returns a String[] of the bits.  Double quotes are ignored.
     * Lines beginning with # are ignored.
     * @param reader the Reader to read from
     * @param delim character to split the files, eg "|"
     * @return an Iterator over the lines of the Reader
     * @throws IOException if there is an error while reading from the Reader
     */
    public static Iterator<String[]> parseDelimitedReader(final Reader reader, final char delim)
        throws IOException {
        return parseDelimitedReader(reader, delim, CSVParser.NULL_CHARACTER);
    }

    /**
     * Return an Iterator over a delimited file.  Iterator.next() splits the current line
     * and returns a String[] of the bits.
     * Lines beginning with # are ignored.
     * @param reader the Reader to read from
     * @param delim character to split the files, eg "|"
     * @param quoteChar the character to use for quoted elements
     * @return an Iterator over the lines of the Reader
     * @throws IOException if there is an error while reading from the Reader
     */
    private static Iterator<String[]> parseDelimitedReader(final Reader reader, final char delim,
            char quoteChar) throws IOException {
        final CSVReader bufferedReader = new CSVReader(reader, delim, quoteChar);

        return new Iterator<String[]>() {
            String[] currentLine = null;
            {
                currentLine = getNextNonCommentLine();
            }

            @Override
            public boolean hasNext() {
                return currentLine != null;
            }

            @Override
            public String[] next() {
                if (currentLine == null) {
                    throw new NoSuchElementException();
                }
                String[] lastLine = currentLine;

                try {
                    currentLine = getNextNonCommentLine();
                } catch (IOException e) {
                    throw new RuntimeException("error while reading from " + reader, e);
                }
                return lastLine;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private String[] getNextNonCommentLine() throws IOException {
                String[] line = null;
                while (true) {
                    line = bufferedReader.readNext();
                    if (line == null) {
                        // EOF
                        return null;
                    }
                    if (line[0] != null && line[0].startsWith("#")) {
                        // skip comments, go to next line
                        continue;
                    }
                    // legal line
                    return line;
                }
            }
        };
    }
}
