package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.InterMineException;

/**
 * Exception thrown during parsing constraint value.
 * @author Jakub Kulaviak
 **/
public class ParseValueException extends InterMineException
{
    private static final long serialVersionUID = -3249560275431835992L;

    /**
     * Constructor
     * @param message  message
     */
    public ParseValueException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param msg message
     * @param t exception
     */
    public ParseValueException(String msg, Throwable t) {
        super(msg, t);
    }
}
