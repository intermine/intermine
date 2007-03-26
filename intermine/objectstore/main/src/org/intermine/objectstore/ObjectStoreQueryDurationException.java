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
 * An Exception thrown if the estimated time to complete a query is greater than the maximum
 * permitted.
 *
 * @author Kim Rutherford
 */

public class ObjectStoreQueryDurationException extends ObjectStoreException
{
    /**
     * Create a new ObjectStoreQueryDurationException.
     */
    public ObjectStoreQueryDurationException () {
        super();
    }

    /**
     * Create a new ObjectStoreQueryDurationException with the given detail message.
     *
     * @param msg the detail message
     */
    public ObjectStoreQueryDurationException (String msg) {
        super(msg);
    }

    /**
     * Create a new ObjectStoreQueryDurationException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public ObjectStoreQueryDurationException(Throwable t) {
        super(t);
    }

    /**
     * Create a new ObjectStoreQueryDurationException with the specified detail message and nested
     * throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public ObjectStoreQueryDurationException(String msg, Throwable t) {
        super(msg, t);
    }

}
