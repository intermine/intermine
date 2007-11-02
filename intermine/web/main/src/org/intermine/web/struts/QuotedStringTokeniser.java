package org.intermine.web.struts;


/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * String tokenizer.
 */
public class QuotedStringTokeniser
{
    /**
     * Remember the string and the current position.
     */
    int index, length;
    String str;
    String delims;
    
    /**
     * Create a tokenizer from a string.
     * Start from the front.
     * @param s the line to be parsed
     * @param delims the delimiters
     */
    QuotedStringTokeniser(String s, String delims) {
        str = s;
        index = 0;
        length = s.length();
        this.delims = delims;
    }

    /**
     * Test if a char is a delimiter or not
     */
    private boolean isDelim(char c) {
        if (delims.indexOf(c) > -1) {
            return true;
        }
        return false;
    }

    /**
     * Return the index of the first nonwhite char.
     */
    private void skipWhite() {
        while (index != length) {
            if (!isDelim(str.charAt(index))) {
                break;
            }
            index++;
        }
    }

    /**
     * Read a quoted string.
     * Guaranteed the first char is a double quote.
     */
    private String readQuotedString() {
        StringBuffer buffer = new StringBuffer();

        // Skip initial quote
        index++;

        // Collect until next quote
    loop:
        while (index != length) {
            char c = str.charAt(index);
            switch(c) {
            case '"':
                index++;
                break loop;
            case '\\':
                if (index < length - 1) {
                    index++;
                }
                c = str.charAt(index);
                switch(c) {
                case 't':
                    c = '\t';
                    break;
                case 'r':
                    c = '\r';
                    break;
                case 'n':
                    c = '\n';
                    break;
                }
                buffer.append(c);
                break;
            default:
                buffer.append(c);
                break;
            }
            index++;
        }
        
        // Create the string
        return buffer.toString();
    }

    /**
     * Read until the next white space.
     */
    private String readString() {
        int start = index;
        while (index != length) {
            if (isDelim(str.charAt(index))) {
                break;
            }
            index++;
        }
        return str.substring(start, index);
    }

    /**
     * See if at eol.
     * @return true if there is more to parse
     */
    public boolean hasMoreTokens() {
        skipWhite();
        return index != length;
    }

    /**
     * Read the next token.
     * @return the next token
     */
    public String nextToken() {
        String arg;

        // Skip white, then read one of the token types
        skipWhite();
        if (index == length) {
            return "";
        }
        char c = str.charAt(index);
        if (c == '"') {
            arg = readQuotedString();
        } else {
            arg = readString();
        }
        return arg;
    }   
}