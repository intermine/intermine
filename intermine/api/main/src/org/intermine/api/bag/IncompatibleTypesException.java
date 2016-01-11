package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Exception thrown when given bags are of incompatible types, has methods to set and fetch the
 * types.
 *
 * @author Richard Smith
 *
 */
public class IncompatibleTypesException extends RuntimeException
{
    /**
     * Constructs an IncompatibleBagTypesException with the specified detail message.
     *
     * @param msg the detail message
     */
    public IncompatibleTypesException(String msg) {
        super(msg);
    }

    /**
     * Constructs an IncompatibleBagTypesException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public IncompatibleTypesException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an IncompatibleBagTypesException with the specified detail message and nested
     * throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public IncompatibleTypesException(String msg, Throwable t) {
        super(msg, t);
    }
}
