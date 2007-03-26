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
 * An Exception that may be thrown by a QueryOptimiser method.
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class QueryOptimiserException extends Exception
{
    /**
     * Constructs an QueryOptimiserException
     */
    public QueryOptimiserException() {
        super();
    }

    /**
     * Constructs an QueryOptimiserException with the specified detail message.
     *
     * @param msg the detail message
     */
    public QueryOptimiserException(String msg) {
        super(msg);
    }

    /**
     * Constructs an QueryOptimiserException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public QueryOptimiserException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an QueryOptimiserException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public QueryOptimiserException(String msg, Throwable t) {
        super(msg, t);
    }
}
