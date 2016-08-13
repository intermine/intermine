package org.intermine.web.logic.results;

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
 * PageOutOfRangeException class
 *
 * @author Kim Rutherford
 */

public class PageOutOfRangeException extends RuntimeException
{
    /**
     * Create a new PageOutOfRangeException.
     * @param message the message to pass to the Exception constructor
     * @param t the Throwable to pass to the Exception constructor
     */
    public PageOutOfRangeException (String message, Throwable t) {
        super(message, t);
    }

    /**
     * Create a new PageOutOfRangeException.
     * @param message the message to pass to the Exception constructor
     */
    public PageOutOfRangeException(String message) {
        super(message);
    }
}
