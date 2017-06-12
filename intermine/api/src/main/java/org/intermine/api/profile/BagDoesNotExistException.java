package org.intermine.api.profile;

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
 * Exception thrown when bag is not found.
 * @author Daneila Butano
 */
public class BagDoesNotExistException extends RuntimeException
{

    /**
     * Auto-generated serial id.
     */
    private static final long serialVersionUID = -4340637468451454992L;

    /**
     * Constructor
     */
    public BagDoesNotExistException() {
    }

    /**
     * Constructor with message.
     * @param msg message
     */
    public BagDoesNotExistException(String msg) {
        super(msg);
    }

    /**
     * Constructor with throwable
     * @param t another throwable
     */
    public BagDoesNotExistException(Throwable t) {
        super(t);
    }

    /**
     * Constructor with message and throwable
     * @param msg message
     * @param t another throwable
     */
    public BagDoesNotExistException(String msg, Throwable t) {
        super(msg, t);
    }

}
