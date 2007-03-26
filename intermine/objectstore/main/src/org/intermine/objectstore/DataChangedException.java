package org.intermine.objectstore;

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
 * An Exception that may be thrown by an ObjectStore indicating that a request cannot be serviced
 * because the database has changed since the request was started.
 *
 * @author Andrew Varley
 */
public class DataChangedException extends ObjectStoreException
{
    /**
     * Constructs an DataChangedException
     */
    public DataChangedException() {
        super();
    }

    /**
     * Constructs an DataChangedException with the specified detail message.
     *
     * @param msg the detail message
     */
    public DataChangedException(String msg) {
        super(msg);
    }

    /**
     * Constructs an DataChangedException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public DataChangedException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an DataChangedException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public DataChangedException(String msg, Throwable t) {
        super(msg, t);
    }
}

