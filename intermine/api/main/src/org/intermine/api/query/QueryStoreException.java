package org.intermine.api.query;

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
 *
 * @author Alex
 *
 */
public class QueryStoreException extends Exception
{

    /**
     * @param message message
     * @param e exception
     */
    public QueryStoreException(String message, Exception e) {
        super(message, e);
    }

    /**
     * @param message error message
     */
    public QueryStoreException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 477829937698800861L;

}
