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
 * An exception that may be thrown by the objectstore, when an access is made outside the allowable
 * maximum query size.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ObjectStoreLimitReachedException extends ObjectStoreException
{
    /**
     * Constructs an ObjectStoreLimitReachedException.
     */
    public ObjectStoreLimitReachedException() {
        super();
    }

    /**
     * Constructs an ObjectStoreLimitReachedException with the specified detail message.
     *
     * @param msg the detail message
     */
    public ObjectStoreLimitReachedException(String msg) {
        super(msg);
    }
}
