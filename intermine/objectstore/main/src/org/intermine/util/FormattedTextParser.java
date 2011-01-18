package org.intermine.util;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;

/**
 * Class for methods and util methods for parsing csv files and others.
 * @author Kim Rutherford
 * @author Jakub Kulaviak
 **/
public final class FormattedTextParser
{
    private FormattedTextParser() {
    }

    /**
     * Return an Iterator over a tab delimited file.  Iterator.next() splits the current line at the
     * tabs and returns a String[] of the bits.  No attempt is made to deal with quoted tabs.
     * Lines beginning with # are ignored.
     * @param reader the Reader to read from
     * @return an Iterator over the lines of the Reader
     * @throws IOException if there is an error while reading from the Reader
     */
    public static Iterator<String[]> parseTabDelimitedReader(final Reader reader)
        throws IOException {
        return parseDelimitedReader(reader, false, "\t");
    }

    /**
     * Return an Iterator over a tab delimited file.  Iterator.next() splits the current line at the
     * tabs and returns a String[] of the bits.  No attempt is made to deal with quoted tabs.
     * Lines beginning with # are ignored.
     * @param reader the Reader to read from
     * @param stripQuotes whether or not to remove double quotes
     * @return an Iterator over the lines of the Reader
     * @throws IOException if there is an error while reading from the Reader
     */
    public static Iterator<String[]> parseTabDelimitedReader(final Reader reader,
            boolean stripQuotes) throws IOException {
        return parseDelimitedReader(reader, stripQuotes, "\t");
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
        return parseDelimitedReader(reader, true, ",");
    }

    /**
     * Return an Iterator over a delimited file.  Iterator.next() splits the current line
     * and returns a String[] of the bits.
     * Lines beginning with # are ignored.
     * @param reader the Reader to read from
     * @param delim character to split the files, eg "|"
     * @return an Iterator over the lines of the Reader
     * @throws IOException if there is an error while reading from the Reader
     */
    public static Iterator<String[]> parseDelimitedReader(final Reader reader, char delim)
        throws IOException {
        return parseDelimitedReader(reader, false, String.valueOf(delim));
    }

    private static Iterator<String[]> parseDelimitedReader(final Reader reader,
            final boolean stripQuotes, final String delim) throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(reader);

        return new Iterator<String[]>() {
            String currentLine = null;

            {
                currentLine = getNextNonCommentLine();
            }

            public boolean hasNext() {
                return currentLine != null;
            }

            public String[] next() {
                if (currentLine == null) {
                    throw new NoSuchElementException();
                }
                String lastLine = currentLine;

                try {
                    currentLine = getNextNonCommentLine();
                } catch (IOException e) {
                    throw new RuntimeException("error while reading from " + reader, e);
                }

                if (stripQuotes) {
                    StrMatcher delimMatcher = null;
                    StrTokenizer tokeniser = null;
                    if (",".equals(delim)) {
                        delimMatcher = StrMatcher.commaMatcher();
                        tokeniser = new StrTokenizer(lastLine, delimMatcher,
                                StrMatcher.doubleQuoteMatcher());
                    } else if ("\t".equals(delim)) {
                        delimMatcher = StrMatcher.tabMatcher();
                        tokeniser = new StrTokenizer(lastLine, delimMatcher,
                                StrMatcher.doubleQuoteMatcher());
                    } else {
                        tokeniser = new StrTokenizer(lastLine, delim);
                    }

                    tokeniser.setEmptyTokenAsNull(false);
                    tokeniser.setIgnoreEmptyTokens(false);
                    return tokeniser.getTokenArray();
                }
                return StringUtil.split(lastLine, delim);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            private String getNextNonCommentLine() throws IOException {
                String line = null;

                while (true) {
                    line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (!line.startsWith("#")) {
                        break;
                    }
                }
                return line;
            }
        };
    }
}
