package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.StringTokenizer;


/**
 * Implements RowFormatter interface.
 * @author Jakub Kulaviak
 **/
public class RowFormatterImpl implements RowFormatter
{

    private String delimiter;
    private boolean quoted;
    private static final String QUOTE = "\"";

    /**
     * Constructor.
     * @param delimiter used delimiter
     * @param quoted flag if the strings should be quoted
     */
    public RowFormatterImpl(String delimiter, boolean quoted) {
        this.delimiter = delimiter;
        this.quoted = quoted;
    }

    /**
     * Formats objects to string. Just convert objects to strings
     * with toString method and quotes it if required or needed
     * because delimiter is inside of string. Number is formatted
     * without quotations.
     * @param row row to be formatted
     * @return resulted string
     */
    public String format(List<Object> row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.size(); i++) {
            Object o = row.get(i);
            if (o != null) {
                if (o instanceof Number
                    || (!quoted && o.toString().indexOf(delimiter) < 0
                        && !"".equals(o.toString()))) {
                    sb.append(getUnQuoted(o));
                } else {
                    sb.append(getQuoted(o));
                }
            } else {
                sb.append(getQuoted(""));
            }
            if (i < row.size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * Formats Object to String with quoting.  Any double-quote characters
     * are quoted doubling them.  The output will be surrounded by double quotes.
     * ie.  fred"eric -&gt; "fred""eric"
     * @param o the Object to be formatted
     */
    private String getQuoted(Object o) {
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
        return buffer.toString();
    }

    private String getUnQuoted(Object o) {
        return o.toString();
    }
}
