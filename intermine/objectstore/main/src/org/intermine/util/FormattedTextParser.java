package org.intermine.util;

/*
 * Copyright (C) 2002-2009 FlyMine
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
public class FormattedTextParser
{

    /**
     * Return an Iterator over a tab delimited file.  Iterator.next() splits the current line at the
     * tabs and returns a String[] of the bits.  No attempt is made to deal with quoted tabs.
     * Lines beginning with # are ignored.
     * @param reader the Reader to read from
     * @return an Iterator over the lines of the Reader
     * @throws IOException if there is an error while reading from the Reader
     */
    public static Iterator parseTabDelimitedReader(final Reader reader) throws IOException {
        return parseDelimitedReader(reader, false, "\t");
    }
    
    /**
     * Return an Iterator over a comma delimited file.  Iterator.next() splits the current line at 
     * the commas and returns a String[] of the bits, stripped of all quotes.
     * Lines beginning with # are ignored.
     * @param reader the Reader to read from
     * @return an Iterator over the lines of the Reader
     * @throws IOException if there is an error while reading from the Reader
     */
    public static Iterator<String[]> parseCsvDelimitedReader(final Reader reader) 
    throws IOException {
        return parseDelimitedReader(reader, true, ",");
    }
    
    private static Iterator parseDelimitedReader(final Reader reader, final boolean stripQuotes, 
                                                 final String delim) 
    throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(reader);

        return new Iterator() {
            String currentLine = null;

            {
                currentLine = getNextNonCommentLine();
            }

            public boolean hasNext() {
                return currentLine != null;
            }

            public Object next() {
                if (currentLine == null) {
                    throw new NoSuchElementException();
                } else {
                    String lastLine = currentLine;

                    try {
                        currentLine = getNextNonCommentLine();
                    } catch (IOException e) {
                        throw new RuntimeException("error while reading from " + reader, e);
                    }
                    
                    if (stripQuotes) {
                        StrMatcher delimMatcher = null;
                        
                        if (delim.equals(",")) {
                            delimMatcher = StrMatcher.commaMatcher();
                        } else {
                            delimMatcher = StrMatcher.tabMatcher();
                        }
                        StrTokenizer tokeniser
                        = new StrTokenizer(lastLine, delimMatcher, StrMatcher.doubleQuoteMatcher());
                        tokeniser.setEmptyTokenAsNull(false);
                        tokeniser.setIgnoreEmptyTokens(false);
                        return tokeniser.getTokenArray();
                    }                    
                    return StringUtil.split(lastLine, delim);
                }
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
                    } else {
                        if (!line.startsWith("#")) {
                            break;
                        }
                    }
                }

                return line;
            }
        };
    }
}
