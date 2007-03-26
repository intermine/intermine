package org.intermine.sql.precompute;

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
 * A class specifically for use with the OptimiserCache, designed to split a SQL string into
 * its functional part, and the limit and offset.
 *
 * @author Matthew Wakeling
 */
public class LimitOffsetQuery
{
    private String query;
    private int limit;
    private int offset;

    private static final int START = 0;
    private static final int NUMBER = 1;
    private static final int GOT_NUMBER = 2;
    private static final int T = 3;
    private static final int OFFSET_E = 4;
    private static final int OFFSET_S = 5;
    private static final int OFFSET_F2 = 6;
    private static final int OFFSET_F1 = 7;
    private static final int OFFSET_O = 8;
    private static final int LIMIT_I2 = 9;
    private static final int LIMIT_M = 10;
    private static final int LIMIT_I1 = 11;
    private static final int LIMIT_L = 12;
    private static final int FINISHED = 13;
    private static final int FINISHED_WELL = 14;
    
    /**
     * Creates a new LimitOffsetQuery object from this query string.
     *
     * @param in the SQL query String
     */
    public LimitOffsetQuery(String in) {
        // Load of parsing here. Probably best to start from the end of the query, and work back.
        int state = START;
        int multiplier = 1;
        int number = 0;
        int pos = in.length() - 1;
        limit = Integer.MAX_VALUE;
        offset = 0;
        while ((state != FINISHED) && (state != FINISHED_WELL)) {
            char c = in.charAt(pos);
            pos--;
            switch (state) {
                case START:
                    if (c == ' ') { 
                        state = START;
                    } else if ((c >= '0') && (c <= '9')) {
                        state = NUMBER;
                        number = c - '0';
                        multiplier = 10;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case NUMBER:
                    if (c == ' ') {
                        state = GOT_NUMBER;
                    } else if ((c >= '0') && (c <= '9')) {
                        number += multiplier * (c - '0');
                        multiplier *= 10;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case GOT_NUMBER:
                    if ((c == 'T') || (c == 't')) {
                        state = T;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case T:
                    if ((c == 'E') || (c == 'e')) {
                        state = OFFSET_E;
                    } else if ((c == 'I') || (c == 'i')) {
                        state = LIMIT_I2;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case OFFSET_E:
                    if ((c == 'S') || (c == 's')) {
                        state = OFFSET_S;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case OFFSET_S:
                    if ((c == 'F') || (c == 'f')) {
                        state = OFFSET_F2;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case OFFSET_F2:
                    if ((c == 'F') || (c == 'f')) {
                        state = OFFSET_F1;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case OFFSET_F1:
                    if ((c == 'O') || (c == 'o')) {
                        state = OFFSET_O;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case OFFSET_O:
                    if (c == ' ') {
                        state = START;
                        offset = number;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case LIMIT_I2:
                    if ((c == 'M') || (c == 'm')) {
                        state = LIMIT_M;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case LIMIT_M:
                    if ((c == 'I') || (c == 'i')) {
                        state = LIMIT_I1;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case LIMIT_I1:
                    if ((c == 'L') || (c == 'l')) {
                        state = LIMIT_L;
                    } else {
                        state = FINISHED;
                    }
                    break;
                case LIMIT_L:
                    if (c == ' ') {
                        state = FINISHED_WELL;
                        limit = number;
                    } else {
                        state = FINISHED;
                    }
                    break;
            }
        }
        if (state == FINISHED) {
            limit = Integer.MAX_VALUE;
            offset = 0;
            query = in;
        } else {
            query = in.substring(0, pos + 1);
        }
    }

    /**
     * Getter for the stripped query.
     *
     * @return the stripped String
     */
    public String getQuery() {
        return query;
    }

    /**
     * Getter for limit.
     *
     * @return limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Getter for offset.
     *
     * @return offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Reconstruct a SQL string, given this alternative SQL query-part.
     *
     * @param in the new SQL query minus the LIMIT and OFFSET
     * @return in plus the LIMIT and OFFSET
     */
    public String reconstruct(String in) {
        return in + (limit == Integer.MAX_VALUE ? "" : " LIMIT " + limit)
            + (offset == 0 ? "" : " OFFSET " + offset);
    }
}

