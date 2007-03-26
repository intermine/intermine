package org.intermine.metadata;

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
 * An Exception that may be thrown if metadata is in an invalid state.
 * Some aspects of metadata cannot be validated on model construction.
 *
 * @author Richard Smith
 */
public class MetaDataException extends Exception
{
    /**
     * Constructs an MetaDataException
     */
    public MetaDataException() {
        super();
    }

    /**
     * Constructs an MetaDataException with the specified detail message.
     *
     * @param msg the detail message
     */
    public MetaDataException(String msg) {
        super(msg);
    }

    /**
     * Constructs an MetaDataException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public MetaDataException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an MetaDataException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public MetaDataException(String msg, Throwable t) {
        super(msg, t);
    }
}
